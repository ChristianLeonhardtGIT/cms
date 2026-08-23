import assert from 'node:assert/strict';
import { randomUUID } from 'node:crypto';
import { mkdtemp, readFile, rm } from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import test from 'node:test';
import { accessPhase, addDays, randomToken, tokenDigest } from './lib/security.mjs';


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

  let response = await fetch(`${origin}/beta/`, { redirect: 'manual' });
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
});
