#!/bin/bash
set -euo pipefail
umask 077

PROJECT_DIR="/opt/cleonhardt"
TARGET_DIR="${PROJECT_DIR}/secrets/workspace"
TARGET_FILE="${TARGET_DIR}/smtp.json"
TEMP_FILE=""
PASSWORD=""
PASSWORD_CONFIRM=""

cleanup() {
  PASSWORD=""
  PASSWORD_CONFIRM=""
  if [ -n "${TEMP_FILE}" ]; then
    rm -f -- "${TEMP_FILE}"
  fi
}
trap cleanup EXIT INT TERM

if [ ! -t 0 ]; then
  echo "Das SMTP-Kennwort muss interaktiv eingegeben werden." >&2
  exit 1
fi

if [ -e "${TARGET_FILE}" ] && [ "${1:-}" != "--replace" ]; then
  echo "${TARGET_FILE} existiert bereits. Für einen bewussten Austausch --replace verwenden." >&2
  exit 1
fi

read -r -s -p "SMTP-Kennwort für kontakt@cleonhardt.de: " PASSWORD
printf '\n'
read -r -s -p "SMTP-Kennwort wiederholen: " PASSWORD_CONFIRM
printf '\n'

if [ -z "${PASSWORD}" ] || [ "${PASSWORD}" != "${PASSWORD_CONFIRM}" ]; then
  echo "Die Kennwörter fehlen oder stimmen nicht überein." >&2
  exit 1
fi

if [[ "${PASSWORD}" =~ [[:cntrl:]] ]]; then
  echo "Das Kennwort darf keine Steuerzeichen enthalten." >&2
  exit 1
fi

# Escape the two JSON-special characters that can occur in the single input line.
PASSWORD=${PASSWORD//\\/\\\\}
PASSWORD=${PASSWORD//\"/\\\"}

install -d -o 100 -g 100 -m 0700 "${TARGET_DIR}"
TEMP_FILE=$(mktemp "${TARGET_DIR}/smtp.json.tmp.XXXXXX")
printf '%s\n' \
  '{' \
  '  "host": "smtp.hostinger.com",' \
  '  "port": 465,' \
  '  "user": "kontakt@cleonhardt.de",' \
  "  \"password\": \"${PASSWORD}\"," \
  '  "from": "kontakt@cleonhardt.de"' \
  '}' > "${TEMP_FILE}"
chown 100:100 "${TEMP_FILE}"
chmod 0600 "${TEMP_FILE}"
mv -f -- "${TEMP_FILE}" "${TARGET_FILE}"
TEMP_FILE=""

echo "SMTP-Konfiguration wurde geschützt unter ${TARGET_FILE} eingerichtet."
echo "Vor Aktivierung mit beta-portal/smtp-check.mjs prüfen."
