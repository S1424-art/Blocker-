package app.phasea.logger;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** Guard mode controls, the always-available kill switch, and the written recovery steps. */
public class GuardActivity extends Activity {

    private TextView modeValue;
    private TextView leaseValue;
    private TextView protectedValue;
    private TextView actsValue;
    private EditText keywordsField;
    private final List<Button> armButtons = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = Ui.column(this);
        root.addView(Ui.text(this, "Guard & recovery", 22, true, R.color.pc_primary));

        LinearLayout about = Ui.card(this);
        about.addView(Ui.text(this, "What Guard does", 16, true, R.color.pc_text));
        about.addView(Ui.text(this,
                "Guard watches the Settings app only. When the App info screen of a protected app opens, "
                        + "Monitor writes a log line, and an armed Guard also sends you to the Home screen.\n\n"
                        + "It does not block Settings or Wi-Fi and mobile data screens, does not stop other "
                        + "ways to force-stop or uninstall an app, and cannot stop you turning it off. "
                        + "Accessibility is a convenience layer, not tamper-proof protection.",
                14, false, R.color.pc_text));
        root.addView(about);

        LinearLayout status = Ui.card(this);
        status.addView(Ui.text(this, "Status", 16, true, R.color.pc_text));
        modeValue = Ui.valueRow(this, status, "Mode");
        leaseValue = Ui.valueRow(this, status, "Armed");
        protectedValue = Ui.valueRow(this, status, "Protected apps");
        actsValue = Ui.valueRow(this, status, "This build can act");
        root.addView(status);

        LinearLayout actions = Ui.card(this);
        actions.addView(Ui.text(this, "Mode", 16, true, R.color.pc_text));
        Button off = Ui.button(this, "Turn Guard OFF now", Ui.DANGER);
        off.setOnClickListener(v -> {
            Config.setMode(this, Config.Mode.OFF);
            LogStore.log(this, "GUARD mode -> OFF (user)");
            refresh();
        });
        actions.addView(off);
        Button monitor = Ui.button(this, "Monitor only (log, never act)", Ui.SECONDARY);
        monitor.setOnClickListener(v -> {
            Config.setMode(this, Config.Mode.MONITOR);
            LogStore.log(this, "GUARD mode -> MONITOR (user)");
            refresh();
        });
        actions.addView(monitor);
        actions.addView(armButton("Arm Guard for 15 minutes", 15));
        actions.addView(armButton("Arm Guard for 1 hour", 60));
        actions.addView(armButton("Arm Guard for 4 hours", 240));
        root.addView(actions);

        LinearLayout kw = Ui.card(this);
        kw.addView(Ui.text(this, "App info screen keywords", 16, true, R.color.pc_text));
        kw.addView(Ui.text(this,
                "A screen counts as a protected app's App info screen when it shows the app's name "
                        + "and one of these button words exactly. Edit them to match your phone's language.",
                13, false, R.color.pc_muted));
        keywordsField = new EditText(this);
        keywordsField.setText(Config.keywordsRaw(this));
        kw.addView(keywordsField);
        Button saveKw = Ui.button(this, "Save keywords", Ui.SECONDARY);
        saveKw.setOnClickListener(v -> {
            Config.setKeywordsRaw(this, keywordsField.getText().toString());
            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
        });
        kw.addView(saveKw);
        Button resetKw = Ui.button(this, "Restore default keywords", Ui.SECONDARY);
        resetKw.setOnClickListener(v -> {
            Config.setKeywordsRaw(this, "");
            keywordsField.setText(Config.keywordsRaw(this));
        });
        kw.addView(resetKw);
        root.addView(kw);

        LinearLayout rec = Ui.card(this);
        rec.addView(Ui.text(this, "Recovery", 16, true, R.color.pc_text));
        rec.addView(Ui.text(this,
                "1. Turn Guard OFF here.\n"
                        + "2. Guard also returns to Monitor by itself when the armed time ends, or after "
                        + GuardEngine.BREAKER_MAX_ACTIONS + " actions within a minute.\n"
                        + "3. Settings > Accessibility > Installed apps > " + getString(R.string.app_name)
                        + " > turn off. Guard never acts on Accessibility screens.\n"
                        + "4. Settings > Apps > " + getString(R.string.app_name)
                        + " > Uninstall. This app cannot protect itself.\n"
                        + "5. Safe mode (standard Android method): hold the power button, long-press "
                        + "Power off, tap Safe mode. Third-party services do not run there.\n\n"
                        + "Nothing here blocks calls, emergency calling, the keyboard, or the launcher.",
                14, false, R.color.pc_text));
        root.addView(rec);

        setContentView(Ui.page(this, root));
    }

    private Button armButton(String label, int minutes) {
        Button b = Ui.button(this, label, Ui.SECONDARY);
        b.setEnabled(GuardEngine.ACTIONS_COMPILED_IN);
        b.setOnClickListener(v -> confirmArm(minutes));
        armButtons.add(b);
        return b;
    }

    private void confirmArm(int minutes) {
        if (Config.protectedPackages(this).isEmpty()) {
            Toast.makeText(this, "Select at least one protected app first.", Toast.LENGTH_LONG).show();
            return;
        }
        if (!Config.isServiceEnabled(this)) {
            Toast.makeText(this, "Turn on the Accessibility service first.", Toast.LENGTH_LONG).show();
            return;
        }
        String span = minutes < 60 ? minutes + " minutes" : (minutes / 60) + (minutes == 60 ? " hour" : " hours");
        new AlertDialog.Builder(this)
                .setTitle("Arm Guard for " + span + "?")
                .setMessage("While armed, opening the App info screen of a protected app in Settings "
                        + "sends you to the Home screen. Guard returns to Monitor by itself when the time "
                        + "ends, or after " + GuardEngine.BREAKER_MAX_ACTIONS + " actions in one minute. "
                        + "You can turn it off here at any time.")
                .setPositiveButton("Arm", (d, w) -> {
                    Config.armGuard(this, minutes * 60_000L);
                    refresh();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        Config.Mode m = Config.effectiveMode(this);
        if (m == Config.Mode.GUARD) {
            long until = Config.leaseUntil(this);
            long left = Math.max(0L, (until - System.currentTimeMillis()) / 60_000L);
            modeValue.setText("GUARD (armed)");
            modeValue.setTextColor(getColor(R.color.pc_caution));
            leaseValue.setText("until " + DateFormat.getTimeFormat(this).format(new Date(until))
                    + " (" + left + " min left)");
        } else {
            modeValue.setText(m == Config.Mode.MONITOR ? "Monitor only" : "Off");
            modeValue.setTextColor(getColor(R.color.pc_text));
            leaseValue.setText("not armed");
        }
        protectedValue.setText(Config.protectedPackages(this).size() + " selected");
        actsValue.setText(GuardEngine.ACTIONS_COMPILED_IN ? "Yes" : "No (monitor-only build)");
    }
}
