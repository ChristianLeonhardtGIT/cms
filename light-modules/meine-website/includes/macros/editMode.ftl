[#macro wrapContent]
    [#if cmsfn.editMode]<div>[/#if]

    [#nested]

    [#if cmsfn.editMode]</div>[/#if]
[/#macro]