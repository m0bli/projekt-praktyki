package com.example.bank3.ui.home;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
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

        findViewById(R.id.settingProfile).setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, ProfileActivity.class);
            intent.putExtra("uzytkownikId", getIntent().getLongExtra("uzytkownikId", -1L));
            intent.putExtra("nick", getIntent().getStringExtra("nick"));
            startActivity(intent);
        });

        SwitchCompat switchNotifications = findViewById(R.id.switchNotifications);
        SharedPreferences prefs = getSharedPreferences("notifications_prefs", MODE_PRIVATE);
        boolean isPushEnabled = prefs.getBoolean("push_notifications_enabled", true);

        if (switchNotifications != null) {
            switchNotifications.setChecked(isPushEnabled);
        }
        updateNotificationsDescription(isPushEnabled);

        findViewById(R.id.settingNotifications).setOnClickListener(v -> {
            boolean current = prefs.getBoolean("push_notifications_enabled", true);
            boolean newStatus = !current;

            prefs.edit().putBoolean("push_notifications_enabled", newStatus).apply();
            if (switchNotifications != null) {
                switchNotifications.setChecked(newStatus);
            }
            updateNotificationsDescription(newStatus);

            Toast.makeText(
                    this,
                    newStatus ? "Powiadomienia push zostały włączone" : "Powiadomienia push zostały wyłączone",
                    Toast.LENGTH_SHORT
            ).show();
        });

        View settingLogout = findViewById(R.id.settingLogout);
        if (settingLogout != null) {
            settingLogout.setOnClickListener(v -> showLogoutConfirmationDialog());
        }

        applyThemeColors();
    }

    private void showLogoutConfirmationDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Wyloguj się")
                .setMessage("Czy na pewno chcesz się wylogować?")
                .setNegativeButton("Anuluj", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("Wyloguj", (dialog, which) -> {
                    dialog.dismiss();
                    Intent intent = new Intent(SettingsActivity.this, com.example.bank3.ui.auth.MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .show();
    }

    private void updateNotificationsDescription(boolean enabled) {
        TextView description = findViewById(R.id.descriptionNotifications);
        if (description != null) {
            description.setText(enabled ? "Powiadomienia push są włączone" : "Powiadomienia push są wyłączone");
        }
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
                R.id.settingNotifications,
                R.id.settingLogout
        };

        for (int id : surfaceIds) {
            View view = findViewById(id);
            if (view != null) {
                view.setBackground(round(
                        surface,
                        id == R.id.btnBackSettings ? 16 : 18
                ));
            }
        }

        int[] iconIds = {
                R.id.iconThemes,
                R.id.iconProfile,
                R.id.iconNotifications
        };

        for (int id : iconIds) {
            TextView icon = findViewById(id);
            if (icon != null) {
                icon.setBackground(round(iconSurface, 14));
                icon.setTextColor(accent);
            }
        }

        TextView iconLogout = findViewById(R.id.iconLogout);
        if (iconLogout != null) {
            iconLogout.setBackground(round(Color.parseColor("#15EF4444"), 14));
        }

        int[] primaryTextIds = {
                R.id.txtSettingsTitle,
                R.id.titleThemes,
                R.id.titleProfile,
                R.id.titleNotifications
        };

        for (int id : primaryTextIds) {
            TextView tv = findViewById(id);
            if (tv != null) tv.setTextColor(text);
        }

        int[] secondaryTextIds = {
                R.id.txtSettingsSubtitle,
                R.id.sectionAppearance,
                R.id.sectionAccount,
                R.id.sectionApp,
                R.id.descriptionThemes,
                R.id.descriptionProfile,
                R.id.descriptionNotifications,
                R.id.descriptionLogout,
                R.id.txtSettingsVersion
        };

        for (int id : secondaryTextIds) {
            TextView tv = findViewById(id);
            if (tv != null) tv.setTextColor(secondary);
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
