document.documentElement.classList.add('has-js');

const siteHeader = document.querySelector('.site-header');
const siteBrand = document.querySelector('.site-brand');
const navigationTrigger = document.querySelector('.site-nav-trigger');
const navigationMenu = document.querySelector('.site-nav');
const mobileMenuQuery = window.matchMedia('(max-width: 77.5rem)');
let menuScrollPosition = 0;
let submenuOpenLockedUntil = 0;

navigationMenu?.toggleAttribute('inert', mobileMenuQuery.matches);

const setSubmenuState = (toggle, isOpen) => {
  const item = toggle.closest('.site-nav__item');
  const submenu = item?.querySelector(':scope > .site-nav__list');
  const entryLink = item?.querySelector(':scope > .site-nav__entry > a');
  item?.classList.toggle('is-open', isOpen);
  if (mobileMenuQuery.matches && item?.parentElement?.classList.contains('site-nav__list--level-1')) {
    navigationMenu?.classList.toggle('is-submenu-open', isOpen);
    Array.from(item.parentElement.children).forEach((sibling) => {
      if (sibling !== item) sibling.toggleAttribute('inert', isOpen);
    });
    const entry = item.querySelector(':scope > .site-nav__entry');
    if (entry) entry.toggleAttribute('inert', isOpen);
    if (submenu) {
      submenu.toggleAttribute('inert', !isOpen);
      submenu.setAttribute('aria-hidden', String(!isOpen));
    }
    entryLink?.setAttribute('aria-expanded', String(isOpen));
  }
  toggle.setAttribute('aria-expanded', String(isOpen));
  toggle.setAttribute('aria-label', `Untermenü ${toggle.dataset.menuLabel} ${isOpen ? 'schließen' : 'öffnen'}`);
};

const closeSubmenus = () => {
  document.querySelectorAll('.site-nav__toggle[aria-expanded="true"]').forEach((toggle) => {
    setSubmenuState(toggle, false);
  });
  navigationMenu?.classList.remove('is-submenu-open');
};

document.querySelectorAll('.site-nav__list--level-1 > .site-nav__item--has-children').forEach((item) => {
  const toggle = item.querySelector(':scope > .site-nav__entry > .site-nav__toggle');
  const entryLink = item.querySelector(':scope > .site-nav__entry > a');
  const submenu = item.querySelector(':scope > .site-nav__list');
  if (!toggle || !entryLink || !submenu) return;

  const backItem = document.createElement('li');
  backItem.className = 'site-nav__back-item';
  const backButton = document.createElement('button');
  backButton.className = 'site-nav__back';
  backButton.type = 'button';
  backButton.innerHTML = `<span aria-hidden="true">←</span><span>Zurück</span>`;
  backButton.setAttribute('aria-label', `Zurück zum Hauptmenü von ${toggle.dataset.menuLabel}`);
  backButton.addEventListener('click', (event) => {
    event.preventDefault();
    event.stopPropagation();
    submenuOpenLockedUntil = Date.now() + 250;
    backButton.disabled = true;
    window.setTimeout(() => {
      setSubmenuState(toggle, false);
      backButton.disabled = false;
      toggle.focus({ preventScroll: true });
    }, 0);
  });
  backItem.append(backButton);

  const titleItem = document.createElement('li');
  titleItem.className = 'site-nav__mobile-title';
  titleItem.textContent = toggle.dataset.menuLabel;

  const overviewItem = document.createElement('li');
  overviewItem.className = 'site-nav__mobile-overview';
  const overviewLink = document.createElement('a');
  overviewLink.href = entryLink.href;
  overviewLink.innerHTML = `<span>${toggle.dataset.menuLabel} im Überblick</span><span aria-hidden="true">→</span>`;
  if (new URL(entryLink.href, window.location.origin).pathname === window.location.pathname) {
    overviewLink.setAttribute('aria-current', 'page');
  }
  overviewItem.append(overviewLink);

  submenu.prepend(backItem);
  backItem.after(titleItem, overviewItem);

  if (new URL(entryLink.href, window.location.origin).pathname === '/leistungen') {
    const guidanceItem = document.createElement('li');
    guidanceItem.className = 'site-nav__mobile-guidance';
    guidanceItem.innerHTML = `
      <p>Nicht sicher, welches Format passt?</p>
      <span>Beginne mit einer typischen Situation, prüfe dein System selbst oder vertiefe ein Thema.</span>
      <div class="site-nav__mobile-guidance-links">
        <a href="/probleme"><span>Situationen ansehen</span><span aria-hidden="true">→</span></a>
        <a href="/chos-selbstcheck"><span>Selbstcheck starten</span><span aria-hidden="true">→</span></a>
        <a href="/insights"><span>Insights vertiefen</span><span aria-hidden="true">→</span></a>
      </div>`;
    submenu.append(guidanceItem);
  }

  entryLink.addEventListener('click', (event) => {
    if (!mobileMenuQuery.matches) return;
    event.preventDefault();
    if (Date.now() < submenuOpenLockedUntil) return;
    toggle.click();
  });

  if (mobileMenuQuery.matches) {
    entryLink.setAttribute('aria-haspopup', 'true');
    entryLink.setAttribute('aria-expanded', 'false');
    submenu.setAttribute('inert', '');
    submenu.setAttribute('aria-hidden', 'true');
  }
});

const openDefaultSubmenus = () => {
  document.querySelectorAll('.site-nav__item--default-open > .site-nav__entry > .site-nav__toggle').forEach((toggle) => {
    setSubmenuState(toggle, true);
  });
};

const setBackgroundInert = (isInert) => {
  Array.from(document.body.children).forEach((element) => {
    if (element !== siteHeader) element.inert = isInert;
  });
};

const getMenuFocusableElements = () => Array.from(
  siteHeader?.querySelectorAll('a[href], button:not([disabled])') ?? []
).filter((element) => element.getClientRects().length > 0 && getComputedStyle(element).visibility !== 'hidden');

const setMainMenuState = (isOpen, { returnFocus = false } = {}) => {
  if (isOpen) {
    menuScrollPosition = window.scrollY;
    const scrollbarWidth = window.innerWidth - document.documentElement.clientWidth;
    document.documentElement.style.setProperty('--menu-scrollbar-compensation', `${scrollbarWidth}px`);
    document.documentElement.style.setProperty('--menu-scroll-offset', `${-menuScrollPosition}px`);
  }

  siteHeader?.classList.toggle('is-menu-open', isOpen);
  document.documentElement.classList.toggle('is-menu-overlay-open', isOpen);
  navigationTrigger?.setAttribute('aria-expanded', String(isOpen));
  navigationTrigger?.setAttribute('aria-label', `Hauptmenü ${isOpen ? 'schließen' : 'öffnen'}`);
  navigationMenu?.toggleAttribute('inert', mobileMenuQuery.matches && !isOpen);
  setBackgroundInert(isOpen);
  siteBrand?.toggleAttribute('inert', isOpen);

  if (isOpen) {
    siteHeader?.setAttribute('role', 'dialog');
    siteHeader?.setAttribute('aria-modal', 'true');
    siteHeader?.setAttribute('aria-label', 'Hauptmenü');
    if (!mobileMenuQuery.matches) openDefaultSubmenus();
  } else {
    siteHeader?.removeAttribute('role');
    siteHeader?.removeAttribute('aria-modal');
    siteHeader?.removeAttribute('aria-label');
    document.documentElement.style.removeProperty('--menu-scrollbar-compensation');
    document.documentElement.style.removeProperty('--menu-scroll-offset');

    const previousScrollBehavior = document.documentElement.style.scrollBehavior;
    document.documentElement.style.scrollBehavior = 'auto';
    window.scrollTo(0, menuScrollPosition);
    document.documentElement.style.scrollBehavior = previousScrollBehavior;
  }

  if (!isOpen) {
    closeSubmenus();
    if (returnFocus) navigationTrigger?.focus();
  }
};

navigationTrigger?.addEventListener('click', () => {
  const isOpen = navigationTrigger.getAttribute('aria-expanded') === 'true';
  setMainMenuState(!isOpen);
});

document.querySelectorAll('.site-nav__toggle').forEach((toggle) => {
  toggle.addEventListener('click', () => {
    const isOpen = toggle.getAttribute('aria-expanded') === 'true';
    if (mobileMenuQuery.matches && !isOpen && Date.now() < submenuOpenLockedUntil) return;
    if (mobileMenuQuery.matches && !isOpen) closeSubmenus();
    setSubmenuState(toggle, !isOpen);
    if (mobileMenuQuery.matches && !isOpen) {
      const item = toggle.closest('.site-nav__item');
      const backButton = item?.querySelector(':scope > .site-nav__list > .site-nav__back-item .site-nav__back');
      window.setTimeout(() => {
        if (item?.parentElement) item.parentElement.scrollLeft = 0;
        backButton?.focus({ preventScroll: true });
      }, 320);
    }
  });
});

document.addEventListener('keydown', (event) => {
  const mainMenuIsOpen = navigationTrigger?.getAttribute('aria-expanded') === 'true';

  if (event.key === 'Tab' && mainMenuIsOpen) {
    const focusableElements = getMenuFocusableElements();
    const firstElement = focusableElements[0];
    const lastElement = focusableElements[focusableElements.length - 1];

    if (event.shiftKey && document.activeElement === firstElement) {
      event.preventDefault();
      lastElement?.focus();
    } else if (!event.shiftKey && document.activeElement === lastElement) {
      event.preventDefault();
      firstElement?.focus();
    }
    return;
  }

  if (event.key !== 'Escape') return;

  const mobileOpenSubmenu = mobileMenuQuery.matches
    ? siteHeader?.querySelector('.site-nav__list--level-1 > .site-nav__item.is-open')
    : null;
  if (mobileOpenSubmenu) {
    const submenuToggle = mobileOpenSubmenu.querySelector(':scope > .site-nav__entry > .site-nav__toggle');
    setSubmenuState(submenuToggle, false);
    submenuToggle?.focus();
    return;
  }

  if (mainMenuIsOpen) {
    const focusIsInsideHeader = siteHeader?.contains(document.activeElement);
    setMainMenuState(false, { returnFocus: focusIsInsideHeader });
    return;
  }

  const activeSubmenuItem = document.activeElement?.closest?.('.site-nav__item.is-open');
  const activeSubmenuToggle = activeSubmenuItem?.querySelector(':scope > .site-nav__entry > .site-nav__toggle');
  closeSubmenus();
  activeSubmenuToggle?.focus();
});

document.addEventListener('pointerdown', (event) => {
  if (navigationTrigger?.getAttribute('aria-expanded') !== 'true') return;
  if (!siteHeader?.contains(event.target)) setMainMenuState(false);
});

document.addEventListener('focusin', (event) => {
  if (navigationTrigger?.getAttribute('aria-expanded') !== 'true') return;
  if (!siteHeader?.contains(event.target)) navigationTrigger?.focus();
});

mobileMenuQuery.addEventListener('change', (event) => {
  if (!event.matches) {
    setMainMenuState(false);
    document.querySelectorAll('.site-nav__item, .site-nav__entry, .site-nav__list--level-2, .site-nav__list--level-3').forEach((element) => {
      element.removeAttribute('inert');
      if (element.matches('.site-nav__list--level-2, .site-nav__list--level-3')) element.removeAttribute('aria-hidden');
    });
    document.querySelectorAll('.site-nav__list--level-1 > .site-nav__item--has-children > .site-nav__entry > a').forEach((link) => {
      link.removeAttribute('aria-haspopup');
      link.removeAttribute('aria-expanded');
    });
  } else {
    closeSubmenus();
    document.querySelectorAll('.site-nav__list--level-2, .site-nav__list--level-3').forEach((submenu) => {
      submenu.setAttribute('inert', '');
      submenu.setAttribute('aria-hidden', 'true');
    });
    document.querySelectorAll('.site-nav__list--level-1 > .site-nav__item--has-children > .site-nav__entry > a').forEach((link) => {
      link.setAttribute('aria-haspopup', 'true');
      link.setAttribute('aria-expanded', 'false');
    });
    navigationMenu?.toggleAttribute('inert', true);
  }
});

document.addEventListener('page-transition-start', () => {
  if (navigationTrigger?.getAttribute('aria-expanded') === 'true') {
    setMainMenuState(false);
  }
});
