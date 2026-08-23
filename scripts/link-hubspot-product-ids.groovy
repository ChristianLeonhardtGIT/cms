import info.magnolia.context.MgnlContext
import javax.jcr.Node
import javax.jcr.Session

/*
 * Hinterlegt die einmalig von HubSpot vergebenen Produkt-IDs im zentralen
 * Magnolia-Angebotskatalog. Das Skript ist wiederholbar und verändert keine
 * Preise, Texte oder sonstigen Leistungsdaten.
 */

Session offers = MgnlContext.getJCRSession('offers')

Map<String, String> hubspotProductIds = [
    'chos-clarity-session'           : '427873950906',
    'decision-review'                : '427851108552',
    'leadership-product-sparring'    : '427808389315',
    'executive-sparring'             : '427822658770',
    'chos-quick-diagnostic'           : '427905065206',
    'product-organisation-diagnostic': '427866865890',
    'workshops'                      : '427910434033'
]

hubspotProductIds.each { String nodeName, String productId ->
    String path = "/cleonhardt/${nodeName}"
    if (!offers.nodeExists(path)) {
        throw new IllegalStateException("Magnolia-Leistung fehlt: ${path}")
    }

    Node offer = offers.getNode(path)
    offer.setProperty('hubspotProductId', productId)
    println "${offer.getProperty('sku').string} -> ${productId}"
}

offers.save()
println "${hubspotProductIds.size()} HubSpot-Produkt-IDs im Magnolia-Katalog hinterlegt."
