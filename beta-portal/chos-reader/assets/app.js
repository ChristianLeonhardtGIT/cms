(() => {
  const root = document.documentElement;
  const body = document.body;
  const storedTheme = localStorage.getItem('chos-theme');
  if (storedTheme) root.dataset.theme = storedTheme;

  document.querySelectorAll('[data-theme-toggle]').forEach((button) => {
    button.addEventListener('click', () => {
      const current = root.dataset.theme || (matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light');
      const next = current === 'dark' ? 'light' : 'dark';
      root.dataset.theme = next;
      localStorage.setItem('chos-theme', next);
    });
  });

  document.querySelectorAll('[data-menu-toggle]').forEach((button) => {
    button.addEventListener('click', () => body.classList.toggle('menu-open'));
  });

  const input = document.querySelector('[data-search]');
  const results = document.querySelector('[data-search-results]');
  const index = window.CHOS_SEARCH_INDEX || [];
  if (!input || !results) return;

  const normalize = (value) => value.toLocaleLowerCase('de-DE').normalize('NFD').replace(/[\u0300-\u036f]/g, '');
  const pagePrefix = location.pathname.includes('/docs/') ? '../' : '';

  const hideResults = () => {
    results.hidden = true;
    results.innerHTML = '';
  };

  input.addEventListener('input', () => {
    const query = normalize(input.value.trim());
    if (query.length < 2) {
      hideResults();
      return;
    }
    const terms = query.split(/\s+/).filter(Boolean);
    const matches = index
      .map((entry) => {
        const haystack = normalize(`${entry.title} ${entry.heading} ${entry.text}`);
        const score = terms.reduce((sum, term) => sum + (haystack.includes(term) ? 1 : 0), 0);
        return { entry, score };
      })
      .filter(({ score }) => score === terms.length)
      .slice(0, 12);

    results.hidden = false;
    if (!matches.length) {
      results.innerHTML = '<div class="search-empty">Keine passenden Inhalte gefunden.</div>';
      return;
    }
    results.innerHTML = matches.map(({ entry }) => {
      const heading = entry.heading === 'Dokument' ? '' : `<span>${escapeHtml(entry.heading)}</span>`;
      return `<a class="search-result" href="${pagePrefix}${entry.url}"><strong>${escapeHtml(entry.title)}</strong>${heading}</a>`;
    }).join('');
  });

  document.addEventListener('click', (event) => {
    if (!event.target.closest('.search-wrap')) hideResults();
  });

  function escapeHtml(value) {
    return value.replace(/[&<>'"]/g, (char) => ({
      '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;'
    })[char]);
  }
})();
