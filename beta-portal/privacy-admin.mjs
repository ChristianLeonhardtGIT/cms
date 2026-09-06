#!/usr/bin/env node
import { writeFile, readFile } from 'node:fs/promises';
import { SparringRepository } from './lib/sparring.mjs';
import { loadStore } from './lib/store.mjs';
import { AuditLog } from './lib/audit.mjs';
import { recordDeletion } from './lib/privacy-ledger.mjs';
const [command, requestFile] = process.argv.slice(2);
try {
  if (!['export', 'restrict', 'unrestrict', 'erase-message', 'erase-intake', 'reconcile'].includes(command)) throw new Error('Invalid command');
  const directory = process.env.BETA_DATA_DIR || '/app/data';
  const repo = new SparringRepository(directory);
  const audit = new AuditLog(directory);
  const users = (await loadStore()).users;
  if (command === 'reconcile') {
    await repo.purge(users.map(u => u.id));
  } else {
    const request = JSON.parse(await readFile(requestFile, 'utf8'));
    const user = users.find(u => u.id === request.userId);
    if (!user) throw new Error('Unknown account');
    if (command === 'export') {
      // Never include password hashes, invitation tokens, MFA keys or unrelated accounts.
      const account = Object.fromEntries(['id','name','email','role','createdAt','startAt','activeUntil','readUntil'].map(k => [k,user[k]]));
      const engagements = (await repo.load()).engagements.filter(e => e.clientId === user.id).map(({ notification, ...e }) => e);
      await audit.record('operator', 'export', user.id);
      await writeFile(request.output, JSON.stringify({ account, engagements }, null, 2), { flag: 'wx', mode: 0o600 });
    } else {
      const e = (await repo.load()).engagements.find(e => e.id === request.engagementId && e.clientId === user.id);
      if (!e) throw new Error('Unknown engagement');
      const owner = users.find(u => u.id === e.coachId && u.role === 'owner');
      if (!owner) throw new Error('Unknown coach');
      if (command.startsWith('erase-')) await recordDeletion(command, user.id, e.id, request.messageId || null);
      // Erasure replay has already applied when the repository reloads.
      if (command.startsWith('erase-')) await repo.update(() => {});
      else await repo.act(e.id, owner, command, request);
      await audit.record('operator', command, e.id);
    }
  }
  console.log('Datenschutzauftrag verarbeitet.');
} catch { console.error('Datenschutzauftrag fehlgeschlagen. Konfiguration und Auftragsdatei prüfen.'); process.exitCode = 1; }
