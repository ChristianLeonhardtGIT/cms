import info.magnolia.commands.CommandsManager
import info.magnolia.context.MgnlContext

import javax.jcr.Node

def website = MgnlContext.getJCRSession('website')
Node start = website.getNode('/start')

Node findByProperty(Node root, String propertyName, String propertyValue) {
    if (root.hasProperty(propertyName) && root.getProperty(propertyName).string == propertyValue) return root
    for (Node child : root.nodes) {
        Node match = findByProperty(child, propertyName, propertyValue)
        if (match != null) return match
    }
    null
}

Node orientation = findByProperty(start, 'heading', 'Wie möchtest du starten?')
if (orientation == null) throw new IllegalStateException('Orientierungsblock fehlt.')

orientation.setProperty('text', '''
<div class="chos-orientation">
  <p class="lead">Wähle den Einstieg, der zu deiner aktuellen Situation passt. Du musst noch nicht wissen, welches Angebot du brauchst.</p>
  <div class="mvp-grid mvp-grid--3">
    <a class="mvp-panel mvp-panel--link" href="/probleme" aria-label="Typische Situationen ansehen"><p class="mvp-meta">Problem wiedererkennen</p><h3>Typische Situationen</h3><p>Prüfe vier konkrete Führungs-, Entscheidungs- und Organisationssituationen.</p><span class="mvp-panel__link"><span>Situationen ansehen</span><span aria-hidden="true">→</span></span></a>
    <a class="mvp-panel mvp-panel--link" href="/chos-selbstcheck" aria-label="ChOS Selbstcheck starten"><p class="mvp-meta">8–10 Minuten · lokal</p><h3>ChOS Selbstcheck</h3><p>Ordne erste Signale entlang der fünf ChOS-Perspektiven ein.</p><span class="mvp-panel__link"><span>Selbstcheck starten</span><span aria-hidden="true">→</span></span></a>
    <a class="mvp-panel mvp-panel--link" href="/insights" aria-label="Insights lesen"><p class="mvp-meta">Fachlich vertiefen</p><h3>Insights</h3><p>Lies weiter zu Entscheidungen, Verantwortung, Leadership und AI.</p><span class="mvp-panel__link"><span>Insights lesen</span><span aria-hidden="true">→</span></span></a>
  </div>
</div>''')

website.save()

def parameters = new LinkedHashMap<String, Object>()
parameters.put('repository', 'website')
parameters.put('path', '/start')
parameters.put('recursive', true)
def result = CommandsManager.getInstance().executeCommand('default', 'publish', parameters)
println "Orientierungs-CTAs optimiert: ${result ? 'veröffentlicht' : 'Veröffentlichung nicht bestätigt'}"
