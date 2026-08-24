const DOCUMENT_PATH_PATTERN = /^docs\/[a-z0-9._/-]+\.html$/i;

const PROFILES = [
  {
    path: 'docs/pb-001-team-uebernehmen.html',
    signals: ['team übernehmen', 'neues team', 'produktteam', 'teamleitung', 'verantwortungsbereich', 'neue rolle', 'onboarding'],
    reason: 'Hilft beim strukturierten Einstieg in ein neues Team.'
  },
  {
    path: 'docs/28-fuehrung-erste-sechs-monate.html',
    signals: ['führungskraft', 'führung', 'erste monate', 'neue rolle', 'verantwortungsbereich', 'übernehme', 'einarbeitung'],
    reason: 'Ordnet die ersten Schritte in neuer Führungsverantwortung.'
  },
  {
    path: 'docs/05-entscheidungsmodell.html',
    signals: ['entscheidung', 'entscheiden', 'entscheidungswege', 'entscheidungsraum', 'vertagt', 'unklar wer entscheidet', 'entscheidungskompetenz'],
    reason: 'Vertieft nachvollziehbare Entscheidungen unter Unsicherheit.'
  },
  {
    path: 'docs/05-entscheidungsrahmen.html',
    signals: ['entscheidung', 'entscheiden', 'optionen', 'risiko', 'abwägung', 'entscheidungsrahmen'],
    reason: 'Bietet einen direkt anwendbaren Rahmen für eine konkrete Entscheidung.'
  },
  {
    path: 'docs/01-denkraum.html',
    signals: ['zusammenarbeit', 'kommunikation', 'missverständnis', 'konflikt', 'perspektive', 'kontext', 'gemeinsames verständnis'],
    reason: 'Schafft eine gemeinsame Sprache für unterschiedliche Perspektiven.'
  },
  {
    path: 'docs/03-erkenntnismodell.html',
    signals: ['annahme', 'unsicherheit', 'hypothese', 'beobachtung', 'wissen nicht', 'unklar', 'lernen'],
    reason: 'Hilft, Beobachtung, Annahme und offene Frage sauber zu trennen.'
  },
  {
    path: 'docs/17-wirkungsmodell-und-diagnose.html',
    signals: ['diagnose', 'ursache', 'wirkung', 'symptom', 'problem', 'muster', 'wechselwirkung'],
    reason: 'Vertieft die Diagnose von Ursachen, Wirkungen und Mustern.'
  },
  {
    path: 'docs/01-diagnosebogen.html',
    signals: ['diagnose', 'situation verstehen', 'problem', 'beobachtung', 'annahme', 'offene frage'],
    reason: 'Liefert einen kompakten Leitfaden für die aktuelle Diagnose.'
  },
  {
    path: 'docs/14-gestaltungsprinzipien.html',
    signals: ['struktur', 'prozess', 'regel', 'gestaltung', 'organisation', 'arbeitsweise', 'intervention'],
    reason: 'Verbindet die Diagnose mit tragfähigen Gestaltungsprinzipien.'
  },
  {
    path: 'docs/15-ziele-feedback-und-entwicklung.html',
    signals: ['ziel', 'feedback', 'entwicklung', 'leistung', 'lernen', 'mitarbeitergespräch'],
    reason: 'Ordnet Ziele, Feedback und Entwicklung als zusammenhängenden Lernprozess.'
  },
  {
    path: 'docs/pb-002-mitarbeiterentwicklung.html',
    signals: ['mitarbeiter', 'entwicklungsgespräch', 'feedback', 'entwicklung', 'leistung', 'förderung'],
    reason: 'Unterstützt die konkrete Arbeit an Mitarbeiterentwicklung.'
  },
  {
    path: 'docs/25-strategie-und-anwendung.html',
    signals: ['strategie', 'markt', 'positionierung', 'fokus', 'ausrichtung', 'priorität'],
    reason: 'Verknüpft strategische Ausrichtung mit konkreten Entscheidungen.'
  },
  {
    path: 'docs/27-strategische-priorisierung.html',
    signals: ['priorisierung', 'priorität', 'roadmap', 'portfolio', 'initiative', 'epic', 'ressourcen'],
    reason: 'Hilft, Beiträge und Belastungen bei Prioritäten sichtbar zu machen.'
  },
  {
    path: 'docs/26-wirtschaftliche-systemsteuerung.html',
    signals: ['controlling', 'kennzahl', 'wirtschaftlich', 'steuerung', 'budget', 'kosten', 'ertrag'],
    reason: 'Ordnet wirtschaftliche Signale in die Systemsteuerung ein.'
  },
  {
    path: 'docs/18-wirtschaftlichkeit.html',
    signals: ['wirtschaftlichkeit', 'kosten', 'nutzen', 'aufwand', 'budget', 'investition'],
    reason: 'Unterstützt eine nachvollziehbare Nutzen-Aufwand-Prüfung.'
  },
  {
    path: 'docs/21-ethik-und-zielkonflikte.html',
    signals: ['ethik', 'zielkonflikt', 'werte', 'schaden', 'dilemma', 'verantwortung'],
    reason: 'Macht ethische Grenzen und Zielkonflikte explizit.'
  },
  {
    path: 'docs/13-menschenbild.html',
    signals: ['menschenbild', 'motivation', 'verhalten', 'autonomie', 'vertrauen', 'kontrolle'],
    reason: 'Hilft, Verhalten nicht vorschnell von seinen Bedingungen zu trennen.'
  },
  {
    path: 'docs/03-clarity-session.html',
    signals: ['klärung', 'klarheit', 'workshop', 'session', 'gemeinsam verstehen', 'nächster schritt'],
    reason: 'Bietet einen klaren Ablauf für eine gemeinsame Klärung.'
  }
];

const FALLBACK_PATHS = [
  'docs/17-wirkungsmodell-und-diagnose.html',
  'docs/03-erkenntnismodell.html',
  'docs/00-executive-summary.html'
];

const STOP_WORDS = new Set([
  'aber', 'alle', 'auch', 'dann', 'dass', 'deine', 'einem', 'einen', 'einer', 'eine', 'fuer', 'haben', 'hier',
  'immer', 'kann', 'kein', 'keine', 'machen', 'mehr', 'noch', 'oder', 'sich', 'sind', 'sein', 'sehr', 'thema',
  'ueber', 'unser', 'unsere', 'unter', 'wenn', 'werden', 'wird', 'worum'
]);

function decodeEntities(value) {
  return String(value)
    .replaceAll('&amp;', '&')
    .replaceAll('&lt;', '<')
    .replaceAll('&gt;', '>')
    .replaceAll('&quot;', '"')
    .replaceAll('&#039;', "'")
    .replaceAll('&nbsp;', ' ');
}

function plainText(value) {
  return decodeEntities(String(value).replace(/<[^>]*>/g, ' ')).replace(/\s+/g, ' ').trim();
}

function normalize(value) {
  return plainText(value)
    .toLocaleLowerCase('de-DE')
    .replaceAll('ß', 'ss')
    .normalize('NFKD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[^a-z0-9]+/g, ' ')
    .replace(/\s+/g, ' ')
    .trim();
}

function tokens(value) {
  return new Set(normalize(value).split(' ').filter((token) => token.length > 2 && !STOP_WORDS.has(token)));
}

export function buildKnowledgeIndexFromHtml(html) {
  const entries = [];
  const cards = /<a\s+class="document-card"\s+href="([^"]+)">[\s\S]*?<span\s+class="card-group">([\s\S]*?)<\/span>[\s\S]*?<h2>([\s\S]*?)<\/h2>[\s\S]*?<p>([\s\S]*?)<\/p>[\s\S]*?<\/a>/gi;
  for (const match of String(html).matchAll(cards)) {
    const path = match[1];
    if (!DOCUMENT_PATH_PATTERN.test(path)) continue;
    entries.push(Object.freeze({
      path,
      group: plainText(match[2]),
      title: plainText(match[3]),
      excerpt: plainText(match[4]).slice(0, 360)
    }));
  }
  return entries;
}

export function validateKnowledgeIndex(value) {
  if (!value || value.schemaVersion !== 1 || typeof value.version !== 'string' || !Array.isArray(value.entries)) {
    throw new Error('Der ChOS-Wissensindex ist ungültig.');
  }
  if (value.entries.length > 500) throw new Error('Der ChOS-Wissensindex ist zu groß.');
  const entries = value.entries.map((entry) => {
    if (!entry || !DOCUMENT_PATH_PATTERN.test(entry.path) || typeof entry.title !== 'string' || !entry.title || typeof entry.excerpt !== 'string' || typeof entry.group !== 'string') {
      throw new Error('Der ChOS-Wissensindex enthält einen ungültigen Eintrag.');
    }
    return {
      path: entry.path,
      title: String(entry.title).slice(0, 240),
      group: String(entry.group).slice(0, 120),
      excerpt: String(entry.excerpt).slice(0, 360)
    };
  });
  return { schemaVersion: 1, version: value.version, generatedAt: value.generatedAt || null, entries };
}

export function rankRelatedContent(index, context, limit = 3) {
  const valid = validateKnowledgeIndex(index);
  const contextItems = Array.isArray(context?.items) ? context.items : [];
  const source = [context?.title, context?.context, ...contextItems.map((item) => item?.text)].filter(Boolean).join(' ');
  const normalizedSource = normalize(source);
  const sourceTokens = tokens(source);
  const byPath = new Map(valid.entries.map((entry) => [entry.path, entry]));
  const ranked = [];

  for (const profile of PROFILES) {
    const entry = byPath.get(profile.path);
    if (!entry) continue;
    const matchedSignals = profile.signals.filter((signal) => normalizedSource.includes(normalize(signal)));
    const titleMatches = [...tokens(entry.title)].filter((token) => sourceTokens.has(token)).length;
    const excerptMatches = [...tokens(entry.excerpt)].filter((token) => sourceTokens.has(token)).length;
    const score = matchedSignals.reduce((sum, signal) => sum + (normalize(signal).includes(' ') ? 9 : 5), 0)
      + titleMatches * 3
      + Math.min(excerptMatches, 4);
    if (score > 0) ranked.push({ ...entry, reason: profile.reason, score });
  }

  ranked.sort((left, right) => right.score - left.score || left.title.localeCompare(right.title, 'de'));
  if (ranked.length >= limit) return ranked.slice(0, limit);

  const used = new Set(ranked.map((entry) => entry.path));
  for (const path of FALLBACK_PATHS) {
    const entry = byPath.get(path);
    if (!entry || used.has(path)) continue;
    ranked.push({
      ...entry,
      reason: path.includes('erkenntnismodell')
        ? 'Hilft, Wissen, Annahmen und offene Fragen sauber zu trennen.'
        : path.includes('wirkungsmodell')
          ? 'Bietet eine belastbare Grundlage für die weitere Diagnose.'
          : 'Gibt einen kompakten Überblick über die ChOS-Arbeitsweise.',
      score: 0
    });
    used.add(path);
    if (ranked.length >= limit) break;
  }
  return ranked.slice(0, limit);
}
