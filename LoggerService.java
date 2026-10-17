package app.phasea.logger;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import android.widget.Toast;

import java.util.LinkedHashSet;

/**
 * Observer (Phase A) plus an optional, time-limited Guard (Phase B).
 *
 * Always (unless logging is paused):
 *  - every window-change event: package name and class name only;
 *  - Settings (and packages you add yourself): the window title reported by the system and a
 *    short list of view resource IDs (structure only).
 *
 * Guard (only when its mode is MONITOR or GUARD and you selected protected apps):
 *  - on Settings screens only, node text is compared IN MEMORY with the names of your protected
 *    apps and a keyword list. Nothing read there is stored or logged except the protected
 *    package name on a match;
 *  - MONITOR logs matches. GUARD additionally sends you Home (the system Home action), limited by
 *    a lease and a circuit breaker.
 *
 * This is a screen-level convenience, not a security boundary: it does not stop force-stop,
 * uninstall, ADB, other routes to the same settings, or you switching this service off.
 */
public class LoggerService extends AccessibilityService {

    private static final String SETTINGS_PKG = "com.android.settings";
    private static final int MAX_NODES = 80;
    private static final int MAX_IDS = 20;
    private static final int MAX_TITLE_CHARS = 60;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final GuardEngine guard = new GuardEngine();
    private String lastWscLine = "";
    private String lastSnapLine = "";
    private String lastGuardLine = "";

    private final Runnable snapshotRunnable = new Runnable() {
        @Override
        public void run() {
            takeSnapshot();
        }
    };

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        LogStore.log(this, "SERVICE connected");
    }

    @Override
    public void onInterrupt() {
        LogStore.log(this, "SERVICE interrupted");
    }

    @Override
    public boolean onUnbind(Intent intent) {
        LogStore.log(this, "SERVICE unbound");
        return super.onUnbind(intent);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent e) {
        if (e == null) return;
        boolean logging = LogStore.isEnabled(this);
        Config.Mode mode = Config.effectiveMode(this);
        if (!logging && mode == Config.Mode.OFF) return;

        String pkg = str(e.getPackageName());
        if (pkg.isEmpty() || pkg.equals(getPackageName())) return;

        boolean settings = SETTINGS_PKG.equals(pkg);
        boolean wantSnapshot = (logging && isDetailed(pkg)) || (mode != Config.Mode.OFF && settings);
        int type = e.getEventType();

        if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (logging) {
                String line = "WSC pkg=" + pkg + " cls=" + str(e.getClassName());
                if (!line.equals(lastWscLine)) {
                    lastWscLine = line;
                    LogStore.append(this, line);
                }
            }
            if (wantSnapshot) schedule(300);
        } else if (type == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED && wantSnapshot) {
            schedule(500);
        }
    }

    // ---- snapshot: structure logging (Phase A) + guard evaluation (Phase B) ----

    private void schedule(long delayMs) {
        handler.removeCallbacks(snapshotRunnable);
        handler.postDelayed(snapshotRunnable, delayMs);
    }

    private void takeSnapshot() {
        boolean logging = LogStore.isEnabled(this);
        Config.Mode mode = Config.effectiveMode(this);
        if (!logging && mode == Config.Mode.OFF) return;

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;
        try {
            String pkg = str(root.getPackageName());

            if (logging && isDetailed(pkg)) {
                String winTitle = "";
                int winType = -1;
                AccessibilityWindowInfo w = root.getWindow();
                if (w != null) {
                    winTitle = str(w.getTitle());
                    winType = w.getType();
                    w.recycle();
                }

                LinkedHashSet<String> ids = new LinkedHashSet<>();
                int[] budget = {MAX_NODES};
                collectIds(root, ids, budget, 0);

                String line = "SNAP pkg=" + pkg
                        + " wintitle=" + quote(winTitle)
                        + " wintype=" + winType
                        + " ids=" + ids;
                if (!line.equals(lastSnapLine)) {
                    lastSnapLine = line;
                    LogStore.append(this, line);
                }
            }

            if (mode != Config.Mode.OFF && SETTINGS_PKG.equals(pkg)) {
                evaluateGuard(root, mode);
            }
        } finally {
            root.recycle();
        }
    }

    private void evaluateGuard(AccessibilityNodeInfo root, Config.Mode mode) {
        String hit = guard.match(this, root);
        if (hit == null) {
            lastGuardLine = "";
            return;
        }

        String line = "GUARD match pkg=" + hit + " mode=" + mode;
        if (!line.equals(lastGuardLine)) {
            lastGuardLine = line;
            LogStore.log(this, line);
        }

        if (mode != Config.Mode.GUARD || !GuardEngine.ACTIONS_COMPILED_IN) return;

        if (!guard.allowAction(this)) {
            LogStore.log(this, "GUARD breaker tripped -> MONITOR");
            Toast.makeText(this, "Protection Companion: Guard paused (too many actions).",
                    Toast.LENGTH_LONG).show();
            return;
        }

        boolean ok = performGlobalAction(GLOBAL_ACTION_HOME);
        LogStore.log(this, "GUARD action=HOME ok=" + ok + " pkg=" + hit);
        Toast.makeText(this, "Protection Companion: that App info screen is protected.",
                Toast.LENGTH_SHORT).show();
        lastGuardLine = "";
    }

    /** Collects view resource IDs only (never view text). */
    private void collectIds(AccessibilityNodeInfo n, LinkedHashSet<String> ids, int[] budget, int depth) {
        if (n == null || budget[0] <= 0 || depth > 12 || ids.size() >= MAX_IDS) return;
        budget[0]--;
        String id = n.getViewIdResourceName();
        if (id != null) {
            ids.add(id.replace("com.android.settings:id/", "s:").replace("android:id/", "a:"));
        }
        int count = n.getChildCount();
        for (int i = 0; i < count && budget[0] > 0 && ids.size() < MAX_IDS; i++) {
            AccessibilityNodeInfo child = n.getChild(i);
            if (child != null) {
                collectIds(child, ids, budget, depth + 1);
                child.recycle();
            }
        }
    }

    // ---- helpers --------------------------------------------------------

    private boolean isDetailed(String pkg) {
        return SETTINGS_PKG.equals(pkg) || LogStore.extraPackages(this).contains(pkg);
    }

    private static String str(CharSequence cs) {
        return cs == null ? "" : cs.toString();
    }

    private static String quote(String s) {
        String t = s.replace('\n', ' ').replace('\r', ' ');
        if (t.length() > MAX_TITLE_CHARS) t = t.substring(0, MAX_TITLE_CHARS) + "...";
        return "\"" + t + "\"";
    }
}
