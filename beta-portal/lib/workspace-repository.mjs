import { createHash } from 'node:crypto';
import { chmod, mkdir, open, readFile, rename, unlink } from 'node:fs/promises';
import path from 'node:path';
import { validateClientOperation, validateOperation } from './chos-domain.mjs';
import { SYNC_PROTOCOL_VERSION } from './local-first-workspace.mjs';

const MAX_WORKSPACE_OPERATIONS = 5000;
const MAX_WORKSPACE_BYTES = 10 * 1024 * 1024;

function freshStore() {
  return { version: 1, nextCursor: 1, operations: [] };
}

function validateStore(value) {
  if (!value || value.version !== 1 || !Number.isInteger(value.nextCursor) || value.nextCursor < 1 || !Array.isArray(value.operations)) {
    throw new Error('Ungültiger Workspace-Datenbestand.');
  }
  let expectedCursor = 1;
  for (const operation of value.operations) {
    validateOperation(operation);
    if (operation.cursor !== expectedCursor || typeof operation.receivedAt !== 'string' || Number.isNaN(Date.parse(operation.receivedAt))) {
      throw new Error('Workspace-Operationsjournal ist beschädigt.');
    }
    expectedCursor += 1;
  }
  if (value.nextCursor !== expectedCursor) throw new Error('Workspace-Cursor ist inkonsistent.');
  return value;
}

function operationFingerprint(operation) {
  return JSON.stringify({
    id: operation.id,
    actorId: operation.actorId,
    entityId: operation.entityId,
    type: operation.type,
    occurredAt: operation.occurredAt,
    ...(operation.caseId === undefined ? {} : { caseId: operation.caseId }),
    payload: operation.payload
  });
}

function workspaceName(userId) {
  return createHash('sha256').update(String(userId)).digest('hex');
}

export class FileWorkspaceRepository {
  constructor(dataDirectory) {
    this.directory = path.join(dataDirectory, 'workspaces');
  }

  paths(userId) {
    const name = workspaceName(userId);
    return {
      data: path.join(this.directory, `${name}.json`),
      lock: path.join(this.directory, `${name}.lock`)
    };
  }

  async load(userId) {
    try {
      return validateStore(JSON.parse(await readFile(this.paths(userId).data, 'utf8')));
    } catch (error) {
      if (error.code === 'ENOENT') return freshStore();
      throw error;
    }
  }

  async write(userId, store) {
    await mkdir(this.directory, { recursive: true, mode: 0o700 });
    const target = this.paths(userId).data;
    const temporary = path.join(this.directory, `.${workspaceName(userId)}-${process.pid}-${Date.now()}.tmp`);
    const handle = await open(temporary, 'wx', 0o600);
    try {
      await handle.writeFile(`${JSON.stringify(validateStore(store), null, 2)}\n`, 'utf8');
      await handle.sync();
    } finally {
      await handle.close();
    }
    await rename(temporary, target);
    await chmod(target, 0o600);
  }

  async lock(userId) {
    await mkdir(this.directory, { recursive: true, mode: 0o700 });
    const lockPath = this.paths(userId).lock;
    for (let attempt = 0; attempt < 100; attempt += 1) {
      try {
        return await open(lockPath, 'wx', 0o600);
      } catch (error) {
        if (error.code !== 'EEXIST') throw error;
        await new Promise((resolve) => setTimeout(resolve, 20));
      }
    }
    throw new Error('Workspace ist derzeit gesperrt.');
  }

  async sync(userId, request, { canWrite }) {
    if (!request || request.protocolVersion !== SYNC_PROTOCOL_VERSION || !Number.isInteger(request.after) || request.after < 0) {
      throw new WorkspaceRequestError('Ungültige Sync-Anfrage.');
    }
    if (!Array.isArray(request.operations) || request.operations.length > 100) {
      throw new WorkspaceRequestError('Pro Sync sind höchstens 100 Operationen erlaubt.');
    }
    request.operations.forEach(validateClientOperation);
    if (!canWrite && request.operations.length) throw new WorkspaceReadOnlyError();

    const lock = await this.lock(userId);
    try {
      const store = await this.load(userId);
      const originalOperationCount = store.operations.length;
      const existingById = new Map(store.operations.map((operation) => [operation.id, operation]));
      const acceptedOperationIds = [];

      for (const operation of request.operations) {
        acceptedOperationIds.push(operation.id);
        const existing = existingById.get(operation.id);
        if (existing) {
          if (operationFingerprint(existing) !== operationFingerprint(operation)) {
            throw new WorkspaceRequestError('Eine Operations-ID wurde mit abweichendem Inhalt erneut verwendet.');
          }
          continue;
        }
        store.operations.push({
          ...operation,
          cursor: store.nextCursor,
          receivedAt: new Date().toISOString()
        });
        store.nextCursor += 1;
        existingById.set(operation.id, operation);
      }

      const storeChanged = store.operations.length !== originalOperationCount;
      if (storeChanged) {
        if (store.operations.length > MAX_WORKSPACE_OPERATIONS) {
          throw new WorkspaceRequestError('Die maximale Anzahl an Workspace-Änderungen ist erreicht. Bitte wende dich an den Support.');
        }
        if (Buffer.byteLength(`${JSON.stringify(store, null, 2)}\n`, 'utf8') > MAX_WORKSPACE_BYTES) {
          throw new WorkspaceRequestError('Der maximale Workspace-Speicher ist erreicht. Bitte wende dich an den Support.');
        }
        await this.write(userId, store);
      }
      const cursor = store.nextCursor - 1;
      const reset = request.after > cursor;
      return {
        protocolVersion: SYNC_PROTOCOL_VERSION,
        cursor,
        reset,
        operations: store.operations.filter((operation) => reset || operation.cursor > request.after),
        acceptedOperationIds,
        serverTime: new Date().toISOString()
      };
    } finally {
      await lock.close();
      await unlink(this.paths(userId).lock).catch(() => {});
    }
  }

  async delete(userId) {
    const lock = await this.lock(userId);
    try {
      await unlink(this.paths(userId).data).catch((error) => {
        if (error.code !== 'ENOENT') throw error;
      });
    } finally {
      await lock.close();
      await unlink(this.paths(userId).lock).catch(() => {});
    }
  }
}

export class WorkspaceRequestError extends Error {
  constructor(message) {
    super(message);
    this.name = 'WorkspaceRequestError';
  }
}

export class WorkspaceReadOnlyError extends Error {
  constructor() {
    super('Der Workspace ist in dieser Zugriffsphase schreibgeschützt.');
    this.name = 'WorkspaceReadOnlyError';
  }
}
