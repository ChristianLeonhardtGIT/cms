import info.magnolia.context.MgnlContext
import groovy.transform.Field
import javax.jcr.Node
import javax.jcr.Session

/*
 * Migriert cleonhardt.de auf die geschärfte ChOS-Positionierung.
 * Wiederholbar: Die betroffenen Seiten werden kontrolliert neu aufgebaut,
 * bestehende Angebotsknoten (inkl. HubSpot-Verknüpfungen) bleiben erhalten.
 */

@Field final String PAGE_TEMPLATE = 'meine-website:pages/home'
@Field final String COMPONENT_PREFIX = 'meine-website:components/'
@Field final String TODAY = '2026-08-08'

Session website = MgnlContext.getJCRSession('website')
Session offers = MgnlContext.getJCRSession('offers')
Session navigation = MgnlContext.getJCRSession('navigation')
Session footer = MgnlContext.getJCRSession('footer')
Session siteSettings = MgnlContext.getJCRSession('siteSettings')

void setProperties(Node node, Map<String, Object> properties) {
    properties.each { String key, Object value ->
        if (value == null) return
        if (value instanceof Boolean) node.setProperty(key, value as boolean)
        else if (value instanceof Long || value instanceof Integer) node.setProperty(key, value as long)
        else if (value instanceof Number) node.setProperty(key, value as double)
        else node.setProperty(key, value.toString())
    }
}

Node ensurePage(Session session, String path, Map<String, Object> properties) {
    String name = path.replaceFirst('^/', '')
    Node page = session.nodeExists(path) ? session.getNode(path) : session.rootNode.addNode(name, 'mgnl:page')
    setProperties(page, [
        'mgnl:template': PAGE_TEMPLATE,
        brandName: 'Christian Leonhardt',
        footerText: 'Klarheit für komplexe Produktorganisationen.',
        hideInNavigation: false,
        dateModified: TODAY
    ] + properties)
    page
}

Node resetArea(Node page) {
    if (page.hasNode('main')) page.getNode('main').remove()
    page.addNode('main', 'mgnl:area')
}

Node addComponent(Node area, String template, Map<String, Object> properties = [:]) {
    String name = String.format('%02d', area.nodes.size)
    Node component = area.addNode(name, 'mgnl:component')
    component.setProperty('mgnl:template', template.contains(':') ? template : COMPONENT_PREFIX + template)
    setProperties(component, properties)
    component
}

Node addHero(Node area, String eyebrow, String title, String description, String target = null, String button = null) {
    Node hero = addComponent(area, 'hero', [eyebrow: eyebrow, title: title, description: description])
    if (target && button) {
        Node chooser = hero.addNode('ctaChooser', 'mgnl:contentNode')
        chooser.setProperty('field', 'withCta')
        chooser.setProperty('ctaText', button)
        Node link = chooser.addNode('ctaLink', 'mgnl:contentNode')
        link.setProperty('field', 'internalPageLink')
        link.setProperty('internalLink', target)
    }
    hero
}

Node addText(Node area, String heading, String html, boolean tinted = false) {
    addComponent(area, 'text', [heading: heading, text: html, showBackground: tinted])
}

Node addCta(Node area, String title, String html, String target, String button) {
    Node cta = addComponent(area, 'callToAction', [title: title, description: html, buttonText: button])
    Node chooser = cta.addNode('pageLinkChooser', 'mgnl:contentNode')
    chooser.setProperty('field', 'internalPageLink')
    chooser.setProperty('internalLink', target)
    cta
}

Node addOfferCatalog(Node area, String heading, String intro, String category, String columns, boolean tinted = false, boolean showFeatures = true) {
    addComponent(area, 'offerCatalog', [
        heading: heading, intro: intro, category: category == 'featured' ? 'all' : category,
        featuredOnly: category == 'featured', columns: columns,
        showFeatures: showFeatures, showPriceNote: true, showBackground: tinted
    ])
}

void setOffer(Node offer, Map<String, Object> data, Session websiteSession) {
    setProperties(offer, data.findAll { key, value -> key != 'features' && key != 'targetPath' })
    offer.setProperty('currency', 'EUR')
    offer.setProperty('taxRate', 19d)
    offer.setProperty('active', data.containsKey('active') ? (data.active as Boolean) : true)
    if (data.targetPath && websiteSession.nodeExists(data.targetPath as String)) {
        offer.setProperty('targetPage', websiteSession.getNode(data.targetPath as String).getIdentifier())
    }
    if (offer.hasNode('features')) offer.getNode('features').remove()
    Node features = offer.addNode('features', 'mgnl:contentNode')
    (data.features as List<String>).eachWithIndex { String text, int index ->
        Node feature = features.addNode(String.format('%02d', index), 'mgnl:contentNode')
        feature.setProperty('text', text)
    }
}

// Neue und bestehende Detailseiten zuerst anlegen, damit Angebote darauf zeigen können.
Map<String, Map<String, Object>> servicePages = [
    '/clarity-session': [
        title: 'ChOS Clarity Session', nav: 'Clarity Session',
        window: 'ChOS Clarity Session - komplexe Situationen strukturiert klären',
        description: 'In 90 Minuten eine konkrete Produkt-, Führungs- oder Organisationssituation strukturieren und den nächsten sinnvollen Schritt bestimmen.',
        eyebrow: '90 Minuten · 390 EUR netto', hero: 'Klarheit, bevor Aktion weitere Komplexität erzeugt.',
        lead: 'Die ChOS Clarity Session ist der fokussierte Einstieg für eine konkrete Situation. Wir trennen Beobachtung, Annahme und Ursache und bestimmen den nächsten sinnvollen Test.',
        situations: '<div class="mvp-grid mvp-grid--3"><article class="mvp-panel"><h3>Festgefahrene Situation</h3><p>Ein Produkt-, Führungs- oder Organisationsthema dreht sich im Kreis.</p></article><article class="mvp-panel"><h3>Viel Aktivität, wenig Wirkung</h3><p>Es wird viel verändert, aber das eigentliche Problem bleibt bestehen.</p></article><article class="mvp-panel"><h3>Unklarer nächster Schritt</h3><p>Mehrere Erklärungen sind plausibel und ein großer Eingriff wäre verfrüht.</p></article></div>',
        included: '<ul class="mvp-checks"><li>Vorbereitender Kurzfragebogen</li><li>90 Minuten persönliches Gespräch</li><li>ChOS-Einordnung entlang der fünf Perspektiven</li><li>Erste Hypothesen und Gegenfragen</li><li>Konkreter nächster Schritt</li><li>Schriftlicher Clarity Brief</li></ul>',
        process: '<div class="mvp-grid mvp-grid--3"><div class="mvp-panel"><span class="mvp-number">1</span><h3>Kontext</h3><p>Sie schildern Situation, Beteiligte und beobachtbare Signale.</p></div><div class="mvp-panel"><span class="mvp-number">2</span><h3>Diagnose</h3><p>Wir prüfen Muster, Annahmen und mögliche Mechanismen.</p></div><div class="mvp-panel mvp-panel--accent"><span class="mvp-number">3</span><h3>Nächster Schritt</h3><p>Sie erhalten eine klare, begrenzte Handlungsempfehlung.</p></div></div>',
        cta: 'Clarity Session anfragen'
    ],
    '/decision-review': [
        title: 'ChOS Decision Review', nav: 'Decision Review',
        window: 'ChOS Decision Review - wichtige Produkt- und Organisationsentscheidungen prüfen',
        description: 'Wichtige Produkt-, Technologie- oder Organisationsentscheidungen vor der Umsetzung unabhängig gegenprüfen.',
        eyebrow: 'Decision Review · 990 EUR netto', hero: 'Teure Entscheidungen verdienen einen unabhängigen Gegencheck.',
        lead: 'Das Decision Review macht Entscheidungslogik, Annahmen, Risiken und Alternativen sichtbar. Ziel ist nicht, Verantwortung abzunehmen, sondern die Qualität der Entscheidung zu erhöhen.',
        situations: '<div class="mvp-grid mvp-grid--3"><article class="mvp-panel"><h3>Produkt und Technologie</h3><p>Plattform, Make-or-Buy, Roadmap oder Investition binden relevantes Budget.</p></article><article class="mvp-panel"><h3>Schwer umkehrbare Entscheidung</h3><p>Die Folgen reichen weit und Annahmen sollten vorab geprüft werden.</p></article><article class="mvp-panel"><h3>Organisation</h3><p>Rollen, Teams oder Entscheidungsrechte sollen neu geordnet werden.</p></article></div>',
        included: '<ul class="mvp-checks"><li>Sichtung vorhandener Unterlagen</li><li>Trennung von Fakten, Annahmen und Bewertungen</li><li>Red-Team-Betrachtung</li><li>Risiken, Alternativen und Umkehrbarkeit</li><li>Persoenliches Review</li><li>Schriftliche Empfehlung</li></ul>',
        process: '<div class="mvp-grid mvp-grid--3"><div class="mvp-panel"><span class="mvp-number">1</span><h3>Entscheidung klären</h3><p>Optionen, Ziel und Entscheidungsrahmen werden präzisiert.</p></div><div class="mvp-panel"><span class="mvp-number">2</span><h3>Annahmen challengen</h3><p>Wir suchen gezielt Gegenbelege, Alternativen und schwer umkehrbare Folgen.</p></div><div class="mvp-panel mvp-panel--accent"><span class="mvp-number">3</span><h3>Empfehlung</h3><p>Sie erhalten eine belastbare Entscheidungsgrundlage.</p></div></div>',
        cta: 'Decision Review anfragen'
    ],
    '/product-organisation-diagnostic': [
        title: 'ChOS Operating Model Diagnostic', nav: 'Operating Model Diagnostic',
        window: 'ChOS Operating Model Diagnostic für Produktorganisationen',
        description: 'Richtung, Entscheidungswege, Verantwortung, Zusammenarbeit und Lernen in einer Produktorganisation systematisch diagnostizieren.',
        eyebrow: 'Diagnostic · 4.900 EUR netto', hero: 'Das System verstehen, bevor Sie Rollen und Strukturen verändern.',
        lead: 'Das ChOS Operating Model Diagnostic untersucht einen klar abgegrenzten Ausschnitt Ihrer Produktorganisation. Es verbindet Dokumente, Interviews und konkrete Entscheidungssituationen zu einem belastbaren Ursachenbild.',
        situations: '<div class="mvp-grid mvp-grid--3"><article class="mvp-panel"><h3>Wiederkehrende Reibung</h3><p>Priorisierung, Eskalationen oder Abhängigkeiten bleiben trotz neuer Prozesse bestehen.</p></article><article class="mvp-panel"><h3>Unklare Verantwortung</h3><p>Mandate und Ergebnisverantwortung passen im Alltag nicht zusammen.</p></article><article class="mvp-panel"><h3>Geplante Veränderung</h3><p>Vor einer Reorganisation soll klar werden, welcher Mechanismus wirklich verändert werden muss.</p></article></div>',
        included: '<ul class="mvp-checks"><li>Auftrags- und Systemgrenzenklärung</li><li>Dokumentenanalyse</li><li>Zwei bis vier fokussierte Interviews</li><li>Auswertung entlang der fünf ChOS-Perspektiven</li><li>Hypothesen, Risiken und Wechselwirkungen</li><li>Priorisierte Interventionen und Ergebnisgespraech</li></ul>',
        process: '<div class="mvp-grid mvp-grid--3"><div class="mvp-panel"><span class="mvp-number">1</span><h3>Abgrenzen</h3><p>Untersuchungsfrage, Systemgrenze und Daten werden festgelegt.</p></div><div class="mvp-panel"><span class="mvp-number">2</span><h3>Diagnostizieren</h3><p>Beobachtungen werden zu prüfbaren Hypothesen verdichtet.</p></div><div class="mvp-panel mvp-panel--accent"><span class="mvp-number">3</span><h3>Priorisieren</h3><p>Sie erhalten ein Ursachenbild und eine sinnvolle Reihenfolge von Eingriffen.</p></div></div>',
        cta: 'Diagnostic anfragen'
    ],
    '/ai-operating-model-assessment': [
        title: 'AI Operating Model Assessment', nav: 'AI Operating Model Assessment',
        window: 'AI Operating Model Assessment - Rollen, Governance und Decision Rights',
        description: 'Prüfen, ob Rollen, Entscheidungsrechte, Governance und Zusammenarbeit für AI und autonome Agents tragfähig gestaltet sind.',
        eyebrow: 'Assessment · 9.900 EUR netto', hero: 'AI verändert Arbeit. Ist Ihr Operating Model darauf vorbereitet?',
        lead: 'Das Assessment untersucht nicht nur Technologie-Readiness. Es klärt, wo AI Wert erzeugen soll, wie Menschen und Agents Entscheidungen teilen und wer Ergebnis, Kontrolle und Eskalation verantwortet.',
        situations: '<div class="mvp-grid mvp-grid--3"><article class="mvp-panel"><h3>Vom Pilot in den Betrieb</h3><p>AI funktioniert technisch, aber Rollen, Freigaben und Ownership bleiben offen.</p></article><article class="mvp-panel"><h3>Agentische Workflows</h3><p>Agents sollen Aufgaben oder Entscheidungen eigenständiger übernehmen.</p></article><article class="mvp-panel"><h3>Governance-Lücke</h3><p>Human Oversight, Eskalation, Auditierbarkeit und Ergebnisverantwortung sind nicht konsistent.</p></article></div>',
        included: '<ul class="mvp-checks"><li>Value- und Use-Case-Klärung</li><li>Human/AI/Agent Decision Rights</li><li>Rollen, Accountability und Ownership</li><li>Human Oversight und Eskalationswege</li><li>Zusammenarbeits- und Lernmechanismen</li><li>Readiness-Bild und priorisierte Handlungsfelder</li></ul>',
        process: '<div class="mvp-grid mvp-grid--3"><div class="mvp-panel"><span class="mvp-number">1</span><h3>Wertbeitrag</h3><p>Wir klären, welche Wirkung AI im betrachteten System erzeugen soll.</p></div><div class="mvp-panel"><span class="mvp-number">2</span><h3>Operating Model</h3><p>Rollen, Entscheidungen, Kontrolle und Zusammenarbeit werden untersucht.</p></div><div class="mvp-panel mvp-panel--accent"><span class="mvp-number">3</span><h3>Readiness Plan</h3><p>Sie erhalten ein Zielbild und priorisierte Veränderungen.</p></div></div>',
        cta: 'Assessment anfragen'
    ],
    '/ai-enabled-workflow-sprint': [
        title: 'AI-enabled Workflow / Product Sprint', nav: 'AI-enabled Workflow Sprint',
        window: 'AI-enabled Workflow Sprint - Human-AI-Zusammenarbeit neu gestalten',
        description: 'Einen konkreten Workflow oder Product Value Stream für wirksame Human-AI-Zusammenarbeit neu gestalten.',
        eyebrow: 'Design Sprint · 19.500 EUR netto', hero: 'Nicht AI auf den alten Prozess setzen. Den Workflow neu denken.',
        lead: 'Der Sprint gestaltet einen relevanten Workflow end-to-end neu: Aufgaben, Entscheidungen, Human Reviews, Agent-Autonomie, Verantwortung, Eskalation und Wirkungsmessung.',
        situations: '<div class="mvp-grid mvp-grid--3"><article class="mvp-panel"><h3>Langsamer Value Stream</h3><p>Viele Übergaben, Freigaben und manuelle Analysen bremsen die Wirkung.</p></article><article class="mvp-panel"><h3>Unklare Human-AI-Handoffs</h3><p>AI erstellt Ergebnisse, aber Review, Entscheidung und Verantwortung sind unscharf.</p></article><article class="mvp-panel"><h3>Lokale Automatisierung</h3><p>Einzelne Schritte sind schneller, der Gesamtprozess aber nicht.</p></article></div>',
        included: '<ul class="mvp-checks"><li>Abgrenzung eines relevanten Workflows oder Value Streams</li><li>Ist-Bild mit Reibung, Entscheidungen und Übergaben</li><li>Human/AI/Agent-Aufgabenteilung</li><li>Decision Rights, Reviews und Eskalationen</li><li>Target Workflow und Pilotdesign</li><li>Messkriterien und Umsetzungs-Roadmap</li></ul>',
        process: '<div class="mvp-grid mvp-grid--3"><div class="mvp-panel"><span class="mvp-number">1</span><h3>Verstehen</h3><p>Wir untersuchen Wert, Engpässe und Entscheidungslogik im Ist-Zustand.</p></div><div class="mvp-panel"><span class="mvp-number">2</span><h3>Designen</h3><p>Der Workflow wird mit klaren Human-AI-Handoffs neu gestaltet.</p></div><div class="mvp-panel mvp-panel--accent"><span class="mvp-number">3</span><h3>Testen</h3><p>Ein Pilot prüft Wirkung, Qualität und Risiken unter realen Bedingungen.</p></div></div>',
        cta: 'Workflow Sprint anfragen'
    ],
    '/chos-transformation-program': [
        title: 'ChOS Transformation Program', nav: 'ChOS Transformation Program',
        window: 'ChOS Transformation Program - Operating Models wirksam verändern',
        description: 'Produktorganisationen von der Diagnose über das Target Operating Model bis zur Umsetzung und Wirkungsmessung begleiten.',
        eyebrow: 'Transformation · 30.000-150.000 EUR+ netto', hero: 'Diagnose. Design. Veränderung.',
        lead: 'Das ChOS Transformation Program begleitet eine größere Organisationseinheit von einem belastbaren Ursachenbild über das Target Operating Model bis zu Umsetzung, Lernschleifen und Wirkungsmessung.',
        situations: '<div class="mvp-grid mvp-grid--3"><article class="mvp-panel"><h3>Operating-Model-Redesign</h3><p>Strategie, Wertströme, Entscheidungen und Organisation passen nicht mehr zusammen.</p></article><article class="mvp-panel"><h3>AI-bedingte Transformation</h3><p>Rollen, Workflows und Führung müssen für Human-AI-Zusammenarbeit neu gestaltet werden.</p></article><article class="mvp-panel"><h3>Wiederkehrende Symptome</h3><p>Frühere Reorganisationen haben Strukturen geändert, aber die Muster nicht.</p></article></div>',
        included: '<ul class="mvp-checks"><li>ChOS-Diagnose und gemeinsames Ursachenbild</li><li>Target Operating Model</li><li>Decision Rights und Verantwortungsarchitektur</li><li>Intervention Design und Umsetzungs-Roadmap</li><li>Führungs- und Teamarbeit in der Veränderung</li><li>Wirkungsmessung und iterative Anpassung</li></ul>',
        process: '<div class="mvp-grid mvp-grid--3"><div class="mvp-panel"><span class="mvp-number">1</span><h3>Diagnose</h3><p>Die Organisation entwickelt ein belastbares Bild der wirkenden Mechanismen.</p></div><div class="mvp-panel"><span class="mvp-number">2</span><h3>Design</h3><p>Struktur, Entscheidungsrechte, Verantwortung und Zusammenarbeit werden neu gestaltet.</p></div><div class="mvp-panel mvp-panel--accent"><span class="mvp-number">3</span><h3>Veränderung</h3><p>Interventionen werden umgesetzt, gemessen und bewusst angepasst.</p></div></div>',
        cta: 'Transformation besprechen'
    ],
    '/executive-sparring': [
        title: 'Executive / Product Leadership Sparring', nav: 'Executive Sparring',
        window: 'Executive und Product Leadership Sparring für komplexe Entscheidungen',
        description: 'Vertrauliches, wiederkehrendes Sparring für Product Leader und Führungskräfte in komplexen Produkt- und Organisationsfragen.',
        eyebrow: 'Retainer · 1.750 EUR netto pro Monat', hero: 'Ein unabhängiger Denkraum für Entscheidungen, die Sie nicht delegieren können.',
        lead: 'Das Sparring verbindet konkrete Entscheidungsarbeit mit dem Blick auf wiederkehrende Muster in Führung und Produktorganisation.',
        situations: '<div class="mvp-grid mvp-grid--3"><article class="mvp-panel"><h3>Wichtige Entscheidungen</h3><p>Strategie, Organisation, Technologie und Menschen greifen gleichzeitig ineinander.</p></article><article class="mvp-panel"><h3>Product Leadership</h3><p>Prioritäten, Stakeholder und Ergebnisverantwortung müssen wiederholt ausbalanciert werden.</p></article><article class="mvp-panel"><h3>Veränderung führen</h3><p>Neue Rollen, Strukturen oder Arbeitsweisen verlangen kontinuierliche Urteilskraft.</p></article></div>',
        included: '<ul class="mvp-checks"><li>Zwei bis vier persönliche Sessions pro Monat</li><li>Asynchrone Rückfragen im vereinbarten Kanal</li><li>Decision Reviews light</li><li>Situations- und Systemanalyse</li><li>Monatliche Reflexion wiederkehrender Muster</li><li>Priorisierte nächste Schritte</li></ul>',
        process: '<div class="mvp-grid mvp-grid--3"><div class="mvp-panel"><span class="mvp-number">1</span><h3>Fokus</h3><p>Rolle, aktuelle Spannungen und Arbeitsmodus werden geklärt.</p></div><div class="mvp-panel"><span class="mvp-number">2</span><h3>Sparring</h3><p>Konkrete Entscheidungen werden vorbereitet und gegengeprüft.</p></div><div class="mvp-panel mvp-panel--accent"><span class="mvp-number">3</span><h3>Lernschleife</h3><p>Wiederkehrende Muster werden zu Führungsprinzipien verdichtet.</p></div></div>',
        cta: 'Sparring anfragen'
    ]
]

servicePages.each { String path, Map<String, Object> data ->
    Node page = ensurePage(website, path, [
        title: data.title, navigationTitle: data.nav, windowTitle: data.window,
        metaDescription: data.description, hideInNavigation: true,
        serviceType: data.title, serviceAudience: 'Product Leaders, Führungsteams und digitale Organisationen'
    ])
    Node area = resetArea(page)
    addHero(area, data.eyebrow as String, data.hero as String, data.description as String)
    addText(area, 'Kurz erklärt', "<p class=\"lead\">${data.lead}</p>", true)
    addText(area, 'Wann dieses Format sinnvoll ist', data.situations as String)
    addText(area, 'Die Leistung auf einen Blick', """
<div class="mvp-grid mvp-grid--2">
  <article class="mvp-panel mvp-panel--accent">
    <p class="mvp-meta">${data.eyebrow}</p>
    <h3>Enthalten</h3>
    ${data.included}
  </article>
  <article class="mvp-panel">
    <h3>Das Ergebnis</h3>
    <p>${data.lead}</p>
    <p><strong>Sie erhalten einen klaren, nachvollziehbaren nächsten Schritt - passend zu Ihrer konkreten Situation.</strong></p>
  </article>
</div>""", true)
    addText(area, 'So läuft die Zusammenarbeit ab', data.process as String)
    addText(area, 'Klare Abgrenzung', '<p>ChOS ersetzt keine Rechts-, Steuer-, Sicherheits- oder technische Spezialprüfung. Für rechtliche AI-Compliance oder technische Implementierung binden wir bei Bedarf passende Spezialisten ein.</p>', true)
    addCta(area, 'Passt das zu Ihrer Situation?', '<p>Beschreiben Sie kurz Kontext und gewünschtes Ergebnis. Ich prüfe persönlich, welches Format den sinnvollsten Einstieg bietet.</p>', '/kontakt', data.cta as String)
}

// Startseite
Node home = ensurePage(website, '/start', [
    title: 'Start', navigationTitle: 'Start',
    windowTitle: 'Christian Leonhardt - Klarheit für komplexe Produktorganisationen',
    metaDescription: 'ChOS hilft Produktorganisationen, Richtung, Entscheidungen, Verantwortung, Zusammenarbeit und Lernen klar zu gestalten.',
    hideInNavigation: false
])
Node homeArea = resetArea(home)
addHero(homeArea, 'Product Leadership · Organisation · Veränderung',
    'Klarheit für komplexe Produktorganisationen.',
    'Ich helfe Führungsteams, schwierige Situationen zu verstehen, Entscheidungen zu klären und Verantwortung wirksam zu gestalten.',
    '/leistungen', 'Leistungen ansehen')
addText(homeArea, 'Mehr Technik löst keine unklaren Strukturen.', '''
<div class="section-intro"><p class="lead">Neue Werkzeuge können Arbeit beschleunigen. Sie lösen aber keine unklaren Ziele, langen Entscheidungswege oder fehlende Verantwortung.</p></div>
<div class="mvp-grid mvp-grid--3">
  <article class="mvp-panel"><h3>Unklare Richtung</h3><p>Teams arbeiten viel, aber nicht sichtbar auf dasselbe Ergebnis hin.</p></article>
  <article class="mvp-panel"><h3>Langsame Entscheidungen</h3><p>Zu viele Beteiligte, Freigaben und Eskalationen bremsen die Arbeit.</p></article>
  <article class="mvp-panel"><h3>Verteilte Verantwortung</h3><p>Viele wirken mit, aber niemand besitzt Ergebnis und Konsequenzen vollständig.</p></article>
</div>''', true)
addText(homeArea, 'Erst verstehen. Dann wirksam verändern - ChOS.', '''
<p class="lead">ChOS hilft, eine Organisation zuerst zu verstehen. Dafür betrachten wir fünf einfache Perspektiven.</p>
<div class="mvp-grid mvp-grid--3">
  <article class="mvp-panel"><p class="mvp-meta">01</p><h3>Richtung</h3><p>Was soll für Kunden und Unternehmen erreicht werden?</p></article>
  <article class="mvp-panel"><p class="mvp-meta">02</p><h3>Entscheidungen</h3><p>Wer entscheidet was und auf welcher Grundlage?</p></article>
  <article class="mvp-panel"><p class="mvp-meta">03</p><h3>Verantwortung</h3><p>Wer besitzt Ergebnis, Kontrolle und Konsequenzen?</p></article>
  <article class="mvp-panel"><p class="mvp-meta">04</p><h3>Zusammenarbeit</h3><p>Wie arbeiten Teams und Führung im Alltag zusammen?</p></article>
  <article class="mvp-panel mvp-panel--accent"><p class="mvp-meta">05</p><h3>Lernen</h3><p>Wie wird sichtbar, was funktioniert und was nicht?</p></article>
</div>
<p><a href="/chos">ChOS kennenlernen -></a></p>''')
addText(homeArea, 'Mit einer konkreten Situation starten.', '''
<div class="mvp-grid mvp-grid--2">
  <article class="mvp-panel mvp-panel--accent">
    <p class="mvp-meta">90 Minuten · 390 EUR netto*</p>
    <h3>ChOS Clarity Session</h3>
    <p>Eine konkrete Produkt-, Führungs- oder Organisationssituation strukturiert verstehen.</p>
    <h4>Enthalten</h4>
    <ul class="mvp-checks"><li>Vorbereitender Kurzfragebogen</li><li>90 Minuten persönliches Gespräch</li><li>ChOS-Einordnung</li><li>Hypothesen und nächster Schritt</li><li>Schriftlicher Clarity Brief</li></ul>
    <p class="mvp-panel__action"><a class="mvp-offer-cta" href="/clarity-session">Clarity Session ansehen <span aria-hidden="true">→</span></a></p>
  </article>
  <article class="mvp-panel">
    <p class="mvp-meta">Monatlich · 1.750 EUR netto*</p>
    <h3>Executive / Product Leadership Sparring</h3>
    <p>Ein vertraulicher und unabhängiger Denkraum für Führungskräfte mit komplexen Entscheidungen.</p>
    <h4>Enthalten</h4>
    <ul class="mvp-checks"><li>Zwei bis vier Sessions pro Monat</li><li>Asynchrone Rückfragen</li><li>Gegenprüfung wichtiger Entscheidungen</li><li>Situations- und Systemanalyse</li><li>Monatliche Lernschleife</li></ul>
    <p class="mvp-panel__action"><a class="mvp-offer-cta" href="/executive-sparring">Executive Sparring ansehen <span aria-hidden="true">→</span></a></p>
  </article>
</div>
<p class="mvp-fineprint">* Alle Preise verstehen sich netto zzgl. der gesetzlichen Umsatzsteuer.</p>''', true)
addText(homeArea, 'Diagnose -> Design -> Veränderung', '''
<div class="mvp-grid mvp-grid--3">
  <article class="mvp-panel"><span class="mvp-number">1</span><h3>Diagnose</h3><p>Beobachtungen, Muster und Mechanismen belastbar unterscheiden.</p></article>
  <article class="mvp-panel"><span class="mvp-number">2</span><h3>Design</h3><p>Entscheidungsrechte, Verantwortung, Workflows und Zusammenarbeit passend gestalten.</p></article>
  <article class="mvp-panel mvp-panel--accent"><span class="mvp-number">3</span><h3>Veränderung</h3><p>Begrenzte Interventionen umsetzen, Wirkung messen und bewusst lernen.</p></article>
</div>''', true)
addOfferCatalog(homeArea, 'Weitere passende Angebote', '<p class="lead">Für Entscheidungen, wiederkehrende Muster oder größere Veränderungen.</p>', 'featured', '2', false, false)
addComponent(homeArea, 'profileTrust', [
    heading: 'Product Leadership, Organisation und Transformation',
    eyebrow: 'Christian Leonhardt',
    title: 'Erfahrung aus Produktverantwortung und Führung',
    lead: 'Ich verbinde Product Leadership, Organisationsdiagnose und Transformation mit einem systemischen Blick auf Entscheidungen, Verantwortung und Zusammenarbeit.',
    body: '<p>Meine Erfahrung reicht von digitalen Produkt- und Plattformorganisationen über Loyalty, CRM und E-Commerce bis zur Stabilisierung und Neuordnung komplexer Programme.</p>',
    topics: 'Product Organizations, Operating Models, Leadership, Transformation',
    signature: 'Erst verstehen. Dann wirksam verändern.',
    targetPage: '/ueber-mich', linkText: 'Mehr über meinen Hintergrund'
])
addCta(homeArea, 'Welche Situation möchten Sie klären?', '<p>Ein konkretes Problem oder eine wichtige Entscheidung reicht für den Einstieg.</p>', '/kontakt', 'Situation schildern')

// Leistungsübersicht
Node services = ensurePage(website, '/leistungen', [
    title: 'Leistungen', navigationTitle: 'Leistungen',
    windowTitle: 'Leistungen - ChOS für Produktorganisationen',
    metaDescription: 'ChOS-Leistungen für klare Entscheidungen, tragfähige Verantwortung und wirksame Veränderungen in Produktorganisationen.',
    hideInNavigation: false
])
Node servicesArea = resetArea(services)
addHero(servicesArea, 'Leistungen', 'Vom konkreten Problem zum wirksamen Operating Model.',
    'Die ChOS-Angebotsarchitektur verbindet Diagnose, Design und Veränderung. Sie können klein starten und nur dann größer werden, wenn die Diagnose es rechtfertigt.',
    '/kontakt', 'Passenden Einstieg klären')
addOfferCatalog(servicesArea, 'Fokussierter Einstieg', '<p class="lead">Eine konkrete Situation oder wichtige Entscheidung klar abgrenzen und belastbar prüfen.</p>', 'entry', '2')
addOfferCatalog(servicesArea, 'Leadership Sparring', '<p class="lead">Kontinuierlicher, vertraulicher Denkraum für Product Leaders und Executives.</p>', 'sparring', '1', true)
addOfferCatalog(servicesArea, 'Diagnose', '<p class="lead">Strukturen, Entscheidungswege und Verantwortung systematisch untersuchen.</p>', 'diagnostic', '2', true)
addOfferCatalog(servicesArea, 'Design und Transformation', '<p class="lead">Einen Workflow neu gestalten oder eine größere Organisationseinheit von Diagnose bis Wirkung begleiten.</p>', 'transformation', '2')
addText(servicesArea, 'Welche Stufe passt?', '<div class="mvp-grid mvp-grid--3"><article class="mvp-panel"><h3>Eine Situation oder Entscheidung</h3><p>Clarity Session oder Decision Review.</p></article><article class="mvp-panel"><h3>Kontinuierliche Führungsfragen</h3><p>Executive / Product Leadership Sparring.</p></article><article class="mvp-panel mvp-panel--accent"><h3>Ein Team oder ein Ablauf</h3><p>ChOS Diagnostic, Assessment oder Workflow Sprint.</p></article></div>', true)
addCta(servicesArea, 'Sie müssen das Format nicht vorab kennen.', '<p>Beschreiben Sie Situation und gewünschtes Ergebnis. Ich empfehle den kleinsten sinnvollen Einstieg.</p>', '/kontakt', 'Situation klären')

// ChOS-Seite
Node chos = ensurePage(website, '/chos', [
    title: 'ChOS', navigationTitle: 'ChOS',
    windowTitle: 'ChOS - Erst verstehen. Dann wirksam verändern.',
    metaDescription: 'ChOS betrachtet Richtung, Entscheidungen, Verantwortung, Zusammenarbeit und Lernen in komplexen Produktorganisationen.',
    hideInNavigation: false
])
Node chosArea = resetArea(chos)
addHero(chosArea, 'Christian Operating System', 'Erst verstehen. Dann wirksam verändern.',
    'ChOS ist ein einfacher Rahmen, um komplexe Produktorganisationen zu verstehen und gezielt zu verändern.',
    '/chos-selbstcheck', 'ChOS Selbstcheck starten')
addText(chosArea, 'Der Leitsatz', '<blockquote class="mvp-quote">Erst verstehen. Dann wirksam verändern.</blockquote><p>Neue Methoden, Strukturen oder Werkzeuge helfen nur, wenn das eigentliche Problem verstanden ist. Deshalb beginnt ChOS mit Beobachtungen und konkreten Situationen - nicht mit einer fertigen Lösung.</p>', true)
addText(chosArea, 'Die fünf Perspektiven', '''
<div class="mvp-grid mvp-grid--3">
  <article class="mvp-panel"><h3>Richtung</h3><p>Welches Ergebnis zählt wirklich?</p><p class="mvp-meta">Ziele · Prioritäten · Wert</p></article>
  <article class="mvp-panel"><h3>Entscheidungen</h3><p>Wer entscheidet was - und mit welchen Informationen?</p><p class="mvp-meta">Mandat · Klarheit · Geschwindigkeit</p></article>
  <article class="mvp-panel"><h3>Verantwortung</h3><p>Wer trägt das Ergebnis und seine Folgen?</p><p class="mvp-meta">Verantwortung · Kontrolle · Eskalation</p></article>
  <article class="mvp-panel"><h3>Zusammenarbeit</h3><p>Wie greifen Teams und Führung ineinander?</p><p class="mvp-meta">Übergaben · Konflikte · Koordination</p></article>
  <article class="mvp-panel mvp-panel--accent"><h3>Lernen</h3><p>Wie wird Wirkung sichtbar und Verbesserung möglich?</p><p class="mvp-meta">Feedback · Wirkung · Anpassung</p></article>
</div>''')
addText(chosArea, 'Vom Signal zur Veränderung', '<div class="mvp-grid mvp-grid--3"><article class="mvp-panel"><span class="mvp-number">1</span><h3>Diagnose</h3><p>Beobachtbare Signale, Muster und plausible Mechanismen prüfen.</p></article><article class="mvp-panel"><span class="mvp-number">2</span><h3>Design</h3><p>Einen passenden Zielzustand für Entscheidungen, Verantwortung und Zusammenarbeit entwickeln.</p></article><article class="mvp-panel mvp-panel--accent"><span class="mvp-number">3</span><h3>Veränderung</h3><p>Interventionen bewusst testen, Wirkung messen und das System anpassen.</p></article></div>', true)
addText(chosArea, 'Was ChOS nicht ist', '<div class="mvp-grid mvp-grid--3"><article class="mvp-panel"><h3>Kein Reifegradmodell</h3><p>Es gibt keinen universellen Idealzustand für jede Organisation.</p></article><article class="mvp-panel"><h3>Keine fertige Schablone</h3><p>ChOS beginnt nicht mit einem Organigramm oder einer Standardlösung.</p></article><article class="mvp-panel"><h3>Keine Standardreorganisation</h3><p>Strukturen folgen der Diagnose - nicht einem vorgefertigten Plan.</p></article></div>')
addCta(chosArea, 'ChOS auf Ihre Organisation anwenden.', '<p>Starten Sie mit dem Selbstcheck, einer Clarity Session oder einem Operating Model Diagnostic.</p>', '/leistungen', 'Leistungen ansehen')

// Über-mich-Seite
Node about = ensurePage(website, '/ueber-mich', [
    title: 'Über Christian', navigationTitle: 'Über mich',
    windowTitle: 'Christian Leonhardt - Product Leadership und Organisationsdiagnose',
    metaDescription: 'Christian Leonhardt verbindet Product Leadership, Führung und Organisationsdiagnose für komplexe Produktorganisationen.',
    hideInNavigation: false
])
Node aboutArea = resetArea(about)
addHero(aboutArea, 'Über mich', 'Organisationen verändern sich nicht durch Folien. Sondern durch bessere Entscheidungen.',
    'Ich verbinde Produktverantwortung, Führung und Organisationsdiagnose mit der Frage, wie Menschen und Teams unter realen Bedingungen wirksam zusammenarbeiten.',
    '/kontakt', 'Kennenlernen')
addComponent(aboutArea, 'profileTrust', [
    heading: 'Christian Leonhardt', eyebrow: 'Product Leadership · Operating Models · Transformation',
    title: 'Organisationen für wirksame Entscheidungen gestalten',
    lead: 'Meine Arbeit liegt an den Übergängen: zwischen Strategie und Umsetzung, Produkt und Technologie, Verantwortung und formaler Struktur.',
    body: '<p>Ich habe digitale Produkt- und Plattformorganisationen aufgebaut und weiterentwickelt, kritische Programme stabilisiert und internationale Stakeholder in Situationen zusammengebracht, in denen Entscheidungen unter Unsicherheit getroffen werden mussten.</p><p>Neue Technologien gehören zu diesem Wandel. Entscheidend bleibt jedoch, wie Ziele, Entscheidungen, Verantwortung und Zusammenarbeit gestaltet sind.</p>',
    topics: 'Product Organizations, Operating Models, Leadership, Transformation',
    signature: 'Diagnose vor Eingriff. Klarheit vor Aktion.', aboutMode: true
])
addText(aboutArea, 'Erfahrung, die Strategie und Alltag verbindet', '<div class="mvp-grid mvp-grid--3"><article class="mvp-panel"><h3>Product Organizations</h3><p>Digitale Produkte, Loyalty, CRM, E-Commerce, Plattformen und Roadmaps.</p></article><article class="mvp-panel mvp-panel--accent"><h3>Leadership und Transformation</h3><p>Führung, Stabilisierung, Entscheidungen und bereichsübergreifende Veränderung.</p></article><article class="mvp-panel"><h3>Operating Models</h3><p>Richtung, Verantwortung, Zusammenarbeit, Abläufe und organisatorische Klarheit.</p></article></div>', true)
addText(aboutArea, 'Mein Arbeitsprinzip', '<blockquote class="mvp-quote">Erst verstehen. Dann wirksam verändern.</blockquote><p>Ich beginne nicht mit einem Blueprint. Ich kläre die Untersuchungsfrage, suche beobachtbare Muster und prüfe mehrere plausible Erklärungen. Erst daraus entstehen Designentscheidungen und Interventionen, die zum Kontext passen.</p><div class="mvp-grid mvp-grid--3"><article class="mvp-panel"><h3>Klarheit</h3><p>Komplexität auf ihren entscheidbaren Kern reduzieren, ohne sie kleinzureden.</p></article><article class="mvp-panel"><h3>Verantwortung</h3><p>Mandat, Ergebnis und Konsequenzen wieder zusammenführen.</p></article><article class="mvp-panel"><h3>Pragmatismus</h3><p>Den kleinsten sinnvollen Eingriff wählen, der echtes Lernen ermöglicht.</p></article></div>')
addCta(aboutArea, 'Lassen Sie uns über Ihr Operating Model sprechen.', '<p>Ein konkretes Problem oder eine anstehende Entscheidung reicht für den Einstieg.</p>', '/kontakt', 'Situation klären')

// Insights-Übersicht und drei neue AI-/Operating-Model-Artikel.
Node insights = ensurePage(website, '/insights', [
    title: 'Insights', navigationTitle: 'Insights',
    windowTitle: 'Insights zu Product Leadership und Organisation',
    metaDescription: 'Klare Gedanken zu Product Leadership, Entscheidungen, Verantwortung, Zusammenarbeit und Veränderung.',
    hideInNavigation: false
])
Node insightsArea = resetArea(insights)
addHero(insightsArea, 'Insights', 'Produktorganisationen besser verstehen.',
    'Klare Gedanken zu Richtung, Entscheidungen, Verantwortung, Zusammenarbeit und Veränderung.')
addText(insightsArea, 'Drei einfache Fragen', '<div class="mvp-grid mvp-grid--3"><article class="mvp-panel"><h3>Was soll erreicht werden?</h3><p>Arbeit wird erst wirksam, wenn das gemeinsame Ergebnis klar ist.</p></article><article class="mvp-panel"><h3>Wer entscheidet und verantwortet?</h3><p>Entscheidung und Verantwortung müssen zusammenpassen.</p></article><article class="mvp-panel mvp-panel--accent"><h3>Was lernen wir daraus?</h3><p>Wirkung und Fehler müssen sichtbar werden, damit Verbesserung möglich ist.</p></article></div>', true)
addText(insightsArea, 'Aktueller Schwerpunkt', '''
<article class="mvp-panel mvp-panel--accent">
  <p class="mvp-meta">Abläufe · Automatisierung</p>
  <h3><a href="/insights/schlechte-prozesse-nicht-nur-schneller-machen">Schlechte Prozesse nicht nur schneller machen</a></h3>
  <p class="lead">Warum ein schneller Einzelschritt noch keinen guten Gesamtprozess ergibt – und wie sich echte Wirkung von lokaler Effizienz unterscheiden lässt.</p>
  <p><a href="/insights/schlechte-prozesse-nicht-nur-schneller-machen">Artikel lesen -></a></p>
</article>''', true)
addText(insightsArea, 'Alle Insights', '''
<div class="mvp-article-list">
  <article><p class="mvp-meta">Product Leadership</p><h3><a href="/insights/warum-produktorganisationen-nicht-an-fehlenden-methoden-scheitern">Warum Produktorganisationen nicht an fehlenden Methoden scheitern</a></h3><p>Warum zusätzliche Frameworks strukturelle Probleme selten lösen.</p></article>
  <article><p class="mvp-meta">Organisationsdiagnose</p><h3><a href="/insights/diagnose-vor-eingriff">Diagnose vor Eingriff</a></h3><p>Wie aus beobachtbaren Signalen überprüfbare Hypothesen und sinnvolle Eingriffe werden.</p></article>
  <article><p class="mvp-meta">Führung</p><h3><a href="/insights/unklare-rollen-sind-selten-das-eigentliche-problem">Unklare Rollen sind selten das eigentliche Problem</a></h3><p>Warum Rollenbeschreibungen ohne Mandat, Information und passende Anreize wenig verändern.</p></article>
  <article><p class="mvp-meta">Product Operating Model</p><h3><a href="/insights/rollen-und-verantwortlichkeiten-in-produktorganisationen-klaeren">Rollen und Verantwortlichkeiten in Produktorganisationen klären</a></h3><p>Wie konkrete Entscheidungsrechte mehr Klarheit schaffen als eine weitere Rollenmatrix.</p></article>
  <article><p class="mvp-meta">Organisation</p><h3><a href="/insights/wann-braucht-eine-produktorganisation-ein-operating-model">Wann braucht eine Produktorganisation ein Operating Model?</a></h3><p>Signale, Gestaltungsfragen und Grenzen eines bewussten Redesigns.</p></article>
  <article><p class="mvp-meta">Decision Review</p><h3><a href="/insights/annahmen-vor-einer-produktentscheidung-pruefen">Annahmen vor einer Produktentscheidung prüfen</a></h3><p>Ein praktischer Red-Team-Ansatz für wichtige und schwer umkehrbare Entscheidungen.</p></article>
  <article><p class="mvp-meta">Organisationsdiagnose</p><h3><a href="/insights/organisationsdiagnose-statt-standardberatung">Organisationsdiagnose statt Standardberatung</a></h3><p>Wann bekannte Lösungen helfen – und wann zuerst der Mechanismus verstanden werden muss.</p></article>
  <article><p class="mvp-meta">ChOS Diagnostic</p><h3><a href="/insights/product-organisation-diagnostic-ablauf-und-ergebnis">Product Organisation Diagnostic: Ablauf und Ergebnis</a></h3><p>Von der Untersuchungsfrage bis zum priorisierten Handlungsbild.</p></article>
  <article><p class="mvp-meta">Leadership</p><h3><a href="/insights/wann-ist-executive-sparring-sinnvoll">Wann ist Executive Sparring sinnvoll?</a></h3><p>Wie unabhängiges Sparring die Qualität komplexer Führungsentscheidungen verbessert.</p></article>
  <article><p class="mvp-meta">Operating Model</p><h3><a href="/insights/warum-ai-einfuehrung-ein-operating-model-thema-ist">Warum AI-Einführung ein Operating-Model-Thema ist</a></h3><p>Wie neue Technik Entscheidungswege, Verantwortung und Zusammenarbeit verändert.</p></article>
  <article><p class="mvp-meta">Entscheidungen</p><h3><a href="/insights/decision-rights-zwischen-mensch-und-ai">Entscheidungen zwischen Mensch und AI klären</a></h3><p>Ein praktischer Rahmen für Autonomie, Kontrolle und menschliche Verantwortung.</p></article>
  <article><p class="mvp-meta">Abläufe</p><h3><a href="/insights/schlechte-prozesse-nicht-nur-schneller-machen">Schlechte Prozesse nicht nur schneller machen</a></h3><p>Den Gesamtprozess verbessern, statt einen Engpass nur zu verschieben.</p></article>
</div>''')
addCta(insightsArea, 'Ein Muster kommt Ihnen bekannt vor?', '<p>Übertragen wir es in einer Clarity Session oder einem Assessment auf Ihre konkrete Situation.</p>', '/leistungen', 'Passenden Einstieg ansehen')

Map<String, Map<String, String>> newInsights = [
    'warum-ai-einfuehrung-ein-operating-model-thema-ist': [
        title: 'Warum AI-Einführung ein Operating-Model-Thema ist', nav: 'AI als Operating-Model-Thema',
        description: 'Warum AI nicht nur Technologie, sondern Entscheidungsrechte, Verantwortung, Workflows und Führung in Produktorganisationen verändert.',
        answer: 'AI verändert nicht nur einzelne Aufgaben. Sie verschiebt, wie Informationen entstehen, Entscheidungen vorbereitet werden und Arbeit zwischen Menschen, Teams und Agents verteilt wird. Ohne Anpassung des Operating Models bleibt AI deshalb häufig ein lokales Effizienzwerkzeug statt eines wirksamen Systems.',
        body: '<h3>Der Pilot ist selten das eigentliche Problem</h3><p>Technische Use Cases lassen sich heute schnell demonstrieren. Schwieriger wird der Übergang in den Betrieb: Wer entscheidet auf Basis eines AI-Ergebnisses? Wer prüft? Wer trägt Konsequenzen? Welche Fehler dürfen automatisch skaliert werden?</p><h3>Fünf Operating-Model-Fragen</h3><ol><li>Welches Ergebnis soll AI verbessern?</li><li>Welche Entscheidungen darf sie vorbereiten oder treffen?</li><li>Wer trägt Accountability?</li><li>Wie funktionieren Human-AI-Handoffs?</li><li>Wie werden Wirkung und Fehler gemessen?</li></ol>'
    ],
    'decision-rights-zwischen-mensch-und-ai': [
        title: 'Decision Rights zwischen Mensch und AI gestalten', nav: 'Decision Rights: Mensch und AI',
        description: 'Ein praktischer Rahmen für Entscheidungen zwischen Mensch, AI, Agent, Team und Führung.',
        answer: 'Decision Rights für AI sollten nicht pauschal nach Technologie, sondern nach Wirkung, Risiko, Umkehrbarkeit, Evidenzqualität und Eskalationsfähigkeit gestaltet werden. Verantwortung bleibt dabei explizit bei einer menschlichen Rolle oder Organisationseinheit.',
        body: '<h3>Autonomie ist kein Ein/Aus-Schalter</h3><p>Zwischen reiner Information und vollständig autonomem Handeln liegen mehrere Stufen: zusammenfassen, empfehlen, vorbereiten, innerhalb von Grenzen ausführen und selbstständig eskalieren.</p><h3>Fünf Kriterien</h3><ol><li>Wie groß ist die mögliche Wirkung?</li><li>Wie leicht ist die Entscheidung umkehrbar?</li><li>Wie belastbar sind Daten und Modell?</li><li>Wie schnell kann ein Mensch eingreifen?</li><li>Wer besitzt Ergebnis und Konsequenzen?</li></ol>'
    ],
    'schlechte-prozesse-nicht-nur-schneller-machen': [
        title: 'Schlechte Prozesse nicht nur schneller machen', nav: 'AI und schlechte Prozesse',
        description: 'Warum lokale AI-Automatisierung den Gesamtprozess nicht automatisch verbessert und wann ein Workflow-Redesign nötig wird.',
        answer: 'AI verbessert einen Workflow nur dann nachhaltig, wenn nicht nur einzelne Aktivitäten, sondern auch Übergaben, Entscheidungen, Verantwortung und Feedbackschleifen neu gestaltet werden. Sonst entstehen schnellere Zwischenergebnisse in einem weiterhin langsamen Gesamtsystem.',
        body: '<h3>Lokale Effizienz ist nicht Systemwirkung</h3><p>Eine schnellere Analyse hilft wenig, wenn das Ergebnis danach in mehreren Gremien neu interpretiert und freigegeben wird. Der Engpass verschiebt sich nur.</p><h3>End-to-end betrachten</h3><ol><li>Welches Ergebnis erzeugt der Workflow?</li><li>Wo liegen heute Wartezeit und Nacharbeit?</li><li>Welche Entscheidungen bestimmen den Durchfluss?</li><li>Wo braucht es Human Review?</li><li>Welche Messgröße zeigt echte Verbesserung?</li></ol>'
    ]
]

newInsights.each { String slug, Map<String, String> data ->
    Node page
    String path = '/insights/' + slug
    if (website.nodeExists(path)) page = website.getNode(path)
    else page = insights.addNode(slug, 'mgnl:page')
    setProperties(page, [
        'mgnl:template': PAGE_TEMPLATE, title: data.title, navigationTitle: data.nav,
        windowTitle: data.title + ' - ChOS Insight', metaDescription: data.description,
        hideInNavigation: true, dateModified: TODAY, articleSection: 'AI-enabled Operating Models'
    ])
    Node area = resetArea(page)
    addHero(area, 'Insight · AI-enabled Operating Models', data.title, data.description)
    addText(area, 'Kurzantwort', "<p class=\"lead\">${data.answer}</p>", true)
    addText(area, 'Im Detail', data.body)
    addCta(area, 'Was bedeutet das für Ihre Organisation?', '<p>In einer Clarity Session oder einem Assessment übertragen wir die Fragen auf Ihren konkreten Kontext.</p>', '/leistungen', 'Leistungen ansehen')
}

website.save()

// Angebotskatalog in-place aktualisieren; IDs und HubSpot-Verknüpfungen bleiben erhalten.
Node offerFolder = offers.nodeExists('/cleonhardt') ? offers.getNode('/cleonhardt') : offers.rootNode.addNode('cleonhardt', 'mgnl:folder')
Map<String, Map<String, Object>> catalog = [
    'chos-clarity-session': [sku: 'CHOS-CLARITY-001', title: 'ChOS Clarity Session', category: 'entry', sortOrder: 10L, duration: '90 Minuten', pricingModel: 'fixed', priceFrom: 390d, priceTo: 390d, priceLabel: '390 EUR netto', featured: false, accent: true, shortDescription: 'Eine konkrete Produkt-, Führungs- oder Organisationssituation strukturiert verstehen.', targetPath: '/clarity-session', ctaLabel: 'Clarity Session ansehen', features: ['Vorbereitender Kurzfragebogen', '90 Minuten persönliches Gespräch', 'ChOS-Einordnung', 'Hypothesen und nächster Schritt', 'Schriftlicher Clarity Brief']],
    'decision-review': [sku: 'CHOS-REVIEW-001', title: 'ChOS Decision Review', category: 'entry', sortOrder: 20L, pricingModel: 'fixed', priceFrom: 990d, priceTo: 990d, priceLabel: '990 EUR netto', featured: true, shortDescription: 'Eine wichtige Produkt-, Technologie- oder Organisationsentscheidung unabhängig gegenprüfen.', targetPath: '/decision-review', ctaLabel: 'Decision Review ansehen', features: ['Unterlagensichtung', 'Annahmenprüfung', 'Red-Team-Betrachtung', 'Risiken und Alternativen', 'Schriftliche Empfehlung']],
    'product-organisation-diagnostic': [sku: 'CHOS-ORG-DIAG-001', title: 'ChOS Operating Model Diagnostic', category: 'diagnostic', sortOrder: 30L, pricingModel: 'fixed', priceFrom: 4900d, priceTo: 4900d, priceLabel: '4.900 EUR netto', featured: true, accent: true, shortDescription: 'Richtung, Entscheidungen, Verantwortung, Zusammenarbeit und Lernen systematisch diagnostizieren.', targetPath: '/product-organisation-diagnostic', ctaLabel: 'Diagnostic ansehen', features: ['Auftrags- und Systemgrenzenklärung', 'Dokumentenanalyse', 'Zwei bis vier Interviews', 'ChOS-Auswertung', 'Priorisierte Interventionen']],
    'chos-quick-diagnostic': [sku: 'CHOS-AI-ASSESS-001', title: 'AI Operating Model Assessment', category: 'diagnostic', sortOrder: 40L, pricingModel: 'fixed', priceFrom: 9900d, priceTo: 9900d, priceLabel: '9.900 EUR netto', featured: false, shortDescription: 'Rollen, Entscheidungswege, Kontrolle und Verantwortung beim Einsatz von AI prüfen.', targetPath: '/ai-operating-model-assessment', ctaLabel: 'Assessment ansehen', features: ['Ziel- und Nutzenklärung', 'Entscheidungsrechte', 'Verantwortung und Kontrolle', 'Eskalationswege', 'Priorisierte nächste Schritte']],
    'workshops': [sku: 'CHOS-AI-WORKFLOW-001', title: 'AI-enabled Workflow / Product Sprint', category: 'transformation', sortOrder: 50L, pricingModel: 'fixed', priceFrom: 19500d, priceTo: 19500d, priceLabel: '19.500 EUR netto', featured: false, shortDescription: 'Einen konkreten Ablauf mit neuen technischen Möglichkeiten wirksam neu gestalten.', targetPath: '/ai-enabled-workflow-sprint', ctaLabel: 'Workflow Sprint ansehen', features: ['Ist-Ablauf und Engpässe', 'Aufgabenteilung', 'Entscheidungen und Kontrollen', 'Zielbild', 'Pilot und Messkriterien']],
    'chos-transformation-program': [sku: 'CHOS-TRANSFORM-001', title: 'ChOS Transformation Program', category: 'transformation', sortOrder: 60L, pricingModel: 'range', priceFrom: 30000d, priceTo: 150000d, priceLabel: '30.000-150.000 EUR+ netto', featured: false, active: false, accent: true, shortDescription: 'Von der Diagnose über das Target Operating Model bis zu Umsetzung und Wirkungsmessung.', targetPath: '/chos-transformation-program', ctaLabel: 'Transformation ansehen', features: ['ChOS-Diagnose', 'Target Operating Model', 'Decision Rights und Verantwortung', 'Intervention Design', 'Umsetzung und Wirkungsmessung']],
    'executive-sparring': [sku: 'CHOS-EXEC-001', title: 'Executive / Product Leadership Sparring', category: 'sparring', sortOrder: 70L, duration: 'Monatlich', pricingModel: 'fixed', priceFrom: 1750d, priceTo: 1750d, priceLabel: '1.750 EUR netto pro Monat', featured: false, shortDescription: 'Vertraulicher Denkraum für komplexe Produkt- und Organisationsentscheidungen.', targetPath: '/executive-sparring', ctaLabel: 'Sparring ansehen', features: ['Zwei bis vier Sessions pro Monat', 'Asynchrone Rückfragen', 'Decision Reviews light', 'Situations- und Systemanalyse', 'Monatliche Lernschleife']]
]

catalog.each { String name, Map<String, Object> data ->
    Node offer = offerFolder.hasNode(name) ? offerFolder.getNode(name) : offerFolder.addNode(name, 'mgnl:content')
    setOffer(offer, data, website)
    Node targetPage = website.getNode(data.targetPath as String)
    targetPage.setProperty('offerReference', offer.getIdentifier())
}
if (offerFolder.hasNode('leadership-product-sparring')) offerFolder.getNode('leadership-product-sparring').setProperty('active', false)
offers.save()
website.save()

// Navigation: Hauptpunkt Leistungen sicherstellen und Unterpunkte neu kuratieren.
Node navItems = navigation.getNode('/cleonhardt/Main/items')
Node findNavItem(Node parent, Node target) {
    for (Node item : parent.nodes) {
        if (item.hasProperty('targetPage') && item.getProperty('targetPage').string == target.getIdentifier()) return item
    }
    null
}
Node resetChildren(Node item) {
    if (item.hasNode('children')) item.getNode('children').remove()
    item.addNode('children', 'mgnl:contentNode')
}
void addNavChild(Node parent, int index, String label, Node target) {
    Node item = parent.addNode(String.format('%02d', index), 'mgnl:contentNode')
    item.setProperty('label', label)
    item.setProperty('targetPage', target.getIdentifier())
}

if (navItems.hasNode('01-services')) navItems.getNode('01-services').remove()
Node servicesNav = navItems.hasNode('1') ? navItems.getNode('1') : findNavItem(navItems, services)
if (servicesNav == null) {
    servicesNav = navItems.addNode('01-services', 'mgnl:contentNode')
}
servicesNav.setProperty('label', 'Leistungen')
servicesNav.setProperty('targetPage', services.getIdentifier())
Node servicesChildren = resetChildren(servicesNav)
[
    ['/clarity-session', 'Clarity Session'],
    ['/decision-review', 'Decision Review'],
    ['/product-organisation-diagnostic', 'Operating Model Diagnostic'],
    ['/ai-operating-model-assessment', 'AI Operating Model Assessment'],
    ['/ai-enabled-workflow-sprint', 'AI-enabled Workflow Sprint'],
    ['/executive-sparring', 'Executive Sparring']
].eachWithIndex { entry, index -> addNavChild(servicesChildren, index, entry[1], website.getNode(entry[0])) }

Node chosNav = navItems.hasNode('2') ? navItems.getNode('2') : findNavItem(navItems, chos)
if (chosNav != null) {
    chosNav.setProperty('label', 'ChOS')
    chosNav.setProperty('targetPage', chos.getIdentifier())
    Node children = resetChildren(chosNav)
    if (website.nodeExists('/chos-selbstcheck')) addNavChild(children, 0, 'ChOS Selbstcheck', website.getNode('/chos-selbstcheck'))
    if (website.nodeExists('/praxisfaelle')) addNavChild(children, 1, 'Praxisfälle', website.getNode('/praxisfaelle'))
}

Node insightsNav = navItems.hasNode('4') ? navItems.getNode('4') : findNavItem(navItems, insights)
if (insightsNav != null) {
    insightsNav.setProperty('label', 'Insights')
    insightsNav.setProperty('targetPage', insights.getIdentifier())
    Node children = resetChildren(insightsNav)
    [
        ['/insights/warum-ai-einfuehrung-ein-operating-model-thema-ist', 'AI als Operating-Model-Thema'],
        ['/insights/decision-rights-zwischen-mensch-und-ai', 'Decision Rights: Mensch und AI'],
        ['/insights/schlechte-prozesse-nicht-nur-schneller-machen', 'AI und schlechte Prozesse'],
        ['/insights/wann-braucht-eine-produktorganisation-ein-operating-model', 'Wann braucht es ein Operating Model?'],
        ['/insights/diagnose-vor-eingriff', 'Diagnose vor Eingriff']
    ].eachWithIndex { entry, index -> if (website.nodeExists(entry[0])) addNavChild(children, index, entry[1], website.getNode(entry[0])) }
}
navigation.save()

// Footer und zentrale Signatur an die neue Positionierung angleichen.
String footerPath = footer.nodeExists('/cleonhardt/Informationen') ? '/cleonhardt/Informationen' : '/cleonhardt/Footer'
if (footer.nodeExists(footerPath)) {
    Node footerRoot = footer.getNode(footerPath)
    footerRoot.setProperty('footerText', 'Klarheit für komplexe Produktorganisationen.')
}
footer.save()

if (siteSettings.nodeExists('/cleonhardt/Logo-Cleonhardt')) {
    Node settings = siteSettings.getNode('/cleonhardt/Logo-Cleonhardt')
    settings.setProperty('signature', 'Christian Leonhardt · Klarheit für komplexe Produktorganisationen.')
    settings.setProperty('socialImageAlt', 'Christian Leonhardt - Klarheit für komplexe Produktorganisationen')
}
siteSettings.save()

// Kontakt-Auswahl auf die neue Angebotsarchitektur umstellen.
if (website.nodeExists('/kontakt/main')) {
    Node contactArea = website.getNode('/kontakt/main')
    for (Node component : contactArea.nodes) {
        if (!component.hasNode('fieldsets')) continue
        Node fieldsets = component.getNode('fieldsets')
        for (Node group : fieldsets.nodes) {
            if (!group.hasNode('fields')) continue
            for (Node field : group.getNode('fields').nodes) {
                if (field.hasProperty('controlName') && field.getProperty('controlName').string == 'anliegen') {
                    field.setProperty('labels', 'Bitte auswählen:\nChOS Clarity Session:clarity-session\nChOS Decision Review:decision-review\nChOS Operating Model Diagnostic:operating-model-diagnostic\nAI Operating Model Assessment:ai-operating-model-assessment\nAI-enabled Workflow / Product Sprint:ai-workflow-sprint\nExecutive / Product Leadership Sparring:executive-sparring\nAnderes Anliegen:anderes')
                }
            }
        }
    }
}
website.save()

println 'ChOS umgesetzt: Start, Leistungen, ChOS, Über mich, Insights, Angebote, Navigation, Footer und CTAs aktualisiert.'
