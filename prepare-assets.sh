#!/bin/bash
# ============================================================
# prepare-assets.sh
# Downloads Gradle distribution + Android platform into APK assets.
# Build-tools (aapt2, d8, zipalign, apksigner) are NOT bundled —
# the app obtains them at runtime from Termux or downloads on demand.
#
# Run on any Linux machine with internet.
# Output: app/src/main/assets/gradle/ and app/src/main/assets/android-platform/
# ============================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$SCRIPT_DIR"
ASSETS_DIR="$PROJECT_ROOT/app/src/main/assets"

GRADLE_VERSION="8.5"
PLATFORM_VERSION="35"
BUILD_TOOLS_VERSION="35.0.0"

echo "=== android-code-editor: Preparing Build Toolchain Assets ==="

# --- Gradle Distribution ---
echo ""
echo "[1/2] Downloading Gradle $GRADLE_VERSION distribution..."

GRADLE_ZIP="gradle-${GRADLE_VERSION}-bin.zip"
GRADLE_URL="https://services.gradle.org/distributions/${GRADLE_ZIP}"
GRADLE_DIR="$ASSETS_DIR/gradle-toolchain"

rm -rf "$GRADLE_DIR"
mkdir -p "$GRADLE_DIR"

TEMP_DIR="$PROJECT_ROOT/.prepare-temp"
mkdir -p "$TEMP_DIR"

if [ ! -f "$TEMP_DIR/$GRADLE_ZIP" ]; then
    echo "  Downloading $GRADLE_URL ..."
    curl -L --progress-bar -o "$TEMP_DIR/$GRADLE_ZIP" "$GRADLE_URL"
else
    echo "  Using cached Gradle zip"
fi

echo "  Extracting Gradle..."
unzip -qo "$TEMP_DIR/$GRADLE_ZIP" -d "$TEMP_DIR/gradle-extract"
mv "$TEMP_DIR/gradle-extract/gradle-${GRADLE_VERSION}" "$GRADLE_DIR/gradle-${GRADLE_VERSION}"

echo "  Gradle size: $(du -sh "$GRADLE_DIR" | cut -f1)"
echo "  Gradle ready at $GRADLE_DIR"

# --- Android Platform ---
echo ""
echo "[2/2] Downloading Android Platform $PLATFORM_VERSION..."

PLATFORM_DIR="$ASSETS_DIR/android-platform"
mkdir -p "$PLATFORM_DIR"

# platform-35_r01.zip is the standard Google distribution
PLATFORM_ZIP="platform-${PLATFORM_VERSION}_r01.zip"
PLATFORM_URL="https://dl.google.com/android/repository/${PLATFORM_ZIP}"

if [ ! -f "$TEMP_DIR/$PLATFORM_ZIP" ]; then
    echo "  Downloading $PLATFORM_URL ..."
    curl -L --progress-bar -o "$TEMP_DIR/$PLATFORM_ZIP" "$PLATFORM_URL"
else
    echo "  Using cached platform zip"
fi

echo "  Extracting platform..."
rm -rf "$PLATFORM_DIR"
mkdir -p "$PLATFORM_DIR"
unzip -qo "$TEMP_DIR/$PLATFORM_ZIP" -d "$PLATFORM_DIR"

echo "  Platform size: $(du -sh "$PLATFORM_DIR" | cut -f1)"
echo "  Platform ready at $PLATFORM_DIR"

# --- Summary ---
echo ""
echo "============================================"
echo " Assets prepared successfully!"
echo "============================================"
echo ""
echo "Total assets: $(du -sh "$ASSETS_DIR" | cut -f1)"
echo ""
echo "Bundle contents:"
echo "  gradle-toolchain/    — Gradle $GRADLE_VERSION distribution"
echo "  android-platform/    — Platform $PLATFORM_VERSION (android.jar)"
echo ""
echo "Next steps:"
echo "  1. Build APK in Android Studio or ./gradlew assembleDebug"
echo "  2. APK will be ~150MB (Gradle ~130MB + platform ~15MB)"
echo "  3. On first launch, app extracts toolchain to internal storage"
echo "  4. Build-tools (aapt2, d8) are obtained from Termux at runtime"

# Cleanup temp if not needed
# rm -rf "$TEMP_DIR"
echo ""
echo "Temp files kept at $TEMP_DIR (delete manually to free space)"
