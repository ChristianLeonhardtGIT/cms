import { createHash, randomBytes, scrypt as scryptCallback, timingSafeEqual } from 'node:crypto';
import { promisify } from 'node:util';

const scrypt = promisify(scryptCallback);
const DAY_MS = 24 * 60 * 60 * 1000;

export function normalizeEmail(value) {
  return String(value ?? '').trim().toLowerCase();
}

export function validateEmail(value) {
  const email = normalizeEmail(value);
  if (email.length > 254 || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    return 'Bitte gib eine gültige E-Mail-Adresse ein.';
  }
  return null;
}

export function randomToken(bytes = 32) {
  return randomBytes(bytes).toString('base64url');
}

export function tokenDigest(token) {
  return createHash('sha256').update(String(token)).digest('hex');
}

export function validatePassword(password) {
  if (typeof password !== 'string' || password.length < 14) {
    return 'Das Passwort muss mindestens 14 Zeichen lang sein.';
  }
  if (password.length > 128) {
    return 'Das Passwort darf höchstens 128 Zeichen lang sein.';
  }
  return null;
}

export async function hashPassword(password) {
  const salt = randomBytes(16);
  const derived = await scrypt(password, salt, 64, { N: 16384, r: 8, p: 1 });
  return `scrypt$${salt.toString('base64url')}$${Buffer.from(derived).toString('base64url')}`;
}

export async function verifyPassword(password, encoded) {
  try {
    const [algorithm, saltValue, hashValue] = String(encoded).split('$');
    if (algorithm !== 'scrypt' || !saltValue || !hashValue) return false;
    const salt = Buffer.from(saltValue, 'base64url');
    const expected = Buffer.from(hashValue, 'base64url');
    const actual = Buffer.from(await scrypt(password, salt, expected.length, { N: 16384, r: 8, p: 1 }));
    return actual.length === expected.length && timingSafeEqual(actual, expected);
  } catch {
    return false;
  }
}

export function addDays(value, days) {
  return new Date(new Date(value).getTime() + Number(days) * DAY_MS).toISOString();
}

export function parseStartDate(value) {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(String(value))) {
    throw new Error('Startdatum muss im Format YYYY-MM-DD angegeben werden.');
  }
  const parsed = new Date(`${value}T00:00:00.000Z`);
  if (Number.isNaN(parsed.getTime())) throw new Error('Ungültiges Startdatum.');
  return parsed.toISOString();
}

export function accessPhase(user, now = new Date()) {
  if (!user || user.disabledAt) return 'disabled';
  if (!user.passwordHash || user.status === 'invited') return 'invited';
  if (user.role === 'owner') return 'owner';
  const current = now.getTime();
  if (current < new Date(user.startAt).getTime()) return 'scheduled';
  if (current < new Date(user.activeUntil).getTime()) return 'active';
  if (current < new Date(user.readUntil).getTime()) return 'readonly';
  return 'expired';
}
