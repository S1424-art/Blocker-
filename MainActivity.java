package app.phasea.logger;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Date;

/** Home screen: status at a glance and navigation. Changes nothing by itself. */
public class MainActivity extends Activity {

    private TextView svcValue;
    private TextView guardValue;
    private TextView protectedValue;
    private TextView loggingValue;
    private Button enableBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = Ui.column(this);

        root.addView(Ui.text(this, getString(R.string.app_name), 26, true, R.color.pc_primary));
        TextView sub = Ui.text(this, "Companion to BlockerX. Everything stays on this phone.",
                14, false, R.color.pc_muted);
        sub.setPadding(0, 0, 0, Ui.dp(this, 14));
        root.addView(sub);

        LinearLayout status = Ui.card(this);
        status.addView(Ui.text(this, "Status", 16, true, R.color.pc_text));
        svcValue = Ui.valueRow(this, status, "Accessibility service");
        guardValue = Ui.valueRow(this, status, "Guard");
        protectedValue = Ui.valueRow(this, status, "Protected apps");
        loggingValue = Ui.valueRow(this, status, "Diagnostic logging");
        enableBtn = Ui.button(this, "Turn on Accessibility service", Ui.PRIMARY);
        enableBtn.setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        status.addView(enableBtn);
        root.addView(status);

        root.addView(nav("Protected apps", Ui.PRIMARY, ProtectedAppsActivity.class));
        root.addView(nav("Guard & recovery", Ui.SECONDARY, GuardActivity.class));
        root.addView(nav("Diagnostics log", Ui.SECONDARY, DiagnosticsActivity.class));
        root.addView(nav("Connectivity test shortcuts", Ui.SECONDARY, ConnectivityActivity.class));

        TextView foot = Ui.text(this,
                "This app has no network permission and never changes system settings.",
                12, false, R.color.pc_muted);
        foot.setPadding(0, Ui.dp(this, 16), 0, 0);
        root.addView(foot);

        setContentView(Ui.page(this, root));
    }

    private Button nav(String label, int style, Class<?> target) {
        Button b = Ui.button(this, label, style);
        b.setOnClickListener(v -> startActivity(new Intent(this, target)));
        return b;
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        boolean on = Config.isServiceEnabled(this);
        svcValue.setText(on ? "Enabled" : "Not enabled");
        svcValue.setTextColor(getColor(on ? R.color.pc_ok : R.color.pc_warn));
        enableBtn.setVisibility(on ? View.GONE : View.VISIBLE);

        Config.Mode m = Config.effectiveMode(this);
        if (m == Config.Mode.GUARD) {
            guardValue.setText("Armed until "
                    + DateFormat.getTimeFormat(this).format(new Date(Config.leaseUntil(this))));
            guardValue.setTextColor(getColor(R.color.pc_caution));
        } else if (m == Config.Mode.MONITOR) {
            guardValue.setText("Monitor only");
            guardValue.setTextColor(getColor(R.color.pc_text));
        } else {
            guardValue.setText("Off");
            guardValue.setTextColor(getColor(R.color.pc_text));
        }

        protectedValue.setText(Config.protectedPackages(this).size() + " selected");
        loggingValue.setText(LogStore.isEnabled(this) ? "On" : "Paused");
    }
}
