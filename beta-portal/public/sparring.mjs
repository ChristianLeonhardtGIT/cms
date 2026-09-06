const root = document.querySelector('[data-sparring]');
const status = document.querySelector('#sparring-update');
let stopped = false;
let timer;
async function refresh() {
  if (stopped || document.hidden) return;
  try {
    const response = await fetch(`/workspace/api/sparring/${root.dataset.sparring}`, { cache: 'no-store', credentials: 'same-origin' });
    if ([401, 403, 404].includes(response.status)) {
      stopped = true;
      document.querySelector('#sparring-messages').replaceChildren();
      for (const field of document.querySelectorAll('textarea')) field.value = '';
      location.replace('/workspace/login');
      return;
    }
    if (!response.ok) throw new Error();
    const e = await response.json();
    const articles = e.messages.map(m => {
      const article = document.createElement('article');
      const heading = document.createElement('h3');
      heading.textContent = m.senderId === root.dataset.user ? 'Du' : 'Gesprächspartner';
      const time = document.createElement('time');
      time.dateTime = m.createdAt;
      time.textContent = new Date(m.createdAt).toLocaleString('de-DE');
      const body = document.createElement('p'); body.className = 'sparring-text'; body.textContent = m.body;
      const read = document.createElement('small');
      read.textContent = e.otherReadIndex >= e.messages.indexOf(m) ? 'Gelesen' : 'Noch nicht als gelesen markiert';
      article.append(heading, time, body, read); return article;
    });
    document.querySelector('#sparring-messages').replaceChildren(...articles);
    status.textContent = e.status === 'active' ? 'Nachrichten aktualisiert.' : 'Das Sparring ist derzeit nicht aktiv.';
    if (e.status !== 'active') {
      const field = document.querySelector('textarea[name="body"]');
      if (field) { field.disabled = true; field.form.querySelector('button').disabled = true; }
    }
  } catch { status.textContent = 'Verbindung unterbrochen. Deine Eingabe bleibt nur in dieser geöffneten Seite.'; }
}
if (root) { await refresh(); timer = setInterval(refresh, 15000); }
window.addEventListener('pagehide', () => {
  stopped = true; clearInterval(timer);
  document.querySelector('#sparring-messages')?.replaceChildren();
  for (const field of document.querySelectorAll('textarea')) field.value = '';
});
window.addEventListener('pageshow', event => { if (event.persisted) location.reload(); });
