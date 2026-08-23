import info.magnolia.context.MgnlContext
import javax.jcr.Node
import javax.jcr.Session

/*
 * Einmalig auf der Author-Instanz in Magnolias Groovy-App ausführen.
 * Das Skript ändert ausschließlich die Mail-Einstellungen des Kontaktformulars.
 */

Session website = MgnlContext.getJCRSession('website')
Node contactPage = website.getNode('/kontakt')
Node contactForm

Closure findContactForm
findContactForm = { Node node ->
    if (node.hasProperty('mgnl:template') &&
        node.getProperty('mgnl:template').string == 'meine-website:components/contactForm') {
        contactForm = node
        return
    }

    def children = node.nodes
    while (children.hasNext() && contactForm == null) {
        findContactForm(children.nextNode())
    }
}

findContactForm(contactPage)

if (contactForm == null) {
    throw new IllegalStateException('Kontaktformular unter /kontakt wurde nicht gefunden.')
}

[
    contactMailFrom: 'kontakt@cleonhardt.de',
    contactMailTo: 'kontakt@cleonhardt.de',
    contactMailSubject: 'Neue Website-Anfrage von ${name}',
    contentType: 'text',
    contactMailBody: '''Neue Anfrage über cleonhardt.de

Anliegen: ${anliegen}
Name: ${name}
E-Mail: ${email}
Rolle / Position: ${rolle}
Unternehmen: ${unternehmen}

Aktuelle Situation:
${situation}

Gewünschtes Ergebnis:
${ziel}

Datenschutz: ${datenschutz}
'''
].each { String name, Object value ->
    contactForm.setProperty(name, value)
}

website.save()

println "Kontaktformular ${contactForm.path} wurde für kontakt@cleonhardt.de konfiguriert."
