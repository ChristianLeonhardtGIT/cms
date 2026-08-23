import assert from 'node:assert/strict';
import { chromium } from 'file:///home/cd/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright/index.mjs';

const baseUrl = (process.env.E2E_BASE_URL || process.env.BASE_URL || 'https://cleonhardt.de').replace(/\/$/, '');

const overview = {
  path: '/probleme',
  h1: 'Welche Situation kommt dir bekannt vor?',
};

const details = [
  {
    path: '/probleme/leadership-bottleneck',
    h1: 'Wenn Entscheidungen immer bei dir landen',
    diagnosis: 'Was könnte hinter dem Muster stecken?',
    checkTitle: '7-Tage-Entscheidungsprotokoll',
    questions: [
      'Welche Entscheidungen landen wiederholt bei dir – und welche davon gehören tatsächlich auf deine Ebene?',
      'Braucht das Team deine Information, deine Beratung, deine Freigabe oder deine Entscheidung?',
      'Fehlen dem Team Mandat, Kontext oder klare Grenzen, um selbst zu entscheiden?',
      'Welche Risiken darf das Team selbst tragen – und wann ist eine Eskalation sinnvoll?',
      'Was passiert, wenn eine Entscheidung anders ausfällt, als du es selbst getan hättest?',
      'Was tust du selbst, das Rückversicherung sicherer macht als eigenständiges Entscheiden?',
    ],
    avoidCount: 4,
    related: [
      ['Selbst prüfen', 'ChOS Selbstcheck', '/chos-selbstcheck'],
      ['Insight', 'Diagnose vor Eingriff', '/insights/diagnose-vor-eingriff'],
      ['Insight', 'Unklare Rollen sind selten das eigentliche Problem', '/insights/unklare-rollen-sind-selten-das-eigentliche-problem'],
      ['Praxisfall', 'Neue Rollen, alte Entscheidungen', '/praxisfaelle'],
      ['Unterstützung', 'ChOS Clarity Session', '/clarity-session'],
      ['Verwandte Situation', 'Wenn Entscheidungen zu lange dauern', '/probleme/langsame-entscheidungen'],
      ['Verwandte Situation', 'Neu in Führung: Was solltest du zuerst verändern?', '/probleme/leadership-transition'],
      ['Verwandte Situation', 'Wenn Teams nicht wissen, woran sie als Nächstes arbeiten sollen', '/probleme/unklare-prioritaeten'],
    ],
  },
  {
    path: '/probleme/langsame-entscheidungen',
    h1: 'Wenn Entscheidungen zu lange dauern',
    diagnosis: 'Was könnte Entscheidungen tatsächlich verlangsamen?',
    checkTitle: 'Eine festhängende Entscheidung in 10 Minuten klären',
    questions: [
      'Was genau muss entschieden werden – in einem Satz?',
      'Wer entscheidet formal – und wer entscheidet praktisch?',
      'Wessen Input ist erforderlich – und wessen Zustimmung wird nur vorsorglich gesucht?',
      'Welche konkrete Information fehlt noch – und würde sie die Entscheidung tatsächlich verändern?',
      'Welches Risiko wird abgesichert – und wie reversibel ist die Entscheidung?',
      'Was kostet eine weitere Woche Nicht-Entscheiden?',
      'Was macht Nicht-Entscheiden im aktuellen System sicherer als Entscheiden?',
    ],
    avoidCount: 4,
    related: [
      ['Selbst prüfen', 'ChOS Selbstcheck', '/chos-selbstcheck'],
      ['Insight', 'Annahmen vor einer Produktentscheidung prüfen', '/insights/annahmen-vor-einer-produktentscheidung-pruefen'],
      ['Insight', 'Rollen und Verantwortlichkeiten in Produktorganisationen klären', '/insights/rollen-und-verantwortlichkeiten-in-produktorganisationen-klaeren'],
      ['Praxisfall', 'Eine Prioritätsentscheidung beginnt immer wieder von vorn', '/praxisfaelle'],
      ['Unterstützung', 'ChOS Decision Review', '/decision-review'],
      ['Verwandte Situation', 'Wenn Entscheidungen immer bei dir landen', '/probleme/leadership-bottleneck'],
      ['Verwandte Situation', 'Was darf AI entscheiden – und wer trägt die Verantwortung?', '/probleme/ai-decision-rights'],
      ['Verwandte Situation', 'Wenn Teams nicht wissen, woran sie als Nächstes arbeiten sollen', '/probleme/unklare-prioritaeten'],
    ],
  },
  {
    path: '/probleme/ai-decision-rights',
    h1: 'Was darf AI entscheiden – und wer trägt die Verantwortung?',
    diagnosis: 'Was sollte vor einer Automatisierung geklärt sein?',
    checkTitle: 'Einen AI-Anwendungsfall vor Autonomie prüfen',
    questions: [
      'Welche konkrete Entscheidung oder Handlung soll die AI übernehmen?',
      'Informiert, empfiehlt, entscheidet oder handelt das System?',
      'Welche Folgen hätte ein plausibler Fehler – und welche davon wären nicht akzeptabel?',
      'Wer trägt die fachliche Verantwortung für die Entscheidung?',
      'Unter welchen Bedingungen muss ein Mensch übernehmen?',
      'Wer darf das System stoppen oder eine Handlung zurücksetzen?',
      'Woran erkennst du Entscheidungsqualität und unerwartete Auswirkungen?',
    ],
    avoidCount: 5,
    related: [
      ['Selbst prüfen', 'ChOS Selbstcheck', '/chos-selbstcheck'],
      ['Insight', 'Warum AI-Einführung ein Operating-Model-Thema ist', '/insights/warum-ai-einfuehrung-ein-operating-model-thema-ist'],
      ['Insight', 'Decision Rights zwischen Mensch und AI gestalten', '/insights/decision-rights-zwischen-mensch-und-ai'],
      ['Unterstützung', 'AI Operating Model Assessment', '/ai-operating-model-assessment'],
      ['Verwandte Situation', 'Wenn Entscheidungen zu lange dauern', '/probleme/langsame-entscheidungen'],
    ],
  },
  {
    path: '/probleme/leadership-transition',
    h1: 'Neu in Führung: Was solltest du zuerst verändern?',
    diagnosis: 'Was solltest du am Anfang verstehen?',
    checkTitle: 'Dein Beobachtungsprotokoll für die ersten zwei Wochen',
    questions: [
      'Was beobachtest du tatsächlich – getrennt von deiner ersten Interpretation?',
      'Wo und von wem werden Entscheidungen formal und praktisch getroffen?',
      'Was funktioniert bereits gut und sollte nicht vorschnell verändert werden?',
      'Welche Erfahrungen haben die heutigen Routinen und Schutzmechanismen geprägt?',
      'Was erwartet dein Team von dir – und was erwartet dein Vorgesetzter tatsächlich?',
      'Welche Erklärung bestätigt sich in mehreren konkreten Situationen?',
      'Welcher kleine, reversible Eingriff könnte diese Hypothese prüfen?',
    ],
    avoidCount: 5,
    related: [
      ['Selbst prüfen', 'ChOS Selbstcheck', '/chos-selbstcheck'],
      ['Insight', 'Diagnose vor Eingriff', '/insights/diagnose-vor-eingriff'],
      ['Insight', 'Wann ist Executive Sparring sinnvoll?', '/insights/wann-ist-executive-sparring-sinnvoll'],
      ['Praxisfall', 'Neue Rollen, alte Entscheidungen', '/praxisfaelle'],
      ['Unterstützung', 'Executive / Product Leadership Sparring', '/executive-sparring'],
      ['Verwandte Situation', 'Wenn Entscheidungen immer bei dir landen', '/probleme/leadership-bottleneck'],
      ['Verwandte Situation', 'Wenn Entscheidungen zu lange dauern', '/probleme/langsame-entscheidungen'],
    ],
  },
  {
    path: '/probleme/unklare-prioritaeten',
    h1: 'Wenn Teams nicht wissen, woran sie als Nächstes arbeiten sollen',
    diagnosis: 'Was könnte tatsächlich dahinterstecken?',
    checkTitle: 'Direction & Priority Check',
    questions: [
      'Welche drei Ergebnisse sind aktuell wirklich am wichtigsten?',
      'Kann das Team erklären, warum genau diese drei wichtig sind?',
      'Nach welchen Kriterien wird neue Arbeit bewertet?',
      'Wer entscheidet bei konkurrierenden Prioritäten?',
      'Wer darf ein neues Thema starten – und wer darf Nein sagen?',
      'Was wird gestoppt, wenn etwas Neues beginnt?',
      'Welche Entscheidung liegt bewusst oberhalb des Teams?',
    ],
    avoidCount: 4,
    related: [
      ['Selbst prüfen', 'ChOS Selbstcheck', '/chos-selbstcheck'],
      ['Insight', 'Annahmen vor einer Produktentscheidung prüfen', '/insights/annahmen-vor-einer-produktentscheidung-pruefen'],
      ['Insight', 'Warum Produktorganisationen nicht an fehlenden Methoden scheitern', '/insights/warum-produktorganisationen-nicht-an-fehlenden-methoden-scheitern'],
      ['Praxisfall', 'Eine Prioritätsentscheidung beginnt immer wieder von vorn', '/praxisfaelle'],
      ['Unterstützung', 'ChOS Clarity Session', '/clarity-session'],
      ['Unterstützung', 'ChOS Decision Review', '/decision-review'],
      ['Verwandte Situation', 'Wenn Entscheidungen zu lange dauern', '/probleme/langsame-entscheidungen'],
      ['Verwandte Situation', 'Wenn Entscheidungen immer bei dir landen', '/probleme/leadership-bottleneck'],
    ],
  },
];

const viewports = [
  { name: 'desktop', width: 1440, height: 1000 },
  { name: 'mobile', width: 390, height: 844 },
];

const expectedNavigation = ['Start', 'Situationen', 'ChOS', 'Leistungen', 'Insights', 'Über mich'];
const expectedHomeJourney = [
  'Klarheit für komplexe Produktorganisationen.',
  'Mehr Technik löst keine unklaren Strukturen.',
  'Erst verstehen. Dann wirksam verändern - ChOS.',
  'Diagnose → Design → Veränderung',
  'Wie möchtest du starten?',
  'Mit einer konkreten Situation starten.',
  'Weitere passende Angebote',
  'Product Leadership, Organisation und Transformation',
  'Vertraulich von Anfang an.',
  'Welche Situation möchtest du klären?',
];

function normalizeText(value = '') {
  return value.replace(/\s+/g, ' ').trim();
}

async function openPage(page, path) {
  const response = await page.goto(`${baseUrl}${path}`, {
    waitUntil: 'domcontentloaded',
    timeout: 30_000,
  });
  assert.equal(response?.status(), 200, `${path}: HTTP ${response?.status() ?? 'unbekannt'}`);
  await page.locator('main#inhalt').waitFor({ state: 'visible' });
}

async function assertCommonPageState(page, expected, viewportName) {
  const state = await page.evaluate(() => ({
    h1: document.querySelector('main h1')?.textContent?.trim() ?? '',
    breadcrumb: Array.from(document.querySelectorAll('.content-breadcrumb li')).map((item) => ({
      text: item.textContent?.trim() ?? '',
      href: item.querySelector('a')?.getAttribute('href') ?? null,
      current: Boolean(item.querySelector('[aria-current="page"]')),
    })),
    horizontalOverflow: document.documentElement.scrollWidth > document.documentElement.clientWidth + 1,
    renderingError: /RenderingException|FreeMarker|TemplateModelException/.test(document.body.innerText),
  }));

  assert.equal(state.h1, expected.h1, `${expected.path} (${viewportName}): falsche H1`);
  assert.equal(state.horizontalOverflow, false, `${expected.path} (${viewportName}): horizontaler Überlauf`);
  assert.equal(state.renderingError, false, `${expected.path} (${viewportName}): Renderingfehler sichtbar`);
  assert.ok(state.breadcrumb.length >= 2, `${expected.path} (${viewportName}): Breadcrumb fehlt`);
  assert.deepEqual(state.breadcrumb[0], { text: 'Startseite', href: '/', current: false });
  assert.equal(state.breadcrumb.at(-1).current, true, `${expected.path} (${viewportName}): aktueller Breadcrumb fehlt`);
  assert.equal(state.breadcrumb.at(-1).text, expected.h1 === overview.h1 ? 'Problemsituationen' : expected.h1);
}

async function assertNavigationOrder(page, viewportName) {
  const labels = await page.evaluate(() => Array.from(
    document.querySelectorAll('#hauptnavigation > .site-nav__list--level-1 > .site-nav__item:not(.site-nav__item--contact) > .site-nav__entry > a .site-nav__link-label'),
  ).map((item) => item.textContent?.trim() ?? ''));
  assert.deepEqual(labels, expectedNavigation, `Navigation (${viewportName}): Reihenfolge folgt nicht der ChOS-Nutzerreise`);
}

async function assertProblemNavigationCoverage(page, viewportName) {
  const state = await page.evaluate(() => {
    const topLevelItems = Array.from(document.querySelectorAll('#hauptnavigation > .site-nav__list--level-1 > .site-nav__item'));
    const problemItem = topLevelItems.find((item) => item.querySelector(':scope > .site-nav__entry > a')?.getAttribute('href') === '/probleme');
    return {
      hasToggle: Boolean(problemItem?.querySelector(':scope > .site-nav__entry > .site-nav__toggle')),
      hrefs: Array.from(problemItem?.querySelectorAll(':scope > .site-nav__list a[href^="/probleme/"]') ?? [])
        .map((link) => link.getAttribute('href')),
    };
  });
  const expectedPaths = details.map((detail) => detail.path);
  assert.equal(state.hasToggle, true, `Navigation (${viewportName}): Situations-Untermenü fehlt`);
  assert.deepEqual(state.hrefs, expectedPaths, `Navigation (${viewportName}): veröffentlichte Problemsituationen sind nicht vollständig verlinkt`);
}

async function assertHomeJourney(page, viewportName) {
  const state = await page.evaluate(() => ({
    headings: Array.from(document.querySelectorAll('main > section'))
      .map((section) => section.querySelector('h1,h2')?.textContent?.trim() ?? '')
      .slice(0, 10),
    heroCta: {
      text: document.querySelector('main > .hero a.button')?.textContent?.trim() ?? '',
      href: document.querySelector('main > .hero a.button')?.getAttribute('href') ?? '',
    },
    orientationLinks: Array.from(document.querySelectorAll('main > section .mvp-panel--link'))
      .filter((link) => link.closest('section')?.querySelector('h2')?.textContent?.trim() === 'Wie möchtest du starten?')
      .map((link) => link.getAttribute('href')),
    orientationCtas: Array.from(document.querySelectorAll('.chos-orientation .mvp-panel > .mvp-panel__link')).map((cta) => ({
      text: Array.from(cta.children).map((part) => part.textContent?.trim() ?? '').join(' '),
      width: Math.round(cta.getBoundingClientRect().width),
      parentWidth: Math.round(cta.parentElement?.getBoundingClientRect().width ?? 0),
      parentBottom: Math.round(cta.parentElement?.getBoundingClientRect().bottom ?? 0),
      bottom: Math.round(cta.getBoundingClientRect().bottom),
      whiteSpace: getComputedStyle(cta).whiteSpace,
    })),
    orientationTitles: Array.from(document.querySelectorAll('.chos-orientation .mvp-panel h3')).map((title) => ({
      text: title.textContent?.trim() ?? '',
      hyphens: getComputedStyle(title).hyphens,
    })),
    horizontalOverflow: document.documentElement.scrollWidth > document.documentElement.clientWidth + 1,
    renderingError: /RenderingException|FreeMarker|TemplateModelException/.test(document.body.innerText),
  }));
  assert.deepEqual(state.headings, expectedHomeJourney, `Startseite (${viewportName}): ChOS-Reihenfolge stimmt nicht`);
  assert.deepEqual(state.heroCta, { text: 'Typische Situationen ansehen', href: '/probleme' }, `Startseite (${viewportName}): Hero startet nicht bei Situationen`);
  assert.deepEqual(state.orientationLinks, ['/probleme', '/chos-selbstcheck', '/insights'], `Startseite (${viewportName}): Orientierungseinstiege fehlen`);
  assert.deepEqual(state.orientationCtas.map((cta) => cta.text), ['Situationen ansehen →', 'Selbstcheck starten →', 'Insights lesen →'], `Startseite (${viewportName}): CTA-Texte stimmen nicht`);
  assert.equal(state.orientationCtas.length, 3, `Startseite (${viewportName}): drei Orientierungs-CTAs erwartet`);
  assert.equal(state.orientationCtas.every((cta) => cta.whiteSpace === 'nowrap'), true, `Startseite (${viewportName}): CTA bricht um`);
  if (viewportName === 'desktop') {
    assert.ok(Math.max(...state.orientationCtas.map((cta) => cta.width)) - Math.min(...state.orientationCtas.map((cta) => cta.width)) <= 2, `Startseite (${viewportName}): CTA-Breiten sind uneinheitlich`);
    assert.ok(Math.max(...state.orientationCtas.map((cta) => cta.bottom)) - Math.min(...state.orientationCtas.map((cta) => cta.bottom)) <= 2, `Startseite (${viewportName}): CTAs stehen nicht auf einer Grundlinie`);
  } else {
    assert.equal(state.orientationCtas.every((cta) => cta.width <= 320 && cta.width < cta.parentWidth - 32), true, `Startseite (${viewportName}): CTA ist unnötig vollflächig`);
  }
  assert.equal(state.orientationCtas.every((cta) => Math.abs(cta.parentBottom - cta.bottom) > 15), true, `Startseite (${viewportName}): CTA-Abstand zum Kartenrand fehlt`);
  assert.equal(state.orientationTitles.every((title) => title.hyphens === 'none'), true, `Startseite (${viewportName}): Kartenüberschrift wird automatisch getrennt`);
  assert.equal(state.horizontalOverflow, false, `Startseite (${viewportName}): horizontaler Überlauf`);
  assert.equal(state.renderingError, false, `Startseite (${viewportName}): Renderingfehler sichtbar`);
}

async function assertOverview(page, viewportName) {
  const cards = page.locator('.problem-card');
  assert.equal(await cards.count(), 5, `Übersicht (${viewportName}): erwartet fünf Problemkarten`);

  const cardState = await page.evaluate(() => Array.from(document.querySelectorAll('.problem-card')).map((card) => {
    const links = card.querySelectorAll('a');
    const link = card.querySelector('.problem-card__link');
    const overlay = link ? getComputedStyle(link, '::after') : null;
    return {
      links: links.length,
      href: link?.getAttribute('href') ?? '',
      cursor: getComputedStyle(card).cursor,
      transition: getComputedStyle(card).transitionProperty,
      overlayPosition: overlay?.position ?? '',
      overlayInset: overlay?.inset ?? '',
      accessibleName: link?.getAttribute('aria-label') ?? '',
    };
  }));

  for (const card of cardState) {
    assert.equal(card.links, 1, `Übersicht (${viewportName}): Karte benötigt genau einen Link`);
    assert.match(card.href, /^\/probleme\//, `Übersicht (${viewportName}): falsches Linkziel`);
    assert.equal(card.cursor, 'pointer', `Übersicht (${viewportName}): Pointer-Cursor fehlt`);
    assert.match(card.transition, /transform/, `Übersicht (${viewportName}): Hover-Transition fehlt`);
    assert.equal(card.overlayPosition, 'absolute', `Übersicht (${viewportName}): Vollflächenlink fehlt`);
    assert.equal(card.overlayInset, '0px', `Übersicht (${viewportName}): Link überdeckt nicht die ganze Karte`);
    assert.match(card.accessibleName, /^Situation prüfen:/, `Übersicht (${viewportName}): zugänglicher Linkname fehlt`);
  }

  const firstCard = cards.first();
  await firstCard.hover();
  await page.waitForTimeout(250);
  const hovered = await firstCard.evaluate((card) => ({
    transform: getComputedStyle(card).transform,
    shadow: getComputedStyle(card).boxShadow,
  }));
  assert.notEqual(hovered.transform, 'none', `Übersicht (${viewportName}): sichtbarer Hover-Lift fehlt`);
  assert.notEqual(hovered.shadow, 'none', `Übersicht (${viewportName}): Hover-Schatten fehlt`);

  const link = firstCard.locator('.problem-card__link');
  await link.focus();
  assert.equal(await firstCard.evaluate((card) => card.matches(':focus-within')), true, `Übersicht (${viewportName}): Tastaturfokus fehlt`);

  const href = await link.getAttribute('href');
  const box = await firstCard.boundingBox();
  assert.ok(box && href, `Übersicht (${viewportName}): Karte kann nicht angeklickt werden`);
  await Promise.all([
    page.waitForURL(`${baseUrl}${href}`),
    firstCard.click({ position: { x: box.width - 24, y: 24 } }),
  ]);
  assert.equal(new URL(page.url()).pathname, href, `Übersicht (${viewportName}): freie Kartenfläche navigiert nicht`);
}

async function assertDetail(page, expected, viewportName) {
  const state = await page.evaluate(() => {
    const selfCheck = document.querySelector('.problem-selfcheck');
    const intro = selfCheck?.querySelector(':scope > .prose')?.cloneNode(true);
    intro?.querySelector('.eyebrow')?.remove();
    intro?.querySelector('h2')?.remove();
    const introText = intro?.textContent?.replace(/\s+/g, ' ').trim() ?? '';
    const stepTexts = Array.from(selfCheck?.querySelectorAll('.problem-selfcheck__steps section') ?? [])
      .map((step) => step.textContent?.replace(/\s+/g, ' ').trim() ?? '');
    return {
      selfChecks: document.querySelectorAll('.problem-selfcheck').length,
      introText,
      stepTexts,
      checkTitle: selfCheck?.querySelector(':scope > .prose h2')?.textContent?.trim() ?? '',
      stepNumbers: Array.from(selfCheck?.querySelectorAll('.problem-selfcheck__number') ?? [])
        .map((number) => number.textContent?.trim() ?? ''),
      rawMarkdownTable: /\|\s*-{3,}\s*\|/.test(selfCheck?.textContent ?? ''),
      diagnosis: document.querySelector('.problem-mechanisms__intro h2')?.textContent?.trim() ?? '',
      mechanismTitles: Array.from(document.querySelectorAll('.problem-mechanism h3')).map((item) => item.textContent?.trim() ?? ''),
      questions: Array.from(document.querySelectorAll('.problem-questions li')).map((item) => item.textContent?.trim() ?? ''),
      avoidActions: Array.from(document.querySelectorAll('.problem-actions__avoid li')).map((item) => item.textContent?.trim() ?? ''),
      relatedCards: Array.from(document.querySelectorAll('.problem-related__card')).map((card) => ({
        tagName: card.tagName,
        label: card.querySelector('span')?.textContent?.trim() ?? '',
        title: card.querySelector('strong')?.textContent?.trim() ?? '',
        href: card.getAttribute('href') ?? '',
        transition: getComputedStyle(card).transitionProperty,
        cursor: getComputedStyle(card).cursor,
      })),
      globalRelatedInsights: document.querySelectorAll('main > .related-insights').length,
      problemOverviewCurrent: document.querySelector('.site-nav__mobile-overview a[href$="/probleme"]')?.getAttribute('aria-current') ?? null,
      activeProblemDetailHrefs: Array.from(document.querySelectorAll('.site-nav__list--level-2 a[href^="/probleme/"][aria-current="page"]'))
        .map((link) => new URL(link.href).pathname),
    };
  });

  assert.equal(state.selfChecks, 1, `${expected.path} (${viewportName}): Selbstcheck fehlt oder ist doppelt`);
  assert.ok(state.introText.length > 30, `${expected.path} (${viewportName}): Selbstcheck ist leer`);
  assert.equal(state.checkTitle, expected.checkTitle, `${expected.path} (${viewportName}): falscher Check-Titel`);
  assert.equal(state.stepTexts.length, 4, `${expected.path} (${viewportName}): Check braucht vier konkrete Schritte`);
  assert.deepEqual(state.stepNumbers, ['1', '2', '3', '4'], `${expected.path} (${viewportName}): Schrittnummern fehlen`);
  assert.equal(state.rawMarkdownTable, false, `${expected.path} (${viewportName}): rohe Markdown-Tabelle sichtbar`);
  assert.equal(
    state.stepTexts.some((step) => normalizeText(step) === normalizeText(state.introText)),
    false,
    `${expected.path} (${viewportName}): Selbstcheck-Inhalt wird doppelt ausgegeben`,
  );
  assert.equal(state.diagnosis, expected.diagnosis, `${expected.path} (${viewportName}): falsche Diagnoseüberschrift`);
  assert.ok(state.mechanismTitles.length >= 5, `${expected.path} (${viewportName}): Diagnosekarten fehlen`);
  assert.equal(state.mechanismTitles.some((title) => /^\d+\./.test(title)), false, `${expected.path} (${viewportName}): doppelte Nummerierung`);
  assert.deepEqual(state.questions, expected.questions, `${expected.path} (${viewportName}): Diagnosefragen sind nicht redaktionell kuratiert`);
  assert.equal(state.avoidActions.length, expected.avoidCount, `${expected.path} (${viewportName}): Anti-Patterns fehlen`);
  assert.equal(state.avoidActions.every((item) => /^Noch /.test(item)), true, `${expected.path} (${viewportName}): Anti-Patterns sind nicht handlungsorientiert formuliert`);
  assert.deepEqual(state.relatedCards.map(({ label, title, href }) => [label, title, href]), expected.related, `${expected.path} (${viewportName}): Content-Graph ist nicht korrekt verdrahtet`);
  assert.equal(state.globalRelatedInsights, 0, `${expected.path} (${viewportName}): globales Insight-Modul doppelt den kuratierten Content Graph`);
  assert.equal(state.problemOverviewCurrent, null, `${expected.path} (${viewportName}): Situationsübersicht ist auf einer Detailseite fälschlich aktiv`);
  assert.deepEqual(state.activeProblemDetailHrefs, [expected.path], `${expected.path} (${viewportName}): nur die geöffnete Problemsituation darf aktiv sein`);
  for (const card of state.relatedCards) {
    assert.equal(card.tagName, 'A', `${expected.path} (${viewportName}): verwandte Karte ist nicht vollflächig verlinkt`);
    assert.ok(card.href, `${expected.path} (${viewportName}): verwandter Karte fehlt das Linkziel`);
    assert.equal(card.cursor, 'pointer', `${expected.path} (${viewportName}): Pointer-Cursor fehlt`);
    assert.match(card.transition, /transform/, `${expected.path} (${viewportName}): Hover-Transition fehlt`);
  }
}

const sitemapResponse = await fetch(`${baseUrl}/sitemap.xml`);
assert.equal(sitemapResponse.status, 200, 'Sitemap ist nicht erreichbar');
const sitemap = await sitemapResponse.text();
for (const page of [overview, ...details]) {
  assert.ok(sitemap.includes(`<loc>${baseUrl}${page.path}</loc>`), `Sitemap enthält ${page.path} nicht`);
}

const leadershipAliasResponse = await fetch(`${baseUrl}/probleme/neu-in-fuehrung`, { redirect: 'manual' });
assert.ok([301, 308].includes(leadershipAliasResponse.status), 'Alias /probleme/neu-in-fuehrung leitet nicht dauerhaft weiter');
assert.equal(
  new URL(leadershipAliasResponse.headers.get('location'), baseUrl).pathname,
  '/probleme/leadership-transition',
  'Alias /probleme/neu-in-fuehrung zeigt nicht auf die kanonische Problemsituation',
);

const homeResponse = await fetch(`${baseUrl}/`);
assert.equal(homeResponse.status, 200, 'Startseite ist nicht erreichbar');
const homeHtml = await homeResponse.text();
assert.match(homeHtml, /href="\/probleme"[^>]*>Typische Situationen ansehen/, 'Startseite verweist nicht sichtbar auf die Problemsituationen');
assert.match(homeHtml, /Prüfe fünf konkrete Führungs-, Entscheidungs- und Organisationssituationen\./, 'Startseite nennt nicht alle fünf Problemsituationen');

const browser = await chromium.launch({ headless: true });
const results = [];

try {
  for (const viewport of viewports) {
    const page = await browser.newPage({ viewport });
    const pageErrors = [];
    page.on('pageerror', (error) => pageErrors.push(error.message));

    await openPage(page, '/');
    await assertNavigationOrder(page, viewport.name);
    await assertProblemNavigationCoverage(page, viewport.name);
    await assertHomeJourney(page, viewport.name);
    results.push({ viewport: viewport.name, path: '/', status: 'ok' });

    await openPage(page, overview.path);
    await assertCommonPageState(page, overview, viewport.name);
    await assertNavigationOrder(page, viewport.name);
    await assertOverview(page, viewport.name);
    results.push({ viewport: viewport.name, path: overview.path, status: 'ok' });

    for (const detail of details) {
      await openPage(page, detail.path);
      await assertCommonPageState(page, detail, viewport.name);
      await assertDetail(page, detail, viewport.name);
      results.push({ viewport: viewport.name, path: detail.path, status: 'ok' });
    }

    assert.deepEqual(pageErrors, [], `${viewport.name}: JavaScript-Seitenfehler: ${pageErrors.join(' | ')}`);
    await page.close();
  }
} finally {
  await browser.close();
}

console.log(JSON.stringify({
  baseUrl,
  checkedPages: results.length,
  results,
  result: 'Problemseiten-E2E-Test bestanden',
}, null, 2));
