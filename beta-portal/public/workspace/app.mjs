import {
  addWorkItemOperation,
  createCaseOperation,
  deleteCaseOperation,
  deleteWorkItemOperation,
  visibleCases,
  visibleWorkItems
} from './chos-domain.mjs';
import {
  createSyncRequest,
  enqueueLocalOperation,
  mergeSyncResponse,
  workspaceView
} from './local-first-workspace.mjs';
import {
  createCloudAiProvider,
  createLocalAiProvider,
  createHybridAiService
} from './ai-runtime.mjs';
import { createIndexedDbRuntimeStore } from './indexed-db.mjs';

const elements = {
  caseDetail: document.querySelector('#case-detail'),
  caseForm: document.querySelector('#case-form'),
  caseList: document.querySelector('#case-list'),
  clearLocal: document.querySelector('#clear-local'),
  deleteCase: document.querySelector('#delete-case'),
  detailContext: document.querySelector('#detail-context'),
  detailTitle: document.querySelector('#detail-title'),
  emptyState: document.querySelector('#empty-state'),
  itemForm: document.querySelector('#item-form'),
  itemList: document.querySelector('#item-list'),
  networkDot: document.querySelector('#network-dot'),
  readonlyNotice: document.querySelector('#readonly-notice'),
  syncStatus: document.querySelector('#sync-status'),
  aiStatus: document.querySelector('#ai-status')
};

let bootstrap;
let runtime;
let runtimeStore;
let selectedCaseId = null;
let syncing = false;
let runtimeEpoch = 0;

const kindLabels = {
  observation: 'Beobachtung',
  assumption: 'Annahme',
  'open-question': 'Offene Frage',
  intervention: 'Intervention',
  review: 'Review'
};

async function jsonRequest(url, options = {}) {
  const response = await fetch(url, {
    credentials: 'same-origin',
    ...options,
    headers: { Accept: 'application/json', ...(options.headers || {}) }
  });
  if (response.status === 401) {
    window.location.assign('/beta/login');
    throw new Error('Anmeldung abgelaufen.');
  }
  const body = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(body.error || 'Anfrage fehlgeschlagen.');
  return body;
}

function updateNetworkStatus(message) {
  const online = navigator.onLine;
  elements.networkDot.classList.toggle('online', online);
  if (message) {
    elements.syncStatus.textContent = message;
    return;
  }
  if (!online) {
    elements.syncStatus.textContent = `${runtime?.outbox.length || 0} Änderung(en) nur lokal gespeichert`;
  } else if (runtime?.outbox.length) {
    elements.syncStatus.textContent = `${runtime.outbox.length} Änderung(en) warten auf Synchronisation`;
  } else if (runtime?.lastSyncAt) {
    elements.syncStatus.textContent = `Synchronisiert · ${new Intl.DateTimeFormat('de-DE', { timeStyle: 'short' }).format(new Date(runtime.lastSyncAt))}`;
  } else {
    elements.syncStatus.textContent = 'Lokale Arbeitskopie bereit';
  }
}

function button(label, className, data = {}) {
  const element = document.createElement('button');
  element.type = 'button';
  element.className = className;
  element.textContent = label;
  for (const [key, value] of Object.entries(data)) element.dataset[key] = value;
  return element;
}

function render() {
  const view = workspaceView(runtime);
  const cases = visibleCases(view);
  if (!selectedCaseId || !cases.some((entry) => entry.id === selectedCaseId)) selectedCaseId = cases[0]?.id || null;

  elements.caseList.replaceChildren();
  if (!cases.length) {
    const empty = document.createElement('p');
    empty.className = 'empty-list';
    empty.textContent = 'Noch keine Arbeitsfälle auf diesem Gerät.';
    elements.caseList.append(empty);
  }
  for (const entry of cases) {
    const caseButton = button('', `case-button${entry.id === selectedCaseId ? ' active' : ''}`, { caseId: entry.id });
    const title = document.createElement('strong');
    title.textContent = entry.title;
    const count = document.createElement('small');
    const itemCount = visibleWorkItems(view, entry.id).length;
    count.textContent = `${itemCount} ${itemCount === 1 ? 'Diagnoseelement' : 'Diagnoseelemente'}`;
    caseButton.append(title, count);
    elements.caseList.append(caseButton);
  }

  const selected = selectedCaseId && view.cases[selectedCaseId];
  elements.emptyState.hidden = Boolean(selected);
  elements.caseDetail.hidden = !selected;
  if (selected) {
    elements.detailTitle.textContent = selected.title;
    elements.detailContext.textContent = selected.context || 'Noch kein zusätzlicher Kontext.';
    elements.itemList.replaceChildren();
    const items = visibleWorkItems(view, selected.id);
    if (!items.length) {
      const empty = document.createElement('p');
      empty.className = 'empty-list';
      empty.textContent = 'Noch keine Beobachtungen, Annahmen oder offenen Fragen.';
      elements.itemList.append(empty);
    }
    for (const item of items) {
      const article = document.createElement('article');
      article.className = 'work-item';
      const kind = document.createElement('span');
      kind.className = `kind kind--${item.kind}`;
      kind.textContent = kindLabels[item.kind];
      const itemText = document.createElement('p');
      itemText.textContent = item.text;
      const remove = button('Entfernen', 'icon-button', { itemId: item.id, caseId: item.caseId });
      remove.disabled = !bootstrap.canWrite;
      article.append(kind, itemText, remove);
      elements.itemList.append(article);
    }
  }

  for (const control of elements.caseForm.elements) control.disabled = !bootstrap.canWrite;
  for (const control of elements.itemForm.elements) control.disabled = !bootstrap.canWrite;
  elements.deleteCase.disabled = !bootstrap.canWrite;
  elements.readonlyNotice.hidden = bootstrap.canWrite;
  updateNetworkStatus();
}

async function persist(operation) {
  runtime = enqueueLocalOperation(runtime, operation);
  await runtimeStore.save(runtime);
  render();
  void syncNow();
}

async function syncNow() {
  if (syncing || !navigator.onLine) return;
  syncing = true;
  let syncSucceeded = false;
  let needsFreshSync = false;
  const syncEpoch = runtimeEpoch;
  updateNetworkStatus('Synchronisiere …');
  try {
    const request = createSyncRequest(runtime);
    if (!bootstrap.canWrite) request.operations = [];
    const response = await jsonRequest('/beta/api/workspace/sync', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'X-ChOS-Client': 'workspace-v1' },
      body: JSON.stringify(request)
    });
    if (syncEpoch !== runtimeEpoch) {
      needsFreshSync = true;
      return;
    }
    runtime = mergeSyncResponse(runtime, response);
    await runtimeStore.save(runtime);
    syncSucceeded = true;
    render();
  } catch (error) {
    updateNetworkStatus(`Nur lokal gespeichert · ${error.message}`);
  } finally {
    syncing = false;
    if (needsFreshSync || (syncSucceeded && bootstrap.canWrite && navigator.onLine && runtime.outbox.length)) {
      setTimeout(() => void syncNow(), 0);
    }
  }
}

elements.caseForm.addEventListener('submit', async (event) => {
  event.preventDefault();
  if (!bootstrap.canWrite) return;
  const form = new FormData(elements.caseForm);
  const operation = createCaseOperation({
    actorId: runtime.actorId,
    title: form.get('title'),
    context: form.get('context')
  });
  selectedCaseId = operation.entityId;
  await persist(operation);
  elements.caseForm.reset();
});

elements.caseList.addEventListener('click', (event) => {
  const target = event.target.closest('[data-case-id]');
  if (!target) return;
  selectedCaseId = target.dataset.caseId;
  render();
});

elements.itemForm.addEventListener('submit', async (event) => {
  event.preventDefault();
  if (!bootstrap.canWrite || !selectedCaseId) return;
  const form = new FormData(elements.itemForm);
  await persist(addWorkItemOperation({
    actorId: runtime.actorId,
    caseId: selectedCaseId,
    kind: form.get('kind'),
    text: form.get('text')
  }));
  elements.itemForm.reset();
});

elements.itemList.addEventListener('click', async (event) => {
  const target = event.target.closest('[data-item-id]');
  if (!target || !bootstrap.canWrite) return;
  await persist(deleteWorkItemOperation({
    actorId: runtime.actorId,
    caseId: target.dataset.caseId,
    itemId: target.dataset.itemId
  }));
});

elements.deleteCase.addEventListener('click', async () => {
  if (!bootstrap.canWrite || !selectedCaseId) return;
  if (!window.confirm('Diesen Arbeitsfall und seine Diagnoseelemente löschen?')) return;
  const caseId = selectedCaseId;
  selectedCaseId = null;
  await persist(deleteCaseOperation({ actorId: runtime.actorId, caseId }));
});

elements.clearLocal.addEventListener('click', async () => {
  if (!window.confirm('Die lokale Kopie auf diesem Gerät löschen? Synchronisierte Daten auf dem Server bleiben erhalten.')) return;
  runtimeEpoch += 1;
  await runtimeStore.clear();
  runtime = await runtimeStore.load();
  selectedCaseId = null;
  render();
  void syncNow();
});

window.addEventListener('online', () => {
  updateNetworkStatus();
  void syncNow();
});
window.addEventListener('offline', () => updateNetworkStatus());

async function start() {
  bootstrap = await jsonRequest('/beta/api/workspace/bootstrap');
  runtimeStore = createIndexedDbRuntimeStore(`user:${bootstrap.user.id}`);
  runtime = await runtimeStore.load();
  render();

  const ai = createHybridAiService({
    local: createLocalAiProvider(),
    cloud: createCloudAiProvider({ enabled: bootstrap.ai.cloudProvider })
  });
  const capabilities = await ai.capabilities();
  elements.aiStatus.textContent = capabilities.local.available || capabilities.cloud.available
    ? 'AI-Verarbeitung verfügbar'
    : 'AI-Schnittstelle vorbereitet · Provider noch deaktiviert';

  void syncNow();
}

start().catch((error) => {
  updateNetworkStatus(`Workspace konnte nicht gestartet werden · ${error.message}`);
});
