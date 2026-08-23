import info.magnolia.commands.CommandsManager
import info.magnolia.context.MgnlContext

import javax.jcr.Node
import javax.jcr.Property
import javax.jcr.PropertyType
import javax.jcr.Session
import javax.jcr.Value

def website = MgnlContext.getJCRSession('website')

Node findByProperty(Node root, String propertyName, String propertyValue) {
    if (root.hasProperty(propertyName) && root.getProperty(propertyName).string == propertyValue) return root
    for (Node child : root.nodes) {
        Node match = findByProperty(child, propertyName, propertyValue)
        if (match != null) return match
    }
    null
}

Node findByTemplate(Node root, String templateSuffix) {
    if (root.hasProperty('mgnl:template') && root.getProperty('mgnl:template').string.endsWith(templateSuffix)) return root
    for (Node child : root.nodes) {
        Node match = findByTemplate(child, templateSuffix)
        if (match != null) return match
    }
    null
}

Node start = website.getNode('/start')
Node main = start.getNode('main')
Node hero = findByTemplate(main, 'components/hero')
if (hero == null) throw new IllegalStateException('Hero der Startseite fehlt.')
if (!hero.hasNode('ctaChooser') || !hero.getNode('ctaChooser').hasNode('ctaLink')) {
    throw new IllegalStateException('Hero-CTA der Startseite fehlt.')
}
hero.getNode('ctaChooser').setProperty('ctaText', 'Typische Situationen ansehen')
hero.getNode('ctaChooser').getNode('ctaLink').setProperty('internalLink', '/probleme')

Node process = findByProperty(main, 'heading', 'Diagnose -> Design -> Veränderung')
if (process == null) process = findByProperty(main, 'heading', 'Diagnose → Design → Veränderung')
if (process == null) throw new IllegalStateException('ChOS-Prozessblock fehlt.')
process.setProperty('heading', 'Diagnose → Design → Veränderung')

String orientationHtml = '''
<div class="chos-orientation">
  <p class="lead">Wähle den Einstieg, der zu deiner aktuellen Situation passt. Du musst noch nicht wissen, welches Angebot du brauchst.</p>
  <div class="mvp-grid mvp-grid--3">
    <a class="mvp-panel mvp-panel--link" href="/probleme" aria-label="Typische Situationen ansehen"><p class="mvp-meta">Problem wiedererkennen</p><h3>Typische Situationen</h3><p>Prüfe fünf konkrete Führungs-, Entscheidungs- und Organisationssituationen.</p><span class="mvp-panel__link"><span>Situationen ansehen</span><span aria-hidden="true">→</span></span></a>
    <a class="mvp-panel mvp-panel--link" href="/chos-selbstcheck" aria-label="ChOS Selbstcheck starten"><p class="mvp-meta">8–10 Minuten · lokal</p><h3>ChOS Selbstcheck</h3><p>Ordne erste Signale entlang der fünf ChOS-Perspektiven ein.</p><span class="mvp-panel__link"><span>Selbstcheck starten</span><span aria-hidden="true">→</span></span></a>
    <a class="mvp-panel mvp-panel--link" href="/insights" aria-label="Insights lesen"><p class="mvp-meta">Fachlich vertiefen</p><h3>Insights</h3><p>Lies weiter zu Entscheidungen, Verantwortung, Leadership und AI.</p><span class="mvp-panel__link"><span>Insights lesen</span><span aria-hidden="true">→</span></span></a>
  </div>
</div>'''

Node orientation = findByProperty(main, 'heading', 'Wie möchtest du starten?')
if (orientation == null) {
    String nodeName = main.hasNode('chos-orientation') ? "chos-orientation-${System.currentTimeMillis()}" : 'chos-orientation'
    orientation = main.addNode(nodeName, 'mgnl:component')
    orientation.setProperty('mgnl:template', 'meine-website:components/text')
}
orientation.setProperty('heading', 'Wie möchtest du starten?')
orientation.setProperty('text', orientationHtml)
orientation.setProperty('showBackground', false)

Node situation = findByProperty(main, 'heading', 'Mehr Technik löst keine unklaren Strukturen.')
Node chos = findByProperty(main, 'heading', 'Erst verstehen. Dann wirksam verändern - ChOS.')
Node entryOffers = findByProperty(main, 'heading', 'Mit einer konkreten Situation starten.')
Node offerCatalog = findByTemplate(main, 'components/offerCatalog')
Node profile = findByProperty(main, 'heading', 'Product Leadership, Organisation und Transformation')
Node confidentiality = findByProperty(main, 'heading', 'Vertraulich von Anfang an.')
Node contact = findByProperty(main, 'title', 'Welche Situation möchten Sie klären?')
if (contact == null) contact = findByProperty(main, 'title', 'Welche Situation möchtest du klären?')

def desired = [hero, situation, chos, process, orientation, entryOffers, offerCatalog, profile, confidentiality, contact]
if (desired.any { it == null }) throw new IllegalStateException('Mindestens ein Startseitenblock für die neue Reihenfolge fehlt.')
desired.each { Node item -> main.orderBefore(item.name, null) }
def desiredNames = desired.collect { it.name } as Set
def leftovers = []
for (Node child : main.nodes) {
    if (!desiredNames.contains(child.name)) leftovers << child
}
leftovers.each { Node item -> main.orderBefore(item.name, null) }

def replacements = new LinkedHashMap<String, String>()
replacements.put('Ihre Qualität zeigt sich darin', 'Die Qualität einer Diagnose zeigt sich darin')
replacements.put('wenn Sie ein anschließend übermitteltes Angebot ausdrücklich annehmen', 'wenn du ein anschließend übermitteltes Angebot ausdrücklich annimmst')
replacements.put('Wenn Sie das Kontaktformular absenden', 'Wenn du das Kontaktformular absendest')
replacements.put('Wenn Sie uns per E-Mail oder Telefon kontaktieren', 'Wenn du uns per E-Mail oder Telefon kontaktierst')
replacements.put('dass Sie diese Datenschutzerklärung zur Kenntnis genommen haben', 'dass du diese Datenschutzerklärung zur Kenntnis genommen hast')
replacements.put('dass Sie das passende Produkt bereits kennen', 'dass du das passende Produkt bereits kennst')
replacements.put('die Sie nicht delegieren können', 'die du nicht delegieren kannst')
replacements.put('bevor Sie die nächste Maßnahme starten', 'bevor du die nächste Maßnahme startest')
replacements.put('bevor Sie Rollen und Strukturen verändern', 'bevor du Rollen und Strukturen veränderst')
replacements.put('bevor Sie sie neu zeichnen', 'bevor du sie neu zeichnest')
replacements.put('bevor Sie eine Rolle, einen Prozess oder eine Struktur verändern', 'bevor du eine Rolle, einen Prozess oder eine Struktur veränderst')
replacements.put('Sie sind beim Format noch unsicher?', 'Du bist beim Format noch unsicher?')
replacements.put('Welches Muster erkennen Sie wieder?', 'Welches Muster erkennst du wieder?')
replacements.put('Woran Sie ein Systemproblem erkennen', 'Woran du ein Systemproblem erkennst')
replacements.put('Was Sie mit dem Ergebnis tun können', 'Was du mit dem Ergebnis tun kannst')
replacements.put('Möchten Sie', 'Möchtest du')
replacements.put('möchten Sie', 'möchtest du')
replacements.put('können Sie', 'kannst du')
replacements.put('Können Sie', 'Kannst du')
replacements.put('haben Sie', 'hast du')
replacements.put('Haben Sie', 'Hast du')
replacements.put('erreichen Sie', 'erreichst du')
replacements.put('Erreichen Sie', 'Erreichst du')
replacements.put('erkennen Sie', 'erkennst du')
replacements.put('Erkennen Sie', 'Erkennst du')
replacements.put('für Sie', 'für dich')
replacements.put('bei Ihnen', 'bei dir')
replacements.put('mit Ihnen', 'mit dir')
replacements.put('von Ihnen', 'von dir')
replacements.put('zu Ihnen', 'zu dir')

[
    'Lassen Sie':'Lass', 'Wählen Sie':'Wähle', 'Beschreiben Sie':'Beschreibe',
    'Bewerten Sie':'Bewerte', 'Prüfen Sie':'Prüfe', 'Trennen Sie':'Trenne',
    'Formulieren Sie':'Formuliere', 'Vergleichen Sie':'Vergleiche', 'Suchen Sie':'Suche',
    'Nehmen Sie':'Nimm', 'Benennen Sie':'Benenne', 'Beobachten Sie':'Beobachte',
    'Vereinbaren Sie':'Vereinbare', 'Markieren Sie':'Markiere', 'Entwickeln Sie':'Entwickle',
    'Fragen Sie':'Frage', 'Definieren Sie':'Definiere', 'Starten Sie':'Starte',
    'Nutzen Sie':'Nutze', 'Achten Sie':'Achte', 'Beginnen Sie':'Beginne',
    'Ergänzen Sie':'Ergänze', 'Schreiben Sie':'Schreibe', 'Machen Sie':'Mach',
    'Unterscheiden Sie':'Unterscheide', 'Betrachten Sie':'Betrachte', 'Laden Sie':'Lade',
    'Versuchen Sie':'Versuche', 'Übermitteln Sie':'Übermittle'
].each { source, target -> replacements.put(source, target) }

[
    'Sie erhalten':'du erhältst', 'Sie schildern':'du schilderst', 'Sie nennen':'du nennst',
    'Sie tragen':'du trägst', 'Sie wollen':'du willst', 'Sie möchten':'du möchtest',
    'Sie können':'du kannst', 'Sie müssen':'du musst', 'Sie finden':'du findest',
    'Sie wählen':'du wählst', 'Sie beschreiben':'du beschreibst', 'Sie prüfen':'du prüfst',
    'Sie starten':'du startest', 'Sie nutzen':'du nutzt', 'Sie vergleichen':'du vergleichst',
    'Sie suchen':'du suchst', 'Sie übermitteln':'du übermittelst', 'Sie kontaktieren':'du kontaktierst',
    'Sie haben':'du hast', 'Sie werden':'du wirst', 'Sie entscheiden':'du entscheidest',
    'Sie sehen':'du siehst', 'Sie brauchen':'du brauchst', 'Sie benötigen':'du benötigst'
].each { source, target -> replacements.put(source, target) }

replacements.put('Ihrem', 'deinem')
replacements.put('Ihren', 'deinen')
replacements.put('Ihrer', 'deiner')
replacements.put('Ihres', 'deines')
replacements.put('Ihnen', 'dir')
replacements.put('Ihre', 'deine')
replacements.put('Ihr', 'dein')

String toDu(String source, Map<String, String> replacements) {
    String result = source
    replacements.each { from, to -> result = result.replace(from, to) }
    [du:'Du', dein:'Dein', deine:'Deine', deinem:'Deinem', deinen:'Deinen', deiner:'Deiner', deines:'Deines', dir:'Dir'].each { lower, upper ->
        result = result.replaceAll("(^|[.!?]\\s+|>\\s*)${lower}\\b", "\$1${upper}")
    }
    result
}

def workspaceNames = ['website', 'problems', 'insights', 'tools', 'offers', 'cases', 'topics', 'navigation', 'footer', 'siteSettings']
def changed = [:].withDefault { [] as Set }

void migrateNode(Node node, Map<String, String> replacements, Map changed) {
    def properties = []
    for (Property property : node.properties) properties << property
    properties.each { Property property ->
        if (property.type != PropertyType.STRING || property.name.startsWith('jcr:') || property.name.startsWith('mgnl:')) return
        if (property.multiple) {
            def oldValues = property.values.collect { it.string }
            def newValues = oldValues.collect { toDu(it, replacements) }
            if (oldValues != newValues) {
                property.setValue(newValues as String[])
                changed[node.getSession().getWorkspace().getName()] << node.getPath()
            }
        } else {
            String oldValue = property.string
            String newValue = toDu(oldValue, replacements)
            if (oldValue != newValue) {
                property.setValue(newValue)
                changed[node.getSession().getWorkspace().getName()] << node.getPath()
            }
        }
    }
    def children = []
    for (Node child : node.nodes) children << child
    children.each { migrateNode(it, replacements, changed) }
}

workspaceNames.each { workspaceName ->
    try {
        Session session = MgnlContext.getJCRSession(workspaceName)
        migrateNode(session.rootNode, replacements, changed)
        session.save()
    } catch (Exception error) {
        println "Workspace ${workspaceName} übersprungen: ${error.message}"
    }
}

def commands = CommandsManager.getInstance()
changed.each { workspaceName, paths ->
    def publishPaths
    if (workspaceName == 'website') {
        publishPaths = paths.collect { path ->
            def segments = path.tokenize('/')
            segments ? '/' + segments[0] : '/'
        }.toSet()
    } else {
        publishPaths = paths.collect { path -> path.startsWith('/cleonhardt') ? '/cleonhardt' : '/' }.toSet()
    }
    publishPaths.each { path ->
        def parameters = new LinkedHashMap<String, Object>()
        parameters.put('repository', workspaceName)
        parameters.put('path', path)
        parameters.put('recursive', true)
        def result = commands.executeCommand('default', 'publish', parameters)
        println "${workspaceName}:${path}: ${result ? 'veröffentlicht' : 'Veröffentlichung nicht bestätigt'}"
    }
}

println "Du-Ansprache migriert. Geänderte Workspaces: ${changed.findAll { it.value }.keySet().join(', ')}"
println 'Startseite folgt jetzt: Situation -> ChOS -> Vorgehen -> Orientierung -> Leistungen -> Vertrauen -> Kontakt.'
