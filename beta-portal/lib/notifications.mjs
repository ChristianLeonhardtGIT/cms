import nodemailer from 'nodemailer';
import { readFile } from 'node:fs/promises';
export async function mailTransport() {
  if (!process.env.WORKSPACE_SMTP_FILE) return null;
  const c = JSON.parse(await readFile(process.env.WORKSPACE_SMTP_FILE, 'utf8'));
  if (!c.host || ![465, 587].includes(c.port) || !c.user || !c.password || !c.from || /[\r\n]/.test(c.from)) throw new Error('Invalid SMTP configuration');
  return { from: c.from, transport: nodemailer.createTransport({ host: c.host, port: c.port, secure: c.port === 465, requireTLS: true, auth: { user: c.user, pass: c.password }, tls: { minVersion: 'TLSv1.2', rejectUnauthorized: true }, logger: false, debug: false, disableFileAccess: true, disableUrlAccess: true, connectionTimeout: 10000, socketTimeout: 20000 }) };
}
export async function deliverNotifications(repository, users, mail, now = Date.now()) {
  if (!mail) return;
  // Single in-process runner; conditional acknowledgement never consumes a newer batch.
  const data = await repository.load();
  for (const e of data.engagements) for (const [recipientId, pending] of Object.entries(e.notification)) {
    if (now - Date.parse(pending.pendingSince) < 5 * 60000 || pending.sentAt || e.restricted) continue;
    const recipient = users.find(u => u.id === recipientId && !u.disabledAt && (u.role === 'owner' || Date.parse(u.readUntil) > now));
    if (!recipient) continue;
    const result = await mail.transport.sendMail({ from: mail.from, to: recipient.email, subject: 'Neue Nachricht im Workspace', text: 'Im Workspace wartet eine neue Nachricht auf dich.\n\nhttps://cleonhardt.de/workspace/sparring\n\nBitte antworte im geschützten Workspace.', disableFileAccess: true, disableUrlAccess: true });
    if (!result.accepted?.length) throw new Error('Mail delivery not accepted');
    await repository.update(current => {
      const n = current.engagements.find(x => x.id === e.id)?.notification?.[recipientId];
      if (n && n.pendingSince === pending.pendingSince) n.sentAt = new Date(now).toISOString();
    });
  }
}
