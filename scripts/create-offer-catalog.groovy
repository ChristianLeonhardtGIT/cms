import info.magnolia.context.MgnlContext
import javax.jcr.Node
import javax.jcr.Session

/*
 * Legt den zentralen Angebotskatalog an und stellt /leistungen sowie den
 * Angebots-Teaser auf der Startseite auf referenzierte Produktdaten um.
 * Das Skript ist wiederholbar: Der Katalog unter /cleonhardt wird dabei
 * kontrolliert neu aufgebaut, andere Workspaces und Seiten bleiben erhalten.
 */

Session offers = MgnlContext.getJCRSession('offers')
Session website = MgnlContext.getJCRSession('website')

Map<String, Map> catalog = [
    'chos-clarity-session': [
        sku: 'CHOS-CLARITY-001', title: 'ChOS Clarity Session', category: 'entry', sortOrder: 10L,
        duration: '90 Minuten', pricingModel: 'fixed', priceFrom: 295d, priceTo: 295d,
        priceLabel: '295 € netto', priceUnit: 'einmalig', featured: true, accent: true,
        shortDescription: 'Eine konkrete Produkt-, Führungs- oder Organisationssituation strukturiert verstehen und den nächsten sinnvollen Schritt bestimmen.',
        targetPath: '/clarity-session', ctaLabel: 'Clarity Session ansehen',
        features: ['Vorbereitender Fragebogen', '90 Minuten persönliches Gespräch', 'Strukturierte Problemanalyse', 'Erste Hypothesen', 'Konkrete nächste Schritte', 'Schriftliches Ergebnisdokument']
    ],
    'decision-review': [
        sku: 'CHOS-REVIEW-001', title: 'Decision Review', category: 'entry', sortOrder: 20L,
        pricingModel: 'range', priceFrom: 490d, priceTo: 750d, priceLabel: '490–750 € netto',
        shortDescription: 'Eine wichtige Entscheidung oder ein vorliegendes Konzept vor der Umsetzung belastbar gegenprüfen.',
        targetPath: '/decision-review', ctaLabel: 'Details ansehen',
        features: ['Sichtung vorhandener Unterlagen', 'Gegenprüfung zentraler Annahmen', 'Red-Team-Betrachtung', 'Persönliches Review', 'Schriftliche Empfehlung']
    ],
    'leadership-product-sparring': [
        sku: 'CHOS-SPARRING-2W-001', title: 'Leadership- oder Product-Sparring', category: 'sparring', sortOrder: 30L,
        duration: '2 Wochen', pricingModel: 'fixed', priceFrom: 690d, priceTo: 690d,
        priceLabel: 'Einführungspreis 690 € netto', priceUnit: 'zwei Wochen', featured: true,
        shortDescription: 'Fokussierte persönliche und asynchrone Begleitung für eine anspruchsvolle Phase mit mehreren Entscheidungen.',
        targetPath: '/kontakt', ctaLabel: 'Sparring anfragen',
        features: ['60 Minuten Auftaktgespräch', 'Zwei Wochen asynchrone Begleitung', 'Klar definierter Messenger-Kanal', 'Rückmeldung im vereinbarten Zeitraum', '45 Minuten Abschlussgespräch', 'Zusammenfassung und nächste Schritte']
    ],
    'executive-sparring': [
        sku: 'CHOS-EXEC-001', title: 'Executive Sparring', category: 'sparring', sortOrder: 40L,
        duration: 'Monatlich', pricingModel: 'range', priceFrom: 1250d, priceTo: 1750d,
        priceLabel: '1.250–1.750 € netto pro Monat', priceUnit: 'pro Monat', accent: true,
        priceNote: 'Zum Einstieg monatlich kündbar. Drei- oder Sechsmonatsprogramme können anschließend vereinbart werden.',
        shortDescription: 'Kontinuierlicher vertraulicher Rückhalt für Führung, Entscheidungen und anspruchsvolle Situationen.',
        targetPath: '/executive-sparring', ctaLabel: 'Details ansehen',
        features: ['Vier persönliche Gespräche', 'Asynchrone Rückfragen', 'Entscheidungs- und Situationsanalysen', 'Monatliche Reflexion', 'Priorisierte Handlungsempfehlungen']
    ],
    'chos-quick-diagnostic': [
        sku: 'CHOS-QUICK-DIAG-001', title: 'ChOS Quick Diagnostic', category: 'company', sortOrder: 50L,
        pricingModel: 'range', priceFrom: 2900d, priceTo: 4900d, priceLabel: '2.900–4.900 € netto',
        shortDescription: 'Eine kompakte Diagnose eines klar abgegrenzten Problems.',
        targetPath: '/kontakt', ctaLabel: 'Angebot anfragen',
        features: ['Dokumentenanalyse', 'Zwei bis vier Interviews', 'Rollen- und Entscheidungsanalyse', 'Hypothesen und Risiken', 'Ergebnispräsentation', 'Konkrete Empfehlungen']
    ],
    'product-organisation-diagnostic': [
        sku: 'CHOS-ORG-DIAG-001', title: 'Product Organisation Diagnostic', category: 'company', sortOrder: 60L,
        pricingModel: 'range', priceFrom: 7500d, priceTo: 15000d, priceLabel: '7.500–15.000 € netto', accent: true,
        shortDescription: 'Eine umfassende Analyse der Produktorganisation mit Zielbild und priorisiertem Maßnahmenplan.',
        targetPath: '/product-organisation-diagnostic', ctaLabel: 'Details ansehen',
        features: ['Interviews mit Führungskräften und Teams', 'Rollen, Verantwortlichkeiten und Entscheidungswege', 'Priorisierung, Zusammenarbeit und Abhängigkeiten', 'Führungs- und Kommunikationsstrukturen', 'ChOS-Auswertung', 'Zielbild und Maßnahmenplan']
    ],
    'workshops': [
        sku: 'CHOS-WORKSHOP-DAY-001', title: 'Workshops', category: 'company', sortOrder: 70L,
        duration: 'Pro Workshoptag', pricingModel: 'range', priceFrom: 1600d, priceTo: 2500d,
        priceLabel: '1.600–2.500 € netto', priceUnit: 'pro Workshoptag', featured: true,
        priceNote: 'Umfangreiche Vorbereitung und Reiseaufwand werden gesondert vereinbart.',
        shortDescription: 'Fokussierte Arbeitsformate für Führungsteams und Produktorganisationen.',
        targetPath: '/workshops', ctaLabel: 'Details ansehen',
        features: ['Rollen- und Verantwortungsklärung', 'Operating Model', 'Leadership und Team Alignment', 'Entscheidungsmodell', 'Product Strategy und Priorisierung', 'Transformation und Zusammenarbeit']
    ]
]

void setValue(Node node, String name, Object value) {
    if (value == null) return
    if (value instanceof Boolean) node.setProperty(name, value as boolean)
    else if (value instanceof Long) node.setProperty(name, value as long)
    else if (value instanceof Number) node.setProperty(name, value as double)
    else node.setProperty(name, value.toString())
}

Node root = offers.rootNode
if (root.hasNode('cleonhardt')) root.getNode('cleonhardt').remove()
Node folder = root.addNode('cleonhardt', 'mgnl:folder')

catalog.each { String nodeName, Map data ->
    Node offer = folder.addNode(nodeName, 'mgnl:content')
    ['sku', 'title', 'shortDescription', 'category', 'duration', 'pricingModel', 'priceFrom', 'priceTo',
     'priceLabel', 'priceUnit', 'priceNote', 'ctaLabel', 'featured', 'accent', 'sortOrder'].each { key ->
        setValue(offer, key, data[key])
    }
    setValue(offer, 'currency', 'EUR')
    setValue(offer, 'taxRate', 19d)
    setValue(offer, 'active', true)
    if (data.targetPath && website.nodeExists(data.targetPath as String)) {
        setValue(offer, 'targetPage', website.getNode(data.targetPath as String).identifier)
    }
    Node features = offer.addNode('features', 'mgnl:contentNode')
    (data.features as List<String>).eachWithIndex { String text, int index ->
        Node feature = features.addNode(String.format('%02d', index), 'mgnl:contentNode')
        feature.setProperty('text', text)
    }
}
offers.save()

catalog.each { String nodeName, Map data ->
    if (data.targetPath && website.nodeExists(data.targetPath as String) && data.targetPath != '/kontakt') {
        Node page = website.getNode(data.targetPath as String)
        page.setProperty('offerReference', folder.getNode(nodeName).identifier)
    }
}

Node addComponent(Node area, String name, String template, Map properties) {
    Node component = area.addNode(name, 'mgnl:component')
    component.setProperty('mgnl:template', 'meine-website:components/' + template)
    properties.each { key, value -> setValue(component, key as String, value) }
    component
}

if (!website.nodeExists('/leistungen')) throw new IllegalStateException('/leistungen fehlt')
Node services = website.getNode('/leistungen')
if (services.hasNode('main')) services.getNode('main').remove()
Node area = services.addNode('main', 'mgnl:area')

Node hero = addComponent(area, '00', 'hero', [
    eyebrow: 'Leistungen', title: 'Der passende Rahmen für Ihre aktuelle Situation.',
    description: 'Vom fokussierten Einzeltermin bis zur umfassenden Organisationsdiagnose: Der Umfang folgt dem Problem – nicht umgekehrt.'
])
Node heroChooser = hero.addNode('ctaChooser', 'mgnl:contentNode')
heroChooser.setProperty('field', 'withCta')
heroChooser.setProperty('ctaText', 'Situation klären')
Node heroLink = heroChooser.addNode('ctaLink', 'mgnl:contentNode')
heroLink.setProperty('field', 'internalPageLink')
heroLink.setProperty('internalLink', '/kontakt')

addComponent(area, '01', 'offerCatalog', [
    heading: 'Bezahlter Einstieg',
    intro: '<p class="lead">Ein klar abgegrenzter Auftrag für ein konkretes Problem oder eine wichtige Entscheidung. Sie kaufen Vorbereitung, Analyse und ein verwertbares Ergebnis.</p>',
    category: 'entry', columns: '2', showFeatures: true, showPriceNote: true
])
addComponent(area, '02', 'offerCatalog', [
    heading: 'Persönliches Sparring',
    intro: '<p class="lead">Ein vertraulicher Denkraum für Führungskräfte und Produktverantwortliche, die Rückhalt über mehrere Entscheidungen hinweg brauchen.</p>',
    category: 'sparring', columns: '2', showFeatures: true, showPriceNote: true, showBackground: true
])
addComponent(area, '03', 'offerCatalog', [
    heading: 'Angebote für Unternehmen',
    intro: '<p class="lead">Wenn mehrere Personen, Schnittstellen und wiederkehrende Muster beteiligt sind, wird aus einem Einzelproblem eine Organisationsfrage.</p>',
    category: 'company', columns: '3', showFeatures: true, showPriceNote: true
])
addComponent(area, '04', 'text', [
    heading: 'Welches Format passt?', showBackground: true,
    text: '<div class="mvp-grid mvp-grid--3"><div class="mvp-panel"><h3>Ein Problem oder Konzept</h3><p><strong>Clarity Session</strong> für eine konkrete Situation, <strong>Decision Review</strong> für eine anstehende Entscheidung.</p></div><div class="mvp-panel"><h3>Fortlaufende Entscheidungen</h3><p><strong>Sparring</strong> für vertraulichen Rückhalt über mehrere Entscheidungen hinweg.</p></div><div class="mvp-panel"><h3>Ein Team oder System</h3><p><strong>Diagnostic</strong> für ein Lagebild, <strong>Workshop</strong> für gemeinsame Klärung.</p></div></div>'
])
Node cta = addComponent(area, '05', 'callToAction', [
    title: 'Sie müssen das Format nicht vorab kennen.',
    description: '<p>Beschreiben Sie kurz Ihre Lage. Gemeinsam klären wir, welcher nächste Schritt sinnvoll ist.</p>',
    buttonText: 'Situation klären'
])
Node ctaChooser = cta.addNode('pageLinkChooser', 'mgnl:contentNode')
ctaChooser.setProperty('field', 'internalPageLink')
ctaChooser.setProperty('internalLink', '/kontakt')

Node start = website.nodeExists('/start') ? website.getNode('/start') : null
if (start != null && start.hasNode('main')) {
    Node main = start.getNode('main')
    Node oldTeaser = null
    for (Node child : main.nodes) {
        if (child.hasProperty('heading') && ['Vom konkreten Problem bis zum Organisationsbild', 'Drei Formate für unterschiedliche Situationen'].contains(child.getProperty('heading').string)) {
            oldTeaser = child
            break
        }
    }
    if (oldTeaser != null) {
        String replacementName = oldTeaser.name + '-catalog'
        oldTeaser.remove()
        addComponent(main, replacementName, 'offerCatalog', [
            heading: 'Vom konkreten Problem bis zum Organisationsbild',
            intro: '<p class="lead">Drei passende Einstiege – zentral aus dem Angebotskatalog gepflegt.</p>',
            category: 'all', columns: '3', featuredOnly: true, showFeatures: false, showPriceNote: false
        ])
    }
}

website.save()

println 'Angebotskatalog erstellt: 7 aktive Angebote mit stabilen Produktnummern.'
println '/leistungen und der Startseiten-Teaser verwenden nun zentrale Produktdaten.'
println 'Als Nächstes: Katalog, /leistungen und /start veröffentlichen.'
