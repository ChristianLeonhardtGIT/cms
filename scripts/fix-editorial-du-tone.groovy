import info.magnolia.commands.CommandsManager
import info.magnolia.context.MgnlContext
import javax.jcr.Node
import javax.jcr.Property
import javax.jcr.PropertyType

/*
 * Repariert bekannte Mischformen aus der früheren Sie-zu-du-Migration.
 * Wiederholbar: Es werden ausschließlich bestätigte fehlerhafte Formulierungen
 * ersetzt. Anaphorische Pronomen wie "Die Antworten … Sie werden …" werden
 * nicht pauschal verändert.
 */

def website = MgnlContext.getJCRSession('website')
website.refresh(false)

def replacements = new LinkedHashMap<String, String>()
replacements.put(
    'Wähle das gewünschte Format und beschreiben Sie kurz deine Situation.',
    'Wähle das gewünschte Format und beschreibe kurz deine Situation.'
)
replacements.put(
    'Du nennst das gewünschte Format und geben uns den notwendigen Kontext für eine erste Einordnung.',
    'Du nennst das gewünschte Format und gibst mir den notwendigen Kontext für eine erste Einordnung.'
)
replacements.put(
    'Beschreibe kurz, was Sie klären oder verändern möchten.',
    'Beschreibe kurz, was du klären oder verändern möchtest.'
)
replacements.put(
    'Welche Situation möchtest du klären und welches Ergebnis wünschen Sie?',
    'Welche Situation möchtest du klären und welches Ergebnis wünschst du?'
)
replacements.put(
    'Bitte schildern Sie zunächst nur den notwendigen Kontext.',
    'Bitte schildere zunächst nur den notwendigen Kontext.'
)
replacements.put(
    'Deine Antworten werden nur in diesem Browser ausgewertet. Du wirst weder übertragen noch gespeichert und es werden dafür keine Cookies gesetzt.',
    'Die Auswertung findet ausschließlich in diesem Browser statt. Dabei werden keine Antworten übertragen oder gespeichert und keine Cookies gesetzt.'
)
replacements.put(
    'Suche mehrere konkrete Beispiele, vergleichen Sie unterschiedliche Perspektiven und prüfen Sie bewusst auch Gegenbelege.',
    'Suche mehrere konkrete Beispiele, vergleiche unterschiedliche Perspektiven und prüfe bewusst auch Gegenbelege.'
)
replacements.put(
    'Nutze es als Gesprächsgrundlage: Vergleiche Wahrnehmungen, suchen Sie konkrete Beispiele und prüfen Sie, ob sich die erkannten Muster in mehreren Situationen wiederholen.',
    'Nutze es als Gesprächsgrundlage: Vergleiche Wahrnehmungen, suche konkrete Beispiele und prüfe, ob sich die erkannten Muster in mehreren Situationen wiederholen.'
)
replacements.put(
    'Beginne mit einer konkreten, wiederkehrenden Situation. Trennen Sie Beobachtung, Erklärung und gewünschte Wirkung, bevor du eine Rolle, einen Prozess oder eine Struktur veränderst.',
    'Beginne mit einer konkreten, wiederkehrenden Situation. Trenne Beobachtung, Erklärung und gewünschte Wirkung, bevor du eine Rolle, einen Prozess oder eine Struktur veränderst.'
)

Set<String> changedPages = [] as Set

String replaceConfirmedPhrases(String source, Map<String, String> replacements) {
    String result = source
    replacements.each { from, to -> result = result.replace(from, to) }
    result
}

String containingPagePath(Node node) {
    Node current = node
    while (current.getDepth() > 0 && !current.isNodeType('mgnl:page')) current = current.getParent()
    current.isNodeType('mgnl:page') ? current.getPath() : '/'
}

void repairNode(Node node, Map<String, String> replacements, Set<String> changedPages) {
    List<Property> properties = []
    for (Property property : node.properties) properties << property
    properties.each { Property property ->
        if (property.type != PropertyType.STRING || property.multiple || property.name.startsWith('jcr:') || property.name.startsWith('mgnl:')) return
        String before = property.string
        String after = replaceConfirmedPhrases(before, replacements)
        if (before == after) return
        property.setValue(after)
        changedPages << containingPagePath(node)
        println "Korrigiert: ${node.getPath()} @ ${property.name}"
    }
    List<Node> children = []
    for (Node child : node.nodes) children << child
    children.each { repairNode(it, replacements, changedPages) }
}

repairNode(website.rootNode, replacements, changedPages)
changedPages.each { path ->
    if (website.nodeExists(path)) website.getNode(path).setProperty('dateModified', '2026-09-02')
}
website.save()

def commands = CommandsManager.getInstance()
changedPages.each { path ->
    def parameters = new LinkedHashMap<String, Object>()
    parameters.put('repository', 'website')
    parameters.put('path', path)
    parameters.put('recursive', true)
    def result = commands.executeCommand('default', 'publish', parameters)
    println "${path}: ${result ? 'veröffentlicht' : 'Veröffentlichung nicht bestätigt'}"
}

println "Redaktioneller Sofortfix abgeschlossen: ${changedPages.size()} Seiten geändert."
