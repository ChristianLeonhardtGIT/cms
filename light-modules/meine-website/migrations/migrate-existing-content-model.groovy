import info.magnolia.context.MgnlContext
import javax.jcr.Node
import javax.jcr.Session
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar
import java.util.GregorianCalendar

/*
 * Migriert bereits veröffentlichte ChOS-Fachinhalte in die strukturierten
 * Content-Type-Workspaces. Website-Seiten und Leistungen bleiben unverändert.
 * Das Skript ist wiederholbar und entfernt keine fremden Datensätze.
 */

Session website = MgnlContext.getJCRSession('website')
Session offers = MgnlContext.getJCRSession('offers')
Session insights = MgnlContext.getJCRSession('insights')
Session tools = MgnlContext.getJCRSession('tools')
Session cases = MgnlContext.getJCRSession('cases')
Session topics = MgnlContext.getJCRSession('topics')

Node ensureFolder(Session session, String name) {
    session.rootNode.hasNode(name) ? session.rootNode.getNode(name) : session.rootNode.addNode(name, 'mgnl:folder')
}

Node ensureContent(Node folder, String name) {
    folder.hasNode(name) ? folder.getNode(name) : folder.addNode(name, 'mgnl:content')
}

void setString(Node node, String name, Object value) {
    if (value != null) node.setProperty(name, value.toString())
}

void setReferences(Node node, String name, Collection<Node> targets) {
    List<Node> cleanTargets = targets.findAll { it != null }
    if (cleanTargets) {
        node.setProperty(name, cleanTargets.collect { it.identifier } as String[])
    } else if (node.hasProperty(name)) {
        node.getProperty(name).remove()
    }
}

void replaceList(Node parent, String name, List<Map> rows) {
    if (parent.hasNode(name)) parent.getNode(name).remove()
    Node list = parent.addNode(name, 'mgnl:contentNode')
    rows.eachWithIndex { Map row, int index ->
        Node item = list.addNode(String.format('%02d', index), 'mgnl:contentNode')
        row.each { key, value -> if (value != null) item.setProperty(key.toString(), value.toString()) }
    }
}

Calendar calendar(String value) {
    if (!value) return null
    LocalDate date = LocalDate.parse(value.take(10))
    GregorianCalendar.from(date.atStartOfDay(ZoneId.of('Europe/Berlin')))
}

String pageProperty(Node page, String name, String fallback = '') {
    page.hasProperty(name) ? page.getProperty(name).string : fallback
}

String stripHtml(String html) {
    (html ?: '').replaceAll(/<[^>]+>/, ' ').replace('&nbsp;', ' ').replaceAll(/\s+/, ' ').trim()
}

List<Node> orderedComponents(Node page) {
    if (!page.hasNode('main')) return []
    List<Node> result = []
    page.getNode('main').nodes.each { result << it }
    result
}

String articleBody(Node page) {
    List<String> sections = []
    orderedComponents(page).each { Node component ->
        String template = pageProperty(component, 'mgnl:template')
        if (template.endsWith('/hero') || template.endsWith('/callToAction') || template.endsWith('/faq')) return
        String heading = pageProperty(component, 'heading')
        String body = pageProperty(component, 'text', pageProperty(component, 'description'))
        if (body) sections << ((heading ? "<h2>${heading}</h2>" : '') + body)
    }
    sections.join('\n') ?: '<p>Der bestehende Artikel ist über die verknüpfte Website-Seite verfügbar.</p>'
}

String keyTakeaway(Node page) {
    Node match = orderedComponents(page).find { pageProperty(it, 'heading') == 'Kurzantwort' }
    match ? stripHtml(pageProperty(match, 'text', pageProperty(match, 'description'))) : ''
}

void addSeo(Node content, Node page) {
    Node node = content.hasNode('seo') ? content.getNode('seo') : content.addNode('seo', 'mgnl:contentNode')
    setString(node, 'seoTitle', pageProperty(page, 'windowTitle', pageProperty(page, 'title')))
    setString(node, 'metaDescription', pageProperty(page, 'metaDescription'))
    setString(node, 'socialTitle', pageProperty(page, 'title'))
    setString(node, 'socialDescription', pageProperty(page, 'metaDescription'))
}

void addPublishing(Node content, Node page, String objective, long priority) {
    Node node = content.hasNode('publishing') ? content.getNode('publishing') : content.addNode('publishing', 'mgnl:contentNode')
    node.setProperty('contentStatus', 'published')
    node.setProperty('contentObjective', objective)
    node.setProperty('priority', priority)
    node.setProperty('active', true)
    String published = pageProperty(page, 'datePublished')
    String modified = pageProperty(page, 'dateModified')
    if (published) node.setProperty('datePublished', calendar(published))
    if (modified) node.setProperty('dateModified', calendar(modified))
}

Node offer(Session offers, String name) {
    String path = '/cleonhardt/' + name
    offers.nodeExists(path) ? offers.getNode(path) : null
}

Map<String, Map> topicDefinitions = [
    'richtung': [title: 'Richtung', description: 'Strategie, Ziele, Prioritäten und gemeinsame Wirkung.', sortOrder: 10L],
    'entscheidungen': [title: 'Entscheidungen', description: 'Entscheidungsrechte, Qualität, Geschwindigkeit und Unsicherheit.', sortOrder: 20L],
    'verantwortung': [title: 'Verantwortung', description: 'Mandate, Ownership und die Verbindung von Entscheidung und Konsequenz.', sortOrder: 30L],
    'zusammenarbeit': [title: 'Zusammenarbeit', description: 'Schnittstellen, Abhängigkeiten und gemeinsames Arbeiten am Ergebnis.', sortOrder: 40L],
    'lernen': [title: 'Lernen', description: 'Wirkung beobachten, Hypothesen prüfen und gezielt anpassen.', sortOrder: 50L],
    'leadership': [title: 'Leadership', description: 'Führung in Produkt- und Veränderungssituationen.', sortOrder: 60L],
    'ai-organisation': [title: 'AI & Organisation', description: 'Human-AI-Zusammenarbeit, Agents, Governance und Operating Models.', sortOrder: 70L]
]

Node topicFolder = ensureFolder(topics, 'chos')
Map<String, Node> topicNodes = [:]
topicDefinitions.each { String slug, Map data ->
    Node node = ensureContent(topicFolder, slug)
    node.setProperty('title', data.title as String)
    node.setProperty('slug', slug)
    node.setProperty('description', data.description as String)
    node.setProperty('sortOrder', data.sortOrder as long)
    node.setProperty('active', true)
    topicNodes[slug] = node
}
topics.save()

Map<String, List<String>> insightTopics = [
    'warum-produktorganisationen-nicht-an-fehlenden-methoden-scheitern': ['richtung', 'entscheidungen', 'verantwortung'],
    'diagnose-vor-eingriff': ['lernen', 'entscheidungen'],
    'unklare-rollen-sind-selten-das-eigentliche-problem': ['verantwortung', 'leadership'],
    'rollen-und-verantwortlichkeiten-in-produktorganisationen-klaeren': ['verantwortung', 'entscheidungen'],
    'wann-braucht-eine-produktorganisation-ein-operating-model': ['richtung', 'zusammenarbeit', 'verantwortung'],
    'annahmen-vor-einer-produktentscheidung-pruefen': ['entscheidungen', 'lernen'],
    'organisationsdiagnose-statt-standardberatung': ['lernen', 'zusammenarbeit'],
    'product-organisation-diagnostic-ablauf-und-ergebnis': ['richtung', 'verantwortung', 'zusammenarbeit', 'lernen'],
    'wann-ist-executive-sparring-sinnvoll': ['leadership', 'entscheidungen'],
    'warum-ai-einfuehrung-ein-operating-model-thema-ist': ['ai-organisation', 'verantwortung', 'zusammenarbeit'],
    'decision-rights-zwischen-mensch-und-ai': ['ai-organisation', 'entscheidungen', 'verantwortung'],
    'schlechte-prozesse-nicht-nur-schneller-machen': ['ai-organisation', 'zusammenarbeit', 'lernen']
]

Map<String, List<String>> insightOffers = [
    'diagnose-vor-eingriff': ['chos-clarity-session'],
    'annahmen-vor-einer-produktentscheidung-pruefen': ['decision-review'],
    'organisationsdiagnose-statt-standardberatung': ['chos-clarity-session', 'product-organisation-diagnostic'],
    'product-organisation-diagnostic-ablauf-und-ergebnis': ['product-organisation-diagnostic'],
    'wann-ist-executive-sparring-sinnvoll': ['executive-sparring'],
    'warum-ai-einfuehrung-ein-operating-model-thema-ist': ['chos-quick-diagnostic'],
    'decision-rights-zwischen-mensch-und-ai': ['chos-quick-diagnostic'],
    'schlechte-prozesse-nicht-nur-schneller-machen': ['workshops']
]

Node insightFolder = ensureFolder(insights, 'cleonhardt')
List<Node> migratedInsights = []
if (!website.nodeExists('/insights')) throw new IllegalStateException('/insights fehlt')
website.getNode('/insights').nodes.each { Node page ->
    if (!page.isNodeType('mgnl:page')) return
    String slug = page.name
    Node content = ensureContent(insightFolder, slug)
    String title = pageProperty(page, 'title', slug)
    String teaser = pageProperty(page, 'metaDescription', title)
    content.setProperty('title', title)
    content.setProperty('slug', slug)
    content.setProperty('teaser', teaser)
    content.setProperty('body', articleBody(page))
    String takeaway = keyTakeaway(page)
    if (takeaway) content.setProperty('keyTakeaway', takeaway)
    content.setProperty('sourcePage', page.identifier)
    setReferences(content, 'topics', (insightTopics[slug] ?: []).collect { topicNodes[it] })
    setReferences(content, 'relatedOffers', (insightOffers[slug] ?: ['chos-clarity-session']).collect { offer(offers, it) })
    addSeo(content, page)
    addPublishing(content, page, 'education', 50L)
    migratedInsights << content
}
insights.save()

Node toolFolder = ensureFolder(tools, 'cleonhardt')
Node checkPage = website.getNode('/chos-selbstcheck')
Node selfCheck = ensureContent(toolFolder, 'chos-selbstcheck')
selfCheck.setProperty('title', 'ChOS Selbstcheck')
selfCheck.setProperty('slug', 'chos-selbstcheck')
selfCheck.setProperty('purpose', 'Erste Orientierung zu fünf Spannungsfeldern einer Produktorganisation – ohne Registrierung und ohne Übertragung der Antworten.')
selfCheck.setProperty('intro', '<p>20 Fragen machen wiederkehrende Signale in Richtung, Verantwortung, Entscheidungen, Zusammenarbeit und Führung sichtbar. Das Ergebnis ist eine Orientierung, keine Ferndiagnose.</p>')
selfCheck.setProperty('instructions', '<p>Bewerten Sie jedes Signal aus Sicht Ihrer aktuellen Organisation. Wählen Sie anschließend ein konkretes Beispiel aus der stärksten Dimension, trennen Sie Beobachtung und Erklärung und formulieren Sie mindestens eine alternative Hypothese.</p>')
selfCheck.setProperty('expectedOutcome', '<p>Ein priorisiertes Spannungsfeld und ein konkretes Beispiel, das anschließend genauer diagnostiziert werden kann.</p>')
selfCheck.setProperty('sourcePage', checkPage.identifier)

Map<String, List<String>> dimensions = [
    'Richtung und Strategie': ['Prioritäten wechseln, ohne dass sich Strategie oder Evidenz erkennbar verändert haben.', 'Teams interpretieren den gewünschten Kundennutzen unterschiedlich.', 'Erfolg wird vor allem über Lieferung statt über Wirkung beschrieben.', 'Kurzfristige Anforderungen verdrängen regelmäßig vereinbarte Produktziele.'],
    'Rollen und Verantwortung': ['Verantwortung ist benannt, das nötige Entscheidungsmandat fehlt jedoch.', 'Mehrere Rollen fühlen sich für dieselbe Entscheidung letztverantwortlich.', 'Entscheidungen werden außerhalb der formal zuständigen Rolle erneut geöffnet.', 'Schnittstellen werden vor allem durch persönliche Abstimmung statt klare Erwartungen stabilisiert.'],
    'Entscheidungen und Priorisierung': ['Wichtige Prioritätsentscheidungen drehen wiederholt neue Schleifen.', 'Es ist unklar, welche Information für eine Entscheidung tatsächlich ausreicht.', 'Eskalationen ersetzen häufig eine Entscheidung am vorgesehenen Ort.', 'Gegenläufige Ziele werden in Gremien vertagt statt bewusst aufgelöst.'],
    'Zusammenarbeit und Abhängigkeiten': ['Teams optimieren lokal, obwohl dadurch andere Teile des Wertstroms langsamer werden.', 'Abhängigkeiten bestimmen die Planung stärker als Kundenergebnisse.', 'Zusammenarbeit funktioniert vor allem über einzelne Schlüsselpersonen.', 'Probleme werden zwischen Bereichen weitergereicht, statt gemeinsam am System gelöst.'],
    'Führung und Kommunikation': ['Führung fordert Eigenverantwortung, greift aber regelmäßig in Detailentscheidungen ein.', 'Unterschiedliche Führungssignale erzeugen widersprüchliche Prioritäten.', 'Probleme werden früh sichtbar, aber erst spät offen angesprochen.', 'Veränderungen werden als Maßnahmen verfolgt, ohne die gewünschte Verhaltensänderung zu prüfen.']
]
List<Map> checkQuestions = []
dimensions.each { dimension, questions -> questions.each { question -> checkQuestions << [text: question, guidance: dimension] } }
replaceList(selfCheck, 'questions', checkQuestions)
setReferences(selfCheck, 'topics', ['richtung', 'entscheidungen', 'verantwortung', 'zusammenarbeit', 'leadership'].collect { topicNodes[it] })
setReferences(selfCheck, 'relatedInsights', migratedInsights.findAll { ['diagnose-vor-eingriff', 'unklare-rollen-sind-selten-das-eigentliche-problem'].contains(it.getProperty('slug').string) })
setReferences(selfCheck, 'relatedOffers', [offer(offers, 'chos-clarity-session')])
addSeo(selfCheck, checkPage)
addPublishing(selfCheck, checkPage, 'diagnosis', 10L)
tools.save()

Map<String, Map> caseDefinitions = [
    'prioritaetsentscheidung-beginnt-von-vorn': [
        title: 'Eine Prioritätsentscheidung beginnt immer wieder von vorn', teaser: 'Ein Fallmuster über wieder geöffnete Prioritätsentscheidungen und fehlende Entscheidungsklarheit.',
        context: 'Produkt, Technologie und Business stimmen einer Priorität im Meeting zu. Wenige Tage später wird dieselbe Entscheidung in einem anderen Kreis erneut geöffnet; Roadmaps verlieren dadurch Verbindlichkeit.',
        diagnosis: 'Die Beteiligten arbeiten mit unterschiedlichen Erfolgskriterien. Zudem ist unklar, wer eine Entscheidung schließen darf und welche neue Evidenz sie später wieder öffnen kann.',
        intervention: 'Für einen realen Entscheidungsweg werden Entscheidungsverantwortung, notwendiger Input, gemeinsame Kriterien und zulässige Gründe für ein Wiederöffnen explizit gemacht und begrenzt erprobt.',
        observedEffect: 'Diskussionen unterscheiden klarer zwischen neuer Evidenz und einem wiederkehrenden Einzelinteresse. Eskalationen werden konkreter und die nächste Entscheidung lässt sich bewusster vorbereiten.',
        learnings: 'Bevor ein neues Priorisierungsverfahren eingeführt wird, sollte die tatsächliche Entscheidungsarchitektur geklärt werden.', topics: ['richtung', 'entscheidungen'], offerNames: ['decision-review']
    ],
    'neue-rollen-alte-entscheidungen': [
        title: 'Neue Rollen, alte Entscheidungen', teaser: 'Ein Fallmuster über formal verteilte Verantwortung und unveränderte Führungsroutinen.',
        context: 'Eine Produktorganisation führt neue Rollen ein. Auf dem Papier ist Verantwortung verteilt, im Alltag warten Teams jedoch weiter auf Freigaben und Führung greift regelmäßig in Detailentscheidungen ein.',
        diagnosis: 'Verantwortung wurde benannt, aber Mandate, Informationszugang und Konsequenzen blieben unverändert. Das alte Führungsverhalten stabilisiert deshalb weiterhin das alte System.',
        intervention: 'An konkreten Entscheidungssituationen werden Mandat, erwartetes Ergebnis, Beteiligung und Eskalationsgrenzen gemeinsam geklärt. Führung vereinbart sichtbar, wann sie unterstützt und wann sie nicht eingreift.',
        observedEffect: 'Unklarheit wird an konkreten Entscheidungen besprechbar. Teams und Führung erkennen früher, ob Information, Kompetenz, Mandat oder ein echter Zielkonflikt fehlt.',
        learnings: 'Eine neue Rolle verändert erst dann etwas, wenn sich dadurch beobachtbar Entscheidungen und Führungsroutinen verändern.', topics: ['verantwortung', 'entscheidungen', 'leadership'], offerNames: ['chos-clarity-session']
    ],
    'lokale-geschwindigkeit-systemweite-reibung': [
        title: 'Lokale Geschwindigkeit erzeugt systemweite Reibung', teaser: 'Ein Fallmuster über lokale Optimierung, widersprüchliche Ziele und Reibung im Wertstrom.',
        context: 'Einzelne Teams liefern schnell, dennoch entstehen Wartezeiten, Übergaben und wiederkehrende Konflikte entlang des gesamten Wertstroms. Jede Einheit optimiert nachvollziehbar ihre eigenen Ziele.',
        diagnosis: 'Lokale Ziele, Finanzierungslogik und Zuständigkeiten belohnen getrennte Optimierung. Die Abhängigkeiten sind daher nicht nur ein Kommunikationsproblem, sondern Ergebnis des Organisationsdesigns.',
        intervention: 'Ein relevanter Wertstrom wird gemeinsam sichtbar gemacht. Beteiligte identifizieren die teuersten Übergaben, widersprüchliche Ziele und Entscheidungen, die heute keinem eindeutigen Ort gehören.',
        observedEffect: 'Die Diskussion verschiebt sich von gegenseitigen Vorwürfen zu gemeinsam beeinflussbaren Mechanismen. Maßnahmen können nach ihrem Beitrag zum Gesamtergebnis priorisiert werden.',
        learnings: 'Mehr Abstimmung kompensiert ein widersprüchliches System nur vorübergehend; Ziele und Entscheidungsorte müssen zusammenpassen.', topics: ['richtung', 'zusammenarbeit', 'lernen'], offerNames: ['product-organisation-diagnostic']
    ]
]

Node casesPage = website.getNode('/praxisfaelle')
Node caseFolder = ensureFolder(cases, 'cleonhardt')
caseDefinitions.each { String slug, Map data ->
    Node content = ensureContent(caseFolder, slug)
    ['title', 'teaser', 'context', 'diagnosis', 'intervention', 'observedEffect', 'learnings'].each { key -> setString(content, key, data[key]) }
    content.setProperty('slug', slug)
    content.setProperty('anonymized', true)
    content.setProperty('sourcePage', casesPage.identifier)
    setReferences(content, 'topics', data.topics.collect { topicNodes[it] })
    setReferences(content, 'relatedTools', [selfCheck])
    setReferences(content, 'relatedOffers', data.offerNames.collect { offer(offers, it) })
    addSeo(content, casesPage)
    addPublishing(content, casesPage, 'education', 40L)
}
cases.save()

println "Migration abgeschlossen: ${topicDefinitions.size()} Themen, ${migratedInsights.size()} Insights, 1 Tool und ${caseDefinitions.size()} Cases."
println 'Problemsituationen bleiben in diesem Schritt leer; Leistungen wurden ausschließlich referenziert.'
