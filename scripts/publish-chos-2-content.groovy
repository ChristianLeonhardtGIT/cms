import info.magnolia.commands.CommandsManager
import info.magnolia.context.MgnlContext

def commands = CommandsManager.getInstance()
def website = MgnlContext.getJCRSession('website')

def websitePaths = [
    '/start', '/leistungen', '/chos', '/ueber-mich', '/kontakt',
    '/clarity-session', '/decision-review', '/product-organisation-diagnostic',
    '/ai-operating-model-assessment', '/ai-enabled-workflow-sprint',
    '/executive-sparring', '/insights'
]

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
    println "${path}: ${result ? 'veröffentlicht' : 'Veröffentlichung ausgelöst'}"
}

[
    [repository: 'offers', path: '/cleonhardt'],
    [repository: 'navigation', path: '/cleonhardt/Main'],
    [repository: 'footer', path: '/cleonhardt/Informationen'],
    [repository: 'siteSettings', path: '/cleonhardt/Logo-Cleonhardt']
].each { target ->
    def session = MgnlContext.getJCRSession(target.repository)
    if (!session.nodeExists(target.path)) {
        println "${target.repository}:${target.path}: übersprungen (nicht vorhanden)"
        return
    }
    def parameters = new LinkedHashMap<String, Object>()
    parameters.put('repository', target.repository)
    parameters.put('path', target.path)
    parameters.put('recursive', true)
    def result = commands.executeCommand('default', 'publish', parameters)
    println "${target.repository}:${target.path}: ${result ? 'veröffentlicht' : 'Veröffentlichung ausgelöst'}"
}

println 'ChOS-Inhalte und zentrale Referenzen wurden veröffentlicht.'
