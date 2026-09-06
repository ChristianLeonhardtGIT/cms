import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const privacy = await readFile('scripts/update-privacy-policy.groovy', 'utf8');
const historicalBuild = await readFile('scripts/build-mvp.groovy', 'utf8');

for (const expected of [
  'Persönlich freigeschalteter Workspace',
  'Sparring-Nachrichten und Intake-Angaben werden ausdrücklich nicht',
  'keine Ende-zu-Ende-Verschlüsselung',
  'Benachrichtigungs-E-Mails enthalten weder Chatnachrichten',
  '30 Tage nach Abschluss oder Stornierung',
  'Die Bestätigung der Datenregeln dokumentiert deren Kenntnisnahme und ist keine datenschutzrechtliche Einwilligung',
  'Auskunft, Datenexport, Berichtigung, Einschränkung'
]) {
  assert.match(privacy, new RegExp(expected.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')));
}

assert.doesNotMatch(privacy, /DSGVO-zertifiziert|Ende-zu-Ende verschlüsselt/);
assert.match(privacy, /getJCRSession\('legalDocuments'\)/);
assert.match(privacy, /meine-website:components\/legalDocument/);
assert.match(privacy, /privacyDocument\.setProperty\('body', privacyHtml\.trim\(\)\)/);
assert.match(privacy, /publish\(commands, 'legalDocuments', privacyDocument\.getPath\(\)\)/);
assert.match(privacy, /publish\(commands, 'website', privacyPage\.getPath\(\)\)/);
assert.match(privacy, /publish\(commands, 'website', contactPage\.getPath\(\)\)/);
assert.doesNotMatch(privacy, /meine-website:components\/text|textComponent\.setProperty/);
assert.match(historicalBuild, /throw new UnsupportedOperationException[\s\S]+historische Aufbau-Skript/);
console.log('Workspace-Datenschutzquellencheck bestanden.');
