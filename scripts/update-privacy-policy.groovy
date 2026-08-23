import info.magnolia.context.MgnlContext
import javax.jcr.Node
import javax.jcr.Session

/*
 * In Magnolias Groovy-App auf der Author-Instanz ausführen.
 * Aktualisiert ausschließlich die Datenschutzseite und den Datenschutzhinweis
 * im Kontaktformular. Das Skript kann gefahrlos wiederholt werden.
 */

Session website = MgnlContext.getJCRSession('website')
Node privacyPage = website.getNode('/datenschutz')
Node contactPage = website.getNode('/kontakt')

Node findByTemplate(Node root, String template) {
    if (root.hasProperty('mgnl:template') &&
        root.getProperty('mgnl:template').string == template) {
        return root
    }

    def children = root.nodes
    while (children.hasNext()) {
        Node match = findByTemplate(children.nextNode(), template)
        if (match != null) {
            return match
        }
    }
    null
}

Node findByControlName(Node root, String controlName) {
    if (root.hasProperty('controlName') &&
        root.getProperty('controlName').string == controlName) {
        return root
    }

    def children = root.nodes
    while (children.hasNext()) {
        Node match = findByControlName(children.nextNode(), controlName)
        if (match != null) {
            return match
        }
    }
    null
}

Node textComponent = findByTemplate(privacyPage, 'meine-website:components/text')
if (textComponent == null) {
    throw new IllegalStateException('Textkomponente unter /datenschutz wurde nicht gefunden.')
}

Node privacyField = findByControlName(contactPage, 'datenschutz')
if (privacyField == null) {
    throw new IllegalStateException('Datenschutzfeld unter /kontakt wurde nicht gefunden.')
}

String privacyHtml = '''
<p><strong>Stand: 29. Juli 2026</strong></p>
<p>Diese Datenschutzerklärung beschreibt die Verarbeitung personenbezogener Daten beim Besuch von cleonhardt.de und bei einer Kontaktaufnahme. Die Website verwendet derzeit keine Reichweitenanalyse, keine Marketing-Tracker, keinen Newsletterdienst und keine eingebetteten Video- oder Social-Media-Dienste.</p>

<h3>1. Verantwortlicher</h3>
<p>Christian Leonhardt<br>Am Spelzgarten 18<br>50129 Bergheim<br>Deutschland</p>
<p>Telefon: <a href="tel:+4915259765342">0152 59765342</a><br>E-Mail: <a href="mailto:kontakt@cleonhardt.de">kontakt@cleonhardt.de</a></p>

<h3>2. Bereitstellung und Hosting der Website</h3>
<p>Die Website und die geschäftliche E-Mail-Infrastruktur werden bei <strong>HOSTINGER operations, UAB</strong>, Švitrigailos str. 34, LT-03230 Vilnius, Litauen („Hostinger“) betrieben. Hostinger verarbeitet Daten als Auftragsverarbeiter auf Grundlage der in den Vertrag einbezogenen Vereinbarung zur Auftragsverarbeitung.</p>
<p>Beim Aufruf der Website werden technisch erforderliche Verbindungsdaten verarbeitet. Dazu können insbesondere IP-Adresse, Datum und Uhrzeit, angeforderte Seite oder Datei, übertragene Datenmenge, HTTP-Status, Browser- und Geräteinformationen sowie technische Fehlerdaten gehören. Diese Verarbeitung dient der sicheren, stabilen und fehlerfreien Bereitstellung der Website sowie der Erkennung und Abwehr von Angriffen. Rechtsgrundlage ist Art. 6 Abs. 1 lit. f DSGVO. Unser berechtigtes Interesse liegt im sicheren und zuverlässigen Betrieb der Website.</p>
<p>Zugriffs- und Anwendungsprotokolle werden auf dem von uns betriebenen Server grundsätzlich für höchstens 30 Tage vorgehalten und anschließend automatisch gelöscht. Eine längere Speicherung erfolgt nur, wenn sie zur Aufklärung eines konkreten Sicherheitsvorfalls erforderlich ist. Sicherungskopien werden turnusmäßig überschrieben; im Wiederherstellungsfall können darin enthaltene Daten vorübergehend erneut verarbeitet werden.</p>
<p>Weitere Informationen: <a href="https://www.hostinger.com/de/legal/datenschutz-bestimmungen">Datenschutz bei Hostinger</a> und <a href="https://www.hostinger.com/de/legal/dpa">Vereinbarung zur Auftragsverarbeitung</a>.</p>

<h3>3. Technisch erforderliche Cookies</h3>
<p>Die Website setzt ausschließlich technisch erforderliche Sicherheitsmechanismen ein. Beim Seitenaufruf kann insbesondere ein mit <code>csrf</code> bezeichnetes Cookie gesetzt werden. Es schützt Formulare und Anfragen gegen missbräuchliche Übermittlung, ist nur über eine verschlüsselte Verbindung nutzbar, für Skripte nicht auslesbar und wird nicht zu Analyse- oder Werbezwecken verwendet.</p>
<p>Das Speichern technisch erforderlicher Informationen auf Ihrem Endgerät erfolgt auf Grundlage von § 25 Abs. 2 Nr. 2 TDDDG. Soweit dabei personenbezogene Daten verarbeitet werden, ist Art. 6 Abs. 1 lit. f DSGVO die Rechtsgrundlage. Da derzeit keine einwilligungspflichtigen Technologien eingesetzt werden, erscheint kein Einwilligungsbanner.</p>

<h3>4. Kontaktformular und HubSpot CRM</h3>
<p>Wenn Sie das Kontaktformular absenden, werden die von Ihnen eingegebenen Daten unmittelbar an unser Kundenbeziehungsmanagement und den gemeinsamen Posteingang von HubSpot übermittelt. Verarbeitet werden Name, E-Mail-Adresse, optional Unternehmen und berufliche Rolle, der gewählte Anlass, Ihre Beschreibung der aktuellen Situation und des gewünschten Ergebnisses, Zeitpunkt und Herkunftsseite der Anfrage sowie technisch erforderliche Verbindungs- und Anfragedaten. Bitte übermitteln Sie keine besonderen Kategorien personenbezogener Daten, etwa Gesundheitsdaten, über das Formular.</p>
<p>Wir nutzen hierfür HubSpot. Vertragspartner für Kunden in Deutschland ist <strong>HubSpot Germany GmbH</strong>. HubSpot verarbeitet die Formulardaten in unserem Auftrag, stellt sie im CRM und im Conversations-Posteingang bereit und ermöglicht die Bearbeitung und Beantwortung Ihrer Anfrage. Die Verarbeitung erfolgt zur Durchführung vorvertraglicher Maßnahmen oder zur Vertragserfüllung auf Grundlage von Art. 6 Abs. 1 lit. b DSGVO. Bei allgemeinen Anfragen ist Art. 6 Abs. 1 lit. f DSGVO die Rechtsgrundlage; unser berechtigtes Interesse liegt in einer strukturierten, sicheren und effizienten Bearbeitung von Anfragen.</p>
<p>Die Pflichtbox unter dem Formular bestätigt lediglich, dass Sie diese Datenschutzerklärung zur Kenntnis genommen haben. Sie ist keine Einwilligung in Werbung. Eine werbliche Ansprache oder Aufnahme in einen Newsletter erfolgt dadurch nicht.</p>
<p>Unser HubSpot-Account wird in der EU-Region betrieben; nach Angaben von HubSpot liegt das regionale Rechenzentrum in Deutschland. Im Rahmen von Support, Sicherheit, Unterauftragsverarbeitung und der globalen Bereitstellung des Dienstes können Daten dennoch durch verbundene Unternehmen oder Dienstleister außerhalb des Europäischen Wirtschaftsraums verarbeitet werden. HubSpot verwendet hierfür nach eigenen Angaben insbesondere Angemessenheitsbeschlüsse wie das EU-US Data Privacy Framework und – soweit erforderlich – die Standardvertragsklauseln der Europäischen Kommission. Die HubSpot-Vereinbarung zur Datenverarbeitung ist in die Nutzungsbedingungen einbezogen.</p>
<p>Wir verwenden auf dieser Website keinen HubSpot-Tracking-Code und übertragen kein HubSpot-Tracking-Cookie mit der Formularanfrage. Das Formular dient ausschließlich der Kontaktaufnahme.</p>
<p>Anfragen ohne anschließende Geschäftsbeziehung werden grundsätzlich spätestens zwölf Monate nach abschließender Bearbeitung gelöscht, sofern keine gesetzlichen Pflichten oder die Geltendmachung, Ausübung oder Verteidigung von Rechtsansprüchen eine längere Speicherung erfordern. Entsteht eine Geschäftsbeziehung, gelten zusätzlich die gesetzlichen handels- und steuerrechtlichen Aufbewahrungsfristen.</p>
<p>Weitere Informationen: <a href="https://legal.hubspot.com/de/privacy-policy">Datenschutzerklärung von HubSpot</a>, <a href="https://legal.hubspot.com/de/dpa">Vereinbarung zur Datenverarbeitung</a>, <a href="https://legal.hubspot.com/de/sub-processors-page">Unterauftragsverarbeiter</a> und <a href="https://knowledge.hubspot.com/de/account-security/hubspot-cloud-infrastructure-and-data-hosting-frequently-asked-questions">Datenhosting bei HubSpot</a>.</p>

<h3>5. Kontaktaufnahme per E-Mail oder Telefon</h3>
<p>Wenn Sie uns per E-Mail oder Telefon kontaktieren, verarbeiten wir Ihre Kontaktdaten, den Inhalt Ihrer Nachricht und die damit verbundenen Kommunikationsdaten zur Bearbeitung Ihres Anliegens. E-Mails werden über die Infrastruktur von Hostinger übermittelt. Soweit erforderlich, wird die Kommunikation zur einheitlichen Bearbeitung im HubSpot CRM dokumentiert.</p>
<p>Rechtsgrundlage ist Art. 6 Abs. 1 lit. b DSGVO, wenn die Kommunikation der Anbahnung oder Durchführung eines Vertrags dient. In anderen Fällen erfolgt die Verarbeitung auf Grundlage von Art. 6 Abs. 1 lit. f DSGVO und unserem berechtigten Interesse an einer effizienten geschäftlichen Kommunikation. Für die Löschung gelten die im Abschnitt zum Kontaktformular genannten Grundsätze; gesetzliche Aufbewahrungspflichten bleiben unberührt.</p>

<h3>6. Empfänger und Übermittlungen in Drittländer</h3>
<p>Personenbezogene Daten erhalten nur Stellen, die sie zur Erfüllung der beschriebenen Zwecke benötigen. Dazu gehören insbesondere Hostinger als Hosting- und E-Mail-Anbieter sowie HubSpot und dessen veröffentlichte Unterauftragsverarbeiter für Kontaktformular, CRM und Kommunikation. Weitere Empfänger kommen nur hinzu, wenn dies gesetzlich vorgeschrieben, zur Vertragsdurchführung erforderlich oder durch eine andere Rechtsgrundlage erlaubt ist.</p>
<p>Bei Übermittlungen in Staaten außerhalb der EU beziehungsweise des EWR achten wir auf die Voraussetzungen der Art. 44 ff. DSGVO, insbesondere einen Angemessenheitsbeschluss oder geeignete Garantien wie die Standardvertragsklauseln.</p>

<h3>7. Allgemeine Speicherdauer</h3>
<p>Soweit in dieser Erklärung keine besondere Frist genannt ist, speichern wir personenbezogene Daten nur so lange, wie sie für den jeweiligen Zweck erforderlich sind. Anschließend werden sie gelöscht, sofern keine gesetzlichen Aufbewahrungspflichten, berechtigten Beweissicherungsinteressen oder sonstigen gesetzlichen Erlaubnisse entgegenstehen.</p>

<h3>8. Ihre Rechte</h3>
<p>Im Rahmen der gesetzlichen Voraussetzungen haben Sie insbesondere das Recht auf Auskunft nach Art. 15 DSGVO, Berichtigung nach Art. 16 DSGVO, Löschung nach Art. 17 DSGVO, Einschränkung der Verarbeitung nach Art. 18 DSGVO sowie Datenübertragbarkeit nach Art. 20 DSGVO. Soweit eine Verarbeitung auf einer Einwilligung beruht, können Sie diese jederzeit mit Wirkung für die Zukunft widerrufen.</p>
<p><strong>Widerspruchsrecht:</strong> Soweit die Verarbeitung auf Art. 6 Abs. 1 lit. f DSGVO beruht, können Sie aus Gründen, die sich aus Ihrer besonderen Situation ergeben, jederzeit Widerspruch einlegen. Einer Verarbeitung zum Zweck der Direktwerbung können Sie jederzeit ohne Angabe von Gründen widersprechen.</p>
<p>Zur Ausübung Ihrer Rechte genügt eine Nachricht an <a href="mailto:kontakt@cleonhardt.de">kontakt@cleonhardt.de</a>. Außerdem haben Sie das Recht, sich bei einer Datenschutzaufsichtsbehörde zu beschweren. Für den Sitz des Verantwortlichen ist insbesondere zuständig:</p>
<p>Landesbeauftragte für Datenschutz und Informationsfreiheit Nordrhein-Westfalen<br>Kavalleriestraße 2–4<br>40213 Düsseldorf<br>Telefon: 0211 38424-0<br>E-Mail: <a href="mailto:poststelle@ldi.nrw.de">poststelle@ldi.nrw.de</a><br>Website: <a href="https://www.ldi.nrw.de/">www.ldi.nrw.de</a></p>

<h3>9. Verschlüsselung und Sicherheit</h3>
<p>Die Website wird ausschließlich verschlüsselt über TLS bereitgestellt. Wir treffen angemessene technische und organisatorische Maßnahmen, um personenbezogene Daten gegen Verlust, Manipulation und unberechtigten Zugriff zu schützen. Eine Datenübertragung im Internet kann dennoch nicht vollständig risikofrei sein.</p>

<h3>10. Aktualisierung dieser Datenschutzerklärung</h3>
<p>Wir aktualisieren diese Datenschutzerklärung, wenn sich die Website, eingesetzte Dienste oder rechtliche Anforderungen ändern. Es gilt die jeweils auf dieser Seite veröffentlichte Fassung.</p>
'''

privacyPage.setProperty('windowTitle', 'Datenschutz – Christian Leonhardt')
privacyPage.setProperty('metaDescription', 'Datenschutzerklärung für cleonhardt.de mit Informationen zu Hosting, Kontaktformular und HubSpot CRM.')
textComponent.setProperty('heading', 'Datenschutzerklärung')
textComponent.setProperty('text', privacyHtml.trim())
privacyField.setProperty('labels', 'Ich habe die Datenschutzerklärung zur Kenntnis genommen.:akzeptiert')

website.save()

println "Datenschutzseite ${privacyPage.path} und Formularfeld ${privacyField.path} wurden aktualisiert."
