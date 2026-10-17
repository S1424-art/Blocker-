package app.phasea.logger;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;

import java.util.LinkedHashSet;

/**
 * Phase A: observe and log only.
 * No blocking, clicks, or device-policy changes.
 *
 * Logs package/class metadata and structural resource IDs for Settings
 * and explicitly selected packages. Never reads or logs view text.
 */
public class LoggerService extends AccessibilityService {

    private static final String SETTINGS_PKG = "com.android.settings";
    private static final int MAX_NODES = 80;
    private static final int MAX_IDS = 20;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private String lastWscLine = "";
    private String lastSnapLine = "";

    private final Runnable snapshotRunnable = new Runnable() {
        @Override
        public void run() {
            takeSnapshot();
        }
    };

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        LogStore.append(this, "SERVICE connected");
    }

    @Override
    public void onInterrupt() {
        LogStore.append(this, "SERVICE interrupted");
    }

    @Override
    public boolean onUnbind(Intent intent) {
        LogStore.append(this, "SERVICE unbound");
        handler.removeCallbacks(snapshotRunnable);
        return super.onUnbind(intent);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent e) {
        if (e == null || !LogStore.isEnabled(this)) return;

        String pkg = str(e.getPackageName());
        if (pkg.isEmpty() || pkg.equals(getPackageName())) return;

        boolean detailed = isDetailed(pkg);
        int type = e.getEventType();

        if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            String line = "WSC pkg=" + pkg
                    + " cls=" + str(e.getClassName());
            if (!line.equals(lastWscLine)) {
                lastWscLine = line;
                LogStore.append(this, line);
            }
            if (detailed) schedule(300);
        } else if (type == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
                && detailed) {
            schedule(500);
        }
    }

    private void schedule(long delayMs) {
        handler.removeCallbacks(snapshotRunnable);
        handler.postDelayed(snapshotRunnable, delayMs);
    }

    private void takeSnapshot() {
        if (!LogStore.isEnabled(this)) return;

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;

        try {
            String pkg = str(root.getPackageName());
            if (!isDetailed(pkg)) return;

            int winType = -1;
            AccessibilityWindowInfo w = root.getWindow();
            if (w != null) {
                winType = w.getType();
                w.recycle();
            }

            LinkedHashSet<String> ids = new LinkedHashSet<>();
            int[] budget = {MAX_NODES};
            collectIds(root, ids, budget, 0);

            String line = "SNAP pkg=" + pkg
                    + " wintype=" + winType
                    + " ids=" + ids;

            if (!line.equals(lastSnapLine)) {
                lastSnapLine = line;
                LogStore.append(this, line);
            }
        } finally {
            root.recycle();
        }
    }

    private void collectIds(
            AccessibilityNodeInfo node,
            LinkedHashSet<String> ids,
            int[] budget,
            int depth) {

        if (node == null || budget[0] <= 0
                || depth > 12 || ids.size() >= MAX_IDS) return;

        budget[0]--;

        String id = node.getViewIdResourceName();
        if (id != null) {
            ids.add(id.replace("com.android.settings:id/", "s:")
                    .replace("android:id/", "a:"));
        }

        int count = node.getChildCount();
        for (int i = 0; i < count
                && budget[0] > 0 && ids.size() < MAX_IDS; i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                collectIds(child, ids, budget, depth + 1);
                child.recycle();
            }
        }
    }

    private boolean isDetailed(String pkg) {
        return SETTINGS_PKG.equals(pkg)
                || LogStore.extraPackages(this).contains(pkg);
    }

    private static String str(CharSequence value) {
        return value == null ? "" : value.toString();
    }
}
