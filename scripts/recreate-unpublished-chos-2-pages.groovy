import info.magnolia.context.MgnlContext
import javax.jcr.Node

/*
 * Einige ältere Autor-Seiten waren als aktiviert markiert, obwohl sie auf der
 * Public-Instanz fehlten. Eine Kopie mit neuer JCR-ID zwingt Magnolia zu einer
 * echten Erstveröffentlichung. Die bisherigen Knoten bleiben als Backup erhalten.
 */

def website = MgnlContext.getJCRSession('website')
def targets = [
    '/leistungen',
    '/clarity-session',
    '/decision-review',
    '/product-organisation-diagnostic',
    '/executive-sparring'
]

void clearPublicationState(Node node) {
    [
        'mgnl:lastActivated',
        'mgnl:activationStatus',
        'mgnl:lastActivatedVersion',
        'mgnl:lastActivatedVersionCreated'
    ].each { propertyName ->
        if (node.hasProperty(propertyName)) node.getProperty(propertyName).remove()
    }
    node.nodes.each { child -> clearPublicationState(child) }
}

targets.each { targetPath ->
    if (!website.nodeExists(targetPath)) {
        throw new IllegalStateException("Autor-Seite fehlt: ${targetPath}")
    }

    def name = targetPath.substring(1)
    def backupPath = "/chos2-backup-${name}"
    if (website.nodeExists(backupPath)) {
        website.getNode(backupPath).remove()
        website.save()
    }

    website.move(targetPath, backupPath)
    website.save()
    website.workspace.copy(backupPath, targetPath)
    clearPublicationState(website.getNode(targetPath))
    website.save()
    println "${targetPath}: mit neuer Inhalts-ID vorbereitet; Backup: ${backupPath}"
}

println 'Fehlende ChOS-2.0-Seiten sind für eine echte Erstveröffentlichung vorbereitet.'
