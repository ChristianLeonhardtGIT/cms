import info.magnolia.context.MgnlContext
import javax.jcr.Node
import javax.jcr.Property
import javax.jcr.Session

/* Read-only diagnostic for the public anonymous role. */

Session roles = MgnlContext.getJCRSession('userroles')
Node anonymous = roles.getNode('/anonymous')

println "Role: ${anonymous.getPath()}"
def children = anonymous.nodes
while (children.hasNext()) {
    Node acl = children.nextNode()
    if (!acl.getName().startsWith('acl_')) continue

    println "${acl.getName()}:"
    def rules = acl.nodes
    while (rules.hasNext()) {
        Node rule = rules.nextNode()
        List<String> values = []
        ['path', 'permissions', 'workspace'].each { name ->
            if (rule.hasProperty(name)) values << "${name}=${rule.getProperty(name).string}"
        }
        println "  ${rule.getName()} ${values.join(', ')}"
    }
}
