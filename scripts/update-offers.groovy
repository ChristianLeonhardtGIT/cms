import info.magnolia.context.MgnlContext
import groovy.transform.Field
import javax.jcr.Node
import javax.jcr.Session

/*
 * Aktualisiert ausschließlich die Leistungsseite und die Auswahl des
 * Kontaktformulars. Andere Seiten und bereits gepflegte Inhalte bleiben
 * unverändert.
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

Node findByProperty(Node root, String propertyName, String propertyValue) {
    if (root.hasProperty(propertyName) && root.getProperty(propertyName).string == propertyValue) {
        return root
    }
    for (Node child : root.nodes) {
        Node match = findByProperty(child, propertyName, propertyValue)
        if (match != null) {
            return match
        }
    }
    null
}

if (!website.nodeExists('/leistungen')) {
    throw new IllegalStateException('Die Seite /leistungen wurde nicht gefunden.')
}
if (!website.nodeExists('/kontakt')) {
    throw new IllegalStateException('Die Seite /kontakt wurde nicht gefunden.')
}

Node page = website.getNode('/leistungen')
setProperties(page, [
    windowTitle: 'Leistungen – Christian Leonhardt',
    metaDescription: 'Clarity Session, Decision Review, persönliches Sparring, Organisationsdiagnostik und Workshops für Produktverantwortliche und digitale Organisationen.'
])

if (page.hasNode('main')) {
    page.getNode('main').remove()
}
Node area = page.addNode('main', 'mgnl:area')

addHero(area, 'Leistungen', 'Der passende Rahmen für Ihre aktuelle Situation.',
    'Vom fokussierten Einzeltermin bis zur umfassenden Organisationsdiagnose: Der Umfang folgt dem Problem – nicht umgekehrt.',
    '/kontakt', 'Situation klären')

addText(area, 'Bezahlter Einstieg', '''
<p class="lead">Ein klar abgegrenzter Auftrag für ein konkretes Problem oder eine wichtige Entscheidung. Sie kaufen nicht nur Gesprächszeit, sondern Vorbereitung, Analyse und ein verwertbares Ergebnis.</p>
<div class="mvp-grid mvp-grid--2">
  <article class="mvp-panel mvp-panel--accent"><p class="mvp-meta">90 Minuten · 295 € netto</p><h3>ChOS Clarity Session</h3><p>Eine festgefahrene oder schwer einzuordnende Produkt-, Führungs- oder Organisationssituation strukturiert verstehen.</p><h4>Enthalten</h4><ul class="mvp-checks"><li>Vorbereitender Fragebogen</li><li>90 Minuten persönliches Gespräch</li><li>Strukturierte Problemanalyse</li><li>Erste Hypothesen</li><li>Konkrete nächste Schritte</li><li>Schriftliches Ergebnisdokument</li></ul></article>
  <article class="mvp-panel"><p class="mvp-meta">490–750 € netto</p><h3>Decision Review</h3><p>Eine wichtige Entscheidung oder ein bereits vorliegendes Konzept vor der Umsetzung belastbar gegenprüfen.</p><h4>Enthalten</h4><ul class="mvp-checks"><li>Sichtung vorhandener Unterlagen</li><li>Gegenprüfung zentraler Annahmen</li><li>Red-Team-Betrachtung</li><li>Persönliches Review</li><li>Schriftliche Empfehlung</li></ul></article>
</div>''')

addText(area, 'Persönliches Sparring', '''
<p class="lead">Ein vertraulicher Denkraum für Führungskräfte und Produktverantwortliche, die nicht nur einen einzelnen Termin, sondern Rückhalt über mehrere Entscheidungen hinweg brauchen.</p>
<div class="mvp-grid mvp-grid--2">
  <article class="mvp-panel"><p class="mvp-meta">2 Wochen · Einführungspreis 690 € netto</p><h3>Leadership- oder Product-Sparring</h3><ul class="mvp-checks"><li>60 Minuten Auftaktgespräch</li><li>Zwei Wochen asynchrone Begleitung</li><li>Klar definierter Messenger-Kanal</li><li>Rückmeldung innerhalb des vereinbarten Zeitraums</li><li>45 Minuten Abschlussgespräch</li><li>Zusammenfassung und nächste Schritte</li></ul></article>
  <article class="mvp-panel mvp-panel--accent"><p class="mvp-meta">1.250–1.750 € netto pro Monat</p><h3>Executive Sparring</h3><ul class="mvp-checks"><li>Vier persönliche Gespräche</li><li>Asynchrone Rückfragen</li><li>Entscheidungs- und Situationsanalysen</li><li>Monatliche Reflexion</li><li>Priorisierte Handlungsempfehlungen</li></ul><p><strong>Flexibler Einstieg:</strong> zunächst monatlich kündbar. Drei- oder Sechsmonatsprogramme können später vereinbart werden.</p></article>
</div>''', true)

addText(area, 'Angebote für Unternehmen', '''
<p class="lead">Wenn mehrere Personen, Schnittstellen und wiederkehrende Muster beteiligt sind, wird aus einem Einzelproblem eine Organisationsfrage. Der konkrete Umfang wird nach einem Vorgespräch verbindlich zugeschnitten.</p>
<div class="mvp-grid mvp-grid--3">
  <article class="mvp-panel"><p class="mvp-meta">2.900–4.900 € netto</p><h3>ChOS Quick Diagnostic</h3><p>Eine kompakte Diagnose eines klar abgegrenzten Problems.</p><ul class="mvp-checks"><li>Dokumentenanalyse</li><li>Zwei bis vier Interviews</li><li>Rollen- und Entscheidungsanalyse</li><li>Hypothesen und Risiken</li><li>Ergebnispräsentation</li><li>Konkrete Empfehlungen</li></ul></article>
  <article class="mvp-panel mvp-panel--accent"><p class="mvp-meta">7.500–15.000 € netto</p><h3>Product Organisation Diagnostic</h3><p>Eine umfassendere Analyse der Produktorganisation.</p><ul class="mvp-checks"><li>Interviews mit Führungskräften und Teams</li><li>Rollen, Verantwortlichkeiten und Entscheidungswege</li><li>Priorisierung, Zusammenarbeit und Abhängigkeiten</li><li>Führungs- und Kommunikationsstrukturen</li><li>ChOS-Auswertung</li><li>Zielbild und Maßnahmenplan</li></ul></article>
  <article class="mvp-panel"><p class="mvp-meta">1.600–2.500 € netto pro Workshoptag</p><h3>Workshops</h3><p>Fokussierte Arbeitsformate für Führungsteams und Produktorganisationen.</p><ul class="mvp-checks"><li>Rollen- und Verantwortungsklärung</li><li>Operating Model</li><li>Leadership und Team Alignment</li><li>Entscheidungsmodell</li><li>Product Strategy und Priorisierung</li><li>Transformation und Zusammenarbeit</li></ul><p>Umfangreiche Vorbereitung und Reiseaufwand werden gesondert vereinbart.</p></article>
</div>
<p><small>Alle genannten Preise sind Nettopreise. Der endgültige Preis richtet sich bei variablen Formaten nach Umfang, vorhandenen Unterlagen, Zahl der Beteiligten und gewünschtem Ergebnis.</small></p>''')

addText(area, 'Welches Format passt?', '''
<div class="mvp-grid mvp-grid--3"><div class="mvp-panel"><h3>Ein Problem oder Konzept</h3><p><strong>Clarity Session</strong> für eine konkrete Situation, <strong>Decision Review</strong> für eine anstehende Entscheidung oder ein vorliegendes Konzept.</p></div><div class="mvp-panel"><h3>Fortlaufende Entscheidungen</h3><p><strong>Zweiwöchiges Sparring</strong> für einen fokussierten Zeitraum, <strong>Executive Sparring</strong> für kontinuierlichen vertraulichen Rückhalt.</p></div><div class="mvp-panel"><h3>Ein Team oder System</h3><p><strong>Diagnostic</strong> für ein belastbares Lagebild, <strong>Workshop</strong> für gemeinsame Klärung und konkrete Arbeit am Zielbild.</p></div></div>''', true)

addCta(area, 'Sie müssen das Format nicht vorab kennen.',
    '<p>Beschreiben Sie kurz Ihre Lage. Gemeinsam klären wir, welcher nächste Schritt sinnvoll ist.</p>',
    '/kontakt', 'Situation klären')

Node concern = findByProperty(website.getNode('/kontakt'), 'controlName', 'anliegen')
if (concern == null) {
    throw new IllegalStateException('Das Auswahlfeld „Worum geht es?“ wurde im Kontaktformular nicht gefunden.')
}
concern.setProperty('labels', 'Bitte auswählen:\nChOS Clarity Session:clarity-session\nDecision Review:decision-review\nZweiwöchiges Leadership- oder Product-Sparring:sparring\nMonatliches Executive Sparring:executive-sparring\nChOS Quick Diagnostic:quick-diagnostic\nProduct Organisation Diagnostic:product-organisation-diagnostic\nWorkshop:workshop\nAnderes Anliegen:anderes')

Node startPage = website.nodeExists('/start') ? website.getNode('/start') : null
Node offerTeaser = startPage != null
    ? findByProperty(startPage, 'heading', 'Drei Formate für unterschiedliche Situationen')
    : null
if (offerTeaser == null && startPage != null) {
    offerTeaser = findByProperty(startPage, 'heading', 'Vom konkreten Problem bis zum Organisationsbild')
}
if (offerTeaser != null) {
    setProperties(offerTeaser, [
        heading: 'Vom konkreten Problem bis zum Organisationsbild',
        text: '''<div class="mvp-grid mvp-grid--3">
  <article class="mvp-panel"><p class="mvp-meta">Bezahlter Einstieg</p><h3>Clarity Session &amp; Decision Review</h3><p>Eine konkrete Situation analysieren oder eine wichtige Entscheidung belastbar gegenprüfen.</p><p class="mvp-price">ab 295 € netto</p></article>
  <article class="mvp-panel"><p class="mvp-meta">Persönliches Sparring</p><h3>Leadership &amp; Product Sparring</h3><p>Vertrauliche Begleitung für laufende Entscheidungen, Kommunikation und Führung.</p><p class="mvp-price">ab 690 € netto</p></article>
  <article class="mvp-panel mvp-panel--accent"><p class="mvp-meta">Unternehmensangebote</p><h3>Diagnostik &amp; Workshops</h3><p>Ein belastbares Organisationsbild schaffen und gemeinsam an den relevanten Hebeln arbeiten.</p><p class="mvp-price">ab 1.600 € netto</p></article>
</div>
<p><a href="/magnoliaPublic/leistungen">Alle Leistungen im Detail</a></p>'''
    ])
}

website.save()

println 'Angebot aktualisiert: 7 Formate in 3 Stufen, Startseiten-Teaser und Auswahl im Kontaktformular.'
println 'Jetzt /start, /leistungen und /kontakt veröffentlichen.'
