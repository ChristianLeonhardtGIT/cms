import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const baseUrl = (process.env.BASE_URL || 'https://cleonhardt.de').replace(/\/$/, '');
const sitemapResponse = await fetch(`${baseUrl}/sitemap.xml`);
assert.equal(sitemapResponse.status, 200, 'Sitemap ist nicht erreichbar');
const sitemap = await sitemapResponse.text();
const urls = Array.from(sitemap.matchAll(/<loc>(.*?)<\/loc>/g), (match) => match[1])
  .filter((url) => url.startsWith(baseUrl));

const formalPatterns = [
  /\b(?:Ihnen|Ihr|Ihre|Ihrem|Ihren|Ihrer|Ihres)\b/g,
  /\b(?:Lassen|Wählen|Beschreiben|Bewerten|Prüfen|Trennen|Formulieren|Vergleichen|Suchen|Nehmen|Benennen|Beobachten|Vereinbaren|Markieren|Entwickeln|Fragen|Definieren|Starten|Nutzen|Achten|Beginnen|Ergänzen|Schreiben|Machen|Unterscheiden|Betrachten|Laden|Versuchen|Übermitteln) Sie\b/g,
  /\bSie (?:erhalten|schildern|nennen|tragen|wollen|möchten|können|müssen|finden|wählen|beschreiben|prüfen|starten|nutzen|vergleichen|suchen|übermitteln|kontaktieren|haben|werden|entscheiden|sehen|brauchen|benötigen)\b/g,
  /\b(?:können|haben|erreichen|möchten|sollten|dürfen|erkennen) Sie\b/g,
  /\b(?:für|bei|mit|von|zu) (?:Sie|Ihnen)\b/g,
  /\b(?:Was|Woran|Warum|Wie|Wo|Welche|Welcher|Welches|wenn|dass|die|bevor) Sie\b/g,
  /\bSie sind (?:beim|noch|unsicher)\b/g,
];
const findings = [];

for (const url of urls) {
  const response = await fetch(url);
  if (!response.ok) continue;
  const html = await response.text();
  const main = html.match(/<main\b[^>]*>([\s\S]*?)<\/main>/i)?.[1] ?? html;
  const text = main
    .replace(/<script\b[\s\S]*?<\/script>/gi, ' ')
    .replace(/<style\b[\s\S]*?<\/style>/gi, ' ')
    .replace(/<[^>]+>/g, ' ')
    .replace(/&nbsp;|&#160;/g, ' ')
    .replace(/&amp;/g, '&')
    .replace(/&quot;|&#34;/g, '"')
    .replace(/&#39;|&apos;/g, "'")
    .replace(/\s+/g, ' ')
    .trim();

  const matches = formalPatterns.flatMap((pattern) => [...text.matchAll(pattern)]);
  if (!matches.length) continue;
  const contexts = matches.map((match) => {
    const start = Math.max(0, text.lastIndexOf('.', match.index - 1) + 1);
    const nextPeriod = text.indexOf('.', match.index);
    const end = nextPeriod === -1 ? Math.min(text.length, match.index + 180) : nextPeriod + 1;
    return text.slice(start, end).trim();
  });
  findings.push({ path: new URL(url).pathname, contexts: [...new Set(contexts)] });
}

console.log(JSON.stringify({ pages: urls.length, findings }, null, 2));
assert.equal(findings.length, 0, `Formelle Ansprache auf ${findings.length} öffentlichen Seiten gefunden`);

const staticFiles = [
  'light-modules/meine-website/webresources/js/navigation.js',
  'light-modules/meine-website/webresources/js/hubspot-form.js',
  'light-modules/meine-website/webresources/js/chos-check.js',
];
for (const file of staticFiles) {
  const source = await readFile(file, 'utf8');
  const matches = formalPatterns.flatMap((pattern) => [...source.matchAll(new RegExp(pattern.source, pattern.flags))]);
  assert.equal(matches.length, 0, `Formelle Ansprache in ${file} gefunden: ${matches.map((match) => match[0]).join(', ')}`);
}
