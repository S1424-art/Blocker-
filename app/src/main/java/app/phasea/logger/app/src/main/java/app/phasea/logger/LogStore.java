package app.phasea.logger;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

/** Private, local-only log file plus two small settings. */
final class LogStore {
    private static final String FILE_NAME = "diag.log";
    private static final long MAX_BYTES = 400_000L;
    private static final Object LOCK = new Object();
    private static final DateTimeFormatter TS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private LogStore() {}

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext()
                .getSharedPreferences("phasea", Context.MODE_PRIVATE);
    }

    static boolean isEnabled(Context c) {
        return prefs(c).getBoolean("enabled", true);
    }

    static void setEnabled(Context c, boolean on) {
        prefs(c).edit().putBoolean("enabled", on).apply();
    }

    static String extraPackagesRaw(Context c) {
        return prefs(c).getString("extra_packages", "");
    }

    static void setExtraPackagesRaw(Context c, String raw) {
        prefs(c).edit().putString(
                "extra_packages", raw == null ? "" : raw
        ).apply();
    }

    static Set<String> extraPackages(Context c) {
        Set<String> out = new HashSet<>();
        for (String s : extraPackagesRaw(c).split(",")) {
            String t = s.trim();
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }

    private static File file(Context c) {
        return new File(c.getApplicationContext().getFilesDir(), FILE_NAME);
    }

    static void append(Context c, String line) {
        synchronized (LOCK) {
            File f = file(c);
            String out = LocalDateTime.now().format(TS) + " " + line + "\n";
            try (FileOutputStream os = new FileOutputStream(f, true)) {
                os.write(out.getBytes(StandardCharsets.UTF_8));
            } catch (IOException ignored) {
                return;
            }
            if (f.length() > MAX_BYTES) trim(f);
        }
    }

    private static void trim(File f) {
        try {
            String all = readAll(f);
            int cut = all.indexOf('\n', all.length() / 2);
            String keep = cut >= 0 ? all.substring(cut + 1) : "";
            try (FileOutputStream os = new FileOutputStream(f, false)) {
                os.write(keep.getBytes(StandardCharsets.UTF_8));
            }
        } catch (IOException ignored) {
        }
    }

    private static String readAll(File f) throws IOException {
        if (!f.exists()) return "";
        try (FileInputStream is = new FileInputStream(f);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) > 0) bos.write(buf, 0, n);
            return new String(bos.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    static String tail(Context c, int maxLines) {
        synchronized (LOCK) {
            try {
                String all = readAll(file(c));
                if (all.isEmpty()) return "";
                String[] lines = all.split("\n");
                int start = Math.max(0, lines.length - maxLines);
                StringBuilder sb = new StringBuilder();
                for (int i = start; i < lines.length; i++) {
                    sb.append(lines[i]).append('\n');
                }
                return sb.toString();
            } catch (IOException e) {
                return "";
            }
        }
    }

    static long sizeBytes(Context c) {
        return file(c).length();
    }

    static void clear(Context c) {
        synchronized (LOCK) {
            File f = file(c);
            if (f.exists()) f.delete();
        }
    }
}
