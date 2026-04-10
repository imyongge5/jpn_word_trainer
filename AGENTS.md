# AGENTS.md

## Project overview

- This repository is for an Android app built with `Kotlin + Jetpack Compose`.
- Prefer Android Studio for project creation, SDK management, running the emulator, and Gradle sync.

## Dev environment tips

- Set `JAVA_HOME` to the Android Studio bundled JDK or another JDK 17+ install.
- Set `ANDROID_SDK_ROOT` and `ANDROID_HOME` to the local Android SDK directory.
- Ensure these SDK tool paths are available from the terminal:
  - `platform-tools`
  - `emulator`
  - `cmdline-tools\latest\bin`
- If command-line Android tools do not work, verify that `java -version`, `adb version`, and `sdkmanager.bat --version` resolve from a fresh terminal.

## Emulator notes

- A CLI-created Android 13 test emulator is available:
  - AVD name: `S22Ultra_API33`
  - Device profile: `pixel_9_pro_xl`
  - System image: `system-images;android-33;google_apis;x86_64`
- This emulator is intended as an approximate `Galaxy S22 Ultra` test device, not a perfect 1:1 Samsung hardware profile.
- Run it from CLI with: `emulator.exe -avd S22Ultra_API33`
- If the emulator is already running, check the active device with `adb devices`
- Common boot wait check:
  - `adb -s emulator-5554 shell getprop sys.boot_completed`
  - Wait until the result is `1`
- Unlock the emulator screen when input is blocked:
  - `adb -s emulator-5554 shell input keyevent 82`

## Emulator workflow

- Install the debug app:
  - `.\gradlew.bat :app:installDebug`
- Launch the app:
  - `adb -s emulator-5554 shell am start -n com.mistbottle.jpnwordtrainer/.MainActivity`
- Restart the app cleanly:
  - `adb -s emulator-5554 shell am force-stop com.mistbottle.jpnwordtrainer`
  - `adb -s emulator-5554 shell am start -n com.mistbottle.jpnwordtrainer/.MainActivity`
- Capture crash logs only:
  - `adb -s emulator-5554 logcat -d -b crash`
- Clear logcat before a repro:
  - `adb -s emulator-5554 logcat -c`
- Dump the current UI tree:
  - `adb -s emulator-5554 exec-out uiautomator dump /dev/tty`
- Save a UI dump to a file for later inspection:
  - `adb -s emulator-5554 exec-out uiautomator dump /dev/tty > $env:TEMP\wordbook-ui.xml`
- Capture a screenshot to a local file:
  - `adb -s emulator-5554 exec-out screencap -p > $env:TEMP\wordbook-screen.png`
- Tap a coordinate manually:
  - `adb -s emulator-5554 shell input tap 500 1200`
- Swipe manually:
  - `adb -s emulator-5554 shell input swipe 700 2200 700 900`

## Backend-assisted device testing

- If a feature depends on the API server, start the local backend first before opening the app:
  - `powershell -NoProfile -ExecutionPolicy Bypass -File backend/start-backend-bg.ps1 -BindHost 127.0.0.1 -Port 8010`
- Quick health check:
  - `Invoke-RestMethod http://127.0.0.1:8010/health`
- When testing built-in deck version APIs, make sure the app has both:
  - a saved server URL
  - a valid login token
- Built-in deck version endpoints currently used by the app:
  - `GET /builtin-decks/{stable_key}/versions`
  - `GET /builtin-decks/{stable_key}/versions/{version_code}`

## Setup commands

- Check Java: `java -version`
- Check adb: `adb version`
- Check SDK manager: `sdkmanager.bat --version`
- List emulators: `emulator.exe -list-avds`
- Launch Android Studio from the local install or IDE shortcut.

## Python rule

- If you need to create and use any `.py` file for project work, first create a virtual environment with `python -m venv venv`.

## Working conventions

- Prefer `rg` or `rg --files` for searching files and text.
- Prefer the Gradle wrapper (`gradlew`, `gradlew.bat`) once the Android project has been created instead of relying on a globally installed Gradle.
- Keep environment-specific paths in this file up to date if Android Studio or the Android SDK is moved.
