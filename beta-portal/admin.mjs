#!/usr/bin/env node
import { randomUUID } from 'node:crypto';
import { accessPhase, addDays, normalizeEmail, parseStartDate, randomToken, tokenDigest } from './lib/security.mjs';
import { ensureStore, findUserByEmail, loadStore, updateStore } from './lib/store.mjs';
import { FileWorkspaceRepository } from './lib/workspace-repository.mjs';

const workspaceRepository = new FileWorkspaceRepository(process.env.BETA_DATA_DIR || '/app/data');

function argumentsMap(values) {
  return Object.fromEntries(values.filter((item) => item.startsWith('--')).map((item) => {
    const index = item.indexOf('=');
    return index === -1 ? [item.slice(2), true] : [item.slice(2, index), item.slice(index + 1)];
  }));
}

function usage() {
  console.log(`ChOS Beta Portal Verwaltung

  node admin.mjs invite --email=name@firma.de --name="Vorname Nachname" --start=YYYY-MM-DD
  node admin.mjs owner --email=christian@beispiel.de --name="Christian Leonhardt"
  node admin.mjs reinvite --email=name@firma.de
  node admin.mjs disable --email=name@firma.de
  node admin.mjs remove --email=name@firma.de --confirm=name@firma.de
  node admin.mjs list

Einladungen sind 72 Stunden gültig. Passwörter werden nie über die Kommandozeile übergeben.`);
}

function inviteUrl(token) {
  const origin = (process.env.BETA_PUBLIC_ORIGIN || 'https://cleonhardt.de').replace(/\/$/, '');
  return `${origin}/beta/einladung?token=${encodeURIComponent(token)}`;
}

async function invite(options) {
  const email = normalizeEmail(options.email);
  const name = String(options.name || '').trim();
  if (!email.includes('@') || !name || !options.start) throw new Error('E-Mail, Name und Startdatum sind erforderlich.');
  const startAt = parseStartDate(options.start);
  const activeDays = Number(options['active-days'] || 60);
  const readDays = Number(options['read-days'] || 30);
  const token = randomToken();
  const inviteExpiresAt = addDays(new Date(), 3);
  await updateStore((store) => {
    const participantCount = store.users.filter((user) => user.role !== 'owner').length;
    if (participantCount >= 5 && !findUserByEmail(store, email)) {
      throw new Error('Das Teilnehmerlimit von fünf Konten ist erreicht.');
    }
    const existing = findUserByEmail(store, email);
    const user = existing || { id: randomUUID(), createdAt: new Date().toISOString() };
    Object.assign(user, {
      email,
      name,
      role: 'participant',
      status: 'invited',
      passwordHash: null,
      inviteTokenHash: tokenDigest(token),
      inviteExpiresAt,
      startAt,
      activeUntil: addDays(startAt, activeDays),
      readUntil: addDays(startAt, activeDays + readDays),
      supportHoursTotal: 8,
      supportHoursUsed: user.supportHoursUsed || 0,
      disabledAt: null,
      updatedAt: new Date().toISOString()
    });
    if (!existing) store.users.push(user);
  });
  console.log(`Einladung erstellt für ${name} <${email}>`);
  console.log(`Gültig bis: ${inviteExpiresAt}`);
  console.log(inviteUrl(token));
}

async function owner(options) {
  const email = normalizeEmail(options.email);
  const name = String(options.name || '').trim();
  if (!email.includes('@') || !name) throw new Error('E-Mail und Name sind erforderlich.');
  const token = randomToken();
  const inviteExpiresAt = addDays(new Date(), 3);
  await updateStore((store) => {
    const existing = findUserByEmail(store, email);
    const user = existing || { id: randomUUID(), createdAt: new Date().toISOString() };
    Object.assign(user, {
      email,
      name,
      role: 'owner',
      status: 'invited',
      passwordHash: null,
      inviteTokenHash: tokenDigest(token),
      inviteExpiresAt,
      startAt: null,
      activeUntil: null,
      readUntil: null,
      supportHoursTotal: 0,
      supportHoursUsed: 0,
      disabledAt: null,
      updatedAt: new Date().toISOString()
    });
    if (!existing) store.users.push(user);
  });
  console.log(`Persönlicher ChOS-Zugang vorbereitet für ${name} <${email}>`);
  console.log(`Gültig bis: ${inviteExpiresAt}`);
  console.log(inviteUrl(token));
}

async function reinvite(options) {
  const email = normalizeEmail(options.email);
  const token = randomToken();
  const expires = addDays(new Date(), 3);
  const name = await updateStore((store) => {
    const user = findUserByEmail(store, email);
    if (!user) throw new Error('Konto nicht gefunden.');
    user.status = 'invited';
    user.passwordHash = null;
    user.inviteTokenHash = tokenDigest(token);
    user.inviteExpiresAt = expires;
    user.disabledAt = null;
    user.updatedAt = new Date().toISOString();
    return user.name;
  });
  console.log(`Neue Einladung erstellt für ${name} <${email}>`);
  console.log(inviteUrl(token));
}

async function disable(options) {
  const email = normalizeEmail(options.email);
  await updateStore((store) => {
    const user = findUserByEmail(store, email);
    if (!user) throw new Error('Konto nicht gefunden.');
    user.disabledAt = new Date().toISOString();
    user.updatedAt = new Date().toISOString();
  });
  console.log(`Konto ${email} wurde deaktiviert.`);
}

async function remove(options) {
  const email = normalizeEmail(options.email);
  if (!email || normalizeEmail(options.confirm) !== email) {
    throw new Error('Zum endgültigen Löschen muss --confirm exakt der E-Mail-Adresse entsprechen.');
  }
  const userId = await updateStore((store) => {
    const user = findUserByEmail(store, email);
    if (!user) throw new Error('Konto nicht gefunden.');
    user.disabledAt = new Date().toISOString();
    user.updatedAt = new Date().toISOString();
    return user.id;
  });
  await workspaceRepository.delete(userId);
  await updateStore((store) => {
    store.users = store.users.filter((user) => user.id !== userId);
  });
  console.log(`Konto und serverseitiger Workspace für ${email} wurden gelöscht.`);
}

async function list() {
  await ensureStore();
  const store = await loadStore();
  if (!store.users.length) {
    console.log('Keine Beta-Konten vorhanden.');
    return;
  }
  console.table(store.users.map((user) => ({
    name: user.name,
    email: user.email,
    phase: accessPhase(user),
    rolle: user.role || 'participant',
    start: user.startAt?.slice(0, 10) || 'dauerhaft',
    aktivBis: user.activeUntil?.slice(0, 10) || '–',
    lesenBis: user.readUntil?.slice(0, 10) || '–',
    support: user.role === 'owner' ? '–' : `${user.supportHoursUsed}/${user.supportHoursTotal} h`
  })));
}

const [command, ...rest] = process.argv.slice(2);
const options = argumentsMap(rest);

try {
  if (command === 'invite') await invite(options);
  else if (command === 'owner') await owner(options);
  else if (command === 'reinvite') await reinvite(options);
  else if (command === 'disable') await disable(options);
  else if (command === 'remove') await remove(options);
  else if (command === 'list') await list();
  else usage();
} catch (error) {
  console.error(`Fehler: ${error.message}`);
  process.exitCode = 1;
}
