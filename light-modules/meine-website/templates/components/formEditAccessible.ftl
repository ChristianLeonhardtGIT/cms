[#assign controlName = content.controlName!""]
[#assign autocompleteToken = "off"]
[#if controlName == "name"]
  [#assign autocompleteToken = "name"]
[#elseif controlName == "email"]
  [#assign autocompleteToken = "email"]
[#elseif controlName == "rolle"]
  [#assign autocompleteToken = "organization-title"]
[#elseif controlName == "unternehmen"]
  [#assign autocompleteToken = "organization"]
[#elseif controlName == "telefon"]
  [#assign autocompleteToken = "tel"]
[#elseif content.autocomplete!false]
  [#assign autocompleteToken = "on"]
[/#if]

<div ${model.style!}>
  [#if content.title?has_content]
    <label id="${controlName}_label" for="${controlName}">
      <span>
        [#if !model.isValid()]
          <em>${i18n['form.error.field']}</em>
        [/#if]
        ${content.title}
        [#if content.mandatory!false]
          <dfn title="Pflichtfeld">${model.requiredSymbol!}</dfn>
        [/#if]
      </span>
    </label>
  [/#if]

  [#assign attributes]
    name="${controlName}"
    id="${controlName}"
    autocomplete="${autocompleteToken}"
    [#if content.maxLength?has_content] maxlength="${content.maxLength}"[/#if]
    [#if content.placeholder?has_content] placeholder="${content.placeholder}"[/#if]
    [#if content.min?has_content] min="${content.min}"[/#if]
    [#if content.max?has_content] max="${content.max}"[/#if]
    [#if content.step?has_content] step="${content.step}"[/#if]
    [#if content.patternDescription?has_content] title="${content.patternDescription!}"[/#if]
    [#if content.description?has_content] aria-describedby="${controlName}-description"[/#if]
    [#if !model.isValid()] aria-invalid="true"[/#if]
    [#if content.readonly!false] readonly[/#if]
    [#if content.disabled!false] disabled[/#if]
    [#if content.mandatory!false] required[/#if]
    [#if content.autofocus!false] autofocus[/#if]
  [/#assign]

  [#if content.rows?default(1) == 1]
    <input ${attributes} type="${content.inputType!"text"}" value="${model.value!}">
  [#else]
    <textarea ${attributes}[#if content.rows?has_content] rows="${content.rows}"[/#if]>${model.value!}</textarea>
  [/#if]

  [#if content.description?has_content]
    <span id="${controlName}-description">${content.description}</span>
  [/#if]
</div>
