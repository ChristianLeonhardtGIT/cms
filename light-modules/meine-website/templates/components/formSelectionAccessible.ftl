[#include "/form/components/init.required.ftl"]
[#import "/meine-website/includes/macros/commercialMode.ftl" as commercial]
[#assign controlName = content.controlName!'']

<div ${model.style!}>
  [#if content.title?has_content]
    <label id="${content.controlName!''}_label" for="${content.controlName!''}">
      <span>
        [#if !model.isValid()]
          <em>${i18n['form.error.field']}</em>
        [/#if]
        ${content.title!}
        [#if content.mandatory!false]
          <dfn title="Pflichtfeld">${model.requiredSymbol!}</dfn>
        [/#if]
      </span>
    </label>
  [/#if]

  <fieldset ${content.horizontal?string("class=\"mod\"", "")}>
    [#if content.legend?has_content]
      <legend>${content.legend}</legend>
    [/#if]

    [#if content.type?index_of("select") < 0 && content.labels?has_content]
      [#assign formItems = cmsfn.decode(content).labels?split("(\r\n|\r|\n|\x0085|\x2028|\x2029)", "rm")]
      [#if formItems?size > 1 && content.type == "checkbox"]
        [#assign renderedRequiredValue = ""]
      [#else]
        [#assign renderedRequiredValue = requiredAttribute!]
      [/#if]

      [#list formItems as label]
        [#assign checked = ""]
        [#assign data = label?split(":")]
        [#if model.value == data[1]!data[0]]
          [#assign checked = "checked=\"checked\""]
        [/#if]

        <div class="form-item">
          <input ${renderedRequiredValue!}
                 type="${content.type}"
                 id="${(content.controlName!'')}_${label_index}"
                 name="${content.controlName!''}"
                 value="${(data[1]!data[0])!?html}"
                 ${checked!}
                 [#if !model.isValid()]aria-invalid="true"[/#if]>
          <label for="${(content.controlName!'')}_${label_index}">
            [#if (content.controlName!'') == "datenschutz"]
              [#assign privacyHref = ctx.contextPath + "/datenschutz"]
              [#assign privacyPage = cmsfn.contentByPath("/datenschutz", "website")!]
              [#if privacyPage?has_content]
                [#assign privacyHref = cmsfn.link(privacyPage)!privacyHref]
              [/#if]
              Ich habe die <a href="${privacyHref}">Datenschutzerklärung</a> zur Kenntnis genommen.
            [#else]
              ${data[0]!?html}
            [/#if]
          </label>
        </div>
      [/#list]

      <div id="checkbox-error" class="text error" style="display:none" role="alert">
        <ul>
          <li>${i18n['form.user.errorMessage.checkboxes']}</li>
        </ul>
      </div>
    [#else]
      <select ${requiredAttribute!}
              id="${content.controlName!''}"
              name="${content.controlName!''}"
              [#if !model.isValid()]aria-invalid="true"[/#if]
              ${content.multiple?string("multiple=\"multiple\"", "")}>
        [#if content.labels?has_content]
          [#list cmsfn.decode(content).labels?split("(\r\n|\r|\n|\x0085|\x2028|\x2029)", "rm") as label]
            [#assign selected = ""]
            [#assign data = label?split(":")]
            [#assign optionValue = data[1]!data[0]]
            [#assign showOption = commercial.offersEnabled || controlName != 'anliegen' || optionValue == '' || optionValue == 'anderes']
            [#if model.value == data[1]!data[0]]
              [#assign selected = "selected=\"selected\""]
            [/#if]
            [#if showOption]
            <option value="${(data[1]!data[0])!?html}" ${selected!}>${data[0]!?html}</option>
            [/#if]
          [/#list]
        [/#if]
      </select>
    [/#if]
  </fieldset>
</div>
