#!/bin/sh
set -eu

site_url="${1:-https://cleonhardt.de}"
indexnow_key="4eba2fbccd4fbe055b49e6d9d41c4e00"
shift || true

if [ "$#" -eq 0 ]; then
  echo "Nutzung: $0 [Website-URL] /geaenderte-seite [/weitere-seite]" >&2
  exit 2
fi

for changed_path in "$@"; do
  changed_url="${site_url%/}/${changed_path#/}"
  curl --fail --silent --show-error --get \
    --data-urlencode "url=$changed_url" \
    --data-urlencode "key=$indexnow_key" \
    "https://api.indexnow.org/indexnow"
  echo "IndexNow informiert: $changed_url"
done
