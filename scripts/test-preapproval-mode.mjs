import assert from 'node:assert/strict';

const baseUrl = process.env.BASE_URL || 'https://cleonhardt.de';
const blockedPaths = [
  '/leistungen',
  '/clarity-session',
  '/decision-review',
  '/leadership-product-sparring',
  '/executive-sparring',
  '/quick-diagnostic',
  '/product-organisation-diagnostic',
  '/workshops',
  '/angebot-anfragen'
];
const publicPaths = ['/', '/chos', '/praxisfaelle', '/chos-selbstcheck', '/ueber-mich', '/insights', '/kontakt'];

for (const path of blockedPaths) {
  const response = await fetch(`${baseUrl}${path}`, { redirect: 'manual' });
  assert.equal(response.status, 404, `${path} must not be publicly available`);
}

const sitemap = await (await fetch(`${baseUrl}/sitemap.xml`)).text();
for (const path of blockedPaths) {
  assert.ok(!sitemap.includes(`<loc>${baseUrl}${path}</loc>`), `${path} must not be in sitemap.xml`);
}

for (const path of publicPaths) {
  const response = await fetch(`${baseUrl}${path}`);
  assert.equal(response.status, 200, `${path} must remain available`);
  const html = await response.text();
  for (const blockedPath of blockedPaths) {
    assert.ok(!html.includes(`href="${blockedPath}`), `${path} links to blocked offer ${blockedPath}`);
  }
  assert.ok(!html.includes('"@type": "Service"'), `${path} exposes Service schema`);
  assert.ok(!html.includes('"@type": "Offer"'), `${path} exposes Offer schema`);
  assert.ok(!html.includes('"@type": "AggregateOffer"'), `${path} exposes AggregateOffer schema`);
}

const homepage = await (await fetch(`${baseUrl}/`)).text();
assert.ok(!/\d+[.,]?\d*\s*€\s*netto/i.test(homepage), 'Homepage still exposes a net price');

const contact = await (await fetch(`${baseUrl}/kontakt`)).text();
for (const value of ['clarity-session', 'decision-review', 'sparring', 'executive-sparring', 'quick-diagnostic', 'product-organisation-diagnostic', 'workshop']) {
  assert.ok(!contact.includes(`<option value="${value}"`), `Contact form still exposes ${value}`);
}

console.log('Pre-approval publication gate passed.');
