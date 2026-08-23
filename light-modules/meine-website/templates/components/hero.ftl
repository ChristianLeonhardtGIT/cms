[#import "/meine-website/includes/macros/editMode.ftl" as editMode]
[#import "/meine-website/includes/macros/image.ftl" as image]
[#import "/meine-website/includes/macros/commercialMode.ftl" as commercial]
[#assign imageNode = content.imageChooser!]
[#assign ctaNode = content.ctaChooser!]
[#assign link = ""]
[#assign isExternal = false]
[#assign currentPage = cmsfn.page(content)!]
[#assign pageOffer = '']
[#assign heroEyebrow = content.eyebrow!'']
[#assign offerRequestUrl = '']
[#if currentPage?has_content && currentPage.offerReference?has_content]
  [#assign pageOffer = cmsfn.contentById(currentPage.offerReference, 'offers')!]
  [#if !pageOffer?has_content]
    [#assign pageOffer = cmsfn.contentByPath(currentPage.offerReference, 'offers')!]
  [/#if]
  [#if pageOffer?has_content]
    [#assign heroEyebrow = '']
    [#if pageOffer.duration?has_content][#assign heroEyebrow = pageOffer.duration + ' · '][/#if]
    [#assign heroEyebrow = heroEyebrow + (pageOffer.priceLabel!'')]
    [#assign offerRequestUrl = '/angebot-anfragen?leistung=' + (pageOffer.sku!'')?url]
  [/#if]
[/#if]

[#if ctaNode?has_content && ctaNode.field! == "withCta" && ctaNode.ctaLink?has_content]
  [#if ctaNode.ctaLink.field! == "internalPageLink" && ctaNode.ctaLink.internalLink?has_content]
    [#assign link = cmsfn.link(cmsfn.contentByPath(ctaNode.ctaLink.internalLink, "website"))!""]
  [#elseif ctaNode.ctaLink.field! == "externalPageLink" && ctaNode.ctaLink.externalLink?has_content]
    [#assign link = ctaNode.ctaLink.externalLink]
    [#assign isExternal = true]
  [/#if]
[/#if]

[#if !cmsfn.editMode && commercial.isBlockedPath(link)]
  [#assign link = ""]
[/#if]
[#if !cmsfn.editMode && !commercial.offersEnabled]
  [#assign offerRequestUrl = '']
[/#if]

[@editMode.wrapContent]
  <section class="hero">
    [#if imageNode?has_content && imageNode.field?has_content]
      <div class="hero__media" aria-hidden="true">
        [#if imageNode.field == "image" && image.hasImage(imageNode.image!"")]
          [@image.renderImageWithClass imageNode.image imageNode.imageAlt!"" "hero__image" /]
        [#elseif imageNode.field == "externalImage" && imageNode.externalImage?has_content]
          [@image.renderExternalImageWithClass imageNode.externalImage imageNode.externalImageAlt!"" "hero__image" /]
        [/#if]
      </div>
    [/#if]
    <div class="site-shell hero__content">
      [#if heroEyebrow?has_content]
        <p class="eyebrow"[#if pageOffer?has_content] data-offer-sku="${pageOffer.sku!''}"[/#if]>
          ${heroEyebrow}[#if pageOffer?has_content]<span class="offer-price__marker" aria-hidden="true">*</span>[/#if]
        </p>
        [#if pageOffer?has_content]<p class="offer-tax-note">* Alle Preise verstehen sich netto zzgl. der gesetzlichen Umsatzsteuer.</p>[/#if]
      [/#if]
      [#if content.title?has_content]<h1>${content.title}</h1>[/#if]
      [#if content.description?has_content]<p class="hero__lead">${content.description}</p>[/#if]
      [#if link?has_content]
        <a class="button button--light" href="${link}" [#if isExternal]target="_blank" rel="noopener noreferrer"[/#if]>${ctaNode.ctaText!'Mehr erfahren'}</a>
      [#elseif offerRequestUrl?has_content]
        <a class="button button--light" href="${offerRequestUrl}">Angebot anfragen</a>
      [/#if]
    </div>
  </section>
[/@editMode.wrapContent]
