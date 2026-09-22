import assert from 'node:assert/strict';
import { existsSync, readFileSync } from 'node:fs';
import { join } from 'node:path';

const root = process.cwd();
const portfolioRoot = join(root, 'deploy/static/portfolio');
const pages = [
  'index.html',
  'projekte/index.html',
  'ueber-mich/index.html',
  'kontakt/index.html',
  'impressum/index.html',
  'datenschutz/index.html',
  '404.html',
];

const assets = [
  'assets/site.css',
  'assets/site.js',
  'assets/signet.svg',
  'assets/christian-leonhardt.webp',
  'assets/og-portfolio.png',
  'robots.txt',
  'sitemap.xml',
  'llms.txt',
  '4eba2fbccd4fbe055b49e6d9d41c4e00.txt',
];

for (const relativePath of [...pages, ...assets]) {
  assert.ok(existsSync(join(portfolioRoot, relativePath)), `Fehlende Portfolio-Datei: ${relativePath}`);
}

const home = readFileSync(join(portfolioRoot, 'index.html'), 'utf8');
assert.match(home, /Christian Leonhardt — Portfolio/);
assert.match(home, /Ich gestalte Systeme, die/);
assert.match(home, /href="\/workspace\/"/);
assert.match(home, /assets\/og-portfolio\.png/);
assert.doesNotMatch(home, /Angebot anfragen|Leistungen buchen|Kennenlerntermin/);
assert.match(home, /ALDI Nord/);
assert.match(home, /Peek &amp; Cloppenburg/);

const projects = readFileSync(join(portfolioRoot, 'projekte/index.html'), 'utf8');
const careerSection = projects.match(/<section class="section section--ink" id="berufliche-projekte">([\s\S]*?)<\/section>/)?.[1];
assert.ok(careerSection, 'Der Bereich mit beruflichen Projekten fehlt.');
for (const projectName of ['ALDI Nord', 'Peek &amp; Cloppenburg', 'SUNZINET', 'KGSt Kommunect', 'Bundeswehr']) {
  assert.match(careerSection, new RegExp(projectName.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')));
}

const careerNumbers = careerSection.match(/\b\d+(?:[.,]\d+)?\b/g) ?? [];
assert.deepEqual(
  [...new Set(careerNumbers)].sort(),
  ['10', '12', '14', '6'],
  'Im CV-Projektbereich dürfen ausschließlich freigegebene Teamgrößen als Zahlen erscheinen.',
);
assert.doesNotMatch(careerSection, /(?:€|%|Mio\.?|Million|Milliard|Umsatz|Budget|Einspar|Nutzer|User|Länder|Wochen|Monate|Jahre)/i);

const caddyfile = readFileSync(join(root, 'deploy/Caddyfile'), 'utf8');
assert.match(caddyfile, /@workspace_portal path \/workspace \/workspace\/\* \/beta \/beta\/\*/);
assert.match(caddyfile, /root \* \/srv\/cleonhardt-static\/portfolio/);
assert.match(caddyfile, /@portfolio_routes path \/ \/projekte/);
assert.match(caddyfile, /@legacy_consulting path/);

const sitemap = readFileSync(join(portfolioRoot, 'sitemap.xml'), 'utf8');
for (const route of ['/', '/projekte', '/ueber-mich', '/kontakt', '/impressum', '/datenschutz']) {
  assert.match(sitemap, new RegExp(`<loc>https://cleonhardt\\.de${route === '/' ? '/' : route}</loc>`));
}
assert.doesNotMatch(sitemap, /leistungen|angebot-anfragen|probleme/);

console.log('Portfolio-Quellen sind vollständig und der ChOS-Workspace bleibt geroutet.');
