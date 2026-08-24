# ChOS Beta Portal

Geschützter MVP-Arbeitsbereich für maximal fünf persönlich freigeschaltete B2B-Beta-Teilnehmende.

## Sicherheits- und Zugriffsmodell

- keine öffentliche Registrierung
- persönliche Einladungslinks, einmalig und 72 Stunden gültig
- Passwörter mit `scrypt` gehasht; Klartextpasswörter werden nicht gespeichert
- sichere, `HttpOnly`- und `SameSite=Strict`-Cookies
- 60 Tage aktive Arbeitsphase, anschließend 30 Tage Lesezugriff
- automatische Zugriffssperre nach Ende der Lesephase
- automatische Löschung des Portal-Kontos nach Ende der 90-Tage-Gesamtlaufzeit
- maximal fünf Konten und acht Stunden Begleitung pro Konto
- getrennte Datenspeicherung außerhalb der Magnolia-Inhalte
- Local-first ChOS Workspace mit lokaler IndexedDB-Arbeitskopie
- idempotenter Geräteabgleich über die geschützte Sync-API
- dauerhaftes, separates Owner-Konto für den persönlichen ChOS-Lesebereich

## Lokal starten

```bash
docker compose up -d --build beta-portal
```

Der lokale Direktzugriff ist standardmäßig `http://127.0.0.1:3090/beta/`.

## Konto einladen

```bash
docker compose exec beta-portal node admin.mjs invite \
  --email=name@firma.de \
  --name="Vorname Nachname" \
  --start=2026-09-01
```

Der ausgegebene Link wird persönlich übermittelt. Erst nach vereinbartem NDA, Vertrag und Zahlung einladen.

## Persönlichen ChOS-Zugang einrichten

```bash
docker compose exec beta-portal node admin.mjs owner \
  --email=christian@beispiel.de \
  --name="Christian Leonhardt"
```

Der einmalige Link führt zur eigenen Passwortvergabe. Das Owner-Konto zählt
nicht zum Limit der fünf Beta-Teilnehmenden und besitzt als einziges Konto
Zugriff auf `/beta/chos/`. Der eingebundene Lesestand wird aus dem privaten
ChOS-Repository übernommen:

```bash
scripts/sync-chos-reader.sh /pfad/zum/ChOS-Repository
```

In Produktion wird die freigegebene Browser-Version automatisch aus dem
Standardbranch `main` des privaten ChOS-Repositories übernommen. Der Server
prüft alle fünf Minuten auf einen neuen Commit. Nur ein Stand mit vollständigen
Reader-Dateien und bestandener strikter System Validation wird atomar
freigeschaltet. Bei einem Fehler bleibt die zuletzt funktionierende Version
online. Das Portal-Image muss für reine ChOS-Inhaltsänderungen nicht neu gebaut
werden.

Der GitHub-Zugriff verwendet einen separaten Read-only Deploy Key. Quellcode,
Git-Metadaten und Zugangsdaten werden nicht über den Webserver ausgeliefert.

## Verwaltung

```bash
docker compose exec beta-portal node admin.mjs list
docker compose exec beta-portal node admin.mjs reinvite --email=name@firma.de
docker compose exec beta-portal node admin.mjs disable --email=name@firma.de
docker compose exec beta-portal node admin.mjs remove --email=name@firma.de --confirm=name@firma.de
```

`remove` ist für bestätigte Löschungen und temporäre E2E-Konten vorgesehen. Der
Befehl deaktiviert das Konto zuerst und löscht anschließend Konto und
serverseitigen Workspace. Die Bestätigung muss exakt der normalisierten
E-Mail-Adresse entsprechen.

## ChOS Workspace

Angemeldete Teilnehmende und das Owner-Konto erreichen die neue Arbeitsfläche
unter `/beta/workspace/`. Neue Arbeitsfälle und Diagnoseelemente werden zuerst
lokal im Browser gespeichert. Bei bestehender Verbindung gleicht die
Arbeitsfläche ihre Operationen mit dem persönlichen Serverbestand ab.

Magnolia- und ChOS-Fachinhalte bleiben davon getrennt. Die vorbereitete
AI-Schnittstelle hat noch keinen aktiven lokalen oder Cloud-Provider. Details,
Grenzen und die spätere PostgreSQL-Migration stehen in
`../docs/CHOS_LOCAL_FIRST_ARCHITECTURE.md`.

## MVP-Grenze

Dieser Stand liefert Anmeldung, Zugriffsphasen, Dashboard, Onboarding, den
persönlichen ChOS-Lesebereich sowie einen schlanken Local-first Workspace für
Arbeitsfälle und Diagnoseelemente. Uploads, Exporte, Team-Kollaboration und
aktive AI-Verarbeitung bleiben spätere Ausbauschritte. Serverseitige
Workspace-Daten sind an denselben automatischen Löschlauf wie das Konto
angebunden.
