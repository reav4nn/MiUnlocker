<div align="center">
  <img src="app/src/main/res/drawable-nodpi/miunlocker_logo.png" width="96" alt="MiUnlocker logo">

  # MiUnlocker

  *A personal Android helper for preparing time-sensitive Xiaomi Community unlock quota actions.*

  ![Android](https://img.shields.io/badge/Android-10%2B-3DDC84?style=flat-square&logo=android&logoColor=white)
  ![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-7F52FF?style=flat-square&logo=kotlin&logoColor=white)
  ![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=flat-square)
  ![Network](https://img.shields.io/badge/Network%20permission-none-success?style=flat-square)

  [Overview](#overview) | [Current Status](#current-status) | [Build](#build) | [Setup](#setup) | [Privacy](#privacy-and-permissions)
</div>

## Overview

MiUnlocker is a native Android Kotlin app built with Jetpack Compose. Its goal is to help prepare a rootless, personal sideload workflow for tapping the Xiaomi Community `Apply for unlocking` button near a configured daily target time.

Default timing context:
- Time zone: `Asia/Baku`
- Base target time: `19:59:59.800`
- Default offset: `0ms`
- Default fallback tap ratios: `x=0.50`, `y=0.89`

> [!IMPORTANT]
> MiUnlocker is not affiliated with Xiaomi. It does not bypass device security, does not require root, and must not be described as guaranteed to work. Android background limits, screen state, Accessibility behavior, Xiaomi Community UI changes, and server quota timing can all affect results.

## Current Status

The project is currently at **Phase 5: exact alarm and foreground service**.

Implemented:
- Android/Kotlin Gradle project skeleton.
- Compose main screen with setup status, timing, offset chips, fallback coordinate display, and daily toggle.
- DataStore-backed settings for target package, target time, offset, fallback tap ratios, and daily enabled state.
- Installed launchable app picker for choosing Xiaomi Community or another target app.
- Manual package input fallback with installed/launchable package validation.
- Setup guidance for Accessibility settings, exact alarm settings, battery optimization settings, and Android 13+ notification permission.
- System-only Settings intent resolution to reduce fake settings screen interception risk.
- Exact alarm scheduling with `AlarmManager.setExactAndAllowWhileIdle`.
- Private alarm receiver that starts a foreground countdown service.
- Foreground notification channel showing `Auto tap armed` countdown status.
- Target app launch attempt 5 seconds before the configured tap time.
- Daily automation toggle now schedules and cancels the next exact alarm.

Not implemented yet:
- AccessibilityService tap execution.
- Calibration screen.
- Manual `Test now` workflow.
- Local logs and diagnostics.

> [!WARNING]
> The current build can schedule an exact alarm, launch the selected target app, and show a foreground countdown. It does not perform the final Accessibility tap yet.

## Build

### Requirements

- Android Studio or Android SDK command-line tools.
- JDK 17.
- Android SDK 35 available locally.
- Android 10+ device or emulator for installation.

### Assemble Debug APK

```bash
./gradlew :app:assembleDebug
```

The debug APK is generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Install with Android Studio, or with `adb`:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Setup

Current build setup flow:

1. Install and open MiUnlocker.
2. Open the setup rows to review Android Accessibility, exact alarm, battery optimization, and notification status.
3. Select the target app from installed launchable apps, or enter a package name manually.
4. Choose a timing offset if needed.
5. Toggle daily automation to schedule the next exact alarm.
6. Keep the device unlocked and screen ready near the target time.

> [!NOTE]
> The exact alarm starts a foreground service 5 seconds before the tap time. That lead time is for opening the selected target app; the final tap command is planned for Phase 6.

## Privacy And Permissions

MiUnlocker is designed as a local-only personal tool.

Current manifest permissions:
- `android.permission.SCHEDULE_EXACT_ALARM`
- `android.permission.FOREGROUND_SERVICE`
- `android.permission.FOREGROUND_SERVICE_SPECIAL_USE`
- `android.permission.POST_NOTIFICATIONS`

Current package visibility:
- Launcher-app visibility query for target app selection.
- No `QUERY_ALL_PACKAGES` permission.

Explicitly not requested:
- No `INTERNET` permission.
- No contacts permission.
- No files/media permission.
- No SMS permission.
- No call/phone permission.

Installed app names are used only for target selection and stay on-device. App settings are stored locally with Android DataStore Preferences.

## Project Structure

```text
app/src/main/java/com/reavann/miunlocker/
|-- MainActivity.kt
|-- data/
|   |-- AppSettings.kt
|   |-- InstalledAppsRepository.kt
|   |-- SettingsRepository.kt
|   `-- SetupStatusRepository.kt
|-- scheduling/
|   |-- AlarmReceiver.kt
|   `-- ExactAlarmScheduler.kt
|-- automation/
|   `-- TapForegroundService.kt
`-- ui/
    |-- MainScreen.kt
    |-- MainUiState.kt
    |-- MainViewModel.kt
    `-- theme/
```

## Roadmap

Next planned milestones:

- Phase 6: AccessibilityService tap engine with node search and coordinate fallback.
- Phase 7: calibration screen and manual `Test now` mode.
- Phase 8: local logs and diagnostics.
- Phase 9: final reliability polish and release verification.

Phase 5 applies offsets relative to the stored base target time. The alarm trigger is scheduled 5 seconds before the final tap time so app launch delay is handled separately from the tap moment.

## Troubleshooting

- If the app picker does not show the target app, use manual package input.
- If exact alarm status stays blocked, check Android special app access settings for alarms and reminders.
- If notification status is blocked on Android 13+, grant notification permission from the app setup row or Android app settings.
- If battery optimization remains active, adjust the device manufacturer's battery/background restrictions manually.
- If the foreground notification does not appear, check notification permission and app notification settings.

## Verification

Latest local checks performed:

```bash
./gradlew :app:assembleDebug
```

The manifest was also checked for forbidden network and personal-data permissions; none were found.
