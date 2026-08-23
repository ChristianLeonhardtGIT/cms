import info.magnolia.commands.CommandsManager
import info.magnolia.context.MgnlContext

import javax.jcr.Node
import javax.jcr.Session
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar
import java.util.GregorianCalendar

/*
 * Legt die fünfte ChOS-Problemsituation strukturiert und wiederholbar an.
 * Der fachliche Inhalt basiert auf der freigegebenen Markdown-Fassung
 * „Wenn Teams nicht wissen, woran sie als Nächstes arbeiten sollen“.
 */

Session problems = MgnlContext.getJCRSession('problems')
Session website = MgnlContext.getJCRSession('website')
Session topics = MgnlContext.getJCRSession('topics')
Session insights = MgnlContext.getJCRSession('insights')
Session tools = MgnlContext.getJCRSession('tools')
Session cases = MgnlContext.getJCRSession('cases')
Session offers = MgnlContext.getJCRSession('offers')
Session navigation = MgnlContext.getJCRSession('navigation')

Node ensureFolder(Session session, String name) {
    session.rootNode.hasNode(name) ? session.rootNode.getNode(name) : session.rootNode.addNode(name, 'mgnl:folder')
}

Node ensureContent(Node folder, String name) {
    folder.hasNode(name) ? folder.getNode(name) : folder.addNode(name, 'mgnl:content')
}

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

Node requireOffer(Session session, String nodeName) {
    String path = "/cleonhardt/${nodeName}"
    if (!session.nodeExists(path)) throw new IllegalStateException("Leistung fehlt: ${path}")
    session.getNode(path)
}

void replaceList(Node parent, String name, List<Map<String, String>> rows) {
    if (parent.hasNode(name)) parent.getNode(name).remove()
    Node list = parent.addNode(name, 'mgnl:contentNode')
    rows.eachWithIndex { Map<String, String> row, int index ->
        Node item = list.addNode(String.format('%02d', index + 1), 'mgnl:contentNode')
        row.each { String key, String value -> item.setProperty(key, value) }
    }
}

void replaceTextList(Node parent, String name, List<String> values) {
    replaceList(parent, name, values.collect { [text: it] })
}

void setReferences(Node node, String name, Collection<Node> targets) {
    List<Node> cleanTargets = targets.findAll { it != null }
    if (cleanTargets) {
        node.setProperty(name, cleanTargets.collect { it.getIdentifier() } as String[])
    } else if (node.hasProperty(name)) {
        node.getProperty(name).remove()
    }
}

void appendReference(Node node, String name, Node target) {
    List<String> identifiers = []
    if (node.hasProperty(name)) {
        def property = node.getProperty(name)
        identifiers = property.multiple ? property.values.collect { it.string } : [property.string]
    }
    if (!identifiers.contains(target.getIdentifier())) identifiers << target.getIdentifier()
    node.setProperty(name, identifiers as String[])
}

Calendar calendar(String date) {
    GregorianCalendar.from(LocalDate.parse(date).atStartOfDay(ZoneId.of('Europe/Berlin')))
}

void replaceTextRecursively(Node node, String from, String to) {
    def properties = node.properties
    while (properties.hasNext()) {
        def property = properties.nextProperty()
        if (!property.multiple && property.type == javax.jcr.PropertyType.STRING && property.string.contains(from)) {
            property.setValue(property.string.replace(from, to))
        }
    }
    for (Node child : node.nodes) replaceTextRecursively(child, from, to)
}

Node findNavigationItem(Node parent, Node target) {
    for (Node item : parent.nodes) {
        if (item.hasProperty('targetPage') && item.getProperty('targetPage').string == target.getIdentifier()) return item
    }
    throw new IllegalStateException("Navigationseintrag nicht gefunden: ${target.getPath()}")
}

void syncProblemNavigation(Session navigation, Node problemsPage) {
    Node items = navigation.getNode('/cleonhardt/Main/items')
    Node situationsItem = findNavigationItem(items, problemsPage)
    if (situationsItem.hasNode('children')) situationsItem.getNode('children').remove()
    Node children = situationsItem.addNode('children', 'mgnl:contentNode')

    int index = 0
    for (Node page : problemsPage.nodes) {
        if (!page.isNodeType('mgnl:page') || !page.hasProperty('problemReference')) continue
        String label = page.hasProperty('navigationTitle') ? page.getProperty('navigationTitle').string :
            (page.hasProperty('title') ? page.getProperty('title').string : page.getName())
        Node item = children.addNode(String.format('%02d', index++), 'mgnl:contentNode')
        item.setProperty('label', label)
        item.setProperty('targetPage', page.getIdentifier())
    }
    navigation.save()
}

String slug = 'unklare-prioritaeten'
Node folder = ensureFolder(problems, 'cleonhardt')
Node problem = ensureContent(folder, slug)

problem.setProperty('title', 'Unklare Prioritäten')
problem.setProperty('slug', slug)
problem.setProperty('headline', 'Wenn Teams nicht wissen, woran sie als Nächstes arbeiten sollen')
problem.setProperty('subheadline', 'Warum das nicht automatisch ein Roadmap- oder Priorisierungsproblem ist')
problem.setProperty('intro', '''
<p>Das Team ist da. Die Menschen sind arbeitsfähig. Es gibt genug Themen – vielleicht sogar zu viele.</p>
<p>Und trotzdem entsteht immer wieder dieselbe Frage:</p>
<p><strong>„Was ist jetzt eigentlich wirklich wichtig?“</strong></p>
<p>Auf der Roadmap stehen mehrere Initiativen. Stakeholder bringen neue Anforderungen ein. Strategische Themen konkurrieren mit operativen Problemen. Technische Schulden wollen gelöst werden. AI-Projekte kommen dazu.</p>
<p>Irgendwann warten Teams auf Orientierung, weil niemand belastbar sagen kann, woran sie als Nächstes arbeiten sollen.</p>
<p>Bevor du ein neues Priorisierungsframework einführst oder die Roadmap neu sortierst, lohnt sich deshalb eine andere Frage:</p>
<p><strong>Was fehlt dem System heute, damit Teams sinnvoll entscheiden können, woran sie arbeiten sollen?</strong></p>''')
problem.setProperty('situation', '''
<h3>Wenn alles wichtig ist, ist häufig nichts entschieden</h3>
<p>In vielen Organisationen fehlt nicht die Idee. Es fehlt eine Entscheidung.</p>
<p>Es gibt Kundenanforderungen, technische Themen, strategische Initiativen, regulatorische Anforderungen, Management-Wünsche, operative Probleme, Wachstumsziele, Effizienzprogramme, neue Technologien und Stakeholder-Erwartungen.</p>
<p>Jedes einzelne Thema kann sinnvoll sein. Das beantwortet aber noch nicht:</p>
<p><strong>Was ist wichtiger als etwas anderes?</strong></p>
<p>Und vor allem: <strong>Wer entscheidet das?</strong></p>''')

replaceTextList(problem, 'symptoms', [
    'Teams fragen regelmäßig, welches Thema Priorität hat.',
    'Mehrere Initiativen laufen gleichzeitig an.',
    'Roadmaps ändern sich ständig.',
    'Neue Themen kommen hinzu, ohne dass alte gestoppt werden.',
    'Unterschiedliche Stakeholder haben unterschiedliche „Top-Prioritäten“.',
    'Teams arbeiten an Themen, deren strategischer Zweck unklar ist.',
    'Entscheidungen über Prioritäten werden immer wieder nach oben eskaliert.',
    'Kurzfristige Anforderungen verdrängen langfristige Ziele.',
    'Roadmaps werden zu Listen statt zu echten Entscheidungsgrundlagen.',
    'Teams warten auf Freigaben, bevor sie anfangen können.',
    'Es gibt viel Aktivität, aber wenig gemeinsame Richtung.'
])

problem.setProperty('commonAssumption', '''
<h3>„Unsere Roadmap funktioniert nicht.“</h3>
<p>Oder: „Wir müssen besser priorisieren.“ Das kann stimmen.</p>
<p>Aber eine Roadmap ist nur ein sichtbarer Ausdruck von Entscheidungen. Wenn die Entscheidungen darunter unklar sind, wird auch eine neue Roadmap das Problem nicht lösen.</p>
<p>Dann entsteht vielleicht nur eine schönere Darstellung derselben Unsicherheit.</p>''')
problem.setProperty('diagnosisIntro', '''
<p>„Bessere Priorisierung“ ist zunächst nur eine mögliche Lösung. Noch keine Diagnose.</p>
<p>Wenn ein Team fragt, woran es arbeiten soll, kann die falsche Reaktion sein: „Das Team muss mehr Ownership übernehmen.“ Vielleicht fehlt dem Team gar keine Ownership. Vielleicht fehlt eine Entscheidung, die außerhalb seines Mandats liegt.</p>
<p><strong>Zusätzliche Verantwortung löst keine fehlende Entscheidung. Sie erhöht dann nur die Unsicherheit.</strong></p>''')

replaceList(problem, 'hypotheses', [
    [title: 'Was könnte tatsächlich dahinterstecken?', body: '<p>Es gibt nicht die eine Ursache. Richtung, Kriterien, Entscheidungsrechte, Governance und fehlender Kontext können gleichzeitig wirken.</p>'],
    [title: '1. Die strategische Richtung ist nicht klar genug', body: '<p>Teams können nur sinnvoll priorisieren, wenn sie verstehen, welche Ziele tatsächlich wichtig sind. Nicht nur: „Wir wollen wachsen.“ Sondern: Welches Ergebnis, welche Kundengruppe, welcher Markt oder welche Business-Wirkung hat aktuell Vorrang?</p><p>Wenn diese Richtung fehlt, wird Priorisierung schnell zur Meinungsfrage.</p>'],
    [title: '2. Es fehlen klare Priorisierungskriterien', body: '<p>„Wir priorisieren nach Business Value“ reicht nicht. Bedeutet das Umsatz, Kundennutzen, Risiko, strategische Relevanz, Effizienz oder Time-to-Market?</p><p>Ohne gemeinsame Kriterien bewertet jeder Stakeholder „wichtig“ anders. Dann entsteht keine Priorisierung, sondern Verhandlung.</p>'],
    [title: '3. Entscheidungsrechte sind unklar', body: '<p>Vielleicht wissen alle, dass priorisiert werden muss. Aber niemand weiß genau, wer bei konkurrierenden Themen entscheiden darf: Team, Product Owner, Head of Product, Business, Steering Committee oder Geschäftsführung.</p><p>Wenn mehrere Personen faktisch ein Vetorecht besitzen, wird jede Priorisierung fragil.</p>'],
    [title: '4. Governance fehlt oder funktioniert nicht', body: '<p>Governance bedeutet zunächst: Wie werden verbindliche Entscheidungen getroffen, sichtbar gemacht und überprüft?</p><p>Fehlt ein verlässlicher Mechanismus, entscheidet situativ der lauteste Stakeholder, der höchste Hierarchiegrad, das dringendste Problem oder der nächste Termin im Steering Committee. Das ist ebenfalls Governance – nur keine besonders gute.</p>'],
    [title: '5. Die Roadmap wird mit Strategie verwechselt', body: '<p>Eine Roadmap kann zeigen, was geplant ist. Sie erklärt aber nicht automatisch, warum diese Themen wichtiger sind als andere.</p><p>Die wichtigeren Fragen lauten: Welche Entscheidungen stecken hinter dieser Roadmap? Und welche Themen haben wir bewusst nicht aufgenommen?</p>'],
    [title: '6. Neue Arbeit kann jederzeit ins System gelangen', body: '<p>Vielleicht ist die Priorisierung am Montag klar. Danach kommen eine Management-Anfrage, ein technisches Problem und ein neuer strategischer Schwerpunkt.</p><p>Wenn neue Arbeit jederzeit gestartet werden kann, aber nichts beendet oder gestoppt wird, verliert jede Priorisierung ihre Wirkung. Was muss aufhören, wenn etwas Neues beginnt?</p>'],
    [title: '7. Teams fehlen Informationen, die sie für Entscheidungen brauchen', body: '<p>Ein Team soll selbstständig priorisieren, kennt aber möglicherweise Budgetgrenzen, strategische Ziele, Abhängigkeiten, Kundenwirkung, Risiken, regulatorische Anforderungen oder Erwartungen anderer Bereiche nicht.</p><p>Dann ist Zurückhaltung verständlich. Autonomie ohne Kontext ist keine echte Entscheidungsfähigkeit.</p>'],
    [title: '8. Entscheidungen liegen bewusst oberhalb des Teams', body: '<p>Nicht jede Priorität gehört ins Team. Vielleicht konkurrieren mehrere Produkte um dasselbe Budget oder mehrere Bereiche um dieselbe Kapazität.</p><p>Das Problem entsteht erst, wenn diese Entscheidung auf der höheren Ebene nicht getroffen wird und das Team trotzdem liefern soll.</p>']
])

replaceTextList(problem, 'diagnosticQuestions', [
    'Welche drei Ergebnisse sind aktuell wirklich am wichtigsten?',
    'Kann das Team erklären, warum genau diese drei wichtig sind?',
    'Nach welchen Kriterien wird neue Arbeit bewertet?',
    'Wer entscheidet bei konkurrierenden Prioritäten?',
    'Wer darf ein neues Thema starten – und wer darf Nein sagen?',
    'Was wird gestoppt, wenn etwas Neues beginnt?',
    'Welche Entscheidung liegt bewusst oberhalb des Teams?'
])

problem.setProperty('selfCheckTitle', 'Direction & Priority Check')
problem.setProperty('selfCheckIntro', '''
<p>Nimm ein Team oder einen Bereich, bei dem regelmäßig Unklarheit über Prioritäten entsteht.</p>
<p>Der Check soll keine neue Roadmap erzeugen. Er macht sichtbar, welche konkrete Entscheidung fehlt, damit das Team sinnvoll weiterarbeiten kann.</p>''')
replaceList(problem, 'selfCheckSteps', [
    [title: 'Drei wichtige Ergebnisse benennen', body: '<p>Notiere die drei Ergebnisse, die aktuell wirklich Vorrang haben. Ergänze zu jedem Ergebnis einen Satz: Warum ist es gerade wichtiger als andere sinnvolle Themen?</p>'],
    [title: 'Kriterien für neue Arbeit klären', body: '<p>Halte fest, nach welchen Kriterien neue Arbeit bewertet wird – zum Beispiel Kundennutzen, Risiko, strategische Relevanz, Umsatz oder Effizienz. Prüfe, ob alle Beteiligten dieselben Kriterien verwenden.</p>'],
    [title: 'Entscheidungsrechte sichtbar machen', body: '<p>Benenne, wer bei konkurrierenden Prioritäten entscheidet, wer neue Arbeit starten darf, wer Nein sagen kann und welche Entscheidungen bewusst oberhalb des Teams liegen.</p>'],
    [title: 'Start, Stopp und Transparenz verbinden', body: '<p>Lege fest, was gestoppt oder verschoben wird, wenn etwas Neues beginnt. Beschreibe außerdem, wie die Entscheidung für Teams und Stakeholder sichtbar und nachvollziehbar wird.</p>']
])

replaceTextList(problem, 'avoidActions', [
    'Noch keine neue Roadmap bauen.',
    'Noch kein neues Priorisierungsframework einführen.',
    'Noch kein zusätzliches Steering Committee aufsetzen.',
    'Noch nicht mehr Governance hinzufügen, bevor klar ist, welche Entscheidung fehlt.'
])

problem.setProperty('smallestNextStep', '''
<h3>Verändere zunächst einen Entscheidungsmechanismus</h3>
<p>Nimm nicht sofort das gesamte Portfolio auseinander. Wähle einen wiederkehrenden Konflikt – zum Beispiel: Neue Business-Anforderungen verdrängen regelmäßig bestehende Produktprioritäten.</p>
<p>Definiere für genau diesen Fall:</p>
<ul><li>Wer darf neue Arbeit einbringen?</li><li>Nach welchen Kriterien wird sie bewertet?</li><li>Wer entscheidet bei Konflikten?</li><li>Was wird dafür gestoppt oder verschoben?</li><li>Wie wird die Entscheidung transparent gemacht?</li></ul>
<p>Teste diesen Mechanismus. Beobachte, ob Teams schneller wissen, woran sie arbeiten sollen, weniger Eskalationen entstehen, Prioritäten stabiler bleiben und neue Zielkonflikte sichtbar werden.</p>
<p>Gute Governance zentralisiert nicht jede Entscheidung. Sie macht klar, welche Entscheidungen zentral getroffen werden müssen, welche Teams selbst treffen können, welcher Kontext nötig ist und wann eskaliert oder überprüft wird.</p>''')

problem.setProperty('whenToGetHelp', '''
<p>Wenn Strategie, Roadmap, Priorisierung, Entscheidungsrechte und Stakeholder-Erwartungen ineinandergreifen, ist eine einzelne Ursache oft schwer zu erkennen.</p>
<p>Dann lohnt es sich, zuerst zu klären: Was ist heute wirklich entschieden? Was ist nur angenommen? Welche Entscheidung fehlt? Wer besitzt das Mandat? Welcher Kontext fehlt? Und welche Governance erzeugt das aktuelle Verhalten?</p>
<p>Eine <strong>ChOS Clarity Session</strong> oder – bei einer konkreten festgefahrenen Prioritätsentscheidung – ein <strong>Decision Review</strong> kann helfen, diese Fragen strukturiert zu klären.</p>
<p>Nicht um eine Roadmap für dich zu bauen. Sondern um herauszufinden, welche Entscheidungen nötig sind, damit eine Roadmap überhaupt Orientierung geben kann.</p>''')

problem.setProperty('closingThought', '''
<p>Wenn dein Team nicht weiß, woran es als Nächstes arbeiten soll, ist die erste Frage nicht:</p>
<p><strong>„Wie bauen wir eine bessere Roadmap?“</strong></p>
<p>Sondern:</p>
<p><strong>„Welche Entscheidung fehlt unserem System gerade, damit Richtung entstehen kann?“</strong></p>''')

problem.setProperty('targetAudience', 'Product-, Digital- und Bereichsverantwortliche mit konkurrierenden Initiativen')
problem.setProperty('buyingSituation', 'unclear-priorities')
setReferences(problem, 'topics', ['richtung', 'entscheidungen', 'verantwortung'].collect { requireByProperty(topics, 'slug', it) })
setReferences(problem, 'relatedProblems', ['langsame-entscheidungen', 'leadership-bottleneck'].collect { requireByProperty(problems, 'slug', it) })
setReferences(problem, 'relatedInsights', [
    requireByProperty(insights, 'slug', 'annahmen-vor-einer-produktentscheidung-pruefen'),
    requireByProperty(insights, 'slug', 'warum-produktorganisationen-nicht-an-fehlenden-methoden-scheitern')
])
setReferences(problem, 'relatedTools', [requireByProperty(tools, 'slug', 'chos-selbstcheck')])
setReferences(problem, 'relatedOffers', [requireOffer(offers, 'chos-clarity-session'), requireOffer(offers, 'decision-review')])
setReferences(problem, 'relatedCases', [requireByProperty(cases, 'slug', 'prioritaetsentscheidung-beginnt-von-vorn')])

Node bottleneckProblem = requireByProperty(problems, 'slug', 'leadership-bottleneck')
Node slowDecisionProblem = requireByProperty(problems, 'slug', 'langsame-entscheidungen')
Node selfCheck = requireByProperty(tools, 'slug', 'chos-selbstcheck')
Node assumptionsInsight = requireByProperty(insights, 'slug', 'annahmen-vor-einer-produktentscheidung-pruefen')
Node methodsInsight = requireByProperty(insights, 'slug', 'warum-produktorganisationen-nicht-an-fehlenden-methoden-scheitern')
Node priorityCase = requireByProperty(cases, 'slug', 'prioritaetsentscheidung-beginnt-von-vorn')
assumptionsInsight.setProperty('sourcePage', website.getNode('/insights/annahmen-vor-einer-produktentscheidung-pruefen').getIdentifier())
methodsInsight.setProperty('sourcePage', website.getNode('/insights/warum-produktorganisationen-nicht-an-fehlenden-methoden-scheitern').getIdentifier())
priorityCase.setProperty('sourcePage', website.getNode('/praxisfaelle').getIdentifier())
appendReference(bottleneckProblem, 'relatedProblems', problem)
appendReference(slowDecisionProblem, 'relatedProblems', problem)
appendReference(selfCheck, 'relatedProblems', problem)
appendReference(assumptionsInsight, 'relatedProblems', problem)
appendReference(methodsInsight, 'relatedProblems', problem)
appendReference(priorityCase, 'relatedProblems', problem)

Node seo = problem.hasNode('seo') ? problem.getNode('seo') : problem.addNode('seo', 'mgnl:contentNode')
seo.setProperty('seoTitle', 'Unklare Prioritäten: Welche Entscheidung deinem Team fehlt | ChOS')
seo.setProperty('metaDescription', 'Wenn Teams nicht wissen, woran sie als Nächstes arbeiten sollen, fehlt nicht automatisch eine bessere Roadmap. Prüfe Richtung, Kriterien und Entscheidungsrechte.')
seo.setProperty('socialTitle', 'Wenn Teams nicht wissen, woran sie als Nächstes arbeiten sollen')
seo.setProperty('socialDescription', 'Warum das nicht automatisch ein Roadmap- oder Priorisierungsproblem ist – und welche Entscheidung möglicherweise fehlt.')

Node publishing = problem.hasNode('publishing') ? problem.getNode('publishing') : problem.addNode('publishing', 'mgnl:contentNode')
publishing.setProperty('contentStatus', 'published')
publishing.setProperty('contentObjective', 'diagnosis')
publishing.setProperty('priority', 30L)
publishing.setProperty('datePublished', calendar('2026-08-14'))
publishing.setProperty('dateModified', calendar('2026-08-14'))
publishing.setProperty('active', true)
problems.save()
tools.save()
insights.save()
cases.save()

Node problemsPage = website.getNode('/probleme')
Node page = problemsPage.hasNode(slug) ? problemsPage.getNode(slug) : problemsPage.addNode(slug, 'mgnl:page')
page.setProperty('mgnl:template', 'meine-website:pages/home')
page.setProperty('title', 'Unklare Prioritäten')
page.setProperty('navigationTitle', 'Unklare Prioritäten')
page.setProperty('hideInNavigation', true)
page.setProperty('windowTitle', 'Unklare Prioritäten: Welche Entscheidung deinem Team fehlt | ChOS')
page.setProperty('metaDescription', 'Wenn Teams nicht wissen, woran sie als Nächstes arbeiten sollen, fehlt nicht automatisch eine bessere Roadmap. Prüfe Richtung, Kriterien und Entscheidungsrechte.')
page.setProperty('datePublished', '2026-08-14')
page.setProperty('dateModified', '2026-08-14')
page.setProperty('problemReference', problem.getIdentifier())

Node main = page.hasNode('main') ? page.getNode('main') : page.addNode('main', 'mgnl:area')
Node detail = main.hasNode('problem-detail') ? main.getNode('problem-detail') : main.addNode('problem-detail', 'mgnl:component')
detail.setProperty('mgnl:template', 'meine-website:components/problemDetail')
detail.setProperty('problemReference', problem.getIdentifier())

replaceTextRecursively(website.getNode('/start'), 'Prüfe vier konkrete Führungs-, Entscheidungs- und Organisationssituationen.', 'Prüfe fünf konkrete Führungs-, Entscheidungs- und Organisationssituationen.')
website.save()
syncProblemNavigation(navigation, problemsPage)

def commands = CommandsManager.getInstance()
[
    [repository: 'problems', path: "/cleonhardt/${slug}"],
    [repository: 'problems', path: '/cleonhardt/leadership-bottleneck'],
    [repository: 'problems', path: '/cleonhardt/langsame-entscheidungen'],
    [repository: 'tools', path: '/cleonhardt/chos-selbstcheck'],
    [repository: 'insights', path: '/cleonhardt/annahmen-vor-einer-produktentscheidung-pruefen'],
    [repository: 'insights', path: '/cleonhardt/warum-produktorganisationen-nicht-an-fehlenden-methoden-scheitern'],
    [repository: 'cases', path: '/cleonhardt/prioritaetsentscheidung-beginnt-von-vorn'],
    [repository: 'website', path: "/probleme/${slug}"],
    [repository: 'website', path: '/start'],
    [repository: 'navigation', path: '/cleonhardt/Main']
].each { Map<String, String> target ->
    def parameters = new LinkedHashMap<String, Object>()
    parameters.put('repository', target.repository)
    parameters.put('path', target.path)
    parameters.put('recursive', true)
    def result = commands.executeCommand('default', 'publish', parameters)
    println "${target.repository}:${target.path} – ${result ? 'veröffentlicht' : 'Veröffentlichung nicht bestätigt'}"
}

println 'Problemsituation „Unklare Prioritäten“ strukturiert angelegt, mit der Website verdrahtet und veröffentlicht.'
