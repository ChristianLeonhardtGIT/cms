import { applyOperation, applyOperations, emptyWorkspace, validateClientOperation, validateWorkspaceSnapshot } from './chos-domain.mjs';

export const SYNC_PROTOCOL_VERSION = 1;

export function createLocalRuntime(actorId) {
  return {
    version: 1,
    actorId,
    cursor: 0,
    confirmed: emptyWorkspace(),
    outbox: [],
    lastSyncAt: null
  };
}

export function validateLocalRuntime(value, fallbackActorId) {
  if (!value || value.version !== 1 || !Number.isInteger(value.cursor) || value.cursor < 0 || !Array.isArray(value.outbox)) {
    return createLocalRuntime(fallbackActorId);
  }
  if (typeof value.actorId !== 'string' || !value.actorId) return createLocalRuntime(fallbackActorId);
  try {
    value.outbox.forEach(validateClientOperation);
    validateWorkspaceSnapshot(value.confirmed);
  } catch {
    return createLocalRuntime(fallbackActorId);
  }
  return {
    version: 1,
    actorId: value.actorId,
    cursor: value.cursor,
    confirmed: value.confirmed || emptyWorkspace(),
    outbox: value.outbox,
    lastSyncAt: value.lastSyncAt || null
  };
}

export function workspaceView(runtime) {
  return applyOperations(runtime.confirmed, runtime.outbox);
}

export function enqueueLocalOperation(runtime, operation) {
  validateClientOperation(operation);
  if (operation.actorId !== runtime.actorId) throw new Error('Operation gehört nicht zu diesem lokalen Gerät.');
  applyOperation(workspaceView(runtime), operation);
  return { ...runtime, outbox: [...runtime.outbox, operation] };
}

export function createSyncRequest(runtime, maximumOperations = 100) {
  return {
    protocolVersion: SYNC_PROTOCOL_VERSION,
    after: runtime.cursor,
    operations: runtime.outbox.slice(0, maximumOperations)
  };
}

export function mergeSyncResponse(runtime, response) {
  if (!response || response.protocolVersion !== SYNC_PROTOCOL_VERSION || !Number.isInteger(response.cursor) || response.cursor < 0) {
    throw new Error('Ungültige Sync-Antwort.');
  }
  if (!Array.isArray(response.operations) || !Array.isArray(response.acceptedOperationIds)) {
    throw new Error('Unvollständige Sync-Antwort.');
  }
  const accepted = new Set(response.acceptedOperationIds);
  const confirmed = applyOperations(response.reset ? emptyWorkspace() : runtime.confirmed, response.operations);
  return {
    ...runtime,
    cursor: response.cursor,
    confirmed,
    outbox: runtime.outbox.filter((operation) => !accepted.has(operation.id)),
    lastSyncAt: response.serverTime || new Date().toISOString()
  };
}
