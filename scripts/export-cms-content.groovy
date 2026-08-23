import info.magnolia.context.MgnlContext

import javax.jcr.Session
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

// Deliberately excludes users, userroles, config, scripts and password-manager
// workspaces. Those may contain credentials or personal data and belong only in
// the protected operational backup.
List<String> workspaces = [
    'website',
    'dam',
    'problems',
    'insights',
    'tools',
    'cases',
    'topics',
    'offers',
    'legalDocuments',
    'navigation',
    'footer',
    'siteSettings'
]

Path target = Paths.get('/tmp/cleonhardt-cms-content-export')
Files.createDirectories(target)

List<String> exported = []
List<String> unavailable = []

workspaces.each { String workspace ->
    try {
        Session session = MgnlContext.getJCRSession(workspace)
        Path file = target.resolve("${workspace}.system-view.xml")
        Files.newOutputStream(file).withCloseable { output ->
            session.exportSystemView('/', output, false, false)
        }
        exported << workspace
    } catch (Exception error) {
        unavailable << "${workspace}: ${error.class.simpleName}: ${error.message}"
    }
}

Files.writeString(target.resolve('EXPORT-METADATA.txt'), """\
createdAt=${new Date().format("yyyy-MM-dd'T'HH:mm:ssXXX")}
exported=${exported.join(',')}
excluded=users,userroles,config,scripts,passwordManager
unavailable=${unavailable.join(' | ')}
""")

println "Exportiert: ${exported.join(', ')}"
if (unavailable) {
    println "Nicht verfügbar: ${unavailable.join(' | ')}"
}
println "Ziel: ${target}"
