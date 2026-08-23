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
```

## MVP-Grenze

Dieser Stand liefert Anmeldung, Zugriffsphasen, Dashboard, Onboarding und den
persönlichen, durchsuchbaren ChOS-Lesebereich. Fallnotizen, Uploads,
Diagnoseflächen und Exporte werden bewusst erst im nächsten Ausbauschritt
ergänzt. Sobald fachliche Arbeitsdaten ergänzt werden, müssen sie an denselben
automatischen Löschlauf angebunden werden.
