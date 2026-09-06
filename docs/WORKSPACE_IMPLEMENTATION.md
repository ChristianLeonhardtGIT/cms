# Workspace und Async Clarity Sparring

Aktualisierung: Die Sicherheits-/Betriebsergänzungen vom 6. September stehen in [WORKSPACE_OPERATIONS.md](WORKSPACE_OPERATIONS.md); die folgende Bestandsaufnahme dokumentiert den ersten Implementierungsschritt.

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

## Implementierte Sparring-Basis

`WORKSPACE_SPARRING_ENABLED=true` schaltet nach bewusster Betriebsfreigabe die
neuen Routen ein. Standard in beiden Compose-Dateien ist false.
Owner findet den Einstieg im Reader, Kunde im Dashboard. Unter
`/workspace/sparring` ordnet der Owner einem aktiven Bestandskonto das Produkt zu.
Keine öffentliche Registrierung oder automatische Buchung. Neue Kunden werden
mit dem vorhandenen Einladungs-/Adminprozess aufgenommen; dessen Beta-Limits
bleiben für diesen Pilot bestehen. Vor Start muss der Kontozugang fünf
Arbeitstage plus 30 Tage Nachlauf abdecken.

Eine Conversation ist im kleinen Dateimodell direkt Teil eines Engagements.
Produktdaten werden bei Anlage als Snapshot gespeichert. Status:
intake → ready → active → completed; zusätzlich cancelled.
Ein Thema, 5 Kalendertage Mo–Fr, keine Feiertagsberechnung, Starttag zählt bei
Mo–Fr mit, Ende ist Berliner Mitternacht nach dem fünften Arbeitstag.
Start am Wochenende zählt ab Montag. Reaktionsfenster persönlich vereinbaren;
dieser Stand führt keine SLA- oder Terminverwaltung ein.

Servergerenderte Intake-/Coach-/Chat-Formulare verwenden die vorhandene
Gestaltung. Nachrichten aktualisieren sich bei sichtbarer Seite alle 15 Sekunden,
Lesestand wird ausdrücklich bestätigt. Inhalte sind Plain Text. Doppelte
Nachrichten werden über eine Request-ID abgefangen. Bereits abgeschlossene
oder zeitlich abgelaufene Fälle sind schreibgeschützt.

Neue Datenablage ist additiv/versioniert; vorhandene Konten/Journale werden
nicht transformiert. Kein SQL-Migrationssystem erfinden. Das bisherige
Dateisperren-Muster benötigt bei einem harten Prozessabbruch gegebenenfalls
manuelle Entfernung einer verwaisten `store.lock`, ausschließlich bei gestopptem
Portal nach Prüfung laufender Prozesse. Keine automatische unsichere Lock-Übernahme.

Benachrichtigungen sind ausschließlich als inhaltsfreie Merkposten vorbereitet.
Kein Mailversand behauptet. Fachliches Pilotangebot und vollständige kommerzielle
Freigabe bleiben getrennt. Weitere offene Punkte: [Datenschutz-/Betriebsstand](PRIVACY_WORKSPACE.md).

## Verifikation

`npm run test:beta-portal`: bestehende Auth-/Kontopflege-/ChOS-Integration,
Legacy-URLs und Worker-Skript, vollständiger Sparring-HTTP-Ablauf, CSRF und IDOR,
XSS-Rendering, no-store, Zeitumstellungen, Wiederholungen/Parallelität,
Löschung und Ausschluss von Chat aus dem Offline-Worker.
Zusätzlich Setup-, redaktionelle, GEO- und Sichtbarkeits-Quellenchecks.
Lokaler Browser: Anmeldung, Übersicht, aktiven Chat öffnen, Nachricht senden
und aktualisierte Anzeige mit synthetischen Testdaten verifiziert. Screenshot-
Erstellung im Browser schlug technisch fehl; keine vollständige visuelle oder
mobile Abnahme behauptet. Produktiver VPS-Smoke-Test steht aus.
