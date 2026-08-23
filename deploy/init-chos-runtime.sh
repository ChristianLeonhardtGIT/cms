#!/bin/sh
set -eu

# Creates a safe initial release from the reader bundled with the portal. This
# keeps the protected area available before the first GitHub sync succeeds.
PROJECT_DIR="${CHOS_PROJECT_DIR:-/opt/cleonhardt}"
SOURCE_DIR="${PROJECT_DIR}/beta-portal/chos-reader"
RUNTIME_DIR="${CHOS_RUNTIME_DIR:-${PROJECT_DIR}/runtime/chos}"
RELEASE_DIR="${RUNTIME_DIR}/releases/bundled"

if [ -e "${RUNTIME_DIR}/current" ]; then
  echo "ChOS-Laufzeit ist bereits initialisiert."
  exit 0
fi

for required in index.html all-in-one.html VERSION.md; do
  if [ ! -f "${SOURCE_DIR}/${required}" ]; then
    echo "Initialisierung abgebrochen: ${required} fehlt." >&2
    exit 1
  fi
done

mkdir -p "${RELEASE_DIR}"
rsync -a --delete "${SOURCE_DIR}/" "${RELEASE_DIR}/"
printf '%s\n' "bundled" > "${RELEASE_DIR}/.source-commit"
printf '%s\n' "bundled" > "${RELEASE_DIR}/.source-branch"
date -u '+%Y-%m-%dT%H:%M:%SZ' > "${RELEASE_DIR}/.synced-at"
ln -s "releases/bundled" "${RUNTIME_DIR}/current"

echo "Gebündelter ChOS-Lesestand als sichere Startversion aktiviert."
