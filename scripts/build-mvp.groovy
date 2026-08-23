import info.magnolia.context.MgnlContext
import groovy.transform.Field
import javax.jcr.Node
import javax.jcr.Session

/*
 * Einmalig in Magnolias Groovy-App ausführen.
 * Das Skript ist wiederholbar. Der bisherige Inhalt der Startseite wird beim
 * ersten Lauf unter /home/legacy-main gesichert.
 */

@Field final String PAGE_TEMPLATE = 'meine-website:pages/home'
@Field final String COMPONENT_PREFIX = 'meine-website:components/'

Session website = MgnlContext.getJCRSession('website')
Session navigation = MgnlContext.getJCRSession('navigation')
Session footer = MgnlContext.getJCRSession('footer')
Session siteSettings = MgnlContext.getJCRSession('siteSettings')

Node ensureNode(Node parent, String name, String type = 'mgnl:contentNode') {
    parent.hasNode(name) ? parent.getNode(name) : parent.addNode(name, type)
}

void setProperties(Node node, Map<String, Object> properties) {
    properties.each { String key, Object value ->
        if (value != null) {
            node.setProperty(key, value)
        }
    }
}

void removeChild(Node parent, String name) {
    if (parent.hasNode(name)) {
        parent.getNode(name).remove()
    }
}

Node createOrResetPage(Session session, String name, Map<String, Object> properties) {
    Node root = session.rootNode
    Node page
    if (name == 'home' && root.hasNode(name)) {
        page = root.getNode(name)
        if (page.hasNode('main') && !page.hasNode('legacy-main')) {
            session.move('/home/main', '/home/legacy-main')
        } else {
            removeChild(page, 'main')
        }
    } else {
        String legacyName = "legacy-${name}"
        if (root.hasNode(name)) {
            if (!root.hasNode(legacyName)) {
                session.move("/${name}", "/${legacyName}")
            } else {
                root.getNode(name).remove()
            }
        }
        page = root.addNode(name, 'mgnl:page')
    }

    setProperties(page, [
        'mgnl:template': PAGE_TEMPLATE,
        brandName: 'Christian Leonhardt',
        footerText: 'Product Leadership · Organisation · Transformation'
    ] + properties)
    page
}

Node addArea(Node page) {
    removeChild(page, 'main')
    page.addNode('main', 'mgnl:area')
}

Node addComponent(Node area, String template, Map<String, Object> properties = [:]) {
    String name = String.format('%02d', area.nodes.size)
    Node component = area.addNode(name, 'mgnl:component')
    component.setProperty('mgnl:template', template.contains(':') ? template : COMPONENT_PREFIX + template)
    setProperties(component, properties)
    component
}

Node addHero(Node area, String eyebrow, String title, String description, String target = null, String cta = null) {
    Node hero = addComponent(area, 'hero', [
        eyebrow: eyebrow,
        title: title,
        description: description
    ])
    if (target && cta) {
        Node chooser = hero.addNode('ctaChooser', 'mgnl:contentNode')
        chooser.setProperty('field', 'withCta')
        chooser.setProperty('ctaText', cta)
        Node link = chooser.addNode('ctaLink', 'mgnl:contentNode')
        link.setProperty('field', 'internalPageLink')
        link.setProperty('internalLink', target)
    }
    hero
}

Node addText(Node area, String heading, String html, boolean tinted = false) {
    addComponent(area, 'text', [
        heading: heading,
        text: html,
        showBackground: tinted
    ])
}

Node addCard(Node area, String title, String html, String target, String linkText = 'Mehr erfahren') {
    Node card = addComponent(area, 'cards', [
        title: title,
        description: html
    ])
    Node chooser = card.addNode('linkChooser', 'mgnl:contentNode')
    chooser.setProperty('field', 'internalLink')
    chooser.setProperty('internalLink', target)
    chooser.setProperty('linkText', linkText)
    card
}

Node addCta(Node area, String title, String html, String target = '/kontakt', String button = 'Situation klären') {
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

Node addFormEdit(Node fields, String name, String title, boolean mandatory = false, long rows = 1, String inputType = 'text', String description = null) {
    Node node = addComponent(fields, 'form:components/formEdit', [
        title: title,
        controlName: name,
        mandatory: mandatory,
        rows: rows,
        inputType: inputType,
        description: description
    ])
    node
}

Node createContactForm(Node area) {
    Node form = addComponent(area, 'contactForm', [
        formName: 'situation-klaeren',
        formText: 'Die Angaben helfen, das erste Gespräch gezielt vorzubereiten. Pflichtfelder sind mit * markiert.',
        requiredSymbol: '*',
        rightText: 'Pflichtfeld',
        errorTitle: 'Bitte prüfen Sie Ihre Angaben',
        successTitle: 'Danke für Ihre Anfrage',
        successMessage: 'Ihre Angaben wurden übermittelt. Ich melde mich persönlich bei Ihnen.',
        trackMail: true,
        contactMailFrom: 'kontakt@cleonhardt.de',
        contactMailTo: 'kontakt@cleonhardt.de',
        contactMailSubject: 'Neue Website-Anfrage von ${name}',
        contentType: 'text',
        contactMailBody: '''Neue Anfrage über cleonhardt.de

Anliegen: ${anliegen}
Name: ${name}
E-Mail: ${email}
Rolle / Position: ${rolle}
Unternehmen: ${unternehmen}

Aktuelle Situation:
${situation}

Gewünschtes Ergebnis:
${ziel}

Datenschutz: ${datenschutz}
'''
    ])

    Node fieldsets = form.addNode('fieldsets', 'mgnl:area')
    Node group = addComponent(fieldsets, 'form:components/formGroupFields', [title: 'Ihre Anfrage'])
    Node fields = group.addNode('fields', 'mgnl:area')

    Node concern = addComponent(fields, 'form:components/formSelection', [
        title: 'Worum geht es?',
        controlName: 'anliegen',
        type: 'select',
        mandatory: true,
        horizontal: false,
        multiple: false,
        labels: 'Bitte auswählen:\nClarity Session:clarity-session\nLeadership- oder Product-Sparring:sparring\nChOS Quick Diagnostic:quick-diagnostic\nAnderes Anliegen:anderes'
    ])

    addFormEdit(fields, 'name', 'Name', true, 1, 'text')
    addFormEdit(fields, 'email', 'E-Mail', true, 1, 'email')
    addFormEdit(fields, 'rolle', 'Rolle / Position', false, 1, 'text')
    addFormEdit(fields, 'unternehmen', 'Unternehmen', false, 1, 'text')
    addFormEdit(fields, 'situation', 'Wie sieht die aktuelle Situation aus?', true, 6, 'text')
    addFormEdit(fields, 'ziel', 'Welches Ergebnis wäre für Sie hilfreich?', true, 5, 'text')

    Node privacy = addComponent(fields, 'form:components/formSelection', [
        title: 'Datenschutz',
        controlName: 'datenschutz',
        type: 'checkbox',
        mandatory: true,
        horizontal: false,
        multiple: false,
        labels: 'Ich habe die Datenschutzerklärung zur Kenntnis genommen.:akzeptiert'
    ])

    addComponent(fields, 'form:components/formHoneypot', [
        title: 'Bitte nicht ausfüllen',
        controlName: 'website-url'
    ])
    addComponent(fields, 'form:components/formSubmit', [
        buttonText: 'Anfrage senden'
    ])
    form
}

Map<String, Node> pages = [:]

pages.home = createOrResetPage(website, 'home', [
    title: 'Start',
    navigationTitle: 'Start',
    windowTitle: 'Christian Leonhardt – Klarheit für komplexe Produktorganisationen',
    metaDescription: 'Product Leadership, Organisationsdiagnose und Transformation mit dem Christian Operating System.',
    hideInNavigation: false
])
Node home = addArea(pages.home)
addHero(home, 'Product Leadership · Organisation · Transformation',
    'Klarheit für komplexe Produkt\u00ADorganisationen.',
    'Ich helfe Produktverantwortlichen, Führungsteams und digitalen Organisationen, Ursachen zu verstehen, Entscheidungen zu schärfen und wirksame nächste Schritte zu gehen.',
    '/kontakt', 'Situation klären')
addText(home, 'Wenn mehr Methoden nicht mehr helfen', '''
<div class="section-intro"><p class="lead">Wachstum, Reorganisation oder hoher Lieferdruck machen Spannungen sichtbar. Häufig wird dann an Prozessen gearbeitet, obwohl die eigentliche Ursache tiefer liegt.</p></div>
<ul class="mvp-grid mvp-grid--3">
  <li class="mvp-panel"><h3>Unklare Verantwortung</h3><p>Entscheidungen wandern zwischen Produkt, Tech, Business und Führung hin und her.</p></li>
  <li class="mvp-panel"><h3>Viel Aktivität, wenig Wirkung</h3><p>Roadmaps sind voll, Meetings zahlreich – trotzdem fehlt ein gemeinsames Bild der Prioritäten.</p></li>
  <li class="mvp-panel"><h3>Symptome statt Ursachen</h3><p>Rollen, Rituale oder Tools werden geändert, ohne das zugrunde liegende System zu verstehen.</p></li>
</ul>''', true)
addText(home, 'Diagnose vor Eingriff. Klarheit vor Aktion.', '''
<p class="lead">Das Christian Operating System – kurz ChOS – verbindet Produktführung, Organisationsdiagnose und praktische Veränderungsarbeit.</p>
<blockquote class="mvp-quote">Nicht vorschnell reparieren, sondern zuerst verstehen, was im System tatsächlich wirkt.</blockquote>
<div class="mvp-grid mvp-grid--3">
  <div class="mvp-panel"><span class="mvp-number">1</span><h3>Beobachten</h3><p>Signale, Spannungen und wiederkehrende Muster sichtbar machen.</p></div>
  <div class="mvp-panel"><span class="mvp-number">2</span><h3>Einordnen</h3><p>Ursachen, Wechselwirkungen und relevante Hebel unterscheiden.</p></div>
  <div class="mvp-panel"><span class="mvp-number">3</span><h3>Handeln</h3><p>Einen tragfähigen nächsten Schritt definieren und bewusst testen.</p></div>
</div>
<p><a href="/magnoliaPublic/chos">Mehr über ChOS erfahren</a></p>''')
addText(home, 'Drei Formate für unterschiedliche Situationen', '''
<div class="mvp-grid mvp-grid--3">
  <article class="mvp-panel"><p class="mvp-meta">90 Minuten</p><h3>ChOS Clarity Session</h3><p>Eine konkrete Situation strukturiert verstehen und den sinnvollsten nächsten Schritt bestimmen.</p><p class="mvp-price">295 € netto</p></article>
  <article class="mvp-panel"><p class="mvp-meta">2 Wochen</p><h3>Leadership &amp; Product Sparring</h3><p>Vertrauliche Begleitung für laufende Entscheidungen, Kommunikation und Führung.</p><p class="mvp-price">690 € netto</p></article>
  <article class="mvp-panel mvp-panel--accent"><p class="mvp-meta">Organisationsdiagnose</p><h3>ChOS Quick Diagnostic</h3><p>Ein kompaktes Lagebild mit Interviews, Mustern, Risiken und priorisierten Empfehlungen.</p><p class="mvp-price">ab 3.900 € netto</p></article>
</div>
<p><a href="/magnoliaPublic/leistungen">Alle Leistungen im Detail</a></p>''', true)
addText(home, 'Erfahrung, die Strategie und Alltag verbindet', '''
<p class="lead">Ich arbeite an der Schnittstelle von Produkt, Organisation, Führung und Transformation – dort, wo einfache Best Practices selten ausreichen.</p>
<ul class="mvp-checks">
  <li>Produktführung in komplexen digitalen Umfeldern</li>
  <li>Organisationsentwicklung und Rollenklärung</li>
  <li>CRM, Loyalty und E-Commerce</li>
  <li>Transformation mit Blick auf Menschen, Strukturen und Entscheidungslogik</li>
</ul>
<p><a href="/magnoliaPublic/ueber-mich">Mehr über meinen Hintergrund</a></p>''')
addCta(home, 'Bringen wir Klarheit in Ihre Situation.', '<p>Die Clarity Session ist ein fokussierter Einstieg: konkret, vertraulich und ohne künstlich aufgeblähtes Beratungsprojekt.</p>')

pages.leistungen = createOrResetPage(website, 'leistungen', [
    title: 'Leistungen',
    navigationTitle: 'Leistungen',
    windowTitle: 'Leistungen – Christian Leonhardt',
    metaDescription: 'Clarity Session, Product Sparring und ChOS Quick Diagnostic für Produktverantwortliche und digitale Organisationen.',
    hideInNavigation: false
])
Node leistungen = addArea(pages.leistungen)
addHero(leistungen, 'Leistungen', 'Der passende Rahmen für Ihre aktuelle Situation.',
    'Vom fokussierten Einzeltermin bis zur kompakten Organisationsdiagnose: Der Umfang folgt dem Problem – nicht umgekehrt.',
    '/kontakt', 'Situation klären')
addText(leistungen, 'ChOS Clarity Session', '''
<p class="mvp-meta">90 Minuten · 295 € netto</p>
<p class="lead">Für eine konkrete, festgefahrene oder schwer einzuordnende Produkt- oder Führungssituation.</p>
<div class="mvp-grid mvp-grid--2">
  <div class="mvp-panel"><h3>Was enthalten ist</h3><ul class="mvp-checks"><li>Kurzer Vorbereitungsfragebogen</li><li>90 Minuten strukturierte Analyse</li><li>Einordnung von Symptomen, Ursachen und Spannungen</li><li>Konkrete nächste Schritte</li><li>Schriftlicher ChOS Clarity Brief</li></ul></div>
  <div class="mvp-panel mvp-panel--accent"><h3>Gut geeignet, wenn …</h3><p>eine Entscheidung blockiert ist, Verantwortlichkeiten diffus sind, Teams aneinander vorbeiarbeiten oder Sie vor einem Eingriff erst Klarheit gewinnen möchten.</p><p><strong>Nicht gedacht als:</strong> vollständige Lösung eines komplexen Organisationsproblems in einem Termin.</p></div>
</div>''')
addText(leistungen, 'Leadership & Product Sparring', '''
<p class="mvp-meta">2 Wochen · 690 € netto · Pilotpreis 590 € netto</p>
<p class="lead">Ein vertraulicher Denkraum für Führungskräfte und Produktverantwortliche, die über mehrere Entscheidungen hinweg Rückhalt brauchen.</p>
<div class="mvp-grid mvp-grid--3"><div class="mvp-panel"><span class="mvp-number">1</span><h3>Kick-off</h3><p>60 Minuten, um Situation, Ziel und relevante Entscheidungen zu schärfen.</p></div><div class="mvp-panel"><span class="mvp-number">2</span><h3>Asynchrones Sparring</h3><p>Zwei Wochen kurze, gezielte Rückfragen und Feedback zu Entscheidungen oder Kommunikation.</p></div><div class="mvp-panel"><span class="mvp-number">3</span><h3>Abschluss</h3><p>45 Minuten Auswertung plus schriftliche Zusammenfassung der wichtigsten Erkenntnisse.</p></div></div>''', true)
addText(leistungen, 'ChOS Quick Diagnostic', '''
<p class="mvp-meta">Kompakte Organisationsdiagnose · ab 3.900 € netto · limitierter Pilot ab 2.900 € netto</p>
<p class="lead">Wenn einzelne Symptome auf ein größeres Systemproblem hindeuten und ein belastbares gemeinsames Lagebild fehlt.</p>
<div class="mvp-grid mvp-grid--2"><div class="mvp-panel"><h3>Ablauf</h3><ul class="mvp-checks"><li>Briefing und Materialsichtung</li><li>Zwei bis vier fokussierte Interviews</li><li>Analyse der Organisations- und Entscheidungslogik</li><li>Ergebnisgespräch mit Empfehlungen</li></ul></div><div class="mvp-panel"><h3>Ergebnis</h3><p>Sie erhalten eine verdichtete Diagnose: zentrale Muster, Spannungen und Risiken, wahrscheinliche Ursachen sowie priorisierte Handlungsfelder für die nächsten Wochen.</p></div></div>''')
addText(leistungen, 'Welches Format passt?', '''
<div class="mvp-grid mvp-grid--3"><div class="mvp-panel"><h3>Eine Situation</h3><p>Wählen Sie die <strong>Clarity Session</strong>, wenn Sie einen konkreten Knoten lösen oder eine Entscheidung vorbereiten möchten.</p></div><div class="mvp-panel"><h3>Mehrere Entscheidungen</h3><p>Wählen Sie das <strong>Sparring</strong>, wenn Sie über zwei Wochen hinweg reflektierte Begleitung brauchen.</p></div><div class="mvp-panel"><h3>Ein Systembild</h3><p>Wählen Sie das <strong>Quick Diagnostic</strong>, wenn mehrere Personen, Schnittstellen und Muster beteiligt sind.</p></div></div>''', true)
addCta(leistungen, 'Sie müssen das Format nicht vorab kennen.', '<p>Beschreiben Sie kurz Ihre Lage. Gemeinsam klären wir, welcher nächste Schritt sinnvoll ist.</p>')

pages.chos = createOrResetPage(website, 'chos', [
    title: 'Christian Operating System',
    navigationTitle: 'ChOS',
    windowTitle: 'ChOS – Christian Operating System',
    metaDescription: 'Ein Diagnose- und Handlungsrahmen für Produktführung, Organisation und Transformation.',
    hideInNavigation: false
])
Node chos = addArea(pages.chos)
addHero(chos, 'Christian Operating System', 'Erst verstehen. Dann wirksam verändern.',
    'ChOS ist ein pragmatischer Diagnose- und Handlungsrahmen für komplexe Produktorganisationen. Er schafft ein gemeinsames Bild, bevor Maßnahmen beschlossen werden.',
    '/kontakt', 'ChOS anwenden')
addText(chos, 'Warum ChOS?', '''
<p class="lead">In komplexen Organisationen erzeugt dieselbe Maßnahme je nach Kontext völlig unterschiedliche Wirkung. Deshalb beginnt ChOS nicht mit einer Methode, sondern mit einer Diagnose.</p>
<blockquote class="mvp-quote">Diagnose vor Eingriff. Klarheit vor Aktion.</blockquote>
<p>Der Rahmen verbindet Beobachtungen aus Produktarbeit, Führung und Organisation. Er hilft, zwischen sichtbaren Symptomen und den Mechanismen zu unterscheiden, die sie immer wieder erzeugen.</p>''')
addText(chos, 'Fünf Perspektiven auf das System', '''
<div class="mvp-model">
  <div class="mvp-model__row"><strong>Richtung</strong><span>Ist klar, welches Problem und welche Wirkung wirklich Priorität haben?</span></div>
  <div class="mvp-model__row"><strong>Entscheidungen</strong><span>Wer entscheidet was – mit welchem Mandat und anhand welcher Informationen?</span></div>
  <div class="mvp-model__row"><strong>Verantwortung</strong><span>Sind Ergebnisverantwortung, Rollen und Schnittstellen tatsächlich zusammengeführt?</span></div>
  <div class="mvp-model__row"><strong>Zusammenarbeit</strong><span>Wie fließen Wissen, Konflikte und Feedback zwischen Teams und Führung?</span></div>
  <div class="mvp-model__row"><strong>Lernen</strong><span>Kann die Organisation Annahmen prüfen und ihr Verhalten aus realer Wirkung anpassen?</span></div>
</div>''', true)
addText(chos, 'Vom Signal zum nächsten Schritt', '''
<div class="mvp-grid mvp-grid--3"><div class="mvp-panel"><span class="mvp-number">1</span><h3>Signale sammeln</h3><p>Beobachtbares Verhalten und konkrete Situationen – nicht nur Meinungen oder Organigramme.</p></div><div class="mvp-panel"><span class="mvp-number">2</span><h3>Muster prüfen</h3><p>Wiederholungen, Abhängigkeiten und widersprüchliche Anreize über mehrere Perspektiven hinweg.</p></div><div class="mvp-panel"><span class="mvp-number">3</span><h3>Hebel wählen</h3><p>Eine Intervention, die klein genug zum Lernen und relevant genug für echte Wirkung ist.</p></div></div>
<h3>Kein starres Reifegradmodell</h3><p>ChOS bewertet Organisationen nicht anhand eines vermeintlichen Idealzustands. Entscheidend ist, ob Strukturen und Verhalten zur jeweiligen Aufgabe, Strategie und Entwicklungsphase passen.</p>''')
addCta(chos, 'Möchten Sie ChOS auf eine reale Situation anwenden?', '<p>Die Clarity Session macht den Ansatz in 90 Minuten konkret und liefert einen schriftlichen Clarity Brief.</p>', '/kontakt', 'Clarity Session anfragen')

pages.ueber = createOrResetPage(website, 'ueber-mich', [
    title: 'Über Christian',
    navigationTitle: 'Über mich',
    windowTitle: 'Über Christian Leonhardt',
    metaDescription: 'Über Christian Leonhardt und seinen Ansatz für Product Leadership, Organisationsentwicklung und Transformation.',
    hideInNavigation: false
])
Node ueber = addArea(pages.ueber)
addHero(ueber, 'Über mich', 'Komplexität lässt sich nicht wegmoderieren.',
    'Ich verbinde Produktdenken, Führung und Organisationsdiagnose – mit dem Anspruch, schwierige Situationen klarer und handhabbar zu machen.',
    '/kontakt', 'Kennenlernen')
addText(ueber, 'Christian Leonhardt', '''
<p class="lead">Ich begleite Produktverantwortliche, Führungsteams und digitale Organisationen in Situationen, in denen Methodenwissen allein nicht weiterhilft.</p>
<p>Meine Arbeit liegt an den Übergängen: zwischen Strategie und Umsetzung, Produkt und Technologie, Verantwortung und formaler Struktur, Wachstum und notwendiger Neuordnung. Gerade dort entstehen Reibung, Verzögerung und Missverständnisse – aber auch die größten Hebel.</p>
<p>Statt vorschnell ein Zielbild über die Organisation zu legen, arbeite ich zuerst heraus, welches Problem tatsächlich gelöst werden muss. Daraus entstehen klare Entscheidungen und Veränderungen, die zum Kontext passen.</p>''')
addText(ueber, 'Wofür ich stehe', '''
<div class="mvp-grid mvp-grid--3"><div class="mvp-panel"><h3>Klarheit</h3><p>Komplexe Sachverhalte auf ihren Kern reduzieren, ohne sie unzulässig zu vereinfachen.</p></div><div class="mvp-panel"><h3>Verantwortung</h3><p>Entscheidungen sichtbar machen und Menschen befähigen, echte Ergebnisverantwortung zu übernehmen.</p></div><div class="mvp-panel"><h3>Pragmatismus</h3><p>Mit dem kleinsten sinnvollen Eingriff lernen, statt große Veränderungsprogramme zu simulieren.</p></div></div>''', true)
addText(ueber, 'Thematische Erfahrung', '''
<ul class="mvp-checks"><li>Aufbau und Führung digitaler Produktorganisationen</li><li>Produktstrategie, Portfolio und Priorisierung</li><li>Rollen, Mandate und Entscheidungsarchitektur</li><li>CRM, Loyalty und E-Commerce</li><li>Zusammenarbeit zwischen Business, Produkt und Technologie</li><li>Transformation in wachsenden oder neu ausgerichteten Organisationen</li></ul>
<p>Der gemeinsame Nenner: Ein belastbares Bild der Situation schaffen und daraus handlungsfähige Führung entwickeln.</p>''')
addCta(ueber, 'Lassen Sie uns über Ihre Situation sprechen.', '<p>Ein kurzes Anliegen reicht für den Anfang. Ich melde mich persönlich zurück.</p>')

pages.insights = createOrResetPage(website, 'insights', [
    title: 'Insights',
    navigationTitle: 'Insights',
    windowTitle: 'Insights – Product Leadership & Organisation',
    metaDescription: 'Gedanken und Werkzeuge zu Produktorganisation, Führung, Diagnose und Transformation.',
    hideInNavigation: false
])
Node insights = addArea(pages.insights)
addHero(insights, 'Insights', 'Produktorganisationen besser verstehen.',
    'Beobachtungen, Denkmodelle und praktische Impulse für Product Leadership, Organisation und Transformation.')
addText(insights, 'Drei Gedanken zum Einstieg', '<p class="lead">Keine Patentrezepte, sondern Perspektiven, die helfen, die richtigen Fragen zu stellen.</p>')
addCard(insights, 'Warum Produktorganisationen nicht an fehlenden Methoden scheitern',
    '<p>Frameworks sind selten der Engpass. Entscheidend ist, ob Richtung, Verantwortung und Entscheidungslogik zusammenpassen.</p>',
    '/insights/warum-produktorganisationen-nicht-an-fehlenden-methoden-scheitern', 'Artikel lesen')
addCard(insights, 'Diagnose vor Eingriff: Ursachen von Symptomen unterscheiden',
    '<p>Wie Sie vermeiden, ein sichtbares Problem mit einer plausiblen, aber wirkungslosen Maßnahme zu beantworten.</p>',
    '/insights/diagnose-vor-eingriff', 'Artikel lesen')
addCard(insights, 'Unklare Rollen sind selten das eigentliche Problem',
    '<p>Warum Rollenklarheit ohne Entscheidungs- und Anreizklarheit oft nur ein neues Dokument produziert.</p>',
    '/insights/unklare-rollen-sind-selten-das-eigentliche-problem', 'Artikel lesen')
addCta(insights, 'Ein Thema kommt Ihnen bekannt vor?', '<p>Bringen Sie eine konkrete Situation in die Clarity Session mit.</p>')

Node article1 = pages.insights.addNode('warum-produktorganisationen-nicht-an-fehlenden-methoden-scheitern', 'mgnl:page')
setProperties(article1, [
    'mgnl:template': PAGE_TEMPLATE, title: 'Warum Produktorganisationen nicht an fehlenden Methoden scheitern',
    navigationTitle: 'Methoden sind selten der Engpass',
    windowTitle: 'Warum Produktorganisationen nicht an fehlenden Methoden scheitern',
    metaDescription: 'Warum zusätzliche Frameworks strukturelle Probleme in Produktorganisationen selten lösen.',
    hideInNavigation: true, brandName: 'Christian Leonhardt'
])
Node a1 = addArea(article1)
addHero(a1, 'Insight · Product Leadership', 'Warum Produktorganisationen nicht an fehlenden Methoden scheitern',
    'Frameworks sind sichtbar, erlernbar und versprechen Ordnung. Genau deshalb greifen Organisationen gern zu ihnen – auch wenn das eigentliche Problem woanders liegt.')
addText(a1, 'Methoden sind attraktiv. Systeme sind unbequem.', '''
<p class="lead">Wenn Produktarbeit stockt, folgen oft dieselben Antworten: ein neues Priorisierungsframework, ein anderes Teammodell, mehr Discovery oder ein überarbeiteter Prozess. Das kann sinnvoll sein. Häufig bleibt die Wirkung trotzdem aus.</p>
<h3>Das sichtbare Problem</h3><p>Teams liefern langsam, Stakeholder umgehen die Roadmap oder Entscheidungen werden immer wieder eskaliert. Ein Prozess scheint zu fehlen. Die naheliegende Intervention lautet: Prozess ergänzen.</p>
<h3>Der unsichtbare Mechanismus</h3><p>Vielleicht dürfen Teams aber gar keine relevanten Entscheidungen treffen. Vielleicht widersprechen sich Umsatz-, Projekt- und Produktziele. Vielleicht ist die formale Verantwortung nicht mit Informationen oder Mandaten verbunden. In diesen Fällen verbessert eine Methode vor allem die Oberfläche.</p>
<blockquote class="mvp-quote">Eine Methode kann fehlende Verantwortung nicht ersetzen. Sie macht nur genauer sichtbar, dass sie fehlt.</blockquote>
<h3>Drei Fragen vor dem nächsten Framework</h3><ol><li><strong>Welche konkrete Entscheidung gelingt heute nicht?</strong> Nicht „Priorisierung ist schwierig“, sondern: Wer kann welche Option aus welchem Grund nicht wählen?</li><li><strong>Welche Anreize stabilisieren das heutige Verhalten?</strong> Menschen handeln oft rational innerhalb widersprüchlicher Ziele.</li><li><strong>Welche Information fehlt am Ort der Entscheidung?</strong> Ohne Kunden-, Wirkungs- oder Risikodaten bleibt auch ein guter Prozess politisch.</li></ol>
<h3>Die kleinere, wirksamere Intervention</h3><p>Oft reicht zunächst ein begrenzter Test: ein klares Mandat für eine Entscheidung, ein gemeinsames Erfolgskriterium oder ein bewusst aufgelöster Zielkonflikt. Erst wenn klar ist, welcher Mechanismus verändert werden soll, lässt sich entscheiden, ob ein Framework dabei hilft.</p>
<p>Das ist langsamer als eine Methode auszurufen – und deutlich schneller als sie nach sechs Monaten wieder auszutauschen.</p>''')
addCta(a1, 'Welche Methode soll bei Ihnen gerade ein Systemproblem lösen?', '<p>In der Clarity Session trennen wir sichtbares Symptom, wahrscheinliche Ursache und nächsten Test.</p>')

Node article2 = pages.insights.addNode('diagnose-vor-eingriff', 'mgnl:page')
setProperties(article2, [
    'mgnl:template': PAGE_TEMPLATE, title: 'Diagnose vor Eingriff',
    navigationTitle: 'Diagnose vor Eingriff',
    windowTitle: 'Diagnose vor Eingriff: Ursachen von Symptomen unterscheiden',
    metaDescription: 'Ein praktischer Ansatz, um Symptome, Muster und Ursachen in Produktorganisationen zu unterscheiden.',
    hideInNavigation: true, brandName: 'Christian Leonhardt'
])
Node a2 = addArea(article2)
addHero(a2, 'Insight · Organisationsdiagnose', 'Diagnose vor Eingriff: Ursachen von Symptomen unterscheiden',
    'Ein plausibler Eingriff ist noch kein wirksamer Eingriff. Gute Veränderungsarbeit beginnt deshalb mit überprüfbaren Hypothesen.')
addText(a2, 'Vom Signal zur Hypothese', '''
<p class="lead">„Unsere Rollen sind unklar.“ „Wir priorisieren nicht konsequent.“ „Die Teams übernehmen zu wenig Verantwortung.“ Solche Sätze enthalten wichtige Signale – aber noch keine Diagnose.</p>
<h3>1. Beobachtung von Deutung trennen</h3><p>Eine belastbare Beobachtung beschreibt eine Situation: Eine Entscheidung wurde in drei Gremien erneut geöffnet. Zwei Teams arbeiten am selben Kundenschmerz mit unterschiedlichen Zielen. Eine Führungskraft genehmigt regelmäßig Details, die laut Rollenbild im Team liegen.</p>
<p>„Fehlende Ownership“ ist dagegen bereits eine Deutung. Sie kann stimmen, sie kann aber auch das Ergebnis fehlender Mandate, gegensätzlicher Ziele oder realer Risiken sein.</p>
<h3>2. Nach Wiederholung suchen</h3><p>Ein einzelner Vorfall ist selten eine Organisationsdiagnose. Relevant werden Signale, wenn sie in unterschiedlichen Situationen nach demselben Muster auftreten. Wer wird regelmäßig übergangen? Wo entstehen Warteschleifen? Welche Konflikte werden vertagt und tauchen später wieder auf?</p>
<h3>3. Mehrere plausible Ursachen zulassen</h3><p>Für dasselbe Symptom sollten zunächst mindestens zwei alternative Erklärungen bestehen. Langsame Entscheidungen können aus unklaren Rechten, fehlenden Daten, hohem Risiko oder einem ungelösten Zielkonflikt entstehen. Eine gute Diagnose versucht, diese Erklärungen zu widerlegen.</p>
<h3>4. Den kleinsten aussagekräftigen Test wählen</h3><p>Statt sofort die gesamte Organisation umzubauen, lässt sich ein Mechanismus oft in begrenztem Rahmen prüfen: ein explizites Mandat, ein gemeinsames Ziel, ein anderes Informationsformat oder eine befristete Entscheidung ohne zusätzliche Freigabe.</p>
<blockquote class="mvp-quote">Die Qualität einer Intervention hängt weniger von ihrer Größe ab als von der Klarheit der Annahme, die sie prüft.</blockquote>
<h3>Was danach anders ist</h3><p>Diagnose beseitigt Unsicherheit nicht vollständig. Sie macht sie handhabbar. Führung kann benennen, worauf eine Entscheidung beruht, welche Wirkung erwartet wird und woran sichtbar wird, ob nachgesteuert werden muss.</p>''', true)
addCta(a2, 'Ein Symptom, mehrere mögliche Ursachen?', '<p>Die Clarity Session schafft ein strukturiertes Lagebild und einen überprüfbaren nächsten Schritt.</p>')

Node article3 = pages.insights.addNode('unklare-rollen-sind-selten-das-eigentliche-problem', 'mgnl:page')
setProperties(article3, [
    'mgnl:template': PAGE_TEMPLATE, title: 'Unklare Rollen sind selten das eigentliche Problem',
    navigationTitle: 'Unklare Rollen',
    windowTitle: 'Unklare Rollen sind selten das eigentliche Problem',
    metaDescription: 'Warum Rollenbeschreibungen allein Verantwortung und Zusammenarbeit nicht klären.',
    hideInNavigation: true, brandName: 'Christian Leonhardt'
])
Node a3 = addArea(article3)
addHero(a3, 'Insight · Führung', 'Unklare Rollen sind selten das eigentliche Problem',
    'Neue Rollenbeschreibungen schaffen formale Ordnung. Ob dadurch bessere Entscheidungen entstehen, hängt von vier weiteren Bedingungen ab.')
addText(a3, 'Eine Rolle ist nur so klar wie ihr Umfeld', '''
<p class="lead">Wenn Zusammenarbeit schwierig wird, landet die Organisation schnell bei RACI, neuen Stellenprofilen oder einer überarbeiteten Aufbauorganisation. Danach ist dokumentiert, wer verantwortlich sein soll. Im Alltag bleibt vieles unverändert.</p>
<h3>Mandat</h3><p>Eine Rolle ohne Entscheidungsrecht ist eine Erwartung ohne Handlungsfähigkeit. Wer ein Ergebnis verantwortet, muss relevante Entscheidungen treffen oder zumindest transparent herbeiführen können.</p>
<h3>Information</h3><p>Entscheidungsrechte helfen wenig, wenn Kundenwissen, Wirtschaftsdaten oder technische Risiken anderswo liegen. Gute Rollenklärung fragt deshalb auch: Welche Information muss zuverlässig zu dieser Rolle fließen?</p>
<h3>Anreize</h3><p>Wenn Teams am Produktergebnis gemessen werden, ihre Stakeholder aber an Projektumfang und Terminen, entsteht struktureller Konflikt. Menschen können ihre Rolle perfekt verstehen und trotzdem gegeneinander arbeiten.</p>
<h3>Beziehungen</h3><p>Verantwortung entsteht nicht allein im Organigramm. Sie wird in Abstimmungen, Konflikten und Eskalationen laufend bestätigt oder entwertet. Greift Führung bei jeder Unsicherheit ein, lernt das System, Entscheidungen nach oben abzugeben.</p>
<blockquote class="mvp-quote">Rollenklarheit ist kein Dokumentzustand. Sie zeigt sich darin, wie reale Entscheidungen zustande kommen.</blockquote>
<h3>Eine bessere Rollenklärung</h3><p>Beginnen Sie nicht mit einer vollständigen Rollenmatrix. Nehmen Sie zwei oder drei wiederkehrende Entscheidungen und verfolgen Sie deren Weg: Wer bringt sie ein? Wer hat relevante Informationen? Wer trägt die Folgen? Wer kann blockieren? Wo wird dieselbe Entscheidung erneut geöffnet?</p>
<p>Aus diesen Fällen lässt sich eine konkrete Vereinbarung ableiten: Entscheidung, Mandat, benötigte Information, Konsultation und Eskalationsbedingung. Erst danach lohnt es sich, das Muster auf weitere Bereiche zu übertragen.</p>''')
addCta(a3, 'Wo bleibt Verantwortung bei Ihnen hängen?', '<p>Wir betrachten eine konkrete Entscheidung und machen den zugrunde liegenden Mechanismus sichtbar.</p>')

pages.kontakt = createOrResetPage(website, 'kontakt', [
    title: 'Kontakt',
    navigationTitle: 'Kontakt',
    windowTitle: 'Kontakt – Situation klären',
    metaDescription: 'Kontakt zu Christian Leonhardt für Clarity Session, Product Sparring oder Organisationsdiagnose.',
    hideInNavigation: false
])
Node kontakt = addArea(pages.kontakt)
addHero(kontakt, 'Kontakt', 'Was soll klarer werden?',
    'Beschreiben Sie die Situation in wenigen Sätzen. Ich prüfe persönlich, ob und in welchem Format ich sinnvoll unterstützen kann.')
addText(kontakt, 'So geht es weiter', '''
<div class="mvp-grid mvp-grid--3"><div class="mvp-panel"><span class="mvp-number">1</span><h3>Anfrage</h3><p>Sie schildern kurz Anliegen, Kontext und gewünschtes Ergebnis.</p></div><div class="mvp-panel"><span class="mvp-number">2</span><h3>Rückmeldung</h3><p>Ich melde mich persönlich mit einer ersten Einordnung und möglichen nächsten Schritten.</p></div><div class="mvp-panel"><span class="mvp-number">3</span><h3>Start</h3><p>Wenn es passt, vereinbaren wir Clarity Session, Sparring oder Diagnostic.</p></div></div>
<p><strong>Hinweis:</strong> Ihre Anfrage ist unverbindlich. Vertrauliche Detailinformationen gehören erst in ein persönliches Gespräch.</p>''', true)
createContactForm(kontakt)

pages.impressum = createOrResetPage(website, 'impressum', [
    title: 'Impressum',
    navigationTitle: 'Impressum',
    windowTitle: 'Impressum – Christian Leonhardt',
    metaDescription: 'Impressum der Website von Christian Leonhardt.',
    hideInNavigation: true
])
Node impressum = addArea(pages.impressum)
addHero(impressum, 'Rechtliches', 'Impressum', 'Anbieterkennzeichnung und Kontaktangaben.')
addText(impressum, 'Impressum', '''
<h3>Angaben gemäß § 5 DDG</h3>
<p>Christian Leonhardt<br>Am Spelzgarten 18<br>50129 Bergheim<br>Deutschland</p>
<h3>Kontakt</h3>
<p>Telefon: <a href="tel:+4915259765342">0152 59765342</a><br>E-Mail: <a href="mailto:kontakt@cleonhardt.de">kontakt@cleonhardt.de</a></p>
<h3>Verantwortlich für journalistisch-redaktionelle Inhalte gemäß § 18 Abs. 2 MStV</h3>
<p>Christian Leonhardt<br>Am Spelzgarten 18<br>50129 Bergheim<br>Deutschland</p>''')

pages.datenschutz = createOrResetPage(website, 'datenschutz', [
    title: 'Datenschutz',
    navigationTitle: 'Datenschutz',
    windowTitle: 'Datenschutz – Christian Leonhardt',
    metaDescription: 'Informationen zum Datenschutz auf dieser Website.',
    hideInNavigation: true
])
Node datenschutz = addArea(pages.datenschutz)
addHero(datenschutz, 'Rechtliches', 'Datenschutz', 'Informationen zur Verarbeitung personenbezogener Daten auf dieser Website.')
addText(datenschutz, 'Datenschutzerklärung', '''
<p><strong>Stand: 29. Juli 2026</strong></p>
<p>Diese Datenschutzerklärung beschreibt die Verarbeitung personenbezogener Daten beim Besuch von cleonhardt.de und bei einer Kontaktaufnahme. Die Website verwendet derzeit keine Reichweitenanalyse, keine Marketing-Tracker, keinen Newsletterdienst und keine eingebetteten Video- oder Social-Media-Dienste.</p>

<h3>1. Verantwortlicher</h3>
<p>Christian Leonhardt<br>Am Spelzgarten 18<br>50129 Bergheim<br>Deutschland</p>
<p>Telefon: <a href="tel:+4915259765342">0152 59765342</a><br>E-Mail: <a href="mailto:kontakt@cleonhardt.de">kontakt@cleonhardt.de</a></p>

<h3>2. Bereitstellung und Hosting der Website</h3>
<p>Die Website und die geschäftliche E-Mail-Infrastruktur werden bei <strong>HOSTINGER operations, UAB</strong>, Švitrigailos str. 34, LT-03230 Vilnius, Litauen („Hostinger“) betrieben. Hostinger verarbeitet Daten als Auftragsverarbeiter auf Grundlage der in den Vertrag einbezogenen Vereinbarung zur Auftragsverarbeitung.</p>
<p>Beim Aufruf der Website werden technisch erforderliche Verbindungsdaten verarbeitet. Dazu können insbesondere IP-Adresse, Datum und Uhrzeit, angeforderte Seite oder Datei, übertragene Datenmenge, HTTP-Status, Browser- und Geräteinformationen sowie technische Fehlerdaten gehören. Diese Verarbeitung dient der sicheren, stabilen und fehlerfreien Bereitstellung der Website sowie der Erkennung und Abwehr von Angriffen. Rechtsgrundlage ist Art. 6 Abs. 1 lit. f DSGVO. Unser berechtigtes Interesse liegt im sicheren und zuverlässigen Betrieb der Website.</p>
<p>Zugriffs- und Anwendungsprotokolle werden auf dem von uns betriebenen Server grundsätzlich für höchstens 30 Tage vorgehalten und anschließend automatisch gelöscht. Eine längere Speicherung erfolgt nur, wenn sie zur Aufklärung eines konkreten Sicherheitsvorfalls erforderlich ist. Sicherungskopien werden turnusmäßig überschrieben; im Wiederherstellungsfall können darin enthaltene Daten vorübergehend erneut verarbeitet werden.</p>
<p>Weitere Informationen: <a href="https://www.hostinger.com/de/legal/datenschutz-bestimmungen">Datenschutz bei Hostinger</a> und <a href="https://www.hostinger.com/de/legal/dpa">Vereinbarung zur Auftragsverarbeitung</a>.</p>

<h3>3. Technisch erforderliche Cookies</h3>
<p>Die Website setzt ausschließlich technisch erforderliche Sicherheitsmechanismen ein. Beim Seitenaufruf kann insbesondere ein mit <code>csrf</code> bezeichnetes Cookie gesetzt werden. Es schützt Formulare und Anfragen gegen missbräuchliche Übermittlung, ist nur über eine verschlüsselte Verbindung nutzbar, für Skripte nicht auslesbar und wird nicht zu Analyse- oder Werbezwecken verwendet.</p>
<p>Das Speichern technisch erforderlicher Informationen auf Ihrem Endgerät erfolgt auf Grundlage von § 25 Abs. 2 Nr. 2 TDDDG. Soweit dabei personenbezogene Daten verarbeitet werden, ist Art. 6 Abs. 1 lit. f DSGVO die Rechtsgrundlage. Da derzeit keine einwilligungspflichtigen Technologien eingesetzt werden, erscheint kein Einwilligungsbanner.</p>

<h3>4. Kontaktformular und HubSpot CRM</h3>
<p>Wenn Sie das Kontaktformular absenden, werden die von Ihnen eingegebenen Daten unmittelbar an unser Kundenbeziehungsmanagement und den gemeinsamen Posteingang von HubSpot übermittelt. Verarbeitet werden Name, E-Mail-Adresse, optional Unternehmen und berufliche Rolle, der gewählte Anlass, Ihre Beschreibung der aktuellen Situation und des gewünschten Ergebnisses, Zeitpunkt und Herkunftsseite der Anfrage sowie technisch erforderliche Verbindungs- und Anfragedaten. Bitte übermitteln Sie keine besonderen Kategorien personenbezogener Daten, etwa Gesundheitsdaten, über das Formular.</p>
<p>Wir nutzen hierfür HubSpot. Vertragspartner für Kunden in Deutschland ist <strong>HubSpot Germany GmbH</strong>. HubSpot verarbeitet die Formulardaten in unserem Auftrag, stellt sie im CRM und im Conversations-Posteingang bereit und ermöglicht die Bearbeitung und Beantwortung Ihrer Anfrage. Die Verarbeitung erfolgt zur Durchführung vorvertraglicher Maßnahmen oder zur Vertragserfüllung auf Grundlage von Art. 6 Abs. 1 lit. b DSGVO. Bei allgemeinen Anfragen ist Art. 6 Abs. 1 lit. f DSGVO die Rechtsgrundlage; unser berechtigtes Interesse liegt in einer strukturierten, sicheren und effizienten Bearbeitung von Anfragen.</p>
<p>Die Pflichtbox unter dem Formular bestätigt lediglich, dass Sie diese Datenschutzerklärung zur Kenntnis genommen haben. Sie ist keine Einwilligung in Werbung. Eine werbliche Ansprache oder Aufnahme in einen Newsletter erfolgt dadurch nicht.</p>
<p>Unser HubSpot-Account wird in der EU-Region betrieben; nach Angaben von HubSpot liegt das regionale Rechenzentrum in Deutschland. Im Rahmen von Support, Sicherheit, Unterauftragsverarbeitung und der globalen Bereitstellung des Dienstes können Daten dennoch durch verbundene Unternehmen oder Dienstleister außerhalb des Europäischen Wirtschaftsraums verarbeitet werden. HubSpot verwendet hierfür nach eigenen Angaben insbesondere Angemessenheitsbeschlüsse wie das EU-US Data Privacy Framework und – soweit erforderlich – die Standardvertragsklauseln der Europäischen Kommission. Die HubSpot-Vereinbarung zur Datenverarbeitung ist in die Nutzungsbedingungen einbezogen.</p>
<p>Wir verwenden auf dieser Website keinen HubSpot-Tracking-Code und übertragen kein HubSpot-Tracking-Cookie mit der Formularanfrage. Das Formular dient ausschließlich der Kontaktaufnahme.</p>
<p>Anfragen ohne anschließende Geschäftsbeziehung werden grundsätzlich spätestens zwölf Monate nach abschließender Bearbeitung gelöscht, sofern keine gesetzlichen Pflichten oder die Geltendmachung, Ausübung oder Verteidigung von Rechtsansprüchen eine längere Speicherung erfordern. Entsteht eine Geschäftsbeziehung, gelten zusätzlich die gesetzlichen handels- und steuerrechtlichen Aufbewahrungsfristen.</p>
<p>Weitere Informationen: <a href="https://legal.hubspot.com/de/privacy-policy">Datenschutzerklärung von HubSpot</a>, <a href="https://legal.hubspot.com/de/dpa">Vereinbarung zur Datenverarbeitung</a>, <a href="https://legal.hubspot.com/de/sub-processors-page">Unterauftragsverarbeiter</a> und <a href="https://knowledge.hubspot.com/de/account-security/hubspot-cloud-infrastructure-and-data-hosting-frequently-asked-questions">Datenhosting bei HubSpot</a>.</p>

<h3>5. Kontaktaufnahme per E-Mail oder Telefon</h3>
<p>Wenn Sie uns per E-Mail oder Telefon kontaktieren, verarbeiten wir Ihre Kontaktdaten, den Inhalt Ihrer Nachricht und die damit verbundenen Kommunikationsdaten zur Bearbeitung Ihres Anliegens. E-Mails werden über die Infrastruktur von Hostinger übermittelt. Soweit erforderlich, wird die Kommunikation zur einheitlichen Bearbeitung im HubSpot CRM dokumentiert.</p>
<p>Rechtsgrundlage ist Art. 6 Abs. 1 lit. b DSGVO, wenn die Kommunikation der Anbahnung oder Durchführung eines Vertrags dient. In anderen Fällen erfolgt die Verarbeitung auf Grundlage von Art. 6 Abs. 1 lit. f DSGVO und unserem berechtigten Interesse an einer effizienten geschäftlichen Kommunikation. Für die Löschung gelten die im Abschnitt zum Kontaktformular genannten Grundsätze; gesetzliche Aufbewahrungspflichten bleiben unberührt.</p>

<h3>6. Empfänger und Übermittlungen in Drittländer</h3>
<p>Personenbezogene Daten erhalten nur Stellen, die sie zur Erfüllung der beschriebenen Zwecke benötigen. Dazu gehören insbesondere Hostinger als Hosting- und E-Mail-Anbieter sowie HubSpot und dessen veröffentlichte Unterauftragsverarbeiter für Kontaktformular, CRM und Kommunikation. Weitere Empfänger kommen nur hinzu, wenn dies gesetzlich vorgeschrieben, zur Vertragsdurchführung erforderlich oder durch eine andere Rechtsgrundlage erlaubt ist.</p>
<p>Bei Übermittlungen in Staaten außerhalb der EU beziehungsweise des EWR achten wir auf die Voraussetzungen der Art. 44 ff. DSGVO, insbesondere einen Angemessenheitsbeschluss oder geeignete Garantien wie die Standardvertragsklauseln.</p>

<h3>7. Allgemeine Speicherdauer</h3>
<p>Soweit in dieser Erklärung keine besondere Frist genannt ist, speichern wir personenbezogene Daten nur so lange, wie sie für den jeweiligen Zweck erforderlich sind. Anschließend werden sie gelöscht, sofern keine gesetzlichen Aufbewahrungspflichten, berechtigten Beweissicherungsinteressen oder sonstigen gesetzlichen Erlaubnisse entgegenstehen.</p>

<h3>8. Ihre Rechte</h3>
<p>Im Rahmen der gesetzlichen Voraussetzungen haben Sie insbesondere das Recht auf Auskunft nach Art. 15 DSGVO, Berichtigung nach Art. 16 DSGVO, Löschung nach Art. 17 DSGVO, Einschränkung der Verarbeitung nach Art. 18 DSGVO sowie Datenübertragbarkeit nach Art. 20 DSGVO. Soweit eine Verarbeitung auf einer Einwilligung beruht, können Sie diese jederzeit mit Wirkung für die Zukunft widerrufen.</p>
<p><strong>Widerspruchsrecht:</strong> Soweit die Verarbeitung auf Art. 6 Abs. 1 lit. f DSGVO beruht, können Sie aus Gründen, die sich aus Ihrer besonderen Situation ergeben, jederzeit Widerspruch einlegen. Einer Verarbeitung zum Zweck der Direktwerbung können Sie jederzeit ohne Angabe von Gründen widersprechen.</p>
<p>Zur Ausübung Ihrer Rechte genügt eine Nachricht an <a href="mailto:kontakt@cleonhardt.de">kontakt@cleonhardt.de</a>. Außerdem haben Sie das Recht, sich bei einer Datenschutzaufsichtsbehörde zu beschweren. Für den Sitz des Verantwortlichen ist insbesondere zuständig:</p>
<p>Landesbeauftragte für Datenschutz und Informationsfreiheit Nordrhein-Westfalen<br>Kavalleriestraße 2–4<br>40213 Düsseldorf<br>Telefon: 0211 38424-0<br>E-Mail: <a href="mailto:poststelle@ldi.nrw.de">poststelle@ldi.nrw.de</a><br>Website: <a href="https://www.ldi.nrw.de/">www.ldi.nrw.de</a></p>

<h3>9. Verschlüsselung und Sicherheit</h3>
<p>Die Website wird ausschließlich verschlüsselt über TLS bereitgestellt. Wir treffen angemessene technische und organisatorische Maßnahmen, um personenbezogene Daten gegen Verlust, Manipulation und unberechtigten Zugriff zu schützen. Eine Datenübertragung im Internet kann dennoch nicht vollständig risikofrei sein.</p>

<h3>10. Aktualisierung dieser Datenschutzerklärung</h3>
<p>Wir aktualisieren diese Datenschutzerklärung, wenn sich die Website, eingesetzte Dienste oder rechtliche Anforderungen ändern. Es gilt die jeweils auf dieser Seite veröffentlichte Fassung.</p>''')

// Navigation: sechs Hauptpunkte, Insights mit drei redaktionellen Unterseiten.
Node navFolder = ensureNode(navigation.rootNode, 'cleonhardt', 'mgnl:folder')
Node navRoot = ensureNode(navFolder, 'Main', 'mgnl:content')
navRoot.setProperty('title', 'Hauptnavigation')
removeChild(navRoot, 'items')
Node navItems = navRoot.addNode('items', 'mgnl:contentNode')

Node addNavItem(Node parent, String index, String label, Node target) {
    Node item = parent.addNode(index, 'mgnl:contentNode')
    item.setProperty('label', label)
    item.setProperty('targetPage', target.identifier)
    item
}

addNavItem(navItems, '00', 'Start', pages.home)
addNavItem(navItems, '01', 'Leistungen', pages.leistungen)
addNavItem(navItems, '02', 'ChOS', pages.chos)
addNavItem(navItems, '03', 'Über mich', pages.ueber)
Node insightsNav = addNavItem(navItems, '04', 'Insights', pages.insights)
Node insightChildren = insightsNav.addNode('children', 'mgnl:contentNode')
addNavItem(insightChildren, '00', 'Methoden sind selten der Engpass', article1)
addNavItem(insightChildren, '01', 'Diagnose vor Eingriff', article2)
addNavItem(insightChildren, '02', 'Unklare Rollen', article3)
addNavItem(navItems, '05', 'Kontakt', pages.kontakt)

// Footer: nur Seiten, die bewusst nicht in der Hauptnavigation erscheinen.
Node footerFolder = ensureNode(footer.rootNode, 'cleonhardt', 'mgnl:folder')
Node footerRoot = ensureNode(footerFolder, 'Informationen', 'mgnl:content')
footerRoot.setProperty('footerText', 'Klarheit für komplexe Produktorganisationen.')
removeChild(footerRoot, 'links')
Node footerLinks = footerRoot.addNode('links', 'mgnl:contentNode')
Node impressumLink = footerLinks.addNode('00', 'mgnl:contentNode')
impressumLink.setProperty('label', 'Impressum')
impressumLink.setProperty('targetPage', pages.impressum.identifier)
Node privacyLink = footerLinks.addNode('01', 'mgnl:contentNode')
privacyLink.setProperty('label', 'Datenschutz')
privacyLink.setProperty('targetPage', pages.datenschutz.identifier)

// Vorhandenes Logo behalten, aber immer auf die Startseite verlinken.
Node settingsRoot
if (siteSettings.nodeExists('/cleonhardt/Logo-Cleonhardt')) {
    settingsRoot = siteSettings.getNode('/cleonhardt/Logo-Cleonhardt')
} else {
    Node settingsFolder = ensureNode(siteSettings.rootNode, 'cleonhardt', 'mgnl:folder')
    settingsRoot = ensureNode(settingsFolder, 'Logo-Cleonhardt', 'mgnl:content')
}
settingsRoot.setProperty('logoAltText', 'Christian Leonhardt')
settingsRoot.setProperty('logoTargetPage', pages.home.identifier)

website.save()
navigation.save()
footer.save()
siteSettings.save()

println "MVP aufgebaut: ${pages.size()} Hauptseiten + 3 Insights-Artikel."
println 'Als Nächstes in Magnolia veröffentlichen: Website-Seiten, Hauptnavigation, Footer und Website-Einstellungen.'
