import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const sources = [
  'scripts/create-quote-request.groovy',
  'scripts/implement-public-faqs.groovy',
  'scripts/add-confidentiality-trust.groovy',
  'scripts/implement-point5-content.groovy',
  'scripts/implement-geo-content.groovy',
];

const invalidPatterns = [
  /\b(?:beschreiben|geben|wünschen|vergleichen|prüfen|schildern|übermitteln|trennen) Sie\b/gi,
  /\b(?:Wählen|Bewerten|Suchen|Beginnen|Nutzen|Achten|Schreiben|Markieren|Sammeln) Sie\b/g,
  /\bSie (?:erhalten|senden|müssen|tragen|wollen|benötigen)\b/g,
  /\b(?:bei|für|zu) Ihnen\b/g,
  /\b(?:Ihr|Ihre|Ihrem|Ihren|Ihrer|Ihres) (?:Angebot|Anfrage|Angaben|Einordnung|Situation|Produktorganisation|Antworten)\b/g,
];

const findings = [];
for (const file of sources) {
  const source = await readFile(file, 'utf8');
  for (const pattern of invalidPatterns) {
    for (const match of source.matchAll(new RegExp(pattern.source, pattern.flags))) {
      findings.push({ file, phrase: match[0] });
    }
  }
}

assert.deepEqual(findings, [], `Inkonsistente Ansprache in Content-Quellen: ${JSON.stringify(findings)}`);

const migration = await readFile('scripts/implement-home-journey-and-du-tone.groovy', 'utf8');
assert.doesNotMatch(migration, /'Sie werden'\s*:\s*'du wirst'/, 'Anaphorisches "Sie werden" darf nicht pauschal ersetzt werden.');

const repairMigration = await readFile('scripts/fix-editorial-du-tone.groovy', 'utf8');
assert.match(repairMigration, /current\.getDepth\(\)/, 'Die JCR-Tiefe muss Groovy-4-kompatibel explizit gelesen werden.');
assert.doesNotMatch(repairMigration, /current\.depth\b/, 'JCR node.depth wird in Groovy 4 als unbekannte Property aufgelöst.');

const exportScript = await readFile('scripts/export-cms-content.groovy', 'utf8');
assert.match(exportScript, /OffsetDateTime\.now\(\)/, 'Export-Metadaten müssen die Java-Time-API verwenden.');
assert.doesNotMatch(exportScript, /new Date\(\)\.format\(/, 'Date.format ist unter Groovy 4 nicht verfügbar.');

console.log(`Redaktioneller Quellencheck bestanden: ${sources.length} Content-Skripte.`);
