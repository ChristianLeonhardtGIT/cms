# Meine Website

Dieses Magnolia-Light-Module liefert die Vorlage **Standardseite** und vier frei kombinierbare Inhaltsbausteine:

- Hero / Seitenaufmacher
- Textabschnitt
- Karte / Teaser
- Handlungsaufforderung

## Footer pflegen

Die App **Footer** verwaltet den Text und die Links in der Fußzeile. Lege dort genau einen Eintrag an, füge beliebig viele Links hinzu und veröffentliche den Eintrag anschließend.

Für Seiten wie **Datenschutz** oder **Impressum**:

1. Seite in der Seiten-App anlegen.
2. In den Seiteneinstellungen **In der Hauptnavigation ausblenden** aktivieren.
3. Seite veröffentlichen.
4. Seite in der Footer-App als Zielseite auswählen und den Footer-Eintrag ebenfalls veröffentlichen.

Solange noch kein Footer-Eintrag angelegt ist, wird der bisherige Footer-Text der Startseite als Rückfall verwendet.

## Navigation pflegen

Die App **Navigation** verwaltet das Hauptmenü unabhängig von der Seitenstruktur. Lege dort genau einen Eintrag an. Unter **Hauptmenü** kannst du Einträge sortieren und jeweils eine Zielseite sowie einen optionalen Linktext auswählen. Jeder Haupteintrag kann ein Untermenü der Ebene 2 enthalten; dort ist nochmals ein Untermenü der Ebene 3 möglich.

1. In der App **Navigation** einen Eintrag anlegen, zum Beispiel **Hauptnavigation**.
2. Unter **Hauptmenü** die gewünschten Seiten in der richtigen Reihenfolge ergänzen.
3. Bei Bedarf unter einem Eintrag **Untermenü – Ebene 2** und darunter **Untermenü – Ebene 3** pflegen.
4. Den Navigationseintrag veröffentlichen.

Solange kein Navigationseintrag veröffentlicht wurde, verwendet die Website weiterhin das automatisch aus der Seitenstruktur erzeugte Menü.

## Logo und zentrale Einstellungen pflegen

Die App **Website-Einstellungen** bündelt globale Angaben, die für die gesamte Website gelten. Aktuell enthält sie das Logo, dessen Alternativtext und das Linkziel des Logos; weitere zentrale Funktionen können später im selben Content Type ergänzt werden.

1. In der App **Website-Einstellungen** genau einen Eintrag anlegen.
2. Das Logo aus den Magnolia Assets auswählen und einen sinnvollen Alternativtext eintragen.
3. Unter **Linkziel des Logos** die Startseite auswählen.
4. Den Eintrag veröffentlichen.

Ist kein Logo gepflegt oder veröffentlicht, zeigt der Kopfbereich weiterhin den bisherigen Website-Namen als Rückfall an. Ist kein Linkziel gewählt, führt das Logo automatisch zur obersten Seite der Website.

Änderungen an YAML-, FreeMarker- und CSS-Dateien werden im Entwicklungsmodus von Magnolia automatisch erkannt.

## Strukturierte ChOS-Inhalte

Das Modul trennt Inhalt und Seitendarstellung. Neben dem bestehenden **Angebotskatalog** (`offer`, redaktionell: „Leistungen“) stehen folgende Content Apps bereit:

- **Problemsituationen** (`problem`): problemorientierte Einstiegsseiten mit Diagnose, Selbstcheck und nächstem Schritt
- **Tools und Checks** (`tool`): wiederverwendbare Selbstdiagnosen und Arbeitsmittel
- **Insights** (`insight`): vertiefende Fachinhalte; bestehende Insights-Seiten können schrittweise überführt werden
- **Cases** (`case`): Kontext, Diagnose, Eingriff, beobachtete Wirkung und Erkenntnisse
- **Themen** (`topic`): kontrollierte Taxonomie statt frei geschriebener Schlagworte

`offer` bleibt die einzige Quelle für Leistungen. Die neuen Typen verweisen über `relatedOffers` darauf; es gibt bewusst keinen zweiten Service-Typ.

### Empfohlene Reihenfolge beim Anlegen

1. Themen in **Themen** anlegen, zum Beispiel Entscheidungen, Verantwortung, Leadership oder AI & Organisation.
2. Bestehende Leistungen im **Angebotskatalog** verwenden.
3. Passende Tools und Insights anlegen.
4. Die Problemsituation erfassen und über die Referenzfelder mit Themen, Tools, Insights und Leistungen verbinden.
5. Cases ergänzen, sobald belastbare Beispiele vorliegen, und anschließend mit Problemsituation, Tool und Leistung verknüpfen.

Die Seitendarstellung liest später diese Referenzen aus. Dadurch werden Preis-, Leistungs- und Inhaltsdaten nur an einer Stelle gepflegt.

### Erste Problemsituationen

Das Schema ist anhand dieser vier Startinhalte gestaltet:

| Problemsituation | Buying Situation | Passender Check | Leistung |
|---|---|---|---|
| Führungskraft wird zum Bottleneck | `leadership-bottleneck` | Decision Bottleneck Check | Clarity Session |
| Entscheidungen dauern zu lange | `slow-decisions` | Decision Delay Check | Decision Review |
| AI Decision Rights | `ai-decision-rights` | Human/AI Decision Rights Check | zunächst Validierung, optionale Referenz |
| Neue Führungsrolle | `leadership-transition` | First Weeks Observation Map | Clarity/Sparring, später ggf. Transition-Angebot |

Die gemeinsamen SEO- und Publishing-Eigenschaften liegen als wiederverwendbare YAML-Fragmente unter `includes/content/`. Neue redaktionelle Content Types sollen diese Fragmente einbinden, statt die Felder erneut zu definieren.
