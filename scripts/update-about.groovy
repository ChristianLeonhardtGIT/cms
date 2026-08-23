import info.magnolia.context.MgnlContext
import groovy.transform.Field
import javax.jcr.Node
import javax.jcr.Session

/*
 * In Magnolias Groovy-App auf der Author-Instanz ausführen.
 * Aktualisiert ausschließlich die Seite /ueber-mich. Das Skript ist
 * wiederholbar und lässt alle anderen Seiten unverändert.
 */

@Field final String COMPONENT_PREFIX = 'meine-website:components/'
Session website = MgnlContext.getJCRSession('website')

void setProperties(Node node, Map<String, Object> properties) {
    properties.each { String key, Object value ->
        if (value != null) {
            node.setProperty(key, value)
        }
    }
}

Node addComponent(Node area, String template, Map<String, Object> properties = [:]) {
    String name = String.format('%02d', area.nodes.size)
    Node component = area.addNode(name, 'mgnl:component')
    component.setProperty('mgnl:template', template.contains(':') ? template : COMPONENT_PREFIX + template)
    setProperties(component, properties)
    component
}

Node addHero(Node area, String eyebrow, String title, String description, String target, String cta) {
    Node hero = addComponent(area, 'hero', [
        eyebrow: eyebrow,
        title: title,
        description: description
    ])
    Node chooser = hero.addNode('ctaChooser', 'mgnl:contentNode')
    chooser.setProperty('field', 'withCta')
    chooser.setProperty('ctaText', cta)
    Node link = chooser.addNode('ctaLink', 'mgnl:contentNode')
    link.setProperty('field', 'internalPageLink')
    link.setProperty('internalLink', target)
    hero
}

Node addText(Node area, String heading, String html, boolean tinted = false) {
    addComponent(area, 'text', [
        heading: heading,
        text: html,
        showBackground: tinted
    ])
}

Node addCta(Node area, String title, String html, String target, String button) {
    Node cta = addComponent(area, 'callToAction', [
        title: title,
        description: html,
        buttonText: button
    ])
    Node chooser = cta.addNode('pageLinkChooser', 'mgnl:contentNode')
    chooser.setProperty('field', 'internalPageLink')
    chooser.setProperty('internalLink', target)
    cta
}

if (!website.nodeExists('/ueber-mich')) {
    throw new IllegalStateException('Die Seite /ueber-mich wurde nicht gefunden.')
}

Node page = website.getNode('/ueber-mich')
setProperties(page, [
    title: 'Über Christian',
    navigationTitle: 'Über mich',
    windowTitle: 'Über Christian Leonhardt',
    metaDescription: 'Christian Leonhardt verbindet Führungserfahrung bei ALDI NORD, Peek & Cloppenburg, SUNZINET, team neusta und der Bundeswehr mit Product Leadership und Organisationsentwicklung.'
])

if (page.hasNode('main')) {
    page.getNode('main').remove()
}
Node area = page.addNode('main', 'mgnl:area')

addHero(area, 'Über mich', 'Komplexität lässt sich nicht wegmoderieren.',
    'Ich verbinde Produktdenken, Führung und Organisationsdiagnose – mit dem Anspruch, schwierige Situationen klarer und handhabbar zu machen.',
    '/kontakt', 'Kennenlernen')

addText(area, 'Christian Leonhardt', '''
<p class="lead">Ich begleite Produktverantwortliche, Führungsteams und digitale Organisationen in Situationen, in denen Methodenwissen allein nicht weiterhilft.</p>
<p>Meine Arbeit liegt an den Übergängen: zwischen Strategie und Umsetzung, Produkt und Technologie, Verantwortung und formaler Struktur, Wachstum und notwendiger Neuordnung. Gerade dort entstehen Reibung, Verzögerung und Missverständnisse – aber auch die größten Hebel.</p>
<p>Diese Perspektive ist nicht am Schreibtisch entstanden. Ich habe Produktorganisationen aufgebaut und weiterentwickelt, kritische Programme stabilisiert, internationale Stakeholder zusammengebracht und Verantwortung in Situationen übernommen, in denen Entscheidungen unter Unsicherheit getroffen werden mussten.</p>
<p>Statt vorschnell ein Zielbild über die Organisation zu legen, arbeite ich zuerst heraus, welches Problem tatsächlich gelöst werden muss. Daraus entstehen klare Entscheidungen und Veränderungen, die zum Kontext passen.</p>''')

addText(area, 'Erfahrung aus Verantwortung', '''
<div class="mvp-grid mvp-grid--3">
  <article class="mvp-panel"><h3>Produkt und Organisation</h3><p>Verantwortung für digitale Produkte, Loyalty, CRM und E-Commerce – von Strategie und Priorisierung bis zu Governance, Teamzuschnitt und Umsetzung.</p></article>
  <article class="mvp-panel mvp-panel--accent"><h3>Transformation und Stabilisierung</h3><p>Aufbau tragfähiger Produkt- und Betriebsmodelle sowie Neuordnung gefährdeter Programme, unklarer Entscheidungswege und belasteter Zusammenarbeit.</p></article>
  <article class="mvp-panel"><h3>Führung in komplexen Situationen</h3><p>Fachliche und disziplinarische Führung, Entwicklung von Verantwortung und Koordination über Bereiche, Unternehmen und Ländergrenzen hinweg.</p></article>
</div>''', true)

addText(area, 'Ausgewählte berufliche Stationen', '''
<div class="mvp-grid mvp-grid--2">
  <article class="mvp-panel"><p class="mvp-meta">ALDI NORD</p><h3>Mobile, Loyalty und CRM</h3><p>Aufbau und Skalierung eines internen Produkt- und Betriebsmodells. Dazu gehören Architektur- und Technologieentscheidungen, Roadmaps, Business Cases, Produkt-Governance und die Zusammenarbeit mit internationalen Länder- und Produktorganisationen.</p></article>
  <article class="mvp-panel"><p class="mvp-meta">Peek &amp; Cloppenburg / Fashion ID</p><h3>Omnichannel-Produktorganisation</h3><p>Verantwortung für digitale Kundeneinstiege über Web und App sowie Mitgestaltung eines domänenorientierten Organisationsmodells. Im Mittelpunkt standen klare Ergebnisverantwortung, strategische Roadmaps und verbindliche Entscheidungsstrukturen.</p></article>
  <article class="mvp-panel"><p class="mvp-meta">SUNZINET und team neusta</p><h3>Digitale Programme und Beratung</h3><p>Steuerung internationaler E-Commerce- und Plattformprogramme, Stabilisierung kritischer Vorhaben und Kundenbeziehungen sowie Aufbau gemeinsamer Standards für Product Ownership, Zusammenarbeit und Umsetzung.</p></article>
  <article class="mvp-panel"><p class="mvp-meta">Bundeswehr</p><h3>Führungsfundament unter Verantwortung</h3><p>Leitung in der Materialwirtschaft und Verantwortung für verlässliche Versorgung in anspruchsvollen operativen Situationen, darunter ein multinationales Einsatzumfeld in Afghanistan. Diese Zeit prägt meinen Blick auf Klarheit, Verbindlichkeit und Führung bis heute.</p></article>
</div>''')

addText(area, 'Wofür ich stehe', '''
<div class="mvp-grid mvp-grid--3">
  <article class="mvp-panel"><h3>Klarheit</h3><p>Komplexe Sachverhalte auf ihren Kern reduzieren, ohne sie unzulässig zu vereinfachen.</p></article>
  <article class="mvp-panel"><h3>Verantwortung</h3><p>Entscheidungen sichtbar machen und Menschen befähigen, echte Ergebnisverantwortung zu übernehmen.</p></article>
  <article class="mvp-panel"><h3>Pragmatismus</h3><p>Mit dem kleinsten sinnvollen Eingriff lernen, statt große Veränderungsprogramme zu simulieren.</p></article>
</div>''', true)

addText(area, 'Berufliches Fundament', '''
<p class="lead">Meine Laufbahn verbindet kaufmännisches Denken, operative Führung, digitale Produktentwicklung und Organisationsarbeit.</p>
<ul class="mvp-checks"><li>Staatlich geprüfter Betriebswirt mit Schwerpunkt Marketing und Absatzwirtschaft</li><li>Zusatzqualifikation als Product Owner und Scrum Master</li><li>Erfahrung mit Strategieumsetzung, Business Cases, Roadmaps, KPI- und OKR-Steuerung</li><li>Praxis in Produkt-Governance, Organisationsmodellen und bereichsübergreifender Transformation</li><li>Führung und Entwicklung von Product Ownern, Projektverantwortlichen und interdisziplinären Teams</li></ul>
<p>Der gemeinsame Nenner: ein belastbares Bild der Situation schaffen und daraus handlungsfähige Führung entwickeln.</p>''')

addCta(area, 'Lassen Sie uns über Ihre Situation sprechen.',
    '<p>Ein kurzes Anliegen reicht für den Anfang. Ich melde mich persönlich zurück.</p>',
    '/kontakt', 'Situation klären')

website.save()

println 'Die Seite /ueber-mich wurde ohne Kennzahlen um belegbare Berufserfahrung ergänzt.'
println 'Jetzt /ueber-mich inklusive Unterknoten veröffentlichen.'
