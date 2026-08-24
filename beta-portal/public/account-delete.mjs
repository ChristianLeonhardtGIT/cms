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
  if (!userId || !globalThis.indexedDB) {
    if (status) status.textContent = 'Es war keine lokale Arbeitskopie in diesem Browser vorhanden.';
    return;
  }
  const database = await openWorkspaceDatabase();
  try {
    await new Promise((resolve, reject) => {
      const transaction = database.transaction('runtimes', 'readwrite');
      transaction.objectStore('runtimes').delete(`user:${userId}`);
      transaction.oncomplete = resolve;
      transaction.onerror = () => reject(transaction.error);
      transaction.onabort = () => reject(transaction.error || new Error('Lokale Löschung wurde abgebrochen.'));
    });
    if (status) status.textContent = 'Die lokale Arbeitskopie wurde aus diesem Browser entfernt.';
  } finally {
    database.close();
  }
}

deleteLocalWorkspace().catch(() => {
  if (status) status.textContent = 'Die lokale Arbeitskopie konnte nicht automatisch entfernt werden. Lösche bitte die Websitedaten dieses Browsers.';
});
