import info.magnolia.context.MgnlContext
import javax.jcr.Node
import javax.jcr.Session

/*
 * Verlinkt die Clarity-Session sichtbar aus Leistungskarte und Navigation.
 * Das Skript ist wiederholbar.
 */

Session website = MgnlContext.getJCRSession('website')
Session navigation = MgnlContext.getJCRSession('navigation')

Node clarityPage = website.getNode('/clarity-session')
Node servicesPage = website.getNode('/leistungen')
Node servicesArea = servicesPage.getNode('main')

Node paidEntry = null
servicesArea.nodes.each { Node component ->
    if (component.hasProperty('heading') &&
        component.getProperty('heading').string == 'Bezahlter Einstieg') {
        paidEntry = component
    }
}

if (paidEntry == null || !paidEntry.hasProperty('text')) {
    throw new IllegalStateException('Der Abschnitt "Bezahlter Einstieg" wurde nicht gefunden.')
}

String html = paidEntry.getProperty('text').string
String opening = '<article class="mvp-panel mvp-panel--accent">'
int cardStart = html.indexOf(opening)
int cardEnd = cardStart >= 0 ? html.indexOf('</article>', cardStart) : -1

if (cardStart >= 0 && cardEnd > cardStart) {
    String cardContent = html.substring(cardStart + opening.length(), cardEnd)
    String linkedCard = '''<a class="mvp-panel mvp-panel--accent mvp-panel--link" href="/magnoliaPublic/clarity-session" aria-label="ChOS Clarity Session ansehen">''' +
        cardContent +
        '''<span class="mvp-panel__link">Clarity Session ansehen <span aria-hidden="true">→</span></span></a>'''
    html = html.substring(0, cardStart) + linkedCard + html.substring(cardEnd + '</article>'.length())
} else if (!html.contains('mvp-panel--link')) {
    throw new IllegalStateException('Die Clarity-Session-Leistungskarte wurde nicht gefunden.')
}

paidEntry.setProperty('text', html)

Node navRoot = navigation.getNode('/cleonhardt/Main')
Node navItems = navRoot.getNode('items')
Node servicesNav = navItems.getNode('01')
Node children = servicesNav.hasNode('children')
    ? servicesNav.getNode('children')
    : servicesNav.addNode('children', 'mgnl:contentNode')
Node clarityNav = children.hasNode('00')
    ? children.getNode('00')
    : children.addNode('00', 'mgnl:contentNode')

clarityNav.setProperty('label', 'Clarity Session')
clarityNav.setProperty('targetPage', clarityPage.getIdentifier())

website.save()
navigation.save()

println 'Die Clarity-Session-Karte ist vollständig klickbar und besitzt einen sichtbaren Link.'
println 'Die Clarity Session wurde als Unterpunkt von Leistungen in die Navigation aufgenommen.'
println 'Bitte /leistungen und die Navigation veröffentlichen.'
