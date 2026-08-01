#!/bin/bash
# ============================================================
# prepare-assets.sh
# Downloads Gradle distribution + Android platform + build-tools
# into APK assets. Fully self-contained — no Termux required.
#
# Build-tools sources:
#   aapt/aapt2/aidl — rendiix aarch64 build (arm64 native binaries)
#   zipalign        — rendiix aarch64 build (arm64 native binary)
#   d8              — Termux official repo (Java, arch-independent)
#   apksigner       — Termux official repo (Java, arch-independent)
#
# Run on any Linux machine with internet + ar/binutils + tar.
# Output: app/src/main/assets/{gradle-toolchain,android-platform,build-tools}/
# ============================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$SCRIPT_DIR"
ASSETS_DIR="$PROJECT_ROOT/app/src/main/assets"

GRADLE_VERSION="8.5"
PLATFORM_VERSION="35"
BUILD_TOOLS_VERSION="35.0.0"

# --- rendiix aarch64 repo (native ARM64 build-tools) ---
RENDIIX_BASE="https://github.com/rendiix/rendiix.github.io/raw/master/dists/android-tools/termux/binary-aarch64"
RENDIIX_SDK_TOOLS_DEB="android-sdk-build-tools_34.0.0_aarch64.deb"
RENDIIX_ZIPALIGN_DEB="zipalign_34.0.0_aarch64.deb"

# --- Termux official repo (Java tools, arch-independent) ---
TERMUX_BASE="https://packages.termux.dev/apt/termux-main/pool/main"
D8_DEB="d8_37.0.0_all.deb"
APKSIGNER_DEB="apksigner_37.0.0_all.deb"

echo "=== android-code-editor: Preparing Build Toolchain Assets ==="

TEMP_DIR="$PROJECT_ROOT/.prepare-temp"
mkdir -p "$TEMP_DIR"

# Helper: extract a .deb package to a temp dir, then copy everything under
# the Termux prefix path to the given dest dir.
# Usage: extract_deb <deb_path> <dest_dir>
extract_deb() {
    local deb="$1"
    local dest="$2"
    local work="$TEMP_DIR/deb-extract-tmp"
    rm -rf "$work"
    mkdir -p "$work" "$dest"

    echo "    Extracting $(basename "$deb") ..."
    # ar x -> data.tar.xz or data.tar.gz
    (cd "$work" && ar x "$deb" data.tar.* 2>/dev/null)
    local data_tar
    data_tar=$(echo "$work"/data.tar.*)
    if [ ! -f "$data_tar" ]; then
        echo "    ERROR: no data.tar.* found in $deb"
        return 1
    fi

    # Extract under work/tree (preserving Termux prefix paths like
    # data/data/com.termux/files/usr/bin/...)
    mkdir -p "$work/tree"
    tar -xf "$data_tar" -C "$work/tree" 2>/dev/null

    # Copy files from the Termux prefix tree into dest, flattening bin/ & lib/
    local prefix_root="$work/tree/data/data/com.termux/files/usr"
    if [ -d "$prefix_root/bin" ]; then
        cp -a "$prefix_root/bin"/* "$dest/" 2>/dev/null || true
    fi
    if [ -d "$prefix_root/lib" ]; then
        mkdir -p "$dest/lib"
        cp -a "$prefix_root/lib"/* "$dest/lib/" 2>/dev/null || true
    fi
    if [ -d "$prefix_root/share" ]; then
        mkdir -p "$dest/share"
        cp -a "$prefix_root/share"/* "$dest/share/" 2>/dev/null || true
    fi

    rm -rf "$work"
}

# --- Gradle Distribution ---
echo ""
echo "[1/5] Downloading Gradle $GRADLE_VERSION distribution..."

GRADLE_ZIP="gradle-${GRADLE_VERSION}-bin.zip"
GRADLE_URL="https://services.gradle.org/distributions/${GRADLE_ZIP}"
GRADLE_DIR="$ASSETS_DIR/gradle-toolchain"

rm -rf "$GRADLE_DIR"
mkdir -p "$GRADLE_DIR"

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
echo "[2/5] Downloading Android Platform $PLATFORM_VERSION..."

PLATFORM_DIR="$ASSETS_DIR/android-platform"
mkdir -p "$PLATFORM_DIR"

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

# --- Build Tools (Native aarch64) ---
echo ""
echo "[3/5] Downloading aarch64 build-tools (aapt, aapt2, aidl, zipalign)..."

BT_DIR="$ASSETS_DIR/build-tools"
rm -rf "$BT_DIR"
mkdir -p "$BT_DIR"

echo "  Fetching android-sdk-build-tools (aarch64)..."
BT_SDK_DEB="$TEMP_DIR/$RENDIIX_SDK_TOOLS_DEB"
if [ ! -f "$BT_SDK_DEB" ]; then
    curl -L --progress-bar -o "$BT_SDK_DEB" "$RENDIIX_BASE/$RENDIIX_SDK_TOOLS_DEB"
fi
extract_deb "$BT_SDK_DEB" "$BT_DIR"

echo "  Fetching zipalign (aarch64)..."
BT_ZIP_DEB="$TEMP_DIR/$RENDIIX_ZIPALIGN_DEB"
if [ ! -f "$BT_ZIP_DEB" ]; then
    curl -L --progress-bar -o "$BT_ZIP_DEB" "$RENDIIX_BASE/$RENDIIX_ZIPALIGN_DEB"
fi
extract_deb "$BT_ZIP_DEB" "$BT_DIR"

echo "  Native build-tools size: $(du -sh "$BT_DIR" | cut -f1)"

# --- d8 (Java, arch-independent) ---
echo ""
echo "[4/5] Downloading d8 (Java, arch-independent)..."

if [ ! -f "$TEMP_DIR/$D8_DEB" ]; then
    echo "  Downloading $TERMUX_BASE/d/d8/$D8_DEB ..."
    curl -L --progress-bar -o "$TEMP_DIR/$D8_DEB" "$TERMUX_BASE/d/d8/$D8_DEB"
fi
extract_deb "$TEMP_DIR/$D8_DEB" "$BT_DIR"
echo "  d8 extracted"

# --- apksigner (Java, arch-independent) ---
echo ""
echo "[5/5] Downloading apksigner (Java, arch-independent)..."

if [ ! -f "$TEMP_DIR/$APKSIGNER_DEB" ]; then
    echo "  Downloading $TERMUX_BASE/a/apksigner/$APKSIGNER_DEB ..."
    curl -L --progress-bar -o "$TEMP_DIR/$APKSIGNER_DEB" "$TERMUX_BASE/a/apksigner/$APKSIGNER_DEB"
fi
extract_deb "$TEMP_DIR/$APKSIGNER_DEB" "$BT_DIR"
echo "  apksigner extracted"

echo "  Total build-tools size: $(du -sh "$BT_DIR" | cut -f1)"

# --- ensure executables ---
chmod +x "$BT_DIR"/aapt "$BT_DIR"/aapt2 "$BT_DIR"/aidl "$BT_DIR"/zipalign 2>/dev/null || true
chmod +x "$BT_DIR"/d8 "$BT_DIR"/apksigner 2>/dev/null || true

# --- Summary ---
echo ""
echo "============================================"
echo " Assets prepared successfully!"
echo "============================================"
echo ""
echo "Total assets: $(du -sh "$ASSETS_DIR" | cut -f1)"
echo ""
echo "Bundle contents:"
echo "  gradle-toolchain/  — Gradle $GRADLE_VERSION distribution"
echo "  android-platform/  — Platform $PLATFORM_VERSION (android.jar)"
echo "  build-tools/       — aapt, aapt2, aidl, zipalign, d8, apksigner"
echo ""
echo "Build-tools listing:"
ls -lh "$BT_DIR"/aapt "$BT_DIR"/aapt2 "$BT_DIR"/aidl "$BT_DIR"/zipalign "$BT_DIR"/d8 "$BT_DIR"/apksigner 2>/dev/null || echo "  (some tools missing — check downloads)"
echo ""
echo "Next steps:"
echo "  1. Build APK: ./gradlew assembleDebug"
echo "  2. On first launch, app extracts ALL toolchain (now including build-tools)"
echo "  3. NO Termux required — fully self-contained offline build"

echo ""
echo "Temp files kept at $TEMP_DIR (delete manually to free space)"
