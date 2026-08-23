[#import "/meine-website/includes/macros/editMode.ftl" as editMode]
[#import "/meine-website/includes/macros/urls.ftl" as urls]

[#function resolveReference reference workspace]
  [#if !reference?has_content][#return ''][/#if]
  [#local item = cmsfn.contentById(reference?string, workspace)!]
  [#if !item?has_content][#local item = cmsfn.contentByPath(reference?string, workspace)!][/#if]
  [#return item]
[/#function]

[#function collectItems parent]
  [#local items = []]
  [#if parent?has_content]
    [#list cmsfn.children(parent)![] as child]
      [#if child.text?has_content || child.title?has_content || child.body?has_content]
        [#local items = items + [child]]
      [#else]
        [#local items = items + collectItems(child)]
      [/#if]
    [/#list]
  [/#if]
  [#return items]
[/#function]

[#function findChild parent name]
  [#if parent?has_content]
    [#list cmsfn.children(parent)![] as child]
      [#if child.@name == name][#return child][/#if]
    [/#list]
  [/#if]
  [#return '']
[/#function]

[#function cleanMechanismTitle title]
  [#return (title!'')?replace('^[0-9]+\\.\\s*', '', 'r')]
[/#function]

[#assign problem = resolveReference(content.problemReference!'', 'problems')]
[#if problem?has_content]
  [#assign symptoms = collectItems(findChild(problem, 'symptoms'))]
  [#assign hypotheses = collectItems(findChild(problem, 'hypotheses'))]
  [#assign diagnosisSection = '']
  [#assign mechanisms = []]
  [#list hypotheses as hypothesis]
    [#if hypothesis?index == 0 && hypotheses?size gt 1 && !(hypothesis.title!'')?matches('^[0-9]+\\..*')]
      [#assign diagnosisSection = hypothesis]
    [#else]
      [#assign mechanisms = mechanisms + [hypothesis]]
    [/#if]
  [/#list]
  [#assign diagnosisHeading = 'Was könnte hinter dem Muster stecken?']
  [#if problem.slug == 'langsame-entscheidungen']
    [#assign diagnosisHeading = 'Was könnte Entscheidungen tatsächlich verlangsamen?']
  [#elseif problem.slug == 'ai-decision-rights']
    [#assign diagnosisHeading = 'Was sollte vor einer Automatisierung geklärt sein?']
  [#elseif problem.slug == 'leadership-transition']
    [#assign diagnosisHeading = 'Was solltest du am Anfang verstehen?']
  [/#if]
  [#if diagnosisSection?has_content && diagnosisSection.title?has_content]
    [#assign diagnosisHeading = diagnosisSection.title]
  [/#if]
  [#assign questions = collectItems(findChild(problem, 'diagnosticQuestions'))]
  [#assign selfCheckSteps = collectItems(findChild(problem, 'selfCheckSteps'))]
  [#assign visibleSelfCheckSteps = []]
  [#list selfCheckSteps as step]
    [#assign duplicatedSelfCheck = selfCheckSteps?size == 1 && (step.title!'') == (problem.selfCheckTitle!'')]
    [#if !duplicatedSelfCheck][#assign visibleSelfCheckSteps = visibleSelfCheckSteps + [step]][/#if]
  [/#list]
  [#assign avoidActions = collectItems(findChild(problem, 'avoidActions'))]
  [#assign relatedProblems = []]
  [#if problem.relatedProblems?has_content]
    [#list problem.relatedProblems as reference]
      [#assign relatedProblem = resolveReference(reference, 'problems')]
      [#if relatedProblem?has_content][#assign relatedProblems = relatedProblems + [relatedProblem]][/#if]
    [/#list]
  [/#if]
  [#assign relatedInsights = []]
  [#if problem.relatedInsights?has_content]
    [#list problem.relatedInsights as reference]
      [#assign item = resolveReference(reference, 'insights')]
      [#if item?has_content][#assign relatedInsights = relatedInsights + [item]][/#if]
    [/#list]
  [/#if]
  [#assign relatedTools = []]
  [#if problem.relatedTools?has_content]
    [#list problem.relatedTools as reference]
      [#assign item = resolveReference(reference, 'tools')]
      [#if item?has_content][#assign relatedTools = relatedTools + [item]][/#if]
    [/#list]
  [/#if]
  [#assign relatedOffers = []]
  [#if problem.relatedOffers?has_content]
    [#list problem.relatedOffers as reference]
      [#assign item = resolveReference(reference, 'offers')]
      [#if item?has_content][#assign relatedOffers = relatedOffers + [item]][/#if]
    [/#list]
  [/#if]
  [#assign relatedCases = []]
  [#if problem.relatedCases?has_content]
    [#list problem.relatedCases as reference]
      [#assign item = resolveReference(reference, 'cases')]
      [#if item?has_content][#assign relatedCases = relatedCases + [item]][/#if]
    [/#list]
  [/#if]

  [@editMode.wrapContent]
    <article class="problem-detail">
      <header class="problem-hero">
        <div class="site-shell problem-hero__inner">
          <p class="eyebrow">Problemsituation</p>
          <h1>${problem.headline!problem.title}</h1>
          [#if problem.subheadline?has_content]<p class="problem-hero__lead">${problem.subheadline}</p>[/#if]
        </div>
      </header>

      [#if problem.intro?has_content || problem.situation?has_content || symptoms?has_content]
        <section class="content-section">
          <div class="site-shell prose">
            <h2>Die Situation</h2>
            [#if problem.intro?has_content]<div class="problem-situation__intro">${urls.normalizeRichText(cmsfn.decode(problem).intro)}</div>[/#if]
            [#if problem.situation?has_content]${urls.normalizeRichText(cmsfn.decode(problem).situation)}[/#if]
            [#if symptoms?has_content]
              <h3>Woran du das Muster erkennen kannst</h3>
              <ul class="problem-checklist">[#list symptoms as item]<li>${item.text}</li>[/#list]</ul>
            [/#if]
          </div>
        </section>
      [/#if]

      [#if problem.commonAssumption?has_content || problem.diagnosisIntro?has_content]
        <section class="content-section content-section--tinted">
          <div class="site-shell problem-diagnosis">
            <div class="problem-diagnosis__assumption prose">
              [#if problem.commonAssumption?has_content]
                <p class="eyebrow">Die naheliegende Erklärung</p>
                ${urls.normalizeRichText(cmsfn.decode(problem).commonAssumption)}
              [/#if]
            </div>
            <div class="problem-diagnosis__bridge prose">
              [#if problem.diagnosisIntro?has_content]
                <p class="eyebrow">Diagnose vor Eingriff</p>
                <h2>Bevor du eine Lösung auswählst</h2>
                ${urls.normalizeRichText(cmsfn.decode(problem).diagnosisIntro)}
              [/#if]
            </div>
          </div>
        </section>
      [/#if]

      [#if mechanisms?has_content]
        <section class="content-section">
          <div class="site-shell">
            <div class="prose problem-mechanisms__intro">
              <p class="eyebrow">Diagnoseperspektive</p>
              <h2>${diagnosisHeading}</h2>
              [#if diagnosisSection?has_content && diagnosisSection.body?has_content]${urls.normalizeRichText(cmsfn.decode(diagnosisSection).body)}[/#if]
            </div>
            <div class="problem-mechanisms">
              [#list mechanisms as hypothesis]
                <section class="problem-mechanism">
                  <span class="problem-mechanism__number">${hypothesis?index + 1}</span>
                  <div><h3>${cleanMechanismTitle(hypothesis.title)}</h3>${urls.normalizeRichText(cmsfn.decode(hypothesis).body)}</div>
                </section>
              [/#list]
            </div>
          </div>
        </section>
      [/#if]

      [#if questions?has_content]
        <section class="content-section content-section--tinted">
          <div class="site-shell prose">
            <p class="eyebrow">Selbstdiagnose</p>
            <h2>Fragen, mit denen du genauer hinschauen kannst</h2>
            <ol class="problem-questions">[#list questions as question]<li>${question.text}</li>[/#list]</ol>
          </div>
        </section>
      [/#if]

      [#if problem.selfCheckTitle?has_content || visibleSelfCheckSteps?has_content]
        <section class="content-section">
          <div class="site-shell problem-selfcheck">
            <div class="prose">
              <p class="eyebrow">Praktischer Check</p>
              <h2>${problem.selfCheckTitle!'Beobachten, bevor du veränderst'}</h2>
              [#if problem.selfCheckIntro?has_content]${urls.normalizeRichText(cmsfn.decode(problem).selfCheckIntro)}[/#if]
            </div>
            [#if visibleSelfCheckSteps?has_content]
              <div class="problem-selfcheck__steps">
                [#list visibleSelfCheckSteps as step]
                  <section>
                    <span class="problem-selfcheck__number" aria-hidden="true">${step?index + 1}</span>
                    <div><h3>${step.title}</h3>${urls.normalizeRichText(cmsfn.decode(step).body)}</div>
                  </section>
                [/#list]
              </div>
            [/#if]
          </div>
        </section>
      [/#if]

      [#if avoidActions?has_content || problem.smallestNextStep?has_content]
        <section class="content-section content-section--tinted">
          <div class="site-shell problem-actions">
            [#if avoidActions?has_content]
              <div class="problem-actions__avoid"><p class="eyebrow">Nicht vorschnell</p><h2>Noch nichts groß umbauen</h2><ul>[#list avoidActions as item]<li>${item.text}</li>[/#list]</ul></div>
            [/#if]
            [#if problem.smallestNextStep?has_content]
              <div class="problem-actions__next"><p class="eyebrow">Nächster Schritt</p><h2>Verändere zunächst so wenig wie möglich</h2>${urls.normalizeRichText(cmsfn.decode(problem).smallestNextStep)}</div>
            [/#if]
          </div>
        </section>
      [/#if]

      [#if problem.whenToGetHelp?has_content]
        <section class="content-section problem-help">
          <div class="site-shell prose"><p class="eyebrow">Externe Perspektive</p><h2>Wann Unterstützung sinnvoll sein kann</h2>${urls.normalizeRichText(cmsfn.decode(problem).whenToGetHelp)}</div>
        </section>
      [/#if]

      [#if relatedOffers?has_content || relatedTools?has_content || relatedInsights?has_content || relatedCases?has_content || relatedProblems?has_content]
        <section class="content-section problem-related" aria-labelledby="problem-related-title">
          <div class="site-shell">
            <div class="prose"><p class="eyebrow">Weiterdenken</p><h2 id="problem-related-title">Passende nächste Schritte und Inhalte</h2></div>
            <div class="problem-related__grid">
              [#list relatedTools as item]
                [#assign sourcePage = resolveReference(item.sourcePage!'', 'website')]
                [#if sourcePage?has_content]<a class="problem-related__card" href="${cmsfn.link(sourcePage)}"><span>Selbst prüfen</span><strong>${item.title}</strong><em>Tool öffnen →</em></a>[/#if]
              [/#list]
              [#list relatedInsights as item]
                [#assign sourcePage = resolveReference(item.sourcePage!'', 'website')]
                [#if sourcePage?has_content]<a class="problem-related__card" href="${cmsfn.link(sourcePage)}"><span>Insight</span><strong>${item.title}</strong><em>Weiterlesen →</em></a>[/#if]
              [/#list]
              [#list relatedCases as item]
                [#assign sourcePage = resolveReference(item.sourcePage!'', 'website')]
                [#if sourcePage?has_content]<a class="problem-related__card" href="${cmsfn.link(sourcePage)}"><span>Praxisfall</span><strong>${item.title}</strong><em>Praxisfall ansehen →</em></a>[/#if]
              [/#list]
              [#list relatedOffers as item]
                [#assign targetPage = resolveReference(item.targetPage!'', 'website')]
                [#if targetPage?has_content]<a class="problem-related__card problem-related__card--accent" href="${cmsfn.link(targetPage)}"><span>Unterstützung</span><strong>${item.title}</strong><em>Leistung ansehen →</em></a>[/#if]
              [/#list]
              [#list relatedProblems as item]
                <a class="problem-related__card" href="${ctx.contextPath}/probleme/${item.slug!item.@name}"><span>Verwandte Situation</span><strong>${item.headline!item.title}</strong><em>Situation prüfen →</em></a>
              [/#list]
            </div>
          </div>
        </section>
      [/#if]

      [#if problem.closingThought?has_content]
        <section class="problem-closing"><div class="site-shell prose"><p class="eyebrow">Ein Gedanke zum Mitnehmen</p>${urls.normalizeRichText(cmsfn.decode(problem).closingThought)}</div></section>
      [/#if]
    </article>
  [/@editMode.wrapContent]
[#elseif cmsfn.editMode]
  <section class="content-section"><div class="site-shell prose"><p>Bitte eine Problemsituation auswählen.</p></div></section>
[/#if]
