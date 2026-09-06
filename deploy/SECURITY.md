# Betrieb und Härtung

Stand: 30. Juli 2026

## Öffentliche Zugänge

- Website: `https://cleonhardt.de`
- Magnolia Author: `https://cms.cleonhardt.de`
- ChOS Beta: `https://cleonhardt.de/workspace/` (nur nach persönlicher Einladung)
- Öffentlich freigegebene Ports: SSH `22/tcp`, HTTP `80/tcp`, HTTPS `443/tcp` und HTTP/3 `443/udp`
- Magnolia Author ist am Host ausschließlich an `127.0.0.1:8080` gebunden und wird nur über Caddy veröffentlicht.

## CMS-Zugang

Der Author-Zugang besitzt zwei getrennte Anmeldestufen:

1. vorgeschalteter, TLS-geschützter Caddy-Zugang mit bcrypt-gehashtem Kennwort;
2. reguläre Magnolia-Anmeldung.

Caddy entfernt den äußeren `Authorization`-Header vor der Weiterleitung an Magnolia. Antworten des CMS werden nicht zwischengespeichert und erhalten restriktive Sicherheits-Header.

Für Magnolias interne Seitenvorschau darf der Author ausschließlich von derselben
CMS-Origin eingebettet werden. Caddy überschreibt dafür die von Magnolia gelieferte
`X-Frame-Options: DENY`-Antwort mit `SAMEORIGIN` und setzt zusätzlich
`Content-Security-Policy: frame-ancestors 'self'`. Einbettungen durch fremde Origins
bleiben damit blockiert.

Die Zugangsdaten liegen nicht im Projekt. Sie sind lokal in der geschützten Deployment-Ablage gespeichert:

`Backups/cleonhardt-magnolia/deployment-secrets/cms-basic-auth.txt`

## Beta-Zugang

Der Beta-Bereich ist als eigener Dienst von Magnolia getrennt. Es gibt keine
öffentliche Registrierung. Einladungen sind einmalig, laufen nach 72 Stunden ab
und werden erst nach Prüfung von Teilnahme, Vertraulichkeit und Zahlung erstellt.
Passwörter werden mit Scrypt gehasht. Sitzungen verwenden zufällige Kennungen in
`Secure`, `HttpOnly` und `SameSite=Strict` geschützten Cookies. Der aktive Zugriff
endet nach 60 Tagen, nach weiteren 30 Tagen endet auch der Lesezugriff.

Kontodaten liegen im separaten Volume `cleonhardt_beta-portal-data`. Die Anwendung
protokolliert weder Passwörter noch Einladungstoken.

## Server

- UFW: eingehend standardmäßig blockiert; nur die oben genannten Ports sind freigegeben.
- SSH: Anmeldung nur per Schlüssel; Passwortanmeldung ist deaktiviert.
- Root: ausschließlich per SSH-Schlüssel, nicht per Kennwort.
- Fail2ban: SSH-Jail mit UFW-Sperre nach wiederholten Fehlversuchen.
- Unattended Upgrades: aktiviert und laufend.

## Regelmäßige Prüfung

Im Projekt:

```sh
npm test
docker compose -f compose.production.yaml config --quiet
```

Auf dem Server:

```sh
ufw status verbose
ss -lntup
sshd -T
fail2ban-client status sshd
systemctl status unattended-upgrades
docker compose -f /opt/cleonhardt/compose.production.yaml ps
```

Von außen:

```sh
curl -I https://cms.cleonhardt.de/
curl -I https://cleonhardt.de/
```

Der erste Aufruf muss ohne Zugangsdaten `401 Unauthorized`, die öffentliche Website `200 OK` liefern.

## Sicherungen

Vor der CMS-Veröffentlichung angelegte Server-Sicherungen:

- `/opt/cleonhardt/deploy/Caddyfile.bak-before-cms-20260730`
- `/opt/cleonhardt/compose.production.yaml.bak-before-cms-20260730`

Zusätzlich sollten regelmäßige externe VPS- oder Volume-Sicherungen aktiviert und Wiederherstellungen gelegentlich getestet werden.
