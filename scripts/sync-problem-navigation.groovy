import info.magnolia.commands.CommandsManager
import info.magnolia.context.MgnlContext

import javax.jcr.Node
import javax.jcr.Session

/*
 * Synchronisiert das Untermenü „Situationen“ mit allen Problem-Unterseiten.
 * Damit muss eine neue Problemsituation nicht zusätzlich von Hand in der
 * Navigation gepflegt werden.
 */
Session website = MgnlContext.getJCRSession('website')
Session navigation = MgnlContext.getJCRSession('navigation')

Node findNavigationItem(Node parent, Node target) {
    for (Node item : parent.nodes) {
        if (item.hasProperty('targetPage') && item.getProperty('targetPage').string == target.getIdentifier()) return item
    }
    throw new IllegalStateException("Navigationseintrag nicht gefunden: ${target.getPath()}")
}

Node problemsPage = website.getNode('/probleme')
Node navigationItems = navigation.getNode('/cleonhardt/Main/items')
Node situationsItem = findNavigationItem(navigationItems, problemsPage)

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

Node navigationRoot = navigation.getNode('/cleonhardt/Main')
['mgnl:lastActivatedVersion', 'mgnl:lastActivatedVersionCreated'].each { name ->
    if (navigationRoot.hasProperty(name)) navigationRoot.getProperty(name).remove()
}
navigation.save()

def parameters = new LinkedHashMap<String, Object>()
parameters.put('repository', 'navigation')
parameters.put('path', '/cleonhardt/Main')
parameters.put('recursive', true)
def result = CommandsManager.getInstance().executeCommand('default', 'publish', parameters)

println "Situationen-Navigation mit ${index} Problemseiten synchronisiert und ${result ? 'veröffentlicht' : 'zur Veröffentlichung übergeben'}."
