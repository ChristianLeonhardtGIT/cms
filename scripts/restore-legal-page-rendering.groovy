import info.magnolia.commands.CommandsManager
import info.magnolia.context.MgnlContext
import javax.jcr.Node
import javax.jcr.Session

/*
 * Sicherheits-Rollback für die Rechtstext-Migration.
 * Kopiert ausschließlich die bereits migrierten Inhalte zurück in die
 * bisherigen Textkomponenten und veröffentlicht die beiden Seiten erneut.
 * Die zentralen legalDocument-Datensätze bleiben bestehen.
 */

Session website = MgnlContext.getJCRSession('website')
Session legalDocuments = MgnlContext.getJCRSession('legalDocuments')
def commands = CommandsManager.getInstance()

Node findByTemplate(Node root, String template) {
    if (root.hasProperty('mgnl:template') && root.getProperty('mgnl:template').string == template) return root
    def children = root.nodes
    while (children.hasNext()) {
        Node match = findByTemplate(children.nextNode(), template)
        if (match != null) return match
    }
    null
}

void publish(def commands, String path) {
    def parameters = new LinkedHashMap<String, Object>()
    parameters.put('repository', 'website')
    parameters.put('path', path)
    parameters.put('recursive', true)
    commands.executeCommand('default', 'publish', parameters)
}

[
    [pagePath: '/impressum', documentPath: '/cleonhardt/impressum'],
    [pagePath: '/datenschutz', documentPath: '/cleonhardt/datenschutz']
].each { definition ->
    Node page = website.getNode(definition.pagePath as String)
    Node component = findByTemplate(page, 'meine-website:components/legalDocument')
    Node document = legalDocuments.getNode(definition.documentPath as String)
    if (component == null) throw new IllegalStateException("Rechtstext-Komponente fehlt unter ${definition.pagePath}.")

    component.setProperty('mgnl:template', 'meine-website:components/text')
    component.setProperty('heading', document.getProperty('title').string)
    component.setProperty('text', document.getProperty('body').string)
    if (component.hasProperty('legalDocumentReference')) component.getProperty('legalDocumentReference').remove()
    website.save()
    publish(commands, page.getPath())
    println "${definition.pagePath}: bisherige Darstellung wiederhergestellt"
}

println 'Öffentliche Rechtstext-Darstellung wurde aus den migrierten Datensätzen wiederhergestellt.'
