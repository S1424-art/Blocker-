package app.phasea.logger;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
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
import android.widget.TextView;
import android.widget.Toast;

/** The Phase A tools: pause/resume, markers, extra title packages, view / copy / clear the log. */
public class DiagnosticsActivity extends Activity {

    private static final int VIEW_LINES = 150;

    private TextView status;
    private TextView logView;
    private Button toggleBtn;
    private EditText markerField;
    private EditText extraField;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = Ui.column(this);
        root.addView(Ui.text(this, "Diagnostics log", 22, true, R.color.pc_primary));
        TextView sub = Ui.text(this,
                "Private to this app. Records app and screen names only, never text you type or read.",
                14, false, R.color.pc_muted);
        sub.setPadding(0, 0, 0, Ui.dp(this, 12));
        root.addView(sub);

        LinearLayout statusCard = Ui.card(this);
        status = Ui.text(this, "", 14, false, R.color.pc_text);
        statusCard.addView(status);
        Button openA11y = Ui.button(this, "Open Accessibility settings", Ui.SECONDARY);
        openA11y.setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        statusCard.addView(openA11y);
        toggleBtn = Ui.button(this, "Pause logging", Ui.SECONDARY);
        toggleBtn.setOnClickListener(v -> {
            LogStore.setEnabled(this, !LogStore.isEnabled(this));
            LogStore.append(this, LogStore.isEnabled(this) ? "LOGGING resumed" : "LOGGING paused");
            refresh();
        });
        statusCard.addView(toggleBtn);
        root.addView(statusCard);

        LinearLayout markerCard = Ui.card(this);
        markerCard.addView(Ui.text(this, "Marker", 16, true, R.color.pc_text));
        markerField = new EditText(this);
        markerField.setHint("e.g. step 3: Wi-Fi page");
        markerField.setInputType(InputType.TYPE_CLASS_TEXT);
        markerField.setSingleLine(true);
        markerCard.addView(markerField);
        Button addMarker = Ui.button(this, "Add marker to log", Ui.SECONDARY);
        addMarker.setOnClickListener(v -> {
            String t = markerField.getText().toString().trim();
            if (!t.isEmpty()) {
                LogStore.append(this, "MARK " + t.replace('\n', ' '));
                markerField.setText("");
                refresh();
            }
        });
        markerCard.addView(addMarker);
        root.addView(markerCard);

        LinearLayout extraCard = Ui.card(this);
        extraCard.addView(Ui.text(this, "Extra packages for title logging", 16, true, R.color.pc_text));
        extraField = new EditText(this);
        extraField.setHint("package names, comma separated");
        extraField.setInputType(InputType.TYPE_CLASS_TEXT);
        extraField.setText(LogStore.extraPackagesRaw(this));
        extraCard.addView(extraField);
        Button saveExtra = Ui.button(this, "Save extra packages", Ui.SECONDARY);
        saveExtra.setOnClickListener(v -> {
            LogStore.setExtraPackagesRaw(this, extraField.getText().toString());
            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
        });
        extraCard.addView(saveExtra);
        root.addView(extraCard);

        LinearLayout logCard = Ui.card(this);
        logCard.addView(Ui.text(this, "Log", 16, true, R.color.pc_text));
        Button refreshBtn = Ui.button(this, "Refresh log view", Ui.SECONDARY);
        refreshBtn.setOnClickListener(v -> refresh());
        logCard.addView(refreshBtn);
        Button copyBtn = Ui.button(this, "Copy last " + VIEW_LINES + " lines", Ui.SECONDARY);
        copyBtn.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("phasea-log", LogStore.tail(this, VIEW_LINES)));
            Toast.makeText(this, "Copied. Review before sharing.", Toast.LENGTH_SHORT).show();
        });
        logCard.addView(copyBtn);
        Button clearBtn = Ui.button(this, "Clear log", Ui.DANGER);
        clearBtn.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Clear log?")
                .setMessage("This permanently deletes the diagnostic log on this phone.")
                .setPositiveButton("Clear", (d, w) -> {
                    LogStore.clear(this);
                    refresh();
                })
                .setNegativeButton("Cancel", null)
                .show());
        logCard.addView(clearBtn);

        logView = new TextView(this);
        logView.setTypeface(Typeface.MONOSPACE);
        logView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        logView.setTextColor(getColor(R.color.pc_text));
        logView.setTextIsSelectable(true);
        logView.setPadding(0, Ui.dp(this, 10), 0, 0);
        logCard.addView(logView);
        root.addView(logCard);

        setContentView(Ui.page(this, root));
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
        status.setText("Accessibility service: " + (Config.isServiceEnabled(this) ? "ENABLED" : "NOT enabled")
                + "\nLogging: " + (on ? "on" : "paused")
                + "\nLog size: " + (LogStore.sizeBytes(this) / 1024) + " KB");
        logView.setText(text.isEmpty() ? "(log is empty)" : text);
    }
}
