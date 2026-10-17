package app.phasea.logger;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.util.TypedValue;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** Phase A screen: status, pause/resume, view/copy/clear local logs. */
public class MainActivity extends Activity {

    private static final int VIEW_LINES = 150;

    private TextView status;
    private TextView logView;
    private Button toggleBtn;
    private EditText markerField;
    private EditText extraField;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(12);
        root.setPadding(pad, pad, pad, pad);
        scroll.addView(root);

        TextView title = new TextView(this);
        title.setText("Phase A Logger (observe only)");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(title);

        TextView note = new TextView(this);
        note.setText("Records foreground app metadata and selected Settings "
                + "structure locally. It blocks nothing and sends no data.");
        note.setPadding(0, dp(4), 0, dp(8));
        root.addView(note);

        status = new TextView(this);
        status.setPadding(0, 0, 0, dp(8));
        root.addView(status);

        Button openA11y = new Button(this);
        openA11y.setText("Open Accessibility settings");
        openA11y.setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        root.addView(openA11y);

        toggleBtn = new Button(this);
        toggleBtn.setOnClickListener(v -> {
            LogStore.setEnabled(this, !LogStore.isEnabled(this));
            LogStore.append(this,
                    LogStore.isEnabled(this) ? "LOGGING resumed" : "LOGGING paused");
            refresh();
        });
        root.addView(toggleBtn);

        markerField = new EditText(this);
        markerField.setHint("Optional test marker");
        markerField.setInputType(InputType.TYPE_CLASS_TEXT);
        markerField.setSingleLine(true);
        root.addView(markerField);

        Button addMarker = new Button(this);
        addMarker.setText("Add marker to log");
        addMarker.setOnClickListener(v -> {
            String t = markerField.getText().toString().trim();
            if (!t.isEmpty()) {
                LogStore.append(this, "MARK " + t.replace('\n', ' '));
                markerField.setText("");
                refresh();
            }
        });
        root.addView(addMarker);

        extraField = new EditText(this);
        extraField.setHint("Extra package names (comma separated)");
        extraField.setInputType(InputType.TYPE_CLASS_TEXT);
        extraField.setText(LogStore.extraPackagesRaw(this));
        root.addView(extraField);

        Button saveExtra = new Button(this);
        saveExtra.setText("Save extra packages");
        saveExtra.setOnClickListener(v -> {
            LogStore.setExtraPackagesRaw(this, extraField.getText().toString());
            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
        });
        root.addView(saveExtra);

        Button refreshBtn = new Button(this);
        refreshBtn.setText("Refresh log view");
        refreshBtn.setOnClickListener(v -> refresh());
        root.addView(refreshBtn);

        Button copyBtn = new Button(this);
        copyBtn.setText("Copy last " + VIEW_LINES + " lines");
        copyBtn.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager)
                    getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(ClipData.newPlainText(
                        "phasea-log", LogStore.tail(this, VIEW_LINES)));
                Toast.makeText(this, "Copied. Review before sharing.",
                        Toast.LENGTH_SHORT).show();
            }
        });
        root.addView(copyBtn);

        Button clearBtn = new Button(this);
        clearBtn.setText("Clear log");
        clearBtn.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Clear log?")
                .setMessage("This deletes the local diagnostic log.")
                .setPositiveButton("Clear", (d, w) -> {
                    LogStore.clear(this);
                    refresh();
                })
                .setNegativeButton("Cancel", null)
                .show());
        root.addView(clearBtn);

        logView = new TextView(this);
        logView.setTypeface(Typeface.MONOSPACE);
        logView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        logView.setTextIsSelectable(true);
        logView.setPadding(0, dp(10), 0, 0);
        root.addView(logView);

        setContentView(scroll);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        boolean on = LogStore.isEnabled(this);
        toggleBtn.setText(on ? "Pause logging" : "Resume logging");
        String text = LogStore.tail(this, VIEW_LINES);
        status.setText("Accessibility service: "
                + (isServiceEnabled() ? "ENABLED" : "NOT enabled")
                + "\nLogging: " + (on ? "on" : "paused")
                + "\nLog size: " + (LogStore.sizeBytes(this) / 1024) + " KB");
        logView.setText(text.isEmpty() ? "(log is empty)" : text);
    }

    private boolean isServiceEnabled() {
        String enabled = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (enabled == null) return false;
        ComponentName me = new ComponentName(this, LoggerService.class);
        return enabled.contains(me.flattenToString())
                || enabled.contains(me.flattenToShortString());
    }

    private int dp(int v) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, v,
                getResources().getDisplayMetrics());
    }
}
