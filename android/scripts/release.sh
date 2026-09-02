#!/bin/sh
set -eu

# mise prepends its own tool bin to PATH, so a PATH-based override of the
# Bitwarden CLI would be shadowed. BW_BIN is the seam used by the tests.
bw_bin="${BW_BIN:-bw}"
item_name="capture-hub-release-keystore"

if ! status_json=$("$bw_bin" status) || ! status=$(printf '%s' "$status_json" | jq -r .status 2>/dev/null); then
  echo "Bitwarden CLI failed to report status; run 'bw status' directly to see the underlying error" >&2
  exit 1
fi
if [ "$status" != "unauthenticated" ] && [ "$status" != "locked" ] && [ "$status" != "unlocked" ]; then
  echo "Bitwarden CLI failed to report status; run 'bw status' directly to see the underlying error" >&2
  exit 1
fi
if [ "$status" = "unauthenticated" ]; then
  echo "Bitwarden is not logged in. Run: bw login" >&2
  exit 1
fi
if [ "$status" = "locked" ]; then
  BW_SESSION=$("$bw_bin" unlock --raw)
  export BW_SESSION
fi

if ! item=$("$bw_bin" get item "$item_name"); then
  echo "Bitwarden item not found: $item_name" >&2
  echo "Register it as described in README, or run 'bw sync' if it was added on another device." >&2
  exit 1
fi

field() {
  printf '%s' "$item" | jq -r --arg n "$1" '(.fields[]? | select(.name == $n) | .value) // empty'
}

keystore_b64=$(printf '%s' "$item" | jq -r '.notes // empty')
store_password=$(field storePassword)
key_alias=$(field keyAlias)
key_password=$(field keyPassword)

if [ -z "$keystore_b64" ]; then
  echo "The note of $item_name is empty; it must hold the base64-encoded keystore" >&2
  exit 1
fi
if [ -z "$store_password" ] || [ -z "$key_alias" ] || [ -z "$key_password" ]; then
  echo "Custom fields storePassword, keyAlias and keyPassword are required on $item_name" >&2
  exit 1
fi

tmp_dir=$(mktemp -d)
trap 'rm -rf "$tmp_dir"' EXIT
printf '%s' "$keystore_b64" | base64 -d > "$tmp_dir/release.jks"

CAPTURE_HUB_KEYSTORE_FILE="$tmp_dir/release.jks" \
CAPTURE_HUB_KEYSTORE_PASSWORD="$store_password" \
CAPTURE_HUB_KEY_ALIAS="$key_alias" \
CAPTURE_HUB_KEY_PASSWORD="$key_password" \
  ./gradlew assembleRelease

echo "APK: android/app/build/outputs/apk/release/app-release.apk"
