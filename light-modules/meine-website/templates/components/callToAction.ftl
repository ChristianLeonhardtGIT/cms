[#import "/meine-website/includes/macros/editMode.ftl" as editMode]
[#import "/meine-website/includes/macros/urls.ftl" as urls]
[#import "/meine-website/includes/macros/commercialMode.ftl" as commercial]
[#assign linkNode = content.pageLinkChooser!]
[#assign link = ""]
[#assign isExternal = false]
[#if linkNode?has_content && linkNode.field! == "internalPageLink" && linkNode.internalLink?has_content]
  [#assign link = cmsfn.link(cmsfn.contentByPath(linkNode.internalLink, "website"))!""]
[#elseif linkNode?has_content && linkNode.field! == "externalPageLink" && linkNode.externalLink?has_content]
  [#assign link = linkNode.externalLink]
  [#assign isExternal = true]
[/#if]

[#assign showComponent = cmsfn.editMode || !commercial.isBlockedPath(link)]
[#if showComponent]
[@editMode.wrapContent]
  <section class="cta-section">
    <div class="site-shell cta[#if link?has_content] cta--linked[/#if]">
      <div>
        [#if content.title?has_content]<h2>${content.title}</h2>[/#if]
        [#if content.description?has_content]<div class="cta__text">${urls.normalizeRichText(cmsfn.decode(content).description)}</div>[/#if]
      </div>
      [#if link?has_content]
        [#assign buttonText = content.buttonText!'Los geht’s']
        [#assign accessibleLinkText = buttonText]
        [#if content.title?has_content]
          [#assign accessibleLinkText = buttonText + ': ' + content.title]
        [/#if]
        <a class="button cta__stretched-link" href="${link}" aria-label="${accessibleLinkText}" [#if isExternal]target="_blank" rel="noopener noreferrer"[/#if]>${buttonText}</a>
      [/#if]
    </div>
  </section>
[/@editMode.wrapContent]
[/#if]
