import info.magnolia.commands.CommandsManager
import info.magnolia.context.MgnlContext
import javax.jcr.Node
import javax.jcr.Session
import java.security.MessageDigest

/*
 * In Magnolias Groovy-App auf der Author-Instanz ausführen.
 *
 * Migriert ausschließlich die aktuell in /impressum und /datenschutz
 * verwendeten Textkomponenten in den Content Type legalDocument. Die
 * öffentlichen URLs, Seiteneinstellungen, SEO-Daten und sichtbaren Inhalte
 * bleiben unverändert. Das Skript fügt keine neuen Rechtstexte hinzu.
 *
 * Reihenfolge aus Ausfallsicherheit:
 * 1. aktuellen Seitentext unverändert in legalDocuments speichern,
 * 2. Rechtstext veröffentlichen,
 * 3. Seitenkomponente auf die zentrale Referenz umstellen,
 * 4. Seite veröffentlichen.
 *
 * Das Skript ist wiederholbar. Bereits migrierte Seiten werden geprüft, aber
 * nicht aus einer alten Seitenkopie überschrieben.
 */

Session website = MgnlContext.getJCRSession('website')
Session legalDocuments = MgnlContext.getJCRSession('legalDocuments')
def commands = CommandsManager.getInstance()

Node ensureFolder(Session session, String name) {
    session.rootNode.hasNode(name)
        ? session.rootNode.getNode(name)
        : session.rootNode.addNode(name, 'mgnl:folder')
}

Node ensureContent(Node folder, String name) {
    folder.hasNode(name)
        ? folder.getNode(name)
        : folder.addNode(name, 'mgnl:content')
}

Node findByTemplate(Node root, String template) {
    if (root.hasProperty('mgnl:template') && root.getProperty('mgnl:template').string == template) {
        return root
    }

    def children = root.nodes
    while (children.hasNext()) {
        Node match = findByTemplate(children.nextNode(), template)
        if (match != null) return match
    }
    null
}

String property(Node node, String name) {
    node != null && node.hasProperty(name) ? node.getProperty(name).string : ''
}

String digest(String heading, String body) {
    byte[] bytes = "${heading}\n${body}".getBytes('UTF-8')
    MessageDigest.getInstance('SHA-256').digest(bytes).encodeHex().toString()
}

void publish(def commands, String repository, String path, boolean recursive = true) {
    def parameters = new LinkedHashMap<String, Object>()
    parameters.put('repository', repository)
    parameters.put('path', path)
    parameters.put('recursive', recursive)
    def result = commands.executeCommand('default', 'publish', parameters)
    println "${repository}:${path} – ${result ? 'veröffentlicht' : 'Publishing-Befehl ohne Rückgabewert ausgeführt'}"
}

Node folder = ensureFolder(legalDocuments, 'cleonhardt')

[
    [pagePath: '/impressum', nodeName: 'impressum', documentType: 'impressum'],
    [pagePath: '/datenschutz', nodeName: 'datenschutz', documentType: 'datenschutz']
].each { definition ->
    if (!website.nodeExists(definition.pagePath as String)) {
        throw new IllegalStateException("Seite fehlt: ${definition.pagePath}")
    }

    Node page = website.getNode(definition.pagePath as String)
    Node legacyComponent = findByTemplate(page, 'meine-website:components/text')
    Node legalComponent = findByTemplate(page, 'meine-website:components/legalDocument')
    Node component = legalComponent ?: legacyComponent

    if (component == null) {
        throw new IllegalStateException("Keine migrierbare Inhaltskomponente unter ${definition.pagePath} gefunden.")
    }

    Node document = ensureContent(folder, definition.nodeName as String)

    if (legacyComponent != null) {
        String heading = property(legacyComponent, 'heading')
        String body = property(legacyComponent, 'text')
        if (!heading || !body) {
            throw new IllegalStateException("Überschrift oder Text fehlt unter ${legacyComponent.getPath()}.")
        }

        String sourceDigest = digest(heading, body)
        document.setProperty('title', heading)
        document.setProperty('slug', definition.nodeName as String)
        document.setProperty('documentType', definition.documentType as String)
        document.setProperty('body', body)
        document.setProperty('sourcePage', page.getIdentifier())
        legalDocuments.save()

        String migratedDigest = digest(property(document, 'title'), property(document, 'body'))
        if (sourceDigest != migratedDigest) {
            throw new IllegalStateException("Inhaltsprüfung fehlgeschlagen für ${definition.pagePath}.")
        }

        // In einem neuen Workspace muss Magnolia den übergeordneten Ordner in
        // einem eigenen Schritt aktivieren, bevor ein Kind veröffentlicht wird.
        publish(commands, 'legalDocuments', folder.getPath(), false)
        Thread.sleep(1000)
        publish(commands, 'legalDocuments', document.getPath())
        Thread.sleep(1000)

        component.setProperty('mgnl:template', 'meine-website:components/legalDocument')
        component.setProperty('legalDocumentReference', document.getIdentifier())
        if (component.hasProperty('heading')) component.getProperty('heading').remove()
        if (component.hasProperty('text')) component.getProperty('text').remove()
        website.save()
        publish(commands, 'website', page.getPath())

        println "${definition.pagePath}: migriert und veröffentlicht (${sourceDigest})"
    } else {
        if (!document.hasProperty('title') || !document.hasProperty('body')) {
            throw new IllegalStateException("Bereits umgestellte Seite verweist nicht auf einen vollständigen Rechtstext: ${definition.pagePath}")
        }

        component.setProperty('legalDocumentReference', document.getIdentifier())
        website.save()
        publish(commands, 'legalDocuments', folder.getPath(), false)
        Thread.sleep(1000)
        publish(commands, 'legalDocuments', document.getPath())
        Thread.sleep(1000)
        publish(commands, 'website', page.getPath())
        println "${definition.pagePath}: bestehende Migration geprüft und erneut veröffentlicht (${digest(property(document, 'title'), property(document, 'body'))})"
    }
}

println 'Impressum und Datenschutz verwenden jetzt zentral veröffentlichte Rechtstexte.'
