import info.magnolia.commands.CommandsManager
import info.magnolia.context.MgnlContext

def websitePaths = ['/praxisfaelle', '/chos-selbstcheck', '/chos']
def commands = CommandsManager.getInstance()
def website = MgnlContext.getJCRSession('website')

websitePaths.each { path ->
    if (!website.nodeExists(path)) throw new IllegalStateException("Seite nicht gefunden: ${path}")
    def page = website.getNode(path)
    ['mgnl:lastActivatedVersion', 'mgnl:lastActivatedVersionCreated'].each { name ->
        if (page.hasProperty(name)) page.getProperty(name).remove()
    }
}
website.save()

websitePaths.each { path ->
    def parameters = new LinkedHashMap<String, Object>()
    parameters.put('repository', 'website')
    parameters.put('path', path)
    parameters.put('recursive', true)
    def result = commands.executeCommand('default', 'publish', parameters)
    println "${path}: ${result ? 'veröffentlicht' : 'Veröffentlichung nicht bestätigt'}"
}

println 'Punkt 5 wurde veröffentlicht.'
