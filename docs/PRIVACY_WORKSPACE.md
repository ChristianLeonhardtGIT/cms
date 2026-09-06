# Sparring: technischer Datenschutzstand und Pilotfreigabe

Aktualisierung: Die Sicherheits-/Betriebsergänzungen vom 6. September stehen in [WORKSPACE_OPERATIONS.md](WORKSPACE_OPERATIONS.md); die folgende Bestandsaufnahme dokumentiert den ersten Implementierungsschritt.

Stand: 6. September 2026. Technische Bestandsaufnahme, keine abschließende
rechtliche Bewertung. Pilot standardmäßig deaktiviert.

## Getrennte Datenwege

Browser → HTTPS/Caddy → bestehendes Node-Portal → bestehendes Datenvolume.
Sparring liegt unter `sparring/store.json` (Schema 1), getrennt von Konten und
ChOS-Operationsjournal. Kein Transfer an Magnolia, HubSpot, Wissensindex,
Brain, KI oder sonstige Chatdienste. Keine Upload-Endpunkte. Das Portal
benötigt keine neue externe Datenbank und hat keine zusätzlichen Abhängigkeiten.

| Daten | Zweck und Ort | Lebenszyklus im Implementierungsstand |
| --- | --- | --- |
| Konto, Passwort-Hash, bestehende Einladung | bestehende Authentifizierung/accounts.json | bestehende Konto-Laufzeit; Selbst-/Adminlöschung |
| Produkt-Snapshot, Konto-/Coach-ID | Zuordnung und vereinbarter Leistungsumfang | zusammen mit Sparring gelöscht |
| Intake, Regelversion und Bestätigungszeit | Auftragsklärung; Kenntnisnahme der Datenregeln, keine Einwilligung | 30 Tage nach Abschluss/Storno |
| Nachrichten und Lesestand | persönliche Beratung | 30 Tage nach Abschluss/Storno |
| Benachrichtigungs-Merkposten | spätere gebündelte Zustellung | ohne Inhalte/Adresse; Lesebestätigung oder Sparring-Löschung |
| Vertrags- und Rechnungsunterlagen | außerhalb dieses Moduls, manuelle Beauftragung | separat festzulegen; keine Begründung für Chatarchivierung |

Die Retention ist im Repository-Adapter konfigurierbar (Standard 30 Tage);
Produkttext und Dokumentation müssen bei Änderung mitangepasst werden.
Automatischer Ablauf beendet aktive Engagements anhand Europe/Berlin.
Cleanup läuft beim Start und alle sechs Stunden; bis dahin kann ein zur
Löschung fälliger Datensatz noch existieren. Unabgeschlossene Intakes werden
nach 90 Tagen entfernt. Kontolöschung entfernt zugeordnete Sparrings; Cleanup
entfernt zusätzlich verwaiste Zuordnungen. Löschen entfernt den ganzen
Datensatz einschließlich Freitext, nicht nur ein Flag.

## Technische Schutzmaßnahmen

- Bestehende serverseitige Sitzung; jedes Lesen und Schreiben prüft Client-ID
  oder die zugeordnete Owner-ID. Andere Owner erhalten keine pauschalen Rechte.
- Schreibaktionen benötigen ein einmaliges, sitzungs- und fallgebundenes Nonce.
  Keine CORS-Freigabe, Cross-site-Formularaufrufe werden abgelehnt.
- Nur formcodierter Text; Größen-/Zeichen-/Nachrichtenlimits, kurze Sendepause,
  idempotente Nachrichtenkennung. HTML wird escaped, Polling benutzt textContent.
- Antworten mit `no-store`, kein Chat in URLs, Browser-Speichern oder Reader-Cache.
  Der Reader-Worker verwendet eine enge Positivliste und behandelt Sparring nicht.
- Keine Telemetrie, Inhaltsvorschau per Mail oder KI-Verarbeitung. Fehlerlogs
  enthalten feste Meldungen statt exception.message. Server-Access-Logging ist
  für die öffentliche Domain in Caddy nicht aktiviert.
- Privates Verzeichnis 0700, atomare Dateien 0600, Dateisperre für parallele
  Schreibvorgänge, Schema-Version. Maximal 100 Datensätze, 500 Nachrichten je
  Sparring, 16 MiB Gesamtbestand. Einzelner Portalprozess wie bisher.
- Anwendung hält Freitext nur im Arbeitsspeicher des geöffneten Browsers;
  keine dauerhafte Offline-Kopie. Das verhindert keine Screenshots, Browser-
  Erweiterungen, Betriebssystem-Swap oder Übertragung durch fremde Tastaturhilfen.

## Noch vor kommerzieller Aktivierung zu erledigen

1. Qualifizierte Prüfung der Leistungsbedingungen, B2B-Abgrenzung,
   Vertraulichkeit, Verantwortlichkeiten und tatsächlichen Datenflüsse.
   Insbesondere Rechtsgrundlage für Ansprechpartner beim Unternehmenskunden
   gesondert klären; Vertragserfüllung nicht pauschal auf jede Person übertragen.
2. Workspace-Datenschutzinformation fertigstellen: Zwecke, Datenkategorien,
   Rechtsgrundlagen, Empfänger, Aufbewahrung, Löschung und Betroffenenrechte.
   Hinweise und Regelbestätigung ersetzen diese Information nicht.
3. Hostinger/VPS-Standort, Vertrag/AVV, Unterauftragnehmer, mögliche Transfers,
   Backups und Zugriffsbefugnisse anhand aktueller Unterlagen prüfen.
4. Verschlüsselung ruhender Daten und Sicherungen klären. JSON-Freitext ist
   derzeit **nicht anwendungsseitig verschlüsselt**. Das vorhandene Backup-
   Skript erzeugt unverschlüsselte tar.gz-Dateien; Dateirechte ersetzen keine
   Verschlüsselung. Neu: restriktive umask 077 für künftige Sicherungen.
   Offsite-Ziel, Schlüsselwiederherstellung und vollständiger Restore bleiben offen.
5. Restore-Prozedur: vor Wiederfreigabe Löschaufträge seit dem Snapshot erneut
   anwenden und Retention laufen lassen. Manuelle Löschaufträge benötigen eine
   getrennte minimale Nachweisliste. Automatischer Restore-Abgleich fehlt noch.
6. Privilegierte Anmeldung stärken (MFA), Idle-Timeout ergänzen und Auditierung
   von Zugriffen/Export/Löschung ohne Inhalte einführen. Bestehende Sessions
   haben 12 Stunden Maximaldauer, aber keinen separaten Idle-Timeout.
7. Auskunft/Export/Berichtigung/Einschränkung und gezielte Entfernung versehentlich
   eingegebener sensibler Daten als dokumentierten Adminprozess fertigstellen.
   Kontolöschung und zeitgesteuerte Löschung sind umgesetzt; eine komplette
   Betroffenenrechte-Oberfläche ist nicht vorhanden. Sicherheitsvorfälle
   getrennt bewerten; verbotene Eingaben sind nicht automatisch meldepflichtig.
8. SMTP-Anbieter und dessen Datenverarbeitung festlegen. Noch kein Versand!
   Vorgemerkte Benachrichtigung erst nach erfolgreichem Versand quittieren;
   konstante Betreff-/Textvorlage, nur Workspace-Link, keine Inhalte verwenden.
9. Betreibertest nach Review/Merge: TLS, Proxy, Altlinks/Worker-Migration,
   Backup/Restore, Zustellweg, Löschlauf und Autorisierung auf dem VPS prüfen.

B2C, Checkout, Dateien und KI bleiben außerhalb dieses Ausbauschritts. Die
B2B-Bestätigung ist eine fachliche Abfrage, keine automatische rechtliche
Qualifikation. Auftragsannahme und Rechnung erfolgen weiterhin manuell.
