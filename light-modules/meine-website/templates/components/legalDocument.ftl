[#import "/meine-website/includes/macros/editMode.ftl" as editMode]
[#import "/meine-website/includes/macros/urls.ftl" as urls]

[#function resolveReference reference workspace]
  [#if !reference?has_content][#return ''][/#if]
  [#local item = cmsfn.contentById(reference?string, workspace)!]
  [#if !item?has_content][#local item = cmsfn.contentByPath(reference?string, workspace)!][/#if]
  [#return item]
[/#function]

[#assign legalDocument = resolveReference(content.legalDocumentReference!'', 'legalDocuments')]

[@editMode.wrapContent]
  [#if legalDocument?has_content]
    <section class="content-section[#if content.showBackground!false] content-section--tinted[/#if]">
      <div class="site-shell prose">
        [#if legalDocument.title?has_content]<h2>${legalDocument.title}</h2>[/#if]
        [#if legalDocument.body?has_content]${urls.normalizeRichText(cmsfn.decode(legalDocument).body)}[/#if]
      </div>
    </section>
  [#elseif cmsfn.editMode]
    <section class="content-section">
      <div class="site-shell prose">
        <p><strong>Bitte einen Rechtstext auswählen.</strong></p>
      </div>
    </section>
  [/#if]
[/@editMode.wrapContent]

