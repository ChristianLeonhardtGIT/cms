import { appendFile, mkdir, readdir, unlink } from 'node:fs/promises';
import path from 'node:path';
const actions = new Set(['access', 'create', 'intake', 'start', 'complete', 'cancel', 'message', 'read', 'restrict', 'unrestrict', 'erase-message', 'erase-intake', 'export', 'delete-user', 'mfa-enrolled']);
export class AuditLog {
  constructor(directory) { this.directory = path.join(directory, 'audit'); }
  async record(actor, action, entity) {
    if (!actions.has(action) || !/^[\w-]{1,80}$/.test(actor) || !/^[\w-]{1,80}$/.test(entity)) throw new Error('Invalid audit event');
    await mkdir(this.directory, { recursive: true, mode: 0o700 });
    const timestamp = new Date().toISOString();
    await appendFile(path.join(this.directory, `${timestamp.slice(0, 10)}.jsonl`), JSON.stringify({ timestamp, actor, action, entity }) + '\n', { mode: 0o600 });
  }
  async purge(now = Date.now()) {
    await mkdir(this.directory, { recursive: true, mode: 0o700 });
    for (const name of await readdir(this.directory)) if (/^\d{4}-\d{2}-\d{2}\.jsonl$/.test(name) && now - Date.parse(name.slice(0, 10)) > 90 * 86400000) await unlink(path.join(this.directory, name));
  }
}
