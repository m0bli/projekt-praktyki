package com.example.bank3.ui.theme;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;

import androidx.appcompat.app.AppCompatDelegate;

public final class ThemeManager {

    private static final String PREFS_NAME = "theme_preferences";
    private static final String KEY_THEME = "selected_theme";
    private static final String KEY_ACCENT = "selected_accent";
    private static final String KEY_BACKGROUND_IMAGE = "background_image_uri";

    public static final String THEME_WHITE = "white";
    public static final String THEME_BLACK = "black";

    public static final String ACCENT_BLUE = "blue";
    public static final String ACCENT_GREEN = "green";
    public static final String ACCENT_PURPLE = "purple";
    public static final String ACCENT_RED = "red";
    public static final String ACCENT_ORANGE = "orange";

    private ThemeManager() {
    }

    private static SharedPreferences getPreferences(Context context) {
        return context.getApplicationContext().getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
        );
    }

    public static String getTheme(Context context) {
        return getPreferences(context).getString(KEY_THEME, THEME_WHITE);
    }

    public static String getAccent(Context context) {
        return getPreferences(context).getString(KEY_ACCENT, ACCENT_BLUE);
    }

    public static void setTheme(Context context, String theme) {
        if (!THEME_WHITE.equals(theme) && !THEME_BLACK.equals(theme)) {
            return;
        }

        getPreferences(context).edit()
                .putString(KEY_THEME, theme)
                .apply();

        applyTheme(context);
    }

    public static void setAccent(Context context, String accent) {
        if (!isValidAccent(accent)) {
            return;
        }

        getPreferences(context).edit()
                .putString(KEY_ACCENT, accent)
                .apply();
    }

    public static boolean isBlack(Context context) {
        return THEME_BLACK.equals(getTheme(context));
    }

    public static int getAccentColor(Context context) {
        switch (getAccent(context)) {
            case ACCENT_GREEN:
                return Color.rgb(16, 185, 129);
            case ACCENT_PURPLE:
                return Color.rgb(139, 92, 246);
            case ACCENT_RED:
                return Color.rgb(239, 68, 68);
            case ACCENT_ORANGE:
                return Color.rgb(249, 115, 22);
            case ACCENT_BLUE:
            default:
                return Color.rgb(52, 120, 246);
        }
    }

    public static int getBackgroundColor(Context context) {
        return isBlack(context)
                ? Color.rgb(18, 24, 35)
                : Color.rgb(244, 246, 250);
    }

    public static int getSurfaceColor(Context context) {
        return isBlack(context)
                ? Color.rgb(30, 38, 52)
                : Color.WHITE;
    }

    public static int getTextColor(Context context) {
        return isBlack(context)
                ? Color.rgb(240, 244, 250)
                : Color.rgb(24, 36, 58);
    }

    public static int getSecondaryTextColor(Context context) {
        return isBlack(context)
                ? Color.rgb(177, 188, 205)
                : Color.rgb(119, 132, 154);
    }

    public static int getAccentSoftColor(Context context) {
        int accent = getAccentColor(context);

        if (isBlack(context)) {
            return Color.argb(
                    65,
                    Color.red(accent),
                    Color.green(accent),
                    Color.blue(accent)
            );
        }

        return Color.argb(
                35,
                Color.red(accent),
                Color.green(accent),
                Color.blue(accent)
        );
    }

    private static boolean isValidAccent(String accent) {
        return ACCENT_BLUE.equals(accent)
                || ACCENT_GREEN.equals(accent)
                || ACCENT_PURPLE.equals(accent)
                || ACCENT_RED.equals(accent)
                || ACCENT_ORANGE.equals(accent);
    }

    public static void setBackgroundImage(Context context, Uri uri) {
        getPreferences(context).edit()
                .putString(
                        KEY_BACKGROUND_IMAGE,
                        uri == null ? null : uri.toString()
                )
                .apply();
    }

    public static Uri getBackgroundImage(Context context) {
        String value = getPreferences(context).getString(
                KEY_BACKGROUND_IMAGE,
                null
        );

        if (value == null || value.isEmpty()) {
            return null;
        }

        try {
            return Uri.parse(value);
        } catch (Exception e) {
            return null;
        }
    }

    public static void clearBackgroundImage(Context context) {
        getPreferences(context).edit()
                .remove(KEY_BACKGROUND_IMAGE)
                .apply();
    }

    public static void applyTheme(Context context) {
        AppCompatDelegate.setDefaultNightMode(
                isBlack(context)
                        ? AppCompatDelegate.MODE_NIGHT_YES
                        : AppCompatDelegate.MODE_NIGHT_NO
        );
    }
}