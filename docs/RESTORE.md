# Wiederherstellung

## 1. Projektcode wiederherstellen

Das private Repository nach `/opt/cleonhardt` klonen und die nicht versionierten
Dateien aus dem sicheren Backup ergänzen:

- `secrets/hubspot.env`,
- Magnolia Password-Manager-Keypair,
- weitere produktive Schlüssel oder Umgebungsdateien.
- `CMS_BASIC_AUTH_HASH` als einfach quotierte Umgebungsvariable, damit die
  Dollarzeichen des Caddy-kompatiblen bcrypt-Hashes erhalten bleiben.

Anschließend die Compose-Konfiguration prüfen und die Images neu bauen.

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

## 4. ChOS-Reader

Nach dem Start des Beta-Portals den Read-only Deploy Key für
`ChristianLeonhardtGIT/ChOS` wiederherstellen und
`cleonhardt-chos-sync.service` einmal ausführen. Der letzte fehlerfrei validierte
`main`-Stand wird anschließend atomar aktiviert.
