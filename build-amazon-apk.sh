#!/usr/bin/env bash
set -euo pipefail

# One-command helper for GitHub Codespaces to create fevro-browser-release.apk
# in the project root so you can right-click -> Download directly.

echo "==> Checking Java & Android SDK..."
SDK_ROOT="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/android-sdk}}"
export ANDROID_HOME="$SDK_ROOT"
export ANDROID_SDK_ROOT="$SDK_ROOT"

if [ ! -x "$SDK_ROOT/cmdline-tools/latest/bin/sdkmanager" ]; then
  echo "==> Installing Android command-line tools into $SDK_ROOT..."
  mkdir -p "$SDK_ROOT/cmdline-tools"
  curl -fsSL https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -o /tmp/cmdline-tools.zip
  rm -rf /tmp/cmdline-tools-extract
  unzip -q /tmp/cmdline-tools.zip -d /tmp/cmdline-tools-extract
  mv /tmp/cmdline-tools-extract/cmdline-tools "$SDK_ROOT/cmdline-tools/latest"
fi

SDKMANAGER="$SDK_ROOT/cmdline-tools/latest/bin/sdkmanager"
set +o pipefail
yes | "$SDKMANAGER" --sdk_root="$SDK_ROOT" --licenses >/dev/null 2>&1 || true
set -o pipefail
"$SDKMANAGER" --sdk_root="$SDK_ROOT" "platform-tools" "platforms;android-36" "build-tools;36.0.0" || true
"$SDKMANAGER" --sdk_root="$SDK_ROOT" "platforms;android-36.1" || true
"$SDKMANAGER" --sdk_root="$SDK_ROOT" "build-tools;36.1.0" || true

GRADLE_BIN="$(command -v gradle || true)"
if [ -z "$GRADLE_BIN" ]; then
  if [ ! -x "$HOME/.local/gradle-9.3.1/bin/gradle" ]; then
    echo "==> Downloading Gradle 9.3.1..."
    mkdir -p "$HOME/.local"
    curl -fsSL https://services.gradle.org/distributions/gradle-9.3.1-bin.zip -o /tmp/gradle-9.3.1.zip
    unzip -q -o /tmp/gradle-9.3.1.zip -d "$HOME/.local"
  fi
  GRADLE_BIN="$HOME/.local/gradle-9.3.1/bin/gradle"
fi

touch .env
if [ -f debug.keystore.base64 ] && [ ! -f debug.keystore ]; then
  base64 -d debug.keystore.base64 > debug.keystore
fi

GENERATED_PASS=$(openssl rand -hex 16)
rm -f /tmp/fevro-release.jks
keytool -genkeypair -v \
  -keystore /tmp/fevro-release.jks \
  -storetype PKCS12 \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -alias upload \
  -storepass "$GENERATED_PASS" \
  -keypass "$GENERATED_PASS" \
  -dname "CN=Fevro Browser, OU=Mobile, O=Fevro, C=US"

echo "==> Building Fevro Browser Release APK..."
KEYSTORE_PATH=/tmp/fevro-release.jks \
STORE_PASSWORD="$GENERATED_PASS" \
KEY_PASSWORD="$GENERATED_PASS" \
"$GRADLE_BIN" :app:assembleRelease --no-daemon

cp app/build/outputs/apk/release/app-release.apk ./fevro-browser-release.apk
rm -f /tmp/fevro-release.jks

echo ""
echo "DONE! Right-click 'fevro-browser-release.apk' in the left explorer and click Download."
ls -lh ./fevro-browser-release.apk
