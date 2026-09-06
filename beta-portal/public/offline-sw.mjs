const CACHE_PREFIX = 'chos-reader-v1-';
const READY_URL = new URL('/workspace/__offline__/ready', self.location.origin).href;
const SUPPORT_PATHS = new Set(['/workspace/assets/owner-bridge.css', '/workspace/assets/offline-reader.mjs', '/workspace/api/chos/knowledge-index']);

self.addEventListener('install', () => self.skipWaiting());
self.addEventListener('activate', (event) => event.waitUntil(self.clients.claim()));

async function readyCaches() {
  const names = (await caches.keys()).filter((name) => name.startsWith(CACHE_PREFIX));
  const ready = [];
  for (const name of names) {
    const cache = await caches.open(name);
    const response = await cache.match(READY_URL);
    if (!response) continue;
    try {
      ready.push({ name, cache, manifest: await response.json() });
    } catch {}
  }
  return ready.sort((left, right) => String(right.manifest.savedAt).localeCompare(String(left.manifest.savedAt)));
}

async function cachedResponse(request) {
  const ready = await readyCaches();
  for (const { cache } of ready) {
    const response = await cache.match(request);
    if (response) return response;
  }
  return null;
}

self.addEventListener('fetch', (event) => {
  if (event.request.method !== 'GET') return;
  const url = new URL(event.request.url);
  const eligible = url.origin === self.location.origin && (url.pathname.startsWith('/workspace/chos/') || SUPPORT_PATHS.has(url.pathname));
  if (!eligible) return;
  event.respondWith((async () => {
    const cached = await cachedResponse(event.request);
    try {
      const response = await fetch(event.request);
      if (response.redirected || response.status === 401 || response.status === 403 || response.status < 500) return response;
      return cached || response;
    } catch {
      if (cached) return cached;
      return new Response('ChOS ist auf diesem Gerät noch nicht offline verfügbar.', {
        status: 503,
        headers: { 'Content-Type': 'text/plain; charset=utf-8' }
      });
    }
  })());
});
