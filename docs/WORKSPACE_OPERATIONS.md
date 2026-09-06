# Workspace: Betrieb und Wiederherstellung

Stand 6. September 2026. Ersetzt die technischen offenen Punkte aus dem ersten
Implementierungsstand; kommerzielle Freigabe bleibt separat.

## Änderungen

- Nodemailer (TLS obligatorisch, Protokoll-/Inhaltslogging aus) und OTPAuth mit
  festen Versionen/Lockfile. Quellen: https://nodemailer.com/smtp und
  https://github.com/hectorm/otpauth (am 6. September geprüft).
- Verschlüsselung des gesamten Sparring-Dateispeichers mit AES-256-GCM,
  zufälliger 96-Bit-Nonce, authentisiertem Zweck/Schlüsselkennung und Keyring.
  Bestandsdateien werden beim nächsten atomaren Schreiben migriert.
- Schlüssel ausschließlich über `WORKSPACE_KEYRING_FILE`. Alte Schlüssel bei
  Rotation zum Lesen behalten; aktive Version wechseln, Datei atomar neu schreiben.
  Keine Behauptung von Ende-zu-Ende-Verschlüsselung: das Backend entschlüsselt.
- Owner richtet unter `/workspace/mfa` einen Authenticator ein. Bestätigung
  verlangt aktuelles Passwort und gültigen Code. Codes werden nur einmal
  akzeptiert. Weitere Sitzungen enden. Produktion verlangt MFA für Sparring.
  Passwortanmeldung mit bestehendem MFA verlangt auch den Code. Bestehender
  ChOS-Zugang bleibt bis zur persönlichen Einrichtung erreichbar.
- Sitzungen: höchstens 12 Stunden; 30 Minuten ohne Seiten-/Formularaktivität
  beenden die Sitzung. Hintergrund-Polling verlängert sie nicht.
- Audit enthält nur Akteur-ID, Aktion, Objekt-ID und Zeit. 90 Tage Retention.
- Benachrichtigung wartet fünf Minuten und wird gebündelt, ohne Inhalt/Thema.
  Erst SMTP-Annahme quittiert die Gruppe. Ohne SMTP-Konfiguration kein Versand.
  Ohne neue Lesebestätigung gibt es keine wiederholten Erinnerungsmails.

## Konfiguration

Produktiv: `secrets/workspace/workspace-keyring.json` (0600, Owner Container-UID
100) und `privacy-ledger/` (0700, UID 100). Das Löschjournal liegt außerhalb des
Datenvolumes und darf beim Rückspielen eines alten Volumes nicht ersetzt werden.

SMTP-Konfiguration als Datei mit `host`, `port` (465 oder 587), `user`,
`password`, `from` nach `secrets/workspace/smtp.json`. Dann
`WORKSPACE_SMTP_FILE=/app/private/smtp.json` in der Compose-Umgebung setzen.
Keine Beispiel-Zugangsdaten in Git. Anbieter anhand bestehender Mailinfrastruktur
festlegen; Verbindungsprüfung und Testzustellung erst nach verfügbarer Konfiguration.

`WORKSPACE_SPARRING_ENABLED` bleibt false bis Mail, persönliche MFA-Einrichtung,
Datenschutzinformationen und Vertragsprüfung abgeschlossen sind.

## Auskunft, Einschränkung und gezielte Löschung

Root-/Betriebszugriff führt `node privacy-admin.mjs COMMAND /geschuetzter/auftrag.json`
im Portalcontainer aus. Identität und Berechtigung vorher organisatorisch prüfen.
Auftragsdatei enthält IDs, keine Chatinhalte. Datei nur temporär und 0600 ablegen.

- `export`: `userId`, `output`; erstellt einmalig eine 0600-Datei mit Konto und
  eigenen Engagements. Keine Passwort-Hashes, MFA-Schlüssel oder Einladungstokens.
  Export vor Herausgabe auf Rechte anderer Personen prüfen, sicher übermitteln
  und danach entfernen.
- `restrict` / `unrestrict`: `userId`, `engagementId`; sperrt bzw. erlaubt weitere
  Leistungsbearbeitung. Lesbarkeit für Betroffenenrechte bleibt bestehen.
- `erase-message`: zusätzlich `messageId`; Inhalt wird gelöscht und ein minimales
  Löschereignis außerhalb des Datenvolumes dokumentiert.
- `erase-intake`: entfernt Intake-Freitext vollständig, ebenfalls mit Löschereignis.
- Kontoberichtigung erfolgt über die bestehende Kontopflege; inhaltliche
  Berichtigung über Entfernung einer falschen Nachricht und neue korrigierte
  Nachricht. Vollständige Kontolöschung über bestehende Selbst-/Adminfunktion.
- `reconcile` ohne Auftragsdatei: Löschjournal/Retention anwenden, verwaiste Fälle
  entfernen. Vor Wiederfreigabe eines Restores ausführen.

Das Journal enthält ausschließlich Lösch-IDs/Zeitpunkte. Es verhindert beim
Lesen erneut die Anzeige gelöschter Inhalte, auch wenn ein alter Snapshot
zurückgespielt wird. Journal mindestens bis zum Ablauf aller betroffenen
Sicherungskopien halten; beim Restore immer die neueste separate Kopie verwenden.

## Verschlüsselte Vorab-Backups

`deploy/backup.sh` benötigt `secrets/backup-recipient.asc` (öffentlicher Schlüssel).
Es erstellt GPG-verschlüsselte Volume-, Projekt-, Secrets-/Umgebungs- und
Systemschlüsselarchive sowie Prüfsummen. Private Entschlüsselungsschlüssel bleiben
auf Christians Rechner unter dessen geschützter Wiederherstellungsablage.
Backups enthalten damit auch den Workspace-Keyring. Schlüsselverlust verhindert
Wiederherstellung: diese lokale Ablage separat gesichert und verfügbar halten.

Die vereinbarte Betriebsregel lautet: unmittelbar vor jeder produktiven
Änderung einen verschlüsselten Snapshot mit `cleonhardt-backup.service`
erstellen, `COMPLETE` und alle Prüfsummen kontrollieren und erst danach ändern.
Der frühere tägliche lokale Timer wird dauerhaft deaktiviert. Vorhandene Archive
bleiben unangetastet und lokale Vorab-Sicherungen werden 30 Tage aufbewahrt.
Hostinger erstellt laut bestätigter Kontokonfiguration einmal pro Woche ein
VPS-Backup als zusätzliche anbieterbetriebene Sicherungsebene.

Bestehende unverschlüsselte Altsicherungen werden nicht durch ein neues Skript
nachträglich verschlüsselt. Ihre Aufbewahrung und Umstellung separat prüfen.
Der Rollout-Snapshot vom 6. September wurde zusätzlich auf Christians Rechner
kopiert; dies ist keine laufende automatische Sicherung.

Restore: Prüfsummen vergleichen, GPG auf dem berechtigten Wiederherstellungsgerät
entschlüsseln, in isolierte frische Volumes extrahieren, neuestes Löschjournal
beibehalten, richtigen Keyring montieren, `privacy-admin.mjs reconcile`, dann
Anmeldung/Autorisation/Inhalt prüfen. Niemals Restore direkt über laufende Volumes.

## Restrisiken und externe Voraussetzungen

Hostinger-Vertrag, tatsächlicher Standort, VPS-Backup-Retention und
Subprozessoren anhand des Kontos verifizieren. Die SMTP-Anmeldedaten fehlen noch.
Eine qualifizierte juristische Freigabe kann die technische Implementierung
nicht selbst erzeugen.
