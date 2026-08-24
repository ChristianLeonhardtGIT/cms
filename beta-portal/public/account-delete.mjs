const status = document.querySelector('#local-delete-status');
const userId = document.querySelector('[data-deleted-user]')?.dataset.deletedUser;

function openWorkspaceDatabase() {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open('chos-workspace', 1);
    request.onupgradeneeded = () => {
      if (!request.result.objectStoreNames.contains('runtimes')) request.result.createObjectStore('runtimes');
    };
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });
}

async function deleteLocalWorkspace() {
  if (!userId || !globalThis.indexedDB) return;
  const database = await openWorkspaceDatabase();
  try {
    await new Promise((resolve, reject) => {
      const transaction = database.transaction('runtimes', 'readwrite');
      transaction.objectStore('runtimes').delete(`user:${userId}`);
      transaction.oncomplete = resolve;
      transaction.onerror = () => reject(transaction.error);
      transaction.onabort = () => reject(transaction.error || new Error('Lokale Löschung wurde abgebrochen.'));
    });
  } finally {
    database.close();
  }
}

async function deleteOfflineReader() {
  localStorage.removeItem('chos:published-knowledge-index:v1');
  if (!globalThis.caches) return;
  const names = (await caches.keys()).filter((name) => name.startsWith('chos-reader-v1-'));
  await Promise.all(names.map((name) => caches.delete(name)));
}

Promise.all([deleteLocalWorkspace(), deleteOfflineReader()]).then(() => {
  if (status) status.textContent = 'Die lokale Arbeitskopie und offline gespeicherte ChOS-Inhalte wurden aus diesem Browser entfernt.';
}).catch(() => {
  if (status) status.textContent = 'Die lokalen Daten konnten nicht vollständig automatisch entfernt werden. Lösche bitte die Websitedaten dieses Browsers.';
});
