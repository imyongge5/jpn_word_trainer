# AGENTS.md

## Project overview

- This repository is for an Android app built with `Kotlin + Jetpack Compose`.
- Prefer Android Studio for project creation, SDK management, running the emulator, and Gradle sync.

## Dev environment tips

- Android Studio is installed at `<ANDROID_STUDIO>`.
- The Android Studio bundled JDK is at `<ANDROID_JBR>`.
- `JAVA_HOME` should point to `<ANDROID_JBR>`.
- Android SDK is installed at `<ANDROID_SDK>`.
- `ANDROID_SDK_ROOT` and `ANDROID_HOME` should point to `<ANDROID_SDK>`.
- Useful SDK tool paths:
  - `<ANDROID_SDK>\platform-tools`
  - `<ANDROID_SDK>\emulator`
  - `<ANDROID_SDK>\cmdline-tools\latest\bin`
- If command-line Android tools do not work, verify that `java -version`, `adb version`, and `sdkmanager.bat --version` resolve from a fresh terminal.

## Setup commands

- Check Java: `java -version`
- Check adb: `adb version`
- Check SDK manager: `sdkmanager.bat --version`
- Launch Android Studio: `& '<ANDROID_STUDIO>'`

## Python rule

- If you need to create and use any `.py` file for project work, first create a virtual environment with `python -m venv venv`.

## Working conventions

- Prefer `rg` or `rg --files` for searching files and text.
- Prefer the Gradle wrapper (`gradlew`, `gradlew.bat`) once the Android project has been created instead of relying on a globally installed Gradle.
- Keep environment-specific paths in this file up to date if Android Studio or the Android SDK is moved.
