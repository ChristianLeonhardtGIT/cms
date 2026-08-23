[#ftl output_format="HTML"]
[#assign divIDPrefix = def.parameters.divIDPrefix!]
[#assign divClass = def.parameters.divClass!"form"]
[#assign validationErrors = model.view.validationErrors!{}]

[#if divIDPrefix?has_content]
  [#assign divID = ' id="${divIDPrefix}-${content.@id}"']
[/#if]

<div class="${divClass}"${divID!}>
  [#if actionResult == "go-to-first-page"]
    <div class="text">
      ${i18n.get("form.user.errorMessage.go-to-first-page", [cmsfn.link("website", model.view.firstPage)])}
    </div>
  [#elseif actionResult == "success"]
    <div class="text success" role="status">
      <h2>${model.view.successTitle!i18n['form.default.successTitle']}</h2>
      <p>${model.view.successMessage!}</p>
    </div>
  [#elseif actionResult == "session-expired"]
    [#if content.formTitle?has_content]<h2>${content.formTitle}</h2>[/#if]
    <div class="text error" role="alert">
      ${i18n.get("form.user.errorMessage.session-expired", [cmsfn.link("website", model.view.firstPage)])}
    </div>
  [#elseif actionResult == "failure"]
    [#if content.formTitle?has_content]<h2>${content.formTitle}</h2>[/#if]
    <div class="text error" role="alert">
      <ul><li>${model.view.errorMessage}</li></ul>
    </div>
  [#else]
    <div class="text">
      [#if content.formTitle?has_content]<h2>${content.formTitle}</h2>[/#if]
      [#if content.formText?has_content]<p>${content.formText}</p>[/#if]
      [#if model.displayNavigation?has_content && model.displayNavigation]
        <div id="step-by-step">
          <ol>
            [#list model.previousStepsNavigation as item]
              <li class="done"><a href="${item.href!}">${item.navigationTitle!}</a></li>
            [/#list]
            <li><strong><em>${i18n['nav.selected']} </em>${content.navigationTitle!content.formTitle!content.@name}</strong></li>
            [#list model.nextStepsNavigation as item]
              <li class="done">${item.navigationTitle!}</li>
            [/#list]
          </ol>
        </div>
      [/#if]
    </div>

    [#if validationErrors?size > 0]
      <div id="formErrorsDisplay" class="text error" role="alert">
        <h2>${model.view.errorTitle!i18n['form.default.errorTitle']}</h2>
        <ul>
          [#list validationErrors?keys as key]
            <li><a href="#${key}_label">${validationErrors[key]!}</a></li>
          [/#list]
        </ul>
      </div>
    [/#if]

    <div class="form-wrapper">
      <form id="${content.formName?default("form0")}" method="post" action="" enctype="${def.parameters.formEnctype?default("multipart/form-data")}">
        <div class="form-item-hidden">
          <input type="hidden" name="mgnlModelExecutionUUID" value="${content.@id}">
          <input type="hidden" name="field" value="">
          [#if model.formState?has_content]
            <input type="hidden" name="mgnlFormToken" value="${model.formState.token!}">
          [/#if]
          <input type="hidden" name="csrf" value="${ctx.getAttribute('csrf')!''}">
        </div>
        [@cms.area name="fieldsets"/]
      </form>
    </div>
  [/#if]
</div>
