# Workspace und Async Clarity Sparring

Bestandsaufnahme: 6. September 2026, Basis `origin/main` = `cf937f1` (GitHub abgerufen).

## Vorhandene Architektur

Magnolia Author/Public und Caddy laufen laut versionierter Compose-Konfiguration
auf dem bestehenden VPS unter `/opt/cleonhardt`. Node 24 übernimmt das Portal,
mit scrypt-Passwörtern, persönlichen Einladungen, In-Memory-Sessions (12 Stunden),
HttpOnly/Secure/SameSite-Strict-Cookies mit Path `/`, Formular-Nonces und Loginlimits.
Neustarts beenden bereits heute Sessions. Es gibt keine PostgreSQL-Datenbank,
kein ORM und keine SQL-Migrationen. Konten: atomare JSON-Datei mit Dateisperre;
ChOS: begrenztes JSON-Operationsjournal je Konto im bestehenden Volume.

Der ChOS-Workspace verwendet IndexedDB und einen lokalen Wissensindex. Nur der
bewusst gespeicherte Reader ist im Service-Worker-Cache. AI-Provider sind
vorbereitet, aber deaktiviert. Kein SMTP-Versand ist im Portal implementiert.
Das lokale Codex-Brain liegt außerhalb des Repositories unter
`/home/cd/Dokumente/Codex/brain`; kanonische Fachinhalte kommen aus dem separaten
ChOS-Repository. Keine Kundenkommunikation in eines dieser Wissenssysteme übernehmen.

Der direkte VPS-Abgleich am 6. September scheiterte an SSH-Authentisierung.
Aussagen zum Betrieb beruhen hier auf Git und den datierten Betriebsnotizen,
nicht auf einem neuen Live-Audit. Produktionsdaten wurden nicht gelesen.

## URL-Migration

Öffentliche und interne Links, Login, Einladung, API, Healthcheck, Reader und
Caddy verwenden `/workspace`. Die bestehende persönliche ChOS-Arbeitsfläche
bleibt als Unterfunktion unter `/workspace/workspace/` erreichbar.
GET/HEAD auf `/beta` und Unterpfade erhalten 308 mit Pfad und Query.
Alte Schreibaufrufe erhalten 409 statt einer stillen Wiederholung.
Einladungs-Token werden wie bisher nach dem ersten Request aus der URL entfernt;
Workspace-Access-Logging bleibt aus. Inhalte gehören niemals in Queryparameter.

Die alte Service-Worker-Skript-URL liefert ein ausführbares Stilllegungsskript,
keinen Redirect. Es entfernt nur alte Reader-Cache-Einträge und meldet sich ab.
Die neuen Portal-Seiten bereinigen ebenfalls alte Registrierungen/Reader-Einträge.
Persönliche IndexedDB-Daten bleiben erhalten. Offline-Lesestand anschließend
bewusst neu speichern. Dauerhaft offline gebliebene Geräte können erst nach
Wiederverbindung aktualisiert werden.

Die existierenden Cookie-Namen, `BETA_*`-Betriebsvariablen, Container-/Volume-Namen
und das Verzeichnis `beta-portal` bleiben als kompatible Speicher-/Betriebsnamen
bestehen. Ihre Umbenennung ist für den URL-Wechsel unnötig und würde insbesondere
bei Volumes einen Datenverlust vortäuschen. Beta bleibt Produktstatus.

## Rollout

Kleine Commits, lokale Tests, Review und Merge vor Deployment gemäß AGENTS.md.
Vor Rollout konsistente Sicherung; Portal-Image und Caddy gemeinsam aktualisieren.
Keine Volumes neu anlegen. Testen: alte Deep Links, neue Anmeldung, Einladung,
Logout, ChOS-Reader/Sync und alte installierte Reader. In-Memory-Sessions erfordern
beim Portal-Neustart erneute Anmeldung. Rollback mit vorherigem Image/Caddy;
Datenvolume unverändert lassen. Bereits permanent gecachte Redirects erfordern
bei Rollback weiterhin eine bedienbare `/workspace`-Route.
