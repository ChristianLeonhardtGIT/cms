import info.magnolia.commands.CommandsManager
import info.magnolia.context.MgnlContext
import javax.jcr.Node

/*
 * Ergänzt Profilbild, Kurzsignatur und Erfahrungsverdichtung auf Start und Über mich.
 * Wiederholbar: vorhandene Komponenten werden gezielt aktualisiert.
 */
def website = MgnlContext.getJCRSession('website')
def commands = CommandsManager.getInstance()

Node findByHeading(Node area, String heading) {
    for (Node child : area.nodes) {
        if (child.hasProperty('heading') && child.getProperty('heading').string == heading) return child
    }
    throw new IllegalStateException("Komponente nicht gefunden: ${heading}")
}

Node findByTemplate(Node area, String template, boolean last = false) {
    Node found = null
    for (Node child : area.nodes) {
        if (child.hasProperty('mgnl:template') && child.getProperty('mgnl:template').string == template) {
            found = child
            if (!last) return found
        }
    }
    if (found == null) throw new IllegalStateException("Komponententyp nicht gefunden: ${template}")
    found
}

void setInternalLink(Node component, String chooserName, String target) {
    Node chooser = component.hasNode(chooserName)
        ? component.getNode(chooserName)
        : component.addNode(chooserName, 'mgnl:contentNode')
    String linkNodeName = chooserName == 'ctaChooser' ? 'ctaLink' : null
    Node linkNode = linkNodeName == null
        ? chooser
        : (chooser.hasNode(linkNodeName) ? chooser.getNode(linkNodeName) : chooser.addNode(linkNodeName, 'mgnl:contentNode'))
    linkNode.setProperty('field', 'internalPageLink')
    linkNode.setProperty('internalLink', target)
}

String picture = '''<picture>
  <source type="image/webp" srcset="https://cleonhardt.de/.resources/meine-website/webresources/images/christian-leonhardt-portrait-v1.webp?v=20260731-1">
  <img src="https://cleonhardt.de/.resources/meine-website/webresources/images/christian-leonhardt-portrait-v1.jpg?v=20260731-1" width="960" height="1200" loading="lazy" decoding="async" alt="Portrait von Christian Leonhardt">
</picture>'''

Node home = website.getNode('/start')
Node homeArea = home.getNode('main')
Node homeHero = findByTemplate(homeArea, 'meine-website:components/hero')
homeHero.setProperty('description', 'Ich helfe Produktverantwortlichen, Führungsteams und digitalen Organisationen, Ursachen zu verstehen, Entscheidungen zu schärfen und wirksame nächste Schritte zu gehen.')
Node heroChooser = homeHero.hasNode('ctaChooser')
    ? homeHero.getNode('ctaChooser')
    : homeHero.addNode('ctaChooser', 'mgnl:contentNode')
heroChooser.setProperty('field', 'withCta')
heroChooser.setProperty('ctaText', 'Clarity Session ansehen')
setInternalLink(homeHero, 'ctaChooser', '/clarity-session')

Node homeTrust = findByHeading(homeArea, 'Erfahrung, die Strategie und Alltag verbindet')
homeTrust.setProperty('heading', 'Erfahrung, die Strategie und Alltag verbindet')
homeTrust.setProperty('showBackground', false)
homeTrust.setProperty('text', """<div class=\"profile-trust\">
  <figure class=\"profile-trust__media\">${picture}</figure>
  <div class=\"profile-trust__content\">
    <p class=\"mvp-meta\">Christian Leonhardt</p>
    <h3>Product Leadership, Organisation und Transformation</h3>
    <p class=\"lead\">Ich verbinde Produktverantwortung mit einem systemischen Blick auf Entscheidungen, Rollen und Zusammenarbeit.</p>
    <ul class=\"profile-trust__topics\"><li>Product Leadership</li><li>Loyalty &amp; CRM</li><li>E-Commerce</li><li>Organisationsdiagnose</li></ul>
    <p>Meine Erfahrung reicht von digitalen Produkt- und Plattformorganisationen über Transformation und Stabilisierung bis zur Führung in komplexen, bereichsübergreifenden Situationen.</p>
    <p class=\"profile-signature\">Christian Leonhardt · Klarheit für komplexe Produktorganisationen.</p>
    <p><a href=\"/ueber-mich\">Mehr über meinen Hintergrund →</a></p>
  </div>
</div>""")

Node homeCta = findByTemplate(homeArea, 'meine-website:components/callToAction', true)
homeCta.setProperty('buttonText', 'Clarity Session ansehen')
setInternalLink(homeCta, 'pageLinkChooser', '/clarity-session')

Node about = website.getNode('/ueber-mich')
Node aboutArea = about.getNode('main')
Node aboutIntro = findByHeading(aboutArea, 'Christian Leonhardt')
aboutIntro.setProperty('text', """<div class=\"profile-trust profile-trust--about\">
  <figure class=\"profile-trust__media\">${picture}</figure>
  <div class=\"profile-trust__content\">
    <p class=\"mvp-meta\">Christian Leonhardt</p>
    <h3>Product Leadership, Organisationsdiagnose und Transformation</h3>
    <p class=\"lead\">Ich begleite Produktverantwortliche, Führungsteams und digitale Organisationen in Situationen, in denen Methodenwissen allein nicht weiterhilft.</p>
    <p>Meine Arbeit liegt an den Übergängen: zwischen Strategie und Umsetzung, Produkt und Technologie, Verantwortung und formaler Struktur, Wachstum und notwendiger Neuordnung. Gerade dort entstehen Reibung, Verzögerung und Missverständnisse – aber auch die größten Hebel.</p>
    <p>Diese Perspektive ist nicht am Schreibtisch entstanden. Ich habe Produktorganisationen aufgebaut und weiterentwickelt, kritische Programme stabilisiert, internationale Stakeholder zusammengebracht und Verantwortung in Situationen übernommen, in denen Entscheidungen unter Unsicherheit getroffen werden mussten.</p>
    <ul class=\"profile-trust__topics\"><li>Product Leadership</li><li>Loyalty &amp; CRM</li><li>E-Commerce</li><li>Organisationsdiagnose</li></ul>
    <p>Statt vorschnell ein Zielbild über die Organisation zu legen, arbeite ich zuerst heraus, welches Problem tatsächlich gelöst werden muss. Daraus entstehen klare Entscheidungen und Veränderungen, die zum Kontext passen.</p>
    <p class=\"profile-signature\">Diagnose vor Eingriff. Klarheit vor Aktion.</p>
  </div>
</div>""")

home.setProperty('dateModified', '2026-07-31')
about.setProperty('dateModified', '2026-07-31')
website.save()

['/start', '/ueber-mich'].each { path ->
    Node page = website.getNode(path)
    ['mgnl:lastActivatedVersion', 'mgnl:lastActivatedVersionCreated'].each { name ->
        if (page.hasProperty(name)) page.getProperty(name).remove()
    }
}
website.save()

['/start', '/ueber-mich'].each { path ->
    def parameters = new LinkedHashMap<String, Object>()
    parameters.put('repository', 'website')
    parameters.put('path', path)
    parameters.put('recursive', true)
    def result = commands.executeCommand('default', 'publish', parameters)
    println "${path}: ${result ? 'veröffentlicht' : 'Veröffentlichung ausgelöst'}"
}

println 'Profilbild, Kurzsignatur, Erfahrungsverdichtung und konkrete Clarity-Session-CTAs wurden aktualisiert.'
