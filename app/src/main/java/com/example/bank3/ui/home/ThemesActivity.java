package com.example.bank3.ui.home;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.example.bank3.R;
import com.example.bank3.ui.theme.ThemeManager;

public class ThemesActivity extends AppCompatActivity
        implements ThemeManager.ThemeChangeListener {

    private int background;
    private boolean dark;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_themes);

        setupEdgeToEdge();

        loadColors();
        updateSystemBars();
        setupViews();
    }

    private void setupEdgeToEdge() {
        View themesScroll = findViewById(R.id.themesScroll);
        if (themesScroll == null) {
            return;
        }

        int initialPaddingLeft = themesScroll.getPaddingLeft();
        int initialPaddingTop = themesScroll.getPaddingTop();
        int initialPaddingRight = themesScroll.getPaddingRight();
        int initialPaddingBottom = themesScroll.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(
                themesScroll,
                (view, windowInsets) -> {
                    Insets bars = windowInsets.getInsets(
                            WindowInsetsCompat.Type.systemBars()
                    );

                    view.setPadding(
                            initialPaddingLeft + bars.left,
                            initialPaddingTop + bars.top,
                            initialPaddingRight + bars.right,
                            initialPaddingBottom + bars.bottom
                    );

                    return windowInsets;
                }
        );
        ViewCompat.requestApplyInsets(themesScroll);
    }

    @Override
    protected void onStart() {
        super.onStart();
        ThemeManager.registerThemeChangeListener(this);
    }

    @Override
    protected void onStop() {
        ThemeManager.unregisterThemeChangeListener(this);
        super.onStop();
    }

    @Override
    public void onThemeChanged() {
        loadColors();
        updateSystemBars();
        setupViews();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadColors();
        updateSystemBars();
        setupViews();
    }

    private void loadColors() {
        dark = ThemeManager.isBlack(this);

        background = dark
                ? Color.rgb(17, 24, 39)
                : Color.rgb(246, 248, 252);
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getWindow().setNavigationBarContrastEnforced(false);
            getWindow().setStatusBarContrastEnforced(false);
        }
    }

    private void setupViews() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Theme options
        String currentTheme = ThemeManager.getTheme(this);

        TextView checkLight = findViewById(R.id.checkThemeLight);
        TextView checkDark = findViewById(R.id.checkThemeDark);

        checkLight.setText(ThemeManager.THEME_WHITE.equals(currentTheme) ? "✓" : "");
        checkDark.setText(ThemeManager.THEME_BLACK.equals(currentTheme) ? "✓" : "");

        findViewById(R.id.itemThemeLight).setOnClickListener(v -> {
            ThemeManager.setTheme(this, ThemeManager.THEME_WHITE);
            recreate();
        });

        findViewById(R.id.itemThemeDark).setOnClickListener(v -> {
            ThemeManager.setTheme(this, ThemeManager.THEME_BLACK);
            recreate();
        });

        // Accent options
        String currentAccent = ThemeManager.getAccent(this);

        setupAccentOption(R.id.swatchBlue, R.id.checkAccentBlue, R.id.itemAccentBlue,
                ThemeManager.ACCENT_BLUE, Color.rgb(52, 120, 246), currentAccent);

        setupAccentOption(R.id.swatchGreen, R.id.checkAccentGreen, R.id.itemAccentGreen,
                ThemeManager.ACCENT_GREEN, Color.rgb(16, 185, 129), currentAccent);

        setupAccentOption(R.id.swatchPurple, R.id.checkAccentPurple, R.id.itemAccentPurple,
                ThemeManager.ACCENT_PURPLE, Color.rgb(139, 92, 246), currentAccent);

        setupAccentOption(R.id.swatchRed, R.id.checkAccentRed, R.id.itemAccentRed,
                ThemeManager.ACCENT_RED, Color.rgb(239, 68, 68), currentAccent);

        setupAccentOption(R.id.swatchOrange, R.id.checkAccentOrange, R.id.itemAccentOrange,
                ThemeManager.ACCENT_ORANGE, Color.rgb(249, 115, 22), currentAccent);
    }

    private void setupAccentOption(
            int swatchId,
            int checkId,
            int itemId,
            String accentName,
            int color,
            String currentAccent
    ) {
        View swatch = findViewById(swatchId);
        swatch.setBackground(round(color, 50));

        TextView check = findViewById(checkId);
        check.setTextColor(color);
        check.setText(currentAccent.equals(accentName) ? "✓" : "");

        findViewById(itemId).setOnClickListener(v -> ThemeManager.setAccent(this, accentName));
    }

    private GradientDrawable round(int color, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}