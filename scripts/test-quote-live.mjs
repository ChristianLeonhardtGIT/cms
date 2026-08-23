import { chromium } from 'file:///home/cd/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright/index.mjs';

const targetUrl = 'https://cleonhardt.de/angebot-anfragen?leistung=workshop';
const viewports = [
  { name: 'desktop', width: 1440, height: 900 },
  { name: 'mobile', width: 390, height: 844 },
];

const browser = await chromium.launch({ headless: true });
const results = [];
let capturedHubSpotPayload = null;

try {
  for (const viewport of viewports) {
    const page = await browser.newPage({
      viewport: { width: viewport.width, height: viewport.height },
      deviceScaleFactor: viewport.name === 'mobile' ? 2 : 1,
      isMobile: viewport.name === 'mobile',
      hasTouch: viewport.name === 'mobile',
    });

    const consoleErrors = [];
    page.on('console', (message) => {
      if (message.type() === 'error') {
        consoleErrors.push(message.text());
      }
    });
    page.on('pageerror', (error) => consoleErrors.push(error.message));

    const response = await page.goto(targetUrl, {
      waitUntil: 'networkidle',
      timeout: 30_000,
    });

    const state = await page.evaluate(() => {
      const form = document.querySelector('form#angebot-anfragen');
      const service = form?.querySelector('select[name="leistung"]');
      const heading = document.querySelector('h1')?.textContent?.trim();
      const submit = form?.querySelector('button[type="submit"], input[type="submit"]');

      return {
        heading,
        formFound: Boolean(form),
        selectedService: service?.value ?? null,
        submitFound: Boolean(submit),
        pageWidth: document.documentElement.clientWidth,
        contentWidth: document.documentElement.scrollWidth,
        horizontalOverflow:
          document.documentElement.scrollWidth > document.documentElement.clientWidth + 1,
      };
    });

    await page.screenshot({
      path: `/tmp/angebot-anfragen-${viewport.name}.png`,
      fullPage: true,
    });

    results.push({
      viewport: viewport.name,
      status: response?.status() ?? null,
      ...state,
      consoleErrors,
    });

    await page.close();
  }

  const integrationPage = await browser.newPage({
    viewport: { width: 1280, height: 900 },
  });
  await integrationPage.route('https://api.hsforms.com/**', async (route) => {
    capturedHubSpotPayload = route.request().postDataJSON();
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: '{}',
    });
  });
  await integrationPage.goto(
    'https://cleonhardt.de/angebot-anfragen?leistung=decision-review&utm_source=chatgpt.com&utm_medium=referral',
    { waitUntil: 'networkidle', timeout: 30_000 },
  );

  await integrationPage.locator('input[name="name"]').fill('Test Anfrage');
  await integrationPage.locator('input[name="email"]').fill('angebotstest@example.invalid');
  await integrationPage.locator('input[name="telefon"]').fill('+49 000 000000');
  await integrationPage.locator('input[name="unternehmen"]').fill('Test Organisation');
  await integrationPage.locator('input[name="rolle"]').fill('Test Rolle');
  await integrationPage
    .locator('textarea[name="situation"]')
    .fill('Technischer Test der Angebotsanfrage ohne Übertragung an HubSpot.');
  await integrationPage
    .locator('select[name="zeitraum"]')
    .selectOption('2-4-wochen');
  await integrationPage.locator('input[name="beteiligte"]').fill('3');
  await integrationPage.locator('input[name="datenschutz"]').check();
  await integrationPage.locator('button[type="submit"], input[type="submit"]').click();
  await integrationPage.getByRole('status').waitFor({ state: 'visible' });
  await integrationPage.close();
} finally {
  await browser.close();
}

for (const result of results) {
  if (result.status !== 200) {
    throw new Error(`${result.viewport}: HTTP ${result.status}`);
  }
  if (!result.formFound || !result.submitFound) {
    throw new Error(`${result.viewport}: Angebotsformular ist unvollständig.`);
  }
  if (result.selectedService !== 'workshop') {
    throw new Error(
      `${result.viewport}: Workshop wurde nicht vorausgewählt (${result.selectedService}).`,
    );
  }
  if (result.horizontalOverflow) {
    throw new Error(
      `${result.viewport}: horizontaler Überlauf (${result.contentWidth}/${result.pageWidth}).`,
    );
  }
  if (result.consoleErrors.length > 0) {
    throw new Error(`${result.viewport}: Konsolenfehler: ${result.consoleErrors.join(' | ')}`);
  }
}

if (!capturedHubSpotPayload) {
  throw new Error('Die HubSpot-Übergabe wurde nicht ausgelöst.');
}

const capturedFields = Object.fromEntries(
  capturedHubSpotPayload.fields.map((field) => [field.name, field.value]),
);

for (const fieldName of [
  'firstname',
  'lastname',
  'email',
  'phone',
  'company',
  'jobtitle',
  'hs_lead_status',
  'websiteanfrage',
]) {
  if (!capturedFields[fieldName]) {
    throw new Error(`HubSpot-Feld fehlt: ${fieldName}`);
  }
}

if (
  !capturedFields.websiteanfrage.includes('Angebotsanfrage: Decision Review') ||
  !capturedFields.websiteanfrage.includes(
    'Gewünschter Zeitraum: Innerhalb der nächsten 2 bis 4 Wochen',
  ) ||
  !capturedFields.websiteanfrage.includes('Voraussichtlich Beteiligte: 3')
  || !capturedFields.websiteanfrage.includes('Quelle: chatgpt.com')
  || !capturedFields.websiteanfrage.includes('Medium: referral')
  || !capturedFields.websiteanfrage.includes('Anfrageseite: /angebot-anfragen')
) {
  throw new Error('Die strukturierte Angebotsanfrage ist im HubSpot-Payload unvollständig.');
}

console.log(
  JSON.stringify(
    {
      renderTests: results,
      hubSpotMapping: {
        intercepted: true,
        transmitted: false,
        fields: Object.keys(capturedFields),
        offer: 'Decision Review',
      },
    },
    null,
    2,
  ),
);
