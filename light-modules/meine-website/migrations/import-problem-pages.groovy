import info.magnolia.context.MgnlContext
import javax.jcr.Node
import javax.jcr.Session
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar
import java.util.GregorianCalendar

/*
 * Idempotenter Import der vier ChOS-Problem-Pages aus Markdown.
 * Vorhandene gleichnamige Datensaetze werden aktualisiert, nicht dupliziert.
 */

Session problems = MgnlContext.getJCRSession('problems')
Session topics = MgnlContext.getJCRSession('topics')
Session insights = MgnlContext.getJCRSession('insights')
Session tools = MgnlContext.getJCRSession('tools')
Session offers = MgnlContext.getJCRSession('offers')
Session cases = MgnlContext.getJCRSession('cases')

Node ensureFolder(Session session, String name) {
    session.rootNode.hasNode(name) ? session.rootNode.getNode(name) : session.rootNode.addNode(name, 'mgnl:folder')
}

Node ensureContent(Node folder, String name) {
    folder.hasNode(name) ? folder.getNode(name) : folder.addNode(name, 'mgnl:content')
}

Node byPath(Session session, String path) {
    session.nodeExists(path) ? session.getNode(path) : null
}

void setReferences(Node node, String name, Collection<Node> targets) {
    List<Node> clean = targets.findAll { it != null }
    if (clean) node.setProperty(name, clean.collect { it.identifier } as String[])
    else if (node.hasProperty(name)) node.getProperty(name).remove()
}

void replaceList(Node parent, String name, List<Map> rows) {
    if (parent.hasNode(name)) parent.getNode(name).remove()
    if (!rows) return
    Node list = parent.addNode(name, 'mgnl:contentNode')
    rows.eachWithIndex { Map row, int index ->
        Node item = list.addNode(String.format('%02d', index), 'mgnl:contentNode')
        row.each { key, value -> if (value != null && value.toString().trim()) item.setProperty(key.toString(), value.toString()) }
    }
}

String inline(String value) {
    String out = (value ?: '').replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;')
    out = out.replaceAll(/\*\*(.+?)\*\*/, '<strong>$1</strong>')
    out.replaceAll(/`(.+?)`/, '<code>$1</code>')
}

String markdownToHtml(String markdown) {
    List<String> out = []
    List<String> paragraph = []
    String listType = null
    def flushParagraph = {
        if (paragraph) {
            out << '<p>' + paragraph.collect { inline(it.trim()) }.join(' ') + '</p>'
            paragraph.clear()
        }
    }
    def closeList = {
        if (listType) {
            out << "</${listType}>"
            listType = null
        }
    }
    (markdown ?: '').readLines().each { raw ->
        String line = raw.trim()
        if (!line) { flushParagraph(); closeList(); return }
        if (line.startsWith('### ')) { flushParagraph(); closeList(); out << '<h3>' + inline(line.substring(4)) + '</h3>'; return }
        if (line.startsWith('> ')) { flushParagraph(); closeList(); out << '<blockquote><p>' + inline(line.substring(2)) + '</p></blockquote>'; return }
        def bullet = (line =~ /^[-*] (.+)$/)
        def number = (line =~ /^\d+\. (.+)$/)
        if (bullet.matches()) {
            flushParagraph()
            if (listType != 'ul') { closeList(); listType = 'ul'; out << '<ul>' }
            out << '<li>' + inline(bullet.group(1)) + '</li>'
            return
        }
        if (number.matches()) {
            flushParagraph()
            if (listType != 'ol') { closeList(); listType = 'ol'; out << '<ol>' }
            out << '<li>' + inline(number.group(1)) + '</li>'
            return
        }
        if (line.startsWith('|')) {
            flushParagraph(); closeList()
            paragraph << line
            return
        }
        paragraph << line
    }
    flushParagraph(); closeList()
    out.join('\n')
}

Map parseMarkdown(File file) {
    List<String> lines = file.readLines('UTF-8')
    String h1 = lines.find { it.startsWith('# ') }?.substring(2)?.trim()
    List<Map> sections = []
    Map current = [heading: '_preamble', lines: []]
    lines.each { line ->
        if (line.startsWith('## ')) {
            sections << current
            current = [heading: line.substring(3).trim(), lines: []]
        } else if (!line.startsWith('# ')) current.lines << line
    }
    sections << current
    [title: h1, sections: sections.findAll { it.lines.join('').trim() }]
}

List<Map> childSections(Map section) {
    List<Map> result = []
    Map current = [title: section.heading, lines: []]
    section.lines.each { line ->
        if (line.startsWith('### ')) {
            if (current.lines.join('').trim()) result << current
            current = [title: line.substring(4).trim(), lines: []]
        } else current.lines << line
    }
    if (current.lines.join('').trim()) result << current
    result
}

List<Map> bullets(Map section) {
    section.lines.findAll { it.trim() ==~ /^[-*] .+/ }.collect { [text: it.trim().substring(2)] }
}

List<Map> questions(String markdown) {
    LinkedHashSet<String> found = []
    markdown.readLines().each { raw ->
        String line = raw.trim().replaceFirst(/^[-*]\s+/, '').replaceFirst(/^\d+\.\s+/, '')
        line = line.replace('**', '').replace('>', '').trim()
        if (line.endsWith('?') && line.size() > 12 && !line.startsWith('#')) found << line
    }
    found.take(12).collect { [text: it] }
}

Map<String, Map> config = [
    'leadership-bottleneck': [file: 'leadership-bottleneck.md', buying: 'leadership-bottleneck', audience: 'Führungskräfte mit einem oder mehreren Teams beziehungsweise einem größeren Verantwortungsbereich.', topics: ['entscheidungen','verantwortung','leadership'], insights: ['diagnose-vor-eingriff','unklare-rollen-sind-selten-das-eigentliche-problem','rollen-und-verantwortlichkeiten-in-produktorganisationen-klaeren'], tools: ['chos-selbstcheck'], offers: ['chos-clarity-session'], cases: ['neue-rollen-alte-entscheidungen']],
    'langsame-entscheidungen': [file: 'langsame-entscheidungen.md', buying: 'slow-decisions', audience: 'Führungskräfte und Verantwortliche in Organisationen, in denen wichtige Entscheidungen wiederholt festhängen.', topics: ['entscheidungen','verantwortung','zusammenarbeit'], insights: ['annahmen-vor-einer-produktentscheidung-pruefen','diagnose-vor-eingriff'], tools: ['chos-selbstcheck'], offers: ['decision-review'], cases: ['prioritaetsentscheidung-beginnt-von-vorn']],
    'ai-decision-rights': [file: 'ai-decision-rights.md', buying: 'ai-decision-rights', audience: 'Führungskräfte und Verantwortliche, die AI-Systeme oder Agents in organisatorische Entscheidungs- und Arbeitsprozesse integrieren.', topics: ['ai-organisation','entscheidungen','verantwortung'], insights: ['warum-ai-einfuehrung-ein-operating-model-thema-ist','decision-rights-zwischen-mensch-und-ai','schlechte-prozesse-nicht-nur-schneller-machen'], tools: ['chos-selbstcheck'], offers: ['chos-quick-diagnostic'], cases: []],
    'leadership-transition': [file: 'leadership-transition.md', buying: 'leadership-transition', audience: 'Führungskräfte, die intern oder extern eine neue Führungsrolle beziehungsweise einen neuen Verantwortungsbereich übernehmen.', topics: ['leadership','entscheidungen','verantwortung','lernen'], insights: ['diagnose-vor-eingriff','wann-ist-executive-sparring-sinnvoll'], tools: ['chos-selbstcheck'], offers: ['executive-sparring','chos-clarity-session'], cases: ['neue-rollen-alte-entscheidungen']]
]

Node folder = ensureFolder(problems, 'cleonhardt')
Map<String, Node> created = [:]

config.each { String slug, Map cfg ->
    File source = new File('/opt/magnolia/light-modules/meine-website/migrations/problem-pages/' + cfg.file)
    if (!source.isFile()) throw new IllegalStateException('Quelldatei fehlt: ' + source)
    Map parsed = parseMarkdown(source)
    List<Map> sections = parsed.sections
    Map lead = sections[0]
    String subheadline = lead.heading == '_preamble' ? '' : lead.heading
    String intro = markdownToHtml(lead.lines.join('\n'))
    Node node = ensureContent(folder, slug)
    node.setProperty('title', parsed.title as String)
    node.setProperty('slug', slug)
    node.setProperty('headline', parsed.title as String)
    if (subheadline) node.setProperty('subheadline', subheadline)
    node.setProperty('intro', intro ?: '<p></p>')
    node.setProperty('targetAudience', cfg.audience as String)
    node.setProperty('buyingSituation', cfg.buying as String)

    List<Map> mechanisms = []
    List<Map> selfSteps = []
    List<Map> avoid = []
    List<String> situationParts = []
    List<String> diagnosisParts = []
    String assumption = ''
    String selfTitle = ''
    String selfIntro = ''
    String nextStep = ''
    String help = ''
    String closing = ''
    List<Map> symptoms = []

    sections.drop(1).each { Map section ->
        String key = section.heading.toLowerCase()
        String body = markdownToHtml(section.lines.join('\n'))
        if (key.contains('warnsignal') || key.contains('bottleneck erkennst') || key.contains('symptom')) symptoms.addAll(bullets(section))
        else if (key.contains('naheliegende erklärung') || key.startsWith('„mein team')) assumption = body
        else if (key.startsWith('was könnte') || key.startsWith('was sollte vor') || key.startsWith('was solltest du am anfang')) mechanisms.addAll(childSections(section).collect { [title: it.title, body: markdownToHtml(it.lines.join('\n'))] })
        else if (key.contains('check') || key.contains('beobachte eine woche') || key.contains('observation map')) {
            selfTitle = section.heading
            selfIntro = body
            selfSteps.addAll(childSections(section).collect { [title: it.title, body: markdownToHtml(it.lines.join('\n'))] })
        }
        else if (key.contains('noch nichts') || key.contains('noch kein neues') || key.startsWith('was du möglicherweise noch nicht')) avoid.addAll(bullets(section) ?: [[text: section.heading]])
        else if (key.startsWith('verändere') || key.startsWith('starte mit') || key.startsWith('wann solltest du handeln')) nextStep = body
        else if (key.startsWith('wann eine externe') || key.startsWith('wann unterstützung')) help = body
        else if (key.startsWith('ein gedanke')) closing = body
        else if (key.contains('diagnose') || key.contains('drei arten') || key.contains('unangenehm') || key.contains('governance')) diagnosisParts << '<h3>' + inline(section.heading) + '</h3>' + body
        else situationParts << '<h3>' + inline(section.heading) + '</h3>' + body
    }

    node.setProperty('situation', situationParts.join('\n') ?: intro)
    if (assumption) node.setProperty('commonAssumption', assumption)
    node.setProperty('diagnosisIntro', diagnosisParts.join('\n') ?: '<p>Die Beobachtung ist noch keine Diagnose. Mehrere Mechanismen können gleichzeitig wirken.</p>')
    replaceList(node, 'symptoms', symptoms)
    replaceList(node, 'hypotheses', mechanisms)
    replaceList(node, 'diagnosticQuestions', questions(source.text))
    if (selfTitle) node.setProperty('selfCheckTitle', selfTitle)
    if (selfIntro) node.setProperty('selfCheckIntro', selfIntro)
    replaceList(node, 'selfCheckSteps', selfSteps ?: (selfTitle ? [[title: selfTitle, body: selfIntro]] : []))
    replaceList(node, 'avoidActions', avoid)
    node.setProperty('smallestNextStep', nextStep ?: '<p>Wähle einen konkreten, begrenzten Fall. Formuliere eine plausible Hypothese, verändere so wenig wie möglich und beobachte die Wirkung.</p>')
    if (help) node.setProperty('whenToGetHelp', help)
    if (closing) node.setProperty('closingThought', closing)

    setReferences(node, 'topics', cfg.topics.collect { byPath(topics, '/chos/' + it) })
    setReferences(node, 'relatedInsights', cfg.insights.collect { byPath(insights, '/cleonhardt/' + it) })
    setReferences(node, 'relatedTools', cfg.tools.collect { byPath(tools, '/cleonhardt/' + it) })
    setReferences(node, 'relatedOffers', cfg.offers.collect { byPath(offers, '/cleonhardt/' + it) })
    setReferences(node, 'relatedCases', cfg.cases.collect { byPath(cases, '/cleonhardt/' + it) })

    if (node.hasNode('seo')) node.getNode('seo').remove()
    Node seo = node.addNode('seo', 'mgnl:contentNode')
    seo.setProperty('seoTitle', parsed.title as String)
    seo.setProperty('metaDescription', (inline(parsed.title as String).replaceAll(/<[^>]+>/, '') + ' – Problem verstehen, selbst prüfen und den kleinsten sinnvollen nächsten Schritt finden.').take(160))
    seo.setProperty('socialTitle', parsed.title as String)
    seo.setProperty('socialDescription', (cfg.audience as String).take(200))

    if (node.hasNode('publishing')) node.getNode('publishing').remove()
    Node publishing = node.addNode('publishing', 'mgnl:contentNode')
    publishing.setProperty('contentStatus', 'published')
    publishing.setProperty('contentObjective', 'problem-recognition')
    publishing.setProperty('priority', 10L)
    publishing.setProperty('active', true)
    publishing.setProperty('datePublished', GregorianCalendar.from(LocalDate.now().atStartOfDay(ZoneId.of('Europe/Berlin'))))
    created[slug] = node
}

setReferences(created['leadership-bottleneck'], 'relatedProblems', [created['langsame-entscheidungen'], created['leadership-transition']])
setReferences(created['langsame-entscheidungen'], 'relatedProblems', [created['leadership-bottleneck'], created['ai-decision-rights']])
setReferences(created['ai-decision-rights'], 'relatedProblems', [created['langsame-entscheidungen']])
setReferences(created['leadership-transition'], 'relatedProblems', [created['leadership-bottleneck'], created['langsame-entscheidungen']])

problems.save()
println 'PROBLEMS_IMPORTED=' + created.size()
created.each { slug, node -> println 'PROBLEM=' + node.path + '|title=' + node.getProperty('title').string + '|hypotheses=' + (node.hasNode('hypotheses') ? node.getNode('hypotheses').nodes.size : 0) + '|questions=' + (node.hasNode('diagnosticQuestions') ? node.getNode('diagnosticQuestions').nodes.size : 0) }
