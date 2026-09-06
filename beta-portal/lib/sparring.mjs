import { deletionEvents, applyDeletions } from './privacy-ledger.mjs';
import { seal, unseal } from './private-data.mjs';
import { randomUUID } from 'node:crypto';
import { mkdir, open, readFile, rename, unlink } from 'node:fs/promises';
import path from 'node:path';

export const PRODUCT = Object.freeze({ slug: 'async-clarity-sparring', name: 'Async Clarity Sparring', priceCents: 24900, currency: 'EUR', businessDays: 5, customerType: 'business' });
export const POLICY_VERSION = '2026-09-06-v1';
export const INTAKE_FIELDS = Object.freeze({ problem: 'Was möchtest du klären?', impact: 'Warum ist das relevant?', goal: 'Was soll danach klarer sein?', role: 'In welcher Rolle bist du?', context: 'Welcher organisatorische Kontext ist wichtig?', urgency: 'Gibt es einen Entscheidungszeitpunkt?' });
export class SparringError extends Error {
  constructor(status, message) { super(message); this.status = status; }
}
const fail = (status, message) => { throw new SparringError(status, message); };
const iso = (date) => date.toISOString();
// UTC is only used for calendar arithmetic; the actual boundary is Berlin midnight.
export function engagementEnd(now, days = 5) {
  const parts = Object.fromEntries(new Intl.DateTimeFormat('en-CA', { timeZone: 'Europe/Berlin', year: 'numeric', month: '2-digit', day: '2-digit' }).formatToParts(now).map(p => [p.type, p.value]));
  const day = new Date(`${parts.year}-${parts.month}-${parts.day}T00:00:00Z`);
  let counted = 0;
  while (counted < days) {
    if (![0, 6].includes(day.getUTCDay())) counted++;
    day.setUTCDate(day.getUTCDate() + 1);
  }
  let boundary = new Date(day);
  for (let i = 0; i < 2; i++) {
    const offset = new Intl.DateTimeFormat('en', { timeZone: 'Europe/Berlin', timeZoneName: 'longOffset' }).formatToParts(boundary).find(p => p.type === 'timeZoneName').value;
    const [, sign, h, m] = offset.match(/GMT([+-])(\d\d):(\d\d)/);
    boundary = new Date(day.getTime() - (sign === '+' ? 1 : -1) * (Number(h) * 60 + Number(m)) * 60000);
  }
  return iso(boundary);
}
function text(value, max = 2000) {
  if (typeof value !== 'string' || !value.trim() || value.length > max) fail(400, 'Bitte alle Texte innerhalb der angegebenen Länge ausfüllen.');
  return value.trim();
}

export class SparringRepository {
  constructor(directory, retentionDays = 30) {
    if (!Number.isInteger(retentionDays) || retentionDays < 1 || retentionDays > 365) throw new Error('Invalid sparring retention');
    this.directory = path.join(directory, 'sparring');
    this.file = path.join(this.directory, 'store.json');
    this.retentionDays = retentionDays;
  }
  async load() {
    try {
      const data = unseal(JSON.parse(await readFile(this.file, 'utf8')), 'sparring-store');
      if (data.version !== 1 || !Array.isArray(data.engagements)) throw new Error('Invalid sparring schema');
      return applyDeletions(data, await deletionEvents());
    } catch (error) {
      if (error.code === 'ENOENT') return { version: 1, engagements: [] };
      throw error;
    }
  }
  async update(mutator) {
    await mkdir(this.directory, { recursive: true, mode: 0o700 });
    const lockPath = path.join(this.directory, 'store.lock');
    let lock;
    for (let i = 0; i < 100; i++) {
      try { lock = await open(lockPath, 'wx', 0o600); break; }
      catch (error) { if (error.code !== 'EEXIST') throw error; await new Promise(resolve => setTimeout(resolve, 20)); }
    }
    if (!lock) fail(503, 'Bitte später erneut versuchen.');
    const temporary = path.join(this.directory, `.${randomUUID()}.tmp`);
    try {
      const data = await this.load();
      const result = await mutator(data);
      const serialized = JSON.stringify(seal(data, 'sparring-store'));
      if (Buffer.byteLength(serialized) > 16 * 1024 * 1024) fail(409, 'Speichergrenze erreicht. Bitte Christian kontaktieren.');
      const file = await open(temporary, 'wx', 0o600);
      try { await file.writeFile(serialized); await file.sync(); } finally { await file.close(); }
      await rename(temporary, this.file);
      return result;
    } finally {
      await unlink(temporary).catch(() => {});
      await lock.close();
      await unlink(lockPath);
    }
  }
  allowed(e, user) { return e.clientId === user.id || (user.role === 'owner' && e.coachId === user.id); }
  find(data, id, user) {
    const e = data.engagements.find(e => e.id === id && this.allowed(e, user));
    if (!e) fail(404, 'Sparring nicht gefunden.');
    return e;
  }
  async list(user) {
    return (await this.load()).engagements.filter(e => this.allowed(e, user)).map(e => ({ id: e.id, status: e.status, startedAt: e.startedAt, endsAt: e.endsAt, product: e.product }));
  }
  async get(id, user) { return this.find(await this.load(), id, user); }
  async create(coach, client) {
    if (coach.role !== 'owner' || client.role === 'owner' || client.disabledAt) fail(403, 'Konto nicht freigeschaltet.');
    return this.update(data => {
      if (data.engagements.length >= 100) fail(409, 'Pilotgrenze erreicht.');
      if (data.engagements.some(e => e.clientId === client.id && !['completed', 'cancelled'].includes(e.status))) fail(409, 'Für dieses Konto besteht bereits ein offenes Sparring.');
      const e = { id: randomUUID(), clientId: client.id, coachId: coach.id, product: { ...PRODUCT }, status: 'intake', intake: null, policyVersion: null, policyAcceptedAt: null, startedAt: null, endsAt: null, completedAt: null, deletionScheduledAt: null, createdAt: iso(new Date()), messages: [], read: {}, notification: {} };
      data.engagements.push(e); return e;
    });
  }
  async act(id, user, action, input = {}, now = new Date()) {
    return this.update(data => {
      const e = this.find(data, id, user);
      this.expire(e, now);
      const coach = user.role === 'owner' && e.coachId === user.id;
      if (e.restricted && !['read', 'unrestrict', 'erase-message', 'erase-intake', 'restrict'].includes(action)) fail(403, 'Verarbeitung ist eingeschränkt.');
      if (['restrict', 'unrestrict', 'erase-message', 'erase-intake'].includes(action)) {
        if (!coach) fail(403, 'Aktion nicht erlaubt.');
        if (action === 'restrict') e.restricted = true;
        if (action === 'unrestrict') e.restricted = false;
        if (action === 'erase-message') {
          if (!e.messages.some(m => m.id === input.messageId)) fail(404, 'Nachricht nicht gefunden.');
          e.messages = e.messages.filter(m => m.id !== input.messageId);
        }
        if (action === 'erase-intake') e.intake = null;
      } else if (action === 'intake') {
        if (coach || e.status !== 'intake') fail(403, 'Intake ist nicht bearbeitbar.');
        if (input.accepted !== POLICY_VERSION || input.business !== 'yes') fail(400, 'Bitte B2B-Beauftragung und Datenregeln bestätigen.');
        e.intake = Object.fromEntries(Object.keys(INTAKE_FIELDS).map(key => [key, text(input[key])]));
        e.policyVersion = POLICY_VERSION; e.policyAcceptedAt = iso(now); e.status = 'ready';
      } else if (action === 'start') {
        if (!coach || e.status !== 'ready') fail(403, 'Start ist nicht möglich.');
        e.startedAt = iso(now); e.endsAt = engagementEnd(now); e.status = 'active';
      } else if (action === 'complete' || action === 'cancel') {
        if (!coach || ['completed', 'cancelled'].includes(e.status)) fail(403, 'Statusänderung ist nicht möglich.');
        if (action === 'complete' && e.status !== 'active') fail(409, 'Nur aktive Sparrings können abgeschlossen werden.');
        e.status = action === 'complete' ? 'completed' : 'cancelled'; e.completedAt = iso(now);
        e.deletionScheduledAt = iso(new Date(now.getTime() + this.retentionDays * 86400000));
      } else if (action === 'message') {
        if (e.status !== 'active') fail(403, 'Das Sparring ist nicht aktiv.');
        const body = text(input.body, 10000);
        if (!/^[\w-]{16,80}$/.test(input.requestId || '')) fail(400, 'Ungültige Nachrichtenkennung.');
        if (e.messages.some(m => m.requestId === input.requestId && m.senderId === user.id)) return e;
        if (e.messages.length >= 500) fail(409, 'Nachrichtenlimit des Piloten erreicht.');
        const last = e.messages.findLast(m => m.senderId === user.id);
        if (last && now - new Date(last.createdAt) < 2000) fail(429, 'Bitte kurz warten.');
        e.messages.push({ id: randomUUID(), requestId: input.requestId, senderId: user.id, body, createdAt: iso(now) });
        const recipient = coach ? e.clientId : e.coachId;
        e.notification[recipient] ||= { pendingSince: iso(now) }; // No text, email address or subject from user input.
      } else if (action === 'read') {
        if (input.messageId && !e.messages.some(m => m.id === input.messageId)) fail(400, 'Ungültiger Lesestand.');
        e.read[user.id] = input.messageId || null;
        if (e.messages.at(-1)?.id === input.messageId) delete e.notification[user.id];
      } else fail(400, 'Unbekannte Aktion.');
      return e;
    });
  }
  expire(e, now) {
    if (e.status === 'active' && new Date(e.endsAt) <= now) {
      e.status = 'completed'; e.completedAt = e.endsAt;
      e.deletionScheduledAt = iso(new Date(new Date(e.endsAt).getTime() + this.retentionDays * 86400000));
    }
  }
  async purge(validUserIds, now = new Date()) {
    return this.update(data => {
      for (const e of data.engagements) this.expire(e, now);
      data.engagements = data.engagements.filter(e => validUserIds.includes(e.clientId) && validUserIds.includes(e.coachId) && !(e.deletionScheduledAt && new Date(e.deletionScheduledAt) <= now) && !(['intake', 'ready'].includes(e.status) && now - new Date(e.createdAt) >= 90 * 86400000));
    });
  }
  async deleteUser(id) {
    return this.update(data => { data.engagements = data.engagements.filter(e => e.clientId !== id && e.coachId !== id); });
  }
}
