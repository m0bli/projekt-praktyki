
package com.example.bank3.ui.home;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowInsetsControllerCompat;

import com.example.bank3.ui.theme.ThemeManager;

public class SettingsActivity extends AppCompatActivity {

    private int navy;
    private int gray;
    private int background;
    private int surface;
    private int accent;
    private boolean dark;

    private LinearLayout root;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);

        loadColors();
        updateSystemBars();
        createLayout();
        setContentView(root);
    }

    private void loadColors() {
        dark = ThemeManager.isBlack(this);

        background = dark
                ? Color.rgb(17, 24, 39)
                : Color.rgb(246, 248, 252);

        surface = dark
                ? Color.rgb(30, 41, 59)
                : Color.WHITE;

        navy = dark
                ? Color.rgb(232, 238, 249)
                : Color.rgb(23, 59, 112);

        gray = dark
                ? Color.rgb(165, 178, 200)
                : Color.rgb(119, 132, 154);

        accent = ThemeManager.getAccentColor(this);
    }

    private void updateSystemBars() {
        getWindow().setStatusBarColor(background);
        getWindow().setNavigationBarColor(background);

        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(
                        getWindow(),
                        getWindow().getDecorView()
                );

        controller.setAppearanceLightStatusBars(!dark);
        controller.setAppearanceLightNavigationBars(!dark);
    }

    private void createLayout() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(18), dp(20), dp(20));
        root.setBackgroundColor(background);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView back = new TextView(this);
        back.setText("‹");
        back.setTextSize(36);
        back.setTextColor(navy);
        back.setGravity(Gravity.CENTER);
        back.setBackground(round(surface, 16));

        header.addView(back, new LinearLayout.LayoutParams(
                dp(48), dp(48)
        ));

        back.setOnClickListener(v -> finish());

        TextView title = new TextView(this);
        title.setText("Ustawienia");
        title.setTextSize(25);
        title.setTextColor(navy);
        title.setTypeface(null, Typeface.BOLD);

        LinearLayout.LayoutParams titleParams =
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                );
        titleParams.leftMargin = dp(14);
        header.addView(title, titleParams);

        root.addView(header);

        TextView subtitle = new TextView(this);
        subtitle.setText("Dostosuj aplikację do swoich potrzeb");
        subtitle.setTextSize(14);
        subtitle.setTextColor(gray);
        subtitle.setPadding(0, dp(10), 0, dp(24));

        root.addView(subtitle);

        addSectionTitle("WYGLĄD");

        addSettingItem(
                "Themes",
                "Zmień motyw i kolorystykę aplikacji",
                "◐",
                v -> {
                    startActivity(new Intent(
                            SettingsActivity.this,
                            ThemesActivity.class
                    ));
                }
        );

        addSectionTitle("KONTO");

        addSettingItem(
                "Moje dane",
                "Informacje o Twoim koncie",
                "♙",
                v -> showMessage(
                        "Moje dane",
                        "Ta funkcja zostanie dodana później."
                )
        );

        addSettingItem(
                "Bezpieczeństwo",
                "Ustawienia bezpieczeństwa konta",
                "⌑",
                v -> showMessage(
                        "Bezpieczeństwo",
                        "Ta funkcja zostanie dodana później."
                )
        );

        addSectionTitle("APLIKACJA");

        addSettingItem(
                "Powiadomienia",
                "Zarządzanie powiadomieniami",
                "♧",
                v -> showMessage(
                        "Powiadomienia",
                        "Ta funkcja zostanie dodana później."
                )
        );

        TextView version = new TextView(this);
        version.setText("Bank3 • Ustawienia");
        version.setTextSize(12);
        version.setTextColor(gray);
        version.setGravity(Gravity.CENTER);
        version.setPadding(0, dp(28), 0, dp(8));

        root.addView(version);
    }

    private void addSectionTitle(String text) {
        TextView section = new TextView(this);
        section.setText(text);
        section.setTextSize(12);
        section.setTextColor(gray);
        section.setTypeface(null, Typeface.BOLD);
        section.setPadding(dp(4), dp(8), 0, dp(10));

        root.addView(section);
    }

    private void addSettingItem(
            String titleText,
            String descriptionText,
            String iconText,
            View.OnClickListener listener
    ) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setPadding(dp(14), dp(14), dp(12), dp(14));
        item.setBackground(round(surface, 18));
        item.setClickable(true);
        item.setFocusable(true);

        TextView icon = new TextView(this);
        icon.setText(iconText);
        icon.setTextSize(23);
        icon.setTextColor(accent);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(round(
                dark
                        ? Color.rgb(44, 57, 78)
                        : Color.rgb(232, 240, 255),
                14
        ));

        item.addView(icon, new LinearLayout.LayoutParams(
                dp(48), dp(48)
        ));

        LinearLayout textContainer = new LinearLayout(this);
        textContainer.setOrientation(LinearLayout.VERTICAL);

        TextView title = new TextView(this);
        title.setText(titleText);
        title.setTextSize(16);
        title.setTextColor(navy);
        title.setTypeface(null, Typeface.BOLD);

        TextView description = new TextView(this);
        description.setText(descriptionText);
        description.setTextSize(12);
        description.setTextColor(gray);
        description.setPadding(0, dp(4), 0, 0);

        textContainer.addView(title);
        textContainer.addView(description);

        LinearLayout.LayoutParams textParams =
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                );
        textParams.leftMargin = dp(12);

        item.addView(textContainer, textParams);

        TextView arrow = new TextView(this);
        arrow.setText("›");
        arrow.setTextSize(28);
        arrow.setTextColor(gray);
        arrow.setGravity(Gravity.CENTER);

        item.addView(arrow, new LinearLayout.LayoutParams(
                dp(24),
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        LinearLayout.LayoutParams itemParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
        itemParams.bottomMargin = dp(12);

        root.addView(item, itemParams);
        item.setOnClickListener(listener);
    }

    private void showMessage(String title, String message) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    private GradientDrawable round(int color, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
    }

    private int dp(float value) {
        return (int) (value * getResources()
                .getDisplayMetrics().density + 0.5f);
    }
}