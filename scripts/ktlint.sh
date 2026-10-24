#!/usr/bin/env bash
# Runs ktlint over the Kotlin sources. Extra arguments pass through, so
# `scripts/ktlint.sh --format` rewrites files in place.
set -euo pipefail

VERSION="1.8.0"
SHA256="369ad2b789f95a011f807e1fcb690ccef80bd7cd014fd139e73ae82dcc0baeab"
URL="https://repo1.maven.org/maven2/com/pinterest/ktlint/ktlint-cli/${VERSION}/ktlint-cli-${VERSION}-all.jar"

cache="${XDG_CACHE_HOME:-$HOME/.cache}/grapho"
jar="$cache/ktlint-cli-${VERSION}-all.jar"

verified() {
    echo "${SHA256}  $1" | sha256sum --check --status
}

if [[ ! -f "$jar" ]] || ! verified "$jar"; then
    mkdir -p "$cache"
    tmp="$(mktemp "$cache/ktlint.XXXXXX")"
    trap 'rm -f "$tmp"' EXIT
    curl --fail --silent --show-error --location --retry 5 --retry-all-errors \
        --output "$tmp" "$URL"
    if ! verified "$tmp"; then
        echo "ktlint download does not match the pinned SHA-256" >&2
        exit 1
    fi
    mv "$tmp" "$jar"
fi

cd "$(dirname "${BASH_SOURCE[0]}")/.."
exec java -jar "$jar" "android/**/*.kt" "android/**/*.kts" "!android/**/build/**" "$@"
