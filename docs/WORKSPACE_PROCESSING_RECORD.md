# Verzeichnis von Verarbeitungstätigkeiten – Workspace und Async Sparring

Stand: 6. September 2026  
Verantwortlicher: Christian Leonhardt, Am Spelzgarten 18, 50129 Bergheim,
kontakt@cleonhardt.de

Diese Dokumentation bildet die Workspace-Verarbeitung nach Art. 30 DSGVO ab und
muss bei Änderungen von Dienstleistern, Datenflüssen oder Fristen aktualisiert
werden.

## Zwecke

- persönliche Kontoeinrichtung, Authentifizierung und Zugriffskontrolle
- Bereitstellung des ChOS-Workspace und der freigegebenen Inhalte
- Anbahnung und Durchführung fallbezogener B2B-Beratung
- Intake, Chat, Status und Abschluss des Async Clarity Sparring
- inhaltsfreie Benachrichtigung über neue Workspace-Nachrichten
- Missbrauchsabwehr, Sicherheitsnachweis und Wiederherstellung
- Bearbeitung von Betroffenenrechten und Datenschutzvorfällen

## Betroffene Personen

- persönlich eingeladene Nutzer und Ansprechpartner von Unternehmenskunden
- Christian Leonhardt als privilegierter Workspace-Nutzer
- versehentlich in Freitext erwähnte Personen; diese Angaben sind nicht vorgesehen
  und sollen unverzüglich minimiert oder entfernt werden

## Datenkategorien

- Stammdaten: Name, geschäftliche E-Mail, Rolle, Konto-ID
- Zugangs-/Sicherheitsdaten: Passwort-Hash, Einladungs- und Sitzungsdaten,
  Zugriffszeiträume, verschlüsselter MFA-Schlüssel, inhaltsfreie Auditdaten
- ChOS-Arbeitsdaten: vom Nutzer angelegte Fälle und Bearbeitungsstände
- Sparring-Daten: Produkt-Snapshot, Intake, Datenregelversion und Zeitpunkt,
  Nachrichten, Zeit-/Lesestatus, Engagementstatus
- technische Daten: IP-Adresse und erforderliche Verbindungs-/Fehlerdaten, soweit
  sie auf Infrastruktur- oder Sicherheitsebene tatsächlich anfallen
- Benachrichtigungsdaten: Empfängeradresse, Versandstatus und konstante Vorlage;
  keine Chat- oder Intake-Inhalte

Besondere Kategorien nach Art. 9 DSGVO und Drittdaten sind nicht vorgesehen.

## Rechtsgrundlagen

- Art. 6 Abs. 1 lit. b DSGVO: Vertrag/Vertragsanbahnung, wenn die betroffene Person
  selbst Vertragspartei ist
- Art. 6 Abs. 1 lit. f DSGVO: Ansprechpartner eines Unternehmenskunden,
  Zugriffsschutz, IT-Sicherheit, Missbrauchsabwehr und inhaltsfreier Nachweis;
  Interessen: Durchführung der angefragten Geschäftsbeziehung und sicherer Betrieb
- Art. 6 Abs. 1 lit. c DSGVO in Verbindung mit der jeweils einschlägigen Pflicht:
  gesetzlich erforderliche Aufbewahrung und Behandlung meldepflichtiger Vorfälle

Die Kenntnisnahme der Datenregeln ist keine Einwilligung. Art.-9-Daten werden nicht
auf Basis einer vorsorglichen Einwilligung zum regulären Produktbestand gemacht.

## Empfänger und Auftragsverarbeiter

- Hostinger für VPS-, Hosting-, Backup- und E-Mail-Leistungen
- Christian Leonhardt und nur erforderliche, verpflichtete Administration
- keine Chatübermittlung an HubSpot, Analyse-, KI- oder Messenger-Dienste

Hostinger nennt Hosting, VPS und E-Mail als vom veröffentlichten DPA erfasste
Dienste. Die im konkret gebuchten Konto geltende Vertragsfassung, Serverregion,
Subprozessorliste und Widerspruchsfrist bei Änderungen sind regelmäßig zu sichern
und zu prüfen.

## Drittlandbezug

Hostinger sieht für Übermittlungen außerhalb des EWR Standardvertragsklauseln und
eine veröffentlichte Subprozessorliste vor. Ob und welche Transfers beim konkret
gebuchten VPS-, Backup- und E-Mail-Setup tatsächlich stattfinden, ist anhand des
Hostinger-Kontos und der jeweils geltenden Liste zu dokumentieren. Es wird nicht
behauptet, sämtliche Verarbeitung finde ausschließlich in Deutschland statt.

## Löschfristen

- Sparring/Intake: grundsätzlich 30 Tage nach Abschluss oder Stornierung
- nicht begonnene Intake-Fälle: spätestens nach 90 Tagen
- inhaltsfreies Workspace-Audit: grundsätzlich 90 Tage
- Konten/ChOS-Serverdaten: nach vereinbarter Zugangs-/Lesephase
- lokale Browserkopien: Nutzerlöschung je Gerät; serverseitige Löschung erreicht
  frühere Geräte nicht automatisch
- lokale verschlüsselte Vorab-Backups: Entfernung beim nächsten Vorab-Lauf, wenn
  älter als 30 Tage; tatsächlicher Zeitpunkt kann bei ausbleibenden Änderungen
  später liegen
- wöchentliches Hostinger-VPS-Backup: Rotation nach gebuchtem Tarif; konkrete
  Retention im Anbieter-Nachweis ergänzen
- Rechnungen/Buchungsbelege: getrennt, regelmäßig acht Jahre; Geschäftsbriefe je
  nach Einordnung regelmäßig sechs Jahre

Bei Restore wird das getrennte Löschjournal angewandt, damit produktiv bereits
gelöschte Inhalte nicht dauerhaft wiederkehren.

## Technische und organisatorische Maßnahmen

- TLS, restriktive Sicherheitsheader und kein öffentlicher Workspace-Index
- persönliche Einladung, starke Passwort-Hashes, sichere Cookies, MFA für Owner,
  30-Minuten-Idle- und 12-Stunden-Maximalsitzung
- serverseitige Objektberechtigung für jeden Sparring-Zugriff, Nonces und
  Eingabe-/Rate-Limits
- AES-256-GCM für Sparring-Speicher; Keyring getrennt und nicht in Git
- GPG-verschlüsselte Vorab-Backups; privater Schlüssel nicht auf dem VPS
- keine Chat-/Intake-Inhalte in URLs, E-Mails, Analytics, Audit oder Logs
- kein Offline-/Service-Worker-Cache für Chat/Intake; keine Uploads, Bots oder KI
- dokumentierte Export-, Einschränkungs-, Einzel- und Gesamtlöschung
- getrenntes Löschjournal für Restore-Abgleich

## Regelmäßige Kontrolle

- mindestens jährlich und nach wesentlichen Änderungen prüfen
- Hostinger-DPA, Subprozessoren, Serverregion und Backup-Retention dokumentieren
- Berechtigungs-, Restore-, Lösch-, Dependency- und SMTP-Test durchführen
- Datenschutzvorfälle nach dem Incident-Prozess bewerten und dokumentieren

