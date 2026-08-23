import assert from 'node:assert/strict';

const baseUrl = process.env.BASE_URL || 'https://cleonhardt.de';
const criticalPagePaths = [
  '/clarity-session',
  '/decision-review',
  '/executive-sparring',
  '/product-organisation-diagnostic',
  '/ai-operating-model-assessment',
  '/ai-enabled-workflow-sprint',
];

const sitemapResponse = await fetch(`${baseUrl}/sitemap.xml`);
assert.equal(sitemapResponse.status, 200, 'Sitemap ist nicht erreichbar.');
const sitemap = await sitemapResponse.text();
const sitemapUrls = [...sitemap.matchAll(/<loc>([^<]+)<\/loc>/g)].map((match) => match[1]);
assert.ok(sitemapUrls.length > 0, 'Sitemap enthält keine Seiten.');

const requestLinks = [];
for (const url of sitemapUrls) {
  const response = await fetch(url);
  assert.equal(response.status, 200, `Öffentliche Seite ist nicht erreichbar: ${url}`);
  const html = await response.text();
  for (const match of html.matchAll(/href=["']([^"']+)["']/gi)) {
    const link = new URL(match[1], baseUrl);
    if (link.origin === baseUrl && link.pathname === '/angebot-anfragen') {
      requestLinks.push({ source: url, target: link.href });
    }
  }
}

assert.ok(requestLinks.length > 0, 'Keine öffentliche Seite verlinkt auf die Angebotsanfrage.');

const aliases = {
  'CHOS-CLARITY-001': 'clarity-session',
  'CHOS-REVIEW-001': 'decision-review',
  'CHOS-SPARRING-2W-001': 'sparring',
  'CHOS-EXEC-001': 'executive-sparring',
  'CHOS-QUICK-DIAG-001': 'quick-diagnostic',
  'CHOS-ORG-DIAG-001': 'product-organisation-diagnostic',
  'CHOS-AI-ASSESS-001': 'ai-operating-model-assessment',
  'CHOS-AI-WORKFLOW-001': 'ai-workflow-sprint',
  'CHOS-WORKSHOP-DAY-001': 'workshop',
};

for (const { source, target } of requestLinks) {
  const response = await fetch(target);
  assert.equal(response.status, 200, `Formularlink auf ${source} liefert HTTP ${response.status}.`);
  const html = await response.text();
  assert.match(html, /<form[^>]+id=["']angebot-anfragen["']/i, `Formular fehlt hinter ${target}.`);
  assert.match(html, /type=["']submit["']/i, `Senden-Button fehlt hinter ${target}.`);

  const requestedOffer = new URL(target).searchParams.get('leistung');
  const expectedOption = aliases[requestedOffer] || requestedOffer;
  if (expectedOption) {
    assert.ok(
      html.includes(`value="${expectedOption}"`) || html.includes(`value='${expectedOption}'`),
      `Leistung ${requestedOffer} hat keine passende Formularoption (${expectedOption}).`,
    );
  }
}

for (const path of criticalPagePaths) {
  if (!sitemapUrls.includes(`${baseUrl}${path}`)) continue;
  const response = await fetch(`${baseUrl}${path}`);
  assert.equal(response.status, 200, `${path} ist nicht erreichbar.`);
  const html = await response.text();
  assert.match(html, /href=["'][^"']*\/angebot-anfragen/i, `${path} enthält keinen Link zur Angebotsanfrage.`);
}

console.log(JSON.stringify({
  checkedPages: sitemapUrls.length,
  checkedRequestLinks: requestLinks.length,
  result: 'Lead-Funnel-Regressionsprüfung bestanden',
}, null, 2));
