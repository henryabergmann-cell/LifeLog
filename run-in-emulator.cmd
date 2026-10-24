@echo off
rem Double-click to open Life Log in the Android emulator (builds the latest version first).
rem Add -SkipBuild to reinstall the last build without compiling.
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\run-in-emulator.ps1" %*
