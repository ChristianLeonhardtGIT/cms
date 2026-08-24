# cleonhardt.de CMS

Dieses private Repository ist die kanonische, geheimnisfreie Quelle für den
CMS-Code und die wiederherstellbaren redaktionellen Inhalte von
`cleonhardt.de`. Das Repository muss privat bleiben.

Enthalten sind insbesondere:

- Magnolia Light Module, Content Types, Apps, Dialoge und Templates,
- Produktions- und Containerkonfiguration ohne Zugangsdaten,
- Beta-Portal und geschützter ChOS-Reader,
- Local-first ChOS Workspace und geschützte Sync-Schnittstelle,
- Betriebs-, Backup- und Sync-Skripte,
- freigegebene Groovy-Migrations- und Wartungsskripte,
- das Werkzeug für versionierte, redaktionell freigegebene JCR-Exporte.

Nicht enthalten sind Passwörter, API-Tokens, private Schlüssel, Beta-Konten,
Magnolia-Benutzer, Server-Laufzeitdaten oder unverschlüsselte Repository-
Volumes. Diese Daten gehören in die gesicherten Produktions-Backups und niemals
in eine Git-Historie.

Der vollständige Laufzeitstand wird deshalb zweistufig gesichert: Der
geheimnisfreie Code- und Konfigurationsstand liegt hier; Magnolia-Repositories,
Konten, Zertifikate und Secrets liegen in den zugriffsgeschützten operativen
Backups. Für eine vollständige Wiederherstellung werden beide Ebenen benötigt.

## Quelle für spätere Updates

- Repository: `git@github.com:ChristianLeonhardtGIT/cms.git`
- Standardbranch: `main`
- Produktionspfad: `/opt/cleonhardt`
- Produktionsserver: `191.218.164.219`
- Magnolia-Projekt: `cleonhardt`

Vor jeder Änderung wird zuerst `origin/main` synchronisiert. Änderungen werden
lokal getestet, nach `main` übernommen und erst anschließend gezielt nach
`/opt/cleonhardt` ausgerollt. Details stehen unter
[`docs/SOURCE_OF_TRUTH.md`](docs/SOURCE_OF_TRUTH.md) und
[`docs/RESTORE.md`](docs/RESTORE.md).

## Sicherheitsregel

Dateien unter `secrets/`, persistente Docker-Volumes sowie private Schlüssel
werden nie eingecheckt. Die Compose-Dateien referenzieren diese Werte nur über
Dateipfade oder Umgebungsvariablen.
