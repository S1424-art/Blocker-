package app.phasea.logger;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Choose which installed apps (e.g. BlockerX) this companion should watch over. Local only. */
public class ProtectedAppsActivity extends Activity {

    private static final class AppEntry {
        String pkg;
        String label;
        boolean system;
        boolean installed;
    }

    private final List<AppEntry> apps = new ArrayList<>();
    private LinearLayout listHolder;
    private EditText search;
    private CheckBox showSystem;
    private TextView summary;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = Ui.column(this);
        root.addView(Ui.text(this, "Protected apps", 22, true, R.color.pc_primary));
        TextView sub = Ui.text(this,
                "Pick the apps this companion should watch over, such as BlockerX. "
                        + "Only apps with a launcher icon are listed. The choice is saved on this phone only "
                        + "and does nothing until you turn on Monitor or Guard.",
                14, false, R.color.pc_muted);
        sub.setPadding(0, 0, 0, Ui.dp(this, 12));
        root.addView(sub);

        LinearLayout controls = Ui.card(this);
        search = new EditText(this);
        search.setHint("Search apps");
        search.setSingleLine(true);
        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                render();
            }
        });
        controls.addView(search);
        showSystem = new CheckBox(this);
        showSystem.setText("Include system apps");
        showSystem.setOnCheckedChangeListener((b, checked) -> render());
        controls.addView(showSystem);
        summary = Ui.text(this, "Loading apps...", 13, false, R.color.pc_muted);
        controls.addView(summary);
        root.addView(controls);

        LinearLayout listCard = Ui.card(this);
        listHolder = Ui.column(this);
        listCard.addView(listHolder);
        root.addView(listCard);

        setContentView(Ui.page(this, root));
        loadApps();
    }

    private void loadApps() {
        new Thread(() -> {
            List<AppEntry> found = queryApps();
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                apps.clear();
                apps.addAll(found);
                render();
            });
        }).start();
    }

    private List<AppEntry> queryApps() {
        PackageManager pm = getPackageManager();
        Intent launcher = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> infos = pm.queryIntentActivities(launcher, 0);
        Map<String, AppEntry> map = new HashMap<>();
        for (ResolveInfo ri : infos) {
            String pkg = ri.activityInfo.packageName;
            if (pkg.equals(getPackageName()) || map.containsKey(pkg)) continue;
            AppEntry e = new AppEntry();
            e.pkg = pkg;
            e.label = String.valueOf(ri.loadLabel(pm));
            int f = ri.activityInfo.applicationInfo.flags;
            e.system = (f & ApplicationInfo.FLAG_SYSTEM) != 0
                    && (f & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0;
            e.installed = true;
            map.put(pkg, e);
        }
        // Previously protected packages that are not in the launcher list (removed, or no icon).
        for (String p : Config.protectedPackages(this)) {
            if (!map.containsKey(p)) {
                AppEntry e = new AppEntry();
                e.pkg = p;
                e.label = Config.label(this, p);
                e.installed = false;
                map.put(p, e);
            }
        }
        List<AppEntry> out = new ArrayList<>(map.values());
        Collections.sort(out, (a, b) -> a.label.compareToIgnoreCase(b.label));
        return out;
    }

    private void render() {
        listHolder.removeAllViews();
        String q = search.getText().toString().trim().toLowerCase(Locale.ROOT);
        boolean withSystem = showSystem.isChecked();
        int shown = 0;
        for (AppEntry e : apps) {
            boolean prot = Config.isProtected(this, e.pkg);
            if (e.system && !withSystem && !prot) continue;
            if (!q.isEmpty()
                    && !e.label.toLowerCase(Locale.ROOT).contains(q)
                    && !e.pkg.toLowerCase(Locale.ROOT).contains(q)) continue;
            listHolder.addView(row(e, prot));
            shown++;
        }
        updateSummary(shown);
    }

    private void updateSummary(int shown) {
        summary.setText(Config.protectedPackages(this).size() + " protected");
        if (shown >= 0) summary.append(", " + shown + " shown");
    }

    private View row(AppEntry e, boolean checked) {
        LinearLayout row = Ui.column(this);
        row.setPadding(0, Ui.dp(this, 6), 0, Ui.dp(this, 6));

        CheckBox cb = new CheckBox(this);
        cb.setText(e.installed ? e.label : e.label + " (not found)");
        cb.setTextSize(16);
        cb.setChecked(checked);
        cb.setOnCheckedChangeListener((b, on) -> {
            Config.setProtected(this, e.pkg, e.label, on);
            updateSummary(-1);
        });
        row.addView(cb);

        TextView pkg = Ui.text(this, e.pkg, 12, false, R.color.pc_muted);
        pkg.setPadding(Ui.dp(this, 48), 0, 0, 0);
        row.addView(pkg);
        return row;
    }
}
