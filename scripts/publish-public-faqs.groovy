import info.magnolia.commands.CommandsManager
import info.magnolia.context.MgnlContext

def website = MgnlContext.getJCRSession('website')
def paths = [
    '/start', '/leistungen', '/chos', '/chos-selbstcheck', '/praxisfaelle',
    '/ueber-mich', '/insights', '/clarity-session', '/decision-review',
    '/product-organisation-diagnostic', '/ai-operating-model-assessment',
    '/ai-enabled-workflow-sprint', '/executive-sparring'
]

if (website.nodeExists('/insights')) {
    for (def child : website.getNode('/insights').nodes) {
        if (child.isNodeType('mgnl:page')) paths << child.getPath()
    }
}

def commands = CommandsManager.getInstance()
paths.each { path ->
    if (!website.nodeExists(path)) throw new IllegalStateException("Seite nicht gefunden: ${path}")
    def parameters = new LinkedHashMap<String, Object>()
    parameters.put('repository', 'website')
    parameters.put('path', path)
    parameters.put('recursive', true)
    def result = commands.executeCommand('default', 'publish', parameters)
    println "${path}: ${result ? 'veröffentlicht' : 'Veröffentlichung nicht bestätigt'}"
}
