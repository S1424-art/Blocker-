# Protection Companion — Phase B

A local-only Android companion to an existing blocker. It is not tamper-proof and does not prevent force-stop, uninstall, Accessibility being switched off, or other bypass routes.

## Phase B behavior
- OFF by default.
- MONITOR records a match only.
- Armed Guard may send the user to Home when Settings appears to show the App info page of a selected app. This is a heuristic convenience, not a security boundary.
- Accessibility text is inspected in memory while Monitor/Guard is active; window titles and view IDs for Settings can still be recorded in diagnostics. Review logs before sharing.
- No `uses-permission` elements, no network code, no Device Owner, and no device-policy enforcement.

## Build
Use JDK 17, Gradle 8.7, AGP 8.5.2, and Android SDK Platform 34. Run `gradle --no-daemon assembleDebug`.
The APK path is `app/build/outputs/apk/debug/app-debug.apk`. The GitHub Actions artifact is `protection-companion-phaseb-apk`.

## Package identity
The application ID is `app.focusguard`, so Phase B can be installed next to Phase A (`app.phasea.logger`). GitHub-hosted CI generates a debug key on each fresh runner, so uninstall the previous Phase B build before installing a later build if Android reports a signing conflict.

## Safety
Keep Guard OFF until Monitor matching has been verified on the target phone. The automatic Home action does not prevent force-stop or uninstall and may false-match Settings screens. Use `docs/RECOVERY.md` to disable service or uninstall.
