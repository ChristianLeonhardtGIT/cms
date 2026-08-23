import { access, readFile } from 'node:fs/promises';

const requiredFiles = [
  'apache-tomcat/bin/catalina.sh',
  'apache-tomcat/webapps/magnoliaAuthor/WEB-INF/web.xml',
  'light-modules/meine-website/module.yaml',
  'light-modules/meine-website/apps/footer.yaml',
  'light-modules/meine-website/apps/navigation.yaml',
  'light-modules/meine-website/apps/siteSettings.yaml',
  'light-modules/meine-website/contentTypes/footer.yaml',
  'light-modules/meine-website/contentTypes/navigation.yaml',
  'light-modules/meine-website/contentTypes/siteSettings.yaml',
  'light-modules/meine-website/templates/pages/home.yaml',
  'light-modules/meine-website/templates/pages/home.ftl',
  'light-modules/meine-website/templates/components/contactForm.yaml',
  'light-modules/meine-website/templates/components/contactForm.ftl',
  'light-modules/meine-website/virtualUriMappings/remove-html-extension.yaml',
  'light-modules/meine-website/virtualUriMappings/legacy-about.yaml',
  'light-modules/meine-website/webresources/css/site.css',
  'light-modules/meine-website/webresources/js/navigation.js',
  'light-modules/meine-website/webresources/js/page-transitions.js',
  'light-modules/meine-website/webresources/js/attribution.js',
  'light-modules/meine-website/webresources/js/hubspot-form.js',
  'light-modules/meine-website/webresources/js/chos-check.js',
  'light-modules/meine-website/webresources/images/christian-leonhardt-portrait-v1.webp',
  'light-modules/meine-website/webresources/images/christian-leonhardt-portrait-v1.jpg',
  'scripts/build-mvp.groovy',
  'scripts/create-clarity-session.groovy',
  'scripts/create-quote-request.groovy',
  'scripts/link-clarity-session.groovy',
  'scripts/implement-geo-content.groovy',
  'scripts/publish-geo-content.groovy',
  'scripts/implement-point5-content.groovy',
  'scripts/publish-point5-content.groovy',
  'scripts/restore-navigation-children.groovy',
  'scripts/implement-profile-trust.groovy',
  'scripts/test-profile-trust-live.mjs',
  'scripts/test-profile-trust-render.mjs',
  'scripts/test-point5-live.mjs',
  'scripts/test-point5-render.mjs',
  'scripts/test-form.mjs',
  'scripts/test-geo-live.mjs',
  'scripts/test-geo-render.mjs',
  'deploy/static/404.html',
  'deploy/indexnow-submit.sh',
  'deploy/static/4eba2fbccd4fbe055b49e6d9d41c4e00.txt',
  'deploy/SECURITY.md',
  'deploy/fail2ban-sshd.local',
  'deploy/sshd-hardening.conf',
  'compose.yaml',
];

for (const file of requiredFiles) await access(file);

const pageDefinition = await readFile('light-modules/meine-website/templates/pages/home.yaml', 'utf8');
const pageTemplate = await readFile('light-modules/meine-website/templates/pages/home.ftl', 'utf8');
const pageDialog = await readFile('light-modules/meine-website/dialogs/pages/home.yaml', 'utf8');
const footerContentType = await readFile('light-modules/meine-website/contentTypes/footer.yaml', 'utf8');
const navigationContentType = await readFile('light-modules/meine-website/contentTypes/navigation.yaml', 'utf8');
const siteSettingsContentType = await readFile('light-modules/meine-website/contentTypes/siteSettings.yaml', 'utf8');
const htmlRedirect = await readFile('light-modules/meine-website/virtualUriMappings/remove-html-extension.yaml', 'utf8');
const navigationScript = await readFile('light-modules/meine-website/webresources/js/navigation.js', 'utf8');
const pageTransitionScript = await readFile('light-modules/meine-website/webresources/js/page-transitions.js', 'utf8');
const hubspotFormScript = await readFile('light-modules/meine-website/webresources/js/hubspot-form.js', 'utf8');
const attributionScript = await readFile('light-modules/meine-website/webresources/js/attribution.js', 'utf8');
const chosCheckScript = await readFile('light-modules/meine-website/webresources/js/chos-check.js', 'utf8');
const siteStyles = await readFile('light-modules/meine-website/webresources/css/site.css', 'utf8');
const caddyConfig = await readFile('deploy/Caddyfile', 'utf8');
const productionCompose = await readFile('compose.production.yaml', 'utf8');
const fail2banSshd = await readFile('deploy/fail2ban-sshd.local', 'utf8');
const sshdHardening = await readFile('deploy/sshd-hardening.conf', 'utf8');
const sitemap = await readFile('deploy/static/sitemap.xml', 'utf8');
const notFoundPage = await readFile('deploy/static/404.html', 'utf8');
const mvpBuilder = await readFile('scripts/build-mvp.groovy', 'utf8');
const clarityBuilder = await readFile('scripts/create-clarity-session.groovy', 'utf8');
const quoteRequestBuilder = await readFile('scripts/create-quote-request.groovy', 'utf8');
const clarityLinker = await readFile('scripts/link-clarity-session.groovy', 'utf8');
const geoBuilder = await readFile('scripts/implement-geo-content.groovy', 'utf8');
const point5Builder = await readFile('scripts/implement-point5-content.groovy', 'utf8');
const profileTrustBuilder = await readFile('scripts/implement-profile-trust.groovy', 'utf8');
const indexNowSubmitter = await readFile('deploy/indexnow-submit.sh', 'utf8');
const componentIds = ['hero', 'text', 'cards', 'callToAction'];

for (const componentId of componentIds) {
  const base = 'light-modules/meine-website';
  await Promise.all([
    access(`${base}/templates/components/${componentId}.yaml`),
    access(`${base}/dialogs/components/${componentId}.yaml`),
    access(`${base}/templates/components/${componentId}.ftl`),
  ]);

  if (!pageDefinition.includes(`meine-website:components/${componentId}`)) {
    throw new Error(`Komponente ${componentId} fehlt in der Seitenvorlage.`);
  }
}

if (!pageDialog.includes('hideInNavigation:') || !pageTemplate.includes('child.hideInNavigation')) {
  throw new Error('Die Option zum Ausblenden von Seiten in der Hauptnavigation fehlt.');
}

if (
  !pageDefinition.includes('meine-website:components/contactForm') ||
  !mvpBuilder.includes("addComponent(area, 'contactForm'") ||
  !mvpBuilder.includes('trackMail: true') ||
  !mvpBuilder.includes("form:components/formHoneypot") ||
  !siteStyles.includes('.form-item')
) {
  throw new Error('Das Magnolia-Kontaktformular oder seine Darstellung ist unvollständig.');
}

for (const path of ['leistungen', 'chos', 'ueber-mich', 'insights', 'kontakt', 'impressum', 'datenschutz']) {
  if (!mvpBuilder.includes(`createOrResetPage(website, '${path}'`)) {
    throw new Error(`Die MVP-Seite ${path} fehlt im Inhaltsaufbau.`);
  }
}

if (
  !mvpBuilder.includes('warum-produktorganisationen-nicht-an-fehlenden-methoden-scheitern') ||
  !mvpBuilder.includes('diagnose-vor-eingriff') ||
  !mvpBuilder.includes('unklare-rollen-sind-selten-das-eigentliche-problem')
) {
  throw new Error('Die drei geplanten Insights-Artikel fehlen.');
}

if (!pageTemplate.includes('findRootPage') || !pageTemplate.includes("cmsfn.parent(page, 'mgnl:page')")) {
  throw new Error('Die zuverlässige Ermittlung der Startseite fehlt.');
}

for (const structuredDataSnippet of [
  'type="application/ld+json"',
  '"@type": "Person"',
  '"@type": "Organization"',
  '"@type": "Article"',
  '"@type": "Service"',
  'datePublished',
  'serviceAudience',
]) {
  if (!pageTemplate.includes(structuredDataSnippet)) {
    throw new Error(`Strukturierte Daten fehlen: ${structuredDataSnippet}`);
  }
}

for (const sourceSnippet of [
  "const attributionKeys = ['utm_source', 'utm_medium', 'utm_campaign']",
  "document.referrer",
  "window.cleonhardtAttribution",
]) {
  if (!attributionScript.includes(sourceSnippet)) {
    throw new Error(`Datensparsame Herkunftsmessung fehlt: ${sourceSnippet}`);
  }
}
for (const sourceSnippet of ['Herkunft:', 'Anfrageseite:', 'safePageUrl']) {
  if (!hubspotFormScript.includes(sourceSnippet)) {
    throw new Error(`HubSpot-Herkunft fehlt: ${sourceSnippet}`);
  }
}

for (const crawler of ['OAI-SearchBot', 'ChatGPT-User', 'GPTBot']) {
  const robots = await readFile('deploy/static/robots.txt', 'utf8');
  if (!robots.includes(`User-agent: ${crawler}`)) {
    throw new Error(`robots.txt enthält keine ausdrückliche Regel für ${crawler}.`);
  }
}
if (
  !caddyConfig.includes('/4eba2fbccd4fbe055b49e6d9d41c4e00.txt') ||
  !indexNowSubmitter.includes('https://api.indexnow.org/indexnow')
) {
  throw new Error('IndexNow-Key oder Benachrichtigung fehlt.');
}

for (const pagePath of [
  'decision-review',
  'executive-sparring',
  'product-organisation-diagnostic',
  'workshops',
]) {
  if (!geoBuilder.includes(`'${pagePath}': [`) || !sitemap.includes(`<loc>https://cleonhardt.de/${pagePath}</loc><lastmod>`)) {
    throw new Error(`GEO-Angebotsseite fehlt: ${pagePath}`);
  }
}
for (const insightPath of [
  'rollen-und-verantwortlichkeiten-in-produktorganisationen-klaeren',
  'wann-braucht-eine-produktorganisation-ein-operating-model',
  'annahmen-vor-einer-produktentscheidung-pruefen',
  'organisationsdiagnose-statt-standardberatung',
  'product-organisation-diagnostic-ablauf-und-ergebnis',
  'wann-ist-executive-sparring-sinnvoll',
]) {
  if (!geoBuilder.includes(`'${insightPath}': [`) || !sitemap.includes(`/insights/${insightPath}</loc><lastmod>`)) {
    throw new Error(`GEO-Insight fehlt: ${insightPath}`);
  }
}

for (const requiredPoint5Content of [
  "ensurePage(website.rootNode, 'praxisfaelle'",
  "ensurePage(website.rootNode, 'chos-selbstcheck'",
  'anonymisierte und verdichtete typische Fallmuster',
  'keine Kundenreferenzen',
  'Ihre Antworten werden nur in diesem Browser ausgewertet',
  "module.setProperty('point5Module', true)",
  'href="/chos-selbstcheck"',
  'href="/praxisfaelle"',
]) {
  if (!point5Builder.includes(requiredPoint5Content)) {
    throw new Error(`Punkt 5 ist inhaltlich unvollständig: ${requiredPoint5Content}`);
  }
}
for (const requiredCheckBehavior of [
  "utm_source', 'chos-selbstcheck'",
  "utm_medium', 'website-tool'",
  'new FormData(form)',
  'result.hidden = false',
]) {
  if (!chosCheckScript.includes(requiredCheckBehavior)) {
    throw new Error(`ChOS-Selbstcheck ist funktional unvollständig: ${requiredCheckBehavior}`);
  }
}
if (
  !pageTemplate.includes("canonicalPath?ends_with('/chos-selbstcheck')") ||
  !pageTemplate.includes('webresources/js/chos-check.js') ||
  !siteStyles.includes('.practice-case') ||
  !siteStyles.includes('.chos-check__question') ||
  !siteStyles.includes('.chos-check__result') ||
  !sitemap.includes('<loc>https://cleonhardt.de/praxisfaelle</loc><lastmod>') ||
  !sitemap.includes('<loc>https://cleonhardt.de/chos-selbstcheck</loc><lastmod>')
) {
  throw new Error('Punkt 5 fehlt in Template, Darstellung oder Sitemap.');
}
if (
  !siteStyles.includes('.prose a.button,') ||
  !siteStyles.includes('.prose a.button:visited') ||
  !siteStyles.includes('.prose a.button:focus-visible')
) {
  throw new Error('Der Ergebnis-CTA des ChOS-Selbstchecks besitzt keine kontraststarke Link-Überschreibung.');
}

for (const requiredProfileContent of [
  'christian-leonhardt-portrait-v1.webp',
  'Portrait von Christian Leonhardt',
  'Product Leadership, Organisation und Transformation',
  'Loyalty &amp; CRM',
  'Organisationsdiagnose',
  "heroChooser.setProperty('ctaText', 'Clarity Session ansehen')",
  "setInternalLink(homeHero, 'ctaChooser', '/clarity-session')",
]) {
  if (!profileTrustBuilder.includes(requiredProfileContent)) {
    throw new Error(`Profil- und Vertrauensblock ist unvollständig: ${requiredProfileContent}`);
  }
}
if (
  !pageTemplate.includes('christian-leonhardt-portrait-v1.jpg') ||
  !siteStyles.includes('.profile-trust') ||
  !siteStyles.includes('.profile-trust__topics') ||
  !siteStyles.includes('.profile-signature')
) {
  throw new Error('Profilbild fehlt in strukturierten Daten oder Darstellung.');
}

if (
  !pageTemplate.includes("[#function pageLink page fallback='#']") ||
  !pageTemplate.includes("link == '/start'") ||
  !pageTemplate.includes("canonicalPath == '/start'") ||
  !caddyConfig.includes('@legacy_start path /start') ||
  !caddyConfig.includes('header @legacy_start X-Robots-Tag "noindex, follow"') ||
  caddyConfig.includes('redir @legacy_start / permanent') ||
  !caddyConfig.includes('@homepage path /') ||
  !caddyConfig.includes('rewrite @homepage /start') ||
  !caddyConfig.includes('path /.resources/meine-website/webresources/*') ||
  !caddyConfig.includes('query v=*') ||
  !caddyConfig.includes('Cache-Control "public, max-age=31536000, immutable"') ||
  !pageTransitionScript.includes('normalizeLegacyHomeUrl') ||
  !pageTransitionScript.includes("searchParams.get('_start')") ||
  !pageTransitionScript.includes('window.history.replaceState') ||
  !sitemap.includes('<loc>https://cleonhardt.de/</loc>') ||
  sitemap.includes('<loc>https://cleonhardt.de/start</loc>')
) {
  throw new Error('Die Startseite ist nicht vollständig unter der Root-URL / eingerichtet.');
}

if (
  !pageTemplate.includes("canonicalPath?ends_with('/kontakt') || canonicalPath?ends_with('/clarity-session') || canonicalPath?ends_with('/angebot-anfragen')") ||
  !pageTemplate.includes('webresources/js/hubspot-form.js')
) {
  throw new Error('Das HubSpot-Formularskript wird nicht gezielt nur auf Formularseiten geladen.');
}

for (const requiredClarityContent of [
  "formName: 'clarity-session-start'",
  'Drei Situationen, in denen Klarheit den Unterschied macht',
  'anonymisierte, typische Fallmuster',
  'Beispiel · Fiktive Situation',
  'Clarity Brief',
  "hideInNavigation: true",
  "internalLink', CLARITY_PATH",
]) {
  if (!clarityBuilder.includes(requiredClarityContent)) {
    throw new Error(`Clarity-Session-Landingpage ist unvollständig: ${requiredClarityContent}`);
  }
}

if (
  !sitemap.includes('<loc>https://cleonhardt.de/clarity-session</loc>') ||
  !siteStyles.includes('.clarity-facts') ||
  !siteStyles.includes('.clarity-case') ||
  !siteStyles.includes('.clarity-brief') ||
  !siteStyles.includes('#clarity-session-start')
) {
  throw new Error('Clarity-Session-Landingpage fehlt in Sitemap oder Darstellung.');
}

for (const requiredQuoteRequestContent of [
  "formName: 'angebot-anfragen'",
  "controlName: 'leistung'",
  'Angebot unverbindlich anfragen',
  'So geht es weiter',
  'Die Anfrage löst keine Bestellung aus.',
  'leistung=${offerKey}',
  "internalLink', QUOTE_PATH",
]) {
  if (!quoteRequestBuilder.includes(requiredQuoteRequestContent)) {
    throw new Error(`Angebotsanfrage ist unvollständig: ${requiredQuoteRequestContent}`);
  }
}
if (
  !sitemap.includes('<loc>https://cleonhardt.de/angebot-anfragen</loc>') ||
  !siteStyles.includes('.quote-process') ||
  !siteStyles.includes('.mvp-offer-cta') ||
  !siteStyles.includes('.mvp-grid--3 .mvp-offer-cta') ||
  !siteStyles.includes('white-space: nowrap') ||
  !hubspotFormScript.includes('form#angebot-anfragen') ||
  !hubspotFormScript.includes("contactForm.elements.namedItem('leistung')") ||
  !hubspotFormScript.includes('innerhalb von zwei Werktagen')
) {
  throw new Error('Die Angebotsanfrage fehlt in Sitemap, Darstellung oder HubSpot-Übergabe.');
}

if (!pageTemplate.includes('© 2026 Christian Leonhardt') || !siteStyles.includes('.site-footer__copyright')) {
  throw new Error('Der Copyright-Hinweis im Footer fehlt.');
}

for (const interactiveCardRule of [
  '--interactive-shadow',
  '--interactive-lift',
  '.mvp-panel:has(.mvp-offer-cta)',
  '.related-insights__card:focus-within',
  '.card--linked:hover',
  '.cta--linked:hover',
]) {
  if (!siteStyles.includes(interactiveCardRule)) {
    throw new Error(`Das globale Hover-Muster fehlt: ${interactiveCardRule}`);
  }
}

if (!siteStyles.includes('.mvp-panel--accent { border-color: rgba(24, 91, 59, 0.2)')) {
  throw new Error('Der sichtbare Rahmen der Akzentboxen fehlt.');
}

for (const requiredClarityLink of [
  'mvp-panel--link',
  'Clarity Session ansehen',
  "navigation.getNode('/cleonhardt/Main')",
  "servicesNav.getNode('children')",
    "clarityNav.setProperty('targetPage', clarityPage.getIdentifier())",
]) {
  if (!clarityLinker.includes(requiredClarityLink)) {
    throw new Error(`Clarity-Session-Verlinkung ist unvollständig: ${requiredClarityLink}`);
  }
}
for (const requiredLinkedPanelStyle of [
  '.prose a.mvp-panel--link',
  '.mvp-panel__link',
  '.mvp-panel--link:hover .mvp-panel__link',
]) {
  if (!siteStyles.includes(requiredLinkedPanelStyle)) {
    throw new Error(`Verlinkte Clarity-Session-Karte ist unvollständig: ${requiredLinkedPanelStyle}`);
  }
}

if (
  !caddyConfig.includes('@not_found status 404') ||
  !caddyConfig.includes('rewrite * /404.html') ||
  !caddyConfig.includes('status 404') ||
  !notFoundPage.includes('<meta name="robots" content="noindex, nofollow">') ||
  !notFoundPage.includes('Diese Seite führt gerade nirgendwo hin.') ||
  !notFoundPage.includes('href="/kontakt"')
) {
  throw new Error('Die eigene 404-Seite oder ihre statuswahrende Caddy-Auslieferung ist unvollständig.');
}

if (
  !caddyConfig.includes('cms.cleonhardt.de {') ||
  !caddyConfig.includes('basic_auth {') ||
  !/\bchristian\s+\$2[ayb]\$\d{2}\$/.test(caddyConfig) ||
  !caddyConfig.includes('reverse_proxy magnolia-author:8080') ||
  !caddyConfig.includes('header_up -Authorization') ||
  !caddyConfig.includes('Cache-Control "no-store"') ||
  !caddyConfig.includes('X-Frame-Options "SAMEORIGIN"') ||
  !productionCompose.includes('- magnolia-author') ||
  !sshdHardening.includes('PasswordAuthentication no') ||
  !sshdHardening.includes('PermitRootLogin prohibit-password') ||
  !sshdHardening.includes('MaxAuthTries 3') ||
  !fail2banSshd.includes('[sshd]') ||
  !fail2banSshd.includes('enabled = true') ||
  !fail2banSshd.includes('banaction = ufw')
) {
  throw new Error('Der geschützte CMS-Zugang oder die SSH-Härtung ist unvollständig.');
}

if (
  siteStyles.includes('visibility 260ms ease') ||
  !navigationScript.includes("navigationMenu?.toggleAttribute('inert', mobileMenuQuery.matches)") ||
  !navigationScript.includes("navigationMenu?.toggleAttribute('inert', mobileMenuQuery.matches && !isOpen)")
) {
  throw new Error('Die mobile Hauptnavigation nutzt noch eine nicht zusammengesetzte Animation oder ist im geschlossenen Zustand nicht inert.');
}

if (
  !siteStyles.includes('.site-nav__item:hover > .site-nav__list') ||
  !/@media \(hover: hover\) and \(min-width: 45\.01rem\) \{[\s\S]*?\.site-nav__item:focus-within > \.site-nav__list/.test(siteStyles) ||
  !/\.site-nav__list--level-2::before\s*\{[^}]*bottom:\s*100%;[^}]*height:\s*0\.5rem;[^}]*\}/s.test(siteStyles) ||
  !/\.site-nav__list--level-3::before\s*\{[^}]*left:\s*100%;[^}]*width:\s*0\.6rem;[^}]*\}/s.test(siteStyles) ||
  !/site\.css\?v=\d{8}-\d+/.test(pageTemplate)
) {
  throw new Error('Das Desktop-Untermenü besitzt keine vollständige Hover-Brücke, Fokus-Unterstützung oder Cache-Versionierung.');
}

if (
  !footerContentType.includes('type: reference:page') ||
  !pageTemplate.includes("cmsfn.contentByPath('/', 'footer')") ||
  !pageTemplate.includes('cmsfn.children(footerConfig)')
) {
  throw new Error('Die Footer-App oder ihre Ausgabe in der Seitenvorlage ist unvollständig.');
}

if (
  !navigationContentType.includes('type: navigationLevel1') ||
  !navigationContentType.includes('type: navigationLevel2') ||
  !navigationContentType.includes('type: navigationLevel3') ||
  !navigationContentType.includes('type: reference:page') ||
  !pageTemplate.includes("cmsfn.contentByPath('/', 'navigation')") ||
  !pageTemplate.includes('renderNavigationItems') ||
  !pageTemplate.includes('site-nav__list--level-${level}') ||
  !pageTemplate.includes('site-nav__toggle') ||
  !pageTemplate.includes('site-nav-trigger') ||
  !navigationScript.includes("setAttribute('aria-expanded'") ||
  !navigationScript.includes("classList.toggle('is-menu-open'")
) {
  throw new Error('Die dreistufige Navigation oder ihre Ausgabe in der Seitenvorlage ist unvollständig.');
}
if (!pageTemplate.includes('[#local items = items + findNavigationItems(child)]')) {
  throw new Error('Die Navigation kann verschachtelte Content-Type-Container nicht rekursiv auflösen.');
}

if (
  !pageTemplate.includes("relatedInsightsExcludedPaths = ['/kontakt', '/clarity-session', '/angebot-anfragen', '/impressum', '/datenschutz', '/insights']") ||
  !pageTemplate.includes('!relatedInsightsExcludedPaths?seq_contains(content.@path)') ||
  !pageTemplate.includes('insightPage.@id != content.@id') ||
  !pageTemplate.includes('relatedInsights?size < 3') ||
  !pageTemplate.includes('class="related-insights"') ||
  !pageTemplate.includes('class="related-insights__link"') ||
  !siteStyles.includes('.related-insights__grid') ||
  !siteStyles.includes('.related-insights__link::after') ||
  !siteStyles.includes('.related-insights__link:focus-visible::after')
) {
  throw new Error('Das globale Related-Insights-Modul, seine Seitenausschlüsse oder seine barrierearme Verlinkung sind unvollständig.');
}

if (
  !pageTemplate.includes('class="page-transition"') ||
  !pageTemplate.includes('page-transitions.js') ||
  !pageTransitionScript.includes("document.documentElement.classList.add('is-page-leaving')") ||
  !pageTransitionScript.includes("window.addEventListener('pageshow'") ||
  !siteStyles.includes('@keyframes page-shimmer') ||
  !siteStyles.includes('.is-page-leaving .page-transition') ||
  !siteStyles.includes('@media (prefers-reduced-motion: reduce)')
) {
  throw new Error('Die barrierearmen Seitenübergänge oder der Skeleton-Shimmer sind unvollständig.');
}

if (
  !siteSettingsContentType.includes('type: reference:asset') ||
  !siteSettingsContentType.includes('name: logoTargetPage') ||
  !siteSettingsContentType.includes('type: reference:page') ||
  !pageTemplate.includes("cmsfn.contentByPath('/', 'siteSettings')") ||
  !pageTemplate.includes('site-brand__logo') ||
  !pageTemplate.includes('site-brand__logo--inline') ||
  !pageTemplate.includes('<svg class="site-brand__logo') ||
  !pageTemplate.includes('aria-label="${logoAltText}"') ||
  !pageTemplate.includes("cmsfn.contentById(logoTargetPageId, 'website')") ||
  !pageTemplate.includes('href="${logoUrl}"')
) {
  throw new Error('Die Website-Einstellungen oder ihre Logo-Ausgabe in der Seitenvorlage sind unvollständig.');
}

if (
  !htmlRedirect.includes('RegexpVirtualUriMapping') ||
  !htmlRedirect.includes('\\.html$') ||
  !htmlRedirect.includes("toUri: 'permanent:/$1'")
) {
  throw new Error('Die permanente Weiterleitung von .html-URLs ist unvollständig.');
}

console.log('Setup-Prüfung erfolgreich: Magnolia-Bundle, Website-Modul, Navigation, Footer und Website-Einstellungen sind vollständig.');
