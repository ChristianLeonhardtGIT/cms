import info.magnolia.commands.CommandsManager
import info.magnolia.context.MgnlContext

import javax.jcr.Node

def website = MgnlContext.getJCRSession('website')

Node findByProperty(Node root, String propertyName, String propertyValue) {
    if (root.hasProperty(propertyName) && root.getProperty(propertyName).string == propertyValue) {
        return root
    }
    for (Node child : root.nodes) {
        Node match = findByProperty(child, propertyName, propertyValue)
        if (match != null) return match
    }
    null
}

if (!website.nodeExists('/start')) {
    throw new IllegalStateException('Startseite fehlt: /start')
}

Node start = website.getNode('/start')
Node situationBlock = findByProperty(start, 'heading', 'Mehr Technik löst keine unklaren Strukturen.')
if (situationBlock == null || !situationBlock.hasProperty('text')) {
    throw new IllegalStateException('Situationsblock auf der Startseite wurde nicht gefunden.')
}

String html = situationBlock.getProperty('text').string
String link = '<p class="text-link"><a href="/probleme">Typische Situationen ansehen <span aria-hidden="true">→</span></a></p>'
if (!html.contains('href="/probleme"')) {
    situationBlock.setProperty('text', html + link)
}

website.save()

def parameters = new LinkedHashMap<String, Object>()
parameters.put('repository', 'website')
parameters.put('path', '/start')
parameters.put('recursive', true)
def result = CommandsManager.getInstance().executeCommand('default', 'publish', parameters)

println "Startseite: Situationslink ergänzt; ${result ? 'veröffentlicht' : 'Veröffentlichung nicht bestätigt'}"
