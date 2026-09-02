const baseUrl = process.env.SITE_URL ?? 'https://cleonhardt.de';

const pages = new Map([
  ['/praxisfaelle', ['ChOS in der Praxis', 'keine Kundenreferenzen', 'Fallmuster 03']],
  ['/chos-selbstcheck', ['ChOS Selbstcheck', '20 Fragen', 'keine Antworten übertragen oder gespeichert']],
  ['/chos', ['ChOS auf deine Organisation anwenden.', '/chos-selbstcheck', '/praxisfaelle']],
]);

for (const [path, expected] of pages) {
  const response = await fetch(`${baseUrl}${path}`);
  const html = await response.text();
  if (response.status !== 200) throw new Error(`${path}: HTTP ${response.status}`);
  if (html.includes('RenderingException')) throw new Error(`${path}: Magnolia RenderingException.`);
  for (const snippet of expected) {
    if (!html.includes(snippet)) throw new Error(`${path}: Inhalt fehlt: ${snippet}`);
  }
  if (!html.includes(`<link rel="canonical" href="${baseUrl}${path}">`)) {
    throw new Error(`${path}: Canonical fehlt.`);
  }
}

const checkHtml = await (await fetch(`${baseUrl}/chos-selbstcheck`)).text();
if ((checkHtml.match(/data-question=/g) ?? []).length !== 20) {
  throw new Error('Der Selbstcheck enthält nicht genau 20 Fragen.');
}
if ((checkHtml.match(/data-dimension=/g) ?? []).length !== 20) {
  throw new Error('Die 20 Fragen sind nicht vollständig Dimensionen zugeordnet.');
}

const sitemap = await (await fetch(`${baseUrl}/sitemap.xml`)).text();
for (const path of ['/praxisfaelle', '/chos-selbstcheck']) {
  if (!sitemap.includes(`<loc>${baseUrl}${path}</loc><lastmod>`)) {
    throw new Error(`Sitemap: ${path} fehlt.`);
  }
}

console.log(JSON.stringify({ checked: pages.size, questions: 20 }, null, 2));
