import info.magnolia.commands.CommandsManager
import info.magnolia.context.MgnlContext

def paths = [
    '/angebot-anfragen',
    '/leistungen',
    '/datenschutz'
]

def website = MgnlContext.getJCRSession('website')

paths.each { path ->
    if (!website.nodeExists(path)) {
        throw new IllegalStateException("Seite nicht gefunden: ${path}")
    }

    def page = website.getNode(path)
    ['mgnl:lastActivatedVersion', 'mgnl:lastActivatedVersionCreated'].each { propertyName ->
        if (page.hasProperty(propertyName)) {
            page.getProperty(propertyName).remove()
        }
    }
}

website.save()

def commands = CommandsManager.getInstance()

paths.each { path ->
    def parameters = new LinkedHashMap<String, Object>()
    parameters.put('repository', 'website')
    parameters.put('path', path)
    parameters.put('recursive', true)

    def result = commands.executeCommand('default', 'publish', parameters)
    println "${path}: ${result ? 'veröffentlicht' : 'Veröffentlichung nicht bestätigt'}"
}

println 'Angebotsanfrage, Leistungen und Datenschutz wurden veröffentlicht.'
