import { access, readFile } from 'node:fs/promises';

const base = 'light-modules/meine-website';
const types = ['problem', 'tool', 'case', 'insight', 'topic', 'legalDocument'];

for (const type of types) {
  await access(`${base}/contentTypes/${type}.yaml`);
  const app = await readFile(`${base}/apps/${type === 'problem' ? 'problems' : `${type}s`}.yaml`, 'utf8');
  if (!app.includes(`!content-type:${type}`)) {
    throw new Error(`Content App für ${type} verweist nicht auf den richtigen Content Type.`);
  }
}

const problem = await readFile(`${base}/contentTypes/problem.yaml`, 'utf8');
const offer = await readFile(`${base}/contentTypes/offer.yaml`, 'utf8');
const legalDocument = await readFile(`${base}/contentTypes/legalDocument.yaml`, 'utf8');
const legalTemplate = await readFile(`${base}/templates/components/legalDocument.ftl`, 'utf8');

for (const relation of [
  'reference:topic',
  'reference:problem',
  'reference:insight',
  'reference:tool',
  'reference:offer',
  'reference:case',
]) {
  if (!problem.includes(relation)) throw new Error(`Problem-Relation fehlt: ${relation}`);
}

for (const field of [
  'buyingSituation',
  'symptoms',
  'hypotheses',
  'diagnosticQuestions',
  'selfCheckSteps',
  'smallestNextStep',
  'whenToGetHelp',
  'closingThought',
]) {
  if (!problem.includes(`name: ${field}`)) throw new Error(`Problem-Feld fehlt: ${field}`);
}

if (!offer.includes('workspace: offers')) {
  throw new Error('Der bestehende Angebotskatalog wurde unerwartet ersetzt.');
}

for (const field of ['title', 'slug', 'documentType', 'body', 'sourcePage']) {
  if (!legalDocument.includes(`name: ${field}`)) throw new Error(`Rechtstext-Feld fehlt: ${field}`);
}

if (!legalTemplate.includes("'legalDocuments'") || !legalTemplate.includes('legalDocumentReference')) {
  throw new Error('Die Rechtstext-Komponente liest nicht aus dem zentralen Workspace.');
}

if (types.includes('service')) {
  throw new Error('Leistungen dürfen nicht durch einen zweiten Service-Typ dupliziert werden.');
}

console.log('ChOS-Content-Modell ist vollständig und referenziert den bestehenden Angebotskatalog.');
