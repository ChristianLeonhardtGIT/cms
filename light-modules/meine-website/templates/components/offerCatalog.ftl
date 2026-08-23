[#import "/meine-website/includes/macros/editMode.ftl" as editMode]
[#import "/meine-website/includes/macros/urls.ftl" as urls]
[#import "/meine-website/includes/macros/commercialMode.ftl" as commercial]

[#assign selectedCategory = content.category!'all']
[#assign offerRoot = cmsfn.contentByPath('/cleonhardt', 'offers')!]
[#assign offers = []]
[#if offerRoot?has_content]
  [#list (cmsfn.children(offerRoot)![])?sort_by('sortOrder') as offer]
    [#assign isActive = offer.active!false]
    [#assign matchesFeatured = (!(content.featuredOnly!false)) || (offer.featured!false)]
    [#assign matchesCategory = (selectedCategory == 'all') || ((offer.category!'') == selectedCategory)]
    [#if isActive && matchesFeatured && matchesCategory]
      [#assign offers = offers + [offer]]
    [/#if]
  [/#list]
[/#if]

[#if commercial.offersEnabled || cmsfn.editMode]
[@editMode.wrapContent]
  <section class="content-section offer-catalog[#if content.showBackground!false] content-section--tinted[/#if]" data-offer-category="${selectedCategory}">
    <div class="site-shell prose">
      [#if content.heading?has_content]<h2>${content.heading}</h2>[/#if]
      [#if content.intro?has_content]<div class="section-intro">${urls.normalizeRichText(cmsfn.decode(content).intro)}</div>[/#if]
      [#if offers?has_content]
        <div class="mvp-grid mvp-grid--${content.columns!'3'} offer-catalog__grid">
          [#list offers as offer]
            [#assign targetPage = '']
            [#assign targetUrl = '/kontakt?angebot=' + (offer.sku!'')?url]
            [#if offer.targetPage?has_content]
              [#assign targetPage = cmsfn.contentById(offer.targetPage, 'website')!]
              [#if targetPage?has_content][#assign targetUrl = cmsfn.link(targetPage)!targetUrl][/#if]
            [/#if]
            <article class="mvp-panel offer-card[#if offer.accent!false] mvp-panel--accent[/#if]"
                     data-offer-sku="${offer.sku!''}"
                     data-offer-price-from="${offer.priceFrom!''}"
                     data-offer-price-to="${offer.priceTo!''}"
                     data-offer-currency="${offer.currency!'EUR'}">
              <p class="mvp-meta">[#if offer.duration?has_content]${offer.duration} · [/#if]${offer.priceLabel!''}<span class="offer-price__marker" aria-hidden="true">*</span></p>
              <h3>${offer.title!offer.@name}</h3>
              <p>${offer.shortDescription!''}</p>
              [#if content.showFeatures!false]
                [#assign features = []]
                [#list cmsfn.children(offer)![] as child]
                  [#if child.text?has_content]
                    [#assign features = features + [child]]
                  [#else]
                    [#list cmsfn.children(child)![] as feature]
                      [#if feature.text?has_content][#assign features = features + [feature]][/#if]
                    [/#list]
                  [/#if]
                [/#list]
                [#if features?has_content]
                  <h4>Enthalten</h4>
                  <ul class="mvp-checks">
                    [#list features as feature]<li>${feature.text}</li>[/#list]
                  </ul>
                [/#if]
              [/#if]
              [#if (content.showPriceNote!false) && (offer.priceNote!'')?has_content]<p class="offer-card__note">${offer.priceNote!''}</p>[/#if]
              <p class="mvp-panel__action">
                <a class="mvp-offer-cta" href="${targetUrl}" data-offer-sku="${offer.sku!''}">
                  ${offer.ctaLabel!'Details ansehen'} <span aria-hidden="true">→</span>
                </a>
              </p>
            </article>
          [/#list]
        </div>
        <p class="offer-tax-note offer-catalog__tax-note">* Alle Preise verstehen sich netto zzgl. der gesetzlichen Umsatzsteuer.</p>
      [#else]
        <p class="offer-catalog__empty">Für diese Auswahl sind noch keine aktiven Angebote gepflegt.</p>
      [/#if]
    </div>
  </section>
[/@editMode.wrapContent]
[/#if]
