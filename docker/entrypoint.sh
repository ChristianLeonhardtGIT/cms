#!/bin/sh
set -eu

role="${MAGNOLIA_ROLE:-author}"

if [ "$role" = "author" ]; then
  key_dir="/opt/magnolia/apache-tomcat/webapps/magnoliaAuthor/WEB-INF/config/activation-key"
  key_file="$key_dir/magnolia-activation-keypair.properties"

  mkdir -p "$key_dir"

  if [ ! -s "$key_file" ]; then
  temp_dir="$(mktemp -d)"
  trap 'rm -rf "$temp_dir"' EXIT HUP INT TERM

  openssl genpkey -quiet -algorithm RSA -pkeyopt rsa_keygen_bits:4096 \
    -out "$temp_dir/private.pem"
  openssl pkcs8 -topk8 -nocrypt -in "$temp_dir/private.pem" -outform DER \
    -out "$temp_dir/private.der"
  openssl pkey -in "$temp_dir/private.pem" -pubout -outform DER \
    -out "$temp_dir/public.der"

  private_key="$(od -An -v -tx1 "$temp_dir/private.der" | tr -d ' \n' | tr '[:lower:]' '[:upper:]')"
  public_key="$(od -An -v -tx1 "$temp_dir/public.der" | tr -d ' \n' | tr '[:lower:]' '[:upper:]')"

  umask 077
  {
    printf '%s\n' '# Magnolia publishing key pair (generated automatically)' 
    printf 'key.private=%s\n' "$private_key"
    printf 'key.public=%s\n' "$public_key"
  } > "$key_file"

  rm -rf "$temp_dir"
  trap - EXIT HUP INT TERM
  fi
fi

exec "$@"
