const contactForms = document.querySelectorAll(
  'form#situation-klaeren, form#clarity-session-start, form#angebot-anfragen'
);

contactForms.forEach((contactForm) => {
  const portalId = '148991650';
  const formId = '62041f9c-6c87-45b6-a277-87b6b198e192';
  const endpoint = `https://api.hsforms.com/submissions/v3/integration/submit/${portalId}/${formId}`;
  const honeypot = contactForm.elements.namedItem('website-url');

  if (honeypot instanceof HTMLElement) {
    honeypot.tabIndex = -1;
    honeypot.setAttribute('aria-hidden', 'true');
    honeypot.setAttribute('autocomplete', 'off');
    honeypot.closest('.formHoneypot')?.setAttribute('aria-hidden', 'true');
  }

  const concernLabels = {
    'clarity-session': 'ChOS Clarity Session',
    'decision-review': 'Decision Review',
    sparring: 'Zweiwöchiges Leadership- oder Product-Sparring',
    'executive-sparring': 'Monatliches Executive Sparring',
    'quick-diagnostic': 'ChOS Quick Diagnostic',
    'product-organisation-diagnostic': 'Product Organisation Diagnostic',
    'ai-operating-model-assessment': 'AI Operating Model Assessment',
    'ai-workflow-sprint': 'AI-enabled Workflow / Product Sprint',
    workshop: 'Workshop',
    'noch-unsicher': 'Noch unsicher',
    anderes: 'Anderes Anliegen'
  };

  const timelineLabels = {
    'so-bald-wie-moeglich': 'So bald wie möglich',
    '2-4-wochen': 'Innerhalb der nächsten 2 bis 4 Wochen',
    '1-3-monate': 'Innerhalb der nächsten 1 bis 3 Monate',
    spaeter: 'Später als in 3 Monaten'
  };

  const isQuoteRequestForm = contactForm.id === 'angebot-anfragen';
  if (isQuoteRequestForm) {
    const offerAliases = {
      'CHOS-CLARITY-001': 'clarity-session',
      'CHOS-REVIEW-001': 'decision-review',
      'CHOS-SPARRING-2W-001': 'sparring',
      'CHOS-EXEC-001': 'executive-sparring',
      'CHOS-QUICK-DIAG-001': 'quick-diagnostic',
      'CHOS-ORG-DIAG-001': 'product-organisation-diagnostic',
      'CHOS-AI-ASSESS-001': 'ai-operating-model-assessment',
      'CHOS-AI-WORKFLOW-001': 'ai-workflow-sprint',
      'CHOS-WORKSHOP-DAY-001': 'workshop'
    };
    const requestedOfferParameter = new URLSearchParams(window.location.search).get('leistung');
    const requestedOffer = offerAliases[requestedOfferParameter] || requestedOfferParameter;
    const offerSelect = contactForm.elements.namedItem('leistung');
    if (
      requestedOffer &&
      offerSelect instanceof HTMLSelectElement &&
      Array.from(offerSelect.options).some((option) => option.value === requestedOffer)
    ) {
      offerSelect.value = requestedOffer;
    }
  }

  const setSubmitState = (isSubmitting) => {
    const submitButton = contactForm.querySelector('[type="submit"]');
    if (!submitButton) return;

    if (isSubmitting) {
      submitButton.dataset.originalText = submitButton.textContent;
      submitButton.textContent = 'Wird gesendet …';
      submitButton.disabled = true;
    } else {
      submitButton.textContent = submitButton.dataset.originalText || 'Anfrage senden';
      submitButton.disabled = false;
    }
  };

  const removeError = () => {
    contactForm.closest('.form-wrapper')?.previousElementSibling
      ?.matches('.hubspot-form-error') && contactForm.closest('.form-wrapper').previousElementSibling.remove();
  };

  const showError = () => {
    removeError();
    const error = document.createElement('div');
    error.className = 'text error hubspot-form-error';
    error.setAttribute('role', 'alert');
    error.innerHTML = '<p>Die Anfrage konnte gerade nicht übermittelt werden. Bitte versuche es erneut oder schreib direkt an <a href="mailto:kontakt@cleonhardt.de">kontakt@cleonhardt.de</a>.</p>';
    contactForm.closest('.form-wrapper')?.before(error);
    error.scrollIntoView({ behavior: 'smooth', block: 'center' });
  };

  const showSuccess = () => {
    const wrapper = contactForm.closest('.form-wrapper');
    if (!wrapper) return;

    const success = document.createElement('div');
    success.className = 'text success';
    success.setAttribute('role', 'status');
    success.setAttribute('tabindex', '-1');
    success.innerHTML = isQuoteRequestForm
      ? '<h2>Danke für deine Angebotsanfrage</h2><p>Deine Angaben wurden übermittelt. Ich prüfe die Anfrage persönlich und melde mich innerhalb von zwei Werktagen.</p>'
      : '<h2>Danke für deine Anfrage</h2><p>Deine Angaben wurden übermittelt. Ich melde mich persönlich bei dir.</p>';
    wrapper.replaceWith(success);
    success.focus();
    success.scrollIntoView({ behavior: 'smooth', block: 'center' });
  };

  contactForm.addEventListener('submit', async (event) => {
    event.preventDefault();
    removeError();

    const data = new FormData(contactForm);
    if (String(data.get('website-url') || '').trim()) {
      showSuccess();
      return;
    }

    const fullName = String(data.get('name') || '').trim();
    const nameParts = fullName.split(/\s+/).filter(Boolean);
    const firstName = nameParts.shift() || fullName;
    const lastName = nameParts.join(' ');
    const isClaritySessionForm = contactForm.id === 'clarity-session-start';
    const concernValue = isClaritySessionForm
      ? 'clarity-session'
      : String(data.get(isQuoteRequestForm ? 'leistung' : 'anliegen') || '');
    const concern = concernLabels[concernValue] || concernValue;
    const websiteRequestParts = [
      `${isQuoteRequestForm ? 'Angebotsanfrage' : 'Anliegen'}: ${concern}`,
      '',
      'Aktuelle Situation:',
      String(data.get('situation') || '').trim()
    ];
    const desiredResult = String(data.get('ziel') || '').trim();
    if (desiredResult) {
      websiteRequestParts.push('', 'Gewünschtes Ergebnis:', desiredResult);
    }
    const timelineValue = String(data.get('zeitraum') || '').trim();
    if (timelineValue) {
      websiteRequestParts.push('', `Gewünschter Zeitraum: ${timelineLabels[timelineValue] || timelineValue}`);
    }
    const participants = String(data.get('beteiligte') || '').trim();
    if (participants) {
      websiteRequestParts.push(`Voraussichtlich Beteiligte: ${participants}`);
    }
    const attribution = window.cleonhardtAttribution || {};
    const attributionParts = [];
    if (attribution.utm_source) attributionParts.push(`Quelle: ${attribution.utm_source}`);
    if (attribution.utm_medium) attributionParts.push(`Medium: ${attribution.utm_medium}`);
    if (attribution.utm_campaign) attributionParts.push(`Kampagne: ${attribution.utm_campaign}`);
    attributionParts.push(`Anfrageseite: ${window.location.pathname}`);
    if (attributionParts.length > 0) {
      websiteRequestParts.push('', 'Herkunft:', ...attributionParts);
    }
    websiteRequestParts.push('', `Datenschutz: ${String(data.get('datenschutz') || 'nicht bestätigt')}`);
    const websiteRequest = websiteRequestParts.join('\n');

    const fields = [
      { objectTypeId: '0-1', name: 'firstname', value: firstName },
      { objectTypeId: '0-1', name: 'email', value: String(data.get('email') || '').trim() },
      { objectTypeId: '0-1', name: 'hs_lead_status', value: 'NEW' },
      { objectTypeId: '0-1', name: 'websiteanfrage', value: websiteRequest }
    ];

    if (lastName) {
      fields.push({ objectTypeId: '0-1', name: 'lastname', value: lastName });
    }

    const company = String(data.get('unternehmen') || '').trim();
    if (company) {
      fields.push({ objectTypeId: '0-1', name: 'company', value: company });
    }

    const jobTitle = String(data.get('rolle') || '').trim();
    if (jobTitle) {
      fields.push({ objectTypeId: '0-1', name: 'jobtitle', value: jobTitle });
    }

    const phone = String(data.get('telefon') || '').trim();
    if (phone) {
      fields.push({ objectTypeId: '0-1', name: 'phone', value: phone });
    }

    const currentUrl = new URL(window.location.href);
    const safePageUrl = new URL(window.location.origin + window.location.pathname);
    for (const key of ['utm_source', 'utm_medium', 'utm_campaign', 'leistung']) {
      const value = currentUrl.searchParams.get(key);
      if (value) safePageUrl.searchParams.set(key, value.slice(0, 120));
    }
    const context = {
      pageUri: safePageUrl.href,
      pageName: document.title
    };

    setSubmitState(true);

    try {
      const response = await fetch(endpoint, {
        method: 'POST',
        credentials: 'omit',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          submittedAt: String(Date.now()),
          fields,
          context
        })
      });

      if (!response.ok) throw new Error(`HubSpot submission failed: ${response.status}`);
      showSuccess();
    } catch (error) {
      console.error('HubSpot form submission failed.', error);
      setSubmitState(false);
      showError();
    }
  });
});
