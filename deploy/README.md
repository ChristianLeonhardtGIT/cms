# Produktionsbetrieb

Die Produktionsumgebung trennt Magnolia Author und Magnolia Public in zwei
Container. Nur die Public-Instanz ist über Caddy erreichbar. Die
Author-Instanz bindet ausschließlich an `127.0.0.1` und wird für die
Administration über einen SSH-Tunnel geöffnet.

## Adressen

- Website: `https://cleonhardt.de`
- Temporärer Test: `http://191.218.164.219`
- Author über SSH-Tunnel: `http://127.0.0.1:9080/magnoliaAuthor`

Der Author veröffentlicht innerhalb des Docker-Netzwerks an
`http://magnolia-public:8080`. Die Produktionsadresse wird über
`docker/microprofile-config.production.properties` eingebunden.

## Persistente Daten

- `cleonhardt_magnolia-author-repositories`
- `cleonhardt_magnolia-author-activation-key`
- `cleonhardt_magnolia-public-repositories`
- `cleonhardt_caddy-data`
- `cleonhardt_caddy-config`
- `cleonhardt_beta-portal-data` (Beta-Konten und Zugriffszeiträume)
- `/opt/cleonhardt/secrets/magnolia-password-manager-keypair.properties`
  (Magnolia Password Manager, Dateirechte `600`, in Author und Public nur lesbar
  eingebunden)

## Backups

`deploy/backup.sh` archiviert die sechs persistenten Docker-Volumes sowie die
Produktionskonfiguration nach `/var/backups/cleonhardt`. Während der Sicherung
werden Author, Public und Beta-Portal für wenige Sekunden pausiert, damit die
Magnolia-Repositories und der Beta-Kontenbestand konsistent bleiben.

Vor jeder Änderung an Produktion wird `cleonhardt-backup.service` manuell
gestartet. Erst wenn `latest/COMPLETE` existiert und die Prüfsummen erfolgreich
validiert wurden, darf die Änderung beginnen. Bei jedem Vorab-Sicherungslauf
werden lokale verschlüsselte Sicherungen, die älter als 30 Tage sind, entfernt.
Ohne weitere Produktionsänderung findet kein zusätzlicher lokaler Löschlauf
statt. Das von Christian bestätigte wöchentliche VPS-Backup bei Hostinger bildet
die zusätzliche anbieterbetriebene Sicherungsebene.

```bash
systemctl start cleonhardt-backup.service
test -f /var/backups/cleonhardt/latest/COMPLETE
cd /var/backups/cleonhardt/latest
sha256sum -c SHA256SUMS
```

Es gibt bewusst keinen täglichen lokalen Backup-Timer mehr. Bei der Umstellung
eines bestehenden Servers den alten Timer dauerhaft abschalten; vorhandene
Archive werden dabei nicht gelöscht:

```bash
systemctl disable --now cleonhardt-backup.timer
rm -f /etc/systemd/system/cleonhardt-backup.timer
systemctl daemon-reload
```

## Automatischer ChOS-Lesestand

Der geschützte Owner-Bereich liest die statische ChOS-Browser-Version aus
`/opt/cleonhardt/runtime/chos/current`. Der Systemd-Timer
`cleonhardt-chos-sync.timer` prüft alle fünf Minuten den Branch `main` des
privaten Repositories `ChristianLeonhardtGIT/ChOS`.

Die Veröffentlichung erfolgt nur, wenn die benötigten Reader-Dateien vorhanden
sind und `reports/validation-report.json` eine bestandene strikte System
Validation ohne Fehler oder Warnungen ausweist. Jeder Commit wird als eigene
Release-Kopie abgelegt; ein symbolischer Link wird erst nach erfolgreicher
Prüfung atomar umgeschaltet. Ein unvollständiger oder fehlerhafter Commit lässt
die zuletzt funktionierende Version unverändert online.

Der Server verwendet dafür ausschließlich den separaten Read-only Deploy Key
`/etc/cleonhardt/chos-github-readonly`. Der private Repository-Checkout liegt
außerhalb des Webroots und wird nicht vom Portal ausgeliefert.

Manuelle Prüfung:

```bash
systemctl start cleonhardt-chos-sync.service
journalctl -u cleonhardt-chos-sync.service -n 30 --no-pager
cat /opt/cleonhardt/runtime/chos/current/.source-commit
```
