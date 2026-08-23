import { readFile } from 'node:fs/promises';

const baseUrl = process.env.MAGNOLIA_PUBLIC_URL ?? 'http://127.0.0.1:8081/magnoliaPublic';
const base = new URL(baseUrl);
const publicPathPrefix = base.pathname.replace(/\/$/, '');
const publicPath = (path) => `${publicPathPrefix}/${path}`.replace(/\/{2,}/g, '/');
const usesCleanRoot = base.origin === 'https://cleonhardt.de' && publicPathPrefix === '';

const pages = [
  ['start', 'Klarheit für komplexe Produktorganisationen.'],
  ['leistungen', 'Der passende Rahmen für Ihre aktuelle Situation.'],
  ['clarity-session', 'Klarheit, bevor Sie die nächste Maßnahme starten.'],
  ['decision-review', 'Eine wichtige Entscheidung verdient mehr als Zustimmung.'],
  ['executive-sparring', 'Ein vertraulicher Denkraum für Entscheidungen mit Wirkung.'],
  ['product-organisation-diagnostic', 'Die Organisation verstehen, bevor Sie sie neu zeichnen.'],
  ['workshops', 'Ein Workshop ist dann wertvoll, wenn danach etwas entschieden ist.'],
  ['angebot-anfragen', 'Ein passender Rahmen beginnt mit einer klaren Anfrage.'],
  ['chos', 'Erst verstehen. Dann wirksam verändern.'],
  ['ueber-mich', 'Komplexität lässt sich nicht wegmoderieren.'],
  ['insights', 'Produktorganisationen besser verstehen.'],
  ['insights/warum-produktorganisationen-nicht-an-fehlenden-methoden-scheitern', 'Warum Produktorganisationen nicht an fehlenden Methoden scheitern'],
  ['insights/diagnose-vor-eingriff', 'Diagnose vor Eingriff: Ursachen von Symptomen unterscheiden'],
  ['insights/unklare-rollen-sind-selten-das-eigentliche-problem', 'Unklare Rollen sind selten das eigentliche Problem'],
  ['insights/rollen-und-verantwortlichkeiten-in-produktorganisationen-klaeren', 'Rollen und Verantwortlichkeiten in Produktorganisationen klären'],
  ['insights/wann-braucht-eine-produktorganisation-ein-operating-model', 'Wann braucht eine Produktorganisation ein Operating Model?'],
  ['insights/annahmen-vor-einer-produktentscheidung-pruefen', 'Annahmen vor einer Produktentscheidung prüfen'],
  ['insights/organisationsdiagnose-statt-standardberatung', 'Organisationsdiagnose statt Standardberatung'],
  ['insights/product-organisation-diagnostic-ablauf-und-ergebnis', 'Product Organisation Diagnostic: Ablauf und Ergebnis'],
  ['insights/wann-ist-executive-sparring-sinnvoll', 'Wann ist Executive Sparring sinnvoll?'],
  ['kontakt', 'Was soll klarer werden?'],
  ['impressum', 'Impressum'],
  ['datenschutz', 'Datenschutz'],
];

const documents = new Map();

for (const [pagePath, expectedHeading] of pages) {
  const requestPath = usesCleanRoot && pagePath === 'start' ? '' : pagePath;
  const response = await fetch(`${baseUrl}/${requestPath}`, { redirect: 'manual' });
  if (response.status !== 200) {
    throw new Error(`${pagePath}: HTTP ${response.status} statt 200.`);
  }

  const html = await response.text();
  const normalizedHtml = html.replaceAll('\u00AD', '');
  documents.set(pagePath, html);
  const canonicalPath = usesCleanRoot
    ? (pagePath === 'start' ? '/' : `/${pagePath}`)
    : publicPath(pagePath);
  const canonicalUrl = `https://cleonhardt.de${canonicalPath}`;

  for (const requiredSnippet of [
    '<header class="site-header">',
    '<main id="inhalt">',
    '<footer class="site-footer">',
    '© 2026 Christian Leonhardt',
    expectedHeading,
    `href="${publicPath('impressum')}"`,
    `href="${publicPath('datenschutz')}"`,
    `<link rel="canonical" href="${canonicalUrl}">`,
    `<meta property="og:url" content="${canonicalUrl}">`,
    '<meta property="og:title"',
    '<meta property="og:description"',
    '<meta property="og:image"',
    '<meta name="twitter:card" content="summary"',
    'class="site-brand__logo site-brand__logo--inline"',
    'class="page-transition"',
    'webresources/js/page-transitions.js',
    'webresources/js/attribution.js',
    'type="application/ld+json"',
  ]) {
    if (!normalizedHtml.includes(requiredSnippet)) {
      throw new Error(`${pagePath}: Erwarteter Inhalt fehlt: ${requiredSnippet}`);
    }
  }

  if (html.includes('&amp;amp;') || html.includes('HTTP Status') || html.includes('RenderingException')) {
    throw new Error(`${pagePath}: Rendering- oder Escaping-Fehler gefunden.`);
  }
}

if (usesCleanRoot) {
  const legacyStartResponse = await fetch(`${baseUrl}/start`, { redirect: 'manual' });
  if (
    legacyStartResponse.status !== 200 ||
    legacyStartResponse.headers.get('x-robots-tag') !== 'noindex, follow'
  ) {
    throw new Error('/start wird nicht als nicht indexierbare Bestands-URL ausgeliefert.');
  }
}

const home = documents.get('start');
for (const navLabel of ['Start', 'Leistungen', 'ChOS', 'Über mich', 'Insights', 'Kontakt']) {
  if (!home.includes(`>${navLabel}<`)) {
    throw new Error(`Navigation: ${navLabel} fehlt.`);
  }
}

if (!home.includes('site-nav__list--level-2') || !home.includes('aria-expanded="false"')) {
  throw new Error('Das Insights-Untermenü ist nicht dreistufig erweiterbar vorbereitet.');
}
for (const requiredMenuSnippet of [
  'class="site-nav-trigger__icon"',
  'aria-label="Hauptmenü öffnen"',
  'aria-controls="hauptnavigation"',
  'class="site-nav__footer"',
  'class="site-nav__footer-cta"',
  'class="site-nav__link-arrow" aria-hidden="true"',
  'site-nav__item--default-open',
]) {
  if (!home.includes(requiredMenuSnippet)) {
    throw new Error(`Navigation: Mobiler Menüschalter ist unvollständig: ${requiredMenuSnippet}`);
  }
}

const pageTemplate = await readFile(
  new URL('../light-modules/meine-website/templates/pages/home.ftl', import.meta.url),
  'utf8',
);
for (const requiredTemplateBehavior of [
  'navigationItem?index < maxItems',
  'navigationChildLimit = 5',
  'Zur Insights-Übersicht',
]) {
  if (!pageTemplate.includes(requiredTemplateBehavior)) {
    throw new Error(`Navigation: Insights-Begrenzung fehlt: ${requiredTemplateBehavior}`);
  }
}

const contact = documents.get('kontakt');
const claritySession = documents.get('clarity-session');
const quoteRequest = documents.get('angebot-anfragen');
const services = documents.get('leistungen');
if (home.includes('webresources/js/hubspot-form.js')) {
  throw new Error('Startseite: Das nicht benötigte HubSpot-Formularskript wird geladen.');
}
if (!contact.includes('webresources/js/hubspot-form.js')) {
  throw new Error('Kontaktseite: Das benötigte HubSpot-Formularskript fehlt.');
}
if (!claritySession.includes('webresources/js/hubspot-form.js')) {
  throw new Error('Clarity Session: Das benötigte HubSpot-Formularskript fehlt.');
}
if (!quoteRequest.includes('webresources/js/hubspot-form.js')) {
  throw new Error('Angebotsanfrage: Das benötigte HubSpot-Formularskript fehlt.');
}
for (const fieldName of ['anliegen', 'name', 'email', 'rolle', 'unternehmen', 'situation', 'ziel', 'datenschutz']) {
  if (!contact.includes(`name="${fieldName}"`)) {
    throw new Error(`Kontaktformular: Feld ${fieldName} fehlt.`);
  }
}
for (const formSnippet of ['id="situation-klaeren"', 'type="email"', 'required', 'Anfrage senden']) {
  if (!contact.includes(formSnippet)) {
    throw new Error(`Kontaktformular: ${formSnippet} fehlt.`);
  }
}
for (const formSnippet of [
  'id="clarity-session-start"',
  'name="name"',
  'name="email"',
  'name="situation"',
  'name="datenschutz"',
  'Clarity Session anfragen',
  'Beispiel · Fiktive Situation',
  'anonymisierte, typische Fallmuster',
]) {
  if (!claritySession.includes(formSnippet)) {
    throw new Error(`Clarity Session: ${formSnippet} fehlt.`);
  }
}
for (const fieldName of ['anliegen', 'rolle', 'unternehmen', 'ziel']) {
  if (claritySession.includes(`name="${fieldName}"`)) {
    throw new Error(`Clarity Session: Das Kurzformular enthält unerwartet das Feld ${fieldName}.`);
  }
}
for (const fieldName of [
  'leistung',
  'name',
  'email',
  'telefon',
  'unternehmen',
  'rolle',
  'situation',
  'zeitraum',
  'beteiligte',
  'datenschutz',
]) {
  if (!quoteRequest.includes(`name="${fieldName}"`)) {
    throw new Error(`Angebotsanfrage: Feld ${fieldName} fehlt.`);
  }
}
for (const formSnippet of [
  'id="angebot-anfragen"',
  'Angebot unverbindlich anfragen',
  'So geht es weiter',
  'Die Anfrage löst keine Bestellung aus.',
  'Noch unsicher',
]) {
  if (!quoteRequest.includes(formSnippet)) {
    throw new Error(`Angebotsanfrage: ${formSnippet} fehlt.`);
  }
}
for (const requiredServicesLink of [
  'class="mvp-panel mvp-panel--accent mvp-panel--link"',
  'href="/clarity-session"',
  'Clarity Session ansehen',
]) {
  if (!services.includes(requiredServicesLink)) {
    throw new Error(`Leistungen: Sichtbare Clarity-Session-Verlinkung fehlt: ${requiredServicesLink}`);
  }
}
for (const offerKey of [
  'sparring',
  'quick-diagnostic',
]) {
  if (!services.includes(`/angebot-anfragen?leistung=${offerKey}`)) {
    throw new Error(`Leistungen: Angebots-CTA für ${offerKey} fehlt.`);
  }
}
for (const detailPage of [
  'decision-review',
  'executive-sparring',
  'product-organisation-diagnostic',
  'workshops',
]) {
  if (!services.includes(`href="/${detailPage}"`)) {
    throw new Error(`Leistungen: Detailseite ${detailPage} ist nicht verlinkt.`);
  }
}
if (!services.includes('Angebot anfragen')) {
  throw new Error('Leistungen: Der allgemeine Angebots-CTA fehlt.');
}
if (!home.includes('Clarity Session') || !home.includes('site-nav__list--level-2')) {
  throw new Error('Navigation: Clarity Session fehlt als Unterpunkt von Leistungen.');
}

const insights = documents.get('insights');
for (const [snippet, expectedCount] of [
  ['class="site-shell card card--linked"', 9],
  ['class="text-link card__stretched-link"', 9],
]) {
  const actualCount = insights.split(snippet).length - 1;
  if (actualCount !== expectedCount) {
    throw new Error(`Insights: ${snippet} wurde ${actualCount}- statt ${expectedCount}-mal gefunden.`);
  }
}

for (const pagePath of [
  'start',
  'leistungen',
  'chos',
  'ueber-mich',
  'insights',
  'insights/warum-produktorganisationen-nicht-an-fehlenden-methoden-scheitern',
  'insights/diagnose-vor-eingriff',
  'insights/unklare-rollen-sind-selten-das-eigentliche-problem',
  'insights/rollen-und-verantwortlichkeiten-in-produktorganisationen-klaeren',
  'insights/wann-braucht-eine-produktorganisation-ein-operating-model',
  'insights/annahmen-vor-einer-produktentscheidung-pruefen',
  'insights/organisationsdiagnose-statt-standardberatung',
  'insights/product-organisation-diagnostic-ablauf-und-ergebnis',
  'insights/wann-ist-executive-sparring-sinnvoll',
]) {
  const html = documents.get(pagePath);
  for (const requiredSnippet of [
    'class="site-shell cta cta--linked"',
    'class="button cta__stretched-link"',
  ]) {
    if (!html.includes(requiredSnippet)) {
      throw new Error(`${pagePath}: Vollflächig klickbarer Abschluss-Teaser fehlt: ${requiredSnippet}`);
    }
  }
}

const cssResponse = await fetch(`${baseUrl}/.resources/meine-website/webresources/css/site.css`);
if (cssResponse.status !== 200) {
  throw new Error(`Stylesheet: HTTP ${cssResponse.status} statt 200.`);
}
const css = await cssResponse.text();
for (const requiredSelector of [
  '@keyframes page-enter',
  '@keyframes page-shimmer',
  '.is-page-leaving .page-transition',
  '.card__stretched-link::after',
  '.card__stretched-link:focus-visible::after',
  '.cta__stretched-link::after',
  '.cta__stretched-link:focus-visible::after',
  '.site-nav-trigger[aria-expanded="true"] .site-nav-trigger__icon',
  '.has-js .site-header.is-menu-open .site-nav',
  'html.is-menu-overlay-open body',
  'position: fixed',
  'height: 100dvh',
  'right: 0',
  'width: auto',
  'top: var(--menu-scroll-offset, 0)',
  'right: var(--menu-scrollbar-compensation, 0)',
  'overscroll-behavior: contain',
  '.site-nav__item--contact { display: none; }',
  '.site-nav__footer-cta:hover .site-nav__footer-arrow',
  '.site-nav__toggle[aria-expanded="true"] span',
  '.site-nav a[aria-current="page"] > .site-nav__link-arrow { display: none; }',
  'box-shadow: inset 0.25rem 0 0 var(--brand)',
  'min-height: 4.5rem',
  'font-size: clamp(1.3rem, 5vw, 1.55rem)',
  '.site-header.is-menu-open .site-brand',
  'cubic-bezier(0.22, 1, 0.36, 1)',
  'transition-delay: 0s, 0s, 180ms',
  'site-nav__item:nth-child(1) { transition-delay: 50ms; }',
  '.has-js .site-nav__footer',
  '@media (prefers-reduced-motion: reduce)',
  'position: absolute',
  'min-height: 2.75rem',
]) {
  if (!css.includes(requiredSelector)) {
    throw new Error(`Stylesheet: Klick- oder Fokusfläche fehlt: ${requiredSelector}`);
  }
}

if (css.includes('visibility 260ms ease')) {
  throw new Error('Stylesheet: Die Hauptnavigation animiert weiterhin die nicht zusammengesetzte visibility-Eigenschaft.');
}

const navigationScriptResponse = await fetch(`${baseUrl}/.resources/meine-website/webresources/js/navigation.js`);
if (navigationScriptResponse.status !== 200) {
  throw new Error(`Navigationsskript: HTTP ${navigationScriptResponse.status} statt 200.`);
}
const navigationScript = await navigationScriptResponse.text();
for (const requiredBehavior of [
  "event.key !== 'Escape'",
  "document.addEventListener('pointerdown'",
  "document.addEventListener('focusin'",
  "mobileMenuQuery.addEventListener('change'",
  "event.key === 'Tab'",
  "siteHeader?.setAttribute('aria-modal', 'true')",
  'setBackgroundInert(isOpen)',
  "siteBrand?.toggleAttribute('inert', isOpen)",
  "navigationMenu?.toggleAttribute('inert', mobileMenuQuery.matches)",
  "navigationMenu?.toggleAttribute('inert', mobileMenuQuery.matches && !isOpen)",
  'openDefaultSubmenus();',
  "'--menu-scroll-offset'",
  'window.scrollTo(0, menuScrollPosition)',
  'returnFocus',
]) {
  if (!navigationScript.includes(requiredBehavior)) {
    throw new Error(`Navigationsskript: Barrierefreies Menüverhalten fehlt: ${requiredBehavior}`);
  }
}

const contactScriptResponse = await fetch(`${baseUrl}/.resources/meine-website/webresources/js/hubspot-form.js`);
if (contactScriptResponse.status !== 200) {
  throw new Error(`Kontaktformularskript: HTTP ${contactScriptResponse.status} statt 200.`);
}
const contactScript = await contactScriptResponse.text();
for (const requiredHoneypotBehavior of [
  "contactForm.elements.namedItem('website-url')",
  'honeypot.tabIndex = -1',
  "honeypot.setAttribute('aria-hidden', 'true')",
]) {
  if (!contactScript.includes(requiredHoneypotBehavior)) {
    throw new Error(`Kontaktformular: Barrierefreier Spam-Schutz fehlt: ${requiredHoneypotBehavior}`);
  }
}
for (const requiredQuoteBehavior of [
  'form#angebot-anfragen',
  "contactForm.elements.namedItem('leistung')",
  'new URLSearchParams(window.location.search)',
  'Danke für Ihre Angebotsanfrage',
  "name: 'phone'",
]) {
  if (!contactScript.includes(requiredQuoteBehavior)) {
    throw new Error(`Angebotsanfrage: HubSpot-Verhalten fehlt: ${requiredQuoteBehavior}`);
  }
}

const internalLinks = new Set();
for (const html of documents.values()) {
  for (const match of html.matchAll(/href="(\/[^"#?]*)/g)) {
    if (!match[1].startsWith(publicPath('.resources/'))) {
      internalLinks.add(match[1]);
    }
  }
}

for (const linkPath of internalLinks) {
  const response = await fetch(new URL(linkPath, base.origin), { redirect: 'manual' });
  if (response.status >= 400) {
    throw new Error(`Interner Link ${linkPath}: HTTP ${response.status}.`);
  }
  if (linkPath.endsWith('.html')) {
    throw new Error(`Veralteter .html-Link gefunden: ${linkPath}`);
  }
}

const legacyResponse = await fetch(`${baseUrl}/ueber-uns`, { redirect: 'manual' });
const legacyLocation = new URL(legacyResponse.headers.get('location'), baseUrl).pathname;
if (legacyResponse.status !== 301 || legacyLocation !== publicPath('ueber-mich')) {
  throw new Error('Die alte Über-uns-URL wird nicht permanent auf /ueber-mich weitergeleitet.');
}

if (usesCleanRoot) {
  for (const metadataPath of ['robots.txt', 'sitemap.xml']) {
    const response = await fetch(`${base.origin}/${metadataPath}`);
    if (response.status !== 200) {
      throw new Error(`${metadataPath}: HTTP ${response.status} statt 200.`);
    }
  }

  const robots = await (await fetch(`${base.origin}/robots.txt`)).text();
  for (const crawler of ['OAI-SearchBot', 'ChatGPT-User', 'GPTBot']) {
    if (!robots.includes(`User-agent: ${crawler}`)) {
      throw new Error(`robots.txt: ausdrückliche Freigabe für ${crawler} fehlt.`);
    }
  }

  const sitemap = await (await fetch(`${base.origin}/sitemap.xml`)).text();
  if (!sitemap.includes('<lastmod>') || !sitemap.includes('/decision-review</loc>')) {
    throw new Error('Sitemap: Änderungsdaten oder GEO-Seiten fehlen.');
  }

  const missingPath = '/diese-seite-gibt-es-nicht-404-test';
  const missingResponse = await fetch(`${base.origin}${missingPath}`, { redirect: 'manual' });
  const missingHtml = await missingResponse.text();
  if (
    missingResponse.status !== 404 ||
    !missingResponse.headers.get('content-type')?.includes('text/html') ||
    missingResponse.headers.get('cache-control') !== 'no-store' ||
    !missingHtml.includes('<meta name="robots" content="noindex, nofollow">') ||
    !missingHtml.includes('Diese Seite führt gerade nirgendwo hin.') ||
    !missingHtml.includes('href="/leistungen"') ||
    !missingHtml.includes('href="/kontakt"')
  ) {
    throw new Error('Die öffentliche 404-Seite ist nicht vollständig oder wird nicht mit HTTP 404 ausgeliefert.');
  }
}

console.log(`MVP-Test erfolgreich: ${pages.length} Seiten und ${internalLinks.size} interne Links geprüft.`);
