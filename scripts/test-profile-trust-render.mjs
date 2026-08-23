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

    for (const path of ['/', '/ueber-mich']) {
      const response = await page.goto(`https://cleonhardt.de${path}`, { waitUntil: 'networkidle', timeout: 30_000 });
      const profileImage = page.locator('img[src*="christian-leonhardt-portrait-v1"]');
      await profileImage.scrollIntoViewIfNeeded();
      await page.waitForFunction(() => {
        const image = document.querySelector('img[src*="christian-leonhardt-portrait-v1"]');
        return Boolean(image?.complete && image?.naturalWidth > 0);
      });
      const state = await page.evaluate(() => {
        const image = document.querySelector('img[src*="christian-leonhardt-portrait-v1"]');
        return {
          heading: document.querySelector('h1')?.textContent?.trim() || '',
          imageLoaded: Boolean(image?.complete && image?.naturalWidth > 0),
          imageAlt: image?.getAttribute('alt') || '',
          viewportWidth: document.documentElement.clientWidth,
          contentWidth: document.documentElement.scrollWidth,
        };
      });
      if (response?.status() !== 200) throw new Error(`${viewport.name} ${path}: HTTP ${response?.status()}`);
      if (!state.heading || !state.imageLoaded) throw new Error(`${viewport.name} ${path}: Überschrift oder Profilbild fehlt.`);
      if (state.imageAlt !== 'Portrait von Christian Leonhardt') throw new Error(`${viewport.name} ${path}: Bildalternative fehlt.`);
      if (state.contentWidth > state.viewportWidth + 1) {
        throw new Error(`${viewport.name} ${path}: horizontaler Überlauf ${state.contentWidth}/${state.viewportWidth}.`);
      }
      if (errors.length) throw new Error(`${viewport.name} ${path}: ${errors.join(' | ')}`);
      results.push({ viewport: viewport.name, path });
    }
    await page.close();
  }
} finally {
  await browser.close();
}

console.log(JSON.stringify({ checked: results.length }, null, 2));
