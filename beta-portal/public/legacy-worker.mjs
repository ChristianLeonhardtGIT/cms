// Served at the historical URL: a redirected service-worker script cannot update.
self.addEventListener('install', () => self.skipWaiting());
self.addEventListener('activate', (event) => event.waitUntil((async () => {
  for (const name of await caches.keys()) {
    if (!name.startsWith('chos-reader-v1-')) continue;
    const cache = await caches.open(name);
    for (const request of await cache.keys()) {
      if (new URL(request.url).pathname.startsWith('/beta/')) await cache.delete(request);
    }
  }
  await self.registration.unregister();
})()));
