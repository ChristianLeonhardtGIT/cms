[#-- Die ChOS-Angebote sind durch den Arbeitgeber freigegeben. --]
[#assign offersEnabled = true]
[#assign hiddenPaths = [
  '/chos-transformation-program'
]]
[#assign blockedPaths = [
  '/leistungen',
  '/clarity-session',
  '/decision-review',
  '/leadership-product-sparring',
  '/executive-sparring',
  '/quick-diagnostic',
  '/product-organisation-diagnostic',
  '/ai-operating-model-assessment',
  '/ai-enabled-workflow-sprint',
  '/chos-transformation-program',
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
  [#local normalized = normalizedPath(path)]
  [#return hiddenPaths?seq_contains(normalized) || (!offersEnabled && blockedPaths?seq_contains(normalized))]
[/#function]

[#function publicLink url]
  [#if isBlockedPath(url)]
    [#return ctx.contextPath + '/kontakt']
  [/#if]
  [#return url]
[/#function]
