import info.magnolia.context.MgnlContext
import javax.jcr.Node
import javax.jcr.Session

/*
 * Installiert die dauerhaft ausführbare Groovy-Command-Klasse und registriert
 * sie als Magnolia-Command. Das eigentliche UI bleibt als Light-Module-YAML
 * versioniert.
 */

String sourcePath = '/tmp/HubSpotProductSyncCommand.groovy'
String source = new File(sourcePath).getText('UTF-8')

Node ensureNode(Node parent, String name, String type) {
    parent.hasNode(name) ? parent.getNode(name) : parent.addNode(name, type)
}

Session scripts = MgnlContext.getJCRSession('scripts')
Node packageRoot = ensureNode(scripts.rootNode, 'cleonhardt', 'mgnl:folder')
Node commandsFolder = ensureNode(packageRoot, 'commands', 'mgnl:folder')
Node commandSource = ensureNode(commandsFolder, 'HubSpotProductSyncCommand', 'mgnl:content')
commandSource.setProperty('text', source)
commandSource.setProperty('script', false)
commandSource.setProperty('enabled', true)
scripts.save()

Session config = MgnlContext.getJCRSession('config')
Node module = config.getNode('/modules/groovy')
Node commands = ensureNode(module, 'commands', 'mgnl:content')
Node catalog = ensureNode(commands, 'default', 'mgnl:content')
Node command
if (catalog.hasNode('syncHubSpotProduct') &&
        catalog.getNode('syncHubSpotProduct').getProperty('jcr:primaryType').string != 'mgnl:contentNode') {
    Node oldCommand = catalog.getNode('syncHubSpotProduct')
    if (!oldCommand.hasProperty('class') ||
            oldCommand.getProperty('class').string != 'cleonhardt.commands.HubSpotProductSyncCommand') {
        throw new IllegalStateException('Unerwartete bestehende Command-Definition; Installation abgebrochen.')
    }
    oldCommand.remove()
}
command = ensureNode(catalog, 'syncHubSpotProduct', 'mgnl:contentNode')
command.setProperty('class', 'cleonhardt.commands.HubSpotProductSyncCommand')

// Korrigiert ausschließlich eine von einer früheren Installationsfassung
// angelegte, gleichnamige Command-Definition des Light Modules.
if (config.nodeExists('/modules/meine-website/commands/default/syncHubSpotProduct')) {
    Node legacyCommand = config.getNode('/modules/meine-website/commands/default/syncHubSpotProduct')
    if (legacyCommand.getProperty('jcr:primaryType').string != 'mgnl:contentNode') {
        if (!legacyCommand.hasProperty('class') ||
                legacyCommand.getProperty('class').string != 'cleonhardt.commands.HubSpotProductSyncCommand') {
            throw new IllegalStateException('Unerwartete bestehende Legacy-Definition; Installation abgebrochen.')
        }
        Node legacyCatalog = legacyCommand.parent
        legacyCommand.remove()
        legacyCommand = legacyCatalog.addNode('syncHubSpotProduct', 'mgnl:contentNode')
    }
    legacyCommand.setProperty('class', 'cleonhardt.commands.HubSpotProductSyncCommand')
}
config.save()

println 'HubSpot-Produktsynchronisation als Magnolia-Command installiert.'
