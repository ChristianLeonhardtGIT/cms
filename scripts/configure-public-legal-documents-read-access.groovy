import info.magnolia.context.MgnlContext
import javax.jcr.Node
import javax.jcr.Session

/*
 * Ausschließlich auf der Magnolia-Public-Instanz ausführen.
 * Ergänzt für anonyme Website-Besucher den gleichen reinen Lesezugriff auf
 * legalDocuments, der bereits für topics, problems, insights, tools und cases
 * verwendet wird. Andere ACLs werden nicht verändert.
 */

Session roles = MgnlContext.getJCRSession('userroles')
Node anonymous = roles.getNode('/anonymous')
String sourcePath = '/anonymous/acl_topics'
String targetPath = '/anonymous/acl_legalDocuments'

if (!roles.nodeExists(sourcePath)) {
    throw new IllegalStateException("Vorlage für öffentliche Lese-ACL fehlt: ${sourcePath}")
}

if (!roles.nodeExists(targetPath)) {
    roles.getWorkspace().copy(sourcePath, targetPath)
    roles.save()
}

Node acl = roles.getNode(targetPath)
Map<String, Long> expected = ['/': 8L, '/*': 8L]
Map<String, Long> actual = [:]
def rules = acl.nodes
while (rules.hasNext()) {
    Node rule = rules.nextNode()
    if (rule.hasProperty('path') && rule.hasProperty('permissions')) {
        actual[rule.getProperty('path').string] = rule.getProperty('permissions').long
    }
}

if (actual != expected) {
    throw new IllegalStateException("Unerwartete ACL für legalDocuments: ${actual}")
}

println "${targetPath}: anonymer Lesezugriff auf / und /* ist aktiv."
