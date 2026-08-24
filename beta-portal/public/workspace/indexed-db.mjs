import { createLocalRuntime, validateLocalRuntime } from './local-first-workspace.mjs';

const DATABASE_NAME = 'chos-workspace';
const STORE_NAME = 'runtimes';

function openDatabase() {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open(DATABASE_NAME, 1);
    request.onupgradeneeded = () => {
      if (!request.result.objectStoreNames.contains(STORE_NAME)) request.result.createObjectStore(STORE_NAME);
    };
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });
}

function transactionResult(transaction, request) {
  return new Promise((resolve, reject) => {
    transaction.oncomplete = () => resolve(request?.result);
    transaction.onerror = () => reject(transaction.error || request?.error);
    transaction.onabort = () => reject(transaction.error || new Error('Lokale Speicherung wurde abgebrochen.'));
  });
}

export function createIndexedDbRuntimeStore(userKey) {
  return Object.freeze({
    async load() {
      const database = await openDatabase();
      try {
        const transaction = database.transaction(STORE_NAME, 'readonly');
        const request = transaction.objectStore(STORE_NAME).get(userKey);
        const stored = await transactionResult(transaction, request);
        const actorId = stored?.actorId || crypto.randomUUID();
        return stored ? validateLocalRuntime(stored, actorId) : createLocalRuntime(actorId);
      } finally {
        database.close();
      }
    },

    async save(runtime) {
      const database = await openDatabase();
      try {
        const transaction = database.transaction(STORE_NAME, 'readwrite');
        const request = transaction.objectStore(STORE_NAME).put(runtime, userKey);
        await transactionResult(transaction, request);
      } finally {
        database.close();
      }
    },

    async clear() {
      const database = await openDatabase();
      try {
        const transaction = database.transaction(STORE_NAME, 'readwrite');
        const request = transaction.objectStore(STORE_NAME).delete(userKey);
        await transactionResult(transaction, request);
      } finally {
        database.close();
      }
    }
  });
}
