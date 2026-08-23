import info.magnolia.context.MgnlContext
import groovy.transform.Field
import javax.jcr.Node
import javax.jcr.Session

/*
 * Punkt 5 der Lead-Basis: drei transparente Praxisfallmuster und ein
 * datensparsamer ChOS-Selbstcheck. Wiederholbar und ohne erfundene Referenzen.
 */

@Field final String PAGE_TEMPLATE = 'meine-website:pages/home'
@Field final String COMPONENT_PREFIX = 'meine-website:components/'
@Field final String TODAY = '2026-07-31'

Session website = MgnlContext.getJCRSession('website')
website.refresh(false)

void setProperties(Node node, Map<String, Object> properties) {
    properties.each { String key, Object value ->
        if (value != null) node.setProperty(key, value)
    }
}

Node ensurePage(Node parent, String name, Map<String, Object> properties) {
    Node page = parent.hasNode(name) ? parent.getNode(name) : parent.addNode(name, 'mgnl:page')
    if (page.hasNode('main')) page.getNode('main').remove()
    setProperties(page, [
        'mgnl:template': PAGE_TEMPLATE,
        brandName: 'Christian Leonhardt',
        footerText: 'Product Leadership · Organisation · Transformation',
        hideInNavigation: true,
        dateModified: TODAY
    ] + properties)
    page
}

Node addArea(Node page) {
    page.addNode('main', 'mgnl:area')
}

Node addComponent(Node area, String template, Map<String, Object> properties = [:]) {
    String name = String.format('%02d', area.nodes.size)
    Node component = area.addNode(name, 'mgnl:component')
    component.setProperty('mgnl:template', template.contains(':') ? template : COMPONENT_PREFIX + template)
    setProperties(component, properties)
    component
}

Node addHero(Node area, String eyebrow, String title, String description) {
    addComponent(area, 'hero', [eyebrow: eyebrow, title: title, description: description])
}

Node addText(Node area, String heading, String html, boolean tinted = false) {
    addComponent(area, 'text', [heading: heading, text: html, showBackground: tinted])
}

Node addCta(Node area, String title, String html, String target, String button) {
    Node cta = addComponent(area, 'callToAction', [title: title, description: html, buttonText: button])
    Node chooser = cta.addNode('pageLinkChooser', 'mgnl:contentNode')
    chooser.setProperty('field', 'internalPageLink')
    chooser.setProperty('internalLink', target)
    cta
}

String caseHtml(String number, String title, List<Map<String, String>> steps) {
    String body = steps.collect { step ->
        "<div class=\"practice-case__step\"><h4>${step.title}</h4><p>${step.text}</p></div>"
    }.join('')
    "<article class=\"practice-case\"><header class=\"practice-case__header\"><p class=\"mvp-meta\">Fallmuster ${number}</p><h3>${title}</h3></header><div class=\"practice-case__grid\">${body}</div></article>"
}

Node casesPage = ensurePage(website.rootNode, 'praxisfaelle', [
    title: 'Praxisfälle',
    navigationTitle: 'Praxisfälle',
    windowTitle: 'Praxisfälle – Produktorganisationen systemisch verstehen',
    metaDescription: 'Drei anonymisierte Fallmuster zeigen, wie sich Entscheidungswege, Rollen und Abhängigkeiten in Produktorganisationen systemisch klären lassen.'
])
Node casesArea = addArea(casesPage)
addHero(casesArea, 'ChOS in der Praxis', 'Wenn das sichtbare Problem nicht die Ursache ist.', 'Drei typische Fallmuster zeigen, wie aus wiederkehrender Reibung eine überprüfbare Diagnose und ein kleiner, wirksamer nächster Schritt wird.')
addText(casesArea, 'Transparenz vor Wirkungserzählung', '<p class="lead">Die folgenden Beispiele sind anonymisierte und verdichtete typische Fallmuster. Sie sind keine Kundenreferenzen und enthalten keine erfundenen Kennzahlen oder Erfolgsversprechen.</p><p>Sie zeigen die Denk- und Arbeitsweise hinter ChOS: Beobachtung und Erklärung trennen, alternative Hypothesen prüfen und erst dann gezielt eingreifen.</p>', true)

addText(casesArea, '', caseHtml('01', 'Eine Prioritätsentscheidung beginnt immer wieder von vorn', [
    [title: 'Ausgangslage', text: 'Produkt, Technologie und Business stimmen einer Priorität im Meeting zu. Wenige Tage später wird dieselbe Entscheidung in einem anderen Kreis erneut geöffnet; Roadmaps verlieren dadurch Verbindlichkeit.'],
    [title: 'Naheliegende Erklärung', text: 'Es fehlt an Disziplin, einem besseren Priorisierungsframework oder einem weiteren Abstimmungstermin.'],
    [title: 'Systemische Diagnose', text: 'Die Beteiligten arbeiten mit unterschiedlichen Erfolgskriterien. Zudem ist unklar, wer eine Entscheidung schließen darf und welche neue Evidenz sie später wieder öffnen kann.'],
    [title: 'Gezielter Eingriff', text: 'Für einen realen Entscheidungsweg werden Entscheidungsverantwortung, notwendiger Input, gemeinsame Kriterien und zulässige Gründe für ein Wiederöffnen explizit gemacht und begrenzt erprobt.'],
    [title: 'Qualitative Wirkung', text: 'Diskussionen unterscheiden klarer zwischen neuer Evidenz und einem wiederkehrenden Einzelinteresse. Eskalationen werden konkreter und die nächste Entscheidung lässt sich bewusster vorbereiten.'],
    [title: 'Übertragbare Erkenntnis', text: 'Bevor ein neues Priorisierungsverfahren eingeführt wird, sollte die tatsächliche Entscheidungsarchitektur geklärt werden.']
]))

addText(casesArea, '', caseHtml('02', 'Neue Rollen, alte Entscheidungen', [
    [title: 'Ausgangslage', text: 'Eine Produktorganisation führt neue Rollen ein. Auf dem Papier ist Verantwortung verteilt, im Alltag warten Teams jedoch weiter auf Freigaben und Führung greift regelmäßig in Detailentscheidungen ein.'],
    [title: 'Naheliegende Erklärung', text: 'Die Rollenbeschreibungen sind noch nicht präzise genug oder wurden nicht ausreichend kommuniziert.'],
    [title: 'Systemische Diagnose', text: 'Verantwortung wurde benannt, aber Mandate, Informationszugang und Konsequenzen blieben unverändert. Das alte Führungsverhalten stabilisiert deshalb weiterhin das alte System.'],
    [title: 'Gezielter Eingriff', text: 'An konkreten Entscheidungssituationen werden Mandat, erwartetes Ergebnis, Beteiligung und Eskalationsgrenzen gemeinsam geklärt. Führung vereinbart sichtbar, wann sie unterstützt und wann sie nicht eingreift.'],
    [title: 'Qualitative Wirkung', text: 'Unklarheit wird an konkreten Entscheidungen besprechbar. Teams und Führung erkennen früher, ob Information, Kompetenz, Mandat oder ein echter Zielkonflikt fehlt.'],
    [title: 'Übertragbare Erkenntnis', text: 'Eine neue Rolle verändert erst dann etwas, wenn sich dadurch beobachtbar Entscheidungen und Führungsroutinen verändern.']
]))

addText(casesArea, '', caseHtml('03', 'Lokale Geschwindigkeit erzeugt systemweite Reibung', [
    [title: 'Ausgangslage', text: 'Einzelne Teams liefern schnell, dennoch entstehen Wartezeiten, Übergaben und wiederkehrende Konflikte entlang des gesamten Wertstroms. Jede Einheit optimiert nachvollziehbar ihre eigenen Ziele.'],
    [title: 'Naheliegende Erklärung', text: 'Teams müssen besser zusammenarbeiten, mehr planen oder Abhängigkeiten konsequenter koordinieren.'],
    [title: 'Systemische Diagnose', text: 'Lokale Ziele, Finanzierungslogik und Zuständigkeiten belohnen getrennte Optimierung. Die Abhängigkeiten sind daher nicht nur ein Kommunikationsproblem, sondern Ergebnis des Organisationsdesigns.'],
    [title: 'Gezielter Eingriff', text: 'Ein relevanter Wertstrom wird gemeinsam sichtbar gemacht. Beteiligte identifizieren die teuersten Übergaben, widersprüchliche Ziele und Entscheidungen, die heute keinem eindeutigen Ort gehören.'],
    [title: 'Qualitative Wirkung', text: 'Die Diskussion verschiebt sich von gegenseitigen Vorwürfen zu gemeinsam beeinflussbaren Mechanismen. Maßnahmen können nach ihrem Beitrag zum Gesamtergebnis priorisiert werden.'],
    [title: 'Übertragbare Erkenntnis', text: 'Mehr Abstimmung kompensiert ein widersprüchliches System nur vorübergehend; Ziele und Entscheidungsorte müssen zusammenpassen.']
]))
addText(casesArea, 'Was die drei Fälle gemeinsam haben', '<ul class="mvp-checks"><li>Das sichtbare Symptom ist ein Ausgangspunkt, keine fertige Diagnose.</li><li>Mehrere plausible Erklärungen werden geprüft, bevor eine Lösung gewählt wird.</li><li>Der erste Eingriff bleibt klein genug, um seine Wirkung beobachten zu können.</li><li>Erkenntnisse werden auf konkrete Entscheidungen, Rollen und Routinen zurückgeführt.</li></ul>', true)
addCta(casesArea, 'Welches Muster erkennen Sie wieder?', '<p>In der Clarity Session betrachten wir eine konkrete Situation vertraulich und trennen Symptom, mögliche Ursache und nächsten sinnvollen Test.</p>', '/clarity-session', 'Clarity Session ansehen')

List<Map<String, Object>> dimensions = [
    [key: 'direction', title: 'Richtung und Strategie', description: 'Wie klar verbinden sich Zielbild, Prioritäten und Wirkung?', questions: [
        'Prioritäten wechseln, ohne dass sich Strategie oder Evidenz erkennbar verändert haben.',
        'Teams interpretieren den gewünschten Kundennutzen unterschiedlich.',
        'Erfolg wird vor allem über Lieferung statt über Wirkung beschrieben.',
        'Kurzfristige Anforderungen verdrängen regelmäßig vereinbarte Produktziele.'
    ]],
    [key: 'responsibility', title: 'Rollen und Verantwortung', description: 'Passen Verantwortung, Mandat und Schnittstellen zusammen?', questions: [
        'Verantwortung ist benannt, das nötige Entscheidungsmandat fehlt jedoch.',
        'Mehrere Rollen fühlen sich für dieselbe Entscheidung letztverantwortlich.',
        'Entscheidungen werden außerhalb der formal zuständigen Rolle erneut geöffnet.',
        'Schnittstellen werden vor allem durch persönliche Abstimmung statt klare Erwartungen stabilisiert.'
    ]],
    [key: 'decisions', title: 'Entscheidungen und Priorisierung', description: 'Entstehen Entscheidungen nachvollziehbar und am richtigen Ort?', questions: [
        'Wichtige Prioritätsentscheidungen drehen wiederholt neue Schleifen.',
        'Es ist unklar, welche Information für eine Entscheidung tatsächlich ausreicht.',
        'Eskalationen ersetzen häufig eine Entscheidung am vorgesehenen Ort.',
        'Gegenläufige Ziele werden in Gremien vertagt statt bewusst aufgelöst.'
    ]],
    [key: 'collaboration', title: 'Zusammenarbeit und Abhängigkeiten', description: 'Unterstützt die Struktur das gemeinsame Ergebnis?', questions: [
        'Teams optimieren lokal, obwohl dadurch andere Teile des Wertstroms langsamer werden.',
        'Abhängigkeiten bestimmen die Planung stärker als Kundenergebnisse.',
        'Zusammenarbeit funktioniert vor allem über einzelne Schlüsselpersonen.',
        'Probleme werden zwischen Bereichen weitergereicht, statt gemeinsam am System gelöst.'
    ]],
    [key: 'leadership', title: 'Führung und Kommunikation', description: 'Stärken Führungssignale und Routinen das gewünschte Verhalten?', questions: [
        'Führung fordert Eigenverantwortung, greift aber regelmäßig in Detailentscheidungen ein.',
        'Unterschiedliche Führungssignale erzeugen widersprüchliche Prioritäten.',
        'Probleme werden früh sichtbar, aber erst spät offen angesprochen.',
        'Veränderungen werden als Maßnahmen verfolgt, ohne die gewünschte Verhaltensänderung zu prüfen.'
    ]]
]
List<Map<String, Object>> answers = [
    [value: 0, label: 'Trifft nicht zu'],
    [value: 1, label: 'Teilweise'],
    [value: 2, label: 'Häufig'],
    [value: 3, label: 'Deutlich']
]

StringBuilder checkHtml = new StringBuilder('<div class="chos-check" data-chos-check>')
checkHtml << '<p class="chos-check__privacy"><strong>Datensparsam:</strong> Ihre Antworten werden nur in diesem Browser ausgewertet. Sie werden weder übertragen noch gespeichert und es werden dafür keine Cookies gesetzt.</p>'
checkHtml << '<form data-chos-form><p>Bewerten Sie jedes Signal aus Sicht Ihrer aktuellen Organisation. Der Check dauert etwa acht bis zehn Minuten.</p>'
int questionNumber = 0
dimensions.eachWithIndex { dimension, dimensionIndex ->
    checkHtml << "<section class=\"chos-check__dimension\" aria-labelledby=\"dimension-${dimension.key}\"><p class=\"mvp-meta\">Dimension ${dimensionIndex + 1} von 5</p><h3 id=\"dimension-${dimension.key}\">${dimension.title}</h3><p>${dimension.description}</p>"
    dimension.questions.each { String question ->
        questionNumber++
        String questionId = "q${questionNumber}"
        checkHtml << "<fieldset class=\"chos-check__question\" data-question=\"${questionId}\" data-dimension=\"${dimension.key}\"><legend><span>${questionNumber}.</span> ${question}</legend><div class=\"chos-check__scale\">"
        answers.each { answer ->
            checkHtml << "<label class=\"chos-check__option\"><input type=\"radio\" name=\"${questionId}\" value=\"${answer.value}\" required><span>${answer.label}</span></label>"
        }
        checkHtml << '</div></fieldset>'
    }
    checkHtml << '</section>'
}
checkHtml << '<div class="chos-check__actions"><button class="button" type="submit">Auswertung anzeigen</button><button class="chos-check__reset" type="reset">Antworten zurücksetzen</button></div></form>'
checkHtml << '<section class="chos-check__result" data-chos-result hidden tabindex="-1" aria-live="polite"><p class="mvp-meta">Ihre Einordnung</p><div data-chos-summary></div><ul class="chos-check__result-list" data-chos-dimensions></ul><a class="button" data-chos-result-cta href="/clarity-session">Ergebnis persönlich einordnen</a></section>'
checkHtml << '<noscript><p class="chos-check__privacy">Für die lokale Auswertung muss JavaScript aktiviert sein. Es werden dennoch keine Antworten an den Server übertragen.</p></noscript></div>'

Node checkPage = ensurePage(website.rootNode, 'chos-selbstcheck', [
    title: 'ChOS Selbstcheck',
    navigationTitle: 'ChOS Selbstcheck',
    windowTitle: 'ChOS Selbstcheck – 20 Fragen zur Produktorganisation',
    metaDescription: 'Kostenloser ChOS-Selbstcheck mit 20 Fragen zu Strategie, Verantwortung, Entscheidungen, Zusammenarbeit und Führung – lokal und ohne Datenübertragung.'
])
Node checkArea = addArea(checkPage)
addHero(checkArea, 'Kostenloser Selbstcheck', 'Wo lohnt sich genaueres Hinsehen?', '20 Fragen geben eine erste Orientierung zu fünf Spannungsfeldern Ihrer Produktorganisation – ohne Registrierung und ohne Übertragung Ihrer Antworten.')
addText(checkArea, 'Eine Orientierung, keine Ferndiagnose', '<p class="lead">Der Selbstcheck macht wiederkehrende Signale sichtbar. Ein hoher Wert bedeutet nicht automatisch, dass die vermutete Ursache feststeht.</p><p><strong>Einordnung je Dimension:</strong> 0–3 Punkte: wenige deutliche Signale · 4–7 Punkte: beobachtbare Spannungen · 8–12 Punkte: deutlicher Klärungsbedarf.</p>', true)
addText(checkArea, '', checkHtml.toString())
addText(checkArea, 'Was Sie mit dem Ergebnis tun können', '<ol><li>Wählen Sie ein konkretes Beispiel aus der am stärksten ausgeprägten Dimension.</li><li>Trennen Sie beobachtbares Verhalten von Ihrer ersten Erklärung.</li><li>Formulieren Sie mindestens eine alternative Hypothese.</li><li>Prüfen Sie einen kleinen Eingriff, dessen Wirkung beobachtbar ist.</li></ol><p>Für eine belastbare Organisationsdiagnose braucht es zusätzlichen Kontext, Gespräche und reale Entscheidungsverläufe.</p>', true)

Node chosPage = website.getNode('/chos')
Node chosArea = chosPage.getNode('main')
List<Node> oldModules = []
for (Node child : chosArea.nodes) {
    if (child.hasProperty('point5Module') && child.getProperty('point5Module').boolean) oldModules << child
}
oldModules.each { it.remove() }

Node finalCta = null
for (Node child : chosArea.nodes) {
    if (child.hasProperty('mgnl:template') && child.getProperty('mgnl:template').string == COMPONENT_PREFIX + 'callToAction') finalCta = child
}
String moduleName = 'point5-einstieg'
if (chosArea.hasNode(moduleName)) chosArea.getNode(moduleName).remove()
Node module = chosArea.addNode(moduleName, 'mgnl:component')
module.setProperty('mgnl:template', COMPONENT_PREFIX + 'text')
module.setProperty('heading', 'ChOS selbst anwenden')
module.setProperty('showBackground', true)
module.setProperty('point5Module', true)
module.setProperty('text', '<p class="lead">Nutzen Sie ChOS als erste Orientierung oder sehen Sie, wie der Ansatz an typischen Situationen arbeitet.</p><div class="mvp-grid mvp-grid--2"><a class="mvp-panel mvp-panel--link" href="/chos-selbstcheck"><p class="mvp-meta">8–10 Minuten · ohne Datenübertragung</p><h3>ChOS Selbstcheck</h3><p>20 Fragen zu Richtung, Verantwortung, Entscheidungen, Zusammenarbeit und Führung.</p><span class="mvp-panel__link">Selbstcheck starten →</span></a><a class="mvp-panel mvp-panel--link" href="/praxisfaelle"><p class="mvp-meta">Drei transparente Fallmuster</p><h3>ChOS in der Praxis</h3><p>Nachvollziehen, wie aus einem sichtbaren Symptom eine überprüfbare Diagnose wird.</p><span class="mvp-panel__link">Praxisfälle ansehen →</span></a></div>')
if (finalCta != null) chosArea.orderBefore(module.name, finalCta.name)

website.save()
println 'Punkt 5 angelegt: /praxisfaelle, /chos-selbstcheck und zwei Einstiege auf /chos.'
println 'Die Fallmuster sind ausdrücklich keine Kundenreferenzen; der Selbstcheck überträgt oder speichert keine Antworten.'
println 'Als Nächstes /praxisfaelle, /chos-selbstcheck und /chos veröffentlichen.'
