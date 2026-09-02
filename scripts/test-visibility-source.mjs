import assert from 'node:assert/strict';
import { readFile, stat } from 'node:fs/promises';

const [caddy, llms, template, portraitFix, sitemapGenerator] = await Promise.all([
  readFile('deploy/Caddyfile', 'utf8'),
  readFile('deploy/static/llms.txt', 'utf8'),
  readFile('light-modules/meine-website/templates/pages/home.ftl', 'utf8'),
  readFile('scripts/fix-central-portrait-delivery.groovy', 'utf8'),
  readFile('scripts/generate-public-sitemap.py', 'utf8'),
]);

assert.match(caddy, /@site_metadata path[^\n]*\/llms\.txt/, 'llms.txt wird nicht statisch ausgeliefert.');
for (const url of [
  'https://cleonhardt.de/chos',
  'https://cleonhardt.de/leistungen',
  'https://cleonhardt.de/insights',
  'https://cleonhardt.de/ueber-mich',
]) {
  assert.ok(llms.includes(url), `llms.txt enthält die zentrale URL nicht: ${url}`);
}

assert.ok(template.includes('hasModifiedDate'), 'CMS-Änderungsdatum wird nicht im WebPage-Schema ausgegeben.');
assert.ok(sitemapGenerator.includes('extract_last_modified'), 'Sitemap übernimmt keine CMS-Änderungsdaten.');
assert.ok(portraitFix.includes("getProperty('portraitImage').remove()"), 'Fehlerhafte DAM-Porträt-Referenz wird nicht korrigiert.');

for (const asset of [
  'light-modules/meine-website/webresources/images/christian-leonhardt-portrait-v1.webp',
  'light-modules/meine-website/webresources/images/christian-leonhardt-portrait-v1.jpg',
]) {
  const info = await stat(asset);
  assert.ok(info.size < 150_000, `${asset} ist mit ${info.size} Bytes zu groß.`);
}

console.log('Sichtbarkeits-Quellencheck bestanden: llms.txt, lastmod und optimiertes Porträt sind abgesichert.');
