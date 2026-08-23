const chosCheck = document.querySelector('[data-chos-check]');

if (chosCheck) {
  const form = chosCheck.querySelector('form');
  const result = chosCheck.querySelector('[data-chos-result]');
  const summary = chosCheck.querySelector('[data-chos-summary]');
  const dimensionList = chosCheck.querySelector('[data-chos-dimensions]');
  const resultCta = chosCheck.querySelector('[data-chos-result-cta]');
  let latestResult = null;

  const dimensions = {
    direction: {
      label: 'Richtung und Strategie',
      advice: 'Prüfe, ob Zielbild, Prioritäten und Erfolgskriterien wirklich dieselbe Richtung erzeugen.'
    },
    responsibility: {
      label: 'Rollen und Verantwortung',
      advice: 'Betrachte konkrete Entscheidungen, Mandate und Schnittstellen – nicht nur Rollenbeschreibungen.'
    },
    decisions: {
      label: 'Entscheidungen und Priorisierung',
      advice: 'Mach sichtbar, wer Entscheidungen treffen und wer sie später wieder öffnen kann.'
    },
    collaboration: {
      label: 'Zusammenarbeit und Abhängigkeiten',
      advice: 'Unterscheide notwendige fachliche Abhängigkeiten von historisch gewachsenen Abstimmungsschleifen.'
    },
    leadership: {
      label: 'Führung und Kommunikation',
      advice: 'Prüfe, welche Führungsroutinen gewünschtes Verhalten stärken oder unbeabsichtigt verhindern.'
    }
  };

  const levelFor = (score) => {
    if (score >= 8) return { label: 'deutlicher Klärungsbedarf', className: 'is-high' };
    if (score >= 4) return { label: 'beobachtbare Spannungen', className: 'is-medium' };
    return { label: 'wenige deutliche Signale', className: 'is-low' };
  };

  const pdfButton = document.createElement('button');
  pdfButton.className = 'button button--secondary chos-check__pdf';
  pdfButton.type = 'button';
  pdfButton.textContent = 'Auswertung als PDF speichern';

  if (resultCta) {
    const resultActions = document.createElement('div');
    resultActions.className = 'chos-check__result-actions';
    resultCta.before(resultActions);
    resultActions.append(pdfButton, resultCta);
  }

  const loadImageData = (url) => new Promise((resolve, reject) => {
    const image = new Image();
    image.onload = () => {
      const canvas = document.createElement('canvas');
      canvas.width = image.naturalWidth;
      canvas.height = image.naturalHeight;
      canvas.getContext('2d').drawImage(image, 0, 0);
      resolve(canvas.toDataURL('image/png'));
    };
    image.onerror = reject;
    image.src = url;
  });

  const loadFontData = async (url) => {
    const response = await fetch(url);
    if (!response.ok) throw new Error(`Font could not be loaded: ${response.status}`);
    const bytes = new Uint8Array(await response.arrayBuffer());
    let binary = '';
    for (let offset = 0; offset < bytes.length; offset += 8192) {
      binary += String.fromCharCode(...bytes.subarray(offset, offset + 8192));
    }
    return window.btoa(binary);
  };

  pdfButton.addEventListener('click', async () => {
    if (!latestResult) return;
    if (!window.jspdf?.jsPDF) {
      window.alert('Der PDF-Export konnte nicht geladen werden. Bitte lade die Seite neu und versuche es erneut.');
      return;
    }
    pdfButton.disabled = true;
    const previousLabel = pdfButton.textContent;
    pdfButton.textContent = 'PDF wird erstellt …';

    const { jsPDF } = window.jspdf;
    const { ranked, focus, focusLevel } = latestResult;
    const brand = [13, 75, 49];
    const green = [22, 99, 66];
    const ink = [23, 35, 29];
    const muted = [99, 112, 105];
    const line = [220, 228, 222];
    const accent = [241, 249, 210];
    const doc = new jsPDF({ orientation: 'portrait', unit: 'mm', format: 'a4', compress: true });
    const margin = 16;
    const pageWidth = 210;
    const contentWidth = pageWidth - (margin * 2);
    const date = new Intl.DateTimeFormat('de-DE', { dateStyle: 'long' }).format(new Date());

    try {
      const fontBase = `${window.location.origin}/.resources/meine-website/webresources/fonts`;
      const [fontRegular, fontBold] = await Promise.all([
        loadFontData(`${fontBase}/DejaVuSans.ttf`),
        loadFontData(`${fontBase}/DejaVuSans-Bold.ttf`)
      ]);
      doc.addFileToVFS('DejaVuSans.ttf', fontRegular);
      doc.addFont('DejaVuSans.ttf', 'ChOS', 'normal');
      doc.addFileToVFS('DejaVuSans-Bold.ttf', fontBold);
      doc.addFont('DejaVuSans-Bold.ttf', 'ChOS', 'bold');
      const logoUrl = `${window.location.origin}/.resources/meine-website/webresources/images/christian-leonhardt-chos-signet-invoice.png`;
      const logoData = await loadImageData(logoUrl);
      doc.addImage(logoData, 'PNG', margin, 12, 17, 17);

      doc.setDrawColor(...line);
      doc.setLineWidth(0.25);
      doc.line(margin, 34, pageWidth - margin, 34);
      doc.setFont('ChOS', 'normal');
      doc.setFontSize(8.5);
      doc.setTextColor(...muted);
      doc.text(`ChOS Selbstcheck · ${date}`, pageWidth - margin, 22, { align: 'right' });

      doc.setTextColor(...brand);
      doc.setFont('ChOS', 'bold');
      doc.setFontSize(26);
      doc.text('Deine ChOS-Einordnung', margin, 49);
      doc.setTextColor(...muted);
      doc.setFont('ChOS', 'normal');
      doc.setFontSize(10.5);
      doc.text('Eine erste strukturierte Sicht auf deine Organisation - entlang der fünf ChOS-Perspektiven.', margin, 57);

      doc.setFillColor(...accent);
      doc.setDrawColor(184, 206, 131);
      doc.roundedRect(margin, 65, contentWidth, 34, 4, 4, 'FD');
      doc.setTextColor(...green);
      doc.setFont('ChOS', 'bold');
      doc.setFontSize(8.5);
      doc.text('ERSTER FOKUS', margin + 6, 74);
      doc.setTextColor(...brand);
      doc.setFontSize(16);
      doc.text(focus.label, margin + 6, 83);
      doc.setTextColor(...ink);
      doc.setFontSize(9.5);
      const focusText = `${focusLevel.label}. ${focus.advice}`;
      doc.text(doc.splitTextToSize(focusText, contentWidth - 12), margin + 6, 91, { lineHeightFactor: 1.25 });

      doc.setTextColor(...brand);
      doc.setFontSize(14);
      doc.text('Die fünf Perspektiven im Überblick', margin, 111);

      let y = 117;
      ranked.forEach(([key, score], index) => {
        const dimension = dimensions[key];
        const level = levelFor(score);
        const rowHeight = 25;
        doc.setFillColor(index === 0 ? 248 : 255, index === 0 ? 251 : 255, index === 0 ? 237 : 255);
        doc.setDrawColor(...line);
        doc.roundedRect(margin, y, contentWidth, rowHeight, 3, 3, 'FD');
        doc.setFillColor(...green);
        doc.roundedRect(margin, y, 2, rowHeight, 1, 1, 'F');
        doc.setTextColor(...brand);
        doc.setFont('ChOS', 'bold');
        doc.setFontSize(10.5);
        doc.text(dimension.label, margin + 7, y + 8);
        doc.setTextColor(...muted);
        doc.setFont('ChOS', 'normal');
        doc.setFontSize(8.2);
        doc.text(doc.splitTextToSize(dimension.advice, 123), margin + 7, y + 14, { lineHeightFactor: 1.25 });
        doc.setTextColor(...brand);
        doc.setFont('ChOS', 'bold');
        doc.setFontSize(10);
        doc.text(`${score} von 12`, pageWidth - margin - 6, y + 8, { align: 'right' });
        doc.setTextColor(...muted);
        doc.setFont('ChOS', 'normal');
        doc.setFontSize(7.8);
        doc.text(level.label, pageWidth - margin - 6, y + 14, { align: 'right' });
        y += rowHeight + 3;
      });

      doc.setFillColor(...green);
      doc.rect(margin, 260, 1.3, 13, 'F');
      doc.setTextColor(...muted);
      doc.setFont('ChOS', 'normal');
      doc.setFontSize(8);
      const note = 'Diese Auswertung dient der Selbstreflexion und ist keine Organisationsdiagnose. Entscheidend sind Kontext, konkrete Beispiele und alternative Erklärungen.';
      doc.text(doc.splitTextToSize(note, contentWidth - 7), margin + 5, 264, { lineHeightFactor: 1.3 });

      doc.setDrawColor(...line);
      doc.line(margin, 282, pageWidth - margin, 282);
      doc.setTextColor(...muted);
      doc.setFontSize(7.5);
      doc.text(`© ${new Date().getFullYear()} Christian Leonhardt`, margin, 288);
      const siteX = 96;
      doc.textWithLink('cleonhardt.de', siteX, 288, { url: 'https://cleonhardt.de/' });
      doc.text('·', 119, 288);
      doc.textWithLink('kontakt@cleonhardt.de', 123, 288, { url: 'mailto:kontakt@cleonhardt.de' });
      doc.save('Deine ChOS-Einordnung.pdf');
    } catch (error) {
      console.error('PDF export failed', error);
      window.alert('Die PDF konnte nicht erstellt werden. Bitte versuche es erneut.');
    } finally {
      pdfButton.disabled = false;
      pdfButton.textContent = previousLabel;
    }
  });

  form?.addEventListener('submit', (event) => {
    event.preventDefault();
    const data = new FormData(form);
    const scores = Object.fromEntries(Object.keys(dimensions).map((key) => [key, 0]));

    form.querySelectorAll('[data-question]').forEach((question) => {
      const dimension = question.dataset.dimension;
      const selected = data.get(question.dataset.question);
      scores[dimension] += Number(selected || 0);
    });

    const ranked = Object.entries(scores).sort((a, b) => b[1] - a[1]);
    const [focusKey, focusScore] = ranked[0];
    const focus = dimensions[focusKey];
    const focusLevel = levelFor(focusScore);
    latestResult = { ranked, focus, focusLevel };

    summary.innerHTML = '';
    const summaryHeading = document.createElement('h3');
    summaryHeading.textContent = `Erster Fokus: ${focus.label}`;
    const summaryText = document.createElement('p');
    summaryText.textContent = `${focusLevel.label}. ${focus.advice}`;
    const disclaimer = document.createElement('p');
    disclaimer.className = 'chos-check__disclaimer';
    disclaimer.textContent = 'Das Ergebnis ist eine Selbstreflexion und keine Organisationsdiagnose. Entscheidend sind Kontext, konkrete Beispiele und alternative Erklärungen.';
    summary.append(summaryHeading, summaryText, disclaimer);

    dimensionList.innerHTML = '';
    for (const [key, score] of ranked) {
      const dimension = dimensions[key];
      const level = levelFor(score);
      const item = document.createElement('li');
      item.className = `chos-check__result-item ${level.className}`;
      const label = document.createElement('span');
      label.textContent = dimension.label;
      const value = document.createElement('strong');
      value.textContent = `${score} von 12 · ${level.label}`;
      item.append(label, value);
      dimensionList.append(item);
    }

    const ctaUrl = new URL('/kontakt', window.location.origin);
    ctaUrl.searchParams.set('utm_source', 'chos-selbstcheck');
    ctaUrl.searchParams.set('utm_medium', 'website-tool');
    ctaUrl.searchParams.set('utm_campaign', focusKey);
    resultCta.href = ctaUrl.href;

    result.hidden = false;
    result.focus();
    result.scrollIntoView({ behavior: 'smooth', block: 'start' });
  });

  form?.addEventListener('reset', () => {
    latestResult = null;
    result.hidden = true;
    summary.innerHTML = '';
    dimensionList.innerHTML = '';
  });
}
