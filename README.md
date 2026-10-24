# Life Log

A calendar and appointment app for Android, built with Kotlin and Jetpack Compose. Free to use, with an optional Premium subscription.

> **Status:** in development, not yet on Google Play. See [PLAN.md](PLAN.md) for the roadmap and milestone progress.

## Features

**Free**
- Month, week, day and agenda views, with swiping between periods
- Create, edit, duplicate and delete appointments (with undo)
- All-day appointments, locations, notes, a reminder, and up to 3 color categories
- Fast search across titles, locations and notes
- English and German, light and dark theme, Material You colors

**Premium** (in progress)
- Journal: a daily note and mood, with a timeline
- Insights: appointments per week and month, busiest days, category breakdown
- Unlimited categories, multiple reminders, home-screen widgets, device-calendar sync, backup and restore

Coming next: repeating appointments and reminder notifications.

## Privacy

Your appointments, journal and settings stay on your device. There is no analytics or tracking. When subscriptions are added, only purchase information is sent to Google Play and RevenueCat to verify your subscription.

## Tech stack

Kotlin · Jetpack Compose · Material 3 · MVVM with clean layers (`domain`, `data`, `billing`, `app`) · Hilt · Room · DataStore · WorkManager · Navigation Compose · kotlinx.coroutines and serialization · JUnit, MockK, Turbine

minSdk 26 · targetSdk 36 · compileSdk 37

## Building

Requirements: JDK 17 or newer (JDK 21 recommended) and the Android SDK with platform 37.

1. Copy `local.properties.example` to `local.properties` and set `sdk.dir`. The other values can stay as they are for debug builds.
2. Build and run the tests:

   ```bash
   ./gradlew assembleDebug test
   ```

3. Install on a device or emulator: `./gradlew installDebug` (on Windows, `run-in-emulator.cmd` starts the emulator, builds, installs and launches the app).

Debug builds have a **Developer options → Simulate Premium** switch in Settings for trying Premium features. It does not exist in release builds.

## License

Life Log is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License, version 3. See [LICENSE](LICENSE).

The source code is open; the name "Life Log" and the app's store listing are not part of the license.
