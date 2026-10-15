package com.example.bank3.ui.theme;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

public final class ThemeManager {

    private static final String PREFS_NAME = "theme_preferences";
    private static final String KEY_THEME = "selected_theme";
    private static final String KEY_ACCENT = "selected_accent";

    public static final String THEME_WHITE = "white";
    public static final String THEME_BLACK = "black";

    public static final String ACCENT_BLUE = "blue";
    public static final String ACCENT_GREEN = "green";
    public static final String ACCENT_PURPLE = "purple";
    public static final String ACCENT_RED = "red";
    public static final String ACCENT_ORANGE = "orange";

    private ThemeManager() {
        // Klasa zawiera wyłącznie metody statyczne.
    }

    private static SharedPreferences getPreferences(Context context) {
        return context.getApplicationContext().getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
        );
    }

    public static String getTheme(Context context) {
        return getPreferences(context).getString(
                KEY_THEME,
                THEME_WHITE
        );
    }

    public static String getAccent(Context context) {
        return getPreferences(context).getString(
                KEY_ACCENT,
                ACCENT_BLUE
        );
    }

    public static void setTheme(Context context, String theme) {
        if (!THEME_WHITE.equals(theme) && !THEME_BLACK.equals(theme)) {
            return;
        }

        getPreferences(context)
                .edit()
                .putString(KEY_THEME, theme)
                .apply();

        applyTheme(context);
    }

    public static void setAccent(Context context, String accent) {
        if (!isValidAccent(accent)) {
            return;
        }

        getPreferences(context)
                .edit()
                .putString(KEY_ACCENT, accent)
                .apply();
    }

    public static boolean isBlack(Context context) {
        return THEME_BLACK.equals(getTheme(context));
    }

    public static int getAccentColor(Context context) {
        switch (getAccent(context)) {
            case ACCENT_GREEN:
                return android.graphics.Color.rgb(16, 185, 129);
            case ACCENT_PURPLE:
                return android.graphics.Color.rgb(139, 92, 246);
            case ACCENT_RED:
                return android.graphics.Color.rgb(239, 68, 68);
            case ACCENT_ORANGE:
                return android.graphics.Color.rgb(249, 115, 22);
            case ACCENT_BLUE:
            default:
                return android.graphics.Color.rgb(52, 120, 246);
        }
    }

    private static boolean isValidAccent(String accent) {
        return ACCENT_BLUE.equals(accent)
                || ACCENT_GREEN.equals(accent)
                || ACCENT_PURPLE.equals(accent)
                || ACCENT_RED.equals(accent)
                || ACCENT_ORANGE.equals(accent);
    }

    public static void applyTheme(Context context) {
        if (isBlack(context)) {
            AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_YES
            );
        } else {
            AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_NO
            );
        }
    }
}