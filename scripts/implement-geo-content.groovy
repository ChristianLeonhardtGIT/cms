import info.magnolia.context.MgnlContext
import groovy.transform.Field
import javax.jcr.Node
import javax.jcr.Session

/*
 * GEO-/AEO-Inhalte für cleonhardt.de.
 * Wiederholbar: ersetzt nur die vier Angebotsseiten, die sechs neuen Insights,
 * die Insights-Übersicht und die verlinkten Abschnitte der Leistungsseite.
 */

@Field final String PAGE_TEMPLATE = 'meine-website:pages/home'
@Field final String COMPONENT_PREFIX = 'meine-website:components/'
@Field final String TODAY = '2026-07-31'

Session website = MgnlContext.getJCRSession('website')
website.refresh(false)

void setProperties(Node node, Map<String, Object> properties) {
    properties.each { String key, Object value ->
        if (value != null) node.setProperty(key, value)
    }
}

void resetMain(Node page) {
    if (page.hasNode('main')) page.getNode('main').remove()
}

Node ensurePage(Node parent, String name, Map<String, Object> properties) {
    Node page = parent.hasNode(name) ? parent.getNode(name) : parent.addNode(name, 'mgnl:page')
    resetMain(page)
    setProperties(page, [
        'mgnl:template': PAGE_TEMPLATE,
        brandName: 'Christian Leonhardt',
        footerText: 'Product Leadership · Organisation · Transformation'
    ] + properties)
    page
}

Node addArea(Node page) {
    page.addNode('main', 'mgnl:area')
}

Node addComponent(Node area, String template, Map<String, Object> properties = [:]) {
    String name = String.format('%02d', area.nodes.size)
    Node component = area.addNode(name, 'mgnl:component')
    component.setProperty('mgnl:template', template.contains(':') ? template : COMPONENT_PREFIX + template)
    setProperties(component, properties)
    component
}

Node addHero(Node area, String eyebrow, String title, String description) {
    addComponent(area, 'hero', [eyebrow: eyebrow, title: title, description: description])
}

Node addText(Node area, String heading, String html, boolean tinted = false) {
    addComponent(area, 'text', [heading: heading, text: html, showBackground: tinted])
}

Node addCard(Node area, String title, String html, String target) {
    Node card = addComponent(area, 'cards', [title: title, description: html])
    Node chooser = card.addNode('linkChooser', 'mgnl:contentNode')
    chooser.setProperty('field', 'internalLink')
    chooser.setProperty('internalLink', target)
    chooser.setProperty('linkText', 'Artikel lesen')
    card
}

Node addCta(Node area, String title, String html, String target = '/kontakt', String button = 'Situation klären') {
    Node cta = addComponent(area, 'callToAction', [title: title, description: html, buttonText: button])
    Node chooser = cta.addNode('pageLinkChooser', 'mgnl:contentNode')
    chooser.setProperty('field', 'internalPageLink')
    chooser.setProperty('internalLink', target)
    cta
}

Node findByHeading(Node area, String heading) {
    for (Node child : area.nodes) {
        if (child.hasProperty('heading') && child.getProperty('heading').string == heading) return child
    }
    null
}

Map<String, Map<String, String>> offers = [
    'decision-review': [
        title: 'Decision Review',
        nav: 'Decision Review',
        browser: 'Decision Review – wichtige Entscheidungen belastbar prüfen',
        description: 'Strukturierte Gegenprüfung einer wichtigen Entscheidung oder eines vorliegenden Konzepts – mit Annahmenprüfung, Red-Team-Betrachtung und schriftlicher Empfehlung.',
        eyebrow: 'Decision Review · 490–750 € netto',
        hero: 'Eine wichtige Entscheidung verdient mehr als Zustimmung.',
        lead: 'Ein Decision Review ist eine unabhängige, strukturierte Gegenprüfung einer wichtigen Entscheidung oder eines vorliegenden Konzepts. Ziel ist nicht, die Entscheidung abzunehmen, sondern Annahmen, Risiken, Alternativen und blinde Flecken vor der Umsetzung sichtbar zu machen.',
        audience: 'Führungskräfte, Produktverantwortliche und Entscheidungsteams',
        low: '490', high: '750', offerKey: 'decision-review',
        situations: '<div class="mvp-grid mvp-grid--3"><article class="mvp-panel"><h3>Strategische Weichenstellung</h3><p>Eine Produkt-, Technologie- oder Organisationsentscheidung bindet Ressourcen und lässt sich später nur schwer korrigieren.</p></article><article class="mvp-panel"><h3>Vorliegendes Konzept</h3><p>Ein Konzept ist weit entwickelt, wurde aber bisher vor allem von Beteiligten mit ähnlicher Perspektive geprüft.</p></article><article class="mvp-panel"><h3>Festgefahrene Diskussion</h3><p>Mehrere plausible Optionen stehen nebeneinander und die Debatte wiederholt sich ohne neuen Erkenntnisgewinn.</p></article></div>',
        included: '<ul class="mvp-checks"><li>Sichtung der relevanten Unterlagen</li><li>Trennung von Fakten, Annahmen und Schlussfolgerungen</li><li>Gegenprüfung der wichtigsten Annahmen</li><li>Red-Team-Betrachtung und plausible Gegenpositionen</li><li>Persönliches Review-Gespräch</li><li>Schriftliche Empfehlung mit offenen Risiken und nächsten Schritten</li></ul>',
        process: '<div class="mvp-grid mvp-grid--3"><div class="mvp-panel"><span class="mvp-number">1</span><h3>Material</h3><p>Sie senden das vorhandene Konzept und benennen die konkrete Entscheidung.</p></div><div class="mvp-panel"><span class="mvp-number">2</span><h3>Prüfung</h3><p>Ich rekonstruiere Logik, Annahmen, Risiken und nicht betrachtete Alternativen.</p></div><div class="mvp-panel mvp-panel--accent"><span class="mvp-number">3</span><h3>Empfehlung</h3><p>Im Review besprechen wir Befund, Konsequenzen und einen belastbaren nächsten Schritt.</p></div></div>',
        boundary: 'Das Decision Review ersetzt keine Rechts-, Steuer-, Sicherheits- oder technische Spezialprüfung. Bei umfangreichen Unterlagen, mehreren Interviews oder einer organisationsweiten Fragestellung klären wir ein Diagnostic.'
    ],
    'executive-sparring': [
        title: 'Executive Sparring',
        nav: 'Executive Sparring',
        browser: 'Executive Sparring für Product Leadership und Transformation',
        description: 'Vertrauliches monatliches Sparring für Führungskräfte in Produktorganisation, Transformation und anspruchsvollen Entscheidungssituationen.',
        eyebrow: 'Executive Sparring · monatlich kündbar',
        hero: 'Ein vertraulicher Denkraum für Entscheidungen mit Wirkung.',
        lead: 'Executive Sparring ist eine persönliche, vertrauliche Begleitung für Führungskräfte, die regelmäßig komplexe Produkt-, Führungs- oder Organisationsentscheidungen treffen. Im Mittelpunkt stehen konkrete Situationen, nicht ein vorgegebenes Coachingprogramm.',
        audience: 'Führungskräfte und leitende Produktverantwortliche',
        low: '1250', high: '1750', offerKey: 'executive-sparring',
        situations: '<div class="mvp-grid mvp-grid--3"><article class="mvp-panel"><h3>Hohe Entscheidungsdichte</h3><p>Mehrere wichtige Themen laufen parallel und benötigen einen unabhängigen Blick auf Prioritäten und Konsequenzen.</p></article><article class="mvp-panel"><h3>Neue Verantwortung</h3><p>Rolle, Mandat und Erwartungen verändern sich schneller als die formalen Strukturen.</p></article><article class="mvp-panel"><h3>Transformation</h3><p>Sie müssen Wirkung erzeugen, obwohl Interessen, Abhängigkeiten und Unsicherheit gleichzeitig zunehmen.</p></article></div>',
        included: '<ul class="mvp-checks"><li>Vier persönliche Gespräche pro Monat</li><li>Asynchrone Rückfragen im vereinbarten Kanal</li><li>Entscheidungs- und Situationsanalysen</li><li>Monatliche Reflexion wiederkehrender Muster</li><li>Priorisierte Handlungsempfehlungen</li><li>Zu Beginn monatlich kündbar</li></ul>',
        process: '<div class="mvp-grid mvp-grid--3"><div class="mvp-panel"><span class="mvp-number">1</span><h3>Fokus</h3><p>Wir klären Rolle, aktuelle Spannungen und den Arbeitsmodus für vertrauliche Themen.</p></div><div class="mvp-panel"><span class="mvp-number">2</span><h3>Sparring</h3><p>Konkrete Entscheidungen werden vorbereitet, gegengeprüft und im Kontext betrachtet.</p></div><div class="mvp-panel mvp-panel--accent"><span class="mvp-number">3</span><h3>Lernschleife</h3><p>Wir verdichten wiederkehrende Muster zu bewussten Führungs- und Handlungsprinzipien.</p></div></div>',
        boundary: 'Sparring ist keine Psychotherapie, Rechtsberatung oder operative Linienfunktion. Entscheidungen und Verantwortung bleiben bei Ihnen; ich schaffe Klarheit, stelle Gegenfragen und mache Alternativen sichtbar.'
    ],
    'product-organisation-diagnostic': [
        title: 'Product Organisation Diagnostic',
        nav: 'Product Organisation Diagnostic',
        browser: 'Product Organisation Diagnostic – Produktorganisation systemisch analysieren',
        description: 'Systemische Analyse einer Produktorganisation: Rollen, Entscheidungswege, Priorisierung, Abhängigkeiten, Führung und Zusammenarbeit.',
        eyebrow: 'Unternehmensangebot · 7.500–15.000 € netto',
        hero: 'Die Organisation verstehen, bevor Sie sie neu zeichnen.',
        lead: 'Ein Product Organisation Diagnostic untersucht nicht nur Rollen und Prozesse, sondern die Mechanismen, die tägliche Entscheidungen tatsächlich prägen. Das Ergebnis ist ein belastbares Lagebild mit priorisierten Handlungsfeldern – kein Organigramm aus der Schublade.',
        audience: 'Geschäftsführungen, Produktleitungen und Transformationsverantwortliche',
        low: '7500', high: '15000', offerKey: 'product-organisation-diagnostic',
        situations: '<div class="mvp-grid mvp-grid--3"><article class="mvp-panel"><h3>Wiederkehrende Reibung</h3><p>Priorisierung, Abhängigkeiten oder Eskalationen bleiben trotz neuer Prozesse bestehen.</p></article><article class="mvp-panel"><h3>Geplante Neuordnung</h3><p>Vor einer Reorganisation soll klar werden, welche Mechanismen wirklich verändert werden müssen.</p></article><article class="mvp-panel"><h3>Unklare Wirksamkeit</h3><p>Teams arbeiten engagiert, aber Strategie, Entscheidungen und Ergebnisse greifen nicht zuverlässig ineinander.</p></article></div>',
        included: '<ul class="mvp-checks"><li>Interviews mit Führungskräften und ausgewählten Teams</li><li>Analyse von Rollen, Mandaten und Verantwortlichkeiten</li><li>Entscheidungswege und Priorisierungsmechanismen</li><li>Zusammenarbeit, Abhängigkeiten und Schnittstellen</li><li>Führungs- und Kommunikationsstrukturen</li><li>ChOS-Auswertung, Zielbild und priorisierter Maßnahmenplan</li></ul>',
        process: '<div class="mvp-grid mvp-grid--3"><div class="mvp-panel"><span class="mvp-number">1</span><h3>Auftrag klären</h3><p>Fragestellung, Systemgrenze, Beteiligte und vorhandene Daten werden verbindlich festgelegt.</p></div><div class="mvp-panel"><span class="mvp-number">2</span><h3>Diagnose</h3><p>Dokumente, Interviews und beobachtbare Muster werden zu überprüfbaren Hypothesen verdichtet.</p></div><div class="mvp-panel mvp-panel--accent"><span class="mvp-number">3</span><h3>Handlungsbild</h3><p>Sie erhalten Befund, Risiken, Zielbild und eine priorisierte Folge von Interventionen.</p></div></div>',
        boundary: 'Der konkrete Umfang hängt von Größe, Fragestellung und Zahl der Beteiligten ab. Vor dem Start erhalten Sie ein verbindliches Angebot mit Untersuchungsrahmen, Ergebnissen und Zeitplan.'
    ],
    'workshops': [
        title: 'Workshops',
        nav: 'Workshops',
        browser: 'Workshops für Product Strategy, Rollen und Operating Models',
        description: 'Fokussierte Workshops für Rollenklärung, Operating Models, Product Strategy, Priorisierung, Leadership und Zusammenarbeit.',
        eyebrow: 'Unternehmensangebot · 1.600–2.500 € netto pro Tag',
        hero: 'Ein Workshop ist dann wertvoll, wenn danach etwas entschieden ist.',
        lead: 'Die Workshops verbinden Vorbereitung, gemeinsame Arbeit und ein dokumentiertes Ergebnis. Sie eignen sich, wenn die relevante Fragestellung ausreichend verstanden ist und die richtigen Personen gemeinsam eine Entscheidung, ein Modell oder einen konkreten nächsten Schritt erarbeiten sollen.',
        audience: 'Führungsteams, Produktorganisationen und bereichsübergreifende Entscheidungsteams',
        low: '1600', high: '2500', offerKey: 'workshop',
        situations: '<div class="mvp-grid mvp-grid--3"><article class="mvp-panel"><h3>Rollen und Verantwortung</h3><p>Mandate, Schnittstellen und Entscheidungsrechte müssen gemeinsam geklärt werden.</p></article><article class="mvp-panel"><h3>Strategie und Priorisierung</h3><p>Aus unterschiedlichen Sichtweisen soll eine verbindliche Richtung mit Kriterien entstehen.</p></article><article class="mvp-panel"><h3>Operating Model</h3><p>Zusammenarbeit, Governance und Routinen sollen zu Strategie und Wertströmen passen.</p></article></div>',
        included: '<ul class="mvp-checks"><li>Vorgespräch und klare Ergebnisdefinition</li><li>Auswahl und Vorbereitung der Teilnehmenden</li><li>Passendes Arbeitsdesign statt Standardagenda</li><li>Moderation mit klaren Entscheidungs- und Arbeitsphasen</li><li>Dokumentation von Ergebnissen, Entscheidungen und offenen Punkten</li><li>Empfehlung für die nächste Umsetzungsschleife</li></ul>',
        process: '<div class="mvp-grid mvp-grid--3"><div class="mvp-panel"><span class="mvp-number">1</span><h3>Vorbereitung</h3><p>Wir klären Ziel, Entscheidungsspielraum, Teilnehmende und notwendige Vorarbeiten.</p></div><div class="mvp-panel"><span class="mvp-number">2</span><h3>Arbeitstag</h3><p>Die Gruppe arbeitet sichtbar an Optionen, Zielkonflikten und verbindlichen Entscheidungen.</p></div><div class="mvp-panel mvp-panel--accent"><span class="mvp-number">3</span><h3>Transfer</h3><p>Ergebnisse, Verantwortliche und nächste Schritte werden schriftlich festgehalten.</p></div></div>',
        boundary: 'Ein Workshop löst keine unklare Auftragslage und ersetzt keine Diagnose. Wenn Ursachen, Beteiligte oder Systemgrenzen noch offen sind, beginnen wir mit einer Clarity Session oder einem Diagnostic.'
    ]
]

offers.each { String slug, Map<String, String> offer ->
    Node page = ensurePage(website.rootNode, slug, [
        title: offer.title,
        navigationTitle: offer.nav,
        windowTitle: offer.browser,
        metaDescription: offer.description,
        hideInNavigation: true,
        serviceType: offer.title,
        serviceAudience: offer.audience,
        priceLow: offer.low,
        priceHigh: offer.high,
        dateModified: TODAY
    ])
    Node area = addArea(page)
    addHero(area, offer.eyebrow, offer.hero, offer.description)
    addText(area, 'Kurz erklärt', "<p class=\"lead\">${offer.lead}</p><p class=\"clarity-actions\"><a class=\"button\" href=\"/angebot-anfragen?leistung=${offer.offerKey}\">Unverbindliches Angebot anfragen</a><a href=\"/leistungen\">Alle Leistungen vergleichen</a></p>", true)
    addText(area, 'Wann dieses Format sinnvoll ist', offer.situations)
    addText(area, 'Was Sie bekommen', offer.included, true)
    addText(area, 'So läuft die Zusammenarbeit ab', offer.process)
    addText(area, 'Klare Grenzen', "<p>${offer.boundary}</p>", true)
    addCta(area, 'Passt das zu Ihrer Situation?', '<p>Beschreiben Sie kurz den Kontext. Ich prüfe persönlich, ob dieses Format passt oder ein kleinerer Einstieg sinnvoller ist.</p>', '/angebot-anfragen', 'Angebot anfragen')
}

Map<String, Map<String, String>> articles = [
    'rollen-und-verantwortlichkeiten-in-produktorganisationen-klaeren': [
        title: 'Rollen und Verantwortlichkeiten in Produktorganisationen klären', nav: 'Rollen und Verantwortung klären',
        description: 'Wie Produktorganisationen Rollen, Mandate, Schnittstellen und Entscheidungsrechte wirksam klären – ohne nur ein neues Rollendokument zu erzeugen.',
        category: 'Product Operating Model',
        answer: 'Rollen werden nicht durch genauere Stellenbeschreibungen klar, sondern durch konkrete Entscheidungsrechte, erwartete Ergebnisse und sichtbare Schnittstellen. Eine wirksame Rollenklärung beginnt deshalb bei wiederkehrenden Entscheidungen: Wer entscheidet, wer liefert Informationen, wer wird beteiligt und woran wird das Ergebnis gemessen?',
        body: '<h3>Mit Entscheidungen statt Titeln beginnen</h3><p>Sammeln Sie zunächst fünf bis zehn Situationen, in denen Verantwortung heute unklar wird: Prioritäten ändern, technische Risiken akzeptieren, Budgets verschieben oder ein Ergebnis stoppen. Erst an diesen Situationen wird sichtbar, ob ein Rollenproblem, ein Mandatsproblem oder ein Zielkonflikt vorliegt.</p><h3>Vier Ebenen der Klärung</h3><ol><li><strong>Ergebnis:</strong> Wofür steht die Rolle erkennbar ein?</li><li><strong>Entscheidung:</strong> Welche Entscheidungen darf sie selbst treffen?</li><li><strong>Information:</strong> Welche Daten und Perspektiven braucht sie?</li><li><strong>Schnittstelle:</strong> Wo beginnt die Verantwortung einer anderen Rolle?</li></ol><blockquote class="mvp-quote">Ein Rollenbild ist erst belastbar, wenn es in einer strittigen Entscheidung Orientierung gibt.</blockquote>',
        questions: '<ul class="mvp-checks"><li>Welche Entscheidungen werden regelmäßig erneut geöffnet?</li><li>Wo trägt jemand Verantwortung ohne ausreichendes Mandat?</li><li>Welche Ziele machen Kooperation unvernünftig?</li><li>Welche Informationen erreichen den Entscheidungsort zu spät?</li><li>Woran erkennen Beteiligte, dass die neue Klärung funktioniert?</li></ul>'
    ],
    'wann-braucht-eine-produktorganisation-ein-operating-model': [
        title: 'Wann braucht eine Produktorganisation ein Operating Model?', nav: 'Wann braucht es ein Operating Model?',
        description: 'Woran man erkennt, dass eine Produktorganisation ein bewusstes Operating Model benötigt – und welche Fragen vor einem Re-Design geklärt werden sollten.',
        category: 'Organisation',
        answer: 'Eine Produktorganisation braucht ein bewusstes Operating Model, wenn Strategie, Wertströme, Entscheidungsrechte und Zusammenarbeit nicht mehr zuverlässig zusammenpassen. Typische Signale sind dauerhafte Eskalationen, konkurrierende Prioritäten, unklare Schnittstellen und lokale Optimierung trotz gemeinsamer Ziele.',
        body: '<h3>Ein Operating Model ist mehr als ein Organigramm</h3><p>Es beschreibt, wie eine Organisation Wert erzeugt und dabei Entscheidungen, Verantwortung, Informationen, Finanzierung und Zusammenarbeit verbindet. Teamzuschnitte sind nur ein sichtbarer Teil davon.</p><h3>Fünf Gestaltungsfragen</h3><ol><li>Welche Kundenergebnisse und Wertströme bilden die Struktur?</li><li>Wo liegen Produkt-, Technologie- und Ergebnisverantwortung?</li><li>Wie werden Prioritäten und Investitionen entschieden?</li><li>Welche Abhängigkeiten sind notwendig und welche nur historisch?</li><li>Welche Führungsroutinen stabilisieren das gewünschte Verhalten?</li></ol><p>Erst wenn diese Fragen beantwortet sind, lohnt sich die Diskussion über konkrete Teams, Rollen und Gremien.</p>',
        questions: '<ul class="mvp-checks"><li>Strategie wird in jedem Bereich anders übersetzt.</li><li>Teams besitzen Backlogs, aber keine echte Ergebnisverantwortung.</li><li>Entscheidungen wandern regelmäßig nach oben.</li><li>Abhängigkeiten dominieren Planung und Lieferfähigkeit.</li><li>Eine Reorganisation steht an, aber das Zielbild bleibt abstrakt.</li></ul>'
    ],
    'annahmen-vor-einer-produktentscheidung-pruefen': [
        title: 'Annahmen vor einer Produktentscheidung prüfen', nav: 'Annahmen vor Entscheidungen prüfen',
        description: 'Ein praktischer Red-Team-Ansatz, um Fakten, Annahmen, Risiken und Alternativen vor wichtigen Produktentscheidungen zu trennen.',
        category: 'Decision Review',
        answer: 'Vor einer wichtigen Produktentscheidung sollten Fakten, Annahmen und Schlussfolgerungen getrennt werden. Entscheidend ist nicht, jede Unsicherheit zu beseitigen, sondern die Annahmen zu identifizieren, deren Irrtum die Entscheidung wesentlich verändern würde, und dafür einen passenden Test oder eine bewusste Risikoentscheidung zu formulieren.',
        body: '<h3>Die Entscheidungslogik sichtbar machen</h3><p>Schreiben Sie die Entscheidung als Satz: „Wir wählen Option A, weil …“. Markieren Sie anschließend jede Begründung als Beobachtung, Annahme oder Bewertung. Häufig zeigt sich, dass eine scheinbar faktenbasierte Entscheidung auf wenigen, kaum geprüften Annahmen ruht.</p><h3>Red-Team-Fragen</h3><ol><li>Welche Information würde unsere Präferenz verändern?</li><li>Welche plausible Alternative erklären wir zu schnell für ungeeignet?</li><li>Was müsste stimmen, damit die Gegenposition richtig ist?</li><li>Welche Folge ist schwer umkehrbar?</li><li>Welche kleine Prüfung reduziert die wichtigste Unsicherheit?</li></ol>',
        questions: '<ul class="mvp-checks"><li>Die Entscheidung bindet viel Zeit, Budget oder Reputation.</li><li>Im Team herrscht auffällig schnelle Einigkeit.</li><li>Ein Konzept wurde überwiegend von seinen Urhebern geprüft.</li><li>Diskussionen vermischen Ziel, Lösung und Umsetzung.</li><li>Ein Scheitern würde erst spät sichtbar werden.</li></ul>'
    ],
    'organisationsdiagnose-statt-standardberatung': [
        title: 'Organisationsdiagnose statt Standardberatung', nav: 'Organisationsdiagnose statt Standardlösung',
        description: 'Warum eine Organisationsdiagnose vor Maßnahmen klärt, welche Mechanismen ein Problem erzeugen – und wann Standardlösungen zu kurz greifen.',
        category: 'Organisationsdiagnose',
        answer: 'Organisationsdiagnose untersucht zuerst, welche Strukturen, Ziele, Informationen und Entscheidungen ein beobachtetes Muster erzeugen. Standardberatung beginnt dagegen häufig mit einer bekannten Lösung. Diagnose ist besonders wichtig, wenn ähnliche Maßnahmen bereits versucht wurden oder mehrere plausible Ursachen gleichzeitig bestehen.',
        body: '<h3>Symptom, Muster und Mechanismus</h3><p>„Priorisierung funktioniert nicht“ ist ein Symptom. Wiederholt geöffnete Entscheidungen sind ein beobachtbares Muster. Dahinter können konkurrierende Ziele, fehlende Daten, unklare Rechte oder ein Finanzierungsmodell stehen. Diese Ursachen benötigen unterschiedliche Eingriffe.</p><h3>Was eine Diagnose leisten muss</h3><ol><li>Beobachtungen und Bewertungen trennen.</li><li>Wiederkehrende Muster über mehrere Situationen prüfen.</li><li>Mindestens zwei plausible Erklärungen zulassen.</li><li>Widersprechende Evidenz aktiv suchen.</li><li>Den kleinsten aussagekräftigen Eingriff ableiten.</li></ol><blockquote class="mvp-quote">Die Qualität einer Maßnahme hängt weniger von ihrer Bekanntheit als von der Qualität der Diagnose ab.</blockquote>',
        questions: '<ul class="mvp-checks"><li>Das Problem kehrt trotz Prozess- oder Rollenänderung zurück.</li><li>Bereiche erklären dieselbe Situation grundlegend unterschiedlich.</li><li>Eine Reorganisation wird diskutiert, bevor der Mechanismus klar ist.</li><li>Die gewünschte Lösung steht bereits fest, die Fragestellung aber nicht.</li><li>Lokale Verbesserungen verschlechtern das Gesamtsystem.</li></ul>'
    ],
    'product-organisation-diagnostic-ablauf-und-ergebnis': [
        title: 'Product Organisation Diagnostic: Ablauf und Ergebnis', nav: 'Product Organisation Diagnostic erklärt',
        description: 'Wie ein Product Organisation Diagnostic abläuft, welche Daten betrachtet werden und welches Ergebnis Führung und Teams erhalten.',
        category: 'Product Organisation Diagnostic',
        answer: 'Ein Product Organisation Diagnostic verbindet Auftragsklärung, Dokumentenanalyse, Interviews und die Untersuchung wiederkehrender Entscheidungs- und Zusammenarbeitsmuster. Das Ergebnis ist ein priorisiertes Lagebild mit Hypothesen, Risiken, Zielbild und konkreten nächsten Interventionen.',
        body: '<h3>1. Systemgrenze und Frage klären</h3><p>Eine Diagnose beginnt mit einer präzisen Frage. „Unsere Produktorganisation verbessern“ ist zu breit. Belastbarer ist: „Warum gelingt es drei Produktbereichen nicht, gemeinsame Prioritäten über Abhängigkeiten hinweg zu halten?“</p><h3>2. Mehrere Datenquellen verbinden</h3><p>Dokumente zeigen die formale Organisation. Interviews zeigen Wahrnehmungen und Entscheidungen. Konkrete Fälle zeigen, was im Alltag tatsächlich geschieht. Erst die Verbindung reduziert vorschnelle Schlussfolgerungen.</p><h3>3. Hypothesen und Gegenbelege</h3><p>Jeder Befund wird als prüfbare Erklärung formuliert. Gute Diagnose sucht nicht nur Bestätigung, sondern Situationen, in denen das vermutete Muster gerade nicht auftritt.</p><h3>4. Priorisiertes Handlungsbild</h3><p>Das Ergebnis unterscheidet sofortige Klärungen, begrenzte Tests und strukturelle Veränderungen. Damit wird aus einer langen Problemliste eine sinnvolle Reihenfolge.</p>',
        questions: '<ul class="mvp-checks"><li>Beantwortete Untersuchungsfrage und Systemgrenze</li><li>Verdichtete Beobachtungen und wiederkehrende Muster</li><li>Überprüfbare Hypothesen und relevante Risiken</li><li>Zielbild für Entscheidungen und Zusammenarbeit</li><li>Priorisierter Maßnahmenplan mit Verantwortlichkeiten</li></ul>'
    ],
    'wann-ist-executive-sparring-sinnvoll': [
        title: 'Wann ist Executive Sparring sinnvoll?', nav: 'Wann ist Executive Sparring sinnvoll?',
        description: 'Für welche Führungssituationen Executive Sparring geeignet ist, wie es sich von Coaching und Beratung unterscheidet und woran gute Zusammenarbeit erkennbar wird.',
        category: 'Leadership',
        answer: 'Executive Sparring ist sinnvoll, wenn Führungskräfte regelmäßig komplexe Entscheidungen unter Unsicherheit treffen und dafür einen unabhängigen, vertraulichen Gegenpart benötigen. Es verbindet Reflexion mit konkreter Situations- und Entscheidungsanalyse, ohne Verantwortung zu übernehmen oder ein starres Programm vorzugeben.',
        body: '<h3>Nicht jede Herausforderung braucht ein Programm</h3><p>In verantwortungsvollen Rollen wechseln strategische, organisatorische und zwischenmenschliche Fragen schnell. Sparring schafft einen konstanten Denkraum, in dem diese Situationen vorbereitet, gegengeprüft und später ausgewertet werden können.</p><h3>Abgrenzung</h3><p><strong>Beratung</strong> liefert häufig Expertise oder ein konkretes Lösungskonzept. <strong>Coaching</strong> arbeitet primär über Fragen an individuellen Zielen und Entwicklung. <strong>Sparring</strong> darf beides berühren, fokussiert aber auf die gemeinsame Gegenprüfung realer Entscheidungen und Handlungsoptionen.</p><h3>Woran gute Zusammenarbeit erkennbar wird</h3><p>Die Gespräche erzeugen keine Abhängigkeit. Sie verbessern die Qualität eigener Entscheidungen, machen Muster schneller sichtbar und führen zu klareren nächsten Schritten.</p>',
        questions: '<ul class="mvp-checks"><li>Sie tragen Verantwortung, können aber nicht jedes Thema intern offen prüfen.</li><li>Entscheidungen betreffen gleichzeitig Strategie, Organisation und Menschen.</li><li>Sie wollen Widerspruch statt vorschneller Bestätigung.</li><li>Ähnliche Situationen wiederholen sich in neuer Form.</li><li>Sie benötigen Kontinuität zwischen einzelnen Entscheidungen.</li></ul>'
    ]
]

Node insightsPage = website.getNode('/insights')
for (Node child : insightsPage.nodes) {
    if (child.isNodeType('mgnl:page')) {
        if (!child.hasProperty('datePublished')) child.setProperty('datePublished', '2026-07-29')
        child.setProperty('dateModified', TODAY)
    }
}

articles.each { String slug, Map<String, String> article ->
    Node page = ensurePage(insightsPage, slug, [
        title: article.title,
        navigationTitle: article.nav,
        windowTitle: article.title,
        metaDescription: article.description,
        hideInNavigation: true,
        datePublished: TODAY,
        dateModified: TODAY
    ])
    Node area = addArea(page)
    addHero(area, "Insight · ${article.category}", article.title, article.description)
    addText(area, 'Kurzantwort', "<p class=\"lead\">${article.answer}</p>", true)
    addText(area, 'Worauf es praktisch ankommt', article.body)
    addText(area, 'Fragen für Ihre Situation', article.questions, true)
    addCta(area, 'Möchten Sie das auf Ihre Situation übertragen?', '<p>In der Clarity Session trennen wir Beobachtung, Hypothese und nächsten sinnvollen Schritt.</p>', '/clarity-session', 'Clarity Session ansehen')
}

resetMain(insightsPage)
Node insightsArea = addArea(insightsPage)
addHero(insightsArea, 'Insights', 'Produktorganisationen besser verstehen.', 'Klare Antworten, Denkmodelle und praktische Diagnosefragen für Product Leadership, Organisation und Transformation.')
addText(insightsArea, 'Perspektiven für konkrete Entscheidungen', '<p class="lead">Jeder Beitrag beginnt mit einer direkten Antwort und vertieft anschließend Mechanismen, Diagnosefragen und praktische Konsequenzen.</p>')

List<Map<String, String>> allCards = [
    [title: 'Warum Produktorganisationen nicht an fehlenden Methoden scheitern', description: 'Frameworks sind selten der Engpass. Entscheidend ist, ob Richtung, Verantwortung und Entscheidungslogik zusammenpassen.', path: '/insights/warum-produktorganisationen-nicht-an-fehlenden-methoden-scheitern'],
    [title: 'Diagnose vor Eingriff: Ursachen von Symptomen unterscheiden', description: 'Wie aus beobachtbaren Signalen überprüfbare Hypothesen und kleinere, wirksamere Interventionen werden.', path: '/insights/diagnose-vor-eingriff'],
    [title: 'Unklare Rollen sind selten das eigentliche Problem', description: 'Warum Rollenklarheit ohne Entscheidungs- und Anreizklarheit oft nur ein neues Dokument produziert.', path: '/insights/unklare-rollen-sind-selten-das-eigentliche-problem']
]
articles.each { String slug, Map<String, String> article ->
    allCards << [title: article.title, description: article.description, path: "/insights/${slug}"]
}
allCards.each { Map<String, String> card ->
    addCard(insightsArea, card.title, "<p>${card.description}</p>", card.path)
}
addCta(insightsArea, 'Ein Thema kommt Ihnen bekannt vor?', '<p>Bringen Sie eine konkrete Situation in die Clarity Session mit.</p>', '/clarity-session', 'Clarity Session ansehen')

Node servicesArea = website.getNode('/leistungen/main')
Node paid = findByHeading(servicesArea, 'Bezahlter Einstieg')
if (paid != null) {
    paid.setProperty('text', '''
<p class="lead">Ein klar abgegrenzter Auftrag für ein konkretes Problem oder eine wichtige Entscheidung. Sie kaufen nicht nur Gesprächszeit, sondern Vorbereitung, Analyse und ein verwertbares Ergebnis.</p>
<div class="mvp-grid mvp-grid--2">
  <a class="mvp-panel mvp-panel--accent mvp-panel--link" href="/clarity-session" aria-label="ChOS Clarity Session ansehen"><p class="mvp-meta">90 Minuten · 295 € netto</p><h3>ChOS Clarity Session</h3><p>Eine festgefahrene Produkt-, Führungs- oder Organisationssituation strukturiert verstehen.</p><ul class="mvp-checks"><li>Vorbereitung und 90 Minuten Gespräch</li><li>Erste Hypothesen und nächste Schritte</li><li>Schriftlicher Clarity Brief</li></ul><span class="mvp-panel__link">Clarity Session ansehen <span aria-hidden="true">→</span></span></a>
  <article class="mvp-panel"><p class="mvp-meta">490–750 € netto</p><h3>Decision Review</h3><p>Eine wichtige Entscheidung oder ein Konzept vor der Umsetzung belastbar gegenprüfen.</p><ul class="mvp-checks"><li>Annahmenprüfung</li><li>Red-Team-Betrachtung</li><li>Schriftliche Empfehlung</li></ul><p class="mvp-panel__action"><a class="mvp-offer-cta" href="/decision-review">Details ansehen <span aria-hidden="true">→</span></a></p></article>
</div>''')
}

Node sparring = findByHeading(servicesArea, 'Persönliches Sparring')
if (sparring != null) {
    String html = sparring.getProperty('text').string
    html = html.replaceAll('href="[^"]*angebot-anfragen\\?leistung=executive-sparring">Angebot anfragen', 'href="/executive-sparring">Details ansehen')
    sparring.setProperty('text', html)
}

Node companyOffers = findByHeading(servicesArea, 'Angebote für Unternehmen')
if (companyOffers != null) {
    String html = companyOffers.getProperty('text').string
    html = html.replaceAll('href="[^"]*angebot-anfragen\\?leistung=product-organisation-diagnostic">Angebot anfragen', 'href="/product-organisation-diagnostic">Details ansehen')
    html = html.replaceAll('href="[^"]*angebot-anfragen\\?leistung=workshop">Angebot anfragen', 'href="/workshops">Details ansehen')
    companyOffers.setProperty('text', html)
}

website.save()

println "GEO-Inhalte angelegt: ${offers.size()} Angebotsseiten und ${articles.size()} neue Insights."
println 'Insights-Übersicht und Leistungslinks wurden aktualisiert; das kuratierte Dropdown bleibt unverändert.'
println 'Als Nächstes alle geänderten Seiten und die Navigation veröffentlichen.'
