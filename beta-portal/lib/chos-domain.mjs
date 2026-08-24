const OPERATION_TYPES = new Set([
  'case.created',
  'case.updated',
  'case.deleted',
  'work-item.added',
  'work-item.updated',
  'work-item.deleted'
]);

const CASE_STATUSES = new Set(['active', 'paused', 'closed']);
const WORK_ITEM_KINDS = new Set(['observation', 'assumption', 'open-question', 'intervention', 'review']);
const IDENTIFIER_PATTERN = /^[a-zA-Z0-9_-]{8,128}$/;

export class DomainValidationError extends Error {
  constructor(message) {
    super(message);
    this.name = 'DomainValidationError';
  }
}

export function emptyWorkspace() {
  return { version: 1, cases: {}, workItems: {} };
}

function plainObject(value) {
  return Boolean(value && typeof value === 'object' && !Array.isArray(value));
}

function identifier(value, field) {
  if (typeof value !== 'string' || !IDENTIFIER_PATTERN.test(value)) {
    throw new DomainValidationError(`${field} ist ungültig.`);
  }
  return value;
}

function text(value, field, maximum, { optional = false } = {}) {
  if (optional && value === undefined) return undefined;
  if (typeof value !== 'string') throw new DomainValidationError(`${field} muss Text sein.`);
  const normalized = value.trim();
  if (!optional && !normalized) throw new DomainValidationError(`${field} darf nicht leer sein.`);
  if (normalized.length > maximum) throw new DomainValidationError(`${field} ist zu lang.`);
  return normalized;
}

function timestamp(value) {
  if (typeof value !== 'string' || Number.isNaN(Date.parse(value))) {
    throw new DomainValidationError('occurredAt ist ungültig.');
  }
  return value;
}

function onlyKeys(value, allowed, operationType) {
  if (Object.keys(value).some((key) => !allowed.includes(key))) {
    throw new DomainValidationError(`${operationType} enthält unbekannte Felder.`);
  }
}

export function validateWorkspaceSnapshot(value) {
  if (!value || value.version !== 1 || !plainObject(value.cases) || !plainObject(value.workItems)) {
    throw new DomainValidationError('Workspace-Snapshot ist ungültig.');
  }
  for (const [id, entry] of Object.entries(value.cases)) {
    identifier(id, 'case.id');
    if (!plainObject(entry) || entry.id !== id || !CASE_STATUSES.has(entry.status)) {
      throw new DomainValidationError('Arbeitsfall im Snapshot ist ungültig.');
    }
    text(entry.title, 'case.title', 160);
    text(entry.context ?? '', 'case.context', 4000, { optional: true });
    timestamp(entry.createdAt);
    timestamp(entry.updatedAt);
    if (entry.deletedAt !== null) timestamp(entry.deletedAt);
  }
  for (const [id, entry] of Object.entries(value.workItems)) {
    identifier(id, 'workItem.id');
    identifier(entry?.caseId, 'workItem.caseId');
    if (!plainObject(entry) || entry.id !== id || !WORK_ITEM_KINDS.has(entry.kind)) {
      throw new DomainValidationError('Diagnoseelement im Snapshot ist ungültig.');
    }
    text(entry.text, 'workItem.text', 10000);
    timestamp(entry.createdAt);
    timestamp(entry.updatedAt);
    if (entry.deletedAt !== null) timestamp(entry.deletedAt);
  }
  return value;
}

export function validateOperation(candidate) {
  if (!plainObject(candidate)) throw new DomainValidationError('Operation muss ein Objekt sein.');
  onlyKeys(candidate, ['id', 'actorId', 'entityId', 'type', 'occurredAt', 'caseId', 'payload', 'cursor', 'receivedAt'], 'Operation');
  identifier(candidate.id, 'id');
  identifier(candidate.actorId, 'actorId');
  identifier(candidate.entityId, 'entityId');
  if (!OPERATION_TYPES.has(candidate.type)) throw new DomainValidationError('Operationstyp ist ungültig.');
  timestamp(candidate.occurredAt);
  if (!plainObject(candidate.payload)) throw new DomainValidationError('payload muss ein Objekt sein.');
  if (candidate.type.startsWith('case.') && candidate.caseId !== undefined) {
    throw new DomainValidationError('Fall-Operation darf keine caseId enthalten.');
  }

  if (candidate.type === 'case.created') {
    onlyKeys(candidate.payload, ['title', 'context'], candidate.type);
    text(candidate.payload.title, 'title', 160);
    text(candidate.payload.context ?? '', 'context', 4000, { optional: true });
  }

  if (candidate.type === 'case.updated') {
    const allowed = ['title', 'context', 'status'];
    onlyKeys(candidate.payload, allowed, candidate.type);
    if (!Object.keys(candidate.payload).length) {
      throw new DomainValidationError('case.updated enthält keine gültigen Felder.');
    }
    if (candidate.payload.title !== undefined) text(candidate.payload.title, 'title', 160);
    if (candidate.payload.context !== undefined) text(candidate.payload.context, 'context', 4000, { optional: true });
    if (candidate.payload.status !== undefined && !CASE_STATUSES.has(candidate.payload.status)) {
      throw new DomainValidationError('status ist ungültig.');
    }
  }

  if (candidate.type === 'work-item.added') {
    onlyKeys(candidate.payload, ['kind', 'text'], candidate.type);
    identifier(candidate.caseId, 'caseId');
    if (!WORK_ITEM_KINDS.has(candidate.payload.kind)) throw new DomainValidationError('kind ist ungültig.');
    text(candidate.payload.text, 'text', 10000);
  }

  if (candidate.type === 'work-item.updated') {
    identifier(candidate.caseId, 'caseId');
    const allowed = ['kind', 'text'];
    onlyKeys(candidate.payload, allowed, candidate.type);
    if (!Object.keys(candidate.payload).length) {
      throw new DomainValidationError('work-item.updated enthält keine gültigen Felder.');
    }
    if (candidate.payload.kind !== undefined && !WORK_ITEM_KINDS.has(candidate.payload.kind)) {
      throw new DomainValidationError('kind ist ungültig.');
    }
    if (candidate.payload.text !== undefined) text(candidate.payload.text, 'text', 10000);
  }

  if (candidate.type === 'case.deleted') onlyKeys(candidate.payload, [], candidate.type);
  if (candidate.type === 'work-item.deleted') {
    onlyKeys(candidate.payload, [], candidate.type);
    identifier(candidate.caseId, 'caseId');
  }
  return candidate;
}

export function validateClientOperation(candidate) {
  validateOperation(candidate);
  if ('cursor' in candidate || 'receivedAt' in candidate) {
    throw new DomainValidationError('Client-Operation darf keine Servermetadaten enthalten.');
  }
  return candidate;
}

export function applyOperation(workspace, candidate) {
  const operation = validateOperation(candidate);
  const next = {
    version: 1,
    cases: { ...(workspace?.cases || {}) },
    workItems: { ...(workspace?.workItems || {}) }
  };

  if (operation.type === 'case.created') {
    if (!next.cases[operation.entityId]) {
      next.cases[operation.entityId] = {
        id: operation.entityId,
        title: operation.payload.title.trim(),
        context: String(operation.payload.context ?? '').trim(),
        status: 'active',
        createdAt: operation.occurredAt,
        updatedAt: operation.occurredAt,
        deletedAt: null
      };
    }
    return next;
  }

  if (operation.type === 'case.updated') {
    const current = next.cases[operation.entityId];
    if (!current || current.deletedAt) return next;
    next.cases[operation.entityId] = {
      ...current,
      ...(operation.payload.title === undefined ? {} : { title: operation.payload.title.trim() }),
      ...(operation.payload.context === undefined ? {} : { context: operation.payload.context.trim() }),
      ...(operation.payload.status === undefined ? {} : { status: operation.payload.status }),
      updatedAt: operation.occurredAt
    };
    return next;
  }

  if (operation.type === 'case.deleted') {
    const current = next.cases[operation.entityId];
    if (!current || current.deletedAt) return next;
    next.cases[operation.entityId] = { ...current, deletedAt: operation.occurredAt, updatedAt: operation.occurredAt };
    for (const [itemId, item] of Object.entries(next.workItems)) {
      if (item.caseId === operation.entityId && !item.deletedAt) {
        next.workItems[itemId] = { ...item, deletedAt: operation.occurredAt, updatedAt: operation.occurredAt };
      }
    }
    return next;
  }

  if (operation.type === 'work-item.added') {
    const parent = next.cases[operation.caseId];
    if (!parent || parent.deletedAt || next.workItems[operation.entityId]) return next;
    next.workItems[operation.entityId] = {
      id: operation.entityId,
      caseId: operation.caseId,
      kind: operation.payload.kind,
      text: operation.payload.text.trim(),
      createdAt: operation.occurredAt,
      updatedAt: operation.occurredAt,
      deletedAt: null
    };
    return next;
  }

  if (operation.type === 'work-item.updated') {
    const current = next.workItems[operation.entityId];
    if (!current || current.deletedAt || current.caseId !== operation.caseId) return next;
    next.workItems[operation.entityId] = {
      ...current,
      ...(operation.payload.kind === undefined ? {} : { kind: operation.payload.kind }),
      ...(operation.payload.text === undefined ? {} : { text: operation.payload.text.trim() }),
      updatedAt: operation.occurredAt
    };
    return next;
  }

  const current = next.workItems[operation.entityId];
  if (current && !current.deletedAt && current.caseId === operation.caseId) {
    next.workItems[operation.entityId] = { ...current, deletedAt: operation.occurredAt, updatedAt: operation.occurredAt };
  }
  return next;
}

export function applyOperations(workspace, operations) {
  return operations.reduce((state, operation) => applyOperation(state, operation), workspace || emptyWorkspace());
}

function operationBase({ actorId, entityId = crypto.randomUUID(), now = new Date().toISOString() }) {
  return {
    id: crypto.randomUUID(),
    actorId,
    entityId,
    occurredAt: now
  };
}

export function createCaseOperation({ actorId, title, context = '', entityId, now }) {
  return validateOperation({
    ...operationBase({ actorId, entityId, now }),
    type: 'case.created',
    payload: { title, context }
  });
}

export function updateCaseOperation({ actorId, caseId, changes, now }) {
  return validateOperation({
    ...operationBase({ actorId, entityId: caseId, now }),
    type: 'case.updated',
    payload: { ...changes }
  });
}

export function deleteCaseOperation({ actorId, caseId, now }) {
  return validateOperation({
    ...operationBase({ actorId, entityId: caseId, now }),
    type: 'case.deleted',
    payload: {}
  });
}

export function addWorkItemOperation({ actorId, caseId, kind, text: value, entityId, now }) {
  return validateOperation({
    ...operationBase({ actorId, entityId, now }),
    type: 'work-item.added',
    caseId,
    payload: { kind, text: value }
  });
}

export function updateWorkItemOperation({ actorId, caseId, itemId, changes, now }) {
  return validateOperation({
    ...operationBase({ actorId, entityId: itemId, now }),
    type: 'work-item.updated',
    caseId,
    payload: { ...changes }
  });
}

export function deleteWorkItemOperation({ actorId, caseId, itemId, now }) {
  return validateOperation({
    ...operationBase({ actorId, entityId: itemId, now }),
    type: 'work-item.deleted',
    caseId,
    payload: {}
  });
}

export function visibleCases(workspace) {
  return Object.values(workspace?.cases || {})
    .filter((entry) => !entry.deletedAt)
    .sort((left, right) => right.updatedAt.localeCompare(left.updatedAt));
}

export function visibleWorkItems(workspace, caseId) {
  return Object.values(workspace?.workItems || {})
    .filter((entry) => entry.caseId === caseId && !entry.deletedAt)
    .sort((left, right) => left.createdAt.localeCompare(right.createdAt));
}
