// Do not touch personal ChOS IndexedDB data or unrelated registrations.
if ('serviceWorker' in navigator) {
  for (const registration of await navigator.serviceWorker.getRegistrations()) {
    if (new URL(registration.scope).pathname === '/beta/') await registration.unregister();
  }
}
if ('caches' in globalThis) {
  for (const name of await caches.keys()) {
    if (!name.startsWith('chos-reader-v1-')) continue;
    const cache = await caches.open(name);
    for (const request of await cache.keys()) {
      if (new URL(request.url).pathname.startsWith('/beta/')) await cache.delete(request);
    }
  }
}
