const root = document.querySelector('[data-offline-reader]');
const CACHE_PREFIX = 'chos-reader-v1-';
const READY_URL = new URL('/beta/__offline__/ready', location.origin).href;

if (root) {
  const panel = root.querySelector('[data-offline-panel]');
  const toggle = root.querySelector('[data-offline-toggle]');
  const close = root.querySelector('[data-offline-close]');
  const save = root.querySelector('[data-offline-save]');
  const remove = root.querySelector('[data-offline-remove]');
  const status = root.querySelector('[data-offline-status]');
  let registration;

  function formatSize(bytes) {
    return new Intl.NumberFormat('de-DE', { style: 'unit', unit: 'megabyte', maximumFractionDigits: 1 })
      .format(bytes / 1024 / 1024);
  }

  function showStatus(message, state = '') {
    status.textContent = message;
    status.dataset.state = state;
  }

  async function registerWorker() {
    if (!registration) {
      registration = await navigator.serviceWorker.register('/beta/offline-sw.mjs?v=20260824-2', { scope: '/beta/' });
      await navigator.serviceWorker.ready;
    }
    return registration;
  }

  async function readyCaches() {
    const names = (await caches.keys()).filter((name) => name.startsWith(CACHE_PREFIX));
    const ready = [];
    for (const name of names) {
      const cache = await caches.open(name);
      const response = await cache.match(READY_URL);
      if (!response) continue;
      try {
        ready.push({ name, manifest: await response.json() });
      } catch {}
    }
    return ready.sort((left, right) => String(right.manifest.savedAt).localeCompare(String(left.manifest.savedAt)));
  }

  function validateBundle(bundle) {
    const manifest = bundle?.manifest;
    if (bundle?.schemaVersion !== 1 || manifest?.schemaVersion !== 1 || !/^[a-f0-9]{16}$/.test(manifest.version)) {
      throw new Error('Der Offline-Lesestand hat ein ungültiges Format.');
    }
    if (!Array.isArray(bundle.files) || !Array.isArray(manifest.urls) || bundle.files.length !== manifest.urls.length || bundle.files.length > 500) {
      throw new Error('Der Offline-Lesestand enthält eine ungültige Dateiliste.');
    }
    if (!Number.isSafeInteger(manifest.sizeBytes) || manifest.sizeBytes < 1 || manifest.sizeBytes > 25 * 1024 * 1024) {
      throw new Error('Der Offline-Lesestand überschreitet die erlaubte Größe.');
    }
    const expected = new Set(manifest.urls);
    for (const file of bundle.files) {
      const url = new URL(file.url, location.origin);
      if (url.origin !== location.origin || !expected.delete(url.pathname) || typeof file.contentType !== 'string' || typeof file.body !== 'string') {
        throw new Error('Der Offline-Lesestand enthält eine nicht erlaubte Datei.');
      }
    }
    if (expected.size) throw new Error('Der Offline-Lesestand ist unvollständig.');
  }

  function decodeBase64(value) {
    const binary = atob(value);
    const bytes = new Uint8Array(binary.length);
    for (let index = 0; index < binary.length; index += 1) bytes[index] = binary.charCodeAt(index);
    return bytes;
  }

  async function storeBundle(bundle) {
    validateBundle(bundle);
    const suffix = crypto.randomUUID?.() || `${Date.now()}-${Math.random().toString(16).slice(2)}`;
    const cacheName = `${CACHE_PREFIX}${bundle.manifest.version}-${suffix}`;
    const cache = await caches.open(cacheName);
    try {
      for (const file of bundle.files) {
        const url = new URL(file.url, location.origin);
        await cache.put(url, new Response(decodeBase64(file.body), {
          headers: { 'Content-Type': file.contentType }
        }));
      }
      const readyManifest = { ...bundle.manifest, savedAt: new Date().toISOString() };
      await cache.put(READY_URL, new Response(JSON.stringify(readyManifest), {
        headers: { 'Content-Type': 'application/json; charset=utf-8' }
      }));
      const oldNames = (await caches.keys()).filter((name) => name.startsWith(CACHE_PREFIX) && name !== cacheName);
      await Promise.all(oldNames.map((name) => caches.delete(name)));
      return readyManifest;
    } catch (error) {
      await caches.delete(cacheName);
      throw error;
    }
  }

  async function refreshStatus() {
    try {
      const [current] = await readyCaches();
      if (!current) {
        showStatus('Noch nicht auf diesem Gerät gespeichert.');
        remove.disabled = true;
        return;
      }
      const savedAt = new Intl.DateTimeFormat('de-DE', { dateStyle: 'medium', timeStyle: 'short' })
        .format(new Date(current.manifest.savedAt));
      showStatus(`Offline bereit · ${formatSize(current.manifest.sizeBytes)} · gespeichert ${savedAt}`, 'ready');
      save.textContent = 'Stand aktualisieren';
      remove.disabled = false;
    } catch (error) {
      showStatus(error.message, 'error');
    }
  }

  function setBusy(busy) {
    save.disabled = busy;
    remove.disabled = busy;
    toggle.disabled = busy;
  }

  if (!('serviceWorker' in navigator) || !('caches' in globalThis)) {
    showStatus('Dieser Browser unterstützt die Offline-Funktion nicht.', 'error');
    save.disabled = true;
    remove.disabled = true;
  } else {
    registerWorker().then(refreshStatus).catch((error) => showStatus(error.message, 'error'));

    save.addEventListener('click', async () => {
      setBusy(true);
      showStatus('Der aktuelle ChOS-Stand wird vollständig gespeichert …');
      try {
        navigator.storage?.persist?.().catch(() => false);
        const response = await fetch('/beta/api/chos/offline-bundle', { cache: 'no-store', credentials: 'same-origin' });
        if (!response.ok || response.redirected) throw new Error('Der aktuelle Lesestand konnte nicht abgerufen werden.');
        const readyManifest = await storeBundle(await response.json());
        await registerWorker();
        showStatus(`Offline bereit · ${formatSize(readyManifest.sizeBytes)} · ${readyManifest.fileCount} Dateien`, 'ready');
        save.textContent = 'Stand aktualisieren';
        remove.disabled = false;
      } catch (error) {
        showStatus(error.message, 'error');
      } finally {
        setBusy(false);
      }
    });

    remove.addEventListener('click', async () => {
      setBusy(true);
      showStatus('Lokaler Lesestand wird entfernt …');
      try {
        const names = (await caches.keys()).filter((name) => name.startsWith(CACHE_PREFIX));
        await Promise.all(names.map((name) => caches.delete(name)));
        showStatus('Der ChOS-Lesestand wurde von diesem Gerät gelöscht.');
        save.textContent = 'Aktuellen Stand speichern';
        remove.disabled = true;
      } catch (error) {
        showStatus(error.message, 'error');
      } finally {
        setBusy(false);
      }
    });
  }

  toggle.addEventListener('click', () => {
    const open = panel.hidden;
    panel.hidden = !open;
    toggle.setAttribute('aria-expanded', String(open));
    if (open) {
      close.focus();
      refreshStatus();
    }
  });

  close.addEventListener('click', () => {
    panel.hidden = true;
    toggle.setAttribute('aria-expanded', 'false');
    toggle.focus();
  });
}
