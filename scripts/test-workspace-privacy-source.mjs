import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const privacy = await readFile('scripts/update-privacy-policy.groovy', 'utf8');

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
console.log('Workspace-Datenschutzquellencheck bestanden.');
