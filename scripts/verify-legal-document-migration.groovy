import info.magnolia.context.MgnlContext
import javax.jcr.Node
import javax.jcr.Session
import java.security.MessageDigest

/*
 * Read-only verification for the legal-document migration.
 * Run in Magnolia's Groovy console on the Author instance.
 */

Session website = MgnlContext.getJCRSession('website')
Session legalDocuments = MgnlContext.getJCRSession('legalDocuments')

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

String digest(String heading, String body) {
    byte[] bytes = "${heading}\n${body}".getBytes('UTF-8')
    MessageDigest.getInstance('SHA-256').digest(bytes).encodeHex().toString()
}

[
    [pagePath: '/impressum', documentPath: '/cleonhardt/impressum'],
    [pagePath: '/datenschutz', documentPath: '/cleonhardt/datenschutz']
].each { definition ->
    Node page = website.getNode(definition.pagePath as String)
    Node component = findByTemplate(page, 'meine-website:components/legalDocument')
    if (component == null) {
        throw new IllegalStateException("Zentrale Rechtstext-Komponente fehlt unter ${definition.pagePath}.")
    }
    if (!component.hasProperty('legalDocumentReference')) {
        throw new IllegalStateException("Rechtstext-Referenz fehlt unter ${component.getPath()}.")
    }
    if (component.hasProperty('heading') || component.hasProperty('text')) {
        throw new IllegalStateException("Alte Textfelder sind unter ${component.getPath()} noch vorhanden.")
    }

    String reference = component.getProperty('legalDocumentReference').string
    Node document = legalDocuments.getNodeByIdentifier(reference)
    if (document.getPath() != definition.documentPath) {
        throw new IllegalStateException("Falsche Referenz unter ${definition.pagePath}: ${document.getPath()}")
    }
    if (!document.hasProperty('title') || !document.hasProperty('body')) {
        throw new IllegalStateException("Rechtstext ist unvollständig: ${document.getPath()}")
    }

    String checksum = digest(document.getProperty('title').string, document.getProperty('body').string)
    println "OK ${definition.pagePath} -> legalDocuments:${document.getPath()} (${checksum})"
}

println 'Zentrale Rechtstext-Migration ist vollständig und konsistent.'
