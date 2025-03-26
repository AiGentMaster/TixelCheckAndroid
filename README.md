# TixelCheck Android App

An Android application that monitors Tixel ticket URLs and alerts you when tickets become available.

**Note: This project has been thoroughly reviewed and fixed with updated dependencies and improved build configuration. Now using direct Gradle installation rather than wrapper.**

## Features

- Monitor multiple Tixel ticket URLs
- Customizable checking frequency (1, 2, 5, 10, 30, or 60 minutes)
- Loud alerts with sound, vibration, and LED flash when tickets are found
- One-click access to ticket pages from notifications
- Test alert feature to verify notification settings
- Automatic restart after device reboot

## Building the App

TixelCheck is set up with GitHub Actions to automatically build APKs when code is pushed to the repository. You can download the latest APK from the Releases section of this repository.

### Manual Build Instructions

To build the app manually:

1. Install Gradle 7.2:
   ```
   # On Ubuntu
   sudo apt install gradle
   
   # On macOS
   brew install gradle
   
   # On Windows
   choco install gradle
   ```

2. Clone this repository:
   ```
   git clone https://github.com/AiGentMaster/TixelCheckAndroid.git
   ```

3. Navigate to the project directory:
   ```
   cd TixelCheckAndroid
   ```

4. Build the debug APK:
   ```
   gradle assembleProductionDebug
   ```

5. The APK will be generated at:
   ```
   app/build/outputs/apk/production/debug/app-production-debug.apk
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
- **Build Errors**: Make sure you have the correct version of Gradle installed

## Recent Fixes

- Updated GitHub Actions workflow to use Gradle directly instead of wrapper
- Updated Gradle to version 7.2 for better compatibility
- Simplified build configuration to focus on core functionality
- Updated Android Gradle Plugin to version 7.0.4

## License

This project is licensed under the MIT License - see the LICENSE file for details.
