import test from 'node:test';
import assert from 'node:assert/strict';
import { mkdtemp, rm, readFile, stat } from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import { SparringRepository, POLICY_VERSION, INTAKE_FIELDS, engagementEnd } from './lib/sparring.mjs';
import { sparringDetail } from './lib/sparring-ui.mjs';
const coach = { id: 'coach', role: 'owner' };
const client = { id: 'client' };
const stranger = { id: 'stranger' };
const otherCoach = { id: 'other-coach', role: 'owner' };
const intake = { ...Object.fromEntries(Object.keys(INTAKE_FIELDS).map(k => [k, 'Abstrahierter Kontext'])), accepted: POLICY_VERSION, business: 'yes' };

test('Berlin-Kalendertage berücksichtigen Wochenende und beide Zeitumstellungen', () => {
  assert.equal(engagementEnd(new Date('2026-09-07T08:00:00Z')), '2026-09-11T22:00:00.000Z');
  assert.equal(engagementEnd(new Date('2026-09-06T08:00:00Z')), '2026-09-11T22:00:00.000Z');
  assert.equal(engagementEnd(new Date('2026-03-27T08:00:00Z')), '2026-04-02T22:00:00.000Z');
  assert.equal(engagementEnd(new Date('2026-10-23T08:00:00Z')), '2026-10-29T23:00:00.000Z');
});

test('Sparring erzwingt Teilnehmer, Status, idempotente Nachrichten und echte Löschung', async t => {
  const dir = await mkdtemp(path.join(os.tmpdir(), 'sparring-'));
  t.after(() => rm(dir, { recursive: true, force: true }));
  const repo = new SparringRepository(dir);
  await assert.rejects(repo.create(client, stranger), { status: 403 });
  const e = await repo.create(coach, client);
  for (const actor of [stranger, otherCoach]) {
    assert.deepEqual(await repo.list(actor), []);
    await assert.rejects(repo.get(e.id, actor), { status: 404 });
    await assert.rejects(repo.act(e.id, actor, 'message', { body: 'Fremdzugriff' }), { status: 404 });
  }
  await assert.rejects(repo.act(e.id, client, 'start'), { status: 403 });
  await assert.rejects(repo.act(e.id, client, 'intake', { ...intake, accepted: 'old' }), { status: 400 });
  await assert.rejects(repo.act(e.id, client, 'intake', { ...intake, business: 'no' }), { status: 400 });
  const ready = await repo.act(e.id, client, 'intake', intake);
  assert.equal(ready.policyVersion, POLICY_VERSION);
  const now = new Date('2026-09-07T08:00:00Z');
  await repo.act(e.id, coach, 'start', {}, now);
  const input = { body: '<script>private marker</script>', requestId: 'unique-request-id-123' };
  await Promise.all([repo.act(e.id, client, 'message', input, now), repo.act(e.id, client, 'message', input, now)]);
  assert.equal((await repo.get(e.id, client)).messages.length, 1);
  await assert.rejects(repo.act(e.id, client, 'message', { body: 'x'.repeat(10001), requestId: 'other-request-id-123' }, now), { status: 400 });
  const current = await repo.get(e.id, coach);
  assert.doesNotMatch(JSON.stringify(current.notification), /private marker/);
  await repo.act(e.id, coach, 'read', { messageId: current.messages[0].id }, now);
  assert.deepEqual((await repo.get(e.id, coach)).notification, {});
  const html = sparringDetail(current, client, { escapeHtml: s => String(s).replaceAll('<', '&lt;').replaceAll('>', '&gt;'), nonce: 'test', requestId: 'test' });
  assert.doesNotMatch(html, /<script>private marker/);
  assert.match(html, /&lt;script&gt;private marker/);
  await repo.act(e.id, coach, 'complete', {}, now);
  await assert.rejects(repo.act(e.id, client, 'message', input, now), { status: 403 });
  await repo.purge([coach.id, client.id], new Date('2026-10-08T00:00:00Z'));
  await repo.purge([coach.id, client.id], new Date('2026-10-08T00:00:00Z'));
  assert.deepEqual(await repo.list(client), []);
  assert.doesNotMatch(await readFile(repo.file, 'utf8'), /private marker|Abstrahierter Kontext/);
  assert.equal((await stat(repo.file)).mode & 0o777, 0o600);
});

test('Zeitablauf sperrt Schreiben ohne Cleanup; Kontolöschung entfernt alle zugeordneten Daten', async t => {
  const dir = await mkdtemp(path.join(os.tmpdir(), 'sparring-'));
  t.after(() => rm(dir, { recursive: true, force: true }));
  const repo = new SparringRepository(dir);
  const e = await repo.create(coach, client);
  await repo.act(e.id, client, 'intake', intake);
  await repo.act(e.id, coach, 'start', {}, new Date('2026-01-05T12:00:00Z'));
  await assert.rejects(repo.act(e.id, client, 'message', { body: 'Zu spät', requestId: 'request-id-late-123' }, new Date('2026-01-10T12:00:00Z')), { status: 403 });
  await repo.deleteUser(coach.id);
  assert.deepEqual(await repo.list(client), []);
});

test('Reader-Service-Worker ignoriert Chat und Intake auch bei vorhandenen Offline-Caches', async () => {
  const { runInNewContext } = await import('node:vm');
  const handlers = {};
  const source = await readFile(new URL('./public/offline-sw.mjs', import.meta.url), 'utf8');
  runInNewContext(source, { URL, self: { location: { origin: 'https://example.test' }, addEventListener: (name, fn) => { handlers[name] = fn; } } });
  for (const pathname of ['/workspace/sparring/123', '/workspace/api/sparring/123', '/workspace/api/workspace/sync', '/workspace/konto']) {
    let intercepted = false;
    handlers.fetch({ request: { method: 'GET', url: `https://example.test${pathname}` }, respondWith: () => { intercepted = true; } });
    assert.equal(intercepted, false, pathname);
  }
});
