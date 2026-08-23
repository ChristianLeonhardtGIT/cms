import info.magnolia.commands.CommandsManager
import info.magnolia.context.MgnlContext
import javax.jcr.Node

def website = MgnlContext.getJCRSession('website')
Node area = website.getNode('/start/main')
boolean updated = false

for (Node component : area.nodes) {
    if (!component.hasProperty('heading') || component.getProperty('heading').string != 'Mit einer konkreten Situation starten.') continue
    String html = component.getProperty('text').string
    html = html.replace('<p><a class="mvp-button" href="/clarity-session">Clarity Session ansehen -&gt;</a></p>', '<p class="mvp-panel__action"><a class="mvp-offer-cta" href="/clarity-session">Clarity Session ansehen <span aria-hidden="true">→</span></a></p>')
    html = html.replace('<p><a class="mvp-button" href="/clarity-session">Clarity Session ansehen -></a></p>', '<p class="mvp-panel__action"><a class="mvp-offer-cta" href="/clarity-session">Clarity Session ansehen <span aria-hidden="true">→</span></a></p>')
    html = html.replace('<p><a class="mvp-button" href="/executive-sparring">Executive Sparring ansehen -&gt;</a></p>', '<p class="mvp-panel__action"><a class="mvp-offer-cta" href="/executive-sparring">Executive Sparring ansehen <span aria-hidden="true">→</span></a></p>')
    html = html.replace('<p><a class="mvp-button" href="/executive-sparring">Executive Sparring ansehen -></a></p>', '<p class="mvp-panel__action"><a class="mvp-offer-cta" href="/executive-sparring">Executive Sparring ansehen <span aria-hidden="true">→</span></a></p>')
    component.setProperty('text', html)
    updated = true
}

if (!updated) throw new IllegalStateException('Prioritätskarten auf /start nicht gefunden.')
website.save()

def parameters = new LinkedHashMap<String, Object>()
parameters.put('repository', 'website')
parameters.put('path', '/start')
parameters.put('recursive', true)
CommandsManager.getInstance().executeCommand('default', 'publish', parameters)
println 'Hover-Effekt der priorisierten Startseitenkarten aktualisiert.'
