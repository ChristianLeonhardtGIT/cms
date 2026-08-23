import info.magnolia.commands.CommandsManager
import info.magnolia.context.MgnlContext
import javax.jcr.Node

/*
 * Stellt Profilbereiche auf das zentrale Porträt aus den Website-Einstellungen um.
 * Wiederholbar: Die bestehenden Abschnitte werden in-place konvertiert, damit
 * ihre Position auf der Seite erhalten bleibt.
 */

def website = MgnlContext.getJCRSession('website')
def siteSettings = MgnlContext.getJCRSession('siteSettings')
def commands = CommandsManager.getInstance()

Node findByHeading(Node area, String heading) {
    for (Node child : area.nodes) {
        if (child.hasProperty('heading') && child.getProperty('heading').string == heading) return child
    }
    throw new IllegalStateException("Komponente nicht gefunden: ${heading}")
}

void setProperties(Node node, Map<String, Object> properties) {
    properties.each { key, value ->
        if (value != null) node.setProperty(key, value)
    }
}

Node homeProfile = findByHeading(website.getNode('/start/main'), 'Erfahrung, die Strategie und Alltag verbindet')
if (homeProfile.hasProperty('text')) homeProfile.getProperty('text').remove()
homeProfile.setProperty('mgnl:template', 'meine-website:components/profileTrust')
setProperties(homeProfile, [
    heading: 'Erfahrung, die Strategie und Alltag verbindet',
    eyebrow: 'Christian Leonhardt',
    title: 'Product Leadership, Organisation und Transformation',
    lead: 'Ich verbinde Produktverantwortung mit einem systemischen Blick auf Entscheidungen, Rollen und Zusammenarbeit.',
    body: '<p>Meine Erfahrung reicht von digitalen Produkt- und Plattformorganisationen über Transformation und Stabilisierung bis zur Führung in komplexen, bereichsübergreifenden Situationen.</p>',
    topics: 'Product Leadership, Loyalty & CRM, E-Commerce, Organisationsdiagnose',
    signature: 'Christian Leonhardt · Klarheit für komplexe Produktorganisationen.',
    linkText: 'Mehr über meinen Hintergrund',
    targetPage: '/ueber-mich',
    aboutMode: false
])

Node aboutProfile = findByHeading(website.getNode('/ueber-mich/main'), 'Christian Leonhardt')
if (aboutProfile.hasProperty('text')) aboutProfile.getProperty('text').remove()
aboutProfile.setProperty('mgnl:template', 'meine-website:components/profileTrust')
setProperties(aboutProfile, [
    heading: 'Christian Leonhardt',
    eyebrow: 'Profil',
    title: 'Product Leadership, Organisationsdiagnose und Transformation',
    lead: 'Ich begleite Produktverantwortliche, Führungsteams und digitale Organisationen in Situationen, in denen Methodenwissen allein nicht weiterhilft.',
    body: '<p>Meine Arbeit liegt an den Übergängen: zwischen Strategie und Umsetzung, Produkt und Technologie, Verantwortung und formaler Struktur, Wachstum und notwendiger Neuordnung. Gerade dort entstehen Reibung, Verzögerung und Missverständnisse – aber auch die größten Hebel.</p><p>Diese Perspektive ist nicht am Schreibtisch entstanden. Ich habe Produktorganisationen aufgebaut und weiterentwickelt, kritische Programme stabilisiert, internationale Stakeholder zusammengebracht und Verantwortung in Situationen übernommen, in denen Entscheidungen unter Unsicherheit getroffen werden mussten.</p><p>Statt vorschnell ein Zielbild über die Organisation zu legen, arbeite ich zuerst heraus, welches Problem tatsächlich gelöst werden muss. Daraus entstehen klare Entscheidungen und Veränderungen, die zum Kontext passen.</p>',
    topics: 'Product Leadership, Loyalty & CRM, E-Commerce, Organisationsdiagnose',
    signature: 'Diagnose vor Eingriff. Klarheit vor Aktion.',
    aboutMode: true
])

Node settingsNode = null
if (siteSettings.nodeExists('/cleonhardt/Logo-Cleonhardt')) settingsNode = siteSettings.getNode('/cleonhardt/Logo-Cleonhardt')
if (settingsNode != null) {
    if (!settingsNode.hasProperty('portraitImageAlt')) settingsNode.setProperty('portraitImageAlt', 'Portrait von Christian Leonhardt')
    if (!settingsNode.hasProperty('socialImageAlt')) settingsNode.setProperty('socialImageAlt', 'Christian Leonhardt – Klarheit für komplexe Produktorganisationen')
}

website.save()
siteSettings.save()

['/start', '/ueber-mich'].each { path ->
    def parameters = new LinkedHashMap<String, Object>()
    parameters.put('repository', 'website')
    parameters.put('path', path)
    parameters.put('recursive', true)
    commands.executeCommand('default', 'publish', parameters)
    println "${path}: Veröffentlichung ausgelöst"
}

if (settingsNode != null) {
    def parameters = new LinkedHashMap<String, Object>()
    parameters.put('repository', 'siteSettings')
    parameters.put('path', settingsNode.getPath())
    parameters.put('recursive', true)
    commands.executeCommand('default', 'publish', parameters)
    println "${settingsNode.getPath()}: zentrale Bildfelder veröffentlicht"
}

println 'Profilbereiche verwenden jetzt das zentrale Porträt aus den Website-Einstellungen.'

