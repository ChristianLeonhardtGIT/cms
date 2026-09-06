# Wiederherstellung

## 1. Projektcode wiederherstellen

Das private Repository nach `/opt/cleonhardt` klonen und die nicht versionierten
Dateien aus dem sicheren Backup ergänzen:

- `secrets/hubspot.env`,
- Magnolia Password-Manager-Keypair,
- `secrets/workspace/workspace-keyring.json` für verschlüsselte Sparring-Daten,
- `secrets/workspace/smtp.json` für inhaltsfreie Workspace-Benachrichtigungen,
- `secrets/backup-recipient.asc` als öffentlicher Backup-Empfänger,
- das separate aktuelle Verzeichnis `privacy-ledger/` mit dem Löschjournal,
- weitere produktive Schlüssel oder Umgebungsdateien,
- `CMS_BASIC_AUTH_HASH` als einfach quotierte Umgebungsvariable, damit die
  Dollarzeichen des Caddy-kompatiblen bcrypt-Hashes erhalten bleiben.

Der private Backup-Entschlüsselungsschlüssel gehört nicht auf den VPS und nicht
in Git. Er bleibt in Christians geschützter Wiederherstellungsablage.

Anschließend Dateirechte prüfen: Workspace-Secrets `0600` und Eigentümer
Container-UID 100, Secret-Verzeichnisse sowie `privacy-ledger/` `0700`. Danach
Compose-Konfiguration prüfen und die Images neu bauen.

## 2. Redaktionelle Inhalte wiederherstellen

Die Exporte unter `content-snapshots/<datum>/` enthalten ausschließlich die
freigegebenen redaktionellen Workspaces. Sie werden in Magnolia in die jeweils
gleichnamigen Workspaces importiert. Vor einem Import immer einen vollständigen
operativen Backup-Snapshot erstellen.

## 3. Vollständige Laufzeit wiederherstellen

Für eine identische Wiederherstellung einschließlich Benutzerkonten,
Publikationsstatus, Beta-Konten und Systemkonfiguration werden die verschlüsselten
bzw. zugriffsgeschützten Betriebsbackups benötigt. Sie werden mit
`deploy/backup.sh` erzeugt und liegen nicht in GitHub.

Vor dem Entschlüsseln zuerst `COMPLETE` und `sha256sum -c SHA256SUMS` prüfen.
Archive nur auf dem berechtigten Wiederherstellungsgerät entschlüsseln und in
isolierte, frische Zielverzeichnisse oder Volumes extrahieren. Niemals ein Archiv
direkt über laufende Produktionsvolumes schreiben.

Das neueste separate Löschjournal darf durch ein älteres Journal aus dem Backup
nicht ersetzt werden. Nach dem Einspielen und vor jeder Wiederfreigabe ausführen:

```bash
docker compose -f /opt/cleonhardt/compose.production.yaml run --rm --no-deps \
  beta-portal node privacy-admin.mjs reconcile
```

Danach Anmeldung, serverseitige Autorisierung zwischen zwei getrennten Konten,
Entschlüsselung bestehender Inhalte, Retention und Kontolöschung prüfen. Ohne den
passenden Workspace-Keyring bleiben Sparring-Inhalte absichtlich unlesbar.

Die SMTP-Datei wird separat wiederhergestellt. Vor Aktivierung des Sparring-Flags
TLS und Anmeldung prüfen und nur die konstante, inhaltsfreie Testmail senden:

```bash
docker compose -f /opt/cleonhardt/compose.production.yaml run --rm --no-deps \
  beta-portal node smtp-check.mjs --send-test
```

## 4. ChOS-Reader

Nach dem Start des Beta-Portals den Read-only Deploy Key für
`ChristianLeonhardtGIT/ChOS` wiederherstellen und
`cleonhardt-chos-sync.service` einmal ausführen. Der letzte fehlerfrei validierte
`main`-Stand wird anschließend atomar aktiviert.
