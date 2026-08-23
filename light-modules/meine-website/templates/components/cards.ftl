[#import "/meine-website/includes/macros/editMode.ftl" as editMode]
[#import "/meine-website/includes/macros/image.ftl" as image]
[#import "/meine-website/includes/macros/urls.ftl" as urls]
[#import "/meine-website/includes/macros/commercialMode.ftl" as commercial]
[#assign imageNode = content.imageChooser!]
[#assign linkNode = content.linkChooser!]
[#assign linkHref = ""]
[#assign isExternal = false]

[#if linkNode?has_content && linkNode.field! == "internalLink" && linkNode.internalLink?has_content]
  [#assign linkHref = cmsfn.link(cmsfn.contentByPath(linkNode.internalLink, "website"))!""]
[#elseif linkNode?has_content && linkNode.field! == "externalLink" && linkNode.externalLink?has_content]
  [#assign linkHref = linkNode.externalLink]
  [#assign isExternal = true]
[/#if]

[#if !cmsfn.editMode && commercial.isBlockedPath(linkHref)]
  [#assign linkHref = ""]
[/#if]

[@editMode.wrapContent]
  <section class="card-section">
    <article class="site-shell card[#if linkHref?has_content] card--linked[/#if]">
      [#if imageNode?has_content && imageNode.field?has_content]
        <div class="card__media">
          [#if imageNode.field == "image" && image.hasImage(imageNode.image!"")]
            [@image.renderImageWithClass imageNode.image imageNode.imageAlt!"" "card__image" /]
          [#elseif imageNode.field == "externalImage" && imageNode.externalImage?has_content]
            [@image.renderExternalImageWithClass imageNode.externalImage imageNode.externalImageAlt!"" "card__image" /]
          [/#if]
        </div>
      [/#if]
      <div class="card__content">
        [#if content.title?has_content]<h2>${content.title}</h2>[/#if]
        [#if content.description?has_content]<div class="prose">${urls.normalizeRichText(cmsfn.decode(content).description)}</div>[/#if]
        [#if linkHref?has_content]
          [#assign linkText = linkNode.linkText!'Mehr erfahren']
          [#assign accessibleLinkText = linkText]
          [#if content.title?has_content]
            [#assign accessibleLinkText = linkText + ': ' + content.title]
          [/#if]
          <a class="text-link card__stretched-link" href="${linkHref}" aria-label="${accessibleLinkText}" [#if isExternal]target="_blank" rel="noopener noreferrer"[/#if]>${linkText} <span aria-hidden="true">→</span></a>
        [/#if]
      </div>
    </article>
  </section>
[/@editMode.wrapContent]
