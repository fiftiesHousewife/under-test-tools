#!/usr/bin/env bash
# Sets up the key the release workflow signs with: makes a PGP signing key (or uses one you name), stores it and its
# passphrase as the repository secrets SIGNING_KEY and SIGNING_KEY_PASSWORD, writes its public half to KEYS for
# users to verify against, and sends it to the key servers.
#
#   maintainer-tools/release/set-up-signing.sh                 make a new key; asks for the email to put on it
#   maintainer-tools/release/set-up-signing.sh <fingerprint>   use a key already in your keyring; asks its passphrase
#
# Run it in your own terminal, signed in to gh. Commit KEYS afterwards.
set -euo pipefail

REPO=fiftiesHousewife/under-test-tools
cd "$(git rev-parse --show-toplevel)"

if [[ $# -eq 1 ]]; then
  KEY=$1
  read -r -s -p "Passphrase of $KEY: " PASSPHRASE; echo
else
  read -r -p "Email to put on the release signing key: " EMAIL
  PASSPHRASE=$(openssl rand -base64 32)
  gpg --batch --pinentry-mode loopback --passphrase "$PASSPHRASE" \
    --quick-generate-key "under-test-tools releases <$EMAIL>" ed25519 sign never
  KEY=$(gpg --list-secret-keys --with-colons "under-test-tools releases <$EMAIL>" | awk -F: '/^fpr/ {print $10; exit}')
fi

# Checks the passphrase opens the key before storing either.
echo test | gpg --batch --pinentry-mode loopback --passphrase "$PASSPHRASE" --local-user "$KEY" --sign >/dev/null

gpg --armor --export-secret-keys --batch --pinentry-mode loopback --passphrase "$PASSPHRASE" "$KEY" \
  | gh secret set SIGNING_KEY --repo "$REPO"
printf '%s' "$PASSPHRASE" | gh secret set SIGNING_KEY_PASSWORD --repo "$REPO"

gpg --armor --export "$KEY" > KEYS
gpg --keyserver hkps://keys.openpgp.org --send-keys "$KEY"
gpg --keyserver hkps://keyserver.ubuntu.com --send-keys "$KEY"

echo
echo "Secrets set on $REPO, and KEYS written. Fingerprint: $KEY"
echo "keys.openpgp.org emails the key's address a link: follow it, or the key is found by fingerprint only."
[[ $# -eq 1 ]] || echo "The key's passphrase, for your password manager (it is in the secret too): $PASSPHRASE"
echo "Commit KEYS, and put the fingerprint in the README's 'Running it' section."
