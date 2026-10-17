# Protection Companion (Phase B): build, install, use, remove

Companion to BlockerX for a Samsung Galaxy A32 5G (SM-A326B, Android 13). It does NOT replace
BlockerX, does NOT use Device Owner, does NOT change device policies or settings, and requests
NO permissions (no network).

Docs: `docs/DESIGN.md` (architecture, what Accessibility can and cannot enforce),
`docs/RECOVERY.md` (how to switch everything off), `docs/TEST_PLAN.md` (manual tests).

## Toolchain (pinned; unchanged from Phase B)

| Component | Version | Where it is set |
|---|---|---|
| JDK | 17 (Temurin) | workflow `actions/setup-java`, `app/build.gradle` compile options |
| Gradle | 8.7 | workflow `gradle/actions/setup-gradle` |
| Android Gradle Plugin | 8.5.2 | root `build.gradle` |
| compileSdk / targetSdk / minSdk | 34 / 33 / 30 | `app/build.gradle` |
| SDK Build Tools | 34.0.0 (AGP 8.5 default) | provided by the CI runner / AGP |

Build command used by CI: `gradle --no-daemon assembleDebug`
(no Gradle wrapper is included; the workflow installs Gradle 8.7).

## 1. Build the debug APK (phone + Termux, no PC)

Same flow as Phase B. In your existing local clone (Termux), replace the project files with the
contents of this ZIP, then commit and push:

    cd ~ && unzip -o ~/storage/downloads/phasea-logger-phaseB.zip
    cd phasea
    git add -A && git commit -m "Phase B: Protection Companion"
    git push

(For a fresh repo, follow the Phase B steps: private repo, token with Contents + Workflows
read/write, `git init -b main`, add remote, push.)

Then: GitHub > Actions > "build-debug" > latest run > Artifacts > `protection-companion-phaseb-apk`.
If the run is red, open the failed step and send me the error lines. Unzip the artifact in
Termux: `unzip ~/storage/downloads/protection-companion-phaseb-apk.zip -d ~/apk`.

Signing: the standard Android debug signing configuration. CI creates a new debug key on each run,
so a new build cannot be installed over an older one: uninstall the old app first. This deletes
its log and settings (protected apps, Guard mode).

## 2. Install

Open `~/apk/app-debug.apk` from your file manager (or `termux-open ~/apk/app-debug.apk`), allow
"Install unknown apps" for that app, and install. The launcher name is "Protection Companion".

## 3. First run

1. Open the app. Home shows the status. Tap "Turn on Accessibility service".
2. Installed apps > "Protection Companion (observer)" > on. If greyed out (Restricted setting):
   tap it once, then Settings > Apps > Protection Companion > three-dot menu >
   "Allow restricted settings", and try again.
3. Back in the app, "Accessibility service" must read Enabled.
4. "Protected apps": tick BlockerX.
5. "Guard & recovery": leave it OFF, or choose "Monitor only" for testing. Do not arm Guard until
   the Monitor tests in `docs/TEST_PLAN.md` pass.

## 4. Where things are stored (all local)

- Settings and the protected-app list: private SharedPreferences `phasea`.
- Log: private file `files/diag.log` (about 400 KB cap, oldest lines trimmed).
- View, copy, pause or clear the log in "Diagnostics log".
- Line types: `SERVICE`, `WSC` (package + class), `SNAP` (Settings / extra packages: window
  title + view IDs), `MARK` (your markers), `LOGGING`, `CONFIG` (protected add/remove),
  `GUARD` (match, armed, action, lease ended, breaker).

## 5. Uninstall

1. Optional: Diagnostics log > Clear log.
2. Settings > Accessibility > Installed apps > Protection Companion (observer) > off.
3. Settings > Apps > Protection Companion > Uninstall. All app data is deleted.

## Permissions

None requested. See `docs/DESIGN.md` for the manifest `<queries>` entry and the accessibility
capabilities, and why each exists.

## Verification status

Read the message that accompanied this ZIP. This project has not been built with Gradle by its
author; the first CI run is the real build test.
