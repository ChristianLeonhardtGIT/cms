# ChOS Beta – Login- und Zugriffsmodell v0.1

Stand: 23. August 2026

## Ziel

Der Login-Bereich schafft einen geschützten Einstieg für maximal fünf B2B-Beta-Teilnehmende. Er bildet zunächst nur Identität, Freischaltung und zeitlich begrenzten Zugriff ab. Die fachlichen ChOS-Arbeitsflächen folgen darauf aufbauend.

Zusätzlich existiert ein getrenntes, dauerhaftes Owner-Konto für Christian
Leonhardt. Nur dieses Konto darf die vollständige ChOS-Lesekopie unter
`/workspace/chos/` aufrufen. Owner-Zugriff und Teilnehmerzugriff werden serverseitig
bei jeder Datei geprüft.

## Nutzerweg

1. Eignung und organisatorischer Rahmen werden persönlich geklärt.
2. NDA und Teilnahmeunterlagen werden vereinbart.
3. Die Rechnung über 590 Euro netto wird gestellt und bezahlt.
4. Christian erstellt manuell eine einmalige Einladung.
5. Der Teilnehmer vergibt selbst ein Passwort und bestätigt die Teilnahmebedingungen.
6. Der Arbeitsbereich ist bis zu 60 Tage aktiv.
7. Danach bleibt er 30 Tage ausschließlich lesbar.
8. Nach insgesamt 90 Tagen werden Zugriff und Portal-Konto automatisch gelöscht. Zukünftige Arbeitsinhalte müssen an denselben Löschlauf angebunden werden.

## Rollen

### Teilnehmer

- Zugriff nur auf den eigenen Arbeitsbereich
- während der aktiven Phase Bearbeitung zukünftiger Arbeitsflächen
- danach ausschließlich Lesezugriff
- kein Zugriff nach Ablauf oder Deaktivierung

### Christian Leonhardt

- prüft und bestätigt Teilnehmer persönlich
- erstellt, erneuert und deaktiviert Einladungen
- überwacht Laufzeit und Supportstunden
- führt Abschluss und Löschung durch
- besitzt als Owner den persönlichen Zugriff auf die vollständige ChOS-Lesekopie

## Bewusste Entscheidungen

- Kein Selbstregistrierungsformular.
- Keine Magnolia-Redakteurskonten für Beta-Teilnehmer.
- Keine Speicherung von Teilnehmerdaten in öffentlich ausgespielten Magnolia-Content-Workspaces.
- Kein Zugriff von Teilnehmerkonten auf den internen ChOS-Wissensbereich.
- Keine automatischen Marketing- oder E-Mail-Flows im MVP.
- Keine medizinischen, hochsensiblen oder unnötigen personenbezogenen Daten in Arbeitsfällen.

## Technische Abgrenzung

Der Dienst läuft unter `cleonhardt.de/workspace/`, wird aber als eigenständige Anwendung betrieben. Caddy leitet ausschließlich diesen URL-Bereich an das Beta-Portal weiter; alle übrigen Website-Inhalte bleiben bei Magnolia.

Der Owner-Bereich liefert den geprüften statischen ChOS-Stand aus dem privaten
Repository als durchsuchbare, mobile Lesekopie aus. Eine Aktualisierung erfolgt
bewusst über das Synchronisationsskript und einen neuen Container-Build; der
Browserbereich verändert das ChOS-Repository nicht.

Kontodaten liegen in einem separaten Docker-Volume und werden in die bestehende Sicherung aufgenommen. Passwörter werden ausschließlich als Scrypt-Hash gespeichert. Einladungen sind einmalig und laufen nach 72 Stunden ab. Ein Hintergrundlauf entfernt abgelaufene aktivierte Portal-Konten spätestens innerhalb von sechs Stunden nach Ende der Lesephase.

Sicherungskopien rotieren nach dem bestehenden 30-Tage-Backupplan. Gelöschte Beta-Daten können daher noch bis zum Ablauf dieser Sicherungsfrist in nicht produktiv eingebundenen Backups enthalten sein und werden nicht erneut in den laufenden Betrieb zurückgespielt, sofern keine rechtliche Pflicht oder ein technischer Notfall dies erfordert.

## Noch nicht Bestandteil dieses MVP

- strukturierte Fallakte
- Arbeitsjournal und Diagnose-Canvas
- Datei-Uploads
- Termin- oder Supportbuchung
- automatische Löschung zukünftiger personenbezogener Arbeitsinhalte; die Kontolöschung ist bereits enthalten
- Export am Ende der Beta

Diese Funktionen werden erst nach Prüfung des Login-MVP ergänzt.
