#!/usr/bin/env node
import { mailTransport } from './lib/notifications.mjs';

try {
  const mail = await mailTransport();
  if (!mail) throw new Error('missing');
  await mail.transport.verify();
  console.log('SMTP-Anmeldung und TLS-Verbindung wurden akzeptiert.');
  if (process.argv.includes('--send-test')) {
    const result = await mail.transport.sendMail({
      from: mail.from,
      to: mail.checkAddress,
      subject: 'Workspace SMTP-Test',
      text: 'Die inhaltsfreie Workspace-Benachrichtigung kann über diesen SMTP-Zugang zugestellt werden.',
      disableFileAccess: true,
      disableUrlAccess: true
    });
    if (!result.accepted?.length) throw new Error('rejected');
    console.log('Eine inhaltsfreie Testmail wurde an das Absenderpostfach angenommen.');
  }
  mail.transport.close();
} catch {
  console.error('SMTP-Prüfung fehlgeschlagen. Konfiguration, Kennwort und Anbieterstatus prüfen.');
  process.exitCode = 1;
}
