# ReminderApp

An Android-first React Native reminder and alarm application with a native Kotlin alarm engine.

The application is designed to work **offline after installation** and provides precise alarms, lock-screen notifications, a full-screen alarm experience, reminder text, Android system time picker, and voice TTS.

---

## Features

- Native Android exact alarms using `AlarmManager`
- `setExactAndAllowWhileIdle()` for precise alarms
- Android system `TimePickerDialog`
- Live current device time in the UI
- Reminder title and custom reminder message
- Lock-screen visible alarm notifications
- Full-screen alarm experience
- Stop and Snooze actions
- Android Text-to-Speech (TTS)
- Local alarm persistence using `SharedPreferences`
- Alarm restoration after device reboot
- Android 12+ exact-alarm permission handling
- Android 13+ notification permission handling
- Full-screen intent support
- Offline-first operation
- React Native UI with Kotlin native alarm services
- No backend or internet connection required for alarm scheduling

---

# 1. Architecture

```text
                         ┌─────────────────────────┐
                         │     React Native UI     │
                         │       TypeScript        │
                         └────────────┬────────────┘
                                      │
                                      │ NativeModules
                                      ▼
                         ┌─────────────────────────┐
                         │      AlarmModule        │
                         │          Kotlin         │
                         └────────────┬────────────┘
                                      │
                     ┌────────────────┼────────────────┐
                     │                │                │
                     ▼                ▼                ▼
              AlarmScheduler     SharedPrefs     Android APIs
                     │                │
                     ▼                ▼
              AlarmManager       Local Storage
                     │
                     │ Exact trigger
                     ▼
              ┌───────────────┐
              │ AlarmReceiver │
              └───────┬───────┘
                      │
          ┌───────────┼────────────┐
          │           │            │
          ▼           ▼            ▼
    Notification     TTS      Full-Screen Intent
          │           │            │
          │           │            ▼
          │           │     ┌───────────────┐
          │           └────►│ AlarmActivity │
          │                 └───────┬───────┘
          │                         │
          │                  ┌──────┴──────┐
          │                  ▼             ▼
          │                STOP          SNOOZE
          │
          ▼
     Lock Screen
```

---

# 2. Component Responsibilities

## React Native UI

Responsible for:

- Displaying current device time
- Selecting alarm time
- Creating/editing alarms
- Entering notification/reminder text
- Displaying scheduled alarms
- Enabling/disabling alarms
- Deleting alarms
- Calling native Android functionality

The React Native layer does **not** directly control `AlarmManager`.

---

## AlarmModule.kt

The bridge between JavaScript/TypeScript and native Android.

Typical responsibilities:

- Schedule an alarm
- Cancel an alarm
- Read stored alarms
- Request/check Android permissions
- Open system settings when required
- Launch the Android system time picker
- Communicate alarm data to native Kotlin code

The module is exposed to React Native through:

```text
NativeModules.AlarmModule
```

---

## AlarmScheduler.kt

Responsible for scheduling and cancelling alarms.

The important API is:

```kotlin
AlarmManager.setExactAndAllowWhileIdle()
```

This allows alarms to fire accurately even when Android enters Doze mode, subject to Android's exact-alarm rules and device restrictions.

Responsibilities:

- Calculate trigger time
- Schedule `PendingIntent`
- Cancel scheduled alarm
- Handle snooze scheduling
- Store/update alarm information

---

## AlarmReceiver.kt

Android `BroadcastReceiver` invoked by `AlarmManager`.

Flow:

```text
AlarmManager
     ↓
AlarmReceiver
     ↓
Notification
     ↓
TTS
     ↓
Full-screen alarm
```

The receiver should do only the work required to respond quickly to the alarm and hand off longer UI work appropriately.

---

## AlarmNotification.kt

Creates the Android notification.

Important notification properties include:

```kotlin
NotificationCompat.CATEGORY_ALARM
NotificationCompat.PRIORITY_MAX
NotificationCompat.VISIBILITY_PUBLIC
```

The notification:

- Appears on the lock screen when permitted by system settings
- Displays the reminder title
- Displays the reminder message
- Opens `AlarmActivity` when tapped
- Can launch the full-screen intent for an alarm

Android notification channels are created with high importance.

---

## AlarmActivity.kt

The full-screen alarm screen.

It is configured for lock-screen operation using Android activity attributes such as:

```xml
android:showWhenLocked="true"
android:turnScreenOn="true"
```

Responsibilities:

- Display alarm title
- Display reminder text
- Provide Stop
- Provide Snooze
- Wake/turn on the display when permitted
- Handle alarm dismissal

---

## Text-to-Speech

Android's native:

```text
android.speech.tts.TextToSpeech
```

is used.

When an alarm fires, the application can speak:

```text
Alarm title + reminder message
```

Example:

```text
Morning Exercise.
Time for your yoga and meditation.
```

TTS does not require an internet connection if the required voice/language data is installed on the device.

---

## SharedPreferences

Alarm information is persisted locally.

Example data:

```text
alarm ID
alarm time
title
reminder message
enabled state
repeat configuration
```

This allows the application to continue working without a backend.

---

## Boot Receiver

Android can terminate/restart application processes during device reboot.

The boot receiver restores enabled alarms after:

```text
BOOT_COMPLETED
LOCKED_BOOT_COMPLETED
```

The application therefore does not depend on the user manually reopening the app after every reboot.

---

# 3. Offline Architecture

The application is intentionally offline-first.

```text
                NO INTERNET
                    │
                    ▼
        ┌─────────────────────┐
        │   React Native UI   │
        └──────────┬──────────┘
                   │
                   ▼
           Native Kotlin
                   │
          ┌────────┴────────┐
          ▼                 ▼
   SharedPreferences   AlarmManager
          │                 │
          │                 ▼
          │            AlarmReceiver
          │                 │
          │        ┌────────┼────────┐
          │        ▼        ▼        ▼
          │   Notification  TTS  AlarmActivity
          │
          └──── Persistent Local State
```

No server is required to:

- Create an alarm
- Store an alarm
- Trigger an alarm
- Display a notification
- Speak TTS
- Snooze
- Stop
- Restore alarms after reboot

### TTS offline requirement

The Android device must have the appropriate TTS language/voice installed.

---

# 4. Project Structure

A simplified project structure:

```text
ReminderApp/
│
├── App.tsx
├── app.json
├── index.js
├── package.json
│
├── android/
│   └── app/
│       └── src/
│           └── main/
│               ├── AndroidManifest.xml
│               │
│               ├── java/
│               │   └── com/
│               │       └── reminderapp/
│               │           ├── MainActivity.kt
│               │           ├── MainApplication.kt
│               │           ├── AlarmModule.kt
│               │           ├── AlarmPackage.kt
│               │           ├── AlarmScheduler.kt
│               │           ├── AlarmReceiver.kt
│               │           ├── AlarmNotification.kt
│               │           ├── AlarmActivity.kt
│               │           └── BootReceiver.kt
│               │
│               └── res/
│                   └── ...
│
└── README.md
```

---

# 5. Prerequisites

## Required

Install:

- Node.js
- npm
- Java JDK 17
- Android Studio
- Android SDK
- Android SDK Platform 35
- Android SDK Build Tools
- Android Platform Tools
- React Native development environment
- A physical Android device or Android emulator

The project currently targets a modern React Native 0.87-based Android setup.

---

# 6. Java Configuration

Check:

```powershell
java -version
```

The project should use JDK 17.

Example:

```text
java version "17.x.x"
```

Set:

```text
JAVA_HOME=C:\Program Files\Java\jdk-17
```

Do not set `JAVA_HOME` to the `bin` directory.

Correct:

```text
JAVA_HOME=C:\Program Files\Java\jdk-17
```

Incorrect:

```text
JAVA_HOME=C:\Program Files\Java\jdk-17\bin
```

---

# 7. Android SDK Configuration

Typical Windows SDK location:

```text
C:\Users\<USERNAME>\AppData\Local\Android\Sdk
```

Set:

```text
ANDROID_HOME=C:\Users\<USERNAME>\AppData\Local\Android\Sdk
```

Add these to PATH:

```text
%ANDROID_HOME%\platform-tools
%ANDROID_HOME%\emulator
%ANDROID_HOME%\cmdline-tools\latest\bin
```

Verify:

```powershell
adb --version
```

and:

```powershell
adb devices
```

---

# 8. Clone the Repository

Clone the repository:

```powershell
git clone <YOUR_REPOSITORY_URL>
```

Example:

```powershell
git clone https://github.com/<username>/ReminderApp.git
```

Enter the project:

```powershell
cd ReminderApp
```

---

# 9. Install JavaScript Dependencies

Run:

```powershell
npm install
```

Do not commit `node_modules`.

---

# 10. Connect Android Device

Enable:

```text
Developer Options
USB Debugging
```

Connect the Android device.

Verify:

```powershell
adb devices
```

Expected:

```text
List of devices attached
XXXXXXXX    device
```

If the device is shown as `unauthorized`, unlock the phone and accept the USB debugging prompt.

---

# 11. Build Android

First clean:

```powershell
cd android
.\gradlew.bat clean
```

Build:

```powershell
.\gradlew.bat assembleDebug
```

Return to project root:

```powershell
cd ..
```

---

# 12. Run React Native

Start Metro:

```powershell
npx react-native start --reset-cache
```

Open another terminal:

```powershell
cd ReminderApp
```

Run:

```powershell
npx react-native run-android
```

---

# 13. Important: Debug vs Release

### Debug

A debug React Native application normally requires Metro.

```text
React Native App
      │
      └── Metro server
```

Therefore, the development computer/network connection is normally required.

### Release

A release APK contains the JavaScript bundle.

Therefore the installed release APK can operate without Metro.

For an offline test, build a release APK.

---

# 14. Build Release APK

From the project root:

```powershell
cd android
.\gradlew.bat assembleRelease
```

The APK is normally generated under:

```text
android/app/build/outputs/apk/release/
```

Install it:

```powershell
adb install -r app/build/outputs/apk/release/app-release.apk
```

Then disable Wi-Fi and mobile data and test the application.

---

# 15. Required Android Permissions

The application uses Android permissions/features including:

```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<uses-permission android:name="android.permission.USE_FULL_SCREEN_INTENT" />

<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />

<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />

<uses-permission android:name="android.permission.WAKE_LOCK" />
```

Actual permission behavior depends on Android version and manufacturer.

---

# 16. First Launch Setup

On first launch:

1. Allow notifications.
2. Allow exact alarms if Android requests it.
3. Allow full-screen notifications/intent if required by the device.
4. Check that TTS has the required language installed.
5. Create a test alarm.

For lock-screen testing, verify that the system allows the application to show notifications on the lock screen.

---

# 17. Testing Alarm Flow

Recommended test:

1. Open application.
2. Confirm current time is displayed.
3. Tap alarm time.
4. Select a time using the Android system TimePicker.
5. Enter notification/reminder text.
6. Save alarm.
7. Lock the phone.
8. Wait for the alarm.

Expected:

```text
Alarm time reached
       ↓
AlarmReceiver
       ↓
Notification appears
       ↓
Notification visible on lock screen
       ↓
TTS speaks reminder
       ↓
Full-screen alarm can appear
       ↓
STOP / SNOOZE
```

---

# 18. Lock-Screen Testing

If TTS works but the notification is not visible while locked, check:

```text
Settings
  ↓
Apps
  ↓
Reminder App
  ↓
Notifications
  ↓
Lock screen
  ↓
Show notifications
```

Also check:

```text
Settings
  ↓
Apps
  ↓
Special app access
  ↓
Full-screen notifications
```

Manufacturer names may differ.

---

# 19. Notification Channel Important Note

Android notification channels retain their configuration.

If notification-channel settings were created incorrectly during testing, changing Kotlin code does not always reset the existing channel.

For development testing, uninstall the application:

```powershell
adb uninstall com.reminderapp
```

Then reinstall it.

This creates the notification channel again with the current configuration.

---

# 20. Testing Reboot Recovery

1. Create an enabled alarm.
2. Confirm it is scheduled.
3. Restart the phone.
4. Wait for Android to finish booting.
5. Verify the alarm remains scheduled.

The boot receiver restores locally persisted alarms.

---

# 21. Testing Offline

After installing a release APK:

```text
Disable Wi-Fi
Disable Mobile Data
```

Test:

- Open application
- View current time
- Select alarm time
- Create alarm
- Lock device
- Wait for alarm
- Verify notification
- Verify TTS
- Stop alarm
- Snooze alarm

None of these operations require a backend.

---

# 22. Troubleshooting

## Gradle cannot find Java

Check:

```powershell
java -version
echo $env:JAVA_HOME
```

Ensure JDK 17 is configured.

---

## Android device not detected

Run:

```powershell
adb devices
```

If empty:

- Enable USB debugging
- Reconnect USB
- Accept debugging authorization
- Install the device manufacturer's USB driver if necessary

---

## Build fails after changing native Kotlin code

Clean:

```powershell
cd android
.\gradlew.bat clean
```

Then:

```powershell
.\gradlew.bat assembleDebug
```

---

## Metro cache problems

Run:

```powershell
npx react-native start --reset-cache
```

---

## Notification appears only after unlocking

Check:

1. Notification permission
2. Notification channel importance
3. `VISIBILITY_PUBLIC`
4. Lock-screen notification settings
5. Full-screen intent permission
6. Manufacturer-specific battery/notification restrictions

For a clean channel test:

```powershell
adb uninstall com.reminderapp
```

Then reinstall.

---

## TTS does not speak

Check:

```text
Android Settings
  ↓
Accessibility / Language & Input
  ↓
Text-to-Speech
```

Confirm:

- TTS engine exists
- Required language is installed
- Media/alarm volume is audible
- Device is not silenced by manufacturer-specific restrictions

---

# 23. Important Android Behavior

Exact alarm behavior is controlled by Android.

The application requests:

```text
SCHEDULE_EXACT_ALARM
```

but Android may require the user to explicitly enable exact alarms.

Similarly, lock-screen and full-screen behavior can be affected by:

- Android version
- Notification permission
- Full-screen intent permission
- Do Not Disturb
- Manufacturer ROM
- Battery optimization
- Lock-screen privacy settings

The application should therefore always handle these states gracefully.

---

# 24. Development Guidelines

Do not modify:

```text
node_modules
```

to fix native compilation errors.

Prefer fixing:

```text
android/app/src/main/java/com/reminderapp/
```

and the project Gradle configuration.

Keep the working React Native bootstrap intact unless there is a specific reason to change it.

---

# 25. Recommended Development Flow

Use this sequence for future changes:

```text
1. React Native UI change
        ↓
2. Test UI
        ↓
3. Native Kotlin change
        ↓
4. Gradle clean
        ↓
5. Build Debug
        ↓
6. Test foreground alarm
        ↓
7. Test background alarm
        ↓
8. Test locked-screen alarm
        ↓
9. Test reboot
        ↓
10. Test offline release APK
```

This prevents unrelated Android/RN configuration changes from making debugging difficult.

---

# 26. Future Enhancements

Potential next features:

- Repeat alarms
- Daily/weekly schedules
- Multiple reminder sounds
- Custom alarm sounds
- Volume escalation
- Vibration patterns
- Calendar integration
- Alarm history
- Categories
- Quick snooze durations
- Custom TTS language/voice
- TTS speed and pitch controls
- Backup/restore
- Export/import alarms
- Material 3 UI
- Dark mode
- Widget support
- Wear OS support

---

# 27. Summary

ReminderApp uses a hybrid architecture:

```text
React Native
    │
    ├── UI
    ├── Current time
    ├── System TimePicker
    └── User interaction
          │
          ▼
       Kotlin
          │
          ├── AlarmManager
          ├── BroadcastReceiver
          ├── NotificationManager
          ├── TextToSpeech
          ├── SharedPreferences
          └── AlarmActivity
```

The key design principle is:

> **React Native manages the user experience; native Android manages reliable alarm execution.**

This allows alarms to continue working when the application UI is not running, including background operation, lock-screen operation, and reboot recovery.
