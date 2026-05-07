#!/usr/bin/env bash
# Bygger melosys-skjema-api-types og publiserer til lokal Maven cache (~/.m2/repository).
# Brukes av konsumenter (f.eks. melosys-api) for å teste types-endringer uten å merge til main.
#
# Bruk:
#   ./scripts/publish-types-local.sh
#
# Output:
#   - Progresjon og gradle-output: stderr
#   - Versjons-streng: siste linje på stdout (slik at konsumenter kan capture den)

set -euo pipefail

# Alt utenom selve versjon-strengen skal til stderr, slik at konsumenter
# kan kjøre VERSION=$(./scripts/publish-types-local.sh) og kun få versjonen.
log() { echo "$@" >&2; }

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

START_TIME=$SECONDS

log "==> melosys-skjema-api-types: lokal publish"
log "    repo: $REPO_ROOT"

log ""
log "[1/3] Beregner versjon..."
SHA="$(git rev-parse --short=12 HEAD)"
DIRTY=""
if ! git diff --quiet HEAD 2>/dev/null; then
    DIRTY=".dirty"
fi
DATE="$(date +%Y.%m.%d)"
VERSION="${DATE}-${SHA}${DIRTY}-LOCAL"
log "      versjon: $VERSION"

log ""
log "[2/3] Kjører gradle publishToMavenLocal..."
log "      (første kjøring kan ta 30-60s pga. daemon-oppstart og dependency resolution)"
log ""

# --console=plain sikrer at output flushes linjevis selv når stdout ikke er en TTY.
# Sender alt til stderr så stdout holdes rent for selve versjons-strengen.
./gradlew \
    :melosys-skjema-api-types:clean \
    :melosys-skjema-api-types:publishToMavenLocal \
    -Pversion="$VERSION" \
    --console=plain \
    --no-build-cache \
    1>&2

ELAPSED=$((SECONDS - START_TIME))

log ""
log "[3/3] Publisert til ~/.m2/repository på ${ELAPSED}s:"
log "      no.nav.melosys:melosys-skjema-api-types:$VERSION"
log ""

# Stdout: bare versjonen, slik at konsumenter kan capture den enkelt
echo "$VERSION"
