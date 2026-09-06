# Sparring: technischer Datenschutzstand und Pilotfreigabe

Aktualisierung: Die Sicherheits-/Betriebsergänzungen vom 6. September stehen in [WORKSPACE_OPERATIONS.md](WORKSPACE_OPERATIONS.md); die folgende Bestandsaufnahme dokumentiert den ersten Implementierungsschritt.

Stand: 6. September 2026. Technische Bestandsaufnahme; die juristische Vorprüfung
steht in `WORKSPACE_LEGAL_REVIEW.md`. Der Bereich ist intern für persönlich
freigeschaltete Konten aktiv. Öffentliche Registrierung und Checkout sind aus.

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

## Stand der Pilotfreigabe

1. Leistungsbedingungen, B2B-Abgrenzung, Vertraulichkeit,
   Verantwortlichkeiten und FernUSG-Risiko sind in der juristischen Vorprüfung
   bewertet. Vor jedem Auftrag den individuellen Angebotsprozess anwenden.
2. Die Workspace-Datenschutzinformation mit Zwecken, Datenkategorien,
   Rechtsgrundlagen, Empfängern, Aufbewahrung, Löschung und Betroffenenrechten ist
   veröffentlicht. Hinweise und Regelbestätigung bleiben davon getrennt.
3. Hostinger/VPS-Standort, Vertrag/AVV, Unterauftragnehmer, mögliche Transfers,
   Zugriffsbefugnisse und Retention des bestätigten wöchentlichen VPS-Backups
   anhand aktueller Unterlagen prüfen.
4. Der Sparring-Speicher und neue Vorab-Sicherungen sind verschlüsselt. Der VPS
   erhält nur den öffentlichen Backup-Empfänger; der private
   Entschlüsselungsschlüssel bleibt in Christians Wiederherstellungsablage. Vor
   jeder Produktionsänderung wird ein verschlüsselter Snapshot erstellt und
   geprüft. Bestehende unverschlüsselte Altarchive bleiben gesondert zu bewerten.
5. Die Restore-Prozedur erhält das getrennte Löschjournal und führt vor
   Wiederfreigabe `privacy-admin.mjs reconcile` aus. Einen vollständigen
   Bare-Metal-Restore noch als Betriebsübung protokollieren.
6. MFA, 30-Minuten-Idle-Timeout und inhaltsfreies Zugriffsaudit sind technisch
   umgesetzt; die persönliche Owner-MFA ist produktiv eingerichtet.
7. Auskunft/Export, Einschränkung und gezielte Entfernung versehentlich
   eingegebener sensibler Daten sind als geschützter Adminprozess dokumentiert.
   Kontolöschung und zeitgesteuerte Löschung sind umgesetzt; eine komplette
   Betroffenenrechte-Oberfläche ist nicht vorhanden. Sicherheitsvorfälle
   getrennt bewerten; verbotene Eingaben sind nicht automatisch meldepflichtig.
8. Hostinger Email ist als SMTP-Weg eingerichtet; Absender und Benutzername sind
   `kontakt@cleonhardt.de`. TLS-Anmeldung und inhaltsfreie Testzustellung wurden
   produktiv geprüft. Vormerkungen werden erst nach erfolgreichem Versand
   quittiert; die Vorlage enthält nur den Workspace-Link und keine Inhalte.
9. Produktiver Betreibertest für TLS, Proxy, Altlinks/Worker-Migration,
   Zustellweg und Autorisierung ist erfolgt. Ein vollständiger Bare-Metal-Restore
   bleibt eine spätere Betriebsübung.

B2C, Checkout, Dateien und KI bleiben außerhalb dieses Ausbauschritts. Die
B2B-Bestätigung ist eine fachliche Abfrage, keine automatische rechtliche
Qualifikation. Auftragsannahme und Rechnung erfolgen weiterhin manuell.
