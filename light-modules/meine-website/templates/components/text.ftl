[#import "/meine-website/includes/macros/editMode.ftl" as editMode]
[#import "/meine-website/includes/macros/urls.ftl" as urls]
[#import "/meine-website/includes/macros/commercialMode.ftl" as commercial]
[#assign decodedText = '']
[#if content.text?has_content][#assign decodedText = cmsfn.decode(content).text][/#if]
[#assign isCommercialOfferBlock = decodedText?contains('class="mvp-price"')
  || (content.heading!'') == 'Vom konkreten Problem bis zum Organisationsbild'
  || (content.heading!'') == 'Mit einer konkreten Situation starten.']
[#assign showTextBlock = cmsfn.editMode || commercial.offersEnabled || !isCommercialOfferBlock]
[#if showTextBlock]
[@editMode.wrapContent]
  <section class="content-section[#if content.showBackground!false] content-section--tinted[/#if]">
    <div class="site-shell prose">
      [#if content.heading?has_content]<h2>${content.heading}</h2>[/#if]
      [#if decodedText?has_content]${urls.normalizeRichText(decodedText)}[/#if]
    </div>
  </section>
[/@editMode.wrapContent]
[/#if]
