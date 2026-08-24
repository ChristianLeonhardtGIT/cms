import { createServer } from 'node:http';
import { readFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import path from 'node:path';
import {
  accessPhase,
  hashPassword,
  normalizeEmail,
  randomToken,
  tokenDigest,
  validateEmail,
  validatePassword,
  verifyPassword
} from './lib/security.mjs';
import { ensureStore, findUserByEmail, findUserById, loadStore, updateStore } from './lib/store.mjs';
import { DomainValidationError } from './lib/chos-domain.mjs';
import {
  FileWorkspaceRepository,
  WorkspaceReadOnlyError,
  WorkspaceRequestError
} from './lib/workspace-repository.mjs';

const port = Number(process.env.BETA_PORT || 3000);
const host = process.env.BETA_HOST || '0.0.0.0';
const secureCookies = process.env.BETA_COOKIE_SECURE !== 'false';
const sessionCookie = secureCookies ? '__Host-chos_beta_session' : 'chos_beta_session';
const inviteCookie = secureCookies ? '__Host-chos_beta_invite' : 'chos_beta_invite';
const deletedWorkspaceCookie = 'chos_beta_deleted_workspace';
const sessions = new Map();
const nonces = new Map();
const loginAttempts = new Map();
const SESSION_MS = 12 * 60 * 60 * 1000;
const NONCE_MS = 15 * 60 * 1000;
const LOGIN_WINDOW_MS = 15 * 60 * 1000;
const CLEANUP_INTERVAL_MS = 6 * 60 * 60 * 1000;
const applicationDirectory = path.dirname(fileURLToPath(import.meta.url));
const cssPath = path.join(applicationDirectory, 'public', 'portal.css');
const accountDeleteScriptPath = path.join(applicationDirectory, 'public', 'account-delete.mjs');
const ownerBridgeCssPath = path.join(applicationDirectory, 'public', 'owner-bridge.css');
const chosDirectory = path.resolve(process.env.BETA_CHOS_DIR || path.join(applicationDirectory, 'chos-reader'));
const dataDirectory = process.env.BETA_DATA_DIR || '/app/data';
const workspaceRepository = new FileWorkspaceRepository(dataDirectory);
const portalVersion = JSON.parse(await readFile(path.join(applicationDirectory, 'package.json'), 'utf8')).version;
const workspaceDirectory = path.join(applicationDirectory, 'public', 'workspace');
const workspaceIndex = await readFile(path.join(workspaceDirectory, 'index.html'), 'utf8');
const workspaceAssets = new Map([
  ['workspace.css', { file: path.join(workspaceDirectory, 'workspace.css'), type: 'text/css; charset=utf-8' }],
  ['app.mjs', { file: path.join(workspaceDirectory, 'app.mjs'), type: 'text/javascript; charset=utf-8' }],
  ['indexed-db.mjs', { file: path.join(workspaceDirectory, 'indexed-db.mjs'), type: 'text/javascript; charset=utf-8' }],
  ['chos-domain.mjs', { file: path.join(applicationDirectory, 'lib', 'chos-domain.mjs'), type: 'text/javascript; charset=utf-8' }],
  ['local-first-workspace.mjs', { file: path.join(applicationDirectory, 'lib', 'local-first-workspace.mjs'), type: 'text/javascript; charset=utf-8' }],
  ['ai-runtime.mjs', { file: path.join(applicationDirectory, 'lib', 'ai-runtime.mjs'), type: 'text/javascript; charset=utf-8' }]
]);
const css = await readFile(cssPath, 'utf8');
const accountDeleteScript = await readFile(accountDeleteScriptPath, 'utf8');
const ownerBridgeCss = await readFile(ownerBridgeCssPath, 'utf8');

await ensureStore();

async function purgeExpiredAccounts() {
  const deletedIds = await updateStore((store) => {
    const expired = store.users
      .filter((user) => user.role !== 'owner' && user.passwordHash && new Date(user.readUntil).getTime() <= Date.now())
      .map((user) => user.id);
    if (expired.length) store.users = store.users.filter((user) => !expired.includes(user.id));
    return expired;
  });
  if (deletedIds.length) {
    await Promise.all(deletedIds.map((userId) => workspaceRepository.delete(userId)));
    for (const [token, session] of sessions) {
      if (deletedIds.includes(session.userId)) sessions.delete(token);
    }
    console.log(`[beta-portal] ${deletedIds.length} abgelaufene(s) Konto/Konten gelöscht`);
  }
}

await purgeExpiredAccounts();
setInterval(() => purgeExpiredAccounts().catch((error) => console.error(`[beta-portal] cleanup: ${error.message}`)), CLEANUP_INTERVAL_MS).unref();

function escapeHtml(value) {
  return String(value ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#039;');
}

function cookies(request) {
  return Object.fromEntries(String(request.headers.cookie || '').split(';').map((part) => part.trim()).filter(Boolean).map((part) => {
    const index = part.indexOf('=');
    return [decodeURIComponent(part.slice(0, index)), decodeURIComponent(part.slice(index + 1))];
  }));
}

function cookie(name, value, options = {}) {
  const items = [`${encodeURIComponent(name)}=${encodeURIComponent(value)}`, `Path=${options.path || '/'}`, 'SameSite=Strict'];
  if (options.httpOnly !== false) items.push('HttpOnly');
  if (secureCookies) items.push('Secure');
  if (options.maxAge !== undefined) items.push(`Max-Age=${options.maxAge}`);
  return items.join('; ');
}

function commonHeaders(contentType = 'text/html; charset=utf-8') {
  return {
    'Content-Type': contentType,
    'Cache-Control': 'no-store, max-age=0',
    Pragma: 'no-cache',
    'Content-Security-Policy': "default-src 'none'; style-src 'self'; img-src 'self' data:; form-action 'self'; base-uri 'none'; frame-ancestors 'none'",
    'X-Content-Type-Options': 'nosniff',
    'X-Frame-Options': 'DENY',
    'X-Robots-Tag': 'noindex, nofollow, noarchive',
    'Referrer-Policy': 'no-referrer',
    'Permissions-Policy': 'camera=(), geolocation=(), microphone=()'
  };
}

function send(response, status, body, headers = {}) {
  response.writeHead(status, { ...commonHeaders(), ...headers });
  response.end(body);
}

function sendJson(response, status, body) {
  send(response, status, JSON.stringify(body), workspaceHeaders('application/json; charset=utf-8'));
}

function redirect(response, location, setCookie) {
  const headers = { Location: location, ...commonHeaders('text/plain; charset=utf-8') };
  if (setCookie) headers['Set-Cookie'] = setCookie;
  response.writeHead(303, headers);
  response.end('Weiterleitung');
}

function page({ title, eyebrow = 'ChOS Beta', body }) {
  return `<!doctype html>
<html lang="de"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>${escapeHtml(title)} · ChOS Beta</title><link rel="stylesheet" href="/beta/assets/portal.css?v=20260824-2"></head>
<body><header class="site-header"><a class="brand" href="/beta/" aria-label="ChOS Beta Startseite">ChOS<span>Beta</span></a></header>
<main><div class="shell"><p class="eyebrow">${escapeHtml(eyebrow)}</p>${body}</div></main>
<footer><span>© Christian Leonhardt</span><a href="https://cleonhardt.de/impressum">Impressum</a><a href="https://cleonhardt.de/datenschutz">Datenschutz</a></footer></body></html>`;
}

function issueNonce(ip, purpose) {
  for (const [token, nonce] of nonces) {
    if (nonce.expiresAt <= Date.now()) nonces.delete(token);
  }
  const value = randomToken(24);
  nonces.set(value, { ip, purpose, expiresAt: Date.now() + NONCE_MS });
  return value;
}

function consumeNonce(value, ip, purpose) {
  const nonce = nonces.get(value);
  nonces.delete(value);
  return Boolean(nonce && nonce.ip === ip && nonce.purpose === purpose && nonce.expiresAt > Date.now());
}

function requestIp(request) {
  return String(request.headers['x-forwarded-for'] || request.socket.remoteAddress || '').split(',')[0].trim();
}

async function requestBody(request) {
  const chunks = [];
  let size = 0;
  for await (const chunk of request) {
    size += chunk.length;
    if (size > 16 * 1024) throw new Error('Anfrage zu groß.');
    chunks.push(chunk);
  }
  return new URLSearchParams(Buffer.concat(chunks).toString('utf8'));
}

async function requestJson(request) {
  if (!String(request.headers['content-type'] || '').toLowerCase().startsWith('application/json')) {
    throw new WorkspaceRequestError('Content-Type muss application/json sein.');
  }
  if (request.headers['x-chos-client'] !== 'workspace-v1') {
    throw new WorkspaceRequestError('Workspace-Clientkennung fehlt.');
  }
  const chunks = [];
  let size = 0;
  for await (const chunk of request) {
    size += chunk.length;
    if (size > 512 * 1024) throw new WorkspaceRequestError('Sync-Anfrage ist zu groß.');
    chunks.push(chunk);
  }
  try {
    return JSON.parse(Buffer.concat(chunks).toString('utf8'));
  } catch {
    throw new WorkspaceRequestError('Sync-Anfrage enthält kein gültiges JSON.');
  }
}

function formatDate(value) {
  return new Intl.DateTimeFormat('de-DE', { dateStyle: 'long', timeZone: 'Europe/Berlin' }).format(new Date(value));
}

async function authenticatedUser(request) {
  const token = cookies(request)[sessionCookie];
  const session = token && sessions.get(token);
  if (!session || session.expiresAt <= Date.now()) {
    if (token) sessions.delete(token);
    return null;
  }
  const store = await loadStore();
  const user = findUserById(store, session.userId);
  if (!user || ['disabled', 'expired', 'invited'].includes(accessPhase(user))) {
    sessions.delete(token);
    return null;
  }
  return { user, token, phase: accessPhase(user) };
}

function createSession(user) {
  const token = randomToken();
  const hardEnd = user.role === 'owner' ? Number.POSITIVE_INFINITY : new Date(user.readUntil).getTime();
  sessions.set(token, { userId: user.id, expiresAt: Math.min(Date.now() + SESSION_MS, hardEnd) });
  return token;
}

function invalidateUserSessions(userId, keepToken = null) {
  for (const [token, session] of sessions) {
    if (session.userId === userId && token !== keepToken) sessions.delete(token);
  }
}

function destinationFor(user) {
  return user.role === 'owner' ? '/beta/chos/' : '/beta/';
}

function loginPage(request, message = '') {
  const nonce = issueNonce(requestIp(request), 'login');
  return page({
    title: 'Anmelden',
    body: `<section class="auth-card"><h1>Im geschützten ChOS-Bereich anmelden</h1>
      <p class="lede">Der Zugang ist ausschließlich für persönlich freigeschaltete ChOS- und Beta-Konten vorgesehen.</p>
      ${message ? `<p class="notice error" role="alert">${escapeHtml(message)}</p>` : ''}
      <form method="post" action="/beta/login"><input type="hidden" name="nonce" value="${nonce}">
      <label>E-Mail-Adresse<input type="email" name="email" autocomplete="username" required></label>
      <label>Passwort<input type="password" name="password" autocomplete="current-password" required></label>
      <button type="submit">Anmelden</button></form>
      <p class="microcopy">Noch keine Einladung? Die Beta ist auf fünf Teilnehmende begrenzt und wird persönlich freigeschaltet.</p></section>`
  });
}

function invitationPage(request, invitation, message = '') {
  const nonce = issueNonce(requestIp(request), 'accept-invite');
  const owner = invitation.user.role === 'owner';
  return page({
    title: 'Einladung annehmen',
    eyebrow: owner ? 'Persönlicher ChOS-Zugang' : 'ChOS Beta',
    body: `<section class="auth-card"><h1>${owner ? 'Deinen ChOS-Zugang einrichten' : 'Deinen Beta-Zugang einrichten'}</h1>
      <p class="lede">Willkommen, ${escapeHtml(invitation.user.name)}. Lege jetzt dein persönliches Passwort fest.</p>
      <div class="notice"><strong>Vor der Nutzung:</strong> ${owner ? 'Dieser dauerhaft angelegte Zugang ist persönlich und darf nicht weitergegeben werden.' : 'Der Zugang setzt die vereinbarte Teilnahme, Vertraulichkeit und Zahlung voraus. ChOS darf nicht kopiert, vervielfältigt oder außerhalb der Beta angewandt werden.'}</div>
      ${message ? `<p class="notice error" role="alert">${escapeHtml(message)}</p>` : ''}
      <form method="post" action="/beta/einladung"><input type="hidden" name="nonce" value="${nonce}">
      <label>Passwort <span>mindestens 14 Zeichen</span><input type="password" name="password" autocomplete="new-password" minlength="14" maxlength="128" required></label>
      <label>Passwort wiederholen<input type="password" name="confirm" autocomplete="new-password" minlength="14" maxlength="128" required></label>
      <label class="check"><input type="checkbox" name="accepted" value="yes" required><span>${owner ? 'Ich aktiviere meinen persönlichen ChOS-Zugang.' : 'Ich bestätige die vereinbarten Teilnahme- und Vertraulichkeitsbedingungen.'}</span></label>
      <button type="submit">Zugang aktivieren</button></form></section>`
  });
}

async function inviteFromCookie(request) {
  const rawToken = cookies(request)[inviteCookie];
  if (!rawToken) return null;
  const digest = tokenDigest(rawToken);
  const store = await loadStore();
  const user = store.users.find((entry) => entry.inviteTokenHash === digest && entry.status === 'invited');
  if (!user || new Date(user.inviteExpiresAt).getTime() <= Date.now()) return null;
  return { user, token: rawToken };
}

function phaseText(phase) {
  if (phase === 'owner') return 'Persönlicher Zugriff';
  if (phase === 'active') return 'Aktive Arbeitsphase';
  if (phase === 'readonly') return 'Lesephase';
  return 'Start vorbereitet';
}

function dashboardPage(request, user, phase) {
  const logoutNonce = issueNonce(requestIp(request), 'logout');
  const readOnly = phase === 'readonly';
  const scheduled = phase === 'scheduled';
  return page({
    title: 'Arbeitsbereich',
    body: `<div class="dashboard-head"><div><p class="status">${phaseText(phase)}</p><h1>Willkommen, ${escapeHtml(user.name)}</h1>
    <p class="lede">${readOnly ? 'Deine aktive Beta ist beendet. Inhalte bleiben bis zum Ende der Lesephase verfügbar.' : scheduled ? `Deine Arbeitsphase beginnt am ${formatDate(user.startAt)}.` : 'Hier entsteht dein geschützter ChOS-Arbeitsbereich.'}</p></div>
    <form method="post" action="/beta/logout"><input type="hidden" name="nonce" value="${logoutNonce}"><button class="secondary" type="submit">Abmelden</button></form></div>
    <section class="grid">
      <article><p class="card-label">Zugriffsphase</p><h2>${phaseText(phase)}</h2><dl><div><dt>Aktiv bis</dt><dd>${formatDate(user.activeUntil)}</dd></div><div><dt>Lesbar bis</dt><dd>${formatDate(user.readUntil)}</dd></div></dl></article>
      <article><p class="card-label">Persönliche Begleitung</p><h2>${Number(user.supportHoursTotal) - Number(user.supportHoursUsed)} Stunden verfügbar</h2><p>${user.supportHoursUsed} von ${user.supportHoursTotal} Stunden genutzt.</p></article>
    </section>
    <section class="content-card"><div><p class="card-label">Nächster Schritt</p><h2>Onboarding und Arbeitsrahmen</h2><p>Prüfe den Ablauf, die Arbeitsregeln und was du vor dem ersten Termin vorbereiten solltest.</p></div><a class="button${readOnly ? ' muted' : ''}" href="/beta/onboarding">Onboarding öffnen</a></section>
    <section class="content-card"><div><p class="card-label">Local-first Arbeitsfläche</p><h2>ChOS Workspace</h2><p>Arbeitsfälle, Beobachtungen und Annahmen werden zuerst auf deinem Gerät gespeichert und anschließend geschützt synchronisiert.</p></div><a class="button" href="/beta/workspace/">Workspace öffnen</a></section>`
  });
}

function onboardingPage(request, user, phase) {
  const logoutNonce = issueNonce(requestIp(request), 'logout');
  return page({
    title: 'Onboarding',
    body: `<nav class="portal-nav"><a href="/beta/">← Arbeitsbereich</a><form method="post" action="/beta/logout"><input type="hidden" name="nonce" value="${logoutNonce}"><button class="link-button" type="submit">Abmelden</button></form></nav>
    <p class="status">${phaseText(phase)}</p><h1>Onboarding</h1><p class="lede">Der gemeinsame Rahmen für deine ChOS-Beta.</p>
    <section class="steps"><article><span>01</span><h2>Arbeitsfall abgrenzen</h2><p>Wir arbeiten an einem konkreten organisationalen Fall. Vertrauliche Daten werden nur soweit eingebracht, wie es für die Diagnose erforderlich ist.</p></article>
    <article><span>02</span><h2>Diagnose vor Eingriff</h2><p>Beobachtungen, Interpretationen und Hypothesen werden getrennt. Erst danach folgt ein begrenzter Test.</p></article>
    <article><span>03</span><h2>Persönliche Begleitung planen</h2><p>Für ${escapeHtml(user.name)} sind insgesamt ${user.supportHoursTotal} Stunden persönliche Begleitung vorgesehen.</p></article>
    <article><span>04</span><h2>Zugriff und Abschluss</h2><p>Die aktive Phase endet am ${formatDate(user.activeUntil)}. Danach bleiben die Inhalte bis ${formatDate(user.readUntil)} lesbar und werden anschließend gelöscht.</p></article></section>`
  });
}

function accountPage(request, user, { message = '', error = '' } = {}) {
  const logoutNonce = issueNonce(requestIp(request), 'logout');
  const emailNonce = issueNonce(requestIp(request), 'account-email');
  const passwordNonce = issueNonce(requestIp(request), 'account-password');
  const deleteNonce = issueNonce(requestIp(request), 'account-delete');
  const owner = user.role === 'owner';
  return page({
    title: 'Konto verwalten',
    eyebrow: 'Persönlicher Zugang',
    body: `<nav class="portal-nav"><a href="${owner ? '/beta/chos/' : '/beta/workspace/'}">← ${owner ? 'Zurück zu ChOS' : 'Zurück zum Workspace'}</a><form method="post" action="/beta/logout"><input type="hidden" name="nonce" value="${logoutNonce}"><button class="link-button" type="submit">Abmelden</button></form></nav>
      <p class="status">${owner ? 'Persönlicher Zugriff' : 'Persönliches Konto'}</p><h1>Konto verwalten</h1>
      <p class="lede">Hier verwaltest du die Zugangsdaten für ${escapeHtml(user.name)}.</p>
      ${message ? `<p class="notice success" role="status">${escapeHtml(message)}</p>` : ''}
      ${error ? `<p class="notice error" role="alert">${escapeHtml(error)}</p>` : ''}
      <div class="account-grid">
        <section class="account-card" aria-labelledby="account-email-heading"><p class="card-label">Zugang</p><h2 id="account-email-heading">E-Mail-Adresse ändern</h2><p>Aktuell: <strong>${escapeHtml(user.email)}</strong></p>
          <form method="post" action="/beta/konto/email"><input type="hidden" name="nonce" value="${emailNonce}">
            <label>Neue E-Mail-Adresse<input type="email" name="email" value="${escapeHtml(user.email)}" autocomplete="email" maxlength="254" required></label>
            <label>Aktuelles Passwort<input type="password" name="current-password" autocomplete="current-password" maxlength="128" required></label>
            <button type="submit">E-Mail-Adresse speichern</button>
          </form>
        </section>
        <section class="account-card" aria-labelledby="account-password-heading"><p class="card-label">Sicherheit</p><h2 id="account-password-heading">Passwort ändern</h2><p>Das neue Passwort muss mindestens 14 Zeichen lang sein.</p>
          <form method="post" action="/beta/konto/passwort"><input type="hidden" name="nonce" value="${passwordNonce}">
            <label>Aktuelles Passwort<input type="password" name="current-password" autocomplete="current-password" maxlength="128" required></label>
            <label>Neues Passwort<input type="password" name="password" autocomplete="new-password" minlength="14" maxlength="128" required></label>
            <label>Neues Passwort wiederholen<input type="password" name="confirm" autocomplete="new-password" minlength="14" maxlength="128" required></label>
            <button type="submit">Passwort ändern</button>
          </form>
        </section>
      </div>
      <section class="account-card account-danger" aria-labelledby="account-delete-heading"><p class="card-label">Gefahrenbereich</p><h2 id="account-delete-heading">Konto löschen</h2>
        <p>Damit werden dein Konto und dein serverseitiger Workspace dauerhaft gelöscht. Die lokale Arbeitskopie dieses Kontos wird anschließend in diesem Browser entfernt.</p>
        <form method="post" action="/beta/konto/loeschen"><input type="hidden" name="nonce" value="${deleteNonce}">
          <label>Aktuelles Passwort<input type="password" name="current-password" autocomplete="current-password" maxlength="128" required></label>
          <label>Zur Bestätigung deine E-Mail-Adresse eingeben<input type="email" name="confirm-email" autocomplete="off" maxlength="254" required></label>
          <button class="danger-button" type="submit">Konto und Workspace dauerhaft löschen</button>
        </form>
      </section>`
  });
}

function accountStatus(value) {
  if (value === 'email') return 'Deine E-Mail-Adresse wurde geändert.';
  if (value === 'password') return 'Dein Passwort wurde geändert. Andere Sitzungen wurden beendet.';
  return '';
}

function accountDeletedPage(userId) {
  return `<!doctype html>
<html lang="de"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>Konto gelöscht · ChOS</title><link rel="stylesheet" href="/beta/assets/portal.css?v=20260824-2"></head>
<body><header class="site-header"><a class="brand" href="/chos" aria-label="Zurück zu ChOS">ChOS</a></header>
<main><div class="shell"><p class="eyebrow">Konto gelöscht</p><section class="auth-card" data-deleted-user="${escapeHtml(userId)}"><h1>Dein Konto wurde gelöscht.</h1>
<p class="lede">Deine serverseitigen Kontodaten und dein Workspace wurden entfernt.</p><p id="local-delete-status" class="notice" role="status">Die lokale Arbeitskopie wird aus diesem Browser entfernt …</p>
<a class="button" href="/chos">Zurück zu ChOS</a></section></div></main>
<footer><span>© Christian Leonhardt</span><a href="https://cleonhardt.de/impressum">Impressum</a><a href="https://cleonhardt.de/datenschutz">Datenschutz</a></footer>
<script type="module" src="/beta/assets/account-delete.mjs?v=20260824-1"></script></body></html>`;
}

function workspaceHeaders(contentType = 'text/html; charset=utf-8', cache = false) {
  return {
    ...commonHeaders(contentType),
    'Cache-Control': cache ? 'private, max-age=3600' : 'no-store, max-age=0',
    'Content-Security-Policy': "default-src 'none'; script-src 'self'; style-src 'self'; connect-src 'self'; img-src 'self' data:; form-action 'self'; base-uri 'self'; frame-ancestors 'none'"
  };
}

async function serveWorkspaceAsset(request, response, assetName) {
  const auth = await authenticatedUser(request);
  if (!auth) return send(response, 401, 'Anmeldung erforderlich.', workspaceHeaders('text/plain; charset=utf-8'));
  const asset = workspaceAssets.get(assetName);
  if (!asset) return send(response, 404, 'Nicht gefunden.', workspaceHeaders('text/plain; charset=utf-8'));
  send(response, 200, await readFile(asset.file), workspaceHeaders(asset.type, true));
}

function workspaceBootstrap(auth) {
  return {
    version: 1,
    user: { id: auth.user.id, name: auth.user.name },
    phase: auth.phase,
    canWrite: auth.phase === 'active' || auth.phase === 'owner',
    boundaries: {
      knowledge: 'published-knowledge-content',
      domain: 'chos-domain',
      userData: 'local-first-workspace'
    },
    ai: { localProvider: false, cloudProvider: false }
  };
}

const mimeTypes = new Map([
  ['.html', 'text/html; charset=utf-8'],
  ['.css', 'text/css; charset=utf-8'],
  ['.js', 'text/javascript; charset=utf-8'],
  ['.json', 'application/json; charset=utf-8'],
  ['.svg', 'image/svg+xml'],
  ['.png', 'image/png'],
  ['.jpg', 'image/jpeg'],
  ['.jpeg', 'image/jpeg'],
  ['.webp', 'image/webp'],
  ['.txt', 'text/plain; charset=utf-8']
]);

function chosHeaders(contentType, cache = false) {
  return {
    ...commonHeaders(contentType),
    'Cache-Control': cache ? 'private, max-age=3600' : 'no-store, max-age=0',
    'Content-Security-Policy': "default-src 'none'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; form-action 'self'; base-uri 'self'; frame-ancestors 'none'"
  };
}

async function serveChosDocument(request, response, pathname) {
  const auth = await authenticatedUser(request);
  if (!auth) return redirect(response, '/beta/login', cookie(sessionCookie, '', { maxAge: 0 }));
  if (auth.user.role !== 'owner') {
    return send(response, 403, page({ title: 'Kein Zugriff', body: '<section class="auth-card"><h1>Dieser Bereich ist nicht für dein Konto freigeschaltet.</h1><p><a href="/beta/">Zum Beta-Arbeitsbereich</a></p></section>' }));
  }
  let relativePath;
  try {
    relativePath = decodeURIComponent(pathname.slice('/beta/chos/'.length));
  } catch {
    return send(response, 400, 'Ungültiger Pfad', commonHeaders('text/plain; charset=utf-8'));
  }
  if (!relativePath) relativePath = 'index.html';
  if (relativePath.includes('\0') || relativePath.split('/').includes('..')) {
    return send(response, 400, 'Ungültiger Pfad', commonHeaders('text/plain; charset=utf-8'));
  }
  const filePath = path.resolve(chosDirectory, relativePath);
  if (filePath !== chosDirectory && !filePath.startsWith(`${chosDirectory}${path.sep}`)) {
    return send(response, 400, 'Ungültiger Pfad', commonHeaders('text/plain; charset=utf-8'));
  }
  const extension = path.extname(filePath).toLowerCase();
  const contentType = mimeTypes.get(extension);
  if (!contentType) return send(response, 404, 'Nicht gefunden', commonHeaders('text/plain; charset=utf-8'));
  try {
    let content = await readFile(filePath);
    if (extension === '.html') {
      content = content.toString('utf8')
        .replace('</head>', '<link rel="stylesheet" href="/beta/assets/owner-bridge.css"></head>')
        .replace('</body>', '<a class="chos-owner-access" href="/beta/konto" aria-label="Persönlichen Zugang verwalten">Zugang</a></body>');
    }
    send(response, 200, content, chosHeaders(contentType, extension !== '.html'));
  } catch (error) {
    if (error.code === 'ENOENT' || error.code === 'EISDIR') return send(response, 404, 'Nicht gefunden', commonHeaders('text/plain; charset=utf-8'));
    throw error;
  }
}

function attemptKey(request, email) {
  return `${requestIp(request)}:${email}`;
}

function isRateLimited(key) {
  const attempts = (loginAttempts.get(key) || []).filter((time) => time > Date.now() - LOGIN_WINDOW_MS);
  loginAttempts.set(key, attempts);
  return attempts.length >= 5;
}

function failedLogin(key) {
  const attempts = loginAttempts.get(key) || [];
  attempts.push(Date.now());
  loginAttempts.set(key, attempts);
}

export const server = createServer(async (request, response) => {
  try {
    const url = new URL(request.url, 'http://beta.local');
    if (url.pathname === '/beta/health' && request.method === 'GET') {
      send(response, 200, JSON.stringify({ status: 'ok', version: portalVersion }), { ...commonHeaders('application/json; charset=utf-8') });
      return;
    }
    if (url.pathname === '/beta/assets/portal.css' && request.method === 'GET') {
      send(response, 200, css, { ...commonHeaders('text/css; charset=utf-8'), 'Cache-Control': 'public, max-age=3600' });
      return;
    }
    if (url.pathname === '/beta/assets/account-delete.mjs' && request.method === 'GET') {
      send(response, 200, accountDeleteScript, { ...commonHeaders('text/javascript; charset=utf-8'), 'Cache-Control': 'public, max-age=3600' });
      return;
    }
    if (url.pathname === '/beta/assets/owner-bridge.css' && request.method === 'GET') {
      send(response, 200, ownerBridgeCss, { ...commonHeaders('text/css; charset=utf-8'), 'Cache-Control': 'private, max-age=3600' });
      return;
    }
    if (url.pathname === '/beta/login' && request.method === 'GET') {
      const auth = await authenticatedUser(request);
      if (auth) return redirect(response, destinationFor(auth.user));
      send(response, 200, loginPage(request));
      return;
    }
    if (url.pathname === '/beta/login' && request.method === 'POST') {
      const form = await requestBody(request);
      if (!consumeNonce(form.get('nonce'), requestIp(request), 'login')) return send(response, 400, loginPage(request, 'Die Anmeldung ist abgelaufen. Bitte versuche es erneut.'));
      const email = normalizeEmail(form.get('email'));
      const key = attemptKey(request, email);
      if (isRateLimited(key)) return send(response, 429, loginPage(request, 'Zu viele Versuche. Bitte warte 15 Minuten.'));
      const store = await loadStore();
      const user = findUserByEmail(store, email);
      const valid = user && await verifyPassword(form.get('password'), user.passwordHash);
      const phase = user && accessPhase(user);
      if (!valid || ['invited', 'disabled', 'expired'].includes(phase)) {
        failedLogin(key);
        return send(response, 401, loginPage(request, 'E-Mail-Adresse oder Passwort sind nicht korrekt.'));
      }
      loginAttempts.delete(key);
      const token = createSession(user);
      redirect(response, destinationFor(user), cookie(sessionCookie, token, { maxAge: SESSION_MS / 1000 }));
      return;
    }
    if (url.pathname === '/beta/einladung' && request.method === 'GET' && url.searchParams.has('token')) {
      const token = url.searchParams.get('token');
      const digest = tokenDigest(token);
      const store = await loadStore();
      const user = store.users.find((entry) => entry.inviteTokenHash === digest && entry.status === 'invited' && new Date(entry.inviteExpiresAt).getTime() > Date.now());
      if (!user) return send(response, 410, page({ title: 'Einladung ungültig', body: '<section class="auth-card"><h1>Diese Einladung ist nicht mehr gültig.</h1><p>Bitte fordere eine neue persönliche Einladung an.</p></section>' }));
      redirect(response, '/beta/einladung', cookie(inviteCookie, token, { maxAge: 3 * 24 * 60 * 60 }));
      return;
    }
    if (url.pathname === '/beta/einladung' && request.method === 'GET') {
      const invitation = await inviteFromCookie(request);
      if (!invitation) return send(response, 410, page({ title: 'Einladung ungültig', body: '<section class="auth-card"><h1>Diese Einladung ist nicht mehr gültig.</h1><p>Bitte fordere eine neue persönliche Einladung an.</p></section>' }));
      send(response, 200, invitationPage(request, invitation));
      return;
    }
    if (url.pathname === '/beta/einladung' && request.method === 'POST') {
      const invitation = await inviteFromCookie(request);
      if (!invitation) return send(response, 410, page({ title: 'Einladung ungültig', body: '<section class="auth-card"><h1>Diese Einladung ist nicht mehr gültig.</h1></section>' }));
      const form = await requestBody(request);
      if (!consumeNonce(form.get('nonce'), requestIp(request), 'accept-invite')) return send(response, 400, invitationPage(request, invitation, 'Das Formular ist abgelaufen. Bitte versuche es erneut.'));
      const password = form.get('password');
      const validation = validatePassword(password);
      if (validation || password !== form.get('confirm') || form.get('accepted') !== 'yes') {
        return send(response, 400, invitationPage(request, invitation, validation || 'Bitte bestätige Passwort und Teilnahmebedingungen vollständig.'));
      }
      const passwordHash = await hashPassword(password);
      const user = await updateStore((store) => {
        const current = store.users.find((entry) => entry.id === invitation.user.id && entry.inviteTokenHash === tokenDigest(invitation.token));
        if (!current) throw new Error('Einladung wurde bereits verwendet.');
        current.passwordHash = passwordHash;
        current.status = 'active';
        current.acceptedAt = new Date().toISOString();
        current.inviteTokenHash = null;
        current.inviteExpiresAt = null;
        current.updatedAt = new Date().toISOString();
        return current;
      });
      const session = createSession(user);
      redirect(response, destinationFor(user), [cookie(sessionCookie, session, { maxAge: SESSION_MS / 1000 }), cookie(inviteCookie, '', { maxAge: 0 })]);
      return;
    }
    if (url.pathname === '/beta/logout' && request.method === 'POST') {
      const auth = await authenticatedUser(request);
      const form = await requestBody(request);
      if (!auth || !consumeNonce(form.get('nonce'), requestIp(request), 'logout')) return send(response, 400, page({ title: 'Abmeldung fehlgeschlagen', body: '<h1>Die Abmeldung konnte nicht bestätigt werden.</h1>' }));
      sessions.delete(auth.token);
      redirect(response, '/beta/login', cookie(sessionCookie, '', { maxAge: 0 }));
      return;
    }
    if (url.pathname === '/beta/konto-geloescht' && request.method === 'GET') {
      const deletedUserId = cookies(request)[deletedWorkspaceCookie];
      const userId = /^[0-9a-f-]{36}$/i.test(String(deletedUserId || '')) ? deletedUserId : '';
      send(response, 200, accountDeletedPage(userId), {
        ...workspaceHeaders(),
        'Set-Cookie': cookie(deletedWorkspaceCookie, '', { path: '/beta/konto-geloescht', maxAge: 0, httpOnly: false })
      });
      return;
    }
    if (url.pathname === '/beta/konto/email' && request.method === 'POST') {
      const auth = await authenticatedUser(request);
      if (!auth) return redirect(response, '/beta/login', cookie(sessionCookie, '', { maxAge: 0 }));
      const form = await requestBody(request);
      if (!consumeNonce(form.get('nonce'), requestIp(request), 'account-email')) {
        return send(response, 400, accountPage(request, auth.user, { error: 'Das Formular ist abgelaufen. Bitte versuche es erneut.' }));
      }
      const key = attemptKey(request, `${auth.user.id}:account-email`);
      if (isRateLimited(key)) return send(response, 429, accountPage(request, auth.user, { error: 'Zu viele Versuche. Bitte warte 15 Minuten.' }));
      if (!await verifyPassword(form.get('current-password'), auth.user.passwordHash)) {
        failedLogin(key);
        return send(response, 403, accountPage(request, auth.user, { error: 'Das aktuelle Passwort ist nicht korrekt.' }));
      }
      const email = normalizeEmail(form.get('email'));
      const emailValidation = validateEmail(email);
      if (emailValidation) return send(response, 400, accountPage(request, auth.user, { error: emailValidation }));
      const result = await updateStore((store) => {
        if (store.users.some((entry) => entry.id !== auth.user.id && entry.email === email)) return { duplicate: true };
        const current = findUserById(store, auth.user.id);
        if (!current) throw new Error('Konto wurde nicht gefunden.');
        current.email = email;
        current.updatedAt = new Date().toISOString();
        return { user: current };
      });
      if (result.duplicate) return send(response, 409, accountPage(request, auth.user, { error: 'Diese E-Mail-Adresse wird bereits verwendet.' }));
      loginAttempts.delete(key);
      invalidateUserSessions(auth.user.id, auth.token);
      redirect(response, '/beta/konto?status=email');
      return;
    }
    if (url.pathname === '/beta/konto/passwort' && request.method === 'POST') {
      const auth = await authenticatedUser(request);
      if (!auth) return redirect(response, '/beta/login', cookie(sessionCookie, '', { maxAge: 0 }));
      const form = await requestBody(request);
      if (!consumeNonce(form.get('nonce'), requestIp(request), 'account-password')) {
        return send(response, 400, accountPage(request, auth.user, { error: 'Das Formular ist abgelaufen. Bitte versuche es erneut.' }));
      }
      const key = attemptKey(request, `${auth.user.id}:account-password`);
      if (isRateLimited(key)) return send(response, 429, accountPage(request, auth.user, { error: 'Zu viele Versuche. Bitte warte 15 Minuten.' }));
      if (!await verifyPassword(form.get('current-password'), auth.user.passwordHash)) {
        failedLogin(key);
        return send(response, 403, accountPage(request, auth.user, { error: 'Das aktuelle Passwort ist nicht korrekt.' }));
      }
      const password = form.get('password');
      const passwordValidation = validatePassword(password);
      if (passwordValidation || password !== form.get('confirm')) {
        return send(response, 400, accountPage(request, auth.user, { error: passwordValidation || 'Die neuen Passwörter stimmen nicht überein.' }));
      }
      if (await verifyPassword(password, auth.user.passwordHash)) {
        return send(response, 400, accountPage(request, auth.user, { error: 'Das neue Passwort muss sich vom aktuellen Passwort unterscheiden.' }));
      }
      const passwordHash = await hashPassword(password);
      const user = await updateStore((store) => {
        const current = findUserById(store, auth.user.id);
        if (!current) throw new Error('Konto wurde nicht gefunden.');
        current.passwordHash = passwordHash;
        current.updatedAt = new Date().toISOString();
        return current;
      });
      loginAttempts.delete(key);
      invalidateUserSessions(auth.user.id);
      const token = createSession(user);
      redirect(response, '/beta/konto?status=password', cookie(sessionCookie, token, { maxAge: SESSION_MS / 1000 }));
      return;
    }
    if (url.pathname === '/beta/konto/loeschen' && request.method === 'POST') {
      const auth = await authenticatedUser(request);
      if (!auth) return redirect(response, '/beta/login', cookie(sessionCookie, '', { maxAge: 0 }));
      const form = await requestBody(request);
      if (!consumeNonce(form.get('nonce'), requestIp(request), 'account-delete')) {
        return send(response, 400, accountPage(request, auth.user, { error: 'Das Formular ist abgelaufen. Bitte versuche es erneut.' }));
      }
      const key = attemptKey(request, `${auth.user.id}:account-delete`);
      if (isRateLimited(key)) return send(response, 429, accountPage(request, auth.user, { error: 'Zu viele Versuche. Bitte warte 15 Minuten.' }));
      const passwordValid = await verifyPassword(form.get('current-password'), auth.user.passwordHash);
      const emailValid = normalizeEmail(form.get('confirm-email')) === auth.user.email;
      if (!passwordValid || !emailValid) {
        failedLogin(key);
        return send(response, 403, accountPage(request, auth.user, { error: 'Passwort oder bestätigte E-Mail-Adresse ist nicht korrekt.' }));
      }
      await updateStore((store) => {
        const current = findUserById(store, auth.user.id);
        if (!current) throw new Error('Konto wurde nicht gefunden.');
        current.disabledAt = new Date().toISOString();
        current.updatedAt = new Date().toISOString();
      });
      await workspaceRepository.delete(auth.user.id);
      await updateStore((store) => {
        store.users = store.users.filter((entry) => entry.id !== auth.user.id);
      });
      invalidateUserSessions(auth.user.id);
      redirect(response, '/beta/konto-geloescht', [
        cookie(sessionCookie, '', { maxAge: 0 }),
        cookie(deletedWorkspaceCookie, auth.user.id, { path: '/beta/konto-geloescht', maxAge: 300, httpOnly: false })
      ]);
      return;
    }
    if (url.pathname === '/beta/workspace' && request.method === 'GET') return redirect(response, '/beta/workspace/');
    if (url.pathname === '/beta/workspace/' && request.method === 'GET') {
      const auth = await authenticatedUser(request);
      if (!auth) return redirect(response, '/beta/login', cookie(sessionCookie, '', { maxAge: 0 }));
      send(response, 200, workspaceIndex, workspaceHeaders());
      return;
    }
    if (url.pathname.startsWith('/beta/workspace/assets/') && request.method === 'GET') {
      await serveWorkspaceAsset(request, response, url.pathname.slice('/beta/workspace/assets/'.length));
      return;
    }
    if (url.pathname === '/beta/api/workspace/bootstrap' && request.method === 'GET') {
      const auth = await authenticatedUser(request);
      if (!auth) return sendJson(response, 401, { error: 'Anmeldung erforderlich.' });
      sendJson(response, 200, workspaceBootstrap(auth));
      return;
    }
    if (url.pathname === '/beta/api/workspace/sync' && request.method === 'POST') {
      const auth = await authenticatedUser(request);
      if (!auth) return sendJson(response, 401, { error: 'Anmeldung erforderlich.' });
      try {
        const result = await workspaceRepository.sync(auth.user.id, await requestJson(request), {
          canWrite: auth.phase === 'active' || auth.phase === 'owner'
        });
        sendJson(response, 200, result);
      } catch (error) {
        if (error instanceof WorkspaceReadOnlyError) return sendJson(response, 403, { error: error.message });
        if (error instanceof WorkspaceRequestError || error instanceof DomainValidationError) {
          return sendJson(response, 400, { error: error.message });
        }
        throw error;
      }
      return;
    }
    if (url.pathname === '/beta/chos' && request.method === 'GET') return redirect(response, '/beta/chos/');
    if (url.pathname.startsWith('/beta/chos/') && request.method === 'GET') {
      await serveChosDocument(request, response, url.pathname);
      return;
    }
    if (url.pathname === '/beta/konto' && request.method === 'GET') {
      const auth = await authenticatedUser(request);
      if (!auth) return redirect(response, '/beta/login', cookie(sessionCookie, '', { maxAge: 0 }));
      send(response, 200, accountPage(request, auth.user, { message: accountStatus(url.searchParams.get('status')) }));
      return;
    }
    if (url.pathname === '/beta/' && request.method === 'GET') {
      const auth = await authenticatedUser(request);
      if (!auth) return redirect(response, '/beta/login', cookie(sessionCookie, '', { maxAge: 0 }));
      if (auth.user.role === 'owner') return redirect(response, '/beta/chos/');
      send(response, 200, dashboardPage(request, auth.user, auth.phase));
      return;
    }
    if (url.pathname === '/beta/onboarding' && request.method === 'GET') {
      const auth = await authenticatedUser(request);
      if (!auth) return redirect(response, '/beta/login', cookie(sessionCookie, '', { maxAge: 0 }));
      if (auth.user.role === 'owner') return redirect(response, '/beta/chos/');
      send(response, 200, onboardingPage(request, auth.user, auth.phase));
      return;
    }
    if (url.pathname === '/beta') return redirect(response, '/beta/');
    send(response, 404, page({ title: 'Nicht gefunden', body: '<h1>Diese Seite wurde nicht gefunden.</h1><p><a href="/beta/">Zum Beta-Bereich</a></p>' }));
  } catch (error) {
    console.error(`[beta-portal] ${error.message}`);
    send(response, 500, page({ title: 'Technischer Fehler', body: '<section class="auth-card"><h1>Der Bereich ist gerade nicht erreichbar.</h1><p>Bitte versuche es später erneut oder melde dich direkt bei Christian.</p></section>' }));
  }
});

server.listen(port, host, () => {
  console.log(`[beta-portal] listening on ${port}`);
});

function cleanup() {
  server.close(() => process.exit(0));
}
process.on('SIGTERM', cleanup);
process.on('SIGINT', cleanup);
