---
name: run-in-emulator
description: Open, run, launch or screenshot the Life Log app in the Android emulator (AVD LifeLog_API36), e.g. to show the user the app or verify a UI change on a device.
---

# Run Life Log in the emulator

Use the project launcher; it handles everything:

```powershell
.\run-in-emulator.cmd            # start emulator if needed, wait for boot, build, install, launch
.\run-in-emulator.cmd -SkipBuild # reinstall the last APK without building
```

What it does (`scripts/run-in-emulator.ps1`):
- JDK: newest `%USERPROFILE%\.jdks\jdk-21*`; SDK: `%LOCALAPPDATA%\Android\Sdk`
- Starts AVD `LifeLog_API36` (Android 16, 2 GB data partition) unless an emulator process is already running; logs go to `build/logs/emulator*.log`
- Builds `assembleDebug`, installs with `adb install -r` (Gradle's `installDebug` has failed to see the emulator here), launches `<lifelog.applicationId>/app.lifelog.MainActivity`

Notes for verifying UI:
- The emulator is slow: allow ~20 s for a cold app start after install, and several seconds after each tap.
- Screenshots: `adb shell screencap -p /sdcard/s.png` then `adb pull` into `build/logs/`.
- Compose event blocks expose text via `content-desc`, not `text`, in `uiautomator dump`.
- Gboard may show a stylus tutorial on first text input; dismiss it before typing.
- Disk space on C: is tight; the emulator refuses to start with less than ~3 GB free (see `build/logs/emulator.log` for `FATAL`).
