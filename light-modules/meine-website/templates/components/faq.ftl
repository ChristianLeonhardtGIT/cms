[#import "/meine-website/includes/macros/editMode.ftl" as editMode]
[#assign faqItems = []]
[#if content.items?has_content]
  [#list cmsfn.children(content.items)![] as item]
    [#if item.question?has_content && item.answer?has_content]
      [#assign faqItems = faqItems + [item]]
    [/#if]
  [/#list]
[/#if]

[@editMode.wrapContent]
  [#if faqItems?has_content || cmsfn.editMode]
    <section class="faq-section" aria-labelledby="faq-${content.@id}">
      <div class="site-shell faq-section__inner">
        <header class="faq-section__header">
          [#if content.eyebrow?has_content]<p class="eyebrow">${content.eyebrow}</p>[/#if]
          <h2 id="faq-${content.@id}">${content.heading!'Häufige Fragen'}</h2>
          [#if content.introduction?has_content]<p class="faq-section__intro">${content.introduction}</p>[/#if]
        </header>
        [#if faqItems?has_content]
          <div class="faq-list">
            [#list faqItems as item]
              <details class="faq-item">
                <summary>${item.question}</summary>
                <div class="faq-item__answer"><p>${item.answer?replace('\n', '<br>')}</p></div>
              </details>
            [/#list]
          </div>
          <script type="application/ld+json">
          {
            "@context": "https://schema.org",
            "@type": "FAQPage",
            "mainEntity": [
              [#list faqItems as item]
              {
                "@type": "Question",
                "name": "${item.question?string?json_string}",
                "acceptedAnswer": {
                  "@type": "Answer",
                  "text": "${item.answer?string?json_string}"
                }
              }[#sep],[/#sep]
              [/#list]
            ]
          }
          </script>
        [#elseif cmsfn.editMode]
          <p class="faq-section__empty">Im Dialog mindestens eine Frage und Antwort ergänzen.</p>
        [/#if]
      </div>
    </section>
  [/#if]
[/@editMode.wrapContent]

