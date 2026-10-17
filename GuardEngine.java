package app.phasea.logger;

import android.content.Context;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Decides whether the Settings screen currently shown is the App info screen of a protected app.
 *
 * Privacy: node text is compared in memory against (a) the names of apps YOU selected and
 * (b) a short keyword list. Matching text is never stored or logged; only the protected package
 * name is logged on a match. Only the Settings app is ever scanned.
 *
 * Safety: a circuit breaker stops any action after BREAKER_MAX_ACTIONS within BREAKER_WINDOW_MS
 * and drops the mode to MONITOR, so a mistaken match can never trap you in a Home loop.
 */
final class GuardEngine {

    /** Set to false to build an APK that can never act, only log. */
    static final boolean ACTIONS_COMPILED_IN = true;

    static final int BREAKER_MAX_ACTIONS = 5;
    static final long BREAKER_WINDOW_MS = 60_000L;

    private static final int MAX_NODES = 160;
    private static final int MAX_DEPTH = 14;
    private static final int MAX_TEXT_CHARS = 80;

    private final ArrayDeque<Long> recentActions = new ArrayDeque<>();

    private static final class Hit {
        String pkg;
        boolean keyword;
    }

    /** Returns the protected package whose App info screen is showing, or null. */
    String match(Context c, AccessibilityNodeInfo root) {
        if (root == null) return null;
        Set<String> pkgs = Config.protectedPackages(c);
        if (pkgs.isEmpty()) return null;

        Map<String, String> labelToPkg = new HashMap<>();
        for (String p : pkgs) {
            labelToPkg.put(Config.label(c, p).trim().toLowerCase(Locale.ROOT), p);
        }
        Set<String> keywords = new HashSet<>(Config.keywords(c));
        if (keywords.isEmpty()) return null;

        Hit hit = new Hit();
        int[] budget = {MAX_NODES};
        scan(root, labelToPkg, keywords, hit, budget, 0);
        return hit.keyword ? hit.pkg : null;
    }

    private static void scan(AccessibilityNodeInfo n, Map<String, String> labels,
                             Set<String> keywords, Hit hit, int[] budget, int depth) {
        if (n == null || budget[0] <= 0 || depth > MAX_DEPTH) return;
        if (hit.pkg != null && hit.keyword) return;
        budget[0]--;
        inspect(n.getText(), labels, keywords, hit);
        inspect(n.getContentDescription(), labels, keywords, hit);
        int count = n.getChildCount();
        for (int i = 0; i < count && budget[0] > 0; i++) {
            AccessibilityNodeInfo child = n.getChild(i);
            if (child != null) {
                scan(child, labels, keywords, hit, budget, depth + 1);
                child.recycle();
            }
        }
    }

    private static void inspect(CharSequence cs, Map<String, String> labels,
                                Set<String> keywords, Hit hit) {
        if (cs == null) return;
        String t = cs.toString().trim().toLowerCase(Locale.ROOT);
        if (t.isEmpty() || t.length() > MAX_TEXT_CHARS) return;
        String p = labels.get(t);
        if (p != null && hit.pkg == null) hit.pkg = p;
        if (keywords.contains(t)) hit.keyword = true;
    }

    /**
     * Circuit breaker. Returns true if one more action may be taken now.
     * When the limit is hit it switches the mode to MONITOR and returns false.
     */
    boolean allowAction(Context c) {
        if (!ACTIONS_COMPILED_IN) return false;
        long now = SystemClock.elapsedRealtime();
        while (!recentActions.isEmpty() && now - recentActions.peekFirst() > BREAKER_WINDOW_MS) {
            recentActions.removeFirst();
        }
        if (recentActions.size() >= BREAKER_MAX_ACTIONS) {
            Config.setMode(c, Config.Mode.MONITOR);
            recentActions.clear();
            return false;
        }
        recentActions.addLast(now);
        return true;
    }
}
