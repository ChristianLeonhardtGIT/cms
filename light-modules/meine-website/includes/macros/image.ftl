[#function hasImage imageId=""]
    [#return imageId?has_content && damfn.getAssetLink(imageId)?has_content]
[/#function]

[#macro renderImage imageId altText=""]
    <img
            src="${damfn.getAssetLink(imageId)!}"
            [#if altText?has_content]alt="${altText}"[/#if]
            loading="lazy"
    />
[/#macro]

[#macro renderImageWithClass imageId altText="" class=""]
    <img
            src="${damfn.getAssetLink(imageId)!}"
            [#if altText?has_content]alt="${altText}"[/#if]
            [#if class?has_content]class="${class}"[/#if]
            loading="lazy"
    />
[/#macro]

[#macro renderExternalImageWithClass imageLink altText="" class=""]
    <img
            src="${imageLink}"
            [#if altText?has_content]alt="${altText}"[/#if]
            [#if class?has_content]class="${class}"[/#if]
            loading="lazy"
    />
[/#macro]