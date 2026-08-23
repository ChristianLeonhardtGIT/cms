import info.magnolia.commands.CommandsManager
import info.magnolia.context.MgnlContext

import javax.jcr.Node

def navigation = MgnlContext.getJCRSession('navigation')
Node items = navigation.getNode('/cleonhardt/Main/items')

def desiredLabels = ['Start', 'Situationen', 'ChOS', 'Leistungen', 'Insights', 'Über mich', 'Kontakt']

def itemByLabel = [:]
for (Node item : items.nodes) {
    if (item.hasProperty('label')) {
        itemByLabel[item.getProperty('label').string] = item
    }
}

desiredLabels.each { label ->
    Node item = itemByLabel[label]
    if (item == null) {
        throw new IllegalStateException("Navigationseintrag fehlt: ${label}")
    }
    items.orderBefore(item.name, null)
}

navigation.save()

def parameters = new LinkedHashMap<String, Object>()
parameters.put('repository', 'navigation')
parameters.put('path', '/cleonhardt/Main')
parameters.put('recursive', true)
def result = CommandsManager.getInstance().executeCommand('default', 'publish', parameters)

println "Navigation: ${result ? 'neu geordnet und veröffentlicht' : 'Veröffentlichung nicht bestätigt'}"
println 'Start -> Situationen -> ChOS -> Leistungen -> Insights -> Über mich -> Kontakt'
