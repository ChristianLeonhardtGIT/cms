import info.magnolia.context.MgnlContext
import javax.jcr.Node

def website = MgnlContext.getJCRSession('website')
println 'Insight-Vertiefung gestartet.'

Map<String, String> depth = [
  'warum-produktorganisationen-nicht-an-fehlenden-methoden-scheitern': '''
<h3>Ein typisches Muster</h3><p>Eine Organisation führt ein neues Priorisierungsverfahren ein. Die Kriterien sind nachvollziehbar, die Workshops gut moderiert und dennoch werden wenige Wochen später dieselben Entscheidungen erneut geöffnet. Das Verfahren ist nicht zwingend falsch. Es arbeitet nur an der sichtbaren Oberfläche, während Bereiche weiterhin unterschiedliche Ziele verfolgen und niemand das Mandat besitzt, eine Entscheidung verbindlich zu schließen.</p>
<h3>Die ChOS-Sicht</h3><p>Unter <strong>Richtung</strong> zeigt sich, ob ein gemeinsames Ergebnis existiert. <strong>Entscheidungen</strong> klären, wer eine Priorität schließen darf. <strong>Verantwortung</strong> fragt, wer ihre Folgen trägt. <strong>Zusammenarbeit</strong> macht sichtbar, welche Interessen zusammengebracht werden müssen. Unter <strong>Lernen</strong> wird geprüft, ob die Priorisierung tatsächlich bessere Ergebnisse erzeugt. Erst dieses Zusammenspiel zeigt, ob eine Methode unterstützt oder lediglich bestehende Widersprüche ordentlicher dokumentiert.</p>
<h3>Woran Sie ein Systemproblem erkennen</h3><ul class="mvp-checks"><li>Die Methode wird korrekt angewendet, aber Entscheidungen bleiben unverbindlich.</li><li>Ausnahmen werden regelmäßig außerhalb des vereinbarten Prozesses beschlossen.</li><li>Teams sollen Ergebnisse verantworten, dürfen relevante Zielkonflikte aber nicht entscheiden.</li><li>Jeder Bereich kann seine eigene Priorität sachlich gut begründen.</li></ul>
<h3>Ein kleiner Test</h3><p>Nehmen Sie eine einzige strittige Prioritätsentscheidung. Benennen Sie ein gemeinsames Erfolgskriterium, die entscheidende Rolle, notwendige Beiträge und die Bedingungen, unter denen die Entscheidung wieder geöffnet werden darf. Beobachten Sie vier Wochen lang nicht die Einhaltung der Methode, sondern die tatsächlichen Eingriffe und Ausnahmen.</p>
<h3>Wann eine neue Methode dennoch hilft</h3><p>Wenn Richtung, Mandat und Verantwortung grundsätzlich geklärt sind, kann ein Framework Sprache vereinheitlichen und Entscheidungen beschleunigen. Die Diagnose richtet sich also nicht gegen Methoden. Sie verhindert nur, dass eine bekannte Lösung für einen ungeklärten Mechanismus eingesetzt wird.</p>''',

  'diagnose-vor-eingriff': '''
<h3>Warum vorschnelle Lösungen so überzeugend wirken</h3><p>Unter Zeitdruck wird eine plausible Erklärung leicht mit einer bewiesenen Ursache verwechselt. Wenn Entscheidungen langsam sind, klingt ein kleineres Gremium vernünftig. Wenn Teams wenig Initiative zeigen, klingt mehr Verantwortung vernünftig. Beide Eingriffe können helfen – oder das Problem verschärfen, wenn fehlende Informationen, reale Risiken oder widersprüchliche Ziele der eigentliche Grund sind.</p>
<h3>Eine einfache Evidenzleiter</h3><ol><li><strong>Vorfall:</strong> Was ist konkret geschehen?</li><li><strong>Muster:</strong> In welchen weiteren Situationen tritt es auf?</li><li><strong>Hypothese:</strong> Welcher Mechanismus könnte das Muster erklären?</li><li><strong>Gegenbeleg:</strong> Wo müsste das Muster auftreten, tut es aber nicht?</li><li><strong>Test:</strong> Welche kleine Veränderung unterscheidet zwei Erklärungen?</li></ol>
<h3>Beispiel: eine wiederholt geöffnete Entscheidung</h3><p>Die erste Erklärung lautet häufig: Stakeholder akzeptieren den Prozess nicht. Eine zweite lautet: Die Entscheidung verwendet Kriterien, die für Vertrieb und Produkt unterschiedliche Folgen haben. Eine dritte: Niemand besitzt das sichtbare Mandat, einen Konflikt zu schließen. Ein weiterer Workshop prüft keine dieser Erklärungen. Ein befristetes Entscheidungsmandat oder ein gemeinsames Erfolgskriterium dagegen schon.</p>
<h3>Die fünf Perspektiven als Gegenprüfung</h3><p>ChOS verhindert den Tunnelblick: Ist das gewünschte Ergebnis klar? Liegt die Entscheidung am richtigen Ort? Passen Verantwortung und Mandat zusammen? Unterstützen Beziehungen und Routinen die Vereinbarung? Gibt es eine Rückmeldung, an der Wirkung erkennbar wird?</p>
<h3>Grenze der Diagnose</h3><p>Nicht jede Entscheidung benötigt eine umfangreiche Untersuchung. Bei reversiblen, risikoarmen Fragen ist schnelles Handeln oft die beste Erkenntnisquelle. Je größer Wirkung, Reichweite und Irreversibilität eines Eingriffs sind, desto wichtiger wird jedoch eine belastbare Diagnose.</p>''',

  'unklare-rollen-sind-selten-das-eigentliche-problem': '''
<h3>Warum Rollendokumente im Alltag verblassen</h3><p>Eine Rolle kann formal eindeutig und praktisch wirkungslos sein. Ein Product Manager soll beispielsweise die Produktstrategie verantworten, während Budget, Personal, technische Risiken und Vertriebszusagen an anderen Orten entschieden werden. Das Problem ist dann nicht mangelndes Rollenverständnis, sondern eine Verantwortung ohne ausreichende Mittel.</p>
<h3>Vier Tests für eine belastbare Rolle</h3><ol><li><strong>Ergebnis:</strong> Ist klar, welches Ergebnis die Rolle besitzt?</li><li><strong>Mandat:</strong> Darf sie die dafür notwendigen Entscheidungen treffen?</li><li><strong>Information:</strong> Erreichen sie Kundenwissen, Wirtschaftsdaten und Risiken rechtzeitig?</li><li><strong>Konsequenz:</strong> Kann sie aus Ergebnissen lernen und die nächste Entscheidung verändern?</li></ol>
<h3>Der kritische Moment ist der Konflikt</h3><p>Rollen wirken nicht in ruhigen Situationen, sondern wenn Ziele kollidieren. Wer entscheidet bei einem Konflikt zwischen Liefertermin, technischem Risiko und Kundennutzen? Wer darf Nein sagen? Wann darf Führung eingreifen? Wenn diese Fragen offenbleiben, wird jede Rollenbeschreibung im Ernstfall von informeller Macht ersetzt.</p>
<h3>Ein sinnvoller Einstieg</h3><p>Wählen Sie drei wiederkehrende Entscheidungen und zeichnen Sie ihren tatsächlichen Weg nach. Vergleichen Sie formales Mandat und beobachtetes Verhalten. Vereinbaren Sie anschließend nur für diese Entscheidungen Ergebnis, Entscheidungsrecht, benötigte Beiträge und Eskalationsbedingung. Das ist überprüfbarer als eine vollständige neue Rollenmatrix.</p>
<h3>Wann Dokumentation wichtig wird</h3><p>Dokumentation ist wertvoll, sobald die Vereinbarung praktisch erprobt wurde. Dann hält sie ein funktionierendes Muster fest. Wird sie zu früh erstellt, konserviert sie oft nur Annahmen darüber, wie die Organisation funktionieren sollte.</p>''',

  'rollen-und-verantwortlichkeiten-in-produktorganisationen-klaeren': '''
<h3>Verantwortung ist mehr als Zuständigkeit</h3><p>Zuständigkeit beantwortet, wer eine Aufgabe bearbeitet. Verantwortung beantwortet, wer ein Ergebnis besitzt, Entscheidungen treffen kann und aus den Folgen lernt. Viele Produktorganisationen verteilen Aufgaben sehr genau, lassen aber offen, wer bei konkurrierenden Zielen eine verbindliche Wahl trifft.</p>
<h3>Beispiel: Produkt gegen Plattform</h3><p>Ein Produktteam benötigt kurzfristig eine Sonderlösung, die Plattformverantwortliche ablehnen. Beide Seiten handeln nachvollziehbar: Das Produkt schützt ein Kundenergebnis, die Plattform langfristige Stabilität. Eine RACI-Matrix löst den Zielkonflikt nicht. Nötig sind gemeinsame Kriterien, eine klar benannte Entscheidung und ein Ort, an dem die Folgen beider Optionen sichtbar werden.</p>
<h3>Entscheidungskarte statt Rollenmatrix</h3><p>Eine kleine Entscheidungskarte enthält: die konkrete Entscheidung, das gewünschte Ergebnis, die entscheidende Rolle, verpflichtend einzubeziehende Perspektiven, Grenzen des Mandats und einen Eskalationsgrund. Sie wird an echten Fällen getestet und erst danach verallgemeinert.</p>
<h3>Diagnosefragen</h3><ul class="mvp-checks"><li>Wer kann heute faktisch blockieren, obwohl die Rolle es nicht vorsieht?</li><li>Wer trägt Folgen, ohne entscheiden zu dürfen?</li><li>Welche Entscheidung wird in mehreren Kreisen getroffen?</li><li>Wo ersetzt Eskalation eine fehlende Regel?</li><li>Welche Information fehlt am Entscheidungsort?</li></ul>
<h3>Grenze</h3><p>Nicht jede Schnittstelle braucht eine feste Entscheidungsregel. Bei seltenen oder neuartigen Fragen kann ein bewusster gemeinsamer Entscheid sinnvoller sein. Klarheit bedeutet nicht maximale Formalisierung, sondern eine zur Häufigkeit und zum Risiko passende Vereinbarung.</p>''',

  'wann-braucht-eine-produktorganisation-ein-operating-model': '''
<h3>Das Operating Model ist bereits da</h3><p>Jede Organisation besitzt ein Operating Model – auch wenn es nie bewusst gestaltet wurde. Es steckt in Budgetzyklen, Gremien, Zielsystemen, Informationswegen, Teamgrenzen und Führungsroutinen. Die Frage ist deshalb nicht, ob ein Operating Model benötigt wird, sondern ob das bestehende noch zur Strategie passt.</p>
<h3>Drei typische Auslöser</h3><p><strong>Wachstum:</strong> Informelle Abstimmung funktioniert nicht mehr. <strong>Strategiewechsel:</strong> Neue Ziele treffen auf alte Finanzierung und Zuständigkeiten. <strong>Neue Technik:</strong> Arbeit wird schneller oder automatisierbar, doch Prüfungen und Entscheidungen bleiben unverändert. In allen drei Fällen entsteht Reibung, weil einzelne Elemente sich schneller verändern als das Gesamtsystem.</p>
<h3>Nicht beim Organigramm beginnen</h3><p>Starten Sie mit einem wichtigen Wertstrom und verfolgen Sie, wie ein Kundenergebnis entsteht. Wo warten Entscheidungen? Wo wechseln Ziele? Welche Einheit optimiert lokal? Erst danach werden Teamgrenzen und Rollen betrachtet. Sonst wird eine neue Struktur um alte Entscheidungswege gebaut.</p>
<h3>Fünf ChOS-Prüffragen</h3><ul class="mvp-checks"><li>Richtung: Ist das gemeinsame Ergebnis übersetzbar?</li><li>Entscheidungen: Liegen häufige Entscheidungen nahe an der nötigen Information?</li><li>Verantwortung: Passen Ergebnis, Mandat und Konsequenz zusammen?</li><li>Zusammenarbeit: Unterstützen Schnittstellen den Wertstrom?</li><li>Lernen: Verändert Rückmeldung tatsächlich Prioritäten und Strukturen?</li></ul>
<h3>Wann kein Redesign nötig ist</h3><p>Einzelne Konflikte, personelle Spannungen oder ein vorübergehender Engpass rechtfertigen noch kein neues Operating Model. Ein Redesign wird sinnvoll, wenn dieselben Muster über Teams und Situationen hinweg auftreten und durch lokale Anpassungen nicht verschwinden.</p>''',
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
