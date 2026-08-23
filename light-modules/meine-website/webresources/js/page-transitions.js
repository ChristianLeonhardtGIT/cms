const reducedMotionQuery = window.matchMedia('(prefers-reduced-motion: reduce)');
const transitionDuration = 260;
let navigationStarted = false;

const normalizeLegacyHomeUrl = () => {
  const currentUrl = new URL(window.location.href);
  if (currentUrl.pathname !== '/' || currentUrl.searchParams.get('_start') !== '1') return;

  currentUrl.searchParams.delete('_start');
  window.history.replaceState(
    window.history.state,
    '',
    `${currentUrl.pathname}${currentUrl.search}${currentUrl.hash}`,
  );
};

const resetPageTransition = () => {
  navigationStarted = false;
  document.documentElement.classList.remove('is-page-leaving');
  document.body.removeAttribute('aria-busy');
};

const isInternalPageLink = (link, event) => {
  if (
    event.defaultPrevented ||
    event.button !== 0 ||
    event.metaKey ||
    event.ctrlKey ||
    event.shiftKey ||
    event.altKey ||
    link.target === '_blank' ||
    link.hasAttribute('download')
  ) {
    return false;
  }

  const href = link.getAttribute('href');
  if (!href || href.startsWith('#')) return false;

  const targetUrl = new URL(link.href, window.location.href);
  if (targetUrl.origin !== window.location.origin) return false;

  const currentUrl = new URL(window.location.href);
  return (
    targetUrl.pathname !== currentUrl.pathname ||
    targetUrl.search !== currentUrl.search
  );
};

document.addEventListener('click', (event) => {
  const link = event.target.closest?.('a[href]');
  if (!link || !isInternalPageLink(link, event) || navigationStarted) return;

  if (reducedMotionQuery.matches) return;

  event.preventDefault();
  navigationStarted = true;
  document.dispatchEvent(new CustomEvent('page-transition-start'));
  document.documentElement.classList.add('is-page-leaving');
  document.body.setAttribute('aria-busy', 'true');

  window.setTimeout(() => {
    window.location.assign(link.href);
  }, transitionDuration);
});

window.addEventListener('pageshow', resetPageTransition);
normalizeLegacyHomeUrl();
