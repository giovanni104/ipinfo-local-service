#!/usr/bin/env sh
set -eu

: "${IPINFO_TOKEN:?Debes definir IPINFO_TOKEN}"
OUTPUT="${1:-./data/ipinfo_lite.mmdb}"
TEMP="${OUTPUT}.download"
mkdir -p "$(dirname "$OUTPUT")"

curl --fail --location \
  "https://ipinfo.io/data/ipinfo_lite.mmdb?token=${IPINFO_TOKEN}" \
  --output "$TEMP"

SIZE=$(wc -c < "$TEMP")
if [ "$SIZE" -lt 1000000 ]; then
  rm -f "$TEMP"
  echo "El archivo descargado es demasiado pequeño" >&2
  exit 1
fi

mv -f "$TEMP" "$OUTPUT"
echo "Base descargada en $OUTPUT"
