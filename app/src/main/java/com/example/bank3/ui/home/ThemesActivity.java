package com.example.bank3.ui.home;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowInsetsControllerCompat;

import com.example.bank3.ui.theme.ThemeManager;

public class ThemesActivity extends AppCompatActivity {

    private LinearLayout root;

    private int background;
    private int surface;
    private int textColor;
    private int secondaryText;
    private int accent;
    private boolean dark;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);

        loadColors();
        updateSystemBars();
        createLayout();
        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();

        loadColors();
        updateSystemBars();

        if (root != null) {
            createLayout();
            setContentView(root);
        }
    }

    private void loadColors() {
        dark = ThemeManager.isBlack(this);

        background = dark
                ? Color.rgb(17, 24, 39)
                : Color.rgb(246, 248, 252);

        surface = dark
                ? Color.rgb(30, 41, 59)
                : Color.WHITE;

        textColor = dark
                ? Color.rgb(232, 238, 249)
                : Color.rgb(23, 59, 112);

        secondaryText = dark
                ? Color.rgb(165, 178, 200)
                : Color.rgb(119, 132, 154);

        accent = ThemeManager.getAccentColor(this);
    }

    private void updateSystemBars() {
        getWindow().setStatusBarColor(background);
        getWindow().setNavigationBarColor(background);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getWindow().setNavigationBarDividerColor(background);
        }

        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(
                        getWindow(),
                        getWindow().getDecorView()
                );

        controller.setAppearanceLightStatusBars(!dark);
        controller.setAppearanceLightNavigationBars(!dark);

        // Wyłącz kontrastowe wymuszanie kolorów pasków,
        // aby Android nie zmieniał ich na biały lub szary.
        if (android.os.Build.VERSION.SDK_INT >=
                android.os.Build.VERSION_CODES.Q) {
            getWindow().setNavigationBarContrastEnforced(false);
            getWindow().setStatusBarContrastEnforced(false);
        }
    }

    private void createLayout() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(18), dp(20), dp(24));
        root.setBackgroundColor(background);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView back = new TextView(this);
        back.setText("‹");
        back.setTextSize(36);
        back.setTextColor(textColor);
        back.setGravity(Gravity.CENTER);
        back.setBackground(round(surface, 16));

        header.addView(back, new LinearLayout.LayoutParams(
                dp(48), dp(48)
        ));

        back.setOnClickListener(v -> finish());

        TextView title = new TextView(this);
        title.setText("Themes");
        title.setTextSize(25);
        title.setTextColor(textColor);
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
        subtitle.setText("Wybierz wygląd swojej aplikacji");
        subtitle.setTextSize(14);
        subtitle.setTextColor(secondaryText);
        subtitle.setPadding(0, dp(10), 0, dp(24));

        root.addView(subtitle);

        addSection("MOTYW");

        addThemeOption(
                "Light",
                "Jasne tło i niebieskie akcenty",
                "☀",
                ThemeManager.THEME_WHITE
        );

        addThemeOption(
                "Dark",
                "Ciemne tło i jasny tekst",
                "☾",
                ThemeManager.THEME_BLACK
        );

        addSection("KOLOR AKCENTOWY");

        addAccentOption(
                "Blue", ThemeManager.ACCENT_BLUE,
                Color.rgb(52, 120, 246)
        );

        addAccentOption(
                "Green", ThemeManager.ACCENT_GREEN,
                Color.rgb(16, 185, 129)
        );

        addAccentOption(
                "Purple", ThemeManager.ACCENT_PURPLE,
                Color.rgb(139, 92, 246)
        );

        addAccentOption(
                "Red", ThemeManager.ACCENT_RED,
                Color.rgb(239, 68, 68)
        );

        addAccentOption(
                "Orange", ThemeManager.ACCENT_ORANGE,
                Color.rgb(249, 115, 22)
        );
    }

    private void addSection(String text) {
        TextView section = new TextView(this);
        section.setText(text);
        section.setTextSize(12);
        section.setTypeface(null, Typeface.BOLD);
        section.setTextColor(secondaryText);
        section.setPadding(dp(4), dp(8), 0, dp(12));

        root.addView(section);
    }

    private void addThemeOption(
            String title,
            String description,
            String iconText,
            String theme
    ) {
        LinearLayout item = new LinearLayout(this);
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setPadding(dp(14), dp(12), dp(14), dp(12));
        item.setBackground(round(surface, 18));

        TextView icon = new TextView(this);
        icon.setText(iconText);
        icon.setTextSize(25);
        icon.setTextColor(accent);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(round(background, 14));

        item.addView(icon, new LinearLayout.LayoutParams(
                dp(48), dp(48)
        ));

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);

        TextView name = new TextView(this);
        name.setText(title);
        name.setTextSize(16);
        name.setTextColor(textColor);
        name.setTypeface(null, Typeface.BOLD);

        TextView desc = new TextView(this);
        desc.setText(description);
        desc.setTextSize(12);
        desc.setTextColor(secondaryText);
        desc.setPadding(0, dp(4), 0, 0);

        texts.addView(name);
        texts.addView(desc);

        LinearLayout.LayoutParams textParams =
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                );

        textParams.leftMargin = dp(12);
        item.addView(texts, textParams);

        TextView check = new TextView(this);
        check.setText(
                ThemeManager.getTheme(this).equals(theme)
                        ? "✓" : ""
        );
        check.setTextSize(20);
        check.setTextColor(accent);

        item.addView(check);

        addItem(item);

        item.setOnClickListener(v -> {
            ThemeManager.setTheme(this, theme);

            Toast.makeText(
                    this,
                    "Wybrano motyw " + title,
                    Toast.LENGTH_SHORT
            ).show();

            recreate();
        });
    }

    private void addAccentOption(
            String title,
            String accentName,
            int color
    ) {
        LinearLayout item = new LinearLayout(this);
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setPadding(dp(14), dp(12), dp(14), dp(12));
        item.setBackground(round(surface, 16));

        View swatch = new View(this);
        swatch.setBackground(round(color, 50));

        item.addView(swatch, new LinearLayout.LayoutParams(
                dp(28), dp(28)
        ));

        TextView name = new TextView(this);
        name.setText(title);
        name.setTextSize(15);
        name.setTextColor(textColor);

        LinearLayout.LayoutParams nameParams =
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                );

        nameParams.leftMargin = dp(14);
        item.addView(name, nameParams);

        TextView check = new TextView(this);
        check.setText(
                ThemeManager.getAccent(this).equals(accentName)
                        ? "✓" : ""
        );
        check.setTextSize(20);
        check.setTextColor(color);

        item.addView(check);

        addItem(item);

        item.setOnClickListener(v -> {
            ThemeManager.setAccent(this, accentName);
            loadColors();
            updateSystemBars();
            createLayout();
            setContentView(root);
        });
    }

    private void addItem(View item) {
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        params.bottomMargin = dp(10);
        root.addView(item, params);
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