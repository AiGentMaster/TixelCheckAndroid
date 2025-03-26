# TixelCheck Android App

An Android application that monitors Tixel ticket URLs and alerts you when tickets become available.

**Note: This project has been thoroughly reviewed and fixed with updated dependencies and improved build configuration.**

## Features

- Monitor multiple Tixel ticket URLs
- Customizable checking frequency (1, 2, 5, 10, 30, or 60 minutes)
- Loud alerts with sound, vibration, and LED flash when tickets are found
- One-click access to ticket pages from notifications
- Test alert feature to verify notification settings
- Automatic restart after device reboot

## Building the App

TixelCheck is set up with GitHub Actions to automatically build APKs when code is pushed to the repository. You can download the latest APK from the Releases section of this repository.

### Important Note About Gradle Wrapper

Due to GitHub's file size limitations, the Gradle wrapper JAR file might not be properly included in this repository. Before building the app, you should regenerate the Gradle wrapper:

1. Clone this repository:
   ```
   git clone https://github.com/AiGentMaster/TixelCheckAndroid.git
   ```

2. Navigate to the project directory:
   ```
   cd TixelCheckAndroid
   ```

3. Regenerate the Gradle wrapper:
   ```
   gradle wrapper --gradle-version 7.4 --distribution-type bin
   ```
   Note: You need to have Gradle installed on your system to run this command. If you don't have it installed, you can download it from https://gradle.org/install/

### Manual Build Instructions

After regenerating the Gradle wrapper, you can build the app manually:

1. Build the debug APK:
   ```
   ./gradlew assembleDevelopmentDebug
   ```

2. The APK will be generated at:
   ```
   app/build/outputs/apk/development/debug/app-development-debug.apk
   ```

## Installation

1. Download the APK file from Releases or build it yourself
2. On your Android device, enable "Install from Unknown Sources" in settings
3. Install the APK by opening it on your device
4. Launch the TixelCheck app from your app drawer

## Using the App

1. **Adding URLs to Monitor**
   - Tap the "+" button to add a Tixel URL
   - Enter the URL and select a checking frequency
   - The app will begin monitoring automatically

2. **Managing URLs**
   - Use the toggle switch to enable/disable monitoring
   - Delete URLs you no longer need with the Delete button

3. **Testing Alerts**
   - Tap the "Test Alert" button to experience what alerts will sound like

4. **When Tickets Are Found**
   - You'll receive a notification with sound and vibration
   - Tap "Open Website" to go directly to the ticket page
   - Tap "Stop Alarm" to silence the alert

## Permissions

TixelCheck requires the following permissions:

- Internet: To check ticket availability on websites
- Vibrate: For alert vibrations
- Receive Boot Completed: To restart monitoring after device reboot
- Schedule Exact Alarm: For precise timing of checks
- Wake Lock: To ensure timely checks even when the device is sleeping

## Troubleshooting

- **No Notifications**: Ensure notifications are enabled for TixelCheck in system settings
- **App Not Running in Background**: Disable battery optimization for TixelCheck
- **Alarms Not Working**: Some Android manufacturers (Xiaomi, Huawei, etc.) have aggressive battery management - add TixelCheck to protected apps list
- **Build Errors**: Make sure you've regenerated the Gradle wrapper as described in the building instructions

## Recent Fixes

- Updated GitHub Actions workflow with latest action versions
- Updated Gradle to version 7.4 for better compatibility
- Updated Android Gradle Plugin to version 7.2.2
- Fixed build configuration for modern Android development

## License

This project is licensed under the MIT License - see the LICENSE file for details.
