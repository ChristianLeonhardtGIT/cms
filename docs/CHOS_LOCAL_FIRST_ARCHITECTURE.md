# ChOS Workspace: Local-first- und AI-Hybrid-Architektur

Status: produktiver Grundstand, 24. August 2026

## Entscheidung

Die bestehende öffentliche Website bleibt erhalten. Der ChOS Workspace wird im
bereits getrennten, geschützten Anwendungsbereich aufgebaut. Seine Arbeitsdaten
werden local-first behandelt: Eine Änderung wird zuerst im Browser gespeichert
und erst danach mit dem Server abgeglichen.

Die AI-Integration besteht zunächst nur aus einem providerneutralen Vertrag und
einer Datenschutzregel. Es ist weder ein lokales Modell noch ein Cloud-Modell
aktiviert. WebMCP, IWA und WASM sind ausdrücklich nicht Teil dieses Schritts.

## Prüfung des bestehenden Setups

Der vorhandene Aufbau besitzt bereits drei sinnvolle Grenzen:

1. Das Content-System verwaltet und rendert die öffentliche Website.
2. Das Node-Beta-Portal übernimmt Konten, Einladungen und geschützte Bereiche.
3. Das private ChOS-Repository liefert den geschützten ChOS-Reader als
   freigegebenen, schreibgeschützten Laufzeitstand.

Der Workspace erweitert deshalb den geschützten Anwendungsbereich und verändert
die öffentliche Website nicht. Nutzerantworten, Diagnosen und AI-Eingaben werden
nicht im Content-System gespeichert. ChOS-Fachinhalte werden weiterhin nicht in
diesem Repository bearbeitet.

## Verantwortungsgrenzen

| Bereich | Verantwortung | Persistenz |
|---|---|---|
| Content-System / ChOS-Reader | Wissen, öffentliche Inhalte, redaktionelle Texte | redaktioneller Bestand bzw. freigegebener ChOS-Lesestand |
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
  -> bei Verbindung POST /workspace/api/workspace/sync
  -> Server dedupliziert die Operations-ID
  -> Server vergibt einen monotonen Cursor
  -> Client übernimmt neue Server-Operationen
  -> bestätigte Einträge verlassen atomar die Outbox
```

Der Grundstand verwendet ein kleines, operationsbasiertes Sync-Protokoll in Version 1.
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
zusätzliche Bibliotheken, Metadaten und Konfliktlogik einführen, ohne den aktuellen Stand
messbar zu verbessern. Der Operationenvertrag lässt später einen CRDT- oder
feldweisen Merge-Adapter zu, falls Team-Kollaboration tatsächlich benötigt
wird.

### Serverseitiger Speicher

Der Server legt pro Konto ein atomar geschriebenes JSON-Operationsjournal im
bereits gesicherten `beta-portal-data`-Volume ab. Der Dateiname ist aus der
Konto-ID gehasht; Dateien und Verzeichnis werden mit restriktiven Rechten
angelegt. Das ist für die bestehende Beta mit maximal fünf Teilnehmenden die
kleinste belastbare Lösung.

PostgreSQL ist deshalb keine Voraussetzung für den Start. Bei wachsendem
Volumen wird ausschließlich `FileWorkspaceRepository` gegen einen
PostgreSQL-Adapter ausgetauscht. Browser, Domain-Logik und Sync-Vertrag bleiben
gleich.

Der Produktionsadapter begrenzt ein Konto auf 5.000 Operationen und 10 MiB
Journalgröße. Ein unkontrolliertes Anwachsen des Volumes oder einzelner
Sync-Antworten wird damit verhindert; vor Erreichen der Grenze kann der Adapter
später durch PostgreSQL oder eine Snapshot-Kompaktion ersetzt werden.

Local-first spart vor allem synchrone Serverzugriffe und verbessert Latenz und
Ausfallsicherheit. Es ersetzt keine serverseitige Sicherung und ist kein
Versprechen, dass Rechenkosten auf null fallen.

## Offline verfügbare ChOS-Inhalte

Der freigegebene ChOS-Lesestand ist vom persönlichen Workspace getrennt, wird
aber ebenfalls local-first nutzbar. Ein Service Worker speichert nach einer
bewussten Nutzeraktion eine vollständige, versionierte und schreibgeschützte
Kopie in der Cache Storage des Browsers.

- Die Offline-Kopie umfasst die Startseite, alle Dokumente, Suche, Gestaltung
  und die kleine Offline-Bedienoberfläche.
- Eine serverseitig erzeugte Dateiliste bildet immer den atomar freigegebenen
  ChOS-Laufzeitstand ab; Inhaltsupdates erfordern keinen Portal-Neubau.
- Ein neuer Stand wird zunächst vollständig in einen separaten Cache geladen.
  Erst nach erfolgreichem Abschluss ersetzt er die vorherige Kopie.
- Bei bestehender Verbindung gilt weiterhin die serverseitige Anmeldung. Eine
  Weiterleitung zur Anmeldung oder ein 401/403 darf nie durch Cache-Inhalte
  ersetzt werden. Nur ein Netzfehler oder ein Serverausfall nutzt die lokale
  Kopie.
- Der Lesestand kann in der ChOS-Oberfläche sichtbar aktualisiert und vom Gerät
  gelöscht werden. Bei der Kontolöschung werden Workspace-Kopie und ChOS-Cache
  gemeinsam entfernt.

Offline-Lesen vertraut dem verwendeten Endgerät. Auf gemeinsam genutzten Geräten
muss die lokale Kopie nach der Nutzung gelöscht werden. Cache Storage ist keine
zusätzliche Verschlüsselung und ersetzt weder Geräteschutz noch Browserprofil-
Trennung.

## Kontextbezogene Wissensempfehlungen

Der Workspace verbindet einen persönlichen Arbeitsfall mit passenden Inhalten
aus dem jeweils atomar freigegebenen ChOS-Lesestand, ohne die beiden Datenwelten
zu vermischen:

1. Der Server erzeugt aus dem veröffentlichten Reader einen versionierten Index
   mit Pfad, Titel, Bereich und freigegebenem Kurztext.
2. Nur dieser öffentliche Wissensindex wird an den Browser übertragen und dort
   lokal gespeichert. Er ist außerdem Teil des bewusst gespeicherten
   Offline-Lesestands.
3. Titel, Kontext und Diagnoseelemente des Arbeitsfalls werden ausschließlich im
   Browser gegen kuratierte ChOS-Themensignale ausgewertet. Private Arbeitsdaten
   werden für die Zuordnung weder an den Server noch an einen AI-Provider
   übertragen.
4. Die Oberfläche zeigt höchstens drei Empfehlungen. Eine Kurzvorschau bleibt im
   Workspace; das vollständige Dokument öffnet in einem neuen Tab, damit der
   Arbeitskontext erhalten bleibt.

Die regelbasierte Zuordnung ist absichtlich nachvollziehbar und bildet später
die sichere Basis für ein optionales lokales semantisches Re-Ranking. Ein
Cloud-Fallback ist für private Arbeitskontexte nicht vorgesehen.

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
- Der bestätigungspflichtige Admin-Befehl `remove` löscht temporäre oder manuell
  beendete Konten zusammen mit ihrem serverseitigen Workspace.
- Lokale IndexedDB-Daten bleiben auf dem Endgerät, bis die Person sie über
  „Lokale Kopie löschen“ entfernt oder die Browserdaten löscht. Diese Grenze
  muss im späteren Datenschutz-/Offboarding-Text ausdrücklich genannt werden.

Die lokale Kopie ist in diesem Grundstand nicht zusätzlich anwendungsseitig
verschlüsselt. Der Workspace gehört deshalb auf persönliche, gesicherte Geräte;
besonders sensible Klarnamen oder Geheimnisse sollten nicht erfasst werden. Vor
einer breiteren Einführung ist gerätegebundene Verschlüsselung gesondert zu
entscheiden.

## Bewusst nicht umgesetzt

- keine Änderung an Templates oder bestehenden `cleonhardt.de`-Seiten
- keine vollständige Headless-Delivery-API für den Workspace
- noch kein App-Shell-Offline-Start des Workspace; der Service Worker deckt in
  diesem Schritt ausschließlich den ChOS-Lesestand und seine Reader-Assets ab
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
