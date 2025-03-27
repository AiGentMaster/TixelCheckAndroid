#!/bin/bash

# Script to regenerate Gradle wrapper files locally
# This script will download Gradle 7.4 and use it to regenerate the wrapper files

echo "Starting Gradle wrapper regeneration..."

# Determine OS-specific download
if [[ "$OSTYPE" == "darwin"* ]]; then
  # macOS
  GRADLE_PLATFORM="darwin"
elif [[ "$OSTYPE" == "msys" || "$OSTYPE" == "cygwin" || "$OSTYPE" == "win32" ]]; then
  # Windows
  GRADLE_PLATFORM="windows"
else
  # Linux/Unix
  GRADLE_PLATFORM="linux"
fi

GRADLE_VERSION="7.4"
GRADLE_ZIP="gradle-${GRADLE_VERSION}-bin.zip"
GRADLE_URL="https://services.gradle.org/distributions/${GRADLE_ZIP}"

echo "Downloading Gradle ${GRADLE_VERSION}..."

# Create temp directory
TMP_DIR=$(mktemp -d)
cd "$TMP_DIR"

# Download Gradle
if command -v curl > /dev/null; then
  curl -L -o "$GRADLE_ZIP" "$GRADLE_URL"
elif command -v wget > /dev/null; then
  wget -O "$GRADLE_ZIP" "$GRADLE_URL"
else
  echo "Error: Neither curl nor wget found. Please install one of them and try again."
  exit 1
fi

# Extract Gradle
echo "Extracting Gradle..."
if command -v unzip > /dev/null; then
  unzip -q "$GRADLE_ZIP"
else
  echo "Error: unzip not found. Please install unzip and try again."
  exit 1
fi

# Navigate back to project directory
cd -

# Regenerate wrapper
echo "Regenerating Gradle wrapper..."
"$TMP_DIR/gradle-${GRADLE_VERSION}/bin/gradle" wrapper --gradle-version "$GRADLE_VERSION" --distribution-type bin

# Clean up
echo "Cleaning up..."
rm -rf "$TMP_DIR"

echo "\nGradle wrapper regenerated successfully with version ${GRADLE_VERSION}."
echo "You should now commit the following files:"
echo "- gradle/wrapper/gradle-wrapper.jar"
echo "- gradle/wrapper/gradle-wrapper.properties"
echo "- gradlew"
echo "- gradlew.bat"
