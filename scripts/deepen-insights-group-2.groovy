import info.magnolia.context.MgnlContext
import javax.jcr.Node

def website = MgnlContext.getJCRSession('website')
println 'Insight-Vertiefung gestartet.'

Map<String, String> depth = [
  'annahmen-vor-einer-produktentscheidung-pruefen': '''
<h3>Die gefährlichsten Annahmen wirken wie Fakten</h3><p>„Kunden brauchen diese Funktion“, „die Plattform skaliert nicht“ oder „der Markt erwartet eine schnelle Einführung“ klingen eindeutig. Häufig vermischen solche Sätze Beobachtung, Interpretation und Schlussfolgerung. Ein Decision Review macht diese Kette sichtbar, ohne die Entscheidung durch endlose Analyse zu lähmen.</p>
<h3>Beispiel einer Entscheidungskette</h3><p>Beobachtung: Drei große Kunden fragen nach einer Funktion. Annahme: Die Funktion ist für den gesamten Zielmarkt relevant. Schlussfolgerung: Sie erhält höchste Priorität. Alternative Erklärungen könnten sein, dass nur ein Segment betroffen ist oder ein bestehender Ablauf missverstanden wird. Je nach Erklärung wäre die richtige Investition eine andere.</p>
<h3>Risiko statt Gewissheit steuern</h3><p>Nicht jede Annahme muss geprüft werden. Entscheidend sind Annahmen, deren Irrtum großen Schaden erzeugt und deren Prüfung bezahlbar ist. Dafür helfen drei Kriterien: Auswirkung, Unsicherheit und Umkehrbarkeit. Hohe Wirkung, geringe Evidenz und schwer umkehrbare Folgen verlangen die stärkste Gegenprüfung.</p>
<h3>Ein praktisches Red Team</h3><ul class="mvp-checks"><li>Formulieren Sie die bevorzugte Option und ihre Begründung in einem Satz.</li><li>Markieren Sie Beobachtungen, Annahmen und Bewertungen getrennt.</li><li>Entwickeln Sie mindestens eine plausible Gegenhypothese.</li><li>Fragen Sie, welche Information die Präferenz tatsächlich ändern würde.</li><li>Definieren Sie einen Test oder akzeptieren Sie das Risiko ausdrücklich.</li></ul>
<h3>Grenze</h3><p>Ein Review soll eine Entscheidung verbessern, nicht Verantwortung verdünnen. Nach der Gegenprüfung muss klar bleiben, wer entscheidet, bis wann und unter welchen neuen Informationen die Entscheidung erneut betrachtet wird.</p>''',

  'organisationsdiagnose-statt-standardberatung': '''
<h3>Warum Standardlösungen trotzdem attraktiv sind</h3><p>Sie geben schnell Orientierung, lassen sich budgetieren und vermitteln Handlungsfähigkeit. Das ist nicht grundsätzlich schlecht. Problematisch wird es, wenn die Lösung bereits feststeht, bevor geklärt wurde, welcher Mechanismus das beobachtete Muster erzeugt.</p>
<h3>Ein Symptom, vier mögliche Ursachen</h3><p>„Teams liefern zu langsam“ kann auf zu große Arbeitspakete, fehlende Entscheidungen, technische Abhängigkeiten oder ständig wechselnde Ziele zurückgehen. Mehr agile Rituale adressieren höchstens einen Teil davon. Eine Diagnose unterscheidet diese Erklärungen anhand konkreter Fälle und sucht bewusst nach Gegenbelegen.</p>
<h3>Was gute Diagnose nicht ist</h3><p>Sie ist keine monatelange Bestandsaufnahme und keine Sammlung aller Beschwerden. Sie beginnt mit einer begrenzten Frage, betrachtet mehrere Perspektiven und endet mit priorisierten Hypothesen. Ihre Qualität zeigt sich darin, ob daraus unterschiedliche, überprüfbare Handlungsoptionen entstehen.</p>
<h3>Ein minimales Diagnoseformat</h3><ol><li>Eine wiederkehrende Situation präzise beschreiben.</li><li>Zwei bis vier konkrete Fälle rekonstruieren.</li><li>Betroffene Entscheidungsorte und Ziele vergleichen.</li><li>Mindestens zwei Ursachenhypothesen formulieren.</li><li>Einen kleinen Test mit erwarteter Beobachtung vereinbaren.</li></ol>
<h3>Wann ein Standard sinnvoll ist</h3><p>Bei bekannten, stabilen Problemen mit klarer Ursache kann ein bewährtes Vorgehen die wirtschaftlichste Wahl sein. ChOS verlangt keine Individualisierung um ihrer selbst willen. Es verlangt nur, dass Problem und Lösung nachvollziehbar zusammenpassen.</p>''',

  'product-organisation-diagnostic-ablauf-und-ergebnis': '''
<h3>Die Untersuchungsfrage bestimmt die Qualität</h3><p>„Wie verbessern wir unsere Produktorganisation?“ führt fast zwangsläufig zu einer langen Wunschliste. Eine gute Frage verbindet ein beobachtetes Muster mit einer relevanten Wirkung: Warum werden gemeinsame Prioritäten zwischen drei Bereichen regelmäßig innerhalb weniger Wochen wieder aufgehoben?</p>
<h3>Formale und tatsächliche Organisation vergleichen</h3><p>Strategien, Rollenbilder und Prozessbeschreibungen zeigen den Anspruch. Rekonstruierte Entscheidungen zeigen den Alltag. Interviews erklären unterschiedliche Wahrnehmungen. Kennzahlen zeigen mögliche Wirkung. Kein einzelner Datentyp reicht; interessant sind die Abweichungen zwischen ihnen.</p>
<h3>Aus Aussagen werden prüfbare Hypothesen</h3><p>„Führung vertraut den Teams nicht“ ist zunächst eine Bewertung. Prüfbarer wäre: Führung greift ein, weil wirtschaftliche Risiken erst nach der Teamentscheidung sichtbar werden. Daraus folgt ein anderer Test als aus der Hypothese, dass Mandate bewusst nicht akzeptiert werden.</p>
<h3>Was ein gutes Ergebnis enthält</h3><ul class="mvp-checks"><li>eine beantwortete Untersuchungsfrage und klare Systemgrenze</li><li>beobachtete Muster statt einer Sammlung von Meinungen</li><li>priorisierte Hypothesen einschließlich Gegenbelegen</li><li>Risiken und Wechselwirkungen der möglichen Eingriffe</li><li>wenige nächste Schritte mit erwarteter Wirkung und Lernsignal</li></ul>
<h3>Was es nicht verspricht</h3><p>Eine Diagnose liefert keine mathematische Gewissheit über soziale Systeme. Sie reduziert relevante Unsicherheit und macht Annahmen transparent. Gute Führung behält deshalb nach dem Eingriff die Beobachtung bei und korrigiert das Vorgehen, wenn die erwartete Wirkung ausbleibt.</p>''',

  'wann-ist-executive-sparring-sinnvoll': '''
<h3>Die Einsamkeit komplexer Entscheidungen</h3><p>Je größer die Verantwortung, desto weniger Personen können intern vollständig unabhängig widersprechen. Mitarbeitende sind betroffen, Kolleginnen vertreten eigene Bereiche und Vorgesetzte erwarten eine Empfehlung. Sparring schafft einen vertraulichen Raum, in dem eine reale Entscheidung ohne politische Nebenagenda geprüft werden kann.</p>
<h3>Worum es im Sparring tatsächlich geht</h3><p>Nicht jede Session braucht ein persönliches Entwicklungsthema. Häufig geht es um eine konkrete Entscheidung: eine Reorganisation, einen Konflikt zwischen Führungskräften, eine Priorität oder die Frage, ob überhaupt eingegriffen werden sollte. Das Gegenüber hilft, Beobachtung und Deutung zu trennen, blinde Flecken sichtbar zu machen und Konsequenzen verschiedener Optionen durchzuspielen.</p>
<h3>Ein sinnvoller Rhythmus</h3><p>Vor einer Entscheidung werden Ziel, Annahmen und Risiken geklärt. Danach wird beobachtet, was tatsächlich geschieht. In der nächsten Schleife werden nicht nur Ergebnisse, sondern auch die eigene Entscheidungslogik ausgewertet. So entsteht Lernen über mehrere Situationen hinweg.</p>
<h3>Woran gutes Sparring erkennbar ist</h3><ul class="mvp-checks"><li>Entscheidungen werden klarer, nicht an das Gegenüber delegiert.</li><li>Widerspruch ist konkret und begründet.</li><li>Wiederkehrende Muster werden früher erkannt.</li><li>Optionen enthalten Konsequenzen und Beobachtungskriterien.</li><li>Die Zusammenarbeit erzeugt keine dauerhafte Abhängigkeit.</li></ul>
<h3>Wann etwas anderes nötig ist</h3><p>Fehlt Fachwissen für eine klar begrenzte Aufgabe, ist Beratung passender. Geht es primär um psychische Gesundheit, ist therapeutische Unterstützung richtig. Muss ein Team gemeinsam entscheiden, ersetzt individuelles Sparring keinen moderierten Organisationsprozess.</p>''',
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
