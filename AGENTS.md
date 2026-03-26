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
