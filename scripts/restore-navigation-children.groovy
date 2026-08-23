import info.magnolia.commands.CommandsManager
import info.magnolia.context.MgnlContext
import javax.jcr.Node

/* Stellt die kuratierten Unterpunkte wieder her, ohne die Hauptnavigation zu ersetzen. */
def website = MgnlContext.getJCRSession('website')
def navigation = MgnlContext.getJCRSession('navigation')
Node items = navigation.getNode('/cleonhardt/Main/items')

Node findItem(Node parent, Node target) {
    for (Node item : parent.nodes) {
        if (item.hasProperty('targetPage') && item.getProperty('targetPage').string == target.getIdentifier()) return item
    }
    throw new IllegalStateException("Navigationseintrag nicht gefunden: ${target.getPath()}")
}

Node resetChildren(Node item) {
    if (item.hasNode('children')) item.getNode('children').remove()
    item.addNode('children', 'mgnl:contentNode')
}

void addItem(Node parent, int index, String label, Node target) {
    Node item = parent.addNode(String.format('%02d', index), 'mgnl:contentNode')
    item.setProperty('label', label)
    item.setProperty('targetPage', target.getIdentifier())
}

Node servicesChildren = resetChildren(findItem(items, website.getNode('/leistungen')))
addItem(servicesChildren, 0, 'Clarity Session', website.getNode('/clarity-session'))

Node chosChildren = resetChildren(findItem(items, website.getNode('/chos')))
addItem(chosChildren, 0, 'ChOS Selbstcheck', website.getNode('/chos-selbstcheck'))
addItem(chosChildren, 1, 'Praxisfälle', website.getNode('/praxisfaelle'))

Node insightsChildren = resetChildren(findItem(items, website.getNode('/insights')))
[
    ['/insights/warum-produktorganisationen-nicht-an-fehlenden-methoden-scheitern', 'Methoden sind selten der Engpass'],
    ['/insights/diagnose-vor-eingriff', 'Diagnose vor Eingriff'],
    ['/insights/unklare-rollen-sind-selten-das-eigentliche-problem', 'Unklare Rollen'],
    ['/insights/rollen-und-verantwortlichkeiten-in-produktorganisationen-klaeren', 'Rollen und Verantwortung klären'],
    ['/insights/wann-braucht-eine-produktorganisation-ein-operating-model', 'Wann braucht es ein Operating Model?'],
    ['/insights/annahmen-vor-einer-produktentscheidung-pruefen', 'Annahmen vor Entscheidungen prüfen'],
    ['/insights/organisationsdiagnose-statt-standardberatung', 'Organisationsdiagnose statt Standardberatung'],
    ['/insights/product-organisation-diagnostic-ablauf-und-ergebnis', 'Diagnostic: Ablauf und Ergebnis'],
    ['/insights/wann-ist-executive-sparring-sinnvoll', 'Wann ist Executive Sparring sinnvoll?']
].eachWithIndex { entry, index -> addItem(insightsChildren, index, entry[1], website.getNode(entry[0])) }

navigation.save()

Node root = navigation.getNode('/cleonhardt/Main')
['mgnl:lastActivatedVersion', 'mgnl:lastActivatedVersionCreated'].each { name ->
    if (root.hasProperty(name)) root.getProperty(name).remove()
}
navigation.save()

def parameters = new LinkedHashMap<String, Object>()
parameters.put('repository', 'navigation')
parameters.put('path', '/cleonhardt/Main')
parameters.put('recursive', true)
def result = CommandsManager.getInstance().executeCommand('default', 'publish', parameters)
println "Navigation: ${result ? 'veröffentlicht' : 'Veröffentlichung ausgelöst'}"
println 'Leistungen enthält die Clarity Session; ChOS enthält Selbstcheck und Praxisfälle; Insights enthält neun kuratierte Artikel (Ausgabe auf fünf plus Übersicht begrenzt).'
