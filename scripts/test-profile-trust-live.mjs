const baseUrl = process.env.SITE_URL ?? 'https://cleonhardt.de';

const homeResponse = await fetch(`${baseUrl}/`);
const home = await homeResponse.text();
if (homeResponse.status !== 200) throw new Error(`Startseite: HTTP ${homeResponse.status}`);
for (const snippet of [
  'class="profile-trust"',
  'christian-leonhardt-portrait-v1.webp',
  'alt="Portrait von Christian Leonhardt"',
  'Product Leadership, Organisation und Transformation',
  'Loyalty &amp; CRM',
  'Organisationsdiagnose',
  'href="/clarity-session"',
  '>Clarity Session ansehen<',
  'href="/chos-selbstcheck"',
  'href="/praxisfaelle"',
]) {
  if (!home.includes(snippet)) throw new Error(`Startseite: Inhalt fehlt: ${snippet}`);
}

const aboutResponse = await fetch(`${baseUrl}/ueber-mich`);
const about = await aboutResponse.text();
if (aboutResponse.status !== 200) throw new Error(`Über mich: HTTP ${aboutResponse.status}`);
for (const snippet of [
  'profile-trust profile-trust--about',
  'christian-leonhardt-portrait-v1.jpg',
  'Product Leadership, Organisationsdiagnose und Transformation',
  'Diagnose vor Eingriff. Klarheit vor Aktion.',
]) {
  if (!about.includes(snippet)) throw new Error(`Über mich: Inhalt fehlt: ${snippet}`);
}

const jsonLdMatch = home.match(/<script type="application\/ld\+json">([\s\S]*?)<\/script>/);
if (!jsonLdMatch) throw new Error('Startseite: JSON-LD fehlt.');
const jsonLd = JSON.parse(jsonLdMatch[1]);
const person = jsonLd['@graph'].find((item) => item['@type'] === 'Person');
if (!person?.image?.includes('christian-leonhardt-portrait-v1.jpg')) {
  throw new Error('Startseite: Profilbild fehlt in den Person-Daten.');
}

for (const asset of ['christian-leonhardt-portrait-v1.webp', 'christian-leonhardt-portrait-v1.jpg']) {
  const response = await fetch(`${baseUrl}/.resources/meine-website/webresources/images/${asset}?v=20260731-1`);
  if (response.status !== 200) throw new Error(`${asset}: HTTP ${response.status}`);
  const size = Number(response.headers.get('content-length') || 0);
  if (size > 150_000) throw new Error(`${asset}: zu groß (${size} Bytes).`);
}

console.log(JSON.stringify({ pages: 2, assets: 2, navigationLinks: 2 }, null, 2));
