[#import "/meine-website/includes/macros/editMode.ftl" as editMode]
[#import "/meine-website/includes/macros/urls.ftl" as urls]

[#function firstSettingsItem parent]
  [#list cmsfn.children(parent)![] as child]
    [#if !cmsfn.isNodeType(child, 'mgnl:folder')][#return child][/#if]
    [#local nested = firstSettingsItem(child)]
    [#if nested?has_content][#return nested][/#if]
  [/#list]
  [#return '']
[/#function]

[#assign settings = cmsfn.contentByPath('/cleonhardt/Logo-Cleonhardt', 'siteSettings')!]
[#assign settingsRoot = cmsfn.contentByPath('/', 'siteSettings')!]
[#if !settings?has_content && settingsRoot?has_content][#assign settings = firstSettingsItem(settingsRoot)][/#if]
[#assign portraitAlt = (settings.portraitImageAlt)!'Portrait von Christian Leonhardt']
[#assign portraitLink = '']
[#if settings?has_content && settings.portraitImage?has_content]
  [#assign portraitLink = damfn.getAssetLinkForId('jcr:' + settings.portraitImage?string)!'']
[/#if]
[#assign topics = (content.topics!'')?split(',')]
[#assign targetUrl = '']
[#if content.targetPage?has_content]
  [#assign target = cmsfn.contentByPath(content.targetPage, 'website')!]
  [#if target?has_content][#assign targetUrl = cmsfn.link(target)!''][/#if]
[/#if]

[@editMode.wrapContent]
  <section class="content-section[#if content.aboutMode!false] content-section--tinted[/#if]">
    <div class="site-shell prose">
      [#if content.heading?has_content]<h2>${content.heading}</h2>[/#if]
      <div class="profile-trust[#if content.aboutMode!false] profile-trust--about[/#if]">
        <figure class="profile-trust__media">
          [#if portraitLink?has_content]
            <img class="profile-trust__image" src="${portraitLink}" loading="lazy" decoding="async" alt="${portraitAlt}">
          [#else]
            <picture>
              <source type="image/webp" srcset="${ctx.contextPath}/.resources/meine-website/webresources/images/christian-leonhardt-portrait-v1.webp?v=20260731-1">
              <img class="profile-trust__image" src="${ctx.contextPath}/.resources/meine-website/webresources/images/christian-leonhardt-portrait-v1.jpg?v=20260731-1" width="960" height="1200" loading="lazy" decoding="async" alt="${portraitAlt}">
            </picture>
          [/#if]
        </figure>
        <div class="profile-trust__content">
          [#if content.eyebrow?has_content]<p class="mvp-meta">${content.eyebrow}</p>[/#if]
          [#if content.title?has_content]<h3>${content.title}</h3>[/#if]
          [#if content.lead?has_content]<p class="lead">${content.lead}</p>[/#if]
          [#if content.body?has_content]<div>${urls.normalizeRichText(cmsfn.decode(content).body)}</div>[/#if]
          [#if content.topics?has_content]
            <ul class="profile-trust__topics">[#list topics as topic][#if topic?trim?has_content]<li>${topic?trim}</li>[/#if][/#list]</ul>
          [/#if]
          [#if content.signature?has_content]<p class="profile-signature">${content.signature}</p>[/#if]
          [#if targetUrl?has_content && content.linkText?has_content]<p><a href="${targetUrl}">${content.linkText} <span aria-hidden="true">→</span></a></p>[/#if]
        </div>
      </div>
    </div>
  </section>
[/@editMode.wrapContent]
