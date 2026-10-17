# Phase A — Manual Test Plan

## 1. Build verification

- Run the GitHub Actions build workflow.
- Confirm the workflow completes successfully.
- Confirm the `phasea-debug-apk` artifact exists.
- Download the APK only after a successful build.

## 2. Installation

- Install the debug APK on a test Android device.
- Open Phase A Logger.
- Confirm the app opens without crashing.

## 3. Accessibility service

- Enable the service manually from Android Accessibility settings.
- Return to the app and confirm that its status reports the service as enabled.
- Navigate between a few ordinary apps and Settings screens.
- Refresh the local log and review the recorded metadata.

## 4. Privacy checks

- Confirm that the log contains no screen text, typed text, or window titles.
- Confirm that logs remain in the app's private storage.
- Do not share copied logs without reviewing them.

## 5. Pause and clear

- Pause logging and navigate between apps.
- Confirm that new diagnostic event lines are not recorded while paused.
- Resume logging and confirm that logging resumes.
- Clear the log and confirm that it becomes empty.

## 6. Scope and safety

- Confirm that the app does not block applications.
- Confirm that it does not change device policies or prevent uninstall.
- Confirm that no network permission is declared.
- Disable the Accessibility service after testing.

## 7. Uninstallation

- Disable the Accessibility service.
- Uninstall the prototype through Android settings.
- Confirm that the prototype no longer appears in the installed-app list.

## Result recording

Record each test as PASS, FAIL, or NOT TESTED. A successful build alone does not mean that runtime tests have passed.
