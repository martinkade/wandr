#!/usr/bin/env bash
#
# Builds the Android release (app bundle + APK) and collects the artifacts, including the R8 mapping file, in ./dist.
# The content of ./dist is deleted first. Usage: scripts/build-release.sh
#
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DIST="$ROOT/dist"
OUT="$ROOT/androidApp/build/outputs"

cd "$ROOT"

# 1. Empty dist (the folder itself stays). The path is fixed to <repo>/dist, so nothing else can be hit.
mkdir -p "$DIST"
find "$DIST" -mindepth 1 -delete
echo "==> dist emptied: $DIST"

# 2. Release build: app bundle (Play Store) and APK, both signed with the release key if androidApp/signing/release.key.properties exists.
if [[ ! -f "$ROOT/androidApp/signing/release.key.properties" ]]; then
  echo "WARNING: androidApp/signing/release.key.properties not found, the release is not signed." >&2
fi
echo "==> building release bundle and APK"
./gradlew --console=plain :androidApp:bundleRelease :androidApp:assembleRelease

# 3. Collect the artifacts. Files are named after the version, e.g. wandr-1.0.0.aab
VERSION="$(grep -m1 'versionName' androidApp/build.gradle.kts | sed -E 's/.*"([^"]+)".*/\1/' || true)"
NAME="wandr-${VERSION:-release}"

copy_first() { # <description> <target> <glob...>
  local description="$1" target="$2"; shift 2
  local file
  for file in "$@"; do
    if [[ -f "$file" ]]; then
      cp "$file" "$target"
      echo "    $description -> ${target#"$ROOT/"}"
      return 0
    fi
  done
  return 1
}

copy_first "app bundle" "$DIST/$NAME.aab" "$OUT"/bundle/release/*.aab \
  || { echo "ERROR: no app bundle found in $OUT/bundle/release" >&2; exit 1; }

copy_first "APK" "$DIST/$NAME.apk" "$OUT"/apk/release/*-release.apk "$OUT"/apk/release/*-release-unsigned.apk "$OUT"/apk/release/*.apk \
  || { echo "ERROR: no APK found in $OUT/apk/release" >&2; exit 1; }

# The mapping file only exists when R8 shrinks the release (isMinifyEnabled = true in androidApp/build.gradle.kts).
# It is needed to read crash stack traces of that exact build, so keep it with the artifacts.
if ! copy_first "R8 mapping" "$DIST/$NAME-mapping.txt" "$OUT/mapping/release/mapping.txt"; then
  echo "WARNING: no mapping file was produced: the release is not minified (isMinifyEnabled = false)." >&2
fi

echo "==> done:"
ls -lh "$DIST"
