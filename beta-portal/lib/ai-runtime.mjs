export const AI_DATA_CLASSES = Object.freeze({
  PRIVATE: 'private',
  SHAREABLE: 'shareable'
});

export class AiUnavailableError extends Error {
  constructor(message, reason = 'unavailable') {
    super(message);
    this.name = 'AiUnavailableError';
    this.reason = reason;
  }
}

export function createAiProvider({ id, available, run }) {
  if (!id || typeof available !== 'function' || typeof run !== 'function') {
    throw new TypeError('Ein AI-Provider benötigt id, available() und run().');
  }
  return Object.freeze({ id, available, run });
}

export function createDisabledAiProvider(id, reason = 'noch nicht konfiguriert') {
  return createAiProvider({
    id,
    available: async () => false,
    run: async () => {
      throw new AiUnavailableError(`${id} ist ${reason}.`);
    }
  });
}

export function createLocalAiProvider({ available, run } = {}) {
  if (typeof available !== 'function' || typeof run !== 'function') {
    return createDisabledAiProvider('local', 'noch nicht angeschlossen');
  }
  return createAiProvider({ id: 'local', available, run });
}

export function createCloudAiProvider({ enabled = false, endpoint = '/beta/api/ai/run', fetchImpl = globalThis.fetch } = {}) {
  if (!endpoint.startsWith('/')) throw new TypeError('Der Cloud-AI-Endpunkt muss Same-Origin verwenden.');
  return createAiProvider({
    id: 'cloud',
    available: async () => enabled,
    run: async (request) => {
      if (!enabled) throw new AiUnavailableError('Cloud-AI ist bewusst deaktiviert.');
      const response = await fetchImpl(endpoint, {
        method: 'POST',
        credentials: 'same-origin',
        headers: { 'Content-Type': 'application/json', 'X-ChOS-Client': 'workspace-v1' },
        body: JSON.stringify(request)
      });
      if (!response.ok) throw new Error('Cloud-AI-Anfrage ist fehlgeschlagen.');
      return response.json();
    }
  });
}

function validateRequest(request) {
  if (!request || typeof request.task !== 'string' || !request.task.trim()) {
    throw new TypeError('AI-Anfrage benötigt eine Aufgabe.');
  }
  if (!Object.values(AI_DATA_CLASSES).includes(request.dataClass)) {
    throw new TypeError('AI-Anfrage benötigt eine gültige Datenklasse.');
  }
}

export function createHybridAiService({ local, cloud }) {
  if (!local || !cloud) throw new TypeError('Lokaler und Cloud-Provider sind erforderlich.');

  return Object.freeze({
    async capabilities() {
      const [localAvailable, cloudAvailable] = await Promise.all([local.available(), cloud.available()]);
      return {
        local: { id: local.id, available: localAvailable },
        cloud: { id: cloud.id, available: cloudAvailable }
      };
    },

    async run(request) {
      validateRequest(request);

      if (await local.available()) {
        return { provider: local.id, output: await local.run(request) };
      }

      if (request.dataClass === AI_DATA_CLASSES.PRIVATE) {
        throw new AiUnavailableError(
          'Private Arbeitsdaten werden ohne verfügbaren lokalen Provider nicht verarbeitet.',
          'local-required'
        );
      }

      if (request.allowCloud !== true) {
        throw new AiUnavailableError('Cloud-Verarbeitung wurde für diese Anfrage nicht freigegeben.', 'cloud-consent-required');
      }

      if (!(await cloud.available())) {
        throw new AiUnavailableError('Es ist kein AI-Provider verfügbar.');
      }

      return { provider: cloud.id, output: await cloud.run(request) };
    }
  });
}
