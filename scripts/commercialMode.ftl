[#-- ChOS-2.0-Angebote sind freigegeben; die Liste bleibt als dokumentierte Alt-Sicherung bestehen. --]
[#assign offersEnabled = true]
[#assign blockedPaths = [
  '/leistungen',
  '/clarity-session',
  '/decision-review',
  '/leadership-product-sparring',
  '/executive-sparring',
  '/quick-diagnostic',
  '/product-organisation-diagnostic',
  '/workshops',
  '/angebot-anfragen'
]]

[#function normalizedPath url]
  [#local path = (url!'')?string?split('?')[0]?split('#')[0]]
  [#local contextPath = ctx.contextPath!'']
  [#if contextPath?has_content && path?starts_with(contextPath)]
    [#local path = path?substring(contextPath?length)]
  [/#if]
  [#return path]
[/#function]

[#function isBlockedPath path]
  [#return !offersEnabled && blockedPaths?seq_contains(normalizedPath(path))]
[/#function]

[#function publicLink url]
  [#if isBlockedPath(url)]
    [#return ctx.contextPath + '/kontakt']
  [/#if]
  [#return url]
[/#function]
