import assert from 'node:assert/strict';
import { execFileSync } from 'node:child_process';
import { createHash } from 'node:crypto';
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
  'assets/christian-leonhardt-executive-profile.pdf',
  'robots.txt',
  'sitemap.xml',
  'llms.txt',
  '4eba2fbccd4fbe055b49e6d9d41c4e00.txt',
];

for (const relativePath of [...pages, ...assets]) {
  assert.ok(existsSync(join(portfolioRoot, relativePath)), `Fehlende Portfolio-Datei: ${relativePath}`);
}

const home = readFileSync(join(portfolioRoot, 'index.html'), 'utf8');
assert.match(home, /Christian Leonhardt — Product Leadership/);
assert.match(home, /Ich führe Produktteams/);
assert.match(home, /Mein nächster Schritt: Head of Product/);
assert.match(home, /class="proof-strip"/);
assert.doesNotMatch(home, /class="ticker"/);
assert.match(home, /People Leadership/);
assert.match(home, /href="\/workspace\/"/);
assert.match(home, /assets\/og-portfolio\.png/);
assert.doesNotMatch(home, /Angebot anfragen|Leistungen buchen|Kennenlerntermin/);
assert.match(home, /ALDI Nord/);
assert.match(home, /Peek &amp; Cloppenburg/);
for (const technology of ['Native App-Entwicklung', 'Backend for Frontend', 'Microsoft Azure', 'Kubernetes', 'Redis Caching', 'Salesforce', 'SAP CDP', 'GK Engage', 'Adjust', 'Headless React', 'Node.js', 'Kotlin', 'Google Cloud', 'OneTrust', 'Shopware Enterprise', 'TYPO3', 'PIM', 'Magnolia CMS']) {
  assert.match(home, new RegExp(technology.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')));
}
assert.match(home, /site\.css\?v=leadership-cases/);
assert.match(home, /de\.linkedin\.com\/in\/christian-leonhardt-b341687b/);
assert.match(home, /christian-leonhardt-executive-profile\.pdf/);
assert.doesNotMatch(home, /class="workspace-link"/);

const contact = readFileSync(join(portfolioRoot, 'kontakt/index.html'), 'utf8');
assert.match(contact, /feste Head-of-Product-Rolle/);
assert.doesNotMatch(contact, /Interim|Beratungsmandate|Head-of-Product-Mandate/);
assert.match(contact, /LinkedIn/);
assert.match(contact, /Executive Profile/);

const projects = readFileSync(join(portfolioRoot, 'projekte/index.html'), 'utf8');
assert.doesNotMatch(projects, /Executive Interview OS|anspruchsvolle Interviewprozesse/);
assert.doesNotMatch(readFileSync(join(portfolioRoot, 'llms.txt'), 'utf8'), /Executive Interview OS/);
const careerSection = projects.match(/<section class="section section--ink" id="berufliche-projekte">([\s\S]*?)<\/section>/)?.[1];
assert.ok(careerSection, 'Der Bereich mit beruflichen Projekten fehlt.');
for (const projectName of ['ALDI Nord', 'Peek &amp; Cloppenburg', 'SUNZINET', 'KGSt Kommunect', 'Bundeswehr']) {
  assert.match(careerSection, new RegExp(projectName.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')));
}
assert.equal((careerSection.match(/Technischer Kontext/g) ?? []).length, 4);
for (const technology of ['Liferay DXP', 'Elasticsearch', 'Magnolia CMS', 'Java', 'Jira', 'Confluence', 'Pardot', 'Algolia', 'GK AIR', 'Bloomreach']) {
  assert.match(careerSection, new RegExp(technology.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')));
}

const visibleCareerText = careerSection.replace(/<[^>]+>/g, ' ');
const careerNumbers = visibleCareerText.match(/\b\d+(?:[.,]\d+)?\b/g) ?? [];
assert.deepEqual(
  [...new Set(careerNumbers)].sort(),
  ['10', '12', '14', '6'],
  'Im CV-Projektbereich dürfen ausschließlich freigegebene Teamgrößen als Zahlen erscheinen.',
);
assert.doesNotMatch(careerSection, /(?:€|%|Mio\.?|Million|Milliard|Umsatz|Budget|Einspar|Nutzer|User|Länder|Wochen|Monate|Jahre)/i);

for (const caseId of ['aldi', 'fashion-id', 'sunzinet', 'rewe-group']) {
  const article = projects.match(new RegExp(`<article[^>]+id="${caseId}"[^>]*>([\\s\\S]*?)</article>`))?.[1];
  assert.ok(article, `Führungscase fehlt: ${caseId}`);
  for (const heading of ['Ausgangslage', 'Mein Mandat', 'Meine Veränderung', 'Ergebnis']) {
    assert.ok(article.includes(`<dt>${heading}</dt>`), `Case-Struktur fehlt: ${caseId} / ${heading}`);
  }
}
assert.match(projects, /Auswahlkontext bezeichnet vorbereitete Systementscheidungen/);
for (const html of [home, projects]) {
  assert.match(html, /Plattform-Relaunch &amp; New Retail/);
  assert.doesNotMatch(html, /Kampagnen-Inhaltsmodul/);
}
const pcCase = projects.match(/<article[^>]+id="fashion-id"[^>]*>([\s\S]*?)<\/article>/)?.[1];
assert.match(pcCase, /Den kompletten Relaunch der E-Commerce-Plattform umsetzen/);
assert.match(pcCase, /Welcome-Domäne/);
assert.match(pcCase, /PO-Community/);
assert.match(pcCase, /Quellen beschreiben das Unternehmensvorhaben, nicht meine persönliche Rolle/);
assert.match(pcCase, /https:\/\/at\.linkedin\.com\/posts\/pucat_/);
assert.match(pcCase, /https:\/\/www\.handelsblatt\.com\//);
assert.match(readFileSync(join(portfolioRoot, 'ueber-mich/index.html'), 'utf8'), /kompletten Relaunch der E-Commerce-Plattform/);
assert.match(home, /ohne klassische Linienverantwortung/);
const featuredWork = home.match(/<div class="project-list">([\s\S]*?)<div class="actions">/)?.[1];
assert.ok(featuredWork);
assert.doesNotMatch(featuredWork, /ChOS/);
assert.match(home, /aktive Entwicklungs- und Validierungsarbeit/);
assert.match(projects, /keine bereits bewiesene Standardmethode/);

// Confidentiality applies to every public surface, not just the career cases.
// Decode entities so amounts cannot bypass the check through HTML encoding.
function visibleText(html) {
  return html.replace(/<(script|style)\b[^>]*>[\s\S]*?<\/\1>/gi, '')
    .replace(/<[^>]+>/g, ' ')
    .replace(/&#(x[\da-f]+|\d+);/gi, (_, code) => String.fromCodePoint(code[0].toLowerCase() === 'x' ? parseInt(code.slice(1), 16) : Number(code)))
    .replace(/&euro;/gi, '€')
    .replace(/&percnt;/gi, '%')
    .replace(/&nbsp;/gi, ' ');
}
const confidentialMetrics = /[€$£%]|\b(?:EUR|Euro|USD|GBP|CHF|TEUR|Mio\.?|Million(?:en)?|Milliard(?:e|en)?|Prozent)\b|\b\d[\d.,]*\s*(?:Wochen|Monate|Jahre|Länder|Nutzer|Downloads)\b/i;
for (const relativePath of pages) {
  const html = readFileSync(join(portfolioRoot, relativePath), 'utf8');
  assert.ok(!confidentialMetrics.test(visibleText(html)), `Vertrauliche Kennzahl auf ${relativePath}`);
  const metadata = [...html.matchAll(/<meta\b[^>]*content="([^"]*)"/g)].map(match => match[1]).join(' ');
  assert.ok(!confidentialMetrics.test(metadata), `Vertrauliche Kennzahl in Metadaten: ${relativePath}`);
  assert.match(html, /site\.css\?v=leadership-cases/);
}
for (const relativePath of ['index.html', 'ueber-mich/index.html', 'projekte/index.html']) {
  const html = readFileSync(join(portfolioRoot, relativePath), 'utf8');
  const main = html.match(/<main\b[^>]*>([\s\S]*?)<\/main>/)?.[1];
  assert.ok(main);
  const numbers = visibleText(main).match(/\b\d+(?:[.,]\d+)?\b/g) ?? [];
  assert.ok(numbers.every(value => ['10', '12', '14', '6'].includes(value)), `Nur Teamgrößen erlaubt: ${relativePath}`);
}
const llms = readFileSync(join(portfolioRoot, 'llms.txt'), 'utf8');
assert.ok(!confidentialMetrics.test(llms), 'Vertrauliche Kennzahl in llms.txt');
const pdfPath = join(portfolioRoot, 'assets/christian-leonhardt-executive-profile.pdf');
const pdfText = execFileSync('pdftotext', [pdfPath, '-'], { encoding: 'utf8' });
assert.ok(!confidentialMetrics.test(pdfText), 'Vertrauliche Kennzahl im öffentlichen Executive Profile');
assert.ok((pdfText.match(/\b\d+(?:[.,]\d+)?\b/g) ?? []).every(value => ['12', '14', '6'].includes(value)));
assert.match(pdfText, /Head of Product/);
assert.match(pdfText, /Plattform-Relaunch & New Retail/);
assert.match(pdfText, /kompletten Relaunch der E-Commerce-Plattform/);
assert.doesNotMatch(pdfText, /Kampagnen-Inhaltsmodul/);
assert.match(pdfText, /leonhardtchristian@icloud\.com/);
assert.doesNotMatch(pdfText, /kontakt@cleonhardt\.de/);
for (const employer of ['ALDI Nord', 'Fashion ID', 'SUNZINET', 'neusta webservices', 'Bundeswehr', 'Firma Habuzin']) {
  assert.ok(pdfText.includes(employer), `Station fehlt im Executive Profile: ${employer}`);
}
assert.deepEqual(readFileSync(pdfPath), readFileSync(join(root, 'output/pdf/christian-leonhardt-executive-profile.pdf')));
const profileVersion = createHash('sha256').update(readFileSync(pdfPath)).digest('hex').slice(0, 12);
for (const relativePath of [...pages, 'llms.txt']) {
  const source = readFileSync(join(portfolioRoot, relativePath), 'utf8');
  const links = source.match(/\/assets\/christian-leonhardt-executive-profile\.pdf(?:\?[^"\s)]+)?/g) ?? [];
  for (const link of links) assert.equal(link, `/assets/christian-leonhardt-executive-profile.pdf?v=${profileVersion}`, `Veralteter PDF-Link: ${relativePath}`);
}

// Validate internal links and case anchors, while leaving the protected portal alone.
for (const relativePath of pages) {
  const html = readFileSync(join(portfolioRoot, relativePath), 'utf8');
  for (const [, href] of html.matchAll(/href="([^"]+)"/g)) {
    if (!href.startsWith('/') && !href.startsWith('#')) continue;
    if (href.startsWith('/workspace/')) continue;
    const [pathname, anchor] = href.split('#');
    const cleanPath = pathname.split('?')[0];
    const file = cleanPath ? join(portfolioRoot, cleanPath.slice(1), cleanPath.endsWith('.pdf') || cleanPath.includes('/assets/') ? '' : 'index.html') : join(portfolioRoot, relativePath);
    assert.ok(existsSync(file), `Defekter Link in ${relativePath}: ${href}`);
    if (anchor) assert.ok(readFileSync(file, 'utf8').includes(`id="${anchor}"`), `Fehlender Anker: ${href}`);
  }
}

const caddyfile = readFileSync(join(root, 'deploy/Caddyfile'), 'utf8');
assert.match(caddyfile, /@workspace_portal path \/workspace \/workspace\/\* \/beta \/beta\/\*/);
assert.match(caddyfile, /root \* \/srv\/cleonhardt-static\/portfolio/);
assert.match(caddyfile, /@portfolio_routes path \/ \/projekte/);
assert.match(caddyfile, /@legacy_consulting path/);
const slashPattern = caddyfile.match(/@portfolio_slash path_regexp portfolio_slash ([^\n]+)/)?.[1];
assert.ok(slashPattern, 'Gezielte Normalisierung der Portfolio-URLs fehlt.');
const slashMatcher = new RegExp(slashPattern);
for (const route of ['/projekte', '/ueber-mich', '/kontakt', '/impressum', '/datenschutz']) {
  assert.equal(slashMatcher.exec(`${route}/`)?.[1], route.slice(1));
  assert.equal(slashMatcher.test(route), false);
}
for (const route of ['/', '/workspace/', '/workspace/health', '/beta/', '/assets/site.css', '/unbekannt/']) {
  assert.equal(slashMatcher.test(route), false, `Weiterleitung außerhalb des Portfolios: ${route}`);
}
assert.ok(caddyfile.includes('redir @portfolio_slash /{re.portfolio_slash.1}{?query} permanent'));
assert.ok(caddyfile.includes('header @executive_profile Cache-Control "no-cache"'));
assert.match(caddyfile, /@portfolio_assets\s*\{\s*path \/assets\/\*\s*not path \/assets\/christian-leonhardt-executive-profile\.pdf\s*\}/);

const sitemap = readFileSync(join(portfolioRoot, 'sitemap.xml'), 'utf8');
for (const route of ['/', '/projekte', '/ueber-mich', '/kontakt']) {
  assert.match(sitemap, new RegExp(`<loc>https://cleonhardt\\.de${route === '/' ? '/' : route}</loc>`));
}
assert.doesNotMatch(sitemap, /leistungen|angebot-anfragen|probleme|impressum|datenschutz/);
assert.equal((sitemap.match(/<loc>/g) ?? []).length, 4);
for (const [, location] of sitemap.matchAll(/<loc>([^<]+)<\/loc>/g)) {
  const pathname = new URL(location).pathname;
  const page = readFileSync(join(portfolioRoot, pathname.slice(1), 'index.html'), 'utf8');
  assert.doesNotMatch(page, /<meta[^>]+name="robots"[^>]+content="[^"]*noindex/i, `Nicht indexierbare Seite in Sitemap: ${location}`);
}

console.log('Portfolio-Quellen sind vollständig und der ChOS-Workspace bleibt geroutet.');
