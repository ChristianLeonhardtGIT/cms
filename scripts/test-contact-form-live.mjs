import { chromium } from 'file:///home/cd/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright/index.mjs';

const baseUrl = (process.env.SITE_URL ?? 'https://cleonhardt.de').replace(/\/$/, '');
const contactUrl = `${baseUrl}/kontakt`;
const browser = await chromium.launch({ headless: true });

try {
  const page = await browser.newPage({ viewport: { width: 1280, height: 900 } });
  const consoleErrors = [];
  page.on('console', (message) => {
    if (message.type() === 'error') consoleErrors.push(message.text());
  });
  page.on('pageerror', (error) => consoleErrors.push(error.message));

  const response = await page.goto(contactUrl, { waitUntil: 'networkidle', timeout: 30_000 });
  if (response?.status() !== 200) throw new Error(`Kontaktseite: HTTP ${response?.status()}.`);

  await page.locator('select[name="anliegen"]').selectOption('clarity-session');
  await page.locator('input[name="name"]').fill('Automatisierter Produktions-Funktionstest');
  await page.locator('input[name="email"]').fill('mvp-test@example.invalid');
  await page.locator('input[name="rolle"]').fill('Technischer Test');
  await page.locator('input[name="unternehmen"]').fill('Automatisierte Qualitätsprüfung');
  await page
    .locator('textarea[name="situation"]')
    .fill('Technischer Ende-zu-Ende-Test des Kontaktformulars. Keine echte Anfrage.');
  await page
    .locator('textarea[name="ziel"]')
    .fill('Erfolgreiche Verarbeitung und Anzeige der Bestätigungsseite.');
  await page.locator('input[name="datenschutz"]').check();

  const formState = await page.locator('form#situation-klaeren').evaluate((form) => ({
    valid: form.checkValidity(),
    invalidFields: [...form.elements]
      .filter((field) => typeof field.checkValidity === 'function' && !field.checkValidity())
      .map((field) => field.name),
  }));
  if (!formState.valid) {
    throw new Error(`Formularvalidierung fehlgeschlagen: ${formState.invalidFields.join(', ')}.`);
  }

  const submissionPromise = page.waitForResponse(
    (candidate) =>
      candidate.request().method() === 'POST' &&
      new URL(candidate.url()).hostname === 'api.hsforms.com' &&
      new URL(candidate.url()).pathname.startsWith('/submissions/v3/integration/submit/'),
    { timeout: 30_000 },
  );
  await page.locator('form#situation-klaeren').evaluate((form) => form.requestSubmit());
  const submissionResponse = await submissionPromise;
  if (submissionResponse.status() >= 400) {
    throw new Error(`Formularversand: HTTP ${submissionResponse.status()}.`);
  }
  await page.getByRole('status').waitFor({ state: 'visible', timeout: 30_000 });

  const html = await page.content();
  for (const expected of [
    'class="text success"',
    '<h2>Danke für deine Anfrage</h2>',
    'Deine Angaben wurden übermittelt.',
  ]) {
    if (!html.includes(expected)) throw new Error(`Erfolgsseite: ${expected} fehlt.`);
  }
  if ((html.match(/<h1\b/g) ?? []).length !== 1) {
    throw new Error('Die Erfolgsseite muss genau eine Hauptüberschrift enthalten.');
  }
  if (consoleErrors.length > 0) {
    throw new Error(`Konsolenfehler: ${consoleErrors.join(' | ')}`);
  }

  console.log(
    JSON.stringify(
      {
        page: '/kontakt',
        submitted: true,
        synthetic: true,
        successMessage: true,
        consoleErrors,
      },
      null,
      2,
    ),
  );
} finally {
  await browser.close();
}
