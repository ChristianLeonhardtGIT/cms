import info.magnolia.commands.CommandsManager
import info.magnolia.context.MgnlContext

def website = MgnlContext.getJCRSession('website')
def paths = [
    '/leistungen',
    '/clarity-session',
    '/decision-review',
    '/executive-sparring',
    '/product-organisation-diagnostic',
    '/ai-operating-model-assessment',
    '/ai-enabled-workflow-sprint',
    '/workshops',
    '/angebot-anfragen'
]

def commands = CommandsManager.getInstance()
paths.each { String path ->
    if (!website.nodeExists(path)) {
        println "${path}: auf Author nicht vorhanden"
        return
    }
    def parameters = new LinkedHashMap<String, Object>()
    parameters.put('repository', 'website')
    parameters.put('path', path)
    parameters.put('recursive', true)
    def result = commands.executeCommand('default', 'publish', parameters)
    println "${path}: ${result ? 'veröffentlicht' : 'Veröffentlichung nicht bestätigt'}"
}

println 'Freigegebene Leistungsseiten wurden auf Public veröffentlicht.'
