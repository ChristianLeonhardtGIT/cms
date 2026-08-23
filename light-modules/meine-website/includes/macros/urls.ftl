[#import "/meine-website/includes/macros/commercialMode.ftl" as commercial]

[#function normalizeRichText html]
  [#local contextPath = ctx.contextPath!""]
  [#local normalized = html?string]
  [#local normalized = normalized?replace('href="/magnoliaPublic/', 'href="' + contextPath + '/')]
  [#local normalized = normalized?replace('src="/magnoliaPublic/', 'src="' + contextPath + '/')]
  [#local normalized = normalized?replace('action="/magnoliaPublic/', 'action="' + contextPath + '/')]
  [#if !cmsfn.editMode && !commercial.offersEnabled]
    [#list commercial.blockedPaths as blockedPath]
      [#local normalized = normalized?replace('href="' + blockedPath + '"', 'href="' + contextPath + '/kontakt"')]
      [#local normalized = normalized?replace('href="' + contextPath + blockedPath + '"', 'href="' + contextPath + '/kontakt"')]
    [/#list]
    [#local normalized = normalized?replace(
      'Wenn es passt, vereinbaren wir Clarity Session, Sparring oder Diagnostic.',
      'Wenn es passt, klären wir gemeinsam einen sinnvollen nächsten Schritt.'
    )]
  [/#if]
  [#return normalized]
[/#function]
