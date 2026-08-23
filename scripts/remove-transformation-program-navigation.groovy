import info.magnolia.commands.CommandsManager
import info.magnolia.context.MgnlContext
import javax.jcr.Node

def navigation = MgnlContext.getJCRSession('navigation')
def website = MgnlContext.getJCRSession('website')

Node navItems = navigation.getNode('/cleonhardt/Main/items')
for (Node item : navItems.nodes) {
    if (!item.hasNode('children')) continue
    List<Node> remove = []
    for (Node child : item.getNode('children').nodes) {
        if (child.hasProperty('label') && child.getProperty('label').string == 'Transformation Program') remove << child
    }
    remove.each { it.remove() }
}
navigation.save()

if (website.nodeExists('/kontakt/main')) {
    Node area = website.getNode('/kontakt/main')
    for (Node component : area.nodes) {
        if (!component.hasNode('fieldsets')) continue
        for (Node group : component.getNode('fieldsets').nodes) {
            if (!group.hasNode('fields')) continue
            for (Node field : group.getNode('fields').nodes) {
                if (field.hasProperty('controlName') && field.getProperty('controlName').string == 'anliegen') {
                    field.setProperty('labels', 'Bitte auswählen:\nChOS Clarity Session:clarity-session\nChOS Decision Review:decision-review\nChOS Operating Model Diagnostic:operating-model-diagnostic\nAI Operating Model Assessment:ai-operating-model-assessment\nAI-enabled Workflow / Product Sprint:ai-workflow-sprint\nExecutive / Product Leadership Sparring:executive-sparring\nAnderes Anliegen:anderes')
                }
            }
        }
    }
    website.save()
}

def commands = CommandsManager.getInstance()
[
    [repository: 'navigation', path: '/cleonhardt/Main'],
    [repository: 'website', path: '/kontakt']
].each { target ->
    def parameters = new LinkedHashMap<String, Object>()
    parameters.put('repository', target.repository)
    parameters.put('path', target.path)
    parameters.put('recursive', true)
    commands.executeCommand('default', 'publish', parameters)
}

println 'Transformation Program aus Navigation und Kontakt-Auswahl entfernt.'
