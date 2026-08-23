import { chromium } from 'file:///home/cd/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright/index.mjs';

const browser = await chromium.launch({ headless: true });
const results = [];

try {
  for (const viewport of [
    { name: 'desktop', width: 1440, height: 900 },
    { name: 'mobile', width: 390, height: 844 },
  ]) {
    const page = await browser.newPage({ viewport, isMobile: viewport.name === 'mobile' });
    const errors = [];
    page.on('console', (message) => {
      if (message.type() === 'error') errors.push(message.text());
    });
    page.on('pageerror', (error) => errors.push(error.message));

    for (const path of ['/praxisfaelle', '/chos-selbstcheck', '/chos']) {
      const response = await page.goto(`https://cleonhardt.de${path}`, { waitUntil: 'networkidle', timeout: 30_000 });
      const layout = await page.evaluate(() => ({
        heading: document.querySelector('h1')?.textContent?.trim() ?? '',
        viewportWidth: document.documentElement.clientWidth,
        contentWidth: document.documentElement.scrollWidth,
      }));
      if (response?.status() !== 200) throw new Error(`${viewport.name} ${path}: HTTP ${response?.status()}`);
      if (!layout.heading) throw new Error(`${viewport.name} ${path}: H1 fehlt.`);
      if (layout.contentWidth > layout.viewportWidth + 1) {
        throw new Error(`${viewport.name} ${path}: horizontaler Überlauf ${layout.contentWidth}/${layout.viewportWidth}.`);
      }

      if (path === '/chos-selbstcheck') {
        const questions = page.locator('[data-question]');
        if (await questions.count() !== 20) throw new Error(`${viewport.name}: Selbstcheck hat nicht 20 Fragen.`);
        for (let index = 0; index < 20; index += 1) {
          await questions.nth(index).locator('input[value="3"]').check({ force: true });
        }
        await page.locator('[data-chos-form] button[type="submit"]').click();
        const result = page.locator('[data-chos-result]');
        if (!(await result.isVisible())) throw new Error(`${viewport.name}: Auswertung bleibt verborgen.`);
        if (!(await result.textContent()).includes('12 von 12')) throw new Error(`${viewport.name}: Auswertung ist nicht korrekt.`);
        const cta = await page.locator('[data-chos-result-cta]').getAttribute('href');
        if (!cta?.includes('utm_source=chos-selbstcheck') || !cta.includes('utm_medium=website-tool')) {
          throw new Error(`${viewport.name}: CTA-Herkunft fehlt.`);
        }
        const ctaColors = await page.locator('[data-chos-result-cta]').evaluate((element) => {
          const style = window.getComputedStyle(element);
          return { color: style.color, background: style.backgroundColor };
        });
        if (ctaColors.color !== 'rgb(255, 255, 255)' || ctaColors.background === 'rgba(0, 0, 0, 0)') {
          throw new Error(`${viewport.name}: Ergebnis-CTA hat zu wenig Kontrast (${ctaColors.color} auf ${ctaColors.background}).`);
        }
      }

      if (errors.length) throw new Error(`${viewport.name} ${path}: ${errors.join(' | ')}`);
      results.push({ viewport: viewport.name, path });
    }
    await page.close();
  }
} finally {
  await browser.close();
}

console.log(JSON.stringify({ checked: results.length, selfCheckCompleted: 2 }, null, 2));
