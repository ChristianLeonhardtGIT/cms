import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const contentType = await readFile('light-modules/meine-website/contentTypes/siteSettings.yaml', 'utf8');
const pageTemplate = await readFile('light-modules/meine-website/templates/pages/home.ftl', 'utf8');
const profileTemplate = await readFile('light-modules/meine-website/templates/components/profileTrust.ftl', 'utf8');
const profileDefinition = await readFile('light-modules/meine-website/templates/components/profileTrust.yaml', 'utf8');
const implementation = await readFile('scripts/implement-central-site-images.groovy', 'utf8');

for (const field of ['logo', 'portraitImage', 'portraitImageAlt', 'socialImage', 'socialImageAlt']) {
  assert.match(contentType, new RegExp(`name: ${field}`));
}
assert.match(pageTemplate, /globalSiteSettingsConfig/);
assert.match(pageTemplate, /personImageUrl/);
assert.match(pageTemplate, /globalSiteSettingsConfig\.logo/);
assert.match(profileTemplate, /settings\.portraitImage/);
assert.match(profileTemplate, /christian-leonhardt-portrait-v1\.webp/);
assert.match(profileDefinition, /profileTrust\.ftl/);
assert.match(implementation, /meine-website:components\/profileTrust/);
assert.match(implementation, /'\/start'/);
assert.match(implementation, /'\/ueber-mich'/);

console.log('Zentrale Bildpflege: Strukturtests erfolgreich.');
