package cleonhardt.commands

import info.magnolia.commands.MgnlCommand
import info.magnolia.context.Context
import info.magnolia.context.MgnlContext

import javax.jcr.Node
import javax.jcr.Session
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * Synchronisiert eine ausgewählte Magnolia-Leistung mit dem bereits
 * verknüpften Produkt in HubSpot. Bestehende Rechnungspositionen werden von
 * HubSpot nicht rückwirkend verändert.
 */
class HubSpotProductSyncCommand extends MgnlCommand {

    private static final String WORKSPACE = 'offers'
    private static final String CATALOG_ROOT = '/cleonhardt/'
    private static final String API_ROOT = 'https://api.hubapi.com/crm/v3/objects/products/'

    @Override
    boolean execute(Context context) {
        String repository = (context.get('repository') ?: WORKSPACE).toString()
        String path = context.get('path')?.toString()

        if (repository != WORKSPACE || !path?.startsWith(CATALOG_ROOT)) {
            throw new IllegalArgumentException('Bitte eine einzelne Leistung im Angebotskatalog auswählen.')
        }

        Session session = MgnlContext.getJCRSession(WORKSPACE)
        if (!session.nodeExists(path)) {
            throw new IllegalArgumentException("Magnolia-Leistung wurde nicht gefunden: ${path}")
        }

        Node offer = session.getNode(path)
        String productId = required(offer, 'hubspotProductId', 'Die Leistung ist noch nicht mit einem HubSpot-Produkt verknüpft.')
        String serviceKey = System.getenv('HUBSPOT_SERVICE_KEY')
        if (!serviceKey?.trim()) {
            throw new IllegalStateException('Der HubSpot-Serviceschlüssel ist auf dem Magnolia-Server nicht konfiguriert.')
        }

        String title = required(offer, 'title', 'Der öffentliche Titel fehlt.')
        String sku = required(offer, 'sku', 'Die Produktnummer fehlt.')
        if (!offer.hasProperty('priceFrom')) {
            throw new IllegalArgumentException('Der Preis von fehlt.')
        }

        BigDecimal price = BigDecimal.valueOf(offer.getProperty('priceFrom').double).setScale(2)
        String description = buildDescription(offer)
        String payload = '{"properties":{' +
            '"name":' + jsonString(title) + ',' +
            '"hs_sku":' + jsonString(sku) + ',' +
            '"price":' + jsonString(price.toPlainString()) + ',' +
            '"description":' + jsonString(description) +
            '}}'

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(API_ROOT + productId))
            .timeout(Duration.ofSeconds(20))
            .header('Authorization', 'Bearer ' + serviceKey.trim())
            .header('Content-Type', 'application/json')
            .method('PATCH', HttpRequest.BodyPublishers.ofString(payload))
            .build()

        HttpResponse<String> response
        try {
            response = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build()
                .send(request, HttpResponse.BodyHandlers.ofString())
        } catch (Exception exception) {
            rememberFailure(offer, session, 'HubSpot ist momentan nicht erreichbar.')
            throw new IllegalStateException('HubSpot ist momentan nicht erreichbar.', exception)
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            String safeMessage = "HubSpot hat die Aktualisierung abgelehnt (HTTP ${response.statusCode()})."
            rememberFailure(offer, session, safeMessage)
            throw new IllegalStateException(safeMessage)
        }

        offer.setProperty('hubspotSyncStatus', 'success')
        offer.setProperty('hubspotSyncMessage', 'Produkt erfolgreich mit HubSpot synchronisiert.')
        offer.setProperty('hubspotLastSyncedAt', Calendar.getInstance())
        session.save()
        true
    }

    private static String required(Node node, String property, String message) {
        if (!node.hasProperty(property) || !node.getProperty(property).string?.trim()) {
            throw new IllegalArgumentException(message)
        }
        node.getProperty(property).string.trim()
    }

    private static String buildDescription(Node offer) {
        List<String> parts = []
        ['shortDescription', 'duration', 'priceLabel', 'priceNote'].each { String property ->
            if (offer.hasProperty(property)) {
                String value = offer.getProperty(property).string?.trim()
                if (value) parts << value
            }
        }
        parts.join(' ')
    }

    private static String jsonString(String value) {
        '"' + value
            .replace('\\', '\\\\')
            .replace('"', '\\"')
            .replace('\r', '\\r')
            .replace('\n', '\\n') + '"'
    }

    private static void rememberFailure(Node offer, Session session, String message) {
        offer.setProperty('hubspotSyncStatus', 'error')
        offer.setProperty('hubspotSyncMessage', message)
        offer.setProperty('hubspotLastSyncAttemptAt', Calendar.getInstance())
        session.save()
    }
}
