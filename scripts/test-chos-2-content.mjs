import { readFile } from 'node:fs/promises';

const implementation = await readFile(new URL('./implement-chos-2-content.groovy', import.meta.url), 'utf8');
const publication = await readFile(new URL('./publish-chos-2-content.groovy', import.meta.url), 'utf8');
const template = await readFile(new URL('../light-modules/meine-website/templates/pages/home.ftl', import.meta.url), 'utf8');

for (const required of [
  'Klarheit für komplexe Produktorganisationen.',
  'Mehr Technik löst keine unklaren Strukturen.',
  'Erst verstehen. Dann wirksam verändern',
  'Diagnose -&gt; Design -&gt; Veränderung'.replaceAll('&gt;', '>'),
  'Richtung', 'Entscheidungen', 'Verantwortung', 'Zusammenarbeit', 'Lernen',
  'ChOS Clarity Session', 'ChOS Decision Review', 'ChOS Operating Model Diagnostic',
  'AI Operating Model Assessment', 'AI-enabled Workflow / Product Sprint',
  'ChOS Transformation Program', 'Executive / Product Leadership Sparring',
  "'/leistungen'", "'/chos'", "'/ueber-mich'", "'/insights'"
]) {
  if (!implementation.includes(required)) throw new Error(`ChOS-Inhalt fehlt: ${required}`);
}

for (const obsoletePrimaryOffer of ['Online Marketing', 'Performance Marketing', 'SEO-Beratung']) {
  if (implementation.includes(obsoletePrimaryOffer)) throw new Error(`Veraltete Primärpositionierung enthalten: ${obsoletePrimaryOffer}`);
}

for (const path of [
  '/ai-operating-model-assessment', '/ai-enabled-workflow-sprint',
  '/chos-transformation-program', '/insights/decision-rights-zwischen-mensch-und-ai'
]) {
  if (!implementation.includes(path)) throw new Error(`Neue Seite fehlt: ${path}`);
}

if (!implementation.includes("offerFolder.hasNode(name) ? offerFolder.getNode(name)")) {
  throw new Error('Angebote werden nicht in-place aktualisiert; HubSpot-Verknüpfungen wären gefährdet.');
}

if (!publication.includes("[repository: 'offers', path: '/cleonhardt']") ||
    !publication.includes("[repository: 'navigation', path: '/cleonhardt/Main']")) {
  throw new Error('Publikation von Angebotskatalog oder Navigation fehlt.');
}

if (!template.includes('Klarheit für komplexe Produktorganisationen')) {
  throw new Error('Globale Metadaten wurden nicht auf ChOS aktualisiert.');
}

if (/ChOS 2\.0|ChOS-2\.0/.test(implementation + template)) {
  throw new Error('Versionsbezeichnung ChOS 2.0 darf nicht mehr vorkommen.');
}

console.log('ChOS-Strukturtest bestanden.');
