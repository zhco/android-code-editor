#!/bin/bash
set -e
ASSETS_DIR="app/src/main/assets"
mkdir -p "$ASSETS_DIR"

# Gradle 8.5
if [ ! -f "$ASSETS_DIR/gradle-8.5-bin.zip" ]; then
  echo "Downloading Gradle 8.5..."
  curl -L -o "$ASSETS_DIR/gradle-8.5-bin.zip" https://services.gradle.org/distributions/gradle-8.5-bin.zip
fi

# Android Platform 35
if [ ! -f "$ASSETS_DIR/platform-35.zip" ]; then
  echo "Downloading Android Platform 35..."
  curl -L -o "$ASSETS_DIR/platform-35.zip" https://dl.google.com/android/repository/platform-35_r01.zip
fi

# Android Build Tools 35.0.0
if [ ! -f "$ASSETS_DIR/build-tools-35.zip" ]; then
  echo "Downloading Build Tools 35.0.0..."
  curl -L -o "$ASSETS_DIR/build-tools-35.zip" https://dl.google.com/android/repository/build-tools_r35_linux.zip
fi

# Termux tools
if [ ! -f "$ASSETS_DIR/rendiix-aarch64.deb" ]; then
  echo "Downloading rendiix aarch64..."
  curl -L -o "$ASSETS_DIR/rendiix-aarch64.deb" https://github.com/rendiix/termux-adb-fastboot/releases/download/v1.0.0/termux-adb-fastboot_aarch64.deb
fi

if [ ! -f "$ASSETS_DIR/termux-tools.deb" ]; then
  echo "Downloading Termux tools..."
  curl -L -o "$ASSETS_DIR/termux-tools.deb" https://packages.termux.dev/apt/termux-main/pool/main/t/termux-tools/termux-tools_2.1.4_aarch64.deb
fi

echo "Assets prepared"
ls -lh "$ASSETS_DIR/"
