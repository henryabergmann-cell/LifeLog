<#
.SYNOPSIS
    Opens Life Log in the Android emulator: starts the emulator if needed, waits for it to
    boot, builds the current debug version, installs it and launches the app.

.PARAMETER SkipBuild
    Install the last built APK instead of building first (faster when nothing changed).

.EXAMPLE
    .\scripts\run-in-emulator.ps1
    .\scripts\run-in-emulator.ps1 -SkipBuild
#>
param(
    [switch]$SkipBuild
)

# Not 'Stop': in Windows PowerShell, adb messages on stderr (e.g. "no devices" while the
# emulator boots) would abort the script. Failures are checked explicitly instead.
$ErrorActionPreference = 'Continue'
$ProgressPreference = 'SilentlyContinue'

$ProjectDir = Split-Path -Parent $PSScriptRoot
$AvdName = 'LifeLog_API36'
$Sdk = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
$Adb = Join-Path $Sdk 'platform-tools\adb.exe'
$Emulator = Join-Path $Sdk 'emulator\emulator.exe'
$Apk = Join-Path $ProjectDir 'app\build\outputs\apk\debug\app-debug.apk'
$LogDir = Join-Path $ProjectDir 'build\logs'

function Step($text) { Write-Host "`n==> $text" -ForegroundColor Cyan }
function Fail($text) {
    Write-Host "`nERROR: $text" -ForegroundColor Red
    Read-Host 'Press Enter to close' | Out-Null
    exit 1
}

# --- Toolchain -------------------------------------------------------------------------
$jdk = Get-ChildItem (Join-Path $env:USERPROFILE '.jdks') -Directory -Filter 'jdk-21*' -ErrorAction SilentlyContinue |
    Sort-Object Name -Descending | Select-Object -First 1
if (-not $jdk) { Fail "JDK 21 not found in $env:USERPROFILE\.jdks" }
if (-not (Test-Path $Adb) -or -not (Test-Path $Emulator)) { Fail "Android SDK tools not found in $Sdk" }

$env:JAVA_HOME = $jdk.FullName
$env:ANDROID_HOME = $Sdk
$env:ANDROID_SDK_ROOT = $Sdk
$env:Path = "$($jdk.FullName)\bin;$Sdk\platform-tools;$env:Path"
New-Item -ItemType Directory -Force $LogDir | Out-Null

$freeGb = [math]::Round((Get-PSDrive C).Free / 1GB, 1)
if ($freeGb -lt 3) {
    Write-Host "Warning: only $freeGb GB free on C:. The emulator needs about 3 GB to start." -ForegroundColor Yellow
}

# --- Application ID (from gradle.properties) --------------------------------------------
$appIdLine = Select-String -Path (Join-Path $ProjectDir 'gradle.properties') -Pattern '^lifelog\.applicationId=(.+)$'
if (-not $appIdLine) { Fail 'lifelog.applicationId missing in gradle.properties' }
$AppId = $appIdLine.Matches[0].Groups[1].Value.Trim()

# --- Emulator ----------------------------------------------------------------------------
& $Adb start-server | Out-Null
# An emulator that is still booting shows up as "offline", so also look for its process.
$running = ((& $Adb devices) -match '^emulator-\d+\s') -or
    [bool](Get-Process -Name 'qemu-system-x86_64*' -ErrorAction SilentlyContinue)
if (-not $running) {
    $avds = & $Emulator -list-avds
    if ($avds -notcontains $AvdName) { Fail "Virtual device '$AvdName' not found. Available: $($avds -join ', ')" }

    Step "Starting the emulator ($AvdName)"
    Start-Process -FilePath $Emulator `
        -ArgumentList '-avd', $AvdName, '-memory', '2048', '-no-snapshot-save', '-no-boot-anim' `
        -RedirectStandardOutput (Join-Path $LogDir 'emulator.log') `
        -RedirectStandardError (Join-Path $LogDir 'emulator.err.log') | Out-Null
}

Step 'Waiting for Android to boot'
$deadline = (Get-Date).AddMinutes(10)
while ($true) {
    $booted = (& $Adb shell getprop sys.boot_completed 2>$null) -join ''
    if ($booted.Trim() -eq '1') { break }
    if ((Get-Date) -gt $deadline) { Fail "The emulator didn't finish booting. See $LogDir\emulator.log" }
    if (-not (Get-Process -Name 'emulator', 'qemu-system-x86_64*' -ErrorAction SilentlyContinue)) {
        $reason = Select-String -Path (Join-Path $LogDir 'emulator.log') -Pattern 'FATAL' -ErrorAction SilentlyContinue |
            Select-Object -Last 1
        Fail "The emulator stopped. $reason"
    }
    Write-Host '.' -NoNewline
    Start-Sleep -Seconds 5
}
Write-Host ' ready'

# --- Build ---------------------------------------------------------------------------------
if (-not $SkipBuild) {
    Step 'Building the debug app (first build of the day can take a few minutes)'
    Push-Location $ProjectDir
    try {
        & .\gradlew.bat assembleDebug --console=plain *> (Join-Path $LogDir 'run-build.log')
        if ($LASTEXITCODE -ne 0) {
            Get-Content (Join-Path $LogDir 'run-build.log') | Select-String '^e: |error:|What went wrong' -Context 0, 3 |
                Select-Object -First 10 | ForEach-Object { Write-Host $_ -ForegroundColor Red }
            Fail "Build failed. Full log: $LogDir\run-build.log"
        }
    } finally {
        Pop-Location
    }
}
if (-not (Test-Path $Apk)) { Fail "No APK at $Apk. Run without -SkipBuild." }

# --- Install and launch ------------------------------------------------------------------
Step 'Installing'
$install = (& $Adb install -r $Apk) -join "`n"
if ($install -notmatch 'Success') { Fail "Install failed: $install" }

Step 'Launching Life Log'
# A freshly booted emulator sits on its lock screen (no PIN): wake it and dismiss the keyguard.
& $Adb shell input keyevent KEYCODE_WAKEUP | Out-Null
& $Adb shell wm dismiss-keyguard | Out-Null
& $Adb shell am start -n "$AppId/app.lifelog.MainActivity" | Out-Null
Write-Host "`nLife Log is open in the emulator." -ForegroundColor Green
Start-Sleep -Seconds 3
