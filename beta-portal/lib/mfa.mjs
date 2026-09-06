import { Secret, TOTP } from 'otpauth';
import { seal, unseal } from './private-data.mjs';
export function newMfa(user) {
  const secret = new Secret({ size: 20 });
  return { secret: seal(secret.base32, `mfa:${user.id}`), display: secret.base32 };
}
export function verifyMfa(user, token, now = Date.now()) {
  if (!/^\d{6}$/.test(String(token))) return null;
  const totp = new TOTP({ issuer: 'cleonhardt.de', label: user.email, algorithm: 'SHA1', digits: 6, period: 30, secret: Secret.fromBase32(unseal(user.mfa.secret, `mfa:${user.id}`)) });
  const delta = totp.validate({ token, window: 1, timestamp: now });
  if (delta === null) return null;
  const counter = Math.floor(now / 30000) + delta;
  return counter > (user.mfa.lastCounter ?? -1) ? counter : null;
}
