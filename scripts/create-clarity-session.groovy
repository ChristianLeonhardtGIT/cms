import info.magnolia.context.MgnlContext
import groovy.transform.Field
import javax.jcr.Node
import javax.jcr.Session

/*
 * Einmalig in Magnolias Groovy-App ausführen.
 * Das Skript ist wiederholbar und ersetzt ausschließlich den Inhalt der
 * Clarity-Session-Seite. Bestehende Clarity-Session-Teaser werden auf die
 * neue Landingpage verlinkt.
 */

@Field final String PAGE_TEMPLATE = 'meine-website:pages/home'
@Field final String COMPONENT_PREFIX = 'meine-website:components/'
@Field final String CLARITY_PATH = '/clarity-session'

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

Node addHero(Node area, String eyebrow, String title, String description) {
    addComponent(area, 'hero', [
        eyebrow: eyebrow,
        title: title,
        description: description
    ])
}

Node addText(Node area, String heading, String html, boolean tinted = false) {
    addComponent(area, 'text', [
        heading: heading,
        text: html,
        showBackground: tinted
    ])
}

Node addFormEdit(Node fields, String name, String title, boolean mandatory = false, long rows = 1, String inputType = 'text', String description = null) {
    addComponent(fields, 'form:components/formEdit', [
        title: title,
        controlName: name,
        mandatory: mandatory,
        rows: rows,
        inputType: inputType,
        description: description
    ])
}

Node createClarityForm(Node area) {
    Node form = addComponent(area, 'contactForm', [
        formName: 'clarity-session-start',
        formTitle: 'Clarity Session unverbindlich anfragen',
        formText: 'Drei kurze Angaben reichen für den Einstieg. Ich prüfe persönlich, ob die Clarity Session für Ihre Situation passt, und melde mich mit einer ersten Einordnung.',
        requiredSymbol: '*',
        rightText: 'Pflichtfeld',
        errorTitle: 'Bitte prüfen Sie Ihre Angaben',
        successTitle: 'Danke für Ihre Anfrage',
        successMessage: 'Ihre Angaben wurden übermittelt. Ich melde mich persönlich bei Ihnen.',
        trackMail: true,
        contactMailFrom: 'kontakt@cleonhardt.de',
        contactMailTo: 'kontakt@cleonhardt.de',
        contactMailSubject: 'Neue Anfrage zur Clarity Session von ${name}',
        contentType: 'text',
        contactMailBody: '''Neue Anfrage zur Clarity Session über cleonhardt.de

Name: ${name}
E-Mail: ${email}

Situation:
${situation}

Datenschutz: ${datenschutz}
'''
    ])

    Node fieldsets = form.addNode('fieldsets', 'mgnl:area')
    Node group = addComponent(fieldsets, 'form:components/formGroupFields', [title: 'Ihr Einstieg'])
    Node fields = group.addNode('fields', 'mgnl:area')

    addFormEdit(fields, 'name', 'Name', true, 1, 'text')
    addFormEdit(fields, 'email', 'E-Mail', true, 1, 'email')
    addFormEdit(
        fields,
        'situation',
        'Welche konkrete Situation oder Entscheidung möchten Sie klären?',
        true,
        5,
        'text',
        'Bitte noch keine vertraulichen Detailinformationen übermitteln. Zwei bis fünf Sätze reichen.'
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
        buttonText: 'Clarity Session anfragen'
    ])
    form
}

void updateClarityLinks(Node node) {
    if (node.hasProperty('mgnl:template')) {
        String template = node.getProperty('mgnl:template').string
        String title = node.hasProperty('title') ? node.getProperty('title').string : ''
        String description = node.hasProperty('description') ? node.getProperty('description').string : ''
        if (template == 'meine-website:components/callToAction' &&
            (title.contains('Clarity') || description.contains('Clarity Session'))) {
            Node chooser = node.hasNode('pageLinkChooser')
                ? node.getNode('pageLinkChooser')
                : node.addNode('pageLinkChooser', 'mgnl:contentNode')
            chooser.setProperty('field', 'internalPageLink')
            chooser.setProperty('internalLink', CLARITY_PATH)
            if (node.hasProperty('buttonText')) {
                node.setProperty('buttonText', 'Clarity Session ansehen')
            }
        }
    }

    node.nodes.each { Node child -> updateClarityLinks(child) }
}

Node root = website.rootNode
Node page = root.hasNode('clarity-session')
    ? root.getNode('clarity-session')
    : root.addNode('clarity-session', 'mgnl:page')

removeChild(page, 'main')
setProperties(page, [
    'mgnl:template': PAGE_TEMPLATE,
    title: 'ChOS Clarity Session',
    navigationTitle: 'Clarity Session',
    windowTitle: 'ChOS Clarity Session – Klarheit für eine konkrete Entscheidung',
    metaDescription: '90 Minuten strukturierte Analyse für eine konkrete Produkt-, Führungs- oder Organisationssituation – mit Vorbereitung und schriftlichem Clarity Brief.',
    hideInNavigation: true,
    brandName: 'Christian Leonhardt',
    footerText: 'Product Leadership · Organisation · Transformation'
])

Node main = page.addNode('main', 'mgnl:area')

addHero(
    main,
    'ChOS Clarity Session · 90 Minuten · 295 € netto',
    'Klarheit, bevor Sie die nächste Maßnahme starten.',
    'Bringen Sie eine festgefahrene Produkt-, Führungs- oder Organisationssituation mit. Sie erhalten ein strukturiertes Lagebild, erste überprüfbare Hypothesen und einen konkreten nächsten Schritt.'
)

addText(main, 'Ein fokussierter Einstieg für ein konkretes Problem', '''
<div class="section-intro"><p class="lead">Die Clarity Session ist richtig, wenn Sie merken, dass Aktion allein nicht weiterführt – und Sie vor dem nächsten Eingriff Ursache, Annahmen und Entscheidungsraum sauber trennen möchten.</p></div>
<div class="clarity-facts" aria-label="Rahmen der Clarity Session">
  <div><span>Format</span><strong>Vorbereitung + 90 Minuten Gespräch</strong></div>
  <div><span>Ergebnis</span><strong>Schriftlicher Clarity Brief</strong></div>
  <div><span>Preis</span><strong>295 € netto</strong></div>
</div>
<p class="clarity-actions"><a class="button" href="#clarity-session-start">Session unverbindlich anfragen</a><a href="#beispiel-brief">Beispiel-Brief ansehen</a></p>
<p><small>Für umfangreiche Dokumentenprüfungen, mehrere Beteiligte oder eine Organisationsdiagnose klären wir vorab ein passenderes Format.</small></p>''', true)

addText(main, 'Was Sie bekommen', '''
<div class="mvp-grid mvp-grid--3">
  <article class="mvp-panel"><span class="mvp-number">1</span><h3>Vorbereitung</h3><p>Ein kurzer Fragebogen schärft Situation, Kontext und die Entscheidung, die am Ende leichter fallen soll.</p></article>
  <article class="mvp-panel"><span class="mvp-number">2</span><h3>Analysegespräch</h3><p>In 90 Minuten trennen wir Beobachtung und Deutung, prüfen alternative Erklärungen und machen relevante Wechselwirkungen sichtbar.</p></article>
  <article class="mvp-panel mvp-panel--accent"><span class="mvp-number">3</span><h3>Clarity Brief</h3><p>Sie erhalten das strukturierte Lagebild, erste Hypothesen, offene Fragen und einen konkreten nächsten Schritt schriftlich.</p></article>
</div>
<blockquote class="mvp-quote">Sie kaufen keinen reinen Gesprächstermin, sondern Vorbereitung, Analyse und eine belastbare Grundlage für die nächste Entscheidung.</blockquote>''')

addText(main, 'Drei Situationen, in denen Klarheit den Unterschied macht', '''
<p class="lead">Die folgenden Beispiele sind anonymisierte, typische Fallmuster aus Produkt- und Organisationsarbeit. Sie sind keine Kundenreferenzen und versprechen kein vorab feststehendes Ergebnis.</p>
<div class="clarity-cases">
  <article class="clarity-case">
    <p class="mvp-meta">Fallmuster 01 · Entscheidungen</p>
    <h3>Eine Entscheidung dreht immer neue Schleifen</h3>
    <dl>
      <div><dt>Typische Ausgangslage</dt><dd>Produkt, Tech, Business und Führung stimmen formal zu. Trotzdem wird dieselbe Entscheidung in jedem Gremium erneut geöffnet.</dd></div>
      <div><dt>Analysefokus</dt><dd>Ist die Rolle unklar – oder fehlen gemeinsame Entscheidungskriterien, belastbare Informationen oder ein akzeptiertes Mandat?</dd></div>
      <div><dt>Möglicher nächster Schritt</dt><dd>Eine konkrete Entscheidung mit Mandat, Konsultation, Kriterien und Eskalationsbedingung für einen begrenzten Zeitraum testen.</dd></div>
    </dl>
  </article>
  <article class="clarity-case">
    <p class="mvp-meta">Fallmuster 02 · Priorisierung</p>
    <h3>Die Roadmap ist voll, aber die Wirkung bleibt unklar</h3>
    <dl>
      <div><dt>Typische Ausgangslage</dt><dd>Teams sind ausgelastet, neue Anforderungen kommen laufend hinzu und Prioritäten werden mit hoher Dringlichkeit verteidigt.</dd></div>
      <div><dt>Analysefokus</dt><dd>Fehlt ein Priorisierungsprozess – oder sind strategische Zielkonflikte und echte Verzichtsentscheidungen bisher unsichtbar?</dd></div>
      <div><dt>Möglicher nächster Schritt</dt><dd>Eine anstehende Priorisierung anhand weniger gemeinsamer Wirkungskriterien und expliziter Trade-offs entscheiden.</dd></div>
    </dl>
  </article>
  <article class="clarity-case">
    <p class="mvp-meta">Fallmuster 03 · Reorganisation</p>
    <h3>Die Struktur ist neu, die alten Abhängigkeiten bleiben</h3>
    <dl>
      <div><dt>Typische Ausgangslage</dt><dd>Rollen und Teams wurden neu zugeschnitten. Entscheidungen, Abstimmungen und Eskalationen folgen dennoch dem alten Muster.</dd></div>
      <div><dt>Analysefokus</dt><dd>Welche informellen Beziehungen, Informationswege oder Anreize stabilisieren das bisherige Verhalten?</dd></div>
      <div><dt>Möglicher nächster Schritt</dt><dd>Eine wiederkehrende Übergabe oder Entscheidung auswählen und den kleinsten reversiblen Eingriff mit klarer Lernfrage definieren.</dd></div>
    </dl>
  </article>
</div>''', true)

addText(main, 'So kann Ihr Clarity Brief aussehen', '''
<p class="lead">Das Ergebnisdokument verdichtet die Session auf das, was für die nächste Entscheidung zählt. Dieses Beispiel zeigt Struktur und Detailtiefe anhand einer vollständig fiktiven Situation.</p>
<article class="clarity-brief" id="beispiel-brief" aria-labelledby="clarity-brief-title">
  <header class="clarity-brief__header">
    <div><p class="mvp-meta">Beispiel · Fiktive Situation</p><h3 id="clarity-brief-title">Prioritäten wechseln – Teams verlieren Entscheidungssicherheit</h3></div>
    <span>Clarity Brief</span>
  </header>
  <div class="clarity-brief__summary">
    <p><strong>Entscheidungsfrage</strong><br>Wie können neue Anforderungen aufgenommen werden, ohne laufende Prioritäten jedes Mal vollständig neu zu verhandeln?</p>
    <p><strong>Betrachteter Zeitraum</strong><br>Nächste 14 Tage</p>
  </div>
  <div class="clarity-brief__sections">
    <section><h4>1. Beobachtbare Signale</h4><ul><li>Drei laufende Initiativen wurden innerhalb eines Monats neu priorisiert.</li><li>Die Gründe unterscheiden sich je nach Gremium.</li><li>Teams warten vor wichtigen Zusagen auf informelle Freigaben.</li></ul></section>
    <section><h4>2. Arbeitshypothesen</h4><ul><li>Es fehlt weniger an Priorisierung als an einem gemeinsamen Umgang mit Zielkonflikten.</li><li>Das formale Mandat wird durch informelle Eskalationswege überlagert.</li><li>Die Wirkung neuer Anforderungen ist nicht mit den Kosten des Unterbrechens vergleichbar.</li></ul></section>
    <section><h4>3. Offene Fragen</h4><ul><li>Wer darf eine laufende Priorität unter welchen Bedingungen verändern?</li><li>Welche Information ist bei der Entscheidung regelmäßig nicht verfügbar?</li><li>Woran würde nach zwei Wochen sichtbar, dass mehr Entscheidungssicherheit entstanden ist?</li></ul></section>
    <section class="clarity-brief__recommendation"><h4>4. Empfohlener nächster Test</h4><p>Für eine ausgewählte Produktlinie gilt 14 Tage lang ein gemeinsames Änderungsfenster: Neue Anforderungen werden anhand von Wirkung, Dringlichkeit und Unterbrechungskosten bewertet. Außerhalb des Fensters entscheidet eine benannte Rolle nur bei vorher vereinbarten Ausnahmen.</p></section>
    <section><h4>5. Leitplanken und Review</h4><p>Keine Änderung bestehender Rollen oder Gremien im Testzeitraum. Review nach 14 Tagen anhand dokumentierter Prioritätsänderungen, Wartezeiten und strittiger Ausnahmen.</p></section>
  </div>
  <footer><p>Hinweis: Dieses Beispiel ist keine Beratung für einen realen Fall. Hypothesen werden in einer tatsächlichen Session anhand Ihrer Situation geprüft und ausdrücklich als Hypothesen gekennzeichnet.</p></footer>
</article>''')

createClarityForm(main)

updateClarityLinks(root)
website.save()

println 'Clarity-Session-Landingpage, Kurzformular, drei Fallmuster und Beispiel-Clarity-Brief wurden angelegt.'
println 'Bestehende Clarity-Session-Teaser verlinken nun auf /clarity-session.'
println 'Bitte /clarity-session sowie geänderte Seiten mit Clarity-Teasern veröffentlichen.'
