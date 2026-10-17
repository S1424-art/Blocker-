package app.phasea.logger;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Local-only configuration: which apps are protected, the Guard mode, and its lease.
 * Everything lives in this app's private SharedPreferences. Nothing is sent anywhere.
 */
final class Config {

    /** OFF: do nothing. MONITOR: log matches only. GUARD: also send the user Home (time-limited). */
    enum Mode { OFF, MONITOR, GUARD }

    /** Longest allowed GUARD lease. A stored lease beyond this is treated as invalid (expired). */
    static final long MAX_LEASE_MS = 4L * 60L * 60L * 1000L;

    /**
     * Button words that, together with a protected app's name, identify its App info screen.
     * English plus Arabic (Samsung wording; verify on your phone and edit in Guard screen if needed).
     */
    static final String DEFAULT_KEYWORDS = "Force stop,Uninstall,Disable,\u0641\u0631\u0636 \u0627\u0644\u0625\u064a\u0642\u0627\u0641,\u0625\u0644\u063a\u0627\u0621 \u0627\u0644\u062a\u062b\u0628\u064a\u062a,\u062a\u0639\u0637\u064a\u0644";

    private static final String K_MODE = "guard_mode";
    private static final String K_LEASE = "guard_lease_until";
    private static final String K_PKGS = "protected_pkgs";
    private static final String K_LABEL = "label.";
    private static final String K_KEYWORDS = "guard_keywords";

    private Config() {}

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences("protection_companion", Context.MODE_PRIVATE);
    }

    // ---- guard mode -------------------------------------------------------

    static Mode storedMode(Context c) {
        String s = prefs(c).getString(K_MODE, Mode.OFF.name());
        try {
            return Mode.valueOf(s);
        } catch (IllegalArgumentException e) {
            return Mode.OFF;
        }
    }

    /** Sets OFF or MONITOR (or GUARD without a lease, which immediately counts as expired). */
    static void setMode(Context c, Mode m) {
        SharedPreferences.Editor ed = prefs(c).edit().putString(K_MODE, m.name());
        if (m != Mode.GUARD) ed.remove(K_LEASE);
        ed.apply();
    }

    /** Arms GUARD for a limited time (clamped between 1 minute and MAX_LEASE_MS). */
    static void armGuard(Context c, long leaseMs) {
        long clamped = Math.max(60_000L, Math.min(leaseMs, MAX_LEASE_MS));
        prefs(c).edit()
                .putString(K_MODE, Mode.GUARD.name())
                .putLong(K_LEASE, System.currentTimeMillis() + clamped)
                .apply();
        LogStore.log(c, "GUARD armed for " + (clamped / 60_000L) + " min");
    }

    static long leaseUntil(Context c) {
        return prefs(c).getLong(K_LEASE, 0L);
    }

    /** The mode that is really in force. An expired or implausible GUARD lease falls back to MONITOR. */
    static Mode effectiveMode(Context c) {
        Mode m = storedMode(c);
        if (m != Mode.GUARD) return m;
        long now = System.currentTimeMillis();
        long until = leaseUntil(c);
        if (until <= now || until - now > MAX_LEASE_MS) {
            setMode(c, Mode.MONITOR);
            LogStore.log(c, "GUARD lease ended -> MONITOR");
            return Mode.MONITOR;
        }
        return Mode.GUARD;
    }

    // ---- protected apps ---------------------------------------------------

    static Set<String> protectedPackages(Context c) {
        Set<String> s = prefs(c).getStringSet(K_PKGS, null);
        Set<String> copy = new HashSet<>();
        if (s != null) copy.addAll(s);
        return copy;
    }

    static boolean isProtected(Context c, String pkg) {
        return protectedPackages(c).contains(pkg);
    }

    static String label(Context c, String pkg) {
        return prefs(c).getString(K_LABEL + pkg, pkg);
    }

    /** Adds or removes a package. This app itself can never be protected. */
    static void setProtected(Context c, String pkg, String label, boolean on) {
        if (on && pkg.equals(c.getPackageName())) return;
        Set<String> s = protectedPackages(c);
        SharedPreferences.Editor ed = prefs(c).edit();
        if (on) {
            s.add(pkg);
            ed.putString(K_LABEL + pkg, label);
        } else {
            s.remove(pkg);
            ed.remove(K_LABEL + pkg);
        }
        ed.putStringSet(K_PKGS, s).apply();
        LogStore.log(c, "CONFIG protected " + (on ? "add " : "remove ") + pkg);
    }

    // ---- App-info page keywords -------------------------------------------

    static String keywordsRaw(Context c) {
        String s = prefs(c).getString(K_KEYWORDS, null);
        return (s == null || s.trim().isEmpty()) ? DEFAULT_KEYWORDS : s;
    }

    static void setKeywordsRaw(Context c, String raw) {
        prefs(c).edit().putString(K_KEYWORDS, raw == null ? "" : raw).apply();
    }

    static List<String> keywords(Context c) {
        List<String> out = new ArrayList<>();
        for (String s : keywordsRaw(c).split(",")) {
            String t = s.trim().toLowerCase(Locale.ROOT);
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }

    // ---- service status -----------------------------------------------------

    static boolean isServiceEnabled(Context c) {
        String enabled = Settings.Secure.getString(c.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (enabled == null) return false;
        ComponentName me = new ComponentName(c, LoggerService.class);
        return enabled.contains(me.flattenToString()) || enabled.contains(me.flattenToShortString());
    }
}
