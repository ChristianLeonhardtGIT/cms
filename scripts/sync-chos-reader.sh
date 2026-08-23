#!/bin/sh
set -eu

if [ "$#" -ne 1 ]; then
  echo "Verwendung: scripts/sync-chos-reader.sh /pfad/zum/ChOS-Repository" >&2
  exit 1
fi

SOURCE_DIR="${1%/}"
PROJECT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
TARGET_DIR="${PROJECT_DIR}/beta-portal/chos-reader"

for required in index.html all-in-one.html VERSION.md; do
  if [ ! -f "${SOURCE_DIR}/${required}" ]; then
    echo "ChOS-Quelle unvollständig: ${required} fehlt." >&2
    exit 1
  fi
done

mkdir -p "${TARGET_DIR}/assets" "${TARGET_DIR}/docs" "${TARGET_DIR}/reports"
rsync -a --delete "${SOURCE_DIR}/assets/" "${TARGET_DIR}/assets/"
rsync -a --delete "${SOURCE_DIR}/docs/" "${TARGET_DIR}/docs/"
rsync -a --delete "${SOURCE_DIR}/reports/" "${TARGET_DIR}/reports/"
cp "${SOURCE_DIR}/index.html" "${TARGET_DIR}/index.html"
cp "${SOURCE_DIR}/all-in-one.html" "${TARGET_DIR}/all-in-one.html"
cp "${SOURCE_DIR}/VERSION.md" "${TARGET_DIR}/VERSION.md"

echo "ChOS-Lesestand aktualisiert: $(sed -n '3p' "${SOURCE_DIR}/VERSION.md" | tr -d '*')"
