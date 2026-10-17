package app.phasea.logger;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Shortcuts to the standard Android connectivity screens, for testing only.
 * They only open system screens; nothing is changed and no permission is needed.
 */
public class ConnectivityActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = Ui.column(this);
        root.addView(Ui.text(this, "Connectivity test shortcuts", 22, true, R.color.pc_primary));
        TextView sub = Ui.text(this,
                "These buttons open the normal Android Wi-Fi and mobile-data screens. "
                        + "They change nothing. Use them to confirm that Guard never interferes with "
                        + "connectivity settings.",
                14, false, R.color.pc_muted);
        sub.setPadding(0, 0, 0, Ui.dp(this, 12));
        root.addView(sub);

        LinearLayout card = Ui.card(this);
        card.addView(open("Wi-Fi settings", Settings.ACTION_WIFI_SETTINGS));
        card.addView(open("Internet panel (Wi-Fi + mobile data)", Settings.Panel.ACTION_INTERNET_CONNECTIVITY));
        card.addView(open("Mobile network settings", Settings.ACTION_NETWORK_OPERATOR_SETTINGS));
        card.addView(open("Wireless and networks", Settings.ACTION_WIRELESS_SETTINGS));
        root.addView(card);

        setContentView(Ui.page(this, root));
    }

    private Button open(String label, String action) {
        Button b = Ui.button(this, label, Ui.SECONDARY);
        b.setOnClickListener(v -> {
            try {
                startActivity(new Intent(action));
            } catch (ActivityNotFoundException | SecurityException e) {
                Toast.makeText(this, "Not available on this phone.", Toast.LENGTH_LONG).show();
            }
        });
        return b;
    }
}
