import info.magnolia.context.MgnlContext
import javax.jcr.Node
import javax.jcr.Session

Session website = MgnlContext.getJCRSession('website')
Session problems = MgnlContext.getJCRSession('problems')
Session navigation = MgnlContext.getJCRSession('navigation')

Node ensurePage(Node parent, String name) {
    parent.hasNode(name) ? parent.getNode(name) : parent.addNode(name, 'mgnl:page')
}

Node ensureArea(Node page) {
    page.hasNode('main') ? page.getNode('main') : page.addNode('main', 'mgnl:area')
}

Node replaceComponent(Node area, String name, String template) {
    if (area.hasNode(name)) area.getNode(name).remove()
    Node component = area.addNode(name, 'mgnl:component')
    component.setProperty('mgnl:template', template)
    component
}

Node problemsFolder = problems.getNode('/cleonhardt')
Node overview = ensurePage(website.rootNode, 'probleme')
overview.setProperty('mgnl:template', 'meine-website:pages/home')
overview.setProperty('title', 'Problemsituationen verstehen')
overview.setProperty('navigationTitle', 'Situationen')
overview.setProperty('windowTitle', 'Typische Problemsituationen in Führung und Organisation | ChOS')
overview.setProperty('metaDescription', 'Vier konkrete Situationen aus Führung, Entscheidungen und AI-Governance: erkennen, diagnostizieren und mit einem kleinen nächsten Schritt prüfen.')
overview.setProperty('datePublished', '2026-08-13')
overview.setProperty('dateModified', '2026-08-13')
overview.setProperty('hideInNavigation', false)
Node overviewArea = ensureArea(overview)
Node hero = replaceComponent(overviewArea, '00', 'meine-website:components/hero')
hero.setProperty('eyebrow', 'ChOS Problemsituationen')
hero.setProperty('heading', 'Erst verstehen, was wirklich passiert.')
hero.setProperty('lead', 'Vier typische Situationen aus Führung, Entscheidungen und AI-Governance – mit Diagnosefragen und einem kleinen Schritt, den du selbst ausprobieren kannst.')
Node overviewComponent = replaceComponent(overviewArea, '10', 'meine-website:components/problemOverview')
overviewComponent.setProperty('heading', 'Welche Situation kommt dir bekannt vor?')
overviewComponent.setProperty('intro', '<p>Die Seiten liefern keine Ferndiagnose. Sie helfen dir, Beobachtung und Erklärung zu trennen, mögliche Mechanismen zu prüfen und das Problem genauer zu beschreiben.</p>')

Map<String, Map> definitions = [
    'leadership-bottleneck': [title: 'Wenn Entscheidungen immer bei dir landen', nav: 'Leadership Bottleneck'],
    'langsame-entscheidungen': [title: 'Wenn Entscheidungen zu lange dauern', nav: 'Langsame Entscheidungen'],
    'ai-decision-rights': [title: 'Was darf AI entscheiden – und wer trägt die Verantwortung?', nav: 'AI Decision Rights'],
    'leadership-transition': [title: 'Neu in Führung: Erst verstehen, dann verändern', nav: 'Neu in Führung']
]

Map<String, Node> pages = [:]
definitions.each { String slug, Map data ->
    Node problem = problemsFolder.getNode(slug)
    Node page = ensurePage(overview, slug)
    page.setProperty('mgnl:template', 'meine-website:pages/home')
    page.setProperty('title', data.title as String)
    page.setProperty('navigationTitle', data.nav as String)
    page.setProperty('windowTitle', ((problem.hasNode('seo') && problem.getNode('seo').hasProperty('seoTitle')) ? problem.getNode('seo').getProperty('seoTitle').string : data.title) + ' | ChOS')
    page.setProperty('metaDescription', (problem.hasNode('seo') && problem.getNode('seo').hasProperty('metaDescription')) ? problem.getNode('seo').getProperty('metaDescription').string : data.title as String)
    page.setProperty('datePublished', '2026-08-13')
    page.setProperty('dateModified', '2026-08-13')
    page.setProperty('problemReference', problem.identifier)
    page.setProperty('hideInNavigation', false)
    Node area = ensureArea(page)
    Node component = replaceComponent(area, '00', 'meine-website:components/problemDetail')
    component.setProperty('problemReference', problem.identifier)
    pages[slug] = page
}
website.save()

Node navConfig = navigation.nodeExists('/cleonhardt/Main') ? navigation.getNode('/cleonhardt/Main') : null
if (!navConfig) throw new IllegalStateException('Navigation /cleonhardt/Main fehlt')
Node items = navConfig.hasNode('items') ? navConfig.getNode('items') : navConfig.addNode('items', 'mgnl:contentNode')
Node existing = null
items.nodes.each { Node item ->
    if (item.hasProperty('targetPage') && item.getProperty('targetPage').string == overview.identifier) existing = item
}
Node navItem = existing ?: items.addNode('situationen', 'mgnl:contentNode')
navItem.setProperty('label', 'Situationen')
navItem.setProperty('targetPage', overview.identifier)
if (navItem.hasNode('children')) navItem.getNode('children').remove()
Node children = navItem.addNode('children', 'mgnl:contentNode')
definitions.eachWithIndex { String slug, Map data, int index ->
    Node child = children.addNode(String.format('%02d', index), 'mgnl:contentNode')
    child.setProperty('label', data.nav as String)
    child.setProperty('targetPage', pages[slug].identifier)
}
navigation.save()

println 'PUBLIC_PROBLEM_PAGES=5'
println 'OVERVIEW=' + overview.path
pages.each { slug, page -> println 'DETAIL=' + page.path + '|problem=' + page.getProperty('problemReference').string }
println 'NAVIGATION=Situationen|children=4'
