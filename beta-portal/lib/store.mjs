import { chmod, mkdir, open, readFile, rename, unlink } from 'node:fs/promises';
import path from 'node:path';

const dataDirectory = process.env.BETA_DATA_DIR || '/app/data';
const dataFile = path.join(dataDirectory, 'accounts.json');
const lockFile = path.join(dataDirectory, 'accounts.lock');

function freshStore() {
  return { version: 1, users: [] };
}

function validateStore(value) {
  if (!value || value.version !== 1 || !Array.isArray(value.users)) {
    throw new Error('Ungültiger Datenbestand des Beta-Portals.');
  }
  return value;
}

export async function ensureStore() {
  await mkdir(dataDirectory, { recursive: true, mode: 0o700 });
  try {
    return await loadStore();
  } catch (error) {
    if (error.code !== 'ENOENT') throw error;
    await writeStore(freshStore());
    return freshStore();
  }
}

export async function loadStore() {
  const raw = await readFile(dataFile, 'utf8');
  return validateStore(JSON.parse(raw));
}

async function writeStore(store) {
  const tempFile = path.join(dataDirectory, `.accounts-${process.pid}-${Date.now()}.tmp`);
  const handle = await open(tempFile, 'wx', 0o600);
  try {
    await handle.writeFile(`${JSON.stringify(store, null, 2)}\n`, 'utf8');
    await handle.sync();
  } finally {
    await handle.close();
  }
  await rename(tempFile, dataFile);
  await chmod(dataFile, 0o600);
}

async function acquireLock() {
  for (let attempt = 0; attempt < 100; attempt += 1) {
    try {
      return await open(lockFile, 'wx', 0o600);
    } catch (error) {
      if (error.code !== 'EEXIST') throw error;
      await new Promise((resolve) => setTimeout(resolve, 20));
    }
  }
  throw new Error('Datenbestand ist derzeit gesperrt.');
}

export async function updateStore(mutator) {
  await mkdir(dataDirectory, { recursive: true, mode: 0o700 });
  const lock = await acquireLock();
  try {
    let store;
    try {
      store = await loadStore();
    } catch (error) {
      if (error.code !== 'ENOENT') throw error;
      store = freshStore();
    }
    const result = await mutator(store);
    validateStore(store);
    await writeStore(store);
    return result;
  } finally {
    await lock.close();
    await unlink(lockFile).catch(() => {});
  }
}

export function findUserByEmail(store, email) {
  return store.users.find((user) => user.email === email);
}

export function findUserById(store, id) {
  return store.users.find((user) => user.id === id);
}
