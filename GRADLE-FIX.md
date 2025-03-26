# Gradle Wrapper Fix

The Gradle wrapper JAR file in this repository is corrupted or missing. Follow these steps to regenerate it:

1. Clone the repository:
   ```bash
   git clone https://github.com/AiGentMaster/TixelCheckAndroid.git
   cd TixelCheckAndroid
   ```

2. Run the following command to regenerate the Gradle wrapper:
   ```bash
   gradle -b regenerate-wrapper.gradle regenerateWrapper
   ```
   
   If you don't have Gradle installed, you can install it from https://gradle.org/install/

3. Alternatively, if you already have Gradle 7.4 installed, run:
   ```bash
   gradle wrapper --gradle-version 7.4 --distribution-type bin
   ```

4. Commit the changes:
   ```bash
   git add gradle/wrapper/gradle-wrapper.jar
   git add gradle/wrapper/gradle-wrapper.properties
   git add gradlew
   git add gradlew.bat
   git commit -m "Regenerate Gradle wrapper"
   git push
   ```
