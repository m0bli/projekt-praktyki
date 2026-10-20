package com.example.bank3.ui.home;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.example.bank3.R;
import com.example.bank3.ui.theme.ThemeManager;

public class SettingsActivity extends AppCompatActivity
        implements ThemeManager.ThemeChangeListener {

    private ScrollView settingsScroll;

    private int initialPaddingLeft;
    private int initialPaddingTop;
    private int initialPaddingRight;
    private int initialPaddingBottom;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);

        settingsScroll = findViewById(R.id.settingsScroll);

        initialPaddingLeft = settingsScroll.getPaddingLeft();
        initialPaddingTop = settingsScroll.getPaddingTop();
        initialPaddingRight = settingsScroll.getPaddingRight();
        initialPaddingBottom = settingsScroll.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(
                settingsScroll,
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
        ViewCompat.requestApplyInsets(settingsScroll);

        findViewById(R.id.btnBackSettings).setOnClickListener(v -> finish());

        findViewById(R.id.settingThemes).setOnClickListener(v ->
                startActivity(new Intent(
                        SettingsActivity.this,
                        ThemesActivity.class
                ))
        );

        findViewById(R.id.settingProfile).setOnClickListener(v ->
                showMessage(
                        "Moje dane",
                        "Ta funkcja zostanie dodana później."
                )
        );

        findViewById(R.id.settingSecurity).setOnClickListener(v ->
                showMessage(
                        "Bezpieczeństwo",
                        "Ta funkcja zostanie dodana później."
                )
        );

        findViewById(R.id.settingNotifications).setOnClickListener(v ->
                showMessage(
                        "Powiadomienia",
                        "Ta funkcja zostanie dodana później."
                )
        );

        applyThemeColors();
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
    protected void onResume() {
        super.onResume();
        applyThemeColors();
    }

    @Override
    public void onThemeChanged() {
        applyThemeColors();
    }

    private void applyThemeColors() {
        if (settingsScroll == null) {
            return;
        }

        boolean dark = ThemeManager.isBlack(this);
        int background = ThemeManager.getBackgroundColor(this);
        int surface = ThemeManager.getSurfaceColor(this);
        int text = ThemeManager.getTextColor(this);
        int secondary = ThemeManager.getSecondaryTextColor(this);
        int accent = ThemeManager.getAccentColor(this);
        int iconSurface = dark
                ? Color.rgb(44, 57, 78)
                : Color.rgb(232, 240, 255);

        settingsScroll.setBackgroundColor(background);
        findViewById(R.id.settingsRoot).setBackgroundColor(background);

        int[] surfaceIds = {
                R.id.btnBackSettings,
                R.id.settingThemes,
                R.id.settingProfile,
                R.id.settingSecurity,
                R.id.settingNotifications
        };

        for (int id : surfaceIds) {
            View view = findViewById(id);
            view.setBackground(round(
                    surface,
                    id == R.id.btnBackSettings ? 16 : 18
            ));
        }

        int[] iconIds = {
                R.id.iconThemes,
                R.id.iconProfile,
                R.id.iconSecurity,
                R.id.iconNotifications
        };

        for (int id : iconIds) {
            TextView icon = findViewById(id);
            icon.setBackground(round(iconSurface, 14));
            icon.setTextColor(accent);
        }

        int[] primaryTextIds = {
                R.id.txtSettingsTitle,
                R.id.titleThemes,
                R.id.titleProfile,
                R.id.titleSecurity,
                R.id.titleNotifications
        };

        for (int id : primaryTextIds) {
            ((TextView) findViewById(id)).setTextColor(text);
        }

        int[] secondaryTextIds = {
                R.id.txtSettingsSubtitle,
                R.id.sectionAppearance,
                R.id.sectionAccount,
                R.id.sectionApp,
                R.id.descriptionThemes,
                R.id.descriptionProfile,
                R.id.descriptionSecurity,
                R.id.descriptionNotifications,
                R.id.txtSettingsVersion
        };

        for (int id : secondaryTextIds) {
            ((TextView) findViewById(id)).setTextColor(secondary);
        }

        setSystemBarAppearance(dark);
    }

    private void setSystemBarAppearance(boolean dark) {
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getWindow().setStatusBarContrastEnforced(false);
            getWindow().setNavigationBarContrastEnforced(false);
        }

        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(
                        getWindow(),
                        getWindow().getDecorView()
                );

        controller.setAppearanceLightStatusBars(!dark);
        controller.setAppearanceLightNavigationBars(!dark);
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

    private void showMessage(String title, String message) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }
}
