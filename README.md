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

The project is currently at **Phase 4: target app selection and permission guidance**.

Implemented:
- Android/Kotlin Gradle project skeleton.
- Compose main screen with setup status, timing, offset chips, fallback coordinate display, and daily toggle.
- DataStore-backed settings for target package, target time, offset, fallback tap ratios, and daily enabled state.
- Installed launchable app picker for choosing Xiaomi Community or another target app.
- Manual package input fallback with installed/launchable package validation.
- Setup guidance for Accessibility settings, exact alarm settings, battery optimization settings, and Android 13+ notification permission.
- System-only Settings intent resolution to reduce fake settings screen interception risk.

Not implemented yet:
- Exact alarm scheduling.
- Alarm receiver and foreground service.
- Target app launch before tap time.
- AccessibilityService tap execution.
- Calibration screen.
- Manual `Test now` workflow.
- Local logs and diagnostics.

> [!WARNING]
> The current build can persist settings and guide setup, but it does not schedule alarms, launch Xiaomi Community, or perform any tap automation yet.

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
5. Toggle daily automation only if you want the setting persisted for later phases.

> [!NOTE]
> Accessibility settings can be opened now, but the MiUnlocker AccessibilityService is planned for a later phase and is not available in this build.

## Privacy And Permissions

MiUnlocker is designed as a local-only personal tool.

Current manifest permissions:
- `android.permission.SCHEDULE_EXACT_ALARM`
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
`-- ui/
    |-- MainScreen.kt
    |-- MainUiState.kt
    |-- MainViewModel.kt
    `-- theme/
```

## Roadmap

Next planned milestones:

- Phase 5: exact alarm scheduling, alarm receiver, foreground service, and target app launch attempt.
- Phase 6: AccessibilityService tap engine with node search and coordinate fallback.
- Phase 7: calibration screen and manual `Test now` mode.
- Phase 8: local logs and diagnostics.
- Phase 9: final reliability polish and release verification.

Before Phase 5, offset semantics need to be clarified: whether offsets are relative to `19:59:59.800` or absolute around `20:00:00.000`.

## Troubleshooting

- If the app picker does not show the target app, use manual package input.
- If exact alarm status stays blocked, check Android special app access settings for alarms and reminders.
- If notification status is blocked on Android 13+, grant notification permission from the app setup row or Android app settings.
- If battery optimization remains active, adjust the device manufacturer's battery/background restrictions manually.

## Verification

Latest local checks performed:

```bash
./gradlew :app:assembleDebug
```

The manifest was also checked for forbidden network and personal-data permissions; none were found.
