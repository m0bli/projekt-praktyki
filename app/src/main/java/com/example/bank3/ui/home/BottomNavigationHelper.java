package com.example.bank3.ui.home;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.bank3.R;
import com.example.bank3.ui.theme.ThemeManager;

/** Wspólna dolna nawigacja dla ekranów głównych aplikacji. */
public final class BottomNavigationHelper {
    private BottomNavigationHelper() {}

    public static void attach(Activity activity, View content, long userId, int selectedTab) {
        if (!(content.getParent() instanceof ViewGroup)) return;
        ViewGroup parent = (ViewGroup) content.getParent();
        int index = parent.indexOfChild(content);
        ViewGroup.LayoutParams oldParams = content.getLayoutParams();
        parent.removeView(content);

        LinearLayout shell = new LinearLayout(activity);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setBackgroundColor(ThemeManager.getBackgroundColor(activity));
        shell.setLayoutParams(oldParams);
        parent.addView(shell, index, oldParams);

        shell.addView(content, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout nav = new LinearLayout(activity);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER);
        int d = (int) (activity.getResources().getDisplayMetrics().density + 0.5f);
        nav.setPadding(7*d, 7*d, 7*d, 7*d);
        nav.setBackgroundColor(ThemeManager.getSurfaceColor(activity));
        nav.setElevation(8*d);
        shell.addView(nav, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 76*d));

        String[] labels = {"Start", "Płatności", "Karty", "Więcej"};
        String[] icons = {"⌂", "⇄", "▣", "•••"};
        for (int i = 0; i < labels.length; i++) {
            final int tab = i;
            LinearLayout item = new LinearLayout(activity);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);
            item.setPadding(3*d, 5*d, 3*d, 5*d);
            item.setClickable(true);
            item.setFocusable(true);
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(i == selectedTab
                    ? ThemeManager.getAccentSoftColor(activity)
                    : ThemeManager.getSurfaceColor(activity));
            bg.setCornerRadius(18*d);
            item.setBackground(bg);
            LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.MATCH_PARENT, 1f);
            nav.addView(item, itemParams);

            TextView icon = new TextView(activity);
            icon.setText(icons[i]);
            icon.setGravity(Gravity.CENTER);
            icon.setTextSize(22);
            icon.setTextColor(i == selectedTab ? ThemeManager.getAccentColor(activity)
                    : ThemeManager.getSecondaryTextColor(activity));
            item.addView(icon);

            TextView label = new TextView(activity);
            label.setText(labels[i]);
            label.setGravity(Gravity.CENTER);
            label.setTextSize(11);
            label.setTextColor(i == selectedTab ? ThemeManager.getAccentColor(activity)
                    : ThemeManager.getSecondaryTextColor(activity));
            item.addView(label);

            item.setOnClickListener(v -> {
                if (tab == selectedTab) return;
                Intent intent;
                if (tab == 1) {
                    intent = new Intent(activity, TransferActivity.class);
                } else if (tab == 2) {
                    intent = new Intent(activity, CardsActivity.class);
                } else {
                    intent = new Intent(activity, MainMenuActivity.class);
                    if (tab == 3) intent.putExtra("openTab", 3);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                }
                intent.putExtra("uzytkownikId", userId);
                activity.startActivity(intent);
                if (tab == 0 || tab == 3) activity.finish();
            });
        }
    }
}