import assert from 'node:assert/strict';
import { execFile as execFileCallback } from 'node:child_process';
import { randomUUID } from 'node:crypto';
import { mkdtemp, readFile, readdir, rm } from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';
import { promisify } from 'node:util';
import { accessPhase, addDays, randomToken, tokenDigest } from './lib/security.mjs';
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

const execFile = promisify(execFileCallback);

function hidden(html, name) {
  const match = html.match(new RegExp(`name="${name}" value="([^"]+)"`));
  assert.ok(match, `Feld ${name} fehlt`);
  return match[1];
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
      const response = await fetch(`${origin}/beta/health`);
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

test('Workspace und öffentliche ChOS-Seite teilen die Marken- und Einstiegskontrakte', async () => {
  const [workspaceHtml, workspaceCss, pageTemplate, siteCss] = await Promise.all([
    readFile(new URL('./public/workspace/index.html', import.meta.url), 'utf8'),
    readFile(new URL('./public/workspace/workspace.css', import.meta.url), 'utf8'),
    readFile(new URL('../light-modules/meine-website/templates/pages/home.ftl', import.meta.url), 'utf8'),
    readFile(new URL('../light-modules/meine-website/webresources/css/site.css', import.meta.url), 'utf8')
  ]);

  assert.match(workspaceHtml, /class="workspace-brand__mark"/);
  assert.match(workspaceHtml, /id="workspace-main"/);
  assert.match(workspaceHtml, /workspace\.css\?v=20260824-2/);
  assert.match(workspaceHtml, /Lokale Speicherung zuerst/);
  assert.doesNotMatch(workspaceHtml, /\bMVP\b|Magnolia/i);
  assert.match(workspaceCss, /--brand-dark:\s*#0d3f29/);
  assert.match(workspaceCss, /--accent:\s*#d8ef77/);
  assert.match(workspaceCss, /prefers-reduced-motion/);
  assert.match(pageTemplate, /class="chos-workspace-entry"/);
  assert.match(pageTemplate, /href="\/beta\/workspace"/);
  assert.match(pageTemplate, /site\.css\?v=20260824-2/);
  assert.match(siteCss, /\.chos-workspace-entry/);
  assert.match(siteCss, /\.chos-workspace-entry__copy \.eyebrow\s*\{\s*color:\s*var\(--brand\)/);
  assert.match(siteCss, /\.site-workspace-link/);
});

test('Einladung, Passwortvergabe, Login und Logout funktionieren', async (context) => {
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
  const invitationUrl = `${origin}/beta/einladung?token=${token}`;
  const { server } = await import('./server.mjs');
  context.after(async () => {
    await new Promise((resolve) => server.close(resolve));
    await rm(directory, { recursive: true, force: true });
  });
  await waitForServer(origin);

  let response = await fetch(`${origin}/beta/health`);
  assert.deepEqual(await response.json(), { status: 'ok', version: '0.3.0' });

  response = await fetch(`${origin}/beta/`, { redirect: 'manual' });
  assert.equal(response.status, 303);
  assert.equal(response.headers.get('location'), '/beta/login');

  response = await fetch(invitationUrl, { redirect: 'manual' });
  assert.equal(response.status, 303);
  const inviteCookie = firstCookie(response, 'chos_beta_invite');

  response = await fetch(`${origin}/beta/einladung`, { headers: { cookie: inviteCookie } });
  const invitationHtml = await response.text();
  assert.equal(response.status, 200);
  assert.match(invitationHtml, /Beta Person/);
  const invitationNonce = hidden(invitationHtml, 'nonce');

  response = await fetch(`${origin}/beta/einladung`, {
    method: 'POST',
    redirect: 'manual',
    headers: { cookie: inviteCookie, 'content-type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({ nonce: invitationNonce, password: 'Eine-sehr-lange-Test-Passphrase!', confirm: 'Eine-sehr-lange-Test-Passphrase!', accepted: 'yes' })
  });
  assert.equal(response.status, 303);
  const sessionCookie = firstCookie(response, 'chos_beta_session');

  response = await fetch(`${origin}/beta/`, { headers: { cookie: sessionCookie } });
  const dashboard = await response.text();
  assert.equal(response.status, 200);
  assert.match(dashboard, /Willkommen, Beta Person/);
  assert.match(dashboard, /8 Stunden verfügbar/);

  response = await fetch(`${origin}/beta/onboarding`, { headers: { cookie: sessionCookie } });
  assert.equal(response.status, 200);
  assert.match(await response.text(), /Diagnose vor Eingriff/);

  response = await fetch(`${origin}/beta/`, { headers: { cookie: sessionCookie } });
  const logoutNonce = hidden(await response.text(), 'nonce');
  response = await fetch(`${origin}/beta/logout`, {
    method: 'POST',
    redirect: 'manual',
    headers: { cookie: sessionCookie, 'content-type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({ nonce: logoutNonce })
  });
  assert.equal(response.status, 303);

  response = await fetch(`${origin}/beta/login`);
  const loginHtml = await response.text();
  const loginNonce = hidden(loginHtml, 'nonce');
  response = await fetch(`${origin}/beta/login`, {
    method: 'POST',
    redirect: 'manual',
    headers: { 'content-type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({ nonce: loginNonce, email: 'beta@beispiel.de', password: 'Eine-sehr-lange-Test-Passphrase!' })
  });
  assert.equal(response.status, 303);
  const participantLoginCookie = firstCookie(response, 'chos_beta_session');

  response = await fetch(`${origin}/beta/chos/`, { headers: { cookie: participantLoginCookie } });
  assert.equal(response.status, 403);

  response = await fetch(`${origin}/beta/workspace/`, { headers: { cookie: participantLoginCookie } });
  assert.equal(response.status, 200);
  assert.match(await response.text(), /ChOS Arbeitsraum/);

  response = await fetch(`${origin}/beta/workspace/assets/app.mjs`, { headers: { cookie: participantLoginCookie } });
  assert.equal(response.status, 200);
  assert.match(response.headers.get('content-type'), /text\/javascript/);

  response = await fetch(`${origin}/beta/api/workspace/bootstrap`, { headers: { cookie: participantLoginCookie } });
  const bootstrap = await response.json();
  assert.equal(response.status, 200);
  assert.equal(bootstrap.canWrite, true);
  assert.equal(bootstrap.boundaries.userData, 'local-first-workspace');

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
  response = await fetch(`${origin}/beta/api/workspace/sync`, {
    method: 'POST',
    headers: syncHeaders,
    body: JSON.stringify({ protocolVersion: 1, after: 0, operations: [caseOperation, itemOperation] })
  });
  const firstSync = await response.json();
  assert.equal(response.status, 200);
  assert.equal(firstSync.cursor, 2);
  assert.equal(firstSync.operations.length, 2);
  assert.deepEqual(firstSync.acceptedOperationIds, [caseOperation.id, itemOperation.id]);

  response = await fetch(`${origin}/beta/api/workspace/sync`, {
    method: 'POST',
    headers: syncHeaders,
    body: JSON.stringify({ protocolVersion: 1, after: 2, operations: [caseOperation, itemOperation] })
  });
  const repeatedSync = await response.json();
  assert.equal(response.status, 200);
  assert.equal(repeatedSync.cursor, 2);
  assert.equal(repeatedSync.operations.length, 0);

  response = await fetch(`${origin}/beta/api/workspace/sync`, {
    method: 'POST',
    headers: syncHeaders,
    body: JSON.stringify({ protocolVersion: 1, after: 99, operations: [] })
  });
  const resetSync = await response.json();
  assert.equal(resetSync.reset, true);
  assert.equal(resetSync.operations.length, 2);

  response = await fetch(`${origin}/beta/api/workspace/sync`, {
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
    const participant = store.users.find((user) => user.email === 'beta@beispiel.de');
    participant.activeUntil = new Date(Date.now() - 60_000).toISOString();
    participant.readUntil = addDays(new Date(), 1);
  });
  response = await fetch(`${origin}/beta/api/workspace/sync`, {
    method: 'POST',
    headers: syncHeaders,
    body: JSON.stringify({
      protocolVersion: 1,
      after: 2,
      operations: [createCaseOperation({ actorId, title: 'Nicht mehr schreibbar' })]
    })
  });
  assert.equal(response.status, 403);

  response = await fetch(`${origin}/beta/einladung?token=${ownerToken}`, { redirect: 'manual' });
  assert.equal(response.status, 303);
  const ownerInviteCookie = firstCookie(response, 'chos_beta_invite');
  response = await fetch(`${origin}/beta/einladung`, { headers: { cookie: ownerInviteCookie } });
  const ownerInvitationHtml = await response.text();
  assert.match(ownerInvitationHtml, /Deinen ChOS-Zugang einrichten/);
  const ownerNonce = hidden(ownerInvitationHtml, 'nonce');
  response = await fetch(`${origin}/beta/einladung`, {
    method: 'POST',
    redirect: 'manual',
    headers: { cookie: ownerInviteCookie, 'content-type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({ nonce: ownerNonce, password: 'Owner-Testpasswort-2026!', confirm: 'Owner-Testpasswort-2026!', accepted: 'yes' })
  });
  assert.equal(response.status, 303);
  assert.equal(response.headers.get('location'), '/beta/chos/');
  const ownerSessionCookie = firstCookie(response, 'chos_beta_session');

  response = await fetch(`${origin}/beta/chos/`, { headers: { cookie: ownerSessionCookie } });
  const chosHome = await response.text();
  assert.equal(response.status, 200);
  assert.match(chosHome, /ChOS 0.6 Beta.1/);
  assert.match(chosHome, /href="\/beta\/konto"/);
  assert.match(response.headers.get('content-security-policy'), /script-src 'self'/);

  response = await fetch(`${origin}/beta/chos/docs/05-entscheidungsmodell.html`, { headers: { cookie: ownerSessionCookie } });
  assert.equal(response.status, 200);
  assert.match(await response.text(), /Entscheidungsmodell/);

  response = await fetch(`${origin}/beta/chos/assets/app.js`, { headers: { cookie: ownerSessionCookie } });
  assert.equal(response.status, 200);
  assert.match(response.headers.get('content-type'), /text\/javascript/);

  response = await fetch(`${origin}/beta/konto`, { headers: { cookie: ownerSessionCookie } });
  assert.equal(response.status, 200);
  assert.match(await response.text(), /Persönlicher Zugriff/);

  const stored = JSON.parse(await readFile(path.join(directory, 'accounts.json'), 'utf8'));
  assert.equal(stored.users.length, 2);
  assert.equal(stored.users[0].passwordHash.includes('Eine-sehr-lange'), false);
  assert.equal(stored.users[0].inviteTokenHash, null);
  assert.equal(stored.users[1].role, 'owner');
  assert.equal(stored.users[1].inviteTokenHash, null);

  const removal = await execFile(process.execPath, [
    fileURLToPath(new URL('./admin.mjs', import.meta.url)),
    'remove',
    '--email=beta@beispiel.de',
    '--confirm=beta@beispiel.de'
  ], { env: { ...process.env, BETA_DATA_DIR: directory } });
  assert.match(removal.stdout, /Konto und serverseitiger Workspace/);
  const afterRemoval = JSON.parse(await readFile(path.join(directory, 'accounts.json'), 'utf8'));
  assert.equal(afterRemoval.users.length, 1);
  assert.equal((await readdir(path.join(directory, 'workspaces'))).filter((name) => name.endsWith('.json')).length, 0);

  response = await fetch(`${origin}/beta/api/workspace/bootstrap`, { headers: { cookie: participantLoginCookie } });
  assert.equal(response.status, 401);
});
