#!/bin/sh
set -eu

# Publishes the committed ChOS browser build from GitHub main. A new release is
# made visible only after all checks pass; the previous release remains intact.
PROJECT_DIR="${CHOS_PROJECT_DIR:-/opt/cleonhardt}"
RUNTIME_DIR="${CHOS_RUNTIME_DIR:-${PROJECT_DIR}/runtime/chos}"
SOURCE_DIR="${CHOS_SOURCE_DIR:-${RUNTIME_DIR}/source}"
RELEASES_DIR="${RUNTIME_DIR}/releases"
CURRENT_LINK="${RUNTIME_DIR}/current"
REPOSITORY_URL="${CHOS_REPOSITORY_URL:-git@github.com:ChristianLeonhardtGIT/ChOS.git}"
BRANCH="${CHOS_BRANCH:-main}"
DEPLOY_KEY="${CHOS_DEPLOY_KEY:-/etc/cleonhardt/chos-github-readonly}"
KNOWN_HOSTS="${CHOS_KNOWN_HOSTS:-/etc/cleonhardt/github-known-hosts}"

if [ ! -r "${DEPLOY_KEY}" ]; then
  echo "ChOS-Sync abgebrochen: Nur-Lese-Schlüssel fehlt (${DEPLOY_KEY})." >&2
  exit 1
fi

mkdir -p "${RUNTIME_DIR}" "${RELEASES_DIR}"

exec 9>"${RUNTIME_DIR}/sync.lock"
if ! flock -n 9; then
  echo "ChOS-Sync läuft bereits."
  exit 0
fi

export GIT_SSH_COMMAND="ssh -i ${DEPLOY_KEY} -o IdentitiesOnly=yes -o StrictHostKeyChecking=yes -o UserKnownHostsFile=${KNOWN_HOSTS}"

if [ ! -d "${SOURCE_DIR}/.git" ]; then
  git clone --filter=blob:none --no-checkout "${REPOSITORY_URL}" "${SOURCE_DIR}"
fi

git -C "${SOURCE_DIR}" remote set-url origin "${REPOSITORY_URL}"
git -C "${SOURCE_DIR}" fetch --force --prune --depth=1 origin "refs/heads/${BRANCH}"
COMMIT="$(git -C "${SOURCE_DIR}" rev-parse FETCH_HEAD)"

if [ -r "${CURRENT_LINK}/.source-commit" ] && [ "$(cat "${CURRENT_LINK}/.source-commit")" = "${COMMIT}" ]; then
  echo "ChOS main ist bereits aktuell (${COMMIT})."
  exit 0
fi

git -C "${SOURCE_DIR}" checkout --detach --force "${COMMIT}"
git -C "${SOURCE_DIR}" clean -fdx

for required in index.html all-in-one.html VERSION.md assets/app.js assets/search-index.js assets/style.css reports/validation-report.json; do
  if [ ! -f "${SOURCE_DIR}/${required}" ]; then
    echo "ChOS-Sync abgebrochen: ${required} fehlt in ${BRANCH}@${COMMIT}." >&2
    exit 1
  fi
done

if ! grep -q '"status": "BESTANDEN"' "${SOURCE_DIR}/reports/validation-report.json" \
  || ! grep -q '"strict": true' "${SOURCE_DIR}/reports/validation-report.json" \
  || ! grep -q '"error": 0' "${SOURCE_DIR}/reports/validation-report.json" \
  || ! grep -q '"warning": 0' "${SOURCE_DIR}/reports/validation-report.json"; then
  echo "ChOS-Sync abgebrochen: Die strikte System Validation ist nicht fehlerfrei." >&2
  exit 1
fi

DOC_COUNT="$(find "${SOURCE_DIR}/docs" -maxdepth 1 -type f -name '*.html' | wc -l | tr -d ' ')"
if [ "${DOC_COUNT}" -lt 1 ]; then
  echo "ChOS-Sync abgebrochen: Keine erzeugten Dokumentseiten gefunden." >&2
  exit 1
fi

RELEASE_DIR="${RELEASES_DIR}/${COMMIT}"
STAGE_DIR="${RUNTIME_DIR}/.stage-${COMMIT}-$$"
NEXT_LINK="${RUNTIME_DIR}/.current-$$"
cleanup() {
  rm -rf "${STAGE_DIR}"
  rm -f "${NEXT_LINK}"
}
trap cleanup EXIT HUP INT TERM

if [ ! -d "${RELEASE_DIR}" ]; then
  mkdir -p "${STAGE_DIR}"
  rsync -a "${SOURCE_DIR}/assets" "${SOURCE_DIR}/docs" "${SOURCE_DIR}/reports" "${STAGE_DIR}/"
  cp "${SOURCE_DIR}/index.html" "${SOURCE_DIR}/all-in-one.html" "${SOURCE_DIR}/VERSION.md" "${STAGE_DIR}/"
  printf '%s\n' "${COMMIT}" > "${STAGE_DIR}/.source-commit"
  printf '%s\n' "${BRANCH}" > "${STAGE_DIR}/.source-branch"
  date -u '+%Y-%m-%dT%H:%M:%SZ' > "${STAGE_DIR}/.synced-at"
  mv "${STAGE_DIR}" "${RELEASE_DIR}"
fi

ln -s "releases/${COMMIT}" "${NEXT_LINK}"
mv -Tf "${NEXT_LINK}" "${CURRENT_LINK}"
trap - EXIT HUP INT TERM

echo "ChOS main veröffentlicht: ${COMMIT} (${DOC_COUNT} Dokumentseiten)."
