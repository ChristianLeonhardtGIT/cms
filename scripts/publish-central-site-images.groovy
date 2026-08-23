import info.magnolia.commands.CommandsManager
import info.magnolia.context.MgnlContext

def settingsSession = MgnlContext.getJCRSession('siteSettings')
def damSession = MgnlContext.getJCRSession('dam')
def settings = settingsSession.getNode('/cleonhardt/Logo-Cleonhardt')

if (!settings.hasProperty('portraitImage')) {
    throw new IllegalStateException('In den Website-Einstellungen ist kein zentrales Porträt ausgewählt.')
}

def portraitId = settings.getProperty('portraitImage').string
def portrait = damSession.getNodeByIdentifier(portraitId)

[settings, portrait].each { node ->
    ['mgnl:lastActivatedVersion', 'mgnl:lastActivatedVersionCreated'].each { name ->
        if (node.hasProperty(name)) node.getProperty(name).remove()
    }
}
settingsSession.save()
damSession.save()

def commands = CommandsManager.getInstance()
[
    [repository: 'dam', path: portrait.getPath()],
    [repository: 'siteSettings', path: settings.getPath()]
].each { target ->
    def parameters = new LinkedHashMap<String, Object>()
    parameters.put('repository', target.repository)
    parameters.put('path', target.path)
    parameters.put('recursive', true)
    commands.executeCommand('default', 'publish', parameters)
    println "${target.repository}:${target.path} veröffentlicht"
}

println "Zentrales Porträt: ${portrait.getName()} (${portraitId})"
