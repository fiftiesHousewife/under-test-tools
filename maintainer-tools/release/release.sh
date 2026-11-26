#!/usr/bin/env bash
# Starts a release: tags the pushed main branch with v<version> and pushes the tag. The release workflow
# (.github/workflows/release.yml) then runs every check, and publishes only if all of them pass.
#
#   maintainer-tools/release/release.sh 1.2.3
#
# The version must already be in gradle.properties, with its notes in docs/releases/<version>.md, and pushed.
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"

VERSION="${1:?usage: maintainer-tools/release/release.sh 1.2.3}"
TAG="v$VERSION"

case "$VERSION" in
  [0-9]*.[0-9]*.[0-9]*) ;;
  *) echo "$VERSION isn't a version such as 1.2.3" >&2; exit 1 ;;
esac
grep -qx "version=$VERSION" gradle.properties \
  || { echo "gradle.properties doesn't say version=$VERSION; change it and push it first" >&2; exit 1; }
[ -s "docs/releases/$VERSION.md" ] \
  || { echo "docs/releases/$VERSION.md, the release's notes for users, is missing; write it and push it first" >&2; exit 1; }
git ls-files --error-unmatch KEYS >/dev/null 2>&1 \
  || { echo "KEYS, the public key users verify against, isn't committed; run maintainer-tools/release/set-up-signing.sh first" >&2; exit 1; }
[ "$(git rev-parse --abbrev-ref HEAD)" = "main" ] || { echo "Release from main" >&2; exit 1; }
git diff --quiet && git diff --cached --quiet || { echo "Commit or discard local changes first" >&2; exit 1; }
git fetch -q origin main
[ "$(git rev-parse HEAD)" = "$(git rev-parse origin/main)" ] \
  || { echo "main isn't the same as origin/main; push it first" >&2; exit 1; }
if git rev-parse -q --verify "refs/tags/$TAG" >/dev/null; then echo "$TAG already exists" >&2; exit 1; fi

git tag -a "$TAG" -m "under-test-tools $VERSION"
git push origin "$TAG"
echo "Pushed $TAG. The release workflow publishes it once every check passes."
