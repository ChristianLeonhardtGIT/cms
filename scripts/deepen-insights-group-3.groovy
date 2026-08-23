import info.magnolia.context.MgnlContext
import javax.jcr.Node

def website = MgnlContext.getJCRSession('website')
println 'Insight-Vertiefung gestartet.'

Map<String, String> depth = [

  'warum-ai-einfuehrung-ein-operating-model-thema-ist': '''
<h3>Vom Werkzeug zur veränderten Arbeit</h3><p>Ein einzelner Assistent kann Texte schneller zusammenfassen, ohne die Organisation zu verändern. Sobald Ergebnisse jedoch in Entscheidungen einfließen, Aufgaben automatisch weitergegeben werden oder ein System selbst handelt, verändern sich Informationswege, Kontrollpunkte und Verantwortung. Dann reicht eine technische Einführung nicht mehr.</p>
<h3>Ein typisches Beispiel</h3><p>Ein System bewertet Kundenanfragen und schlägt Prioritäten vor. Die Bearbeitung wird schneller, doch niemand weiß, wer falsche Einstufungen überwacht oder wann ein Mensch abweichen darf. Teams folgen der Empfehlung, weil sie objektiv wirkt. Führung erwartet trotzdem menschliche Verantwortung. So entsteht eine Lücke zwischen faktischer Entscheidung und formaler Verantwortung.</p>
<h3>AI innerhalb der fünf ChOS-Perspektiven</h3><ul class="mvp-checks"><li><strong>Richtung:</strong> Welches Ergebnis soll sich verbessern – und für wen?</li><li><strong>Entscheidungen:</strong> Was wird vorgeschlagen, vorbereitet oder selbstständig ausgeführt?</li><li><strong>Verantwortung:</strong> Wer besitzt Ergebnis, Kontrolle und Konsequenzen?</li><li><strong>Zusammenarbeit:</strong> Wie funktionieren Übergaben und Widerspruch?</li><li><strong>Lernen:</strong> Welche Fehler und Nebenwirkungen werden sichtbar?</li></ul>
<h3>Ein begrenzter Einstieg</h3><p>Wählen Sie einen realen Ablauf. Markieren Sie, wo Information entsteht, wo eine Entscheidung fällt und wo Verantwortung liegt. Ergänzen Sie erst danach die technische Unterstützung. Vereinbaren Sie Grenzen, Eskalationen und Messgrößen vor dem Piloten, nicht erst nach einem Vorfall.</p>
<h3>Grenze</h3><p>Nicht jede Nutzung benötigt ein neues Operating Model. Bei persönlicher, reversibler Unterstützung mit menschlicher Prüfung reichen oft klare Arbeitsregeln. Der organisatorische Gestaltungsbedarf steigt mit Autonomie, Reichweite, Geschwindigkeit und möglichem Schaden.</p>''',

  'decision-rights-zwischen-mensch-und-ai': '''
<h3>Die falsche Frage lautet: Mensch oder AI?</h3><p>In der Praxis besteht eine Entscheidung aus mehreren Schritten: Informationen sammeln, Optionen erzeugen, bewerten, auswählen, ausführen und Wirkung beobachten. Diese Schritte können unterschiedlich verteilt werden. Dadurch lässt sich Autonomie gezielt gestalten, statt sie pauschal zu erlauben oder zu verbieten.</p>
<h3>Fünf Stufen der Beteiligung</h3><ol><li>Information verdichten</li><li>Optionen vorschlagen</li><li>eine Empfehlung mit Begründung liefern</li><li>innerhalb klarer Grenzen ausführen</li><li>selbstständig handeln und bei Ausnahmen eskalieren</li></ol><p>Je höher die Stufe, desto klarer müssen Ziel, Datenqualität, Grenzen, Überwachung und Rücknahme sein.</p>
<h3>Beispiel: Preisentscheidung</h3><p>Ein System kann Nachfrage analysieren und einen Preis empfehlen. Die mögliche Wirkung ist unmittelbar, aber meist reversibel. Anders ist eine automatische Vertragsablehnung: Sie betrifft Rechte und Kundenbeziehungen und kann schwer nachvollziehbare Folgen haben. Dasselbe Modell kann deshalb in einem Fall ausführen und im anderen nur vorbereiten.</p>
<h3>Verantwortung bleibt konkret</h3><p>„Der Algorithmus hat entschieden“ ist keine Verantwortungsregel. Eine Rolle oder Einheit muss die Einsatzgrenzen besitzen, Ergebnisse überwachen und bei Fehlern handeln können. Dafür benötigt sie Information und echtes Mandat – nicht nur eine formale Zuständigkeit.</p>
<h3>Prüffragen</h3><ul class="mvp-checks"><li>Wie groß und wie reversibel ist die Wirkung?</li><li>Welche Unsicherheit enthält die Empfehlung?</li><li>Wie schnell wird ein Fehler erkannt?</li><li>Kann ein Mensch sinnvoll eingreifen oder bestätigt er nur noch?</li><li>Wer darf den Ablauf stoppen?</li></ul>''',

  'schlechte-prozesse-nicht-nur-schneller-machen': '''
<h3>Ein schneller Schritt kann den Gesamtprozess verschlechtern</h3><p>Wenn Analysen in Minuten statt Tagen entstehen, wächst häufig die Menge der Ergebnisse. Muss danach jedes Ergebnis geprüft, abgestimmt und freigegeben werden, wird der nächste Engpass größer. Lokale Effizienz erzeugt mehr Arbeit im restlichen System.</p>
<h3>Beispiel aus der Produktarbeit</h3><p>Automatisierte Auswertung erzeugt täglich neue Kundenhypothesen. Product Manager prüfen mehr Vorschläge, Design und Entwicklung erhalten häufiger wechselnde Anforderungen und die Zahl paralleler Initiativen steigt. Die Analyse ist schneller, das Produktergebnis jedoch nicht. Es fehlt eine Entscheidung darüber, welche Information relevant ist und wann daraus Arbeit entstehen darf.</p>
<h3>Den Ablauf vom Ergebnis rückwärts lesen</h3><p>Beginnen Sie nicht beim automatisierbaren Arbeitsschritt, sondern beim gewünschten Ergebnis. Welche Entscheidung muss dafür besser werden? Welche Information benötigt sie? Wo entstehen Wartezeit, Nacharbeit und Übergaben? Erst dann wird sichtbar, ob Technik einen Engpass löst oder nur verschiebt.</p>
<h3>Ein sinnvoller Pilot</h3><ol><li>Einen klar begrenzten End-to-end-Ablauf wählen.</li><li>Durchlaufzeit, Qualität und Nacharbeit vorab erfassen.</li><li>Entscheidungen und Verantwortliche sichtbar machen.</li><li>Nur einen relevanten Teil verändern.</li><li>Gesamtwirkung statt erzeugter Menge messen.</li></ol>
<h3>Wann Automatisierung ohne Redesign reicht</h3><p>Bei stabilen, regelbasierten Aufgaben mit klaren Ein- und Ausgaben kann eine lokale Verbesserung völlig ausreichen. Ein Redesign wird nötig, wenn Ergebnisse neue Entscheidungen auslösen, mehrere Rollen betreffen oder sich Fehler schnell durch den gesamten Ablauf vervielfältigen.</p>'''
]
depth.each { String slug, String html ->
    String path = '/insights/' + slug
    println "Bearbeite ${path} ..."
    if (!website.nodeExists(path + '/main')) throw new IllegalStateException("Insight fehlt: ${path}")
    Node area = website.getNode(path + '/main')
    if (area.hasNode('insight-depth')) area.getNode('insight-depth').remove()
    Node component = area.addNode('insight-depth', 'mgnl:component')
    component.setProperty('mgnl:template', 'meine-website:components/text')
    component.setProperty('heading', 'Vertiefung und Anwendung')
    component.setProperty('text', html)
    component.setProperty('showBackground', false)

    String before = null
    for (Node child : area.nodes) {
        if (!child.hasProperty('mgnl:template')) continue
        String template = child.getProperty('mgnl:template').string
        if (template == 'meine-website:components/callToAction' || template == 'meine-website:components/faq' || template == 'meine-website:components/relatedInsights') {
            before = child.getName()
            break
        }
    }
    if (before != null) area.orderBefore(component.getName(), before)
    website.save()
    println "${path}: vertieft"
}

println "${depth.size()} Insights inhaltlich vertieft."
