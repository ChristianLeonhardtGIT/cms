# Magnolia CMS – eigene Website

Dieses Projekt enthält ein vollständiges lokales Magnolia-CMS-Setup auf Basis der **Magnolia Community Edition 6.4.8**. Zusätzlich ist das Light Module `meine-website` enthalten. Damit kannst du im Magnolia-Seiteneditor ohne eigene Java-Entwicklung Seiten aus fertigen Bausteinen zusammenstellen.

## Am einfachsten: mit Docker starten

Voraussetzung: Docker Desktop beziehungsweise Docker Engine mit Compose.

```bash
docker compose up --build
```

Beim ersten Start richtet Magnolia seine Datenbank und Module ein. Das kann einige Minuten dauern. Öffne danach:

- Administration: <http://localhost:8081/magnoliaAuthor>
- Statusseite: <http://localhost:8081>
- Veröffentlichte Website: <http://localhost:8081/magnoliaPublic>

Beim ersten Aufruf legst du das Passwort für den Benutzer `superuser` selbst fest.

Beenden:

```bash
docker compose down
```

Die CMS-Inhalte, die veröffentlichten Inhalte und der sichere Publishing-Schlüssel bleiben in Docker-Volumes erhalten.

Falls du einen anderen Port verwenden möchtest, starte zum Beispiel mit `MAGNOLIA_PORT=9090 docker compose up --build`.

## Alternativ: direkt auf dem Rechner starten

Voraussetzungen:

- Java 17 oder neuer
- Node.js LTS

Abhängigkeiten installieren und Magnolia starten:

```bash
npm install
npm start
```

## Deine erste Seite

1. Öffne Magnolia unter <http://localhost:8081/magnoliaAuthor> (Docker) beziehungsweise <http://localhost:8080/magnoliaAuthor> (direkter Start) und melde dich als `superuser` an.
2. Öffne die App **Pages**.
3. Wähle **Add page** und als Vorlage **Standardseite**.
4. Trage Seitentitel, Website-Name und optional die Beschreibung für Suchmaschinen ein.
5. Füge im Bereich **Seiteninhalt** über die grüne Leiste Bausteine hinzu.
6. Nutze **Preview page**, um die fertige Seite anzusehen.
7. Mit **Publish** überträgst du die Seite auf die Public-Instanz. Sie ist anschließend unter `http://localhost:8081/magnoliaPublic/<seitenname>` erreichbar.

Enthaltene Bausteine:

- **Hero / Seitenaufmacher** – große Einstiegsfläche mit optionalem Bild und Link
- **Textabschnitt** – formatierbarer Inhalt mit optionalem Hintergrund
- **Karte / Teaser** – Bild, Text und optionale Verlinkung
- **Handlungsaufforderung** – auffälliger Abschlussbereich mit Schaltfläche

Unterseiten der obersten Seite erscheinen automatisch in der Navigation.

## Wo du das Design änderst

- Farben, Abstände und responsive Gestaltung: `light-modules/meine-website/webresources/css/site.css`
- HTML-Struktur der Seite: `light-modules/meine-website/templates/pages/home.ftl`
- Bausteine: `light-modules/meine-website/templates/components/`
- Eingabemasken im CMS: `light-modules/meine-website/dialogs/`

Im Entwicklungsmodus erkennt Magnolia Änderungen an diesen Dateien automatisch. Bei Docker ist der Light-Modules-Ordner dafür in den Container eingebunden.

## Projekt prüfen

```bash
npm test
npm run test:content-model
npm run test:problem-pages
```

`test:problem-pages` prüft die Problemübersicht und alle vier wichtigen
Detailseiten in einem echten Browser auf Desktop und Mobile. Abgedeckt sind
unter anderem Breadcrumbs, Überschriften, Sitemap-Einträge, Diagnosekarten,
einmalige Selbstchecks, interne Beziehungen, vollflächige Kartenlinks,
Hover-/Fokuszustände und horizontaler Überlauf. Gegen eine andere Umgebung kann
der Test mit `E2E_BASE_URL=https://example.test npm run test:problem-pages`
ausgeführt werden.

## Strukturierte ChOS-Inhalte

Fachinhalte werden in eigenen Magnolia-Apps gepflegt: **Problemsituationen**,
**Insights**, **Tools und Checks**, **Cases** und **Themen**. Rechtlich relevante
Texte werden zentral in der App **Rechtstexte** gepflegt. Leistungen bleiben
zentral im bestehenden **Angebotskatalog** und werden aus den anderen Typen nur
referenziert.

Die öffentlichen Seiten `/impressum` und `/datenschutz` bleiben eigenständige
Website-Seiten. Ihr sichtbarer Inhalt stammt jedoch jeweils aus genau einem
veröffentlichten `legalDocument`. Dadurch werden Rechtstexte nicht als Kopien in
Seitenkomponenten gepflegt. Die Migration des aktuellen Bestands erfolgt mit
`scripts/migrate-legal-documents.groovy` und übernimmt den vorhandenen Inhalt
unverändert aus den bisherigen Textkomponenten.

Das Skript `scripts/migrate-existing-content-model.groovy` übernimmt den bereits
vorhandenen Bestand in dieses Modell: alle Artikel unter `/insights`, den ChOS-
Selbstcheck und die drei anonymisierten Fallmuster. Die öffentlichen Website-
Seiten bleiben unverändert und sind über das Feld `sourcePage` zurückverknüpft.
Das Skript ist wiederholbar und entfernt keine fremden Datensätze.

## Hinweis für den produktiven Betrieb

Dieses Setup ist für lokale Entwicklung und erste eigene Websites gedacht. Vor einem öffentlichen Produktivbetrieb solltest du insbesondere HTTPS, externe Datenbank, Backups, Rechte/Rollen, getrennte Author-/Public-Instanzen, Monitoring und Updates einplanen.
