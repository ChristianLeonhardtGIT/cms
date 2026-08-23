import { chromium } from 'file:///home/cd/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright/index.mjs';

const paths = [
  '/',
  '/chos',
  '/ueber-mich',
  '/leistungen',
  '/decision-review',
  '/executive-sparring',
  '/product-organisation-diagnostic',
  '/workshops',
  '/insights',
  '/insights/rollen-und-verantwortlichkeiten-in-produktorganisationen-klaeren',
  '/insights/wann-braucht-eine-produktorganisation-ein-operating-model',
  '/insights/annahmen-vor-einer-produktentscheidung-pruefen',
  '/insights/organisationsdiagnose-statt-standardberatung',
  '/insights/product-organisation-diagnostic-ablauf-und-ergebnis',
  '/insights/wann-ist-executive-sparring-sinnvoll',
];
const interactiveCardSelectors = new Map([
  ['/', '.related-insights__card'],
  ['/chos', '.mvp-panel--link'],
  ['/leistungen', '.mvp-panel:has(.mvp-offer-cta)'],
  ['/insights', '.card--linked'],
]);
const viewports = [
  { name: 'desktop', width: 1440, height: 900 },
  { name: 'mobile', width: 390, height: 844 },
];

const browser = await chromium.launch({ headless: true });
const results = [];
try {
  for (const viewport of viewports) {
    const page = await browser.newPage({
      viewport: { width: viewport.width, height: viewport.height },
      isMobile: viewport.name === 'mobile',
    });
    const errors = [];
    page.on('console', (message) => {
      if (message.type() === 'error') errors.push(message.text());
    });
    page.on('pageerror', (error) => errors.push(error.message));

    for (const path of paths) {
      const response = await page.goto(`https://cleonhardt.de${path}`, {
        waitUntil: 'networkidle',
        timeout: 30_000,
      });
      const state = await page.evaluate(() => ({
        heading: document.querySelector('h1')?.textContent?.trim() || '',
        width: document.documentElement.clientWidth,
        contentWidth: document.documentElement.scrollWidth,
      }));
      if (response?.status() !== 200) throw new Error(`${viewport.name} ${path}: HTTP ${response?.status()}`);
      if (!state.heading) throw new Error(`${viewport.name} ${path}: H1 fehlt.`);
      if (state.contentWidth > state.width + 1) {
        throw new Error(`${viewport.name} ${path}: horizontaler Überlauf ${state.contentWidth}/${state.width}.`);
      }
      if (path === '/leistungen') {
        const offerCtas = await page.locator('.mvp-grid--3 .mvp-offer-cta').evaluateAll((elements) =>
          elements.map((element) => {
            const rect = element.getBoundingClientRect();
            return {
              text: element.textContent?.trim() || '',
              width: Math.round(rect.width),
              height: Math.round(rect.height),
              whiteSpace: window.getComputedStyle(element).whiteSpace,
            };
          }),
        );
        if (offerCtas.length < 3) throw new Error(`${viewport.name}: Angebots-CTAs fehlen.`);
        const ctaHeights = new Set(offerCtas.map((cta) => cta.height));
        if (offerCtas.some((cta) => cta.whiteSpace !== 'nowrap') || ctaHeights.size !== 1) {
          throw new Error(`${viewport.name}: Angebots-CTA bricht um: ${JSON.stringify(offerCtas)}`);
        }
      }
      if (path === '/ueber-mich') {
        const accentPanel = page.locator('.mvp-panel--accent');
        if ((await accentPanel.count()) === 0) throw new Error(`${viewport.name}: Akzentbox fehlt.`);
        const borderColor = await accentPanel.nth(0).evaluate((element) => window.getComputedStyle(element).borderTopColor);
        if (borderColor === 'transparent' || borderColor === 'rgba(0, 0, 0, 0)') {
          throw new Error(`${viewport.name}: Rahmen der Akzentbox ist nicht sichtbar.`);
        }
      }
      if (viewport.name === 'desktop' && interactiveCardSelectors.has(path)) {
        const cards = page.locator(interactiveCardSelectors.get(path));
        const cardCount = await cards.count();
        if (cardCount === 0) throw new Error(`${path}: Interaktive Karte fehlt.`);
        const card = cards.nth(0);
        await card.hover();
        await page.waitForTimeout(240);
        const hoverState = await card.evaluate((element) => {
          const style = window.getComputedStyle(element);
          return { transform: style.transform, boxShadow: style.boxShadow };
        });
        if (hoverState.transform === 'none' || hoverState.boxShadow === 'none') {
          throw new Error(`${path}: Globaler Karten-Hover fehlt (${JSON.stringify(hoverState)}).`);
        }
      }
      if (errors.length) throw new Error(`${viewport.name} ${path}: ${errors.join(' | ')}`);
      results.push({ viewport: viewport.name, path, heading: state.heading });
    }
    await page.close();
  }
} finally {
  await browser.close();
}

console.log(JSON.stringify({ checked: results.length }, null, 2));
