const baseUrl = process.env.MAGNOLIA_PUBLIC_URL ?? 'http://127.0.0.1:8081/magnoliaPublic';
const contactUrl = `${baseUrl}/kontakt`;

function hiddenValue(html, name) {
  const pattern = new RegExp(`name="${name}" value="([^"]*)"`);
  const value = html.match(pattern)?.[1];
  if (!value) throw new Error(`Formularfeld ${name} fehlt.`);
  return value;
}

function cookieHeader(headers) {
  return headers.getSetCookie().map((cookie) => cookie.split(';', 1)[0]).join('; ');
}

const formResponse = await fetch(contactUrl);
if (formResponse.status !== 200) throw new Error(`Formularseite: HTTP ${formResponse.status}.`);

const formHtml = await formResponse.text();
const csrf = hiddenValue(formHtml, 'csrf');
const executionUuid = hiddenValue(formHtml, 'mgnlModelExecutionUUID');
let cookies = cookieHeader(formResponse.headers);

const payload = new URLSearchParams();
for (const [name, value] of Object.entries({
  mgnlModelExecutionUUID: executionUuid,
  field: '',
  csrf,
  anliegen: 'clarity-session',
  name: 'MVP automatisierter Funktionstest',
  email: 'mvp-test@example.invalid',
  rolle: 'Automatisierter Test',
  unternehmen: 'Lokale Magnolia-Testinstanz',
  situation: 'Technischer Funktionstest. Keine echte Anfrage.',
  ziel: 'Erfolgreiche lokale Verarbeitung.',
  datenschutz: 'akzeptiert',
  'website-url': '',
})) {
  payload.append(name, value);
}

const submitResponse = await fetch(contactUrl, {
  method: 'POST',
  body: payload,
  headers: { Cookie: cookies },
  redirect: 'manual',
});

if (submitResponse.status !== 302) {
  throw new Error(`Formularversand: HTTP ${submitResponse.status} statt 302.`);
}

cookies = [cookies, cookieHeader(submitResponse.headers)].filter(Boolean).join('; ');
const resultUrl = new URL(submitResponse.headers.get('location'), contactUrl);
const resultResponse = await fetch(resultUrl, { headers: { Cookie: cookies } });
const resultHtml = await resultResponse.text();

for (const expected of ['<div class="text success" role="status">', '<h2>Danke für deine Anfrage</h2>', 'Deine Angaben wurden übermittelt.']) {
  if (!resultHtml.includes(expected)) {
    throw new Error(`Erfolgsseite: ${expected} fehlt.`);
  }
}

if ((resultHtml.match(/<h1>/g) ?? []).length !== 1) {
  throw new Error('Die Erfolgsseite muss genau eine Hauptüberschrift enthalten.');
}

console.log('Formulartest erfolgreich: anonyme POST-Anfrage validiert, verarbeitet und bestätigt.');
