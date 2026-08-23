const attributionKeys = ['utm_source', 'utm_medium', 'utm_campaign'];
const currentUrl = new URL(window.location.href);
const attribution = new URLSearchParams();

for (const key of attributionKeys) {
  const value = currentUrl.searchParams.get(key)?.trim();
  if (value) attribution.set(key, value.slice(0, 120));
}

if (!attribution.has('utm_source') && document.referrer) {
  const referrer = new URL(document.referrer);
  if (referrer.origin !== window.location.origin) {
    attribution.set('utm_source', referrer.hostname.replace(/^www\./, '').slice(0, 120));
    attribution.set('utm_medium', 'referral');
  }
}

if (attribution.size > 0) {
  document.querySelectorAll('a[href]').forEach((link) => {
    const target = new URL(link.href, window.location.href);
    if (
      target.origin !== window.location.origin ||
      (target.pathname === currentUrl.pathname && target.search === currentUrl.search && target.hash)
    ) return;

    for (const [key, value] of attribution) {
      if (!target.searchParams.has(key)) target.searchParams.set(key, value);
    }
    link.href = target.href;
  });
}

window.cleonhardtAttribution = Object.freeze(Object.fromEntries(attribution));
