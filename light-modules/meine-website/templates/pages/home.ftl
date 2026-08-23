[#import "/meine-website/includes/macros/commercialMode.ftl" as commercial]

[#function findFirstContentItem parent]
  [#list cmsfn.children(parent)![] as child]
    [#if !cmsfn.isNodeType(child, 'mgnl:folder')]
      [#return child]
    [/#if]
    [#local nestedContentItem = findFirstContentItem(child)]
    [#if nestedContentItem?has_content]
      [#return nestedContentItem]
    [/#if]
  [/#list]
  [#return '']
[/#function]

[#function findRootPage page]
  [#local parentPage = cmsfn.parent(page, 'mgnl:page')!]
  [#if parentPage?has_content]
    [#return findRootPage(parentPage)]
  [/#if]
  [#return page]
[/#function]

[#function pageLink page fallback='#']
  [#local link = cmsfn.link(page)!fallback]
  [#if !ctx.contextPath?has_content && link == '/start']
    [#return '/']
  [/#if]
  [#return link]
[/#function]

[#function findNavigationItems parent]
  [#local items = []]
  [#list cmsfn.children(parent)![] as child]
    [#if child.targetPage?has_content]
      [#local items = items + [child]]
    [#else]
      [#local items = items + findNavigationItems(child)]
    [/#if]
  [/#list]
  [#return items]
[/#function]

[#function isCurrentOrAncestor targetPage currentPage]
  [#if targetPage.@id == currentPage.@id]
    [#return true]
  [/#if]
  [#return currentPage.@path?starts_with(targetPage.@path + '/')]
[/#function]

[#macro renderNavigationItems items level listId='' maxItems=0 overviewPage='']
  <ul class="site-nav__list site-nav__list--level-${level}"[#if listId?has_content] id="${listId}"[/#if]>
    [#list items as navigationItem]
      [#if (maxItems == 0 || navigationItem?index < maxItems) && navigationItem.targetPage?has_content]
        [#assign navigationTargetPage = cmsfn.contentById(navigationItem.targetPage, 'website')!]
        [#if navigationTargetPage?has_content && (cmsfn.editMode || !commercial.isBlockedPath(navigationTargetPage.@path))]
          [#assign navigationChildren = findNavigationItems(navigationItem)]
          [#assign navigationLabel = navigationItem.label!navigationTargetPage.navigationTitle!navigationTargetPage.title!navigationTargetPage.@name]
          <li class="site-nav__item[#if navigationChildren?has_content] site-nav__item--has-children[/#if][#if navigationTargetPage.@path == '/kontakt'] site-nav__item--contact[/#if][#if navigationTargetPage.@path == '/insights'] site-nav__item--default-open[/#if]">
            <div class="site-nav__entry">
              <a href="${pageLink(navigationTargetPage)}"
                 [#if navigationTargetPage.@path == '/kontakt'] class="site-nav__cta"[/#if]
                 [#if isCurrentOrAncestor(navigationTargetPage, content)] aria-current="page"[/#if]>
                <span class="site-nav__link-label">${navigationLabel}</span>
                <span class="site-nav__link-arrow" aria-hidden="true">→</span>
              </a>
              [#if navigationChildren?has_content]
                <button class="site-nav__toggle" type="button" aria-expanded="false" aria-controls="submenu-${navigationItem.@id}" aria-label="Untermenü ${navigationLabel} öffnen" data-menu-label="${navigationLabel}">
                  <span aria-hidden="true">›</span>
                </button>
              [/#if]
            </div>
            [#if navigationChildren?has_content]
              [#assign navigationChildLimit = 0]
              [#assign navigationOverviewPage = '']
              [#if navigationTargetPage.@path == '/insights']
                [#assign navigationChildLimit = 5]
                [#assign navigationOverviewPage = navigationTargetPage]
              [/#if]
              [@renderNavigationItems items=navigationChildren level=level + 1 listId='submenu-' + navigationItem.@id maxItems=navigationChildLimit overviewPage=navigationOverviewPage /]
            [/#if]
          </li>
        [/#if]
      [/#if]
    [/#list]
    [#if maxItems > 0 && items?size > maxItems && overviewPage?has_content]
      <li class="site-nav__item site-nav__item--overview">
        <a href="${pageLink(overviewPage)}">
          <span class="site-nav__link-label">Zur Insights-Übersicht</span>
          <span class="site-nav__link-arrow" aria-hidden="true">→</span>
        </a>
      </li>
    [/#if]
  </ul>
[/#macro]

<!DOCTYPE html>
<html lang="de">
  <head>
    [@cms.page /]
    [#assign pageTitle = content.windowTitle!content.title!'Meine Website']
    [#assign pageDescription = content.metaDescription!'']
    [#assign canonicalBase = 'https://cleonhardt.de']
    [#assign globalSiteSettingsConfig = cmsfn.contentByPath('/cleonhardt/Logo-Cleonhardt', 'siteSettings')!]
    [#assign globalSiteSettingsWorkspaceRoot = cmsfn.contentByPath('/', 'siteSettings')!]
    [#if !globalSiteSettingsConfig?has_content && globalSiteSettingsWorkspaceRoot?has_content]
      [#assign globalSiteSettingsConfig = findFirstContentItem(globalSiteSettingsWorkspaceRoot)]
    [/#if]
    [#if !globalSiteSettingsConfig?has_content]
      [#assign globalSiteSettingsConfig = cmsfn.contentByPath('/website', 'siteSettings')!]
    [/#if]
    [#assign canonicalPath = cmsfn.link(content)!'/start']
    [#if !ctx.contextPath?has_content && canonicalPath == '/start']
      [#assign canonicalPath = '/']
    [/#if]
    [#assign canonicalUrl = canonicalBase + canonicalPath]
    [#assign defaultPageDescription = 'Klarheit für komplexe Produktorganisationen – mit ChOS Richtung, Entscheidungen, Verantwortung, Zusammenarbeit und Lernen verstehen.']
    [#if !pageDescription?has_content][#assign pageDescription = defaultPageDescription][/#if]
    [#assign socialImageUrl = canonicalBase + '/.resources/meine-website/webresources/images/christian-leonhardt-social-card.png?v=20260805-1']
    [#assign organizationLogoUrl = canonicalBase + '/.resources/meine-website/webresources/images/christian-leonhardt-chos-signet-invoice.png?v=20260808-1']
    [#assign personImageUrl = canonicalBase + '/.resources/meine-website/webresources/images/christian-leonhardt-portrait-v1.jpg?v=20260731-1']
    [#assign socialImageAlt = 'Christian Leonhardt – Klarheit für komplexe Produktorganisationen']
    [#if globalSiteSettingsConfig?has_content && globalSiteSettingsConfig.socialImageAlt?has_content]
      [#assign socialImageAlt = globalSiteSettingsConfig.socialImageAlt]
    [/#if]
    [#if content.socialImageAlt?has_content][#assign socialImageAlt = content.socialImageAlt][/#if]
    [#assign isDefaultSocialImage = true]
    [#assign selectedSocialImage = '']
    [#if globalSiteSettingsConfig?has_content && globalSiteSettingsConfig.socialImage?has_content]
      [#assign selectedSocialImage = globalSiteSettingsConfig.socialImage]
    [/#if]
    [#if content.socialImage?has_content][#assign selectedSocialImage = content.socialImage][/#if]
    [#if selectedSocialImage?has_content]
      [#assign customSocialImagePath = damfn.getAssetLinkForId('jcr:' + selectedSocialImage?string)!'']
      [#if customSocialImagePath?has_content]
        [#if ctx.contextPath?has_content && customSocialImagePath?starts_with(ctx.contextPath)]
          [#assign customSocialImagePath = customSocialImagePath?substring(ctx.contextPath?length)]
        [/#if]
        [#if customSocialImagePath?starts_with('http://') || customSocialImagePath?starts_with('https://')]
          [#assign socialImageUrl = customSocialImagePath]
        [#else]
          [#assign socialImageUrl = canonicalBase + customSocialImagePath]
        [/#if]
        [#assign isDefaultSocialImage = false]
      [/#if]
    [/#if]
    [#if globalSiteSettingsConfig?has_content && globalSiteSettingsConfig.logo?has_content]
      [#assign configuredLogoPath = damfn.getAssetLinkForId('jcr:' + globalSiteSettingsConfig.logo?string)!'']
      [#if configuredLogoPath?has_content]
        [#if configuredLogoPath?starts_with('http')]
          [#assign organizationLogoUrl = configuredLogoPath]
        [#else]
          [#assign organizationLogoUrl = canonicalBase + configuredLogoPath]
        [/#if]
      [/#if]
    [/#if]
    [#if globalSiteSettingsConfig?has_content && globalSiteSettingsConfig.portraitImage?has_content]
      [#assign configuredPortraitPath = damfn.getAssetLinkForId('jcr:' + globalSiteSettingsConfig.portraitImage?string)!'']
      [#if configuredPortraitPath?has_content]
        [#if configuredPortraitPath?starts_with('http')]
          [#assign personImageUrl = configuredPortraitPath]
        [#else]
          [#assign personImageUrl = canonicalBase + configuredPortraitPath]
        [/#if]
      [/#if]
    [/#if]
    [#assign isInsightArticle = canonicalPath?starts_with('/insights/')]
    [#assign isProblemContent = canonicalPath == '/probleme' || canonicalPath?starts_with('/probleme/')]
    [#assign isInsightContent = canonicalPath == '/insights' || isInsightArticle]
    [#assign isCaseContent = canonicalPath == '/praxisfaelle' || canonicalPath?starts_with('/praxisfaelle/')]
    [#assign showContentBreadcrumb = isProblemContent || isInsightContent || isCaseContent]
    [#assign breadcrumbSectionLabel = '']
    [#assign breadcrumbSectionUrl = '']
    [#if isProblemContent]
      [#assign breadcrumbSectionLabel = 'Problemsituationen']
      [#assign breadcrumbSectionUrl = '/probleme']
    [#elseif isInsightContent]
      [#assign breadcrumbSectionLabel = 'Insights']
      [#assign breadcrumbSectionUrl = '/insights']
    [#elseif isCaseContent]
      [#assign breadcrumbSectionLabel = 'Praxisfälle']
      [#assign breadcrumbSectionUrl = '/praxisfaelle']
    [/#if]
    [#assign breadcrumbCurrentLabel = content.navigationTitle!content.title!pageTitle]
    [#if content.problemReference?has_content]
      [#assign breadcrumbProblem = cmsfn.contentById(content.problemReference, 'problems')!]
      [#if !breadcrumbProblem?has_content][#assign breadcrumbProblem = cmsfn.contentByPath(content.problemReference, 'problems')!][/#if]
      [#if breadcrumbProblem?has_content][#assign breadcrumbCurrentLabel = breadcrumbProblem.headline!breadcrumbProblem.title!breadcrumbCurrentLabel][/#if]
    [/#if]
    [#assign isProfilePage = canonicalPath == '/ueber-mich']
    [#assign isInsightsPage = canonicalPath == '/insights']
    [#assign isContactPage = canonicalPath == '/kontakt']
    [#assign pageSchemaType = 'WebPage']
    [#if isProfilePage]
      [#assign pageSchemaType = 'ProfilePage']
    [#elseif isInsightsPage]
      [#assign pageSchemaType = 'CollectionPage']
    [#elseif isContactPage]
      [#assign pageSchemaType = 'ContactPage']
    [/#if]
    [#assign pageOffer = '']
    [#if content.offerReference?has_content]
      [#assign pageOffer = cmsfn.contentById(content.offerReference, 'offers')!]
      [#if !pageOffer?has_content][#assign pageOffer = cmsfn.contentByPath(content.offerReference, 'offers')!][/#if]
    [/#if]
    [#assign isServicePage = commercial.offersEnabled && (pageOffer?has_content || content.serviceType?has_content)]
    [#assign serviceName = content.serviceType!pageTitle]
    [#assign servicePriceLow = content.priceLow!'']
    [#assign servicePriceHigh = content.priceHigh!'']
    [#if pageOffer?has_content]
      [#assign serviceName = pageOffer.title!serviceName]
      [#assign servicePriceLow = pageOffer.priceFrom!servicePriceLow]
      [#assign servicePriceHigh = pageOffer.priceTo!servicePriceHigh]
    [/#if]
    [#assign publishedDate = content.datePublished!'2026-07-31']
    [#assign modifiedDate = content.dateModified!publishedDate]
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="color-scheme" content="light">
    <meta name="theme-color" content="#ffffff">
    <style>
      html,
      body {
        margin: 0;
        color: #132019;
        background: #ffffff;
      }
      body::before {
        position: fixed;
        z-index: 2147483647;
        inset: 0;
        background: #f7faf6;
        content: "";
        pointer-events: none;
        animation: initial-paint-timeout 8s step-end forwards;
      }
      @keyframes initial-paint-timeout {
        to {
          opacity: 0;
          visibility: hidden;
        }
      }
    </style>
    <meta name="description" content="${pageDescription}">
    <link rel="canonical" href="${canonicalUrl}">
    <meta property="og:type" content="[#if isInsightArticle]article[#else]website[/#if]">
    <meta property="og:locale" content="de_DE">
    <meta property="og:site_name" content="Christian Leonhardt">
    <meta property="og:title" content="${pageTitle}">
    <meta property="og:description" content="${pageDescription}">
    <meta property="og:url" content="${canonicalUrl}">
    <meta property="og:image" content="${socialImageUrl}">
    <meta property="og:image:secure_url" content="${socialImageUrl}">
    [#if isDefaultSocialImage]
      <meta property="og:image:type" content="image/png">
      <meta property="og:image:width" content="1200">
      <meta property="og:image:height" content="630">
    [/#if]
    <meta property="og:image:alt" content="${socialImageAlt}">
    <meta name="twitter:card" content="summary_large_image">
    <meta name="twitter:title" content="${pageTitle}">
    <meta name="twitter:description" content="${pageDescription}">
    <meta name="twitter:image" content="${socialImageUrl}">
    <meta name="twitter:image:alt" content="${socialImageAlt}">
    [#if isInsightArticle]
      <meta property="article:published_time" content="${publishedDate}">
      <meta property="article:modified_time" content="${modifiedDate}">
      <meta property="article:author" content="https://cleonhardt.de/ueber-mich">
      <meta property="article:section" content="Insights">
    [/#if]
    <script type="application/ld+json">
    {
      "@context": "https://schema.org",
      "@graph": [
        {
          "@type": "WebSite",
          "@id": "https://cleonhardt.de/#website",
          "url": "https://cleonhardt.de/",
          "name": "Christian Leonhardt",
          "inLanguage": "de-DE",
          "publisher": { "@id": "https://cleonhardt.de/#organization" }
        },
        {
          "@type": "Person",
          "@id": "https://cleonhardt.de/ueber-mich#person",
          "name": "Christian Leonhardt",
          "url": "https://cleonhardt.de/ueber-mich",
          "image": "${personImageUrl?json_string}",
          "jobTitle": "Berater für Product Leadership und Organisationsentwicklung",
          "knowsAbout": [
            "Product Leadership",
            "Produktorganisation",
            "Organisationsdiagnose",
            "Operating Models",
            "Transformation",
            "Entscheidungsarchitektur"
          ]
        },
        {
          "@type": "Organization",
          "@id": "https://cleonhardt.de/#organization",
          "name": "Christian Leonhardt",
          "url": "https://cleonhardt.de/",
          "logo": {
            "@type": "ImageObject",
            "@id": "https://cleonhardt.de/#logo",
            "url": "${organizationLogoUrl?json_string}",
            "contentUrl": "${organizationLogoUrl?json_string}",
            "width": 512,
            "height": 512,
            "caption": "Christian Leonhardt"
          },
          "founder": { "@id": "https://cleonhardt.de/ueber-mich#person" }
        },
        {
          "@type": "ImageObject",
          "@id": "${canonicalUrl?json_string}#primaryimage",
          "url": "${socialImageUrl?json_string}",
          "contentUrl": "${socialImageUrl?json_string}",
          "caption": "${socialImageAlt?string?json_string}"[#if isDefaultSocialImage],
          "width": 1200,
          "height": 630[/#if]
        },
        {
          "@type": "${pageSchemaType}",
          "@id": "${canonicalUrl?json_string}#webpage",
          "url": "${canonicalUrl?json_string}",
          "name": "${pageTitle?string?json_string}",
          "description": "${pageDescription?string?json_string}",
          "inLanguage": "de-DE",
          "isPartOf": { "@id": "https://cleonhardt.de/#website" },
          "about": { "@id": "https://cleonhardt.de/ueber-mich#person" },
          "primaryImageOfPage": { "@id": "${canonicalUrl?json_string}#primaryimage" }[#if isProfilePage],
          "mainEntity": { "@id": "https://cleonhardt.de/ueber-mich#person" }[/#if]
        }[#if isInsightArticle || isServicePage],[/#if]
        [#if isInsightArticle]
        {
          "@type": "Article",
          "@id": "${canonicalUrl?json_string}#article",
          "url": "${canonicalUrl?json_string}",
          "headline": "${pageTitle?string?json_string}",
          "description": "${pageDescription?string?json_string}",
          "datePublished": "${publishedDate?string?json_string}",
          "dateModified": "${modifiedDate?string?json_string}",
          "inLanguage": "de-DE",
          "mainEntityOfPage": { "@id": "${canonicalUrl?json_string}#webpage" },
          "author": { "@id": "https://cleonhardt.de/ueber-mich#person" },
          "publisher": { "@id": "https://cleonhardt.de/#organization" },
          "image": { "@id": "${canonicalUrl?json_string}#primaryimage" }
        }[#if isServicePage || isInsightArticle],[/#if]
        [/#if]
        [#if isServicePage]
        {
          "@type": "Service",
          "@id": "${canonicalUrl?json_string}#service",
          "url": "${canonicalUrl?json_string}",
          "name": "${serviceName?string?json_string}",
          "description": "${pageDescription?string?json_string}",
          "provider": { "@id": "https://cleonhardt.de/#organization" },
          "areaServed": "Deutschland",
          "audience": {
            "@type": "Audience",
            "audienceType": "${(content.serviceAudience!'Führungskräfte, Produktverantwortliche und digitale Organisationen')?string?json_string}"
          }[#if servicePriceLow?has_content],
          "offers": {
            "@type": "AggregateOffer",
            "priceCurrency": "EUR",
            "lowPrice": "${servicePriceLow?string?json_string}"[#if servicePriceHigh?has_content],
            "highPrice": "${servicePriceHigh?string?json_string}"[/#if],
            "url": "${canonicalUrl?json_string}"
          }[/#if]
        }[#if isInsightArticle],[/#if]
        [/#if]
        [#if isInsightArticle]
        {
          "@type": "BreadcrumbList",
          "@id": "${canonicalUrl?json_string}#breadcrumb",
          "itemListElement": [
            {
              "@type": "ListItem",
              "position": 1,
              "name": "Start",
              "item": "https://cleonhardt.de/"
            },
            {
              "@type": "ListItem",
              "position": 2,
              "name": "Insights",
              "item": "https://cleonhardt.de/insights"
            },
            {
              "@type": "ListItem",
              "position": 3,
              "name": "${pageTitle?string?json_string}",
              "item": "${canonicalUrl?json_string}"
            }
          ]
        }
        [/#if]
      ]
    }
    </script>
    <link rel="icon" type="image/svg+xml" sizes="any" href="${ctx.contextPath}/.resources/meine-website/webresources/images/christian-leonhardt-chos-signet.svg?v=20260728-1">
    <link rel="stylesheet" href="${ctx.contextPath}/.resources/meine-website/webresources/css/site.css?v=20260813-8">
    <script src="${ctx.contextPath}/.resources/meine-website/webresources/js/navigation.js?v=20260814-1" defer></script>
    <script src="${ctx.contextPath}/.resources/meine-website/webresources/js/page-transitions.js?v=20260729-2" defer></script>
    <script src="${ctx.contextPath}/.resources/meine-website/webresources/js/attribution.js?v=20260731-1" defer></script>
    [#if canonicalPath?ends_with('/chos-selbstcheck')]
    <script src="${ctx.contextPath}/.resources/meine-website/webresources/js/jspdf.umd.min.js?v=2.5.2" defer></script>
    <script src="${ctx.contextPath}/.resources/meine-website/webresources/js/chos-check.js?v=20260813-1" defer></script>
    [/#if]
    [#if canonicalPath?ends_with('/kontakt') || canonicalPath?ends_with('/clarity-session') || canonicalPath?ends_with('/angebot-anfragen')]
      <script src="${ctx.contextPath}/.resources/meine-website/webresources/js/hubspot-form.js?v=20260813-1" defer></script>
    [/#if]
    <title>${pageTitle}</title>
  </head>
  <body>
    <a class="skip-link" href="#inhalt">Zum Inhalt springen</a>
    [#assign rootPage = findRootPage(content)]
    [#assign homePage = cmsfn.contentByPath('/home', 'website')!rootPage]
    [#assign homeUrl = pageLink(homePage)]
    [#assign brandName = homePage.brandName!'Christian Leonhardt']
    [#assign siteSettingsConfig = globalSiteSettingsConfig]
    [#assign logoAltText = brandName]
    [#assign logoTargetPageId = '']
    [#assign logoTargetPage = '']
    [#assign logoUrl = homeUrl]
    [#assign navigationConfig = '']
    [#assign navigationItems = []]
    [#assign navigationContactPage = '']
    [#assign navigationContactLabel = 'Kontakt']
    [#assign footerConfig = '']
    [#assign footerText = homePage.footerText!'Klarheit für komplexe Produktorganisationen.']
    [#assign footerLinks = []]
    [#assign relatedInsightsPage = cmsfn.contentByPath('/insights', 'website')!]
    [#assign relatedInsights = []]
    [#assign allInsights = []]
    [#assign relatedInsightsExcludedPaths = ['/kontakt', '/clarity-session', '/angebot-anfragen', '/impressum', '/datenschutz', '/insights']]
    [#assign showRelatedInsights = !isProblemContent && !relatedInsightsExcludedPaths?seq_contains(content.@path)]

    [#if showRelatedInsights && relatedInsightsPage?has_content]
      [#list cmsfn.children(relatedInsightsPage, 'mgnl:page')![] as insightPage]
        [#assign allInsights = allInsights + [insightPage]]
      [/#list]
      [#assign currentInsightIndex = -1]
      [#list allInsights as insightPage]
        [#if insightPage.@id == content.@id][#assign currentInsightIndex = insightPage?index][/#if]
      [/#list]
      [#if currentInsightIndex != -1 && allInsights?size > 1]
        [#list 1..3 as offset]
          [#if offset < allInsights?size]
            [#assign relatedInsights = relatedInsights + [allInsights[(currentInsightIndex + offset) % allInsights?size]]]
          [/#if]
        [/#list]
      [#else]
        [#list allInsights as insightPage]
          [#if insightPage.@id != content.@id && relatedInsights?size < 3]
            [#assign relatedInsights = relatedInsights + [insightPage]]
          [/#if]
        [/#list]
      [/#if]
    [/#if]

    [#assign navigationWorkspaceRoot = cmsfn.contentByPath('/', 'navigation')!]
    [#if navigationWorkspaceRoot?has_content]
      [#assign navigationConfig = findFirstContentItem(navigationWorkspaceRoot)]
    [/#if]
    [#if !navigationConfig?has_content]
      [#assign navigationConfig = cmsfn.contentByPath('/cleonhardt/Main', 'navigation')!]
    [/#if]
    [#if navigationConfig?has_content]
      [#assign navigationItems = findNavigationItems(navigationConfig)]
    [/#if]
    [#list navigationItems as navigationItem]
      [#if navigationItem.targetPage?has_content]
        [#assign navigationItemTarget = cmsfn.contentById(navigationItem.targetPage, 'website')!]
        [#if navigationItemTarget?has_content && navigationItemTarget.@path == '/kontakt']
          [#assign navigationContactPage = navigationItemTarget]
          [#assign navigationContactLabel = navigationItem.label!navigationItemTarget.navigationTitle!navigationItemTarget.title!'Kontakt']
        [/#if]
      [/#if]
    [/#list]
    [#if !navigationContactPage?has_content]
      [#assign navigationContactPage = cmsfn.contentByPath('/kontakt', 'website')!]
      [#if navigationContactPage?has_content]
        [#assign navigationContactLabel = navigationContactPage.navigationTitle!navigationContactPage.title!'Kontakt']
      [/#if]
    [/#if]

    [#assign footerWorkspaceRoot = cmsfn.contentByPath('/', 'footer')!]
    [#if footerWorkspaceRoot?has_content]
      [#assign footerConfig = findFirstContentItem(footerWorkspaceRoot)]
    [/#if]
    [#if !footerConfig?has_content]
      [#assign footerConfig = cmsfn.contentByPath('/cleonhardt/Informationen', 'footer')!]
    [/#if]

    [#if footerConfig?has_content]
      [#if footerConfig.footerText?has_content]
        [#assign footerText = footerConfig.footerText]
      [/#if]
      [#list cmsfn.children(footerConfig)![] as footerChild]
        [#if footerChild.targetPage?has_content]
          [#assign footerLinks = footerLinks + [footerChild]]
        [#else]
          [#assign footerLinks = footerLinks + (cmsfn.children(footerChild)![])]
        [/#if]
      [/#list]
    [/#if]

    [#if siteSettingsConfig?has_content]
      [#assign logoAltText = siteSettingsConfig.logoAltText!brandName]
      [#assign logoTargetPageId = (siteSettingsConfig.logoTargetPage!'')?string]
    [/#if]
    [#if logoTargetPageId?has_content]
      [#assign logoTargetPage = cmsfn.contentById(logoTargetPageId, 'website')!]
      [#if logoTargetPage?has_content]
        [#assign logoUrl = pageLink(logoTargetPage, homeUrl)]
      [/#if]
    [/#if]
    <header class="site-header">
      <div class="site-shell site-header__inner">
        <a class="site-brand" href="${logoUrl}" aria-label="${logoAltText}">
          <svg class="site-brand__logo site-brand__logo--inline"
               xmlns="http://www.w3.org/2000/svg"
               viewBox="0 0 128 128"
               width="44"
               height="44"
               aria-hidden="true"
               focusable="false">
            <path d="M101 27 C88 16 71 12 55 16 C33 21 18 41 18 63 C18 86 34 106 56 111 C73 115 91 111 104 99"
                  fill="none"
                  stroke="#0D3F29"
                  stroke-width="12"
                  stroke-linecap="round"/>
            <path d="M75 40 V86 Q75 94 83 94 H112"
                  fill="none"
                  stroke="#28714E"
                  stroke-width="11"
                  stroke-linecap="round"
                  stroke-linejoin="round"/>
            <circle cx="75" cy="40" r="9" fill="#D8EF77"/>
            <circle cx="75" cy="40" r="3.5" fill="#0D3F29"/>
          </svg>
        </a>
        <button class="site-nav-trigger" type="button" aria-expanded="false" aria-controls="hauptnavigation" aria-label="Hauptmenü öffnen">
          <span class="site-nav-trigger__label">Menü</span>
          <span class="site-nav-trigger__icon" aria-hidden="true">
            <span></span>
            <span></span>
            <span></span>
          </span>
        </button>
        <nav class="site-nav" id="hauptnavigation" aria-label="Hauptnavigation">
          [#if navigationItems?has_content]
            [@renderNavigationItems items=navigationItems level=1 /]
          [#else]
            <ul class="site-nav__list site-nav__list--level-1">
              <li class="site-nav__item">
                <a href="${homeUrl}"[#if homePage.@id == content.@id] aria-current="page"[/#if]>
                  <span class="site-nav__link-label">Start</span>
                  <span class="site-nav__link-arrow" aria-hidden="true">→</span>
                </a>
              </li>
              [#list cmsfn.children(homePage, 'mgnl:page') as child]
                [#if !(child.hideInNavigation!false)]
                  <li class="site-nav__item[#if child.@path == '/kontakt'] site-nav__item--contact[/#if]">
                    <a href="${cmsfn.link(child)!'#'}"[#if child.@path == '/kontakt'] class="site-nav__cta"[/#if][#if child.@id == content.@id] aria-current="page"[/#if]>
                      <span class="site-nav__link-label">${child.navigationTitle!child.title!child.@name}</span>
                      <span class="site-nav__link-arrow" aria-hidden="true">→</span>
                    </a>
                  </li>
                [/#if]
              [/#list]
            </ul>
          [/#if]
          [#if navigationContactPage?has_content]
            <div class="site-nav__footer">
              <a class="site-nav__footer-cta" href="${cmsfn.link(navigationContactPage)!'#'}"[#if navigationContactPage.@id == content.@id] aria-current="page"[/#if]>
                <span>${navigationContactLabel}</span>
                <span class="site-nav__footer-arrow" aria-hidden="true">→</span>
              </a>
              <nav class="site-nav__legal" aria-label="Rechtliche Informationen">
                <a href="${ctx.contextPath}/impressum"[#if canonicalPath == '/impressum'] aria-current="page"[/#if]>Impressum</a>
                <a href="${ctx.contextPath}/datenschutz"[#if canonicalPath == '/datenschutz'] aria-current="page"[/#if]>Datenschutz</a>
              </nav>
            </div>
          [/#if]
        </nav>
      </div>
    </header>

    <div class="page-transition" aria-hidden="true">
      <div class="site-shell page-transition__shell">
        <span class="page-transition__eyebrow"></span>
        <span class="page-transition__title"></span>
        <span class="page-transition__line page-transition__line--wide"></span>
        <span class="page-transition__line"></span>
        <div class="page-transition__cards">
          <span></span>
          <span></span>
          <span></span>
        </div>
      </div>
    </div>

    <main id="inhalt">
      [#if showContentBreadcrumb]
        <nav class="content-breadcrumb" aria-label="Breadcrumb">
          <div class="site-shell">
            <ol class="content-breadcrumb__list">
              <li><a href="${ctx.contextPath}/">Startseite</a></li>
              [#if canonicalPath != breadcrumbSectionUrl]
                <li><a href="${ctx.contextPath}${breadcrumbSectionUrl}">${breadcrumbSectionLabel}</a></li>
              [/#if]
              <li><span aria-current="page">[#if canonicalPath == breadcrumbSectionUrl]${breadcrumbSectionLabel}[#else]${breadcrumbCurrentLabel}[/#if]</span></li>
            </ol>
          </div>
        </nav>
      [/#if]
      [@cms.area name="main" /]

      [#if relatedInsights?has_content]
        <section class="related-insights" aria-labelledby="related-insights-title">
          <div class="site-shell">
            <div class="related-insights__header">
              <div>
                <p class="eyebrow">Verwandte Insights</p>
                <h2 id="related-insights-title">Weitere Perspektiven für klare Entscheidungen.</h2>
              </div>
              <a class="related-insights__overview" href="${pageLink(relatedInsightsPage)}">
                Alle Insights <span aria-hidden="true">→</span>
              </a>
            </div>
            <div class="related-insights__grid">
              [#list relatedInsights as insightPage]
                [#assign insightTitle = insightPage.title!insightPage.navigationTitle!insightPage.@name]
                <article class="related-insights__card">
                  <p class="mvp-meta">Insight</p>
                  <h3>
                    <a class="related-insights__link" href="${pageLink(insightPage)}">${insightTitle}</a>
                  </h3>
                  [#if insightPage.metaDescription?has_content]
                    <p>${insightPage.metaDescription}</p>
                  [/#if]
                  <span class="related-insights__read-more" aria-hidden="true">Artikel lesen →</span>
                </article>
              [/#list]
            </div>
          </div>
        </section>
      [/#if]
    </main>

    <footer class="site-footer">
      <div class="site-shell site-footer__inner">
        <div class="site-footer__identity">
          <strong>${brandName}</strong>
          <span>${footerText}</span>
          <small class="site-footer__copyright">© 2026 Christian Leonhardt</small>
        </div>
        [#if footerLinks?has_content]
          <nav class="site-footer__nav" aria-label="Links in der Fußzeile">
            [#list footerLinks as footerLink]
              [#if footerLink.targetPage?has_content]
                [#assign targetPage = cmsfn.contentById(footerLink.targetPage, 'website')!]
                [#if targetPage?has_content]
                  <a href="${cmsfn.link(targetPage)!'#'}">${footerLink.label!targetPage.title!targetPage.@name}</a>
                [/#if]
              [/#if]
            [/#list]
          </nav>
        [/#if]
      </div>
    </footer>
  </body>
</html>
