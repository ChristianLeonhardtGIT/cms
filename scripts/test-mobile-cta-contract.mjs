import assert from 'node:assert/strict';
import { chromium } from 'file:///home/cd/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright/index.mjs';

const baseUrl = (process.env.E2E_BASE_URL || process.env.BASE_URL || 'https://cleonhardt.de').replace(/\/$/, '');
const paths = ['/', '/leistungen', '/clarity-session', '/decision-review', '/kontakt', '/chos-selbstcheck'];
const selector = 'a.button, button.button, .mvp-panel__link, .mvp-offer-cta, .form input[type="submit"], .form button[type="submit"]';

const browser = await chromium.launch({ headless: true });
const page = await browser.newPage({ viewport: { width: 390, height: 844 } });
const results = [];

try {
  for (const path of paths) {
    const response = await page.goto(`${baseUrl}${path}`, { waitUntil: 'domcontentloaded', timeout: 30_000 });
    assert.equal(response?.status(), 200, `${path}: HTTP ${response?.status() ?? 'unbekannt'}`);
    await page.locator('main#inhalt').waitFor({ state: 'visible' });

    const state = await page.evaluate((ctaSelector) => ({
      overflow: document.documentElement.scrollWidth > document.documentElement.clientWidth + 1,
      ctas: Array.from(document.querySelectorAll(ctaSelector))
        .filter((cta) => {
          const style = getComputedStyle(cta);
          const rect = cta.getBoundingClientRect();
          return style.display !== 'none' && style.visibility !== 'hidden' && rect.width > 0 && rect.height > 0;
        })
        .map((cta) => {
          const rect = cta.getBoundingClientRect();
          return {
            text: cta.textContent?.replace(/\s+/g, ' ').trim() ?? '',
            width: Math.round(rect.width),
            height: Math.round(rect.height),
            fontSize: Number.parseFloat(getComputedStyle(cta).fontSize),
          };
        }),
    }), selector);

    assert.equal(state.overflow, false, `${path}: horizontaler Überlauf durch CTA`);
    assert.ok(state.ctas.length > 0, `${path}: keine sichtbare CTA gefunden`);
    for (const cta of state.ctas) {
      assert.ok(cta.width <= 320, `${path}: CTA „${cta.text}“ ist mobil zu breit (${cta.width}px)`);
      assert.ok(cta.height >= 44, `${path}: CTA „${cta.text}“ unterschreitet die Touch-Zielhöhe (${cta.height}px)`);
      assert.ok(cta.height <= 64, `${path}: CTA „${cta.text}“ ist mobil zu hoch (${cta.height}px)`);
      assert.ok(cta.fontSize <= 16, `${path}: CTA „${cta.text}“ verwendet eine zu große Schrift (${cta.fontSize}px)`);
    }
    results.push({ path, ctas: state.ctas.length, status: 'ok' });
  }

  await page.goto(`${baseUrl}/`, { waitUntil: 'domcontentloaded', timeout: 30_000 });
  const trigger = page.getByRole('button', { name: 'Hauptmenü öffnen' });
  assert.equal(await trigger.count(), 1, 'Mobiler Menü-Trigger fehlt');
  await trigger.click();
  const legal = await page.evaluate(() => Array.from(document.querySelectorAll('.site-nav__legal a')).map((link) => ({
    text: link.textContent?.trim() ?? '',
    href: link.getAttribute('href') ?? '',
    visible: link.getClientRects().length > 0,
    fontSize: Number.parseFloat(getComputedStyle(link).fontSize),
  })));
  assert.deepEqual(legal.map(({ text, href }) => [text, href]), [['Impressum', '/impressum'], ['Datenschutz', '/datenschutz']], 'Rechtliche Links fehlen im mobilen Menü');
  assert.equal(legal.every((link) => link.visible && link.fontSize <= 14), true, 'Rechtliche Links sind nicht dezent und sichtbar');
} finally {
  await browser.close();
}

console.log(JSON.stringify({ baseUrl, viewport: '390x844', results, result: 'Globale mobile CTA-Regel und Menü-Rechtslinks bestanden' }, null, 2));
