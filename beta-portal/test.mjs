import assert from 'node:assert/strict';
import { randomUUID } from 'node:crypto';
import { mkdtemp, readFile, readdir, rm } from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import test from 'node:test';
import { accessPhase, addDays, randomToken, tokenDigest, validateEmail } from './lib/security.mjs';
import { addWorkItemOperation, createCaseOperation } from './lib/chos-domain.mjs';
import {
  createLocalRuntime,
  createSyncRequest,
  enqueueLocalOperation,
  mergeSyncResponse,
  workspaceView
} from './lib/local-first-workspace.mjs';
import {
  AI_DATA_CLASSES,
  AiUnavailableError,
  createAiProvider,
  createDisabledAiProvider,
  createHybridAiService
} from './lib/ai-runtime.mjs';
import {
  buildKnowledgeIndexFromHtml,
  rankRelatedContent,
  validateKnowledgeIndex
} from './lib/related-content.mjs';

function hidden(html, name) {
  const match = html.match(new RegExp(`name="${name}" value="([^"]+)"`));
  assert.ok(match, `Feld ${name} fehlt`);
  return match[1];
}

function formNonce(html, action) {
  const escaped = action.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  const match = html.match(new RegExp(`<form[^>]+action="${escaped}"[\\s\\S]*?</form>`));
  assert.ok(match, `Formular ${action} fehlt`);
  return hidden(match[0], 'nonce');
}

function firstCookie(response, name) {
  const values = response.headers.getSetCookie ? response.headers.getSetCookie() : [response.headers.get('set-cookie')];
  const target = values.find((value) => value?.startsWith(`${name}=`));
  assert.ok(target, `Cookie ${name} fehlt`);
  return target.split(';')[0];
}

async function waitForServer(origin) {
  for (let count = 0; count < 60; count += 1) {
    try {
      const response = await fetch(`${origin}/workspace/health`);
      if (response.ok) return;
    } catch {}
    await new Promise((resolve) => setTimeout(resolve, 50));
  }
  throw new Error('Testserver ist nicht gestartet.');
}

test('Zugriffsphasen werden zeitbasiert ermittelt', () => {
  const base = { passwordHash: 'x', status: 'active', startAt: '2026-01-01T00:00:00.000Z', activeUntil: '2026-03-02T00:00:00.000Z', readUntil: '2026-04-01T00:00:00.000Z' };
  assert.equal(accessPhase(base, new Date('2025-12-31T00:00:00Z')), 'scheduled');
  assert.equal(accessPhase(base, new Date('2026-02-01T00:00:00Z')), 'active');
  assert.equal(accessPhase(base, new Date('2026-03-15T00:00:00Z')), 'readonly');
  assert.equal(accessPhase(base, new Date('2026-04-02T00:00:00Z')), 'expired');
});

test('E-Mail-Adressen werden für die Kontopflege streng geprüft', () => {
  assert.equal(validateEmail('person@beispiel.de'), null);
  assert.match(validateEmail('keine-adresse'), /gültige E-Mail-Adresse/);
  assert.match(validateEmail(`x@${'a'.repeat(250)}.de`), /gültige E-Mail-Adresse/);
});

test('Local-first Runtime hält Änderungen lokal und bestätigt sie nach dem Sync', () => {
  const actorId = 'device_test_0001';
  let runtime = createLocalRuntime(actorId);
  const createCase = createCaseOperation({ actorId, title: 'Team Alpha', context: 'Neue Verantwortung' });
  const addObservation = addWorkItemOperation({
    actorId,
    caseId: createCase.entityId,
    kind: 'observation',
    text: 'Entscheidungen werden vertagt.'
  });
  runtime = enqueueLocalOperation(runtime, createCase);
  runtime = enqueueLocalOperation(runtime, addObservation);
  assert.equal(createSyncRequest(runtime).operations.length, 2);
  assert.equal(workspaceView(runtime).cases[createCase.entityId].title, 'Team Alpha');

  runtime = mergeSyncResponse(runtime, {
    protocolVersion: 1,
    cursor: 2,
    reset: false,
    operations: [{ ...createCase, cursor: 1 }, { ...addObservation, cursor: 2 }],
    acceptedOperationIds: [createCase.id, addObservation.id],
    serverTime: new Date().toISOString()
  });
  assert.equal(runtime.outbox.length, 0);
  assert.equal(runtime.cursor, 2);
  assert.equal(Object.keys(workspaceView(runtime).workItems).length, 1);
});

test('Hybrid-AI bevorzugt lokal und schützt private Daten vor Cloud-Fallback', async () => {
  let cloudCalls = 0;
  const local = createAiProvider({ id: 'local-test', available: async () => true, run: async () => 'lokal' });
  const cloud = createAiProvider({ id: 'cloud-test', available: async () => true, run: async () => { cloudCalls += 1; return 'cloud'; } });
  let service = createHybridAiService({ local, cloud });
  const localResult = await service.run({ task: 'zusammenfassen', dataClass: AI_DATA_CLASSES.PRIVATE, input: {} });
  assert.equal(localResult.provider, 'local-test');
  assert.equal(cloudCalls, 0);

  service = createHybridAiService({ local: createDisabledAiProvider('local-test'), cloud });
  await assert.rejects(
    service.run({ task: 'zusammenfassen', dataClass: AI_DATA_CLASSES.PRIVATE, input: {}, allowCloud: true }),
    (error) => error instanceof AiUnavailableError && error.reason === 'local-required'
  );
  assert.equal(cloudCalls, 0);
  const cloudResult = await service.run({ task: 'öffentlichen Text glätten', dataClass: AI_DATA_CLASSES.SHAREABLE, input: {}, allowCloud: true });
  assert.equal(cloudResult.provider, 'cloud-test');
  assert.equal(cloudCalls, 1);
});

test('ChOS-Inhalte werden lokal und nachvollziehbar zum Arbeitskontext zugeordnet', async () => {
  const html = await readFile(new URL('./chos-reader/index.html', import.meta.url), 'utf8');
  const entries = buildKnowledgeIndexFromHtml(html);
  assert.ok(entries.length > 50);
  const index = validateKnowledgeIndex({ schemaVersion: 1, version: 'test-index-000001', entries });
  const recommendations = rankRelatedContent(index, {
    title: 'Neuer Verantwortungsbereich',
    context: 'Ich übernehme ein neues Produktteam.',
    items: [{ text: 'Die Entscheidungswege und Rollen sind unklar.' }]
  });
  assert.equal(recommendations.length, 3);
  assert.ok(recommendations.some((entry) => entry.path === 'docs/pb-001-team-uebernehmen.html'));
  assert.ok(recommendations.some((entry) => entry.path === 'docs/05-entscheidungsmodell.html'));
  assert.ok(recommendations.every((entry) => entry.path.startsWith('docs/')));
});

test('Workspace und öffentliche ChOS-Seite teilen die Marken- und Einstiegskontrakte', async () => {
  const [workspaceHtml, workspaceCss, workspaceApp, portalCss, serverSource, pageTemplate, siteCss, offlineReader, offlineWorker, ownerBridgeCss] = await Promise.all([
    readFile(new URL('./public/workspace/index.html', import.meta.url), 'utf8'),
    readFile(new URL('./public/workspace/workspace.css', import.meta.url), 'utf8'),
    readFile(new URL('./public/workspace/app.mjs', import.meta.url), 'utf8'),
    readFile(new URL('./public/portal.css', import.meta.url), 'utf8'),
    readFile(new URL('./server.mjs', import.meta.url), 'utf8'),
    readFile(new URL('../light-modules/meine-website/templates/pages/home.ftl', import.meta.url), 'utf8'),
    readFile(new URL('../light-modules/meine-website/webresources/css/site.css', import.meta.url), 'utf8'),
    readFile(new URL('./public/offline-reader.mjs', import.meta.url), 'utf8'),
    readFile(new URL('./public/offline-sw.mjs', import.meta.url), 'utf8'),
    readFile(new URL('./public/owner-bridge.css', import.meta.url), 'utf8')
  ]);

  assert.match(workspaceHtml, /class="workspace-brand__mark"/);
  assert.match(workspaceHtml, /id="workspace-main"/);
  assert.match(workspaceHtml, /workspace\.css\?v=20260825-1/);
  assert.match(workspaceHtml, /Lokale Speicherung zuerst/);
  assert.match(workspaceHtml, /ChOS-Wissen zum Vertiefen/);
  assert.match(workspaceHtml, /href="\/workspace\/konto">Konto/);
  assert.match(workspaceHtml, />Zurück zu ChOS</);
  assert.doesNotMatch(workspaceHtml, /\bMVP\b|Magnolia/i);
  assert.match(workspaceCss, /--brand-dark:\s*#0d3f29/);
  assert.match(workspaceCss, /--accent:\s*#d8ef77/);
  assert.match(workspaceCss, /prefers-reduced-motion/);
  assert.match(workspaceCss, /\.related-card__link/);
  assert.match(workspaceCss, /\.related-card a:focus-visible/);
  assert.match(workspaceApp, /target = '_blank'/);
  assert.match(workspaceApp, /rel = 'noopener'/);
  assert.match(workspaceApp, /rankRelatedContent/);
  assert.match(workspaceApp, /localStorage\.setItem\(KNOWLEDGE_CACHE_KEY/);
  assert.match(workspaceApp, /\/workspace\/api\/workspace\/knowledge-index/);
  assert.match(portalCss, /\.brand\s*\{[^}]*min-height:\s*44px/s);
  assert.match(portalCss, /footer a\s*\{[^}]*min-height:\s*44px/s);
  assert.match(portalCss, /\.portal-nav a\s*\{[^}]*min-height:\s*44px/s);
  assert.match(serverSource, /portal\.css\?v=20260824-3/);
  assert.match(pageTemplate, /class="chos-workspace-entry"/);
  assert.match(pageTemplate, /href="\/workspace\/workspace"/);
  assert.match(pageTemplate, /site\.css\?v=20260824-4/);
  assert.match(siteCss, /\.chos-workspace-entry/);
  assert.match(siteCss, /\.chos-workspace-entry__copy \.eyebrow\s*\{\s*color:\s*var\(--brand\)/);
  assert.doesNotMatch(pageTemplate, /class="site-workspace-link"/);
  assert.match(offlineReader, /\/workspace\/api\/chos\/offline-bundle/);
  assert.match(offlineReader, /await cache\.put/);
  assert.match(offlineReader, /localStorage\.setItem\(KNOWLEDGE_CACHE_KEY/);
  assert.match(offlineReader, /localStorage\.removeItem\(KNOWLEDGE_CACHE_KEY/);
  assert.match(offlineWorker, /response\.redirected \|\| response\.status === 401 \|\| response\.status === 403/);
  assert.match(offlineWorker, /CACHE_PREFIX = 'chos-reader-v1-'/);
  assert.match(offlineWorker, /\/workspace\/api\/chos\/knowledge-index/);
  assert.match(serverSource, /fingerprint\.update\(`portal:\$\{portalVersion\}/);
  assert.match(serverSource, /fingerprint\.update\(ownerBridgeCss\)/);
  assert.match(ownerBridgeCss, /min-height:\s*44px/);
  assert.match(ownerBridgeCss, /flex-wrap:\s*wrap/);
  assert.match(ownerBridgeCss, /chos-offline-panel\[hidden\]/);
});

test('Einladung, Login, Kontopflege, Workspace und Selbstlöschung funktionieren', async (context) => {
  const directory = await mkdtemp(path.join(os.tmpdir(), 'chos-beta-test-'));
  const testPort = 31991;
  const origin = `http://127.0.0.1:${testPort}`;
  const start = new Date().toISOString().slice(0, 10);
  process.env.BETA_DATA_DIR = directory;
  process.env.BETA_PORT = String(testPort);
  process.env.BETA_HOST = '127.0.0.1';
  process.env.BETA_COOKIE_SECURE = 'false';
  process.env.BETA_PUBLIC_ORIGIN = origin;
  const token = randomToken();
  const ownerToken = randomToken();
  const startAt = `${start}T00:00:00.000Z`;
  const { updateStore } = await import('./lib/store.mjs');
  await updateStore((store) => {
    store.users.push({
      id: randomUUID(),
      email: 'beta@beispiel.de',
      name: 'Beta Person',
      status: 'invited',
      passwordHash: null,
      inviteTokenHash: tokenDigest(token),
      inviteExpiresAt: addDays(new Date(), 3),
      startAt,
      activeUntil: addDays(startAt, 60),
      readUntil: addDays(startAt, 90),
      supportHoursTotal: 8,
      supportHoursUsed: 0,
      disabledAt: null,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    });
    store.users.push({
      id: randomUUID(),
      email: 'owner@beispiel.de',
      name: 'Christian Test',
      role: 'owner',
      status: 'invited',
      passwordHash: null,
      inviteTokenHash: tokenDigest(ownerToken),
      inviteExpiresAt: addDays(new Date(), 3),
      startAt: null,
      activeUntil: null,
      readUntil: null,
      supportHoursTotal: 0,
      supportHoursUsed: 0,
      disabledAt: null,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    });
  });
  const invitationUrl = `${origin}/workspace/einladung?token=${token}`;
  const { server } = await import('./server.mjs');
  context.after(async () => {
    await new Promise((resolve) => server.close(resolve));
    await rm(directory, { recursive: true, force: true });
  });
  await waitForServer(origin);
  const legacy = await fetch(`${origin}/beta/konto?status=email`, { redirect: 'manual' });
  assert.equal(legacy.status, 308);
  assert.equal(legacy.headers.get('location'), '/workspace/konto?status=email');
  assert.equal((await fetch(`${origin}/beta/api/workspace/sync`, { method: 'POST' })).status, 409);
  const oldWorker = await fetch(`${origin}/beta/offline-sw.mjs`, { redirect: 'manual' });
  assert.equal(oldWorker.status, 200);
  assert.match(await oldWorker.text(), /registration.unregister/);
  assert.equal(oldWorker.headers.get('service-worker-allowed'), '/beta/');


  let response = await fetch(`${origin}/workspace/health`);
  assert.deepEqual(await response.json(), { status: 'ok', version: '0.6.0' });

  response = await fetch(`${origin}/workspace/`, { redirect: 'manual' });
  assert.equal(response.status, 303);
  assert.equal(response.headers.get('location'), '/workspace/login');

  response = await fetch(invitationUrl, { redirect: 'manual' });
  assert.equal(response.status, 303);
  const inviteCookie = firstCookie(response, 'chos_beta_invite');

  response = await fetch(`${origin}/workspace/einladung`, { headers: { cookie: inviteCookie } });
  const invitationHtml = await response.text();
  assert.equal(response.status, 200);
  assert.match(invitationHtml, /Beta Person/);
  const invitationNonce = hidden(invitationHtml, 'nonce');

  response = await fetch(`${origin}/workspace/einladung`, {
    method: 'POST',
    redirect: 'manual',
    headers: { cookie: inviteCookie, 'content-type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({ nonce: invitationNonce, password: 'Eine-sehr-lange-Test-Passphrase!', confirm: 'Eine-sehr-lange-Test-Passphrase!', accepted: 'yes' })
  });
  assert.equal(response.status, 303);
  const sessionCookie = firstCookie(response, 'chos_beta_session');

  response = await fetch(`${origin}/workspace/`, { headers: { cookie: sessionCookie } });
  const dashboard = await response.text();
  assert.equal(response.status, 200);
  assert.match(dashboard, /Willkommen, Beta Person/);
  assert.match(dashboard, /8 Stunden verfügbar/);

  response = await fetch(`${origin}/workspace/onboarding`, { headers: { cookie: sessionCookie } });
  assert.equal(response.status, 200);
  assert.match(await response.text(), /Diagnose vor Eingriff/);

  response = await fetch(`${origin}/workspace/`, { headers: { cookie: sessionCookie } });
  const logoutNonce = hidden(await response.text(), 'nonce');
  response = await fetch(`${origin}/workspace/logout`, {
    method: 'POST',
    redirect: 'manual',
    headers: { cookie: sessionCookie, 'content-type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({ nonce: logoutNonce })
  });
  assert.equal(response.status, 303);

  response = await fetch(`${origin}/workspace/login`);
  const loginHtml = await response.text();
  const loginNonce = hidden(loginHtml, 'nonce');
  response = await fetch(`${origin}/workspace/login`, {
    method: 'POST',
    redirect: 'manual',
    headers: { 'content-type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({ nonce: loginNonce, email: 'beta@beispiel.de', password: 'Eine-sehr-lange-Test-Passphrase!' })
  });
  assert.equal(response.status, 303);
  let participantLoginCookie = firstCookie(response, 'chos_beta_session');

  response = await fetch(`${origin}/workspace/konto`, { headers: { cookie: participantLoginCookie } });
  let accountHtml = await response.text();
  assert.equal(response.status, 200);
  assert.match(accountHtml, /Konto verwalten/);
  assert.match(accountHtml, /beta@beispiel\.de/);

  response = await fetch(`${origin}/workspace/konto/email`, {
    method: 'POST',
    redirect: 'manual',
    headers: { cookie: participantLoginCookie, 'content-type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({ nonce: formNonce(accountHtml, '/workspace/konto/email'), email: 'beta-neu@beispiel.de', 'current-password': 'Falsches-Testpasswort-2026!' })
  });
  accountHtml = await response.text();
  assert.equal(response.status, 403);
  assert.match(accountHtml, /aktuelle Passwort ist nicht korrekt/);

  response = await fetch(`${origin}/workspace/konto/email`, {
    method: 'POST',
    redirect: 'manual',
    headers: { cookie: participantLoginCookie, 'content-type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({ nonce: formNonce(accountHtml, '/workspace/konto/email'), email: 'owner@beispiel.de', 'current-password': 'Eine-sehr-lange-Test-Passphrase!' })
  });
  accountHtml = await response.text();
  assert.equal(response.status, 409);
  assert.match(accountHtml, /bereits verwendet/);

  response = await fetch(`${origin}/workspace/konto/email`, {
    method: 'POST',
    redirect: 'manual',
    headers: { cookie: participantLoginCookie, 'content-type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({ nonce: formNonce(accountHtml, '/workspace/konto/email'), email: 'beta-neu@beispiel.de', 'current-password': 'Eine-sehr-lange-Test-Passphrase!' })
  });
  assert.equal(response.status, 303);
  assert.equal(response.headers.get('location'), '/workspace/konto?status=email');

  response = await fetch(`${origin}/workspace/konto?status=email`, { headers: { cookie: participantLoginCookie } });
  accountHtml = await response.text();
  assert.match(accountHtml, /E-Mail-Adresse wurde geändert/);
  assert.match(accountHtml, /beta-neu@beispiel\.de/);

  response = await fetch(`${origin}/workspace/konto/passwort`, {
    method: 'POST',
    redirect: 'manual',
    headers: { cookie: participantLoginCookie, 'content-type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({
      nonce: formNonce(accountHtml, '/workspace/konto/passwort'),
      'current-password': 'Eine-sehr-lange-Test-Passphrase!',
      password: 'Neue-sehr-lange-Test-Passphrase!',
      confirm: 'Neue-sehr-lange-Test-Passphrase!'
    })
  });
  assert.equal(response.status, 303);
  assert.equal(response.headers.get('location'), '/workspace/konto?status=password');
  participantLoginCookie = firstCookie(response, 'chos_beta_session');

  response = await fetch(`${origin}/workspace/konto?status=password`, { headers: { cookie: participantLoginCookie } });
  assert.equal(response.status, 200);
  assert.match(await response.text(), /Passwort wurde geändert/);

  response = await fetch(`${origin}/workspace/login`);
  let changedLoginHtml = await response.text();
  response = await fetch(`${origin}/workspace/login`, {
    method: 'POST',
    redirect: 'manual',
    headers: { 'content-type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({ nonce: hidden(changedLoginHtml, 'nonce'), email: 'beta@beispiel.de', password: 'Eine-sehr-lange-Test-Passphrase!' })
  });
  assert.equal(response.status, 401);

  response = await fetch(`${origin}/workspace/login`);
  changedLoginHtml = await response.text();
  response = await fetch(`${origin}/workspace/login`, {
    method: 'POST',
    redirect: 'manual',
    headers: { 'content-type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({ nonce: hidden(changedLoginHtml, 'nonce'), email: 'beta-neu@beispiel.de', password: 'Neue-sehr-lange-Test-Passphrase!' })
  });
  assert.equal(response.status, 303);
  participantLoginCookie = firstCookie(response, 'chos_beta_session');

  response = await fetch(`${origin}/workspace/chos/`, { headers: { cookie: participantLoginCookie } });
  assert.equal(response.status, 403);

  response = await fetch(`${origin}/workspace/workspace/`, { headers: { cookie: participantLoginCookie } });
  assert.equal(response.status, 200);
  assert.match(await response.text(), /ChOS Arbeitsraum/);

  response = await fetch(`${origin}/workspace/workspace/assets/app.mjs`, { headers: { cookie: participantLoginCookie } });
  assert.equal(response.status, 200);
  assert.match(response.headers.get('content-type'), /text\/javascript/);

  response = await fetch(`${origin}/workspace/api/workspace/bootstrap`, { headers: { cookie: participantLoginCookie } });
  const bootstrap = await response.json();
  assert.equal(response.status, 200);
  assert.equal(bootstrap.canWrite, true);
  assert.equal(bootstrap.boundaries.userData, 'local-first-workspace');
  assert.equal(bootstrap.knowledge.available, false);

  response = await fetch(`${origin}/workspace/api/workspace/knowledge-index`, { headers: { cookie: participantLoginCookie } });
  assert.equal(response.status, 403);

  const actorId = 'browser_test_0001';
  const caseOperation = createCaseOperation({ actorId, title: 'Neuer Verantwortungsbereich', context: 'Produktteam' });
  const itemOperation = addWorkItemOperation({
    actorId,
    caseId: caseOperation.entityId,
    kind: 'assumption',
    text: 'Entscheidungswege sind unklar.'
  });
  const syncHeaders = {
    cookie: participantLoginCookie,
    'content-type': 'application/json',
    'x-chos-client': 'workspace-v1'
  };
  response = await fetch(`${origin}/workspace/api/workspace/sync`, {
    method: 'POST',
    headers: syncHeaders,
    body: JSON.stringify({ protocolVersion: 1, after: 0, operations: [caseOperation, itemOperation] })
  });
  const firstSync = await response.json();
  assert.equal(response.status, 200);
  assert.equal(firstSync.cursor, 2);
  assert.equal(firstSync.operations.length, 2);
  assert.deepEqual(firstSync.acceptedOperationIds, [caseOperation.id, itemOperation.id]);

  response = await fetch(`${origin}/workspace/api/workspace/sync`, {
    method: 'POST',
    headers: syncHeaders,
    body: JSON.stringify({ protocolVersion: 1, after: 2, operations: [caseOperation, itemOperation] })
  });
  const repeatedSync = await response.json();
  assert.equal(response.status, 200);
  assert.equal(repeatedSync.cursor, 2);
  assert.equal(repeatedSync.operations.length, 0);

  response = await fetch(`${origin}/workspace/api/workspace/sync`, {
    method: 'POST',
    headers: syncHeaders,
    body: JSON.stringify({ protocolVersion: 1, after: 99, operations: [] })
  });
  const resetSync = await response.json();
  assert.equal(resetSync.reset, true);
  assert.equal(resetSync.operations.length, 2);

  response = await fetch(`${origin}/workspace/api/workspace/sync`, {
    method: 'POST',
    headers: syncHeaders,
    body: JSON.stringify({
      protocolVersion: 1,
      after: 2,
      operations: [{ ...caseOperation, payload: { ...caseOperation.payload, title: 'Abweichender Inhalt' } }]
    })
  });
  assert.equal(response.status, 400);

  await updateStore((store) => {
    const participant = store.users.find((user) => user.email === 'beta-neu@beispiel.de');
    participant.activeUntil = new Date(Date.now() - 60_000).toISOString();
    participant.readUntil = addDays(new Date(), 1);
  });
  response = await fetch(`${origin}/workspace/api/workspace/sync`, {
    method: 'POST',
    headers: syncHeaders,
    body: JSON.stringify({
      protocolVersion: 1,
      after: 2,
      operations: [createCaseOperation({ actorId, title: 'Nicht mehr schreibbar' })]
    })
  });
  assert.equal(response.status, 403);

  response = await fetch(`${origin}/workspace/einladung?token=${ownerToken}`, { redirect: 'manual' });
  assert.equal(response.status, 303);
  const ownerInviteCookie = firstCookie(response, 'chos_beta_invite');
  response = await fetch(`${origin}/workspace/einladung`, { headers: { cookie: ownerInviteCookie } });
  const ownerInvitationHtml = await response.text();
  assert.match(ownerInvitationHtml, /Deinen ChOS-Zugang einrichten/);
  const ownerNonce = hidden(ownerInvitationHtml, 'nonce');
  response = await fetch(`${origin}/workspace/einladung`, {
    method: 'POST',
    redirect: 'manual',
    headers: { cookie: ownerInviteCookie, 'content-type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({ nonce: ownerNonce, password: 'Owner-Testpasswort-2026!', confirm: 'Owner-Testpasswort-2026!', accepted: 'yes' })
  });
  assert.equal(response.status, 303);
  assert.equal(response.headers.get('location'), '/workspace/chos/');
  const ownerSessionCookie = firstCookie(response, 'chos_beta_session');

  response = await fetch(`${origin}/workspace/api/workspace/bootstrap`, { headers: { cookie: ownerSessionCookie } });
  const ownerBootstrap = await response.json();
  assert.equal(response.status, 200);
  assert.equal(ownerBootstrap.knowledge.available, true);
  assert.equal(ownerBootstrap.knowledge.processing, 'local');

  response = await fetch(`${origin}/workspace/api/workspace/knowledge-index`, { headers: { cookie: ownerSessionCookie } });
  const knowledgeIndex = await response.json();
  assert.equal(response.status, 200);
  assert.equal(knowledgeIndex.schemaVersion, 1);
  assert.match(knowledgeIndex.version, /^[a-f0-9]{16}$/);
  assert.ok(knowledgeIndex.entries.length > 50);
  assert.ok(knowledgeIndex.entries.some((entry) => entry.path === 'docs/05-entscheidungsmodell.html'));

  response = await fetch(`${origin}/workspace/chos/`, { headers: { cookie: ownerSessionCookie } });
  const chosHome = await response.text();
  assert.equal(response.status, 200);
  assert.match(chosHome, /ChOS 0.6 Beta.1/);
  assert.match(chosHome, /href="\/workspace\/workspace\/">Workspace<\/a>/);
  assert.match(chosHome, /href="\/workspace\/konto"/);
  assert.match(chosHome, /data-offline-reader/);
  assert.match(chosHome, /Aktuellen Stand speichern/);
  assert.match(chosHome, /offline-reader\.mjs/);
  assert.match(response.headers.get('content-security-policy'), /script-src 'self'/);
  assert.match(response.headers.get('content-security-policy'), /worker-src 'self'/);

  response = await fetch(`${origin}/workspace/api/chos/offline-manifest`, { headers: { cookie: ownerSessionCookie } });
  const offlineManifest = await response.json();
  assert.equal(response.status, 200);
  assert.equal(offlineManifest.schemaVersion, 1);
  assert.match(offlineManifest.version, /^[a-f0-9]{16}$/);
  assert.ok(offlineManifest.fileCount > 80);
  assert.ok(offlineManifest.sizeBytes > 2 * 1024 * 1024);
  assert.ok(offlineManifest.urls.includes('/workspace/chos/'));
  assert.ok(offlineManifest.urls.includes('/workspace/assets/offline-reader.mjs'));
  assert.ok(offlineManifest.urls.includes('/workspace/api/chos/knowledge-index'));

  response = await fetch(`${origin}/workspace/api/chos/offline-bundle`, { headers: { cookie: ownerSessionCookie } });
  const offlineBundle = await response.json();
  assert.equal(response.status, 200);
  assert.equal(offlineBundle.schemaVersion, 1);
  assert.equal(offlineBundle.manifest.version, offlineManifest.version);
  assert.equal(offlineBundle.files.length, offlineManifest.fileCount);
  assert.match(Buffer.from(offlineBundle.files.find((file) => file.url === '/workspace/chos/').body, 'base64').toString('utf8'), /data-offline-reader/);
  assert.deepEqual(
    JSON.parse(Buffer.from(offlineBundle.files.find((file) => file.url === '/workspace/api/chos/knowledge-index').body, 'base64').toString('utf8')),
    knowledgeIndex
  );

  response = await fetch(`${origin}/workspace/api/chos/offline-manifest`, { headers: { cookie: participantLoginCookie } });
  assert.equal(response.status, 403);

  response = await fetch(`${origin}/workspace/api/chos/offline-bundle`, { headers: { cookie: participantLoginCookie } });
  assert.equal(response.status, 403);

  response = await fetch(`${origin}/workspace/offline-sw.mjs`);
  assert.equal(response.status, 200);
  assert.match(response.headers.get('service-worker-allowed'), /\/workspace\//);
  assert.match(response.headers.get('cache-control'), /no-cache/);

  response = await fetch(`${origin}/workspace/assets/offline-reader.mjs`);
  assert.equal(response.status, 200);
  assert.match(response.headers.get('content-type'), /text\/javascript/);

  response = await fetch(`${origin}/workspace/chos/docs/05-entscheidungsmodell.html`, { headers: { cookie: ownerSessionCookie } });
  assert.equal(response.status, 200);
  assert.match(await response.text(), /Entscheidungsmodell/);

  response = await fetch(`${origin}/workspace/chos/assets/app.js`, { headers: { cookie: ownerSessionCookie } });
  assert.equal(response.status, 200);
  assert.match(response.headers.get('content-type'), /text\/javascript/);

  response = await fetch(`${origin}/workspace/konto`, { headers: { cookie: ownerSessionCookie } });
  assert.equal(response.status, 200);
  assert.match(await response.text(), /Persönlicher Zugriff/);

  const stored = JSON.parse(await readFile(path.join(directory, 'accounts.json'), 'utf8'));
  assert.equal(stored.users.length, 2);
  assert.equal(stored.users[0].passwordHash.includes('Eine-sehr-lange'), false);
  assert.equal(stored.users[0].inviteTokenHash, null);
  assert.equal(stored.users[1].role, 'owner');
  assert.equal(stored.users[1].inviteTokenHash, null);

  response = await fetch(`${origin}/workspace/konto`, { headers: { cookie: participantLoginCookie } });
  accountHtml = await response.text();
  response = await fetch(`${origin}/workspace/konto/loeschen`, {
    method: 'POST',
    redirect: 'manual',
    headers: { cookie: participantLoginCookie, 'content-type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({
      nonce: formNonce(accountHtml, '/workspace/konto/loeschen'),
      'current-password': 'Neue-sehr-lange-Test-Passphrase!',
      'confirm-email': 'beta-neu@beispiel.de'
    })
  });
  assert.equal(response.status, 303);
  assert.equal(response.headers.get('location'), '/workspace/konto-geloescht');
  const deletedWorkspaceCookie = firstCookie(response, 'chos_beta_deleted_workspace');

  response = await fetch(`${origin}/workspace/konto-geloescht`, { headers: { cookie: deletedWorkspaceCookie } });
  const deletedHtml = await response.text();
  assert.equal(response.status, 200);
  assert.match(deletedHtml, /Dein Konto wurde gelöscht/);
  assert.match(deletedHtml, /account-delete\.mjs/);
  assert.match(deletedHtml, /offline gespeicherte ChOS-Inhalte/);

  response = await fetch(`${origin}/workspace/assets/account-delete.mjs`);
  assert.equal(response.status, 200);
  assert.match(response.headers.get('content-type'), /text\/javascript/);
  const accountDeleteScript = await response.text();
  assert.match(accountDeleteScript, /chos-reader-v1-/);
  assert.match(accountDeleteScript, /chos:published-knowledge-index:v1/);

  const afterRemoval = JSON.parse(await readFile(path.join(directory, 'accounts.json'), 'utf8'));
  assert.equal(afterRemoval.users.length, 1);
  assert.equal((await readdir(path.join(directory, 'workspaces'))).filter((name) => name.endsWith('.json')).length, 0);

  response = await fetch(`${origin}/workspace/api/workspace/bootstrap`, { headers: { cookie: participantLoginCookie } });
  assert.equal(response.status, 401);
});
