# Portfolio-Rollout 2026-09-22

## Ziel

Der öffentliche Auftritt von `cleonhardt.de` wird als persönliches Portfolio
ausgeliefert. Die bestehende Magnolia-Consulting-Seite, ihre Inhalte und die
redaktionellen Workspaces bleiben unverändert erhalten.

Der geschützte ChOS-Bereich bleibt weiterhin unter `/workspace` und `/beta`
aktiv. Auch `cms.cleonhardt.de` bleibt unverändert die geschützte
Magnolia-Author-Instanz.

## Technische Trennung

- Portfolio: `deploy/static/portfolio/`
- Öffentliche Auslieferung: `deploy/Caddyfile`
- Bisherige Consulting-Inhalte: unverändert in Magnolia und den bestehenden
  Content-Snapshots
- ChOS-Fachsystem: weiterhin separates Repository
- ChOS Workspace und Reader: weiterhin Beta-Portal und ChOS-Sync

Alte Consulting-URLs werden vorübergehend auf `/projekte` umgeleitet. So
bleiben die früheren Inhalte erhalten, ohne öffentlich als Angebotsseite zu
erscheinen.

## Rückkehr zur Magnolia-Seite

Für eine vollständige Rückkehr wird ausschließlich die Portfolio-Routing-
Änderung in `deploy/Caddyfile` zurückgenommen. Die Magnolia-Inhalte müssen
nicht erneut importiert werden, solange die produktiven Volumes erhalten sind.

Vor jeder Produktionsänderung gilt weiterhin der dokumentierte Backup-Prozess
aus `deploy/README.md` und `docs/RESTORE.md`.

## Prüfung

`npm run test:portfolio` prüft die Portfolio-Dateien, Metadaten, Sitemap und
die unveränderten Workspace-Routen.
