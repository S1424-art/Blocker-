# Manual test plan: Android 13, Samsung Galaxy A32 5G (SM-A326B)

Setup: install the Phase B debug APK alongside Phase A (the package IDs differ), enable the Accessibility
service, tick BlockerX in "Protected apps". Mark each test PASS / FAIL and note the log lines.

Note: GitHub-hosted runners generate a debug signing key for each fresh runner. To install a later CI build
over an already installed Phase B build, uninstall the old Phase B build first; this does not require removing Phase A.

## A. Identity and regressions
- T1 Launcher icon and name: the app shows as "Protection Companion" with the shield icon; with
  themed icons on (Android 13), a single-colour version appears.
- T2 Home screen: status rows are correct (service, Guard, protected count, logging) and buttons open
  the four screens and return.
- T3 Phase A tools (Diagnostics log): `SERVICE connected`, `WSC` and `SNAP` lines appear while you
  use Settings; Pause stops new lines; Resume continues; markers appear; Copy works; Clear asks for
  confirmation and empties the log.

## B. Configuration
- T4 Picker: BlockerX is listed; this app is not; search filters; system apps appear only with the
  toggle; ticking and unticking updates the count and writes a `CONFIG` line.
- T5 Persistence: the selection survives closing the app and a reboot.

## C. Guard (do these in order; keep Guard OFF until T6 passes)
- T6 Off: with Guard OFF open BlockerX's App info: no `GUARD` lines, nothing intercepted.
- T7 Monitor, positive: set Monitor; Settings > Apps > BlockerX: `GUARD match pkg=<BlockerX> mode=MONITOR`
  appears; you are NOT sent Home. If it does not appear, check the `SNAP` lines, adjust the keywords
  in Guard & recovery to your phone's button wording, and retest. Record the wording that worked.
- T8 Monitor, negatives (no `GUARD match` line expected): the Apps list, another app's App info, the
  Wi-Fi page, Mobile networks, Accessibility settings, this app's own App info, Settings home.
- T9 Armed: arm for 15 minutes (confirm dialog appears). Open BlockerX's App info: you are sent Home,
  a toast shows, the log has `GUARD action=HOME ok=true`.
- T10 Breaker: while armed, open BlockerX's App info 6 times quickly: after 5 actions the mode falls
  to Monitor (`GUARD breaker tripped -> MONITOR`) and the 6th open is not intercepted.
- T11 Lease end: arm for 15 minutes, wait: the app shows Monitor and the log has `GUARD lease ended`.
- T12 Kill switch: arm, then "Turn Guard OFF now": BlockerX's App info opens normally.

## D. Connectivity and essentials
- T13 Connectivity shortcuts: each of the four buttons opens a screen (or says "Not available"). With
  Guard armed none of them is intercepted. Change nothing in them.
- T14 Essentials with Guard armed: open the dialer screen (do not call), type in a keyboard field,
  pull the notification shade and quick panel, use Home and Recents, open Settings home.

## E. Recovery
- T15 Turn the service off in Settings > Accessibility: Guard stops acting; Home shows "Not enabled".
- T16 Reboot with Guard armed: the service restarts, Home shows the correct state, the lease still ends
  on time and falls back to Monitor.
- T17 Uninstall from Settings > Apps: succeeds and removes the data.

## F. Known limits to observe (informational, not defects)
Record whether each of these is intercepted (expected: NOT intercepted, except the first): launcher
long-press > App info for BlockerX; Settings search > BlockerX; Storage > Clear data; Battery pages;
launcher or Play Store Uninstall; Samsung Device Care. This shows exactly what Phase C would need to
cover with system-level policy.

## G. Privacy and permissions
- T18 Settings > Apps > Protection Companion > Permissions shows none requested.
- T19 Read the log after the tests: only package names, class names, Settings window titles, view IDs,
  your markers, and `GUARD`/`CONFIG`/`SERVICE` lines. No typed text or message content.
