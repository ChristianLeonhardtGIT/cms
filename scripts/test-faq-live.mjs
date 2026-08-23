import assert from 'node:assert/strict';

const baseUrl = process.env.BASE_URL || 'https://cleonhardt.de';
const paths = [
  '/',
  '/chos',
  '/chos-selbstcheck',
  '/praxisfaelle',
  '/ueber-mich',
  '/insights',
  '/insights/warum-produktorganisationen-nicht-an-fehlenden-methoden-scheitern',
  '/insights/diagnose-vor-eingriff',
  '/insights/unklare-rollen-sind-selten-das-eigentliche-problem',
  '/insights/rollen-und-verantwortlichkeiten-in-produktorganisationen-klaeren',
  '/insights/wann-braucht-eine-produktorganisation-ein-operating-model',
  '/insights/annahmen-vor-einer-produktentscheidung-pruefen',
  '/insights/organisationsdiagnose-statt-standardberatung',
  '/insights/product-organisation-diagnostic-ablauf-und-ergebnis',
  '/insights/wann-ist-executive-sparring-sinnvoll'
];

const htmlByPath = new Map();
for (const path of paths) {
  const response = await fetch(`${baseUrl}${path}`);
  assert.equal(response.status, 200, `${path} must return 200`);
  const html = await response.text();
  htmlByPath.set(path, html);
  assert.equal((html.match(/<details class="faq-item">/g) || []).length, 3, `${path} must show three FAQ items`);

  const scripts = [...html.matchAll(/<script type="application\/ld\+json">([\s\S]*?)<\/script>/g)].map((match) => JSON.parse(match[1]));
  const faq = scripts.find((entry) => entry['@type'] === 'FAQPage');
  assert.ok(faq, `${path} must expose FAQPage JSON-LD`);
  assert.equal(faq.mainEntity.length, 3, `${path} must expose three structured questions`);
  assert.ok(faq.mainEntity.every((item) => item['@type'] === 'Question' && item.acceptedAnswer?.['@type'] === 'Answer'));
}

const homeScripts = [...htmlByPath.get('/').matchAll(/<script type="application\/ld\+json">([\s\S]*?)<\/script>/g)].map((match) => JSON.parse(match[1]));
const homeGraph = homeScripts.find((entry) => Array.isArray(entry['@graph']))?.['@graph'];
const organization = homeGraph?.find((entry) => entry['@type'] === 'Organization');
assert.match(organization?.logo?.url || '', /(?:\/dam\/jcr:|christian-leonhardt-chos-signet-invoice\.png)/);
assert.doesNotMatch(organization?.logo?.url || '', /social-card/);

const weakTargets = [
  '/insights/annahmen-vor-einer-produktentscheidung-pruefen',
  '/insights/organisationsdiagnose-statt-standardberatung',
  '/insights/product-organisation-diagnostic-ablauf-und-ergebnis',
  '/insights/wann-ist-executive-sparring-sinnvoll'
];
const insightHtml = paths.filter((path) => path.startsWith('/insights/')).map((path) => htmlByPath.get(path)).join('\n');
for (const target of weakTargets) {
  const escaped = target.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  const links = insightHtml.match(new RegExp(`href="${escaped}"`, 'g')) || [];
  assert.ok(links.length >= 2, `${target} needs at least two inbound related-insight links`);
}

console.log(`FAQ-Live-Test erfolgreich: ${paths.length} Seiten, gültiges Schema und gestärkte interne Links.`);
