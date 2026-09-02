# Rollout: Redaktion, GEO und Sichtbarkeit

Dieser Rollout darf gemäß `docs/SOURCE_OF_TRUTH.md` erst nach Review und Merge
nach `main` auf Produktion ausgeführt werden.

## 1. Redaktioneller Sofortfix

1. Geänderte Light-Module-, Script- und Deployment-Dateien nach
   `/opt/cleonhardt` ausrollen.
2. `scripts/fix-editorial-du-tone.groovy` in der Magnolia-Groovy-Konsole
   ausführen. Das Skript ersetzt ausschließlich bestätigte Mischformen und
   veröffentlicht nur tatsächlich geänderte Top-Level-Seiten.
3. `npm run test:tone` ausführen. Erwartung: 35 geprüfte Seiten, keine Funde.

## 2. GEO-Inhalte

1. `scripts/implement-geo-content.groovy` ausführen.
2. `scripts/implement-public-faqs.groovy` danach ausführen, weil der GEO-Aufbau
   die Hauptbereiche der Insights neu anlegt.
3. `scripts/publish-geo-content.groovy` und
   `scripts/publish-public-faqs.groovy` ausführen.
4. In den Website-Einstellungen nur bestätigte externe Profile in
   `linkedInUrl` beziehungsweise `professionalProfileUrl` hinterlegen und die
   Einstellungen veröffentlichen. Ohne bestätigte URL bleibt `sameAs` bewusst
   aus den Person-Daten entfernt.

## 3. Technische Sichtbarkeit

1. `scripts/fix-central-portrait-delivery.groovy` ausführen. Damit wird die
   fehlerhafte DAM-Referenz entfernt und der optimierte WebP-/JPEG-Fallback
   aktiviert.
2. Caddy nach Prüfung der Konfiguration neu laden, damit `/llms.txt` statisch
   erreichbar ist.
3. `cleonhardt-sitemap.service` ausführen. Der Generator übernimmt valide
   `dateModified`-Werte aus den strukturierten CMS-Daten als `<lastmod>`.
4. Über `deploy/indexnow-submit.sh` die tatsächlich geänderten öffentlichen
   URLs melden. Die pausierte Route `/workshops` bleibt ausgeschlossen.

## Abnahme

```text
npm run test:editorial-geo-visibility
npm run test:tone
node scripts/test-geo-live.mjs
node scripts/test-profile-trust-live.mjs
```

Zusätzlich manuell prüfen:

- `/angebot-anfragen`, `/kontakt` und `/chos-selbstcheck` auf Desktop und Mobil,
- sichtbare Autor-/Datumszeile auf mindestens zwei Insights,
- `/llms.txt`, `/sitemap.xml` und die Porträtdateien per HTTP,
- `/workshops` weiterhin mit Status 404 und ohne Sitemap-Eintrag.
