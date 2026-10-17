# Recovery and kill switches

Guard can only act when ALL of these are true: the Accessibility service is on, Guard mode is
GUARD (armed, within its time limit), and the Settings screen showing is the App info screen of an
app you selected. Phase B never blocks Settings in general, calls, emergency calling, the keyboard,
the launcher or connectivity screens.

## Switch it off (fastest first)
1. App > Guard & recovery > "Turn Guard OFF now".
2. Wait: armed Guard returns to Monitor by itself when its time ends (15 min / 1 h / 4 h, never more
   than 4 h), or immediately after 5 actions in one minute.
3. Settings > Accessibility > Installed apps > Protection Companion (observer) > off. Guard never
   acts on Accessibility screens.
4. Remove the protected app in "Protected apps" (untick it).
5. Uninstall: Settings > Apps > Protection Companion > Uninstall. This app's own App info screen is
   never matched because the app cannot protect itself.
6. Safe mode (standard Android method, not tested here): hold the power button, long-press
   "Power off", tap "Safe mode". Third-party apps and services do not run until you restart.

## If you cannot open the app
Use steps 3 and 5 from Settings. If a Home action seems to repeat, wait: the breaker stops it after
5 actions in a minute.

## Build-time kill switch
In `GuardEngine.java`, set `ACTIONS_COMPILED_IN = false` and rebuild: the APK can only log.

## What this does not cover
Phase B has no Device Owner, so there is nothing to un-provision and no factory reset is ever needed
to recover from this app.
