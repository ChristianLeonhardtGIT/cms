const baseUrl = process.env.SITE_URL ?? 'https://cleonhardt.de';

const services = new Map([
  ['/decision-review', 'Decision Review'],
  ['/executive-sparring', 'Executive Sparring'],
  ['/product-organisation-diagnostic', 'Product Organisation Diagnostic'],
]);

const articles = new Map([
  ['/insights/rollen-und-verantwortlichkeiten-in-produktorganisationen-klaeren', 'Rollen und Verantwortlichkeiten in Produktorganisationen klären'],
  ['/insights/wann-braucht-eine-produktorganisation-ein-operating-model', 'Wann braucht eine Produktorganisation ein Operating Model?'],
  ['/insights/annahmen-vor-einer-produktentscheidung-pruefen', 'Annahmen vor einer Produktentscheidung prüfen'],
  ['/insights/organisationsdiagnose-statt-standardberatung', 'Organisationsdiagnose statt Standardberatung'],
  ['/insights/product-organisation-diagnostic-ablauf-und-ergebnis', 'Product Organisation Diagnostic: Ablauf und Ergebnis'],
  ['/insights/wann-ist-executive-sparring-sinnvoll', 'Wann ist Executive Sparring sinnvoll?'],
]);

const results = [];
for (const [path, title] of [...services, ...articles]) {
  const response = await fetch(`${baseUrl}${path}`);
  const html = await response.text();
  if (response.status !== 200) throw new Error(`${path}: HTTP ${response.status}`);
  if (!html.includes(`<link rel="canonical" href="${baseUrl}${path}">`)) {
    throw new Error(`${path}: Canonical fehlt.`);
  }
  if (!html.includes(title) || html.includes('RenderingException')) {
    throw new Error(`${path}: Inhalt oder Rendering fehlerhaft.`);
  }

  const jsonLdMatch = html.match(/<script type="application\/ld\+json">([\s\S]*?)<\/script>/);
  if (!jsonLdMatch) throw new Error(`${path}: JSON-LD fehlt.`);
  const structuredData = JSON.parse(jsonLdMatch[1]);
  const types = structuredData['@graph'].map((entry) => entry['@type']);
  const expectedType = services.has(path) ? 'Service' : 'Article';
  if (!types.includes(expectedType) || !types.includes('Person') || !types.includes('Organization')) {
    throw new Error(`${path}: Erwartete strukturierte Typen fehlen (${types.join(', ')}).`);
  }
  if (articles.has(path) && (!html.includes('Kurzantwort') || !html.includes('datePublished') || !html.includes('class="article-meta"') || !html.includes('rel="author"'))) {
    throw new Error(`${path}: Zitierfähige Kurzantwort, sichtbare Autorenschaft oder Veröffentlichungsdatum fehlt.`);
  }
  results.push({ path, type: expectedType, status: response.status });
}

const robots = await (await fetch(`${baseUrl}/robots.txt`)).text();
for (const crawler of ['OAI-SearchBot', 'ChatGPT-User', 'GPTBot']) {
  if (!robots.includes(`User-agent: ${crawler}`)) throw new Error(`robots.txt: ${crawler} fehlt.`);
}

const llmsResponse = await fetch(`${baseUrl}/llms.txt`);
const llms = await llmsResponse.text();
if (llmsResponse.status !== 200 || !llms.includes('https://cleonhardt.de/insights')) {
  throw new Error('llms.txt fehlt oder enthält keine Insights-Quelle.');
}

const sitemap = await (await fetch(`${baseUrl}/sitemap.xml`)).text();
for (const path of [...services.keys(), ...articles.keys()]) {
  if (!sitemap.includes(`<loc>${baseUrl}${path}</loc><lastmod>`)) {
    throw new Error(`Sitemap: ${path} oder lastmod fehlt.`);
  }
}
if (sitemap.includes(`<loc>${baseUrl}/workshops</loc>`)) {
  throw new Error('Sitemap: Die pausierte Workshop-Seite darf nicht enthalten sein.');
}

const pausedWorkshop = await fetch(`${baseUrl}/workshops`, { redirect: 'manual' });
if (pausedWorkshop.status !== 404) {
  throw new Error(`/workshops: erwarteter Pausenstatus 404, erhalten ${pausedWorkshop.status}.`);
}

const indexNowKey = '4eba2fbccd4fbe055b49e6d9d41c4e00';
const keyResponse = await fetch(`${baseUrl}/${indexNowKey}.txt`);
if (keyResponse.status !== 200 || (await keyResponse.text()).trim() !== indexNowKey) {
  throw new Error('IndexNow-Key ist nicht öffentlich validierbar.');
}

console.log(JSON.stringify({ checked: results.length, results }, null, 2));
