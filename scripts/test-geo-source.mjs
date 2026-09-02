import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const [template, styles, settings] = await Promise.all([
  readFile('light-modules/meine-website/templates/pages/home.ftl', 'utf8'),
  readFile('light-modules/meine-website/webresources/css/site.css', 'utf8'),
  readFile('light-modules/meine-website/contentTypes/siteSettings.yaml', 'utf8'),
]);

for (const snippet of [
  'class="article-meta"',
  'rel="author"',
  'Veröffentlicht am',
  'Aktualisiert am',
  '"sameAs"',
  'personSameAs',
]) {
  assert.ok(template.includes(snippet), `GEO-Signal fehlt im Seitentemplate: ${snippet}`);
}

assert.ok(styles.includes('.article-meta__inner'), 'Darstellung der sichtbaren Artikelmetadaten fehlt.');
assert.ok(settings.includes('name: linkedInUrl'), 'Konfigurierbares, verifiziertes LinkedIn-Profil fehlt.');
assert.ok(settings.includes('name: professionalProfileUrl'), 'Konfigurierbares weiteres Fachprofil fehlt.');

console.log('GEO-Quellencheck bestanden: Autorenschaft, Datumsangaben und Identitätsverknüpfung sind vorhanden.');
