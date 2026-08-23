import info.magnolia.commands.CommandsManager
import info.magnolia.context.MgnlContext
import javax.jcr.Node

def website = MgnlContext.getJCRSession('website')
Node area = website.getNode('/start/main')
Node target = null

for (Node component : area.nodes) {
    if (component.hasProperty('heading') && component.getProperty('heading').string == 'Erst verstehen. Dann wirksam verändern - ChOS.') {
        target = component
        break
    }
}

if (target == null) throw new IllegalStateException('ChOS-Startseitenblock nicht gefunden.')

target.setProperty('text', '''
<p class="lead">ChOS hilft, eine Organisation zuerst zu verstehen. Dafür betrachten wir fünf einfache Perspektiven.</p>
<div class="mvp-grid mvp-grid--3">
  <article class="mvp-panel"><p class="mvp-meta">01</p><h3>Richtung</h3><p>Was soll für Kunden und Unternehmen erreicht werden?</p></article>
  <article class="mvp-panel"><p class="mvp-meta">02</p><h3>Entscheidungen</h3><p>Wer entscheidet was und auf welcher Grundlage?</p></article>
  <article class="mvp-panel"><p class="mvp-meta">03</p><h3>Verantwortung</h3><p>Wer besitzt Ergebnis, Kontrolle und Konsequenzen?</p></article>
  <article class="mvp-panel"><p class="mvp-meta">04</p><h3>Zusammenarbeit</h3><p>Wie arbeiten Teams und Führung im Alltag zusammen?</p></article>
  <article class="mvp-panel"><p class="mvp-meta">05</p><h3>Lernen</h3><p>Wie wird sichtbar, was funktioniert und was nicht?</p></article>
  <article class="mvp-panel mvp-panel--accent">
    <p class="mvp-meta">ChOS im Detail</p>
    <h3>Wie die fünf Perspektiven zusammenspielen</h3>
    <p>Sehen Sie, wie ChOS aus einer konkreten Situation ein verständliches Bild und einen sinnvollen nächsten Schritt macht.</p>
    <p class="mvp-panel__action"><a class="mvp-offer-cta mvp-offer-cta--compact" href="/chos">ChOS verstehen <span aria-hidden="true">→</span></a></p>
  </article>
</div>''')

website.save()

def parameters = new LinkedHashMap<String, Object>()
parameters.put('repository', 'website')
parameters.put('path', '/start')
parameters.put('recursive', true)
CommandsManager.getInstance().executeCommand('default', 'publish', parameters)

println 'ChOS-CTA auf der Startseite als vollständige sechste Karte neu gestaltet.'
