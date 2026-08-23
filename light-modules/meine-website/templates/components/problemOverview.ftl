[#import "/meine-website/includes/macros/editMode.ftl" as editMode]
[#import "/meine-website/includes/macros/urls.ftl" as urls]

[#assign problemRoot = cmsfn.contentByPath('/cleonhardt', 'problems')!]
[#assign problems = []]
[#if problemRoot?has_content]
  [#list cmsfn.children(problemRoot)![] as problem]
    [#assign publishing = '']
    [#list cmsfn.children(problem)![] as child]
      [#if child.@name == 'publishing'][#assign publishing = child][/#if]
    [/#list]
    [#if !publishing?has_content || (publishing.active!true)]
      [#assign problems = problems + [problem]]
    [/#if]
  [/#list]
[/#if]

[@editMode.wrapContent]
  <section class="content-section problem-overview" aria-labelledby="problem-overview-title">
    <div class="site-shell">
      <div class="problem-overview__header prose">
        <p class="eyebrow">Typische Situationen</p>
        <h1 class="problem-overview__title" id="problem-overview-title">${content.heading!'Womit beschäftigst du dich gerade?'}</h1>
        [#if content.intro?has_content]
          <div class="section-intro">${urls.normalizeRichText(cmsfn.decode(content).intro)}</div>
        [/#if]
      </div>
      [#if problems?has_content]
        <div class="problem-grid">
          [#list problems as problem]
            <article class="problem-card">
              <p class="mvp-meta">Problemsituation ${problem?index + 1}</p>
              <h3>${problem.headline!problem.title!problem.@name}</h3>
              [#if problem.subheadline?has_content]<p>${problem.subheadline}</p>[/#if]
              <a class="problem-card__link" href="${ctx.contextPath}/probleme/${problem.slug!problem.@name}" aria-label="Situation prüfen: ${problem.headline!problem.title!problem.@name}">Situation prüfen <span aria-hidden="true">→</span></a>
            </article>
          [/#list]
        </div>
      [#else]
        <p>Aktuell sind noch keine Problemsituationen veröffentlicht.</p>
      [/#if]
    </div>
  </section>
[/@editMode.wrapContent]
