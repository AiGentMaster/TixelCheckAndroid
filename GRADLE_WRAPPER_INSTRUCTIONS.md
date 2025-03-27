# Gradle Wrapper Instructions

## Overview

This repository uses a GitHub Actions workflow to automatically regenerate the Gradle Wrapper files when needed. This approach works around the limitations of the GitHub API when dealing with binary files.

## When to Use the Regenerate Wrapper Workflow

The Gradle wrapper regeneration workflow should be used when:

1. You need to update the Gradle version
2. The Gradle wrapper JAR file is corrupted or incomplete
3. Build failures occur that are related to the Gradle wrapper

## How to Manually Trigger the Workflow

1. Go to the GitHub repository at https://github.com/AiGentMaster/TixelCheckAndroid
2. Click on the "Actions" tab
3. In the left sidebar, click on "Regenerate Gradle Wrapper"
4. Click the "Run workflow" dropdown on the right side
5. Choose the branch you want to run the workflow on (usually "main")
6. Click the green "Run workflow" button

## Automatic Triggering

The workflow will automatically run when:

- The `gradle/wrapper/gradle-wrapper.properties` file is changed in a pull request

## Troubleshooting Common Issues

If you encounter issues with the Gradle wrapper regeneration:

1. **Workflow fails with "No changes to commit"**:
   - This is normal if the wrapper files already match the expected state
   - No action is needed

2. **Workflow fails with permission errors**:
   - Ensure the repository settings allow GitHub Actions to create commits
   - Check that the `GITHUB_TOKEN` has appropriate permissions

3. **Compilation still fails after wrapper regeneration**:
   - Check the build.gradle files for compatibility issues with the Gradle version
   - Verify that the Android Gradle Plugin version is compatible with the Gradle version

## Local Alternative

If the GitHub Actions workflow is not working, you can regenerate the wrapper locally:

1. Clone the repository
2. Run the following command:
   ```bash
   ./gradlew wrapper --gradle-version 7.4 --distribution-type bin
   ```
3. If the above command fails, download Gradle 7.4 manually and run:
   ```bash
   gradle wrapper --gradle-version 7.4 --distribution-type bin
   ```
4. Commit and push the changes
