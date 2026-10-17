# Phase A Logger

An Android observe-only diagnostic prototype.

## Scope

- Records foreground package and class metadata.
- Can record structural resource IDs for Android Settings and explicitly selected packages.
- Stores diagnostic logs locally in the app's private storage.
- Does not block apps, change device policies, prevent uninstall, or send logs over the network.

## Requirements

- JDK 17
- Gradle 8.7
- Android Gradle Plugin 8.5.2
- Android SDK Platform 34

## Build

Run:

```bash
gradle --no-daemon assembleDebug
```

The expected APK path is:

`app/build/outputs/apk/debug/app-debug.apk`

## Privacy

Accessibility access is sensitive. Enable the service only for testing, review the local log, and disable the service when testing is complete. Avoid entering personal information in test markers or sharing logs without reviewing them.

## Status

This prototype must be built and tested before it is considered installable. Static checks do not prove that the APK builds or works correctly.
