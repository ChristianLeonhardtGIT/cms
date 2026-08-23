# Source of Truth und Update-Prozess

## Verbindliche Quelle

Seit dem Snapshot vom 23. August 2026 ist
`ChristianLeonhardtGIT/cms`, Branch `main`, die verbindliche Quelle für
CMS-Code, Konfiguration und versionierte redaktionelle Magnolia-Exporte.

Der laufende Produktionsstand liegt unter `/opt/cleonhardt`. Änderungen direkt
auf dem Server gelten nur als Notfallmaßnahme und müssen anschließend sofort in
dieses Repository zurückgeführt werden.

## Arbeitsablauf

1. `main` von GitHub abrufen und einen Arbeitsbranch erstellen.
2. Änderung lokal umsetzen und die passenden Tests ausführen.
3. Geheimnisprüfung und vollständigen Diff prüfen.
4. Änderung nach `main` übernehmen.
5. Nur die betroffenen Dateien nach `/opt/cleonhardt` ausrollen.
6. Produktionszustand und öffentliche Routen verifizieren.
7. Bei redaktionellen Änderungen neue JCR-Exporte erzeugen und versionieren.

## Getrennte Quellen

Das eigenständige ChOS-Fachrepository bleibt
`ChristianLeonhardtGIT/ChOS`. Der CMS-Ownerbereich synchronisiert dessen
freigegebenen Branch `main` automatisch und stellt ihn geschützt unter
`/beta/chos/` dar. ChOS-Fachinhalte werden nicht im CMS-Repository bearbeitet.

## Nicht versionierte Daten

Folgende Daten werden über die betrieblichen Backups gesichert und nicht in Git
gespeichert:

- `cleonhardt_magnolia-author-repositories`,
- `cleonhardt_magnolia-public-repositories`,
- `cleonhardt_magnolia-author-activation-key`,
- `cleonhardt_beta-portal-data`,
- Caddy-Zertifikats- und Laufzeitvolumes,
- Dateien unter `/opt/cleonhardt/secrets`.

Der Grund ist nicht fehlende Relevanz, sondern Geheimnis- und Datenschutzschutz:
Diese Daten können Zugangsdaten, Passwort-Hashes, Konten oder personenbezogene
Informationen enthalten und gehören nicht in eine dauerhafte Git-Historie.

