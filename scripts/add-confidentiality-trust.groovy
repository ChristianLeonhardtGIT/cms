import info.magnolia.commands.CommandsManager
import info.magnolia.context.MgnlContext
import groovy.transform.Field
import javax.jcr.Node

def website = MgnlContext.getJCRSession('website')
website.refresh(false)

@Field final String template = 'meine-website:components/text'
@Field final String trustName = 'trust-confidentiality'

Node addTrust(Node area, String html, boolean detailed, String beforeName = null) {
    if (area.hasNode(trustName)) area.getNode(trustName).remove()
    Node trust = area.addNode(trustName, 'mgnl:component')
    trust.setProperty('mgnl:template', template)
    trust.setProperty('heading', 'Vertraulich von Anfang an.')
    trust.setProperty('text', html)
    trust.setProperty('showBackground', detailed)
    if (beforeName) area.orderBefore(trustName, beforeName)
    trust
}

String compactTrust = '''
<p class="lead">Ihre Situation, Unterlagen und Gespräche behandle ich strikt vertraulich. Auf Wunsch vereinbaren wir bereits vor dem ersten Austausch eine gegenseitige Vertraulichkeitsvereinbarung (NDA). Personenbezogene und unternehmensinterne Informationen werden nur verarbeitet, soweit es für die Zusammenarbeit erforderlich ist.</p>'''

String detailedTrust = '''
<p class="lead">Komplexe Führungs- und Organisationsfragen brauchen einen geschützten Rahmen. Deshalb ist Vertraulichkeit von Beginn an Teil der Zusammenarbeit.</p>
<div class="mvp-grid mvp-grid--3">
  <article class="mvp-panel"><h3>Diskreter Rahmen</h3><p>Ihre Situation, Unterlagen und Gespräche behandle ich strikt vertraulich. Inhalte bleiben im vereinbarten Kreis und werden nicht ohne Ihre Zustimmung als Referenz oder Praxisfall verwendet.</p></article>
  <article class="mvp-panel mvp-panel--accent"><h3>NDA vorab möglich</h3><p>Auf Wunsch vereinbaren wir bereits vor dem Austausch sensibler Details eine gegenseitige Vertraulichkeitsvereinbarung. Eine vorbereitete NDA kann dafür kurzfristig bereitgestellt werden.</p></article>
  <article class="mvp-panel"><h3>So wenig Daten wie nötig</h3><p>Für den Einstieg genügt der notwendige Kontext. Personenbezogene und unternehmensinterne Informationen werden nur verarbeitet, soweit sie für die Zusammenarbeit tatsächlich erforderlich sind.</p></article>
</div>'''

Node homeArea = website.getNode('/start/main')
String homeBefore = null
List<Node> homeComponents = []
for (Node component : homeArea.nodes) homeComponents << component
homeComponents.eachWithIndex { Node component, int index ->
    if (component.hasProperty('heading') && component.getProperty('heading').string == 'Mit einer konkreten Situation starten.') {
        if (index + 1 < homeComponents.size()) homeBefore = homeComponents[index + 1].getName()
    }
}
if (homeBefore == null) throw new IllegalStateException('Priorisierter Angebotsblock auf /start nicht gefunden.')
addTrust(homeArea, compactTrust, false, homeBefore)

['/clarity-session', '/executive-sparring'].each { String path ->
    Node area = website.getNode(path + '/main')
    String ctaName = null
    for (Node component : area.nodes) {
        if (component.hasProperty('mgnl:template') && component.getProperty('mgnl:template').string == 'meine-website:components/callToAction') {
            ctaName = component.getName()
            break
        }
    }
    addTrust(area, detailedTrust, true, ctaName)
}

void addOrUpdateFaq(Node area) {
    Node faq = null
    for (Node component : area.nodes) {
        if (component.hasProperty('mgnl:template') && component.getProperty('mgnl:template').string == 'meine-website:components/faq') {
            faq = component
            break
        }
    }
    if (faq == null || !faq.hasNode('items')) return
    Node items = faq.getNode('items')
    Node entry = null
    for (Node item : items.nodes) {
        if (item.hasProperty('question') && item.getProperty('question').string == 'Wie vertraulich ist die Zusammenarbeit?') {
            entry = item
            break
        }
    }
    if (entry == null) entry = items.addNode(String.format('%02d', items.nodes.size), 'mgnl:contentNode')
    entry.setProperty('question', 'Wie vertraulich ist die Zusammenarbeit?')
    entry.setProperty('answer', 'Ihre Situation, Unterlagen und Gespräche werden strikt vertraulich behandelt. Auf Wunsch kann bereits vor dem Austausch sensibler Details eine gegenseitige Vertraulichkeitsvereinbarung (NDA) geschlossen werden. Für den Einstieg werden nur die Informationen benötigt, die zur Einordnung der Situation erforderlich sind.')
}

['/start', '/clarity-session', '/executive-sparring'].each { String path ->
    addOrUpdateFaq(website.getNode(path + '/main'))
}

Node contactArea = website.getNode('/kontakt/main')
for (Node component : contactArea.nodes) {
    if (!component.hasProperty('heading') || component.getProperty('heading').string != 'So geht es weiter') continue
    String html = component.getProperty('text').string
    html = html.replaceAll(/<p><strong>Hinweis:<\/strong>.*?<\/p>/, '<p><strong>Vertraulich &amp; datensparsam:</strong> Bitte schildern Sie zunächst nur den notwendigen Kontext. Sensible Details gehören erst in einen geschützten Austausch; auf Wunsch vereinbaren wir vorher eine gegenseitige Vertraulichkeitsvereinbarung (NDA).</p>')
    component.setProperty('text', html)
}

website.save()

def commands = CommandsManager.getInstance()
['/start', '/clarity-session', '/executive-sparring', '/kontakt'].each { String path ->
    def parameters = new LinkedHashMap<String, Object>()
    parameters.put('repository', 'website')
    parameters.put('path', path)
    parameters.put('recursive', true)
    commands.executeCommand('default', 'publish', parameters)
}

println 'Trust-Elemente zu Vertraulichkeit, NDA und Datenschutz ergänzt und veröffentlicht.'
