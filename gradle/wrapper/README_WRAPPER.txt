# Gradle Wrapper JAR

This directory should contain the gradle-wrapper.jar file, but it may be missing due to GitHub's file size limitations.

To regenerate the Gradle wrapper properly, run the following command from the root of the project:

```
gradle wrapper --gradle-version 7.4 --distribution-type bin
```

This will create the proper gradle-wrapper.jar file in this directory.

Alternatively, you can download the wrapper JAR from the official Gradle repository.
