import info.magnolia.context.MgnlContext
import groovy.transform.Field
import javax.jcr.Node
import javax.jcr.Session

/*
 * Erstellt die zentrale Seite /angebot-anfragen, ergänzt die Leistungsseite
 * um konkrete Angebots-CTAs und erweitert den Datenschutzhinweis.
 * Das Skript ist wiederholbar.
 */

@Field final String PAGE_TEMPLATE = 'meine-website:pages/home'
@Field final String COMPONENT_PREFIX = 'meine-website:components/'
@Field final String QUOTE_PATH = '/angebot-anfragen'

Session website = MgnlContext.getJCRSession('website')

void setProperties(Node node, Map<String, Object> properties) {
    properties.each { String key, Object value ->
        if (value != null) {
            node.setProperty(key, value)
        }
    }
}

void removeChild(Node parent, String name) {
    if (parent.hasNode(name)) {
        parent.getNode(name).remove()
    }
}

Node addComponent(Node area, String template, Map<String, Object> properties = [:]) {
    String name = String.format('%02d', area.nodes.size)
    Node component = area.addNode(name, 'mgnl:component')
    component.setProperty('mgnl:template', template.contains(':') ? template : COMPONENT_PREFIX + template)
    setProperties(component, properties)
    component
}

Node addText(Node area, String heading, String html, boolean tinted = false) {
    addComponent(area, 'text', [
        heading: heading,
        text: html,
        showBackground: tinted
    ])
}

Node addFormEdit(
    Node fields,
    String name,
    String title,
    boolean mandatory = false,
    long rows = 1,
    String inputType = 'text',
    String description = null
) {
    addComponent(fields, 'form:components/formEdit', [
        title: title,
        controlName: name,
        mandatory: mandatory,
        rows: rows,
        inputType: inputType,
        description: description
    ])
}

Node findByProperty(Node root, String propertyName, String propertyValue) {
    if (root.hasProperty(propertyName) && root.getProperty(propertyName).string == propertyValue) {
        return root
    }
    for (Node child : root.nodes) {
        Node match = findByProperty(child, propertyName, propertyValue)
        if (match != null) {
            return match
        }
    }
    null
}

Node findByTemplate(Node root, String template) {
    if (root.hasProperty('mgnl:template') &&
        root.getProperty('mgnl:template').string == template) {
        return root
    }
    for (Node child : root.nodes) {
        Node match = findByTemplate(child, template)
        if (match != null) {
            return match
        }
    }
    null
}

String addOfferCta(String html, String title, String offerKey) {
    if (html.contains("leistung=${offerKey}")) {
        return html
    }

    String heading = "<h3>${title}</h3>"
    int headingPosition = html.indexOf(heading)
    if (headingPosition < 0) {
        return html
    }

    int cardStart = html.lastIndexOf('<article', headingPosition)
    int cardEnd = html.indexOf('</article>', headingPosition)
    if (cardStart < 0 || cardEnd < 0) {
        return html
    }

    String cta = """<p class="mvp-panel__action"><a class="mvp-offer-cta" href="/magnoliaPublic${QUOTE_PATH}?leistung=${offerKey}">Angebot anfragen <span aria-hidden="true">→</span></a></p>"""
    html.substring(0, cardEnd) + cta + html.substring(cardEnd)
}

Node createQuoteForm(Node area) {
    Node form = addComponent(area, 'contactForm', [
        formName: 'angebot-anfragen',
        formTitle: 'Dein unverbindliches Angebot',
        formText: 'Beschreibe kurz, was du klären oder verändern möchtest. Ich prüfe deine Angaben persönlich und melde mich innerhalb von zwei Werktagen. Durch die Anfrage entsteht noch kein Vertrag.',
        requiredSymbol: '*',
        rightText: 'Pflichtfeld',
        errorTitle: 'Bitte prüfe deine Angaben',
        successTitle: 'Danke für deine Angebotsanfrage',
        successMessage: 'Deine Angaben wurden übermittelt. Ich prüfe deine Anfrage persönlich und melde mich innerhalb von zwei Werktagen.',
        trackMail: true,
        contactMailFrom: 'kontakt@cleonhardt.de',
        contactMailTo: 'kontakt@cleonhardt.de',
        contactMailSubject: 'Neue Angebotsanfrage von ${name}',
        contentType: 'text',
        contactMailBody: '''Neue Angebotsanfrage über cleonhardt.de

Leistung: ${leistung}
Name: ${name}
E-Mail: ${email}
Telefon: ${telefon}
Unternehmen: ${unternehmen}
Position: ${rolle}
Zeitraum: ${zeitraum}
Anzahl Beteiligte: ${beteiligte}

Situation und gewünschtes Ergebnis:
${situation}

Datenschutz: ${datenschutz}
'''
    ])

    Node fieldsets = form.addNode('fieldsets', 'mgnl:area')
    Node group = addComponent(fieldsets, 'form:components/formGroupFields', [title: 'Angaben für dein Angebot'])
    Node fields = group.addNode('fields', 'mgnl:area')

    addComponent(fields, 'form:components/formSelection', [
        title: 'Gewünschte Leistung',
        controlName: 'leistung',
        type: 'select',
        mandatory: true,
        horizontal: false,
        multiple: false,
        labels: '''Bitte auswählen:
ChOS Clarity Session:clarity-session
Decision Review:decision-review
Zweiwöchiges Leadership- oder Product-Sparring:sparring
Monatliches Executive Sparring:executive-sparring
ChOS Quick Diagnostic:quick-diagnostic
Product Organisation Diagnostic:product-organisation-diagnostic
Workshop:workshop
Noch unsicher:noch-unsicher'''
    ])

    addFormEdit(fields, 'name', 'Vor- und Nachname', true, 1, 'text')
    addFormEdit(fields, 'email', 'Geschäftliche E-Mail-Adresse', true, 1, 'email')
    addFormEdit(fields, 'telefon', 'Telefonnummer – optional', false, 1, 'tel')
    addFormEdit(fields, 'unternehmen', 'Unternehmen oder Organisation – optional', false, 1, 'text')
    addFormEdit(fields, 'rolle', 'Position oder Rolle – optional', false, 1, 'text')
    addFormEdit(
        fields,
        'situation',
        'Welche Situation möchtest du klären und welches Ergebnis wünschst du?',
        true,
        6,
        'text',
        'Bitte übermittle noch keine vertraulichen oder besonders sensiblen Informationen. Einige Sätze reichen für die erste Einordnung.'
    )

    addComponent(fields, 'form:components/formSelection', [
        title: 'Gewünschter Zeitraum – optional',
        controlName: 'zeitraum',
        type: 'select',
        mandatory: false,
        horizontal: false,
        multiple: false,
        labels: '''Noch offen:
So bald wie möglich:so-bald-wie-moeglich
Innerhalb der nächsten 2 bis 4 Wochen:2-4-wochen
Innerhalb der nächsten 1 bis 3 Monate:1-3-monate
Später als in 3 Monaten:spaeter'''
    ])

    addFormEdit(
        fields,
        'beteiligte',
        'Wie viele Personen sind voraussichtlich beteiligt? – optional',
        false,
        1,
        'text'
    )

    addComponent(fields, 'form:components/formSelection', [
        title: 'Datenschutz',
        controlName: 'datenschutz',
        type: 'checkbox',
        mandatory: true,
        horizontal: false,
        multiple: false,
        labels: 'Ich habe die Datenschutzerklärung zur Kenntnis genommen.:akzeptiert'
    ])

    addComponent(fields, 'form:components/formHoneypot', [
        title: 'Bitte nicht ausfüllen',
        controlName: 'website-url'
    ])
    addComponent(fields, 'form:components/formSubmit', [
        buttonText: 'Angebot unverbindlich anfragen'
    ])
    form
}

Node page = website.nodeExists(QUOTE_PATH)
    ? website.getNode(QUOTE_PATH)
    : website.rootNode.addNode('angebot-anfragen', 'mgnl:page')

removeChild(page, 'main')
setProperties(page, [
    'mgnl:template': PAGE_TEMPLATE,
    title: 'Angebot anfragen',
    navigationTitle: 'Angebot anfragen',
    windowTitle: 'Unverbindliches Angebot anfragen – Christian Leonhardt',
    metaDescription: 'Frag ein unverbindliches Angebot für Clarity Session, Sparring, Diagnostik oder Workshop an. Persönlich geprüft und passend zu deiner Situation.',
    dateModified: '2026-09-02',
    hideInNavigation: true,
    brandName: 'Christian Leonhardt',
    footerText: 'Product Leadership · Organisation · Transformation'
])

Node main = page.addNode('main', 'mgnl:area')
addComponent(main, 'hero', [
    eyebrow: 'Angebot anfragen',
    title: 'Ein passender Rahmen beginnt mit einer klaren Anfrage.',
    description: 'Wähle das gewünschte Format und beschreibe kurz deine Situation. Du erhältst kein automatisches Standardangebot, sondern eine persönliche Rückmeldung und einen nachvollziehbaren Vorschlag für Umfang und nächsten Schritt.'
])

addText(main, 'So geht es weiter', '''
<div class="quote-process">
  <article class="mvp-panel"><span class="mvp-number">1</span><h3>Anfrage</h3><p>Du nennst das gewünschte Format und gibst mir den notwendigen Kontext für eine erste Einordnung.</p></article>
  <article class="mvp-panel"><span class="mvp-number">2</span><h3>Rückfragen</h3><p>Falls Umfang, Beteiligte oder Zielbild noch offen sind, klären wir diese Punkte in einem kurzen persönlichen Gespräch.</p></article>
  <article class="mvp-panel mvp-panel--accent"><span class="mvp-number">3</span><h3>Angebot</h3><p>Du erhältst einen konkreten Vorschlag mit Leistungsumfang, Ergebnis, Zeitrahmen und transparentem Nettopreis.</p></article>
</div>
<div class="quote-note">
  <strong>Unverbindlich und persönlich geprüft</strong>
  <p>Die Anfrage löst keine Bestellung aus. Ein Vertrag entsteht erst, wenn du ein anschließend übermitteltes Angebot ausdrücklich annimmst.</p>
</div>''', true)

createQuoteForm(main)

addText(main, 'Du bist beim Format noch unsicher?', '''
<p>Wähle im Formular „Noch unsicher“. Entscheidend ist nicht, dass du das passende Produkt bereits kennst, sondern dass die Situation und das gewünschte Ergebnis nachvollziehbar werden.</p>
<p>Alternativ erreichst du mich direkt unter <a href="mailto:kontakt@cleonhardt.de">kontakt@cleonhardt.de</a>.</p>''')

Node servicesPage = website.getNode('/leistungen')
servicesPage.setProperty('dateModified', '2026-09-02')
for (Node component : servicesPage.getNode('main').nodes) {
    if (!component.hasProperty('text')) {
        continue
    }
    String html = component.getProperty('text').string
    html = addOfferCta(html, 'Decision Review', 'decision-review')
    html = addOfferCta(html, 'Leadership- oder Product-Sparring', 'sparring')
    html = addOfferCta(html, 'Executive Sparring', 'executive-sparring')
    html = addOfferCta(html, 'ChOS Quick Diagnostic', 'quick-diagnostic')
    html = addOfferCta(html, 'Product Organisation Diagnostic', 'product-organisation-diagnostic')
    html = addOfferCta(html, 'Workshops', 'workshop')
    component.setProperty('text', html)
}

Node finalCta = findByProperty(servicesPage, 'title', 'Du musst das Format nicht vorab kennen.')
if (finalCta != null) {
    finalCta.setProperty('buttonText', 'Angebot anfragen')
    Node chooser = finalCta.hasNode('pageLinkChooser')
        ? finalCta.getNode('pageLinkChooser')
        : finalCta.addNode('pageLinkChooser', 'mgnl:contentNode')
    chooser.setProperty('field', 'internalPageLink')
    chooser.setProperty('internalLink', QUOTE_PATH)
}

Node privacyPage = website.getNode('/datenschutz')
Node privacyText = findByTemplate(privacyPage, 'meine-website:components/text')
if (privacyText != null && privacyText.hasProperty('text')) {
    String privacyHtml = privacyText.getProperty('text').string
    privacyHtml = privacyHtml.replace(
        '<h3>4. Kontaktformular und HubSpot CRM</h3>',
        '<h3>4. Kontakt- und Angebotsformulare sowie HubSpot CRM</h3>'
    )
    privacyHtml = privacyHtml.replace(
        'Verarbeitet werden Name, E-Mail-Adresse, optional Unternehmen und berufliche Rolle, der gewählte Anlass, Ihre Beschreibung der aktuellen Situation und des gewünschten Ergebnisses, Zeitpunkt und Herkunftsseite der Anfrage sowie technisch erforderliche Verbindungs- und Anfragedaten.',
        'Verarbeitet werden Name, E-Mail-Adresse, optional Telefonnummer, Unternehmen und berufliche Rolle, der gewählte Anlass oder die angefragte Leistung, Ihre Beschreibung der aktuellen Situation und des gewünschten Ergebnisses, optional gewünschter Zeitraum und Zahl der Beteiligten, Zeitpunkt und Herkunftsseite der Anfrage sowie technisch erforderliche Verbindungs- und Anfragedaten.'
    )
    privacyText.setProperty('text', privacyHtml)
}

website.save()

println 'Die Seite /angebot-anfragen wurde erstellt oder aktualisiert.'
println 'Die Leistungsseite enthält nun sechs konkrete Angebots-CTAs.'
println 'Der Datenschutzhinweis wurde um die Angebotsanfrage ergänzt.'
println 'Bitte /angebot-anfragen, /leistungen und /datenschutz veröffentlichen.'
