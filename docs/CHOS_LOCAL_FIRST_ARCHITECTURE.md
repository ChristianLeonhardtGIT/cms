# ChOS Workspace: Local-first- und AI-Hybrid-Grundbau

Status: implementierter MVP-Grundbau, 24. August 2026

## Entscheidung

Das bestehende Magnolia-Setup und `cleonhardt.de` bleiben erhalten. Der neue
ChOS Workspace wird im bereits getrennten, geschützten Node-Beta-Portal
aufgebaut. Seine Arbeitsdaten werden local-first behandelt: Eine Änderung wird
zuerst im Browser gespeichert und erst danach mit dem Server abgeglichen.

Die AI-Integration besteht zunächst nur aus einem providerneutralen Vertrag und
einer Datenschutzregel. Es ist weder ein lokales Modell noch ein Cloud-Modell
aktiviert. WebMCP, IWA und WASM sind ausdrücklich nicht Teil dieses Schritts.

## Prüfung des bestehenden Setups

Der vorhandene Aufbau besitzt bereits drei sinnvolle Grenzen:

1. Magnolia Author/Public verwaltet und rendert die öffentliche Website.
2. Das Node-Beta-Portal übernimmt Konten, Einladungen und geschützte Bereiche.
3. Das private ChOS-Repository liefert den geschützten ChOS-Reader als
   freigegebenen, schreibgeschützten Laufzeitstand.

Der Workspace erweitert deshalb das Beta-Portal und verändert Magnolia nicht.
Es werden keine Nutzerantworten, Diagnosen oder AI-Eingaben in JCR/Magnolia
geschrieben. ChOS-Fachinhalte werden weiterhin nicht in diesem CMS-Repository
bearbeitet.

## Verantwortungsgrenzen

| Bereich | Verantwortung | Persistenz |
|---|---|---|
| Magnolia / ChOS-Reader | Wissen, öffentliche Inhalte, redaktionelle Texte | Magnolia JCR bzw. freigegebener ChOS-Lesestand |
| ChOS Domain | Arbeitsfälle, Beobachtung, Annahme, offene Frage, Intervention, Review | reine, frameworkfreie Operationen und Reducer |
| Browser Runtime | primäre Arbeitskopie, Outbox, letzter Sync-Cursor | IndexedDB je angemeldetem Konto |
| Sync API | Authentisierung, Idempotenz, Reihenfolge, Geräteabgleich | eigener Workspace-Speicher im geschützten Beta-Volume |
| AI Runtime | Providerwahl und Datenschutzregel | keine eigene Persistenz |

Die Grenzen sind auch im Code sichtbar:

- `beta-portal/lib/chos-domain.mjs`: fachliche Operationen und Projektion
- `beta-portal/lib/local-first-workspace.mjs`: Outbox und Sync-Zustand
- `beta-portal/lib/workspace-repository.mjs`: austauschbarer Server-Speicher
- `beta-portal/lib/ai-runtime.mjs`: lokaler/Cloud-Providervertrag
- `beta-portal/public/workspace/`: Browser-Oberfläche und IndexedDB-Adapter

## Local-first-Ablauf

```text
Eingabe
  -> Domain-Operation erzeugen
  -> sofort in IndexedDB-Outbox speichern
  -> lokale Projektion aktualisieren
  -> Oberfläche zeigt die Änderung
  -> bei Verbindung POST /beta/api/workspace/sync
  -> Server dedupliziert die Operations-ID
  -> Server vergibt einen monotonen Cursor
  -> Client übernimmt neue Server-Operationen
  -> bestätigte Einträge verlassen atomar die Outbox
```

Der MVP verwendet ein kleines, operationsbasiertes Sync-Protokoll in Version 1.
Jede Operation besitzt eine global eindeutige ID und wird vom Server höchstens
einmal aufgenommen. Wiederholte Requests nach einem Verbindungsabbruch sind
dadurch sicher. Änderungen unterschiedlicher Geräte werden in der Reihenfolge
ihres Servereingangs angewendet. Löschungen sind Tombstones und verhindern,
dass verspätete Diagnoseelemente einen gelöschten Arbeitsfall wiederbeleben.

Ein Browser speichert einen bestätigten Snapshot, den Sync-Cursor und noch
nicht bestätigte Operationen gemeinsam in einem IndexedDB-Datensatz. Dadurch
kann kein Zwischenzustand entstehen, in dem der Cursor bereits fortgeschritten,
die Outbox aber noch nicht bereinigt ist.

### Warum noch kein CRDT

Der aktuelle Anwendungsfall ist ein persönlicher Workspace mit wenigen
Geräten, keine gleichzeitige Mehrpersonenbearbeitung. Ein CRDT würde jetzt
zusätzliche Bibliotheken, Metadaten und Konfliktlogik einführen, ohne den MVP
messbar zu verbessern. Der Operationenvertrag lässt später einen CRDT- oder
feldweisen Merge-Adapter zu, falls Team-Kollaboration tatsächlich benötigt
wird.

### Server-Speicher im MVP

Der Server legt pro Konto ein atomar geschriebenes JSON-Operationsjournal im
bereits gesicherten `beta-portal-data`-Volume ab. Der Dateiname ist aus der
Konto-ID gehasht; Dateien und Verzeichnis werden mit restriktiven Rechten
angelegt. Das ist für die bestehende Beta mit maximal fünf Teilnehmenden die
kleinste belastbare Lösung.

PostgreSQL ist deshalb keine Voraussetzung für den Start. Bei wachsendem
Volumen wird ausschließlich `FileWorkspaceRepository` gegen einen
PostgreSQL-Adapter ausgetauscht. Browser, Domain-Logik und Sync-Vertrag bleiben
gleich.

Local-first spart vor allem synchrone Serverzugriffe und verbessert Latenz und
Ausfallsicherheit. Es ersetzt keine serverseitige Sicherung und ist kein
Versprechen, dass Rechenkosten auf null fallen.

## Lokale und Cloud-AI

`ai-runtime.mjs` definiert zwei gleichartige Provider:

- `local`: später beispielsweise ein vom Browser oder eine lokale Runtime
  bereitgestelltes Modell;
- `cloud`: später ein eigener Backend-Endpunkt, der Anbieterzugänge und
  Schlüssel ausschließlich serverseitig hält.

Die Routingregel ist absichtlich restriktiv:

1. Ein verfügbarer lokaler Provider wird immer bevorzugt.
2. Datenklasse `private` darf niemals automatisch in die Cloud ausweichen.
3. Datenklasse `shareable` darf nur mit `allowCloud: true` und verfügbarem
   Cloud-Provider verarbeitet werden.
4. Ein lokaler Fehler löst keinen stillen Cloud-Fallback aus.

Aktuell sind beide Provider deaktiviert. Die Oberfläche zeigt nur an, dass die
Schnittstelle vorbereitet ist. Es findet keine AI-Verarbeitung und keine
Datenübertragung statt.

## Sicherheit und Datenlebenszyklus

- Die Sync-API verwendet ausschließlich die vorhandene HttpOnly-Session und
  Same-Origin-Anfragen mit einer expliziten Workspace-Clientkennung.
- Request-Größe, Anzahl der Operationen, erlaubte Typen und Textlängen sind
  begrenzt.
- Der Caddy- und Anwendungs-CSP erlaubt Netzwerkzugriffe nur zur eigenen Origin.
- Read-only- und noch nicht gestartete Konten dürfen Serverdaten abrufen, aber
  keine neuen Operationen schreiben.
- Beim bestehenden automatischen Löschen eines abgelaufenen Beta-Kontos wird
  nun auch sein serverseitiger Workspace gelöscht.
- Lokale IndexedDB-Daten bleiben auf dem Endgerät, bis die Person sie über
  „Lokale Kopie löschen“ entfernt oder die Browserdaten löscht. Diese Grenze
  muss im späteren Datenschutz-/Offboarding-Text ausdrücklich genannt werden.

Die lokale Kopie ist in diesem MVP nicht zusätzlich anwendungsseitig
verschlüsselt. Der Workspace gehört deshalb auf persönliche, gesicherte Geräte;
besonders sensible Klarnamen oder Geheimnisse sollten nicht erfasst werden. Vor
einer breiteren Einführung ist gerätegebundene Verschlüsselung gesondert zu
entscheiden.

## Bewusst nicht umgesetzt

- keine Änderung an Magnolia-Templates oder bestehenden `cleonhardt.de`-Seiten
- keine vollständige Headless-Magnolia-Delivery-API für den Workspace
- kein Service-Worker/App-Shell-Offline-Start; eine bereits geladene Oberfläche
  arbeitet offline weiter
- keine Hintergrund-Synchronisation bei geschlossenem Browser
- keine Team-Echtzeitkollaboration und kein CRDT
- kein aktiver lokaler oder Cloud-AI-Anbieter
- kein WebMCP, IWA oder WASM

## Nächste sinnvolle Validierung

Mit ein bis fünf Beta-Konten werden drei Dinge gemessen: Nutzung auf mehreren
Geräten, Häufigkeit echter Offline-Situationen und Art der gespeicherten
Diagnoseelemente. Erst danach sollte entschieden werden, ob App-Shell-Caching,
PostgreSQL, feldweiser Konfliktmerge oder ein lokaler AI-Provider den nächsten
Nutzen bringt.
