import info.magnolia.commands.CommandsManager
import info.magnolia.context.MgnlContext

def targets = [
    [workspace: 'offers', path: '/cleonhardt'],
    [workspace: 'website', path: '/leistungen'],
    [workspace: 'website', path: '/start']
]

def commands = CommandsManager.getInstance()

targets.each { target ->
    def session = MgnlContext.getJCRSession(target.workspace)
    if (!session.nodeExists(target.path)) {
        throw new IllegalStateException("Inhalt nicht gefunden: ${target.workspace}:${target.path}")
    }
    def node = session.getNode(target.path)
    ['mgnl:lastActivatedVersion', 'mgnl:lastActivatedVersionCreated'].each { name ->
        if (node.hasProperty(name)) node.getProperty(name).remove()
    }
    session.save()

    def parameters = new LinkedHashMap<String, Object>()
    parameters.put('repository', target.workspace)
    parameters.put('path', target.path)
    parameters.put('recursive', true)
    def result = commands.executeCommand('default', 'publish', parameters)
    println "${target.workspace}:${target.path}: ${result ? 'veröffentlicht' : 'Veröffentlichung nicht bestätigt'}"
}

println 'Angebotskatalog und referenzierende Seiten wurden veröffentlicht.'
