import info.magnolia.commands.CommandsManager
import info.magnolia.context.MgnlContext

import javax.jcr.Node
import javax.jcr.Session

Session problems = MgnlContext.getJCRSession('problems')

def checks = [
    'leadership-bottleneck': [
        title: '7-Tage-Entscheidungsprotokoll',
        intro: '<p>Notiere sieben Tage lang jede relevante Entscheidung, bei der du einbezogen wirst. Verändere zunächst nichts. Ziel ist nicht, möglichst viele Eskalationen abzuschaffen, sondern sinnvolle und unnötige Einbeziehung voneinander zu unterscheiden.</p>',
        steps: [
            [title: 'Jede Entscheidung kurz festhalten', body: '<p>Notiere direkt nach der Situation:</p><ul><li>Welche Entscheidung stand an?</li><li>Wer hat dich einbezogen?</li><li>Was wurde von dir gebraucht: Information, Beratung, Freigabe oder die Entscheidung selbst?</li><li>Warum war deine Beteiligung aus Sicht der Person nötig?</li></ul>'],
            [title: 'Die Einbeziehung einordnen', body: '<p>Ordne jeden Fall einer von drei Gruppen zu:</p><ul><li><strong>richtige Eskalation</strong> – die Entscheidung gehört wegen Mandat, Budget oder Risiko zu dir;</li><li><strong>sinnvolle Einbeziehung</strong> – das Team entscheidet, braucht aber Kontext oder Beratung;</li><li><strong>unnötige Eskalation</strong> – Wissen und Mandat wären vorhanden, trotzdem entscheidest du.</li></ul>'],
            [title: 'Nach sieben Tagen ein Muster wählen', body: '<p>Suche nicht sofort nach einer Gesamtlösung. Wähle den häufigsten unnötigen Entscheidungstyp und frage: Fehlt Mandat, Kontext, Sicherheit oder eine klare Grenze?</p>'],
            [title: 'Einen kleinen Test vereinbaren', body: '<p>Definiere für genau diesen Entscheidungstyp, wer entscheidet, wann du nur informiert wirst und wann eskaliert werden muss. Teste diese Regel vier Wochen und prüfe anschließend Geschwindigkeit, Qualität und neue Risiken.</p>']
        ]
    ],
    'langsame-entscheidungen': [
        title: 'Eine festhängende Entscheidung in 10 Minuten klären',
        intro: '<p>Nimm eine konkrete Entscheidung, die gerade festhängt. Nach zehn Minuten solltest du nicht zwingend entschieden haben – aber klar benennen können, was die Entscheidung blockiert und was als Nächstes passieren muss.</p>',
        steps: [
            [title: 'Die Entscheidung in einen Satz bringen', body: '<p>Vervollständige: <strong>„Wir entscheiden, ob …“</strong> Wenn der Satz mehrere Entscheidungen enthält, trenne sie. Ein unscharfes Thema lässt sich nicht klar entscheiden.</p>'],
            [title: 'Die Rollen klären', body: '<p>Notiere genau eine Person oder Rolle, die entscheidet. Ergänze, wer vorher Input geben muss und wer anschließend nur informiert wird. Beteiligung bedeutet nicht automatisch Zustimmung.</p>'],
            [title: 'Den echten Blocker benennen', body: '<p>Wähle den wichtigsten Grund: Es fehlt eine konkrete Information, das Mandat ist unklar, das Risiko ist nicht eingeordnet, Konsens wird erwartet oder niemand möchte die Konsequenz tragen.</p>'],
            [title: 'Den nächsten Entscheidungspunkt setzen', body: '<p>Fehlt Information, benenne welche, wer sie bis wann liefert und wann danach entschieden wird. Fehlt keine Information, setze einen Entscheidungstermin und halte fest, was eine weitere Woche Nicht-Entscheiden kostet.</p>']
        ]
    ],
    'ai-decision-rights': [
        title: 'Einen AI-Anwendungsfall vor Autonomie prüfen',
        intro: '<p>Wähle einen einzelnen AI-Anwendungsfall – nicht das gesamte AI-Programm. Der Check macht sichtbar, welche Entscheidung das System übernimmt, welche Grenzen gelten und wo menschliche Verantwortung konkret bleibt.</p>',
        steps: [
            [title: 'Entscheidung und AI-Rolle benennen', body: '<p>Schreibe die konkrete Entscheidung oder Handlung auf. Ordne dann zu: Die AI <strong>informiert</strong>, <strong>empfiehlt</strong>, <strong>entscheidet innerhalb von Grenzen</strong> oder <strong>handelt selbstständig</strong>.</p>'],
            [title: 'Fehlerfolgen einordnen', body: '<p>Beschreibe den plausibelsten Fehler und seine Auswirkung. Ist er leicht rückgängig zu machen, finanziell oder rechtlich relevant, kundenwirksam oder sicherheitskritisch? Lege fest, was nicht akzeptabel wäre.</p>'],
            [title: 'Verantwortung und Eingriff klären', body: '<p>Benenne die fachlich verantwortliche Person. Definiere, wann ein Mensch übernehmen muss und wer das System stoppen oder eine Handlung zurücksetzen darf.</p>'],
            [title: 'Begrenzt testen und Wirkung messen', body: '<p>Starte mit einem kleinen Entscheidungsraum. Lege vorab fest, woran du Qualität, Fehler und unerwartete Auswirkungen erkennst. Erweitere Autonomie erst, wenn diese Signale stabil sind.</p>']
        ]
    ],
    'leadership-transition': [
        title: 'Dein Beobachtungsprotokoll für die ersten zwei Wochen',
        intro: '<p>Nimm dir nach wichtigen Gesprächen, Entscheidungen oder Konflikten drei Minuten Zeit. Halte eine konkrete Situation fest, bevor du sie bewertest. Nach zwei Wochen suchst du nach Mustern – erst dann entscheidest du über Veränderungen.</p>',
        steps: [
            [title: 'Eine konkrete Situation notieren', body: '<p>Schreibe Datum, Beteiligte und Anlass auf. Halte anschließend nur fest, was beobachtbar war: ein Satz, eine Entscheidung oder ein konkretes Verhalten. Noch keine Bewertung wie „Das Team übernimmt keine Verantwortung“.</p>'],
            [title: 'Eine Perspektive zuordnen', body: '<p>Markiere, worum es hauptsächlich ging:</p><ul><li><strong>Richtung:</strong> War klar, was wichtig ist und warum?</li><li><strong>Entscheidungen:</strong> Wer hat tatsächlich entschieden?</li><li><strong>Verantwortung:</strong> Passten Verantwortung und Entscheidungsraum zusammen?</li><li><strong>Zusammenarbeit:</strong> Wo entstand Reibung?</li><li><strong>Lernen:</strong> Woran wurde Wirkung erkannt?</li></ul>'],
            [title: 'Beobachtung und Erklärung trennen', body: '<p>Ergänze drei kurze Sätze: <strong>Das habe ich beobachtet.</strong> – <strong>Meine erste Erklärung dafür ist.</strong> – <strong>Was ich noch nicht weiß.</strong> So wird aus einem ersten Eindruck eine prüfbare Hypothese.</p>'],
            [title: 'Nach zwei Wochen Muster prüfen', body: '<p>Welche Situationen wiederholen sich? Wo widersprechen sich Beobachtungen? Was funktioniert bereits gut? Wähle ein Muster und prüfe es in Gesprächen. Verändere noch nicht die ganze Organisation.</p>']
        ]
    ],
    'unklare-prioritaeten': [
        title: 'Direction & Priority Check',
        intro: '<p>Nimm ein Team oder einen Bereich, bei dem regelmäßig Unklarheit über Prioritäten entsteht. Der Check soll keine neue Roadmap erzeugen. Er macht sichtbar, welche konkrete Entscheidung fehlt, damit das Team sinnvoll weiterarbeiten kann.</p>',
        steps: [
            [title: 'Drei wichtige Ergebnisse benennen', body: '<p>Notiere die drei Ergebnisse, die aktuell wirklich Vorrang haben. Ergänze zu jedem Ergebnis einen Satz: Warum ist es gerade wichtiger als andere sinnvolle Themen?</p>'],
            [title: 'Kriterien für neue Arbeit klären', body: '<p>Halte fest, nach welchen Kriterien neue Arbeit bewertet wird – zum Beispiel Kundennutzen, Risiko, strategische Relevanz, Umsatz oder Effizienz. Prüfe, ob alle Beteiligten dieselben Kriterien verwenden.</p>'],
            [title: 'Entscheidungsrechte sichtbar machen', body: '<p>Benenne, wer bei konkurrierenden Prioritäten entscheidet, wer neue Arbeit starten darf, wer Nein sagen kann und welche Entscheidungen bewusst oberhalb des Teams liegen.</p>'],
            [title: 'Start, Stopp und Transparenz verbinden', body: '<p>Lege fest, was gestoppt oder verschoben wird, wenn etwas Neues beginnt. Beschreibe außerdem, wie die Entscheidung für Teams und Stakeholder sichtbar und nachvollziehbar wird.</p>']
        ]
    ]
]

def setHtml = { Node node, String name, String value ->
    node.setProperty(name, value)
}

checks.each { slug, check ->
    String path = "/cleonhardt/${slug}"
    if (!problems.nodeExists(path)) {
        throw new IllegalStateException("Problemsituation fehlt: ${path}")
    }

    Node problem = problems.getNode(path)
    problem.setProperty('selfCheckTitle', check.title)
    setHtml(problem, 'selfCheckIntro', check.intro)

    if (problem.hasNode('selfCheckSteps')) {
        problem.getNode('selfCheckSteps').remove()
    }
    Node container = problem.addNode('selfCheckSteps', 'mgnl:contentNode')
    check.steps.eachWithIndex { step, index ->
        Node item = container.addNode(String.format('%02d', index + 1), 'mgnl:contentNode')
        item.setProperty('title', step.title)
        setHtml(item, 'body', step.body)
    }
}

problems.save()

def commands = CommandsManager.getInstance()
checks.keySet().each { slug ->
    def parameters = new LinkedHashMap<String, Object>()
    parameters.put('repository', 'problems')
    parameters.put('path', "/cleonhardt/${slug}")
    parameters.put('recursive', true)
    def result = commands.executeCommand('default', 'publish', parameters)
    println "${slug}: ${result ? 'aktualisiert und veröffentlicht' : 'Veröffentlichung nicht bestätigt'}"
}

println 'Alle fünf praktischen Checks wurden in klare Vier-Schritt-Abläufe überführt.'
