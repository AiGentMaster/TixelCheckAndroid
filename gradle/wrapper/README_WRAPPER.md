# Gradle Wrapper Information

This project uses Gradle 7.4 as the build system. The Gradle Wrapper JAR file (`gradle-wrapper.jar`) has been intentionally kept small in the repository for demonstration purposes.

## Important Note

If you encounter compilation errors, please run:

```bash
./gradlew wrapper --gradle-version 7.4 --distribution-type bin
```

This command will regenerate the proper Gradle wrapper files including the correct JAR file.

## Alternative Approach

If you continue to encounter issues with the Gradle wrapper, clone the repository locally and run the Gradle wrapper command mentioned above to regenerate all necessary binary files.
