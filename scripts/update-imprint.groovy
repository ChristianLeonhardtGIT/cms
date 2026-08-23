import info.magnolia.context.MgnlContext
import javax.jcr.Node
import javax.jcr.Session

/*
 * In Magnolias Groovy-App auf der Author-Instanz ausführen.
 * Aktualisiert ausschließlich die Impressumsseite.
 */

Session website = MgnlContext.getJCRSession('website')
Node imprintPage = website.getNode('/impressum')

Node findByTemplate(Node root, String template) {
    if (root.hasProperty('mgnl:template') &&
        root.getProperty('mgnl:template').string == template) {
        return root
    }

    def children = root.nodes
    while (children.hasNext()) {
        Node match = findByTemplate(children.nextNode(), template)
        if (match != null) {
            return match
        }
    }
    null
}

Node textComponent = findByTemplate(imprintPage, 'meine-website:components/text')
if (textComponent == null) {
    throw new IllegalStateException('Textkomponente unter /impressum wurde nicht gefunden.')
}

String imprintHtml = '''
<h3>Angaben gemäß § 5 DDG</h3>
<p>Christian Leonhardt<br>Am Spelzgarten 18<br>50129 Bergheim<br>Deutschland</p>
<h3>Kontakt</h3>
<p>Telefon: <a href="tel:+4915259765342">0152 59765342</a><br>E-Mail: <a href="mailto:kontakt@cleonhardt.de">kontakt@cleonhardt.de</a></p>
<h3>Verantwortlich für journalistisch-redaktionelle Inhalte gemäß § 18 Abs. 2 MStV</h3>
<p>Christian Leonhardt<br>Am Spelzgarten 18<br>50129 Bergheim<br>Deutschland</p>
'''

imprintPage.setProperty('windowTitle', 'Impressum – Christian Leonhardt')
imprintPage.setProperty('metaDescription', 'Impressum und Anbieterkennzeichnung von Christian Leonhardt.')
textComponent.setProperty('heading', 'Impressum')
textComponent.setProperty('text', imprintHtml.trim())

website.save()

println 'Impressumsseite wurde aktualisiert.'
