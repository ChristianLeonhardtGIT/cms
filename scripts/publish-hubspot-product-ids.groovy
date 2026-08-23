import info.magnolia.commands.CommandsManager
import info.magnolia.context.MgnlContext

/* Veröffentlicht ausschließlich die sieben Einträge des Angebotskatalogs. */

def session = MgnlContext.getJCRSession('offers')
def paths = [
    '/cleonhardt/chos-clarity-session',
    '/cleonhardt/decision-review',
    '/cleonhardt/leadership-product-sparring',
    '/cleonhardt/executive-sparring',
    '/cleonhardt/chos-quick-diagnostic',
    '/cleonhardt/product-organisation-diagnostic',
    '/cleonhardt/workshops'
]

paths.each { String path ->
    if (!session.nodeExists(path)) {
        throw new IllegalStateException("Leistung fehlt: offers:${path}")
    }

    def parameters = new LinkedHashMap<String, Object>()
    parameters.put('repository', 'offers')
    parameters.put('path', path)
    parameters.put('recursive', true)
    CommandsManager.getInstance().executeCommand('default', 'publish', parameters)
    println "offers:${path}: Veröffentlichung ausgelöst"
    Thread.sleep(750)
}

println 'Sieben Magnolia-Leistungen einschließlich HubSpot-Produkt-IDs zur Veröffentlichung übergeben.'
