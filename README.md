# TixelCheck Android

An Android application for monitoring Tixel ticket availability.

## Gradle Fix

The Gradle configuration has been updated to fix compile issues:

1. Updated to Gradle 7.4
2. Updated Android Gradle Plugin to 7.2.2
3. Updated build scripts to use modern syntax

## Important Note

To fully fix the Gradle wrapper, you should regenerate the gradle-wrapper.jar locally after pulling these changes:

```bash
./gradlew wrapper --gradle-version 7.4 --distribution-type bin
```

This will ensure you have the correct binary files that couldn't be updated directly through GitHub.