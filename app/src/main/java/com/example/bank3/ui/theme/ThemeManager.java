package com.example.bank3.ui.theme;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;

import com.example.bank3.R;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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

    public interface ThemeChangeListener {
        void onThemeChanged();
    }

    private static final List<WeakReference<ThemeChangeListener>> listeners =
            new ArrayList<>();

    private ThemeManager() {
    }

    private static SharedPreferences getPreferences(Context context) {
        return context.getApplicationContext().getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
        );
    }

    public static synchronized void registerThemeChangeListener(
            ThemeChangeListener listener
    ) {
        if (listener == null) {
            return;
        }

        Iterator<WeakReference<ThemeChangeListener>> iterator =
                listeners.iterator();

        while (iterator.hasNext()) {
            ThemeChangeListener existing = iterator.next().get();

            if (existing == null) {
                iterator.remove();
            } else if (existing == listener) {
                return;
            }
        }

        listeners.add(new WeakReference<>(listener));
    }

    public static synchronized void unregisterThemeChangeListener(
            ThemeChangeListener listener
    ) {
        Iterator<WeakReference<ThemeChangeListener>> iterator =
                listeners.iterator();

        while (iterator.hasNext()) {
            ThemeChangeListener existing = iterator.next().get();

            if (existing == null || existing == listener) {
                iterator.remove();
            }
        }
    }

    private static void notifyThemeChanged() {
        ThemeChangeListener[] targets;

        synchronized (ThemeManager.class) {
            int size = listeners.size();
            ThemeChangeListener[] temp = new ThemeChangeListener[size];
            int count = 0;

            Iterator<WeakReference<ThemeChangeListener>> iterator =
                    listeners.iterator();

            while (iterator.hasNext()) {
                ThemeChangeListener listener = iterator.next().get();

                if (listener == null) {
                    iterator.remove();
                } else {
                    temp[count++] = listener;
                }
            }

            targets = new ThemeChangeListener[count];
            System.arraycopy(temp, 0, targets, 0, count);
        }

        for (ThemeChangeListener listener : targets) {
            listener.onThemeChanged();
        }
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
        if (!THEME_WHITE.equals(theme)
                && !THEME_BLACK.equals(theme)) {
            return;
        }

        String oldTheme = getTheme(context);

        getPreferences(context).edit()
                .putString(KEY_THEME, theme)
                .apply();

        applyTheme(context);

        if (!oldTheme.equals(theme)) {
            notifyThemeChanged();
        }
    }

    public static void setAccent(Context context, String accent) {
        if (!isValidAccent(accent)) {
            return;
        }

        String oldAccent = getAccent(context);

        getPreferences(context).edit()
                .putString(KEY_ACCENT, accent)
                .apply();

        if (!oldAccent.equals(accent)) {
            notifyThemeChanged();
        }
    }

    public static boolean isBlack(Context context) {
        return THEME_BLACK.equals(getTheme(context));
    }

    public static int getAccentColor(Context context) {
        switch (getAccent(context)) {
            case ACCENT_GREEN:
                return ContextCompat.getColor(context, R.color.accent_green);

            case ACCENT_PURPLE:
                return ContextCompat.getColor(context, R.color.accent_purple);

            case ACCENT_RED:
                return ContextCompat.getColor(context, R.color.accent_red);

            case ACCENT_ORANGE:
                return ContextCompat.getColor(context, R.color.accent_orange);

            case ACCENT_BLUE:
            default:
                return ContextCompat.getColor(context, R.color.accent_blue);
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

        return Color.argb(
                isBlack(context) ? 65 : 35,
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
        SharedPreferences.Editor editor =
                getPreferences(context).edit();

        if (uri == null) {
            editor.remove(KEY_BACKGROUND_IMAGE);
        } else {
            editor.putString(
                    KEY_BACKGROUND_IMAGE,
                    uri.toString()
            );
        }

        editor.apply();
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