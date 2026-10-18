#!/usr/bin/env bash
# One-time local generation of Novila's release UPLOAD keystore. Safe to run once anywhere;
# it refuses to overwrite an existing file.
#
# Google Play App Signing is assumed: the app-signing key lives with Google Play, and this
# key is only the upload key. Losing it is recoverable (upload-key reset in the Play Console,
# see docs/RELEASE.md); the app-signing key never leaves Google.
#
#   ./scripts/generate-release-keystore.sh [output-path] [key-alias]
#
# Defaults: novila-upload.jks in the current directory, alias "novila".
# Requires keytool (any JDK, e.g. `apt install openjdk-17-jdk-headless` or a phone-side
# Java app). You will be asked for one password on the terminal; it is never echoed.
#
# After generating:
#   1. Store the keystore file AND the password in a password manager (1Password/Vault) NOW.
#   2. Upload the file to the GitHub secret NOVILA_RELEASE_KEYSTORE_BASE64 (base64 command
#      is printed below) and the passwords to NOVILA_RELEASE_STORE_PASSWORD /
#      NOVILA_RELEASE_KEY_PASSWORD; set the variable NOVILA_RELEASE_KEY_ALIAS.
#   3. Delete the local .jks file. It must never be committed (see .gitignore).
set -euo pipefail

OUTPUT="${1:-novila-upload.jks}"
ALIAS="${2:-novila}"

command -v keytool >/dev/null || {
    echo "error: keytool is missing (install any JDK)" >&2
    exit 1
}
if [ -e "$OUTPUT" ]; then
    echo "error: $OUTPUT already exists. Never overwrite a release keystore: if this is the" >&2
    echo "real upload key, keep it and back it up instead of generating a new one." >&2
    exit 1
fi

echo "Generating upload keystore: $OUTPUT (alias: $ALIAS, RSA 4096, PKCS12, valid 10000 days)"
echo "One password: the default PKCS12 format uses it for both the keystore and the key."
keytool -genkeypair \
    -keystore "$OUTPUT" \
    -alias "$ALIAS" \
    -keyalg RSA \
    -keysize 4096 \
    -validity 10000 \
    -storetype PKCS12 \
    -storepass prompt \
    -keypass prompt \
    -dname "CN=Novila, OU=Release, O=Novila, L=Unknown, ST=Unknown, C=SE"

echo
echo "Done. Now, in this order:"
echo "  1. Back up $OUTPUT and the password in your password manager (1Password/Vault)."
echo "  2. Set the GitHub secrets (repo joeldenke/Novila → Settings → Secrets and variables → Actions):"
echo "       NOVILA_RELEASE_KEYSTORE_BASE64  =  $(base64 -w0 "$OUTPUT" 2>/dev/null || base64 "$OUTPUT" | tr -d '\n')"
echo "       NOVILA_RELEASE_STORE_PASSWORD   =  <the password you just chose>"
echo "       NOVILA_RELEASE_KEY_PASSWORD     =  <the same password>"
echo "       Variable NOVILA_RELEASE_KEY_ALIAS = $ALIAS"
echo "     (If NOVILA_RELEASE_KEYSTORE_BASE64 is already set, it is the live upload key:"
echo "      do NOT replace it with this file.)"
echo "  3. Delete the local file: rm $OUTPUT"
echo "  4. Never commit it. .gitignore already excludes *.jks."
