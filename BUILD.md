# Build Instructions

## Required toolchain

- JDK 17
- Gradle 8.7
- Android Gradle Plugin 8.5.2
- Android SDK Platform 34

## Build the debug APK

From the repository root, run:

```bash
gradle --no-daemon assembleDebug
```

Expected output:

`app/build/outputs/apk/debug/app-debug.apk`

## GitHub Actions

The workflow must be located at:

`.github/workflows/build.yml`

It should build the debug APK and upload it as an artifact.

## Important

A successful workflow run and a downloadable APK are required before installation. Do not treat static source checks as proof of compilation success. This project is an observe-only Phase A prototype and must not enforce device policies or block applications.
