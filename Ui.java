package app.phasea.logger;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Small helpers so every screen has the same look. */
final class Ui {

    static final int PRIMARY = 0;
    static final int SECONDARY = 1;
    static final int DANGER = 2;

    private Ui() {}

    static int dp(Context c, int v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                c.getResources().getDisplayMetrics());
    }

    static LinearLayout column(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    /** Wraps a content column in a scrolling page with the standard background and padding. */
    static ScrollView page(Context c, LinearLayout content) {
        ScrollView sv = new ScrollView(c);
        sv.setBackgroundColor(c.getColor(R.color.pc_bg));
        int p = dp(c, 16);
        content.setPadding(p, p, p, p);
        sv.addView(content, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));
        return sv;
    }

    static TextView text(Context c, CharSequence s, int sp, boolean bold, int colorRes) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(c.getColor(colorRes));
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setPadding(0, 0, 0, dp(c, 4));
        return t;
    }

    /** White rounded card. Must be added to a LinearLayout parent. */
    static LinearLayout card(Context c) {
        LinearLayout l = column(c);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(c.getColor(R.color.pc_surface));
        bg.setCornerRadius(dp(c, 14));
        bg.setStroke(dp(c, 1), c.getColor(R.color.pc_stroke));
        l.setBackground(bg);
        int p = dp(c, 14);
        l.setPadding(p, p, p, p);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(c, 12));
        l.setLayoutParams(lp);
        return l;
    }

    /** Full-width button. Must be added to a LinearLayout parent. */
    static Button button(Context c, CharSequence label, int style) {
        Button b = new Button(c);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        b.setMinHeight(dp(c, 48));
        b.setMinimumHeight(dp(c, 48));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(c, 12));
        if (style == PRIMARY) {
            bg.setColor(c.getColor(R.color.pc_primary));
            b.setTextColor(c.getColor(R.color.pc_on_primary));
        } else if (style == DANGER) {
            bg.setColor(c.getColor(R.color.pc_warn));
            b.setTextColor(c.getColor(R.color.pc_on_primary));
        } else {
            bg.setColor(c.getColor(R.color.pc_surface));
            bg.setStroke(dp(c, 1), c.getColor(R.color.pc_primary));
            b.setTextColor(c.getColor(R.color.pc_primary));
        }
        b.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(c, 8), 0, 0);
        b.setLayoutParams(lp);
        return b;
    }

    /** Adds "label ........ value" to parent and returns the value view so callers can update it. */
    static TextView valueRow(Context c, LinearLayout parent, String label) {
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(c, 4), 0, dp(c, 4));
        TextView l = text(c, label, 14, false, R.color.pc_muted);
        TextView v = text(c, "", 14, true, R.color.pc_text);
        v.setGravity(Gravity.END);
        row.addView(l, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(v, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        parent.addView(row, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        return v;
    }
}
