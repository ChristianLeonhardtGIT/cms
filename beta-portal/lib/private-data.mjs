import { createCipheriv, createDecipheriv, randomBytes } from 'node:crypto';
import { readFileSync } from 'node:fs';

function keyring() {
  const file = process.env.WORKSPACE_KEYRING_FILE;
  if (!file) return null;
  const ring = JSON.parse(readFileSync(file, 'utf8'));
  if (!ring.active || !/^[a-f0-9]{64}$/i.test(ring.keys?.[ring.active] || '')) throw new Error('Invalid key configuration');
  return ring;
}
export function hasEncryption() { return Boolean(keyring()); }
export function seal(value, purpose) {
  const ring = keyring();
  if (!ring) {
    if (process.env.NODE_ENV === 'production') throw new Error('Encryption configuration required');
    return value;
  }
  const iv = randomBytes(12);
  const cipher = createCipheriv('aes-256-gcm', Buffer.from(ring.keys[ring.active], 'hex'), iv);
  cipher.setAAD(Buffer.from(`workspace:${purpose}:v1:${ring.active}`));
  const encrypted = Buffer.concat([cipher.update(JSON.stringify(value), 'utf8'), cipher.final()]);
  return { encrypted: 1, keyId: ring.active, iv: iv.toString('base64'), tag: cipher.getAuthTag().toString('base64'), data: encrypted.toString('base64') };
}
export function unseal(value, purpose) {
  if (value?.encrypted !== 1) return value; // Existing plaintext is migrated on next atomic write.
  const ring = keyring();
  if (!/^[a-f0-9]{64}$/i.test(ring?.keys?.[value.keyId] || '')) throw new Error('Encryption key unavailable');
  const decipher = createDecipheriv('aes-256-gcm', Buffer.from(ring.keys[value.keyId], 'hex'), Buffer.from(value.iv, 'base64'));
  decipher.setAAD(Buffer.from(`workspace:${purpose}:v1:${value.keyId}`));
  decipher.setAuthTag(Buffer.from(value.tag, 'base64'));
  return JSON.parse(Buffer.concat([decipher.update(Buffer.from(value.data, 'base64')), decipher.final()]).toString('utf8'));
}
