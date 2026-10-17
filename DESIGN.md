# Design: Protection Companion (Phase B)

## Purpose and scope
A companion that helps protect an existing BlockerX installation and reduce bypass
opportunities. It does not replace BlockerX. Phase B adds: app identity, a protected-app picker,
an opt-in screen-level Guard, connectivity test shortcuts, and written recovery. Not in Phase B:
Device Owner, device-policy changes, force-stop or uninstall prevention, network lockdown,
suspending or hiding any app, blocking Settings.

## Components
| File | Role |
|---|---|
| `MainActivity` | Home screen: status and navigation |
| `ProtectedAppsActivity` | Choose protected apps from launcher-visible apps |
| `GuardActivity` | Guard mode, kill switch, keywords, recovery text |
| `DiagnosticsActivity` | Phase A tools: log view, pause, markers, copy, clear |
| `ConnectivityActivity` | Buttons that open standard Wi-Fi / mobile-data screens |
| `LoggerService` | The Accessibility service: logging and Guard evaluation |
| `GuardEngine` | App-info-screen matcher and circuit breaker |
| `Config` | Local settings: mode, lease, protected apps, keywords |
| `LogStore` | Private log file and logging switches |
| `Ui` | Shared styling helpers |

## Guard flow
1. A window event arrives from the Settings app (and only then, for Guard).
2. After a short debounce the service reads the active window's node tree.
3. `GuardEngine.match` looks for (a) a node whose whole text equals the name of a protected app
   and (b) a node whose whole text equals one of the keywords (default: Force stop, Uninstall,
   Disable, and Arabic equivalents). Both present = "App info screen of a protected app".
4. MONITOR: log `GUARD match pkg=... mode=MONITOR`. GUARD: also perform the system Home action,
   show a short toast, log the action.
5. Safety: a GUARD lease (15 min / 1 h / 4 h, never above 4 h) falls back to MONITOR;
   5 actions within 60 s trip a breaker back to MONITOR; the app itself can never be protected;
   mode OFF is the default; `GuardEngine.ACTIONS_COMPILED_IN = false` builds an APK that can only log.

## What is read, stored and logged
| Data | Read? | Stored / logged? |
|---|---|---|
| Foreground package + class name (all apps) | yes | logged |
| Window title + view resource IDs (Settings, extra packages you add) | yes | logged (title cut to 60 chars) |
| Text of on-screen nodes in Settings (Guard only, when mode is not OFF) | yes, in memory | never stored or logged; only the matched protected package name is logged |
| Typed text, passwords, messages, notifications | never requested | no |
| Names of protected apps, keywords, mode, lease | n/a | stored in private preferences |
| Your markers | typed by you | logged |

Window titles in Settings can contain user-controlled strings (for example a Wi-Fi network
name). Review the log before sharing it.

## What Accessibility can enforce
- Observe which package/window is in front and read what the app exposes about its screen.
- React after the fact: perform system actions such as Home or Back.
- Do that only while the service is enabled, running and receiving events.

## What Accessibility cannot enforce
- It cannot prevent force-stop, uninstall, disabling or clearing data. It can only react to one
  screen, and only after it has appeared (a brief flash is normal).
- It sees only the Settings screen's accessible tree. Other routes to the same actions are not
  covered: other Settings pages (Storage > Clear data, Battery), launcher long-press menus, Play
  Store uninstall, Samsung Device Care, ADB, Safe mode, a factory reset.
- It can be switched off by you, killed by the system, or be unavailable (for example while the
  phone is locked after a reboot), and it depends on app and OS wording which differs by language
  and One UI version. Screen "identity" is inferred from text, not verified.
- Therefore it is not tamper-proof protection. Real prevention needs system-level policy (Device
  Owner), which is deliberately not part of Phase B.

## Permissions and capabilities
- No `<uses-permission>` entries. No network, storage, contacts, location, or notification permission.
- `<queries>` for the launcher intent: lets the picker list apps that have a launcher icon on
  Android 11+. This is not the broad QUERY_ALL_PACKAGES permission.
- Service `android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"`: only the system may
  bind to the service. It is not a permission the app requests.
- Accessibility capabilities: `canRetrieveWindowContent` (read the Settings screen tree for
  titles, IDs and Guard matching), `flagReportViewIds` (view IDs), `flagRetrieveInteractiveWindows`
  (window title). The Home action needs no extra capability.
