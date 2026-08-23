import info.magnolia.context.MgnlContext
import javax.jcr.Node
import javax.jcr.Session

/*
 * Ergänzt redaktionelle FAQ-Module als letzten Inhaltsblock öffentlicher Seiten.
 * Wiederholbar: Ein vorhandenes FAQ-Modul wird auf jeder Zielseite ersetzt.
 * Kontakt und Rechtstexte bleiben unverändert.
 */

Session website = MgnlContext.getJCRSession('website')
website.refresh(false)

Map<String, List<Map<String, String>>> pageFaqs = [
    '/start': [
        [question: 'Wobei unterstützt Christian Leonhardt?', answer: 'Christian Leonhardt unterstützt Führungskräfte und Produktorganisationen dabei, komplexe Situationen zu diagnostizieren, Entscheidungswege zu klären und tragfähige nächste Schritte abzuleiten. Im Mittelpunkt stehen Product Leadership, Organisationsentwicklung und Transformation.'],
        [question: 'Was ist der Unterschied zwischen Diagnose und Standardberatung?', answer: 'Eine Diagnose beginnt nicht mit einer vorgefertigten Lösung. Sie untersucht zuerst beobachtbare Muster, mögliche Ursachen und den konkreten Kontext. Erst danach wird entschieden, welcher Eingriff sinnvoll und verhältnismäßig ist.'],
        [question: 'Für wen sind die Inhalte auf dieser Website gedacht?', answer: 'Die Inhalte richten sich vor allem an Führungskräfte, Produktverantwortliche und Menschen, die Produktorganisationen gestalten oder verändern. Sie sind auch für Teams hilfreich, die wiederkehrende Reibung besser einordnen möchten.']
    ],
    '/chos': [
        [question: 'Was ist ChOS?', answer: 'ChOS ist ein Diagnose- und Denkmodell für komplexe Produktorganisationen. Es betrachtet nicht nur Rollen und Prozesse, sondern auch Richtung, Entscheidungen, Verantwortung, Zusammenarbeit, Lernen und die Wechselwirkungen zwischen diesen Dimensionen.'],
        [question: 'Ist ChOS ein fertiges Organisationsmodell?', answer: 'Nein. ChOS schreibt keine Standardstruktur und kein Organigramm vor. Es hilft dabei, die aktuelle Organisation systematisch zu lesen, Spannungen sichtbar zu machen und passende Veränderungen aus dem konkreten Kontext abzuleiten.'],
        [question: 'Wann ist eine ChOS-Diagnose sinnvoll?', answer: 'Sie ist besonders sinnvoll, wenn Probleme trotz neuer Prozesse oder Rollen wiederkehren, Entscheidungen regelmäßig eskalieren oder unklar ist, welche Mechanismen hinter sichtbaren Symptomen liegen.']
    ],
    '/chos-selbstcheck': [
        [question: 'Was leistet der ChOS-Selbstcheck?', answer: 'Der Selbstcheck macht erste Spannungsfelder in einer Produktorganisation sichtbar. Er dient der strukturierten Reflexion und liefert Hinweise darauf, welche Dimensionen genauer untersucht werden sollten.'],
        [question: 'Ist das Ergebnis bereits eine Organisationsdiagnose?', answer: 'Nein. Das Ergebnis ist eine erste Orientierung und keine belastbare Diagnose. Dafür müssten konkrete Situationen, Entscheidungen, Daten und unterschiedliche Perspektiven aus der Organisation einbezogen werden.'],
        [question: 'Wie sollte das Ergebnis verwendet werden?', answer: 'Nutzen Sie es als Gesprächsgrundlage: Vergleichen Sie Wahrnehmungen, suchen Sie konkrete Beispiele und prüfen Sie, ob sich die erkannten Muster in mehreren Situationen wiederholen.']
    ],
    '/praxisfaelle': [
        [question: 'Warum sind die Praxisfälle anonymisiert?', answer: 'Organisations- und Führungssituationen enthalten häufig vertrauliche Informationen. Die Fälle sind deshalb so verdichtet und anonymisiert, dass die zugrunde liegenden Muster verständlich bleiben, ohne Beteiligte oder Unternehmen identifizierbar zu machen.'],
        [question: 'Lassen sich die Lösungen direkt übertragen?', answer: 'Nicht unverändert. Praxisfälle zeigen Denkwege und mögliche Interventionen. Ob eine Maßnahme passt, hängt von Strategie, Struktur, Entscheidungsrechten, Menschen und dem konkreten Entstehungsmechanismus des Problems ab.'],
        [question: 'Was ist bei einem Praxisfall besonders relevant?', answer: 'Achten Sie weniger auf die Branche als auf das Muster: Welche Entscheidungen stocken, welche Ziele konkurrieren, wo fehlen Informationen und wodurch wird das beobachtete Verhalten im System plausibel?']
    ],
    '/ueber-mich': [
        [question: 'Welche Themen prägen die Arbeit von Christian Leonhardt?', answer: 'Die Arbeit verbindet Product Leadership, Produktorganisation, Organisationsdiagnose, Operating Models, Transformation und Entscheidungsarchitektur. Der gemeinsame Kern ist die Frage, wie Organisationen unter Komplexität klarer und wirksamer entscheiden.'],
        [question: 'Wie ist die Arbeitsweise?', answer: 'Die Arbeitsweise ist analytisch, systemisch und praxisnah. Beobachtungen werden von Bewertungen getrennt, Annahmen werden explizit gemacht und Empfehlungen werden auf konkrete Entscheidungen und überprüfbare nächste Schritte ausgerichtet.'],
        [question: 'Wo finden sich fachliche Beispiele?', answer: 'Unter Insights finden Sie Fachbeiträge mit Kurzantworten, Diagnosefragen und praktischen Modellen. Die Praxisfälle zeigen zusätzlich anonymisierte Situationen und mögliche Vorgehensweisen.']
    ],
    '/insights': [
        [question: 'Welche Themen behandeln die Insights?', answer: 'Die Insights behandeln Product Leadership, Organisationsdiagnose, Rollen und Verantwortung, Operating Models, Entscheidungen und Transformation in Produktorganisationen.'],
        [question: 'Wie sind die Beiträge aufgebaut?', answer: 'Die Beiträge beginnen mit einer kompakten Antwort und vertiefen anschließend Ursachen, Zusammenhänge, Kriterien und praktische Fragen. Dadurch lassen sich Kernaussagen schnell erfassen und bei Bedarf genauer prüfen.'],
        [question: 'Dürfen die Inhalte als allgemeingültige Lösung verstanden werden?', answer: 'Nein. Die Beiträge vermitteln Modelle und Diagnosefragen. Konkrete organisatorische Entscheidungen sollten immer gegen den jeweiligen Kontext, vorhandene Evidenz und mögliche Nebenwirkungen geprüft werden.']
    ],
    '/leistungen': [
        [question: 'Welches Angebot ist der richtige Einstieg?', answer: 'Das hängt von der Fragestellung ab. Für eine konkrete Situation eignet sich meist die Clarity Session. Eine wichtige Entscheidung passt zum Decision Review. Bei wiederkehrenden organisatorischen Mustern ist eine Diagnose sinnvoll.'],
        [question: 'Muss der Umfang vor der Anfrage feststehen?', answer: 'Nein. Beschreiben Sie die Situation und das gewünschte Ergebnis. Danach wird der kleinste sinnvolle Einstieg empfohlen.'],
        [question: 'Kann aus einem kleinen Auftrag eine größere Begleitung entstehen?', answer: 'Ja, aber nur wenn die Diagnose dies rechtfertigt. Ein größerer Auftrag ist kein Selbstzweck, sondern folgt einem erkennbaren Bedarf.']
    ],
    '/clarity-session': [
        [question: 'Wann ist eine Clarity Session sinnvoll?', answer: 'Wenn eine konkrete Produkt-, Führungs- oder Organisationssituation feststeckt und der nächste sinnvolle Schritt unklar ist.'],
        [question: 'Was ist das Ergebnis?', answer: 'Sie erhalten ein klares Bild der Situation, erste prüfbare Erklärungen und einen begrenzten nächsten Schritt.'],
        [question: 'Ist die Session bereits eine vollständige Diagnose?', answer: 'Nein. Sie ist ein fokussierter Einstieg. Für eine belastbare Organisationsdiagnose sind meist weitere Perspektiven und konkrete Daten notwendig.']
    ],
    '/decision-review': [
        [question: 'Welche Entscheidungen können geprüft werden?', answer: 'Zum Beispiel Produkt-, Technologie-, Investitions- oder Organisationsentscheidungen mit relevanten Folgen.'],
        [question: 'Wird die Entscheidung für mich getroffen?', answer: 'Nein. Das Review prüft Annahmen, Risiken und Alternativen. Die Verantwortung für die Entscheidung bleibt bei Ihnen.'],
        [question: 'Welche Unterlagen werden benötigt?', answer: 'So wenig wie möglich und so viel wie nötig: die Entscheidungsfrage, vorhandene Optionen, wichtige Annahmen und relevante Hintergründe.']
    ],
    '/product-organisation-diagnostic': [
        [question: 'Was untersucht das ChOS Operating Model Diagnostic?', answer: 'Es untersucht Richtung, Entscheidungen, Verantwortung, Zusammenarbeit und Lernen in einem klar abgegrenzten Teil der Organisation.'],
        [question: 'Wie läuft die Diagnose ab?', answer: 'Die Fragestellung wird abgegrenzt, vorhandene Unterlagen werden geprüft und ausgewählte Gespräche werden geführt. Daraus entstehen ein Ursachenbild und priorisierte Schritte.'],
        [question: 'Ist das Ergebnis eine fertige Reorganisation?', answer: 'Nein. Das Ergebnis zeigt, was wahrscheinlich wirkt und welcher Eingriff sinnvoll ist. Eine neue Struktur ist nur eine mögliche Folge.']
    ],
    '/ai-operating-model-assessment': [
        [question: 'Wann ist dieses Assessment sinnvoll?', answer: 'Wenn neue technische Möglichkeiten Arbeit oder Entscheidungen verändern und Rollen, Kontrolle oder Verantwortung noch nicht klar sind.'],
        [question: 'Ist das eine technische Prüfung?', answer: 'Nein. Untersucht werden die organisatorischen Folgen: Ziele, Entscheidungen, Verantwortung, Zusammenarbeit und Lernmechanismen.'],
        [question: 'Was ist das Ergebnis?', answer: 'Sie erhalten ein klares Bild der offenen organisatorischen Fragen und eine priorisierte Reihenfolge für die nächsten Schritte.']
    ],
    '/ai-enabled-workflow-sprint': [
        [question: 'Was wird im Workflow Sprint betrachtet?', answer: 'Ein konkreter Ablauf wird von Anfang bis Ende untersucht: Arbeitsschritte, Übergaben, Entscheidungen, Verantwortung und Messgrößen.'],
        [question: 'Warum reicht die Automatisierung einzelner Schritte nicht?', answer: 'Weil der Engpass häufig nur weiterwandert. Entscheidend ist, ob der gesamte Ablauf schneller, klarer oder wirksamer wird.'],
        [question: 'Was entsteht im Sprint?', answer: 'Ein verständliches Zielbild für den Ablauf, klare Verantwortlichkeiten und ein begrenzter Test unter realen Bedingungen.']
    ],
    '/chos-transformation-program': [
        [question: 'Wann ist ein Transformation Program sinnvoll?', answer: 'Wenn mehrere Teams oder ein größerer Teil der Organisation betroffen sind und einzelne Maßnahmen nicht ausreichen.'],
        [question: 'Wie beginnt die Zusammenarbeit?', answer: 'Immer mit einer Diagnose. Erst danach werden Zielbild, Reihenfolge und Umfang der Veränderung festgelegt.'],
        [question: 'Wie wird die Wirkung geprüft?', answer: 'Über vorab vereinbarte Beobachtungen und Messgrößen. Veränderungen werden angepasst, wenn die erwartete Wirkung ausbleibt.']
    ],
    '/executive-sparring': [
        [question: 'Für wen ist das Sparring gedacht?', answer: 'Für Führungskräfte und Product Leader, die wichtige Entscheidungen in einem vertraulichen und unabhängigen Rahmen prüfen möchten.'],
        [question: 'Welche Themen können besprochen werden?', answer: 'Zum Beispiel Prioritäten, Verantwortung, Stakeholder, Führung, Organisationsfragen und schwer umkehrbare Entscheidungen.'],
        [question: 'Wie unterscheidet sich Sparring von Beratung?', answer: 'Im Sparring bleiben Sie klar in der Verantwortung. Der Wert liegt im strukturierten Gegenüber, in guten Fragen und im unabhängigen Blick auf Muster und Annahmen.']
    ]
]

List<Map<String, String>> insightFaq = [
    [question: 'Was ist die wichtigste praktische Konsequenz?', answer: 'Beginnen Sie mit einer konkreten, wiederkehrenden Situation. Trennen Sie Beobachtung, Erklärung und gewünschte Wirkung, bevor Sie eine Rolle, einen Prozess oder eine Struktur verändern.'],
    [question: 'Wie lässt sich die Aussage im eigenen Kontext prüfen?', answer: 'Suchen Sie mehrere konkrete Beispiele, vergleichen Sie unterschiedliche Perspektiven und prüfen Sie bewusst auch Gegenbelege. Ein einzelner Vorfall reicht selten aus, um einen organisatorischen Mechanismus belastbar zu erklären.'],
    [question: 'Wann ist eine vertiefte Organisationsdiagnose sinnvoll?', answer: 'Wenn das Muster trotz früherer Maßnahmen wiederkehrt, mehrere plausible Ursachen bestehen oder eine Veränderung viele Teams und Entscheidungen betrifft, ist eine systematische Diagnose sinnvoller als ein weiterer Standardansatz.']
]

if (website.nodeExists('/insights')) {
    Node insights = website.getNode('/insights')
    for (Node child : insights.nodes) {
        if (child.isNodeType('mgnl:page')) pageFaqs[child.getPath()] = insightFaq
    }
}

pageFaqs.each { String path, List<Map<String, String>> items ->
    if (!website.nodeExists(path)) throw new IllegalStateException("Seite nicht gefunden: ${path}")
    Node page = website.getNode(path)
    Node area = page.hasNode('main') ? page.getNode('main') : page.addNode('main', 'mgnl:area')

    List<Node> oldFaqs = []
    for (Node child : area.nodes) {
        if (child.hasProperty('mgnl:template') && child.getProperty('mgnl:template').string == 'meine-website:components/faq') oldFaqs << child
    }
    oldFaqs.each { it.remove() }

    Node faq = area.addNode('faq', 'mgnl:component')
    faq.setProperty('mgnl:template', 'meine-website:components/faq')
    faq.setProperty('eyebrow', 'Häufige Fragen')
    faq.setProperty('heading', 'Kurz und konkret beantwortet.')
    faq.setProperty('introduction', 'Antworten auf Fragen, die zu diesem Thema häufig entstehen.')
    Node itemContainer = faq.addNode('items', 'mgnl:contentNode')
    items.eachWithIndex { Map<String, String> item, int index ->
        Node entry = itemContainer.addNode(String.format('%02d', index), 'mgnl:contentNode')
        entry.setProperty('question', item.question)
        entry.setProperty('answer', item.answer)
    }
    println "${path}: ${items.size()} FAQ-Einträge ergänzt"
}

website.save()
println "FAQ-Module auf ${pageFaqs.size()} öffentlichen Seiten vorbereitet."
