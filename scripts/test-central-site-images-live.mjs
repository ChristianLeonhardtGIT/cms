import assert from 'node:assert/strict';

const baseUrl = process.env.BASE_URL || 'https://cleonhardt.de';

for (const path of ['/', '/ueber-mich']) {
  const response = await fetch(`${baseUrl}${path}`);
  assert.equal(response.status, 200, `${path} must return 200`);
  const html = await response.text();
  assert.match(html, /class="profile-trust(?: profile-trust--about)?"/, `${path} needs the profile component`);
  assert.match(html, /class="profile-trust__image"/, `${path} needs the central portrait image`);
  assert.match(html, /alt="Portrait von Christian Leonhardt"/, `${path} needs portrait alt text`);
  assert.doesNotMatch(html, /\d+[.,]?\d*\s*€\s*netto/i, `${path} must not expose prices`);
}

const home = await (await fetch(`${baseUrl}/`)).text();
assert.match(home, /Product Leadership, Organisation und Transformation/);
assert.match(home, /Mehr über meinen Hintergrund/);
const jsonLd = JSON.parse(home.match(/<script type="application\/ld\+json">([\s\S]*?)<\/script>/)[1]);
const person = jsonLd['@graph'].find((item) => item['@type'] === 'Person');
const organization = jsonLd['@graph'].find((item) => item['@type'] === 'Organization');
assert.ok(person?.image, 'Person schema needs the central portrait');
assert.ok(organization?.logo?.url, 'Organization schema needs the central logo');
assert.doesNotMatch(organization.logo.url, /social-card/);

console.log('Zentrale Bilder live: Startseite, Profilseite und Schema erfolgreich geprüft.');
