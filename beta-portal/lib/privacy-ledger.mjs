import { appendFile, mkdir, readFile } from 'node:fs/promises';
import path from 'node:path';
export async function deletionEvents() {
  if (!process.env.WORKSPACE_DELETION_LEDGER) return [];
  try { return (await readFile(process.env.WORKSPACE_DELETION_LEDGER, 'utf8')).split('\n').filter(Boolean).map(line => JSON.parse(line)); }
  catch (e) { if (e.code === 'ENOENT') return []; throw e; }
}
export async function recordDeletion(action, userId, engagementId = null, messageId = null) {
  if (!['delete-user', 'erase-message', 'erase-intake'].includes(action) || ![userId, engagementId, messageId].every(x => x === null || /^[\w-]{1,80}$/.test(x))) throw new Error('Invalid deletion event');
  const file = process.env.WORKSPACE_DELETION_LEDGER;
  if (!file) { if (process.env.NODE_ENV === 'production') throw new Error('Deletion ledger required'); return; }
  await mkdir(path.dirname(file), { recursive: true, mode: 0o700 });
  await appendFile(file, JSON.stringify({ action, userId, engagementId, messageId, at: new Date().toISOString() }) + '\n', { mode: 0o600, flush: true });
}
export function applyDeletions(data, events) {
  for (const event of events) {
    if (event.action === 'delete-user') data.engagements = data.engagements.filter(e => e.clientId !== event.userId && e.coachId !== event.userId);
    const e = data.engagements.find(e => e.id === event.engagementId);
    if (e && event.action === 'erase-message') e.messages = e.messages.filter(m => m.id !== event.messageId);
    if (e && event.action === 'erase-intake') e.intake = null;
  }
  return data;
}
