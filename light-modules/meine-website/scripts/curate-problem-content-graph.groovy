import info.magnolia.commands.CommandsManager
import info.magnolia.context.MgnlContext

import javax.jcr.Node
import javax.jcr.Session

Session problems = MgnlContext.getJCRSession('problems')
Session website = MgnlContext.getJCRSession('website')
Session insights = MgnlContext.getJCRSession('insights')
Session tools = MgnlContext.getJCRSession('tools')
Session cases = MgnlContext.getJCRSession('cases')
Session offers = MgnlContext.getJCRSession('offers')

Node findByProperty(Node root, String propertyName, String propertyValue) {
    if (root.hasProperty(propertyName) && root.getProperty(propertyName).string == propertyValue) return root
    for (Node child : root.nodes) {
        Node match = findByProperty(child, propertyName, propertyValue)
        if (match != null) return match
    }
    null
}

Node requireByProperty(Session session, String propertyName, String propertyValue) {
    Node result = findByProperty(session.rootNode, propertyName, propertyValue)
    if (result == null) throw new IllegalStateException("Inhalt fehlt: ${session.workspace.name}.${propertyName}=${propertyValue}")
    result
}

Node requireOffer(Session offerSession, String nodeName) {
    String path = "/cleonhardt/${nodeName}"
    if (!offerSession.nodeExists(path)) throw new IllegalStateException("Leistung fehlt: ${path}")
    offerSession.getNode(path)
}

void replaceTextList(Node parent, String name, List<String> values) {
    if (parent.hasNode(name)) parent.getNode(name).remove()
    Node list = parent.addNode(name, 'mgnl:contentNode')
    values.eachWithIndex { String value, int index ->
        Node item = list.addNode(String.format('%02d', index + 1), 'mgnl:contentNode')
        item.setProperty('text', value)
    }
}

void setReferences(Node node, String name, Collection<Node> targets) {
    List<Node> cleanTargets = targets.findAll { it != null }
    if (cleanTargets) {
        node.setProperty(name, cleanTargets.collect { it.getIdentifier() } as String[])
    } else if (node.hasProperty(name)) {
        node.getProperty(name).remove()
    }
}

def definitions = [
    'leadership-bottleneck': [
        questions: [
            'Welche Entscheidungen landen wiederholt bei dir – und welche davon gehören tatsächlich auf deine Ebene?',
            'Braucht das Team deine Information, deine Beratung, deine Freigabe oder deine Entscheidung?',
            'Fehlen dem Team Mandat, Kontext oder klare Grenzen, um selbst zu entscheiden?',
            'Welche Risiken darf das Team selbst tragen – und wann ist eine Eskalation sinnvoll?',
            'Was passiert, wenn eine Entscheidung anders ausfällt, als du es selbst getan hättest?',
            'Was tust du selbst, das Rückversicherung sicherer macht als eigenständiges Entscheiden?'
        ],
        avoid: [
            'Noch keine Rollen neu schneiden.',
            'Noch kein Delegationsframework einführen.',
            'Noch keine Ownership-Offensive starten.',
            'Noch keine zusätzlichen Freigaberegeln bauen.'
        ],
        problems: ['langsame-entscheidungen', 'leadership-transition'],
        insightSlugs: ['diagnose-vor-eingriff', 'unklare-rollen-sind-selten-das-eigentliche-problem'],
        caseSlugs: ['neue-rollen-alte-entscheidungen'],
        offerNames: ['chos-clarity-session']
    ],
    'langsame-entscheidungen': [
        questions: [
            'Was genau muss entschieden werden – in einem Satz?',
            'Wer entscheidet formal – und wer entscheidet praktisch?',
            'Wessen Input ist erforderlich – und wessen Zustimmung wird nur vorsorglich gesucht?',
            'Welche konkrete Information fehlt noch – und würde sie die Entscheidung tatsächlich verändern?',
            'Welches Risiko wird abgesichert – und wie reversibel ist die Entscheidung?',
            'Was kostet eine weitere Woche Nicht-Entscheiden?',
            'Was macht Nicht-Entscheiden im aktuellen System sicherer als Entscheiden?'
        ],
        avoid: [
            'Noch kein neues Entscheidungsframework einführen.',
            'Noch kein zusätzliches Steering Committee aufsetzen.',
            'Noch keine weiteren Zustimmungsschleifen einbauen.',
            'Noch nicht mehr Daten sammeln, bevor der konkrete Informationsbedarf klar ist.'
        ],
        problems: ['leadership-bottleneck', 'ai-decision-rights'],
        insightSlugs: ['annahmen-vor-einer-produktentscheidung-pruefen', 'rollen-und-verantwortlichkeiten-in-produktorganisationen-klaeren'],
        caseSlugs: ['prioritaetsentscheidung-beginnt-von-vorn'],
        offerNames: ['decision-review']
    ],
    'ai-decision-rights': [
        questions: [
            'Welche konkrete Entscheidung oder Handlung soll die AI übernehmen?',
            'Informiert, empfiehlt, entscheidet oder handelt das System?',
            'Welche Folgen hätte ein plausibler Fehler – und welche davon wären nicht akzeptabel?',
            'Wer trägt die fachliche Verantwortung für die Entscheidung?',
            'Unter welchen Bedingungen muss ein Mensch übernehmen?',
            'Wer darf das System stoppen oder eine Handlung zurücksetzen?',
            'Woran erkennst du Entscheidungsqualität und unerwartete Auswirkungen?'
        ],
        avoid: [
            'Noch nicht das gesamte AI-Programm auf einmal gestalten.',
            'Noch keine Autonomie freigeben, bevor der Entscheidungsraum klar ist.',
            'Noch kein pauschales „Human in the Loop“ als Governance-Ersatz verwenden.',
            'Noch keine fachliche Verantwortung beim technischen Betrieb abladen.',
            'Noch nicht skalieren, bevor Qualität und Fehlerfolgen messbar sind.'
        ],
        problems: ['langsame-entscheidungen'],
        insightSlugs: ['warum-ai-einfuehrung-ein-operating-model-thema-ist', 'decision-rights-zwischen-mensch-und-ai'],
        caseSlugs: [],
        offerNames: ['chos-quick-diagnostic']
    ],
    'leadership-transition': [
        questions: [
            'Was beobachtest du tatsächlich – getrennt von deiner ersten Interpretation?',
            'Wo und von wem werden Entscheidungen formal und praktisch getroffen?',
            'Was funktioniert bereits gut und sollte nicht vorschnell verändert werden?',
            'Welche Erfahrungen haben die heutigen Routinen und Schutzmechanismen geprägt?',
            'Was erwartet dein Team von dir – und was erwartet dein Vorgesetzter tatsächlich?',
            'Welche Erklärung bestätigt sich in mehreren konkreten Situationen?',
            'Welcher kleine, reversible Eingriff könnte diese Hypothese prüfen?'
        ],
        avoid: [
            'Noch keine Reorganisation starten.',
            'Noch keine Rollen neu schneiden.',
            'Noch kein neues KPI-, Prozess- oder Meeting-System ausrollen.',
            'Noch keine Personalentscheidung allein auf den ersten Eindruck stützen.',
            'Noch keine pauschale Ownership-Diagnose stellen.'
        ],
        problems: ['leadership-bottleneck', 'langsame-entscheidungen'],
        insightSlugs: ['diagnose-vor-eingriff', 'wann-ist-executive-sparring-sinnvoll'],
        caseSlugs: ['neue-rollen-alte-entscheidungen'],
        offerNames: ['executive-sparring']
    ]
]

Map<String, Node> problemNodes = definitions.keySet().collectEntries { String slug ->
    [(slug): requireByProperty(problems, 'slug', slug)]
}
Node selfCheck = requireByProperty(tools, 'slug', 'chos-selbstcheck')

definitions.each { String slug, Map definition ->
    Node problem = problemNodes[slug]
    replaceTextList(problem, 'diagnosticQuestions', definition.questions as List<String>)
    replaceTextList(problem, 'avoidActions', definition.avoid as List<String>)
    setReferences(problem, 'relatedProblems', definition.problems.collect { problemNodes[it] })
    setReferences(problem, 'relatedTools', [selfCheck])
    setReferences(problem, 'relatedInsights', definition.insightSlugs.collect { requireByProperty(insights, 'slug', it) })
    setReferences(problem, 'relatedCases', definition.caseSlugs.collect { requireByProperty(cases, 'slug', it) })
    setReferences(problem, 'relatedOffers', definition.offerNames.collect { requireOffer(offers, it) })
}

setReferences(selfCheck, 'relatedProblems', problemNodes.values())

def insightProblems = [:].withDefault { [] }
def caseProblems = [:].withDefault { [] }
definitions.each { String problemSlug, Map definition ->
    definition.insightSlugs.each { insightProblems[it] = insightProblems[it] + problemNodes[problemSlug] }
    definition.caseSlugs.each { caseProblems[it] = caseProblems[it] + problemNodes[problemSlug] }
}
insightProblems.each { String slug, List<Node> linkedProblems ->
    Node insightNode = requireByProperty(insights, 'slug', slug)
    setReferences(insightNode, 'relatedProblems', linkedProblems.unique { it.getIdentifier() })
    String pagePath = "/insights/${slug}"
    if (!website.nodeExists(pagePath)) throw new IllegalStateException("Insight-Seite fehlt: ${pagePath}")
    insightNode.setProperty('sourcePage', website.getNode(pagePath).getIdentifier())
}
caseProblems.each { String slug, List<Node> linkedProblems ->
    Node caseNode = requireByProperty(cases, 'slug', slug)
    setReferences(caseNode, 'relatedProblems', linkedProblems.unique { it.getIdentifier() })
    caseNode.setProperty('sourcePage', website.getNode('/praxisfaelle').getIdentifier())
}

[problems, insights, tools, cases].each { it.save() }

def commands = CommandsManager.getInstance()
List<Map<String, String>> publishTargets = []
definitions.keySet().each { publishTargets << [repository: 'problems', path: "/cleonhardt/${it}"] }
insightProblems.keySet().each { publishTargets << [repository: 'insights', path: "/cleonhardt/${it}"] }
publishTargets << [repository: 'tools', path: '/cleonhardt/chos-selbstcheck']
caseProblems.keySet().each { publishTargets << [repository: 'cases', path: "/cleonhardt/${it}"] }

publishTargets.each { Map<String, String> target ->
    def parameters = new LinkedHashMap<String, Object>()
    parameters.put('repository', target.repository)
    parameters.put('path', target.path)
    parameters.put('recursive', true)
    commands.executeCommand('default', 'publish', parameters)
}

println 'Diagnosefragen, Anti-Patterns und Related Content der vier Problemsituationen redaktionell kuratiert und veröffentlicht.'
