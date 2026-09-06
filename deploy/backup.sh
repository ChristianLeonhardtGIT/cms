#!/bin/bash
set -euo pipefail
umask 077

PROJECT_DIR="/opt/cleonhardt"
BACKUP_ROOT="/var/backups/cleonhardt"
COMPOSE_FILE="${PROJECT_DIR}/compose.production.yaml"
RECIPIENT_FILE="${PROJECT_DIR}/secrets/backup-recipient.asc"
test -s "${RECIPIENT_FILE}"
STAMP="$(date -u +%Y-%m-%dT%H%M%SZ)"
TARGET_DIR="${BACKUP_ROOT}/${STAMP}"

VOLUMES="
cleonhardt_magnolia-author-repositories
cleonhardt_magnolia-author-activation-key
cleonhardt_magnolia-public-repositories
cleonhardt_caddy-data
cleonhardt_caddy-config
cleonhardt_beta-portal-data
"

paused="false"

resume_services() {
  if [ "${paused}" = "true" ]; then
    docker compose -f "${COMPOSE_FILE}" unpause magnolia-author magnolia-public beta-portal >/dev/null 2>&1 || true
  fi
}

trap resume_services EXIT INT TERM

mkdir -p "${TARGET_DIR}"

# A short pause keeps the Jackrabbit repositories consistent while they are archived.
docker compose -f "${COMPOSE_FILE}" pause magnolia-author magnolia-public beta-portal >/dev/null
paused="true"

for volume in ${VOLUMES}; do
  mountpoint="$(docker volume inspect --format '{{ .Mountpoint }}' "${volume}")"
  tar --numeric-owner -C "${mountpoint}" -czf - . | gpg --batch --yes --recipient-file "${RECIPIENT_FILE}" --encrypt --output "${TARGET_DIR}/${volume}.tar.gz.gpg"
done

tar -C "${PROJECT_DIR}" -czf - \
  VERSION \
  Dockerfile.production \
  compose.production.yaml \
  deploy \
  docker \
  beta-portal \
  docs \
  light-modules | gpg --batch --yes --recipient-file "${RECIPIENT_FILE}" --encrypt --output "${TARGET_DIR}/project-config.tar.gz.gpg"

# Secrets and deletion ledger are a separate encrypted recovery layer.
tar -C "${PROJECT_DIR}" -czf - secrets .env privacy-ledger | gpg --batch --yes --recipient-file "${RECIPIENT_FILE}" --encrypt --output "${TARGET_DIR}/protected-config.tar.gz.gpg"

if test -d /etc/cleonhardt; then
  tar -C / -czf - etc/cleonhardt | gpg --batch --yes --recipient-file "${RECIPIENT_FILE}" --encrypt --output "${TARGET_DIR}/system-secrets.tar.gz.gpg"
fi

(
  cd "${TARGET_DIR}"
  sha256sum ./*.tar.gz.gpg > SHA256SUMS
)

docker compose -f "${COMPOSE_FILE}" unpause magnolia-author magnolia-public beta-portal >/dev/null
paused="false"

touch "${TARGET_DIR}/COMPLETE"
ln -sfn "${STAMP}" "${BACKUP_ROOT}/latest"

# Daily local retention. Hostinger's weekly VPS backup remains the off-server layer.
find "${BACKUP_ROOT}" -mindepth 1 -maxdepth 1 -type d \
  -name '20??-??-??T??????Z' -mtime +30 -exec rm -rf -- {} +
