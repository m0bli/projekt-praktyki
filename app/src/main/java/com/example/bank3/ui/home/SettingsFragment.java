package com.example.bank3.ui.home;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.example.bank3.R;
import com.example.bank3.ui.auth.MainActivity;
import com.example.bank3.ui.theme.ThemeManager;

public class SettingsFragment extends Fragment implements ThemeManager.ThemeChangeListener {

    private ScrollView settingsScroll;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.activity_settings, container, false);

        settingsScroll = view.findViewById(R.id.settingsScroll);

        View btnBack = view.findViewById(R.id.btnBackSettings);
        if (btnBack != null) {
            btnBack.setVisibility(View.GONE);
        }

        long uzytkownikId = -1L;
        String nick = "";
        if (getActivity() != null && getActivity().getIntent() != null) {
            uzytkownikId = getActivity().getIntent().getLongExtra("uzytkownikId", -1L);
            nick = getActivity().getIntent().getStringExtra("nick");
        }

        final long finalUserId = uzytkownikId;
        final String finalNick = nick;

        view.findViewById(R.id.settingThemes).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ThemesActivity.class))
        );

        view.findViewById(R.id.settingProfile).setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), ProfileActivity.class);
            intent.putExtra("uzytkownikId", finalUserId);
            intent.putExtra("nick", finalNick);
            startActivity(intent);
        });

        SwitchCompat switchNotifications = view.findViewById(R.id.switchNotifications);
        SharedPreferences prefs = requireContext().getSharedPreferences("notifications_prefs", Context.MODE_PRIVATE);
        boolean isPushEnabled = prefs.getBoolean("push_notifications_enabled", true);

        if (switchNotifications != null) {
            switchNotifications.setChecked(isPushEnabled);
        }
        updateNotificationsDescription(view, isPushEnabled);

        view.findViewById(R.id.settingNotifications).setOnClickListener(v -> {
            boolean current = prefs.getBoolean("push_notifications_enabled", true);
            boolean newStatus = !current;

            prefs.edit().putBoolean("push_notifications_enabled", newStatus).apply();
            if (switchNotifications != null) {
                switchNotifications.setChecked(newStatus);
            }
            updateNotificationsDescription(view, newStatus);

            Toast.makeText(
                    requireContext(),
                    newStatus ? "Powiadomienia push zostały włączone" : "Powiadomienia push zostały wyłączone",
                    Toast.LENGTH_SHORT
            ).show();
        });

        View settingLogout = view.findViewById(R.id.settingLogout);
        if (settingLogout != null) {
            settingLogout.setOnClickListener(v -> showLogoutConfirmationDialog());
        }

        applyThemeColors(view);
        return view;
    }

    private void updateNotificationsDescription(View root, boolean enabled) {
        TextView description = root.findViewById(R.id.descriptionNotifications);
        if (description != null) {
            description.setText(enabled ? "Powiadomienia push są włączone" : "Powiadomienia push są wyłączone");
        }
    }

    private void showLogoutConfirmationDialog() {
        if (getContext() == null) return;
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_logout, null);

        int surface = ThemeManager.getSurfaceColor(requireContext());
        int text = ThemeManager.getTextColor(requireContext());
        int secondary = ThemeManager.getSecondaryTextColor(requireContext());
        boolean dark = ThemeManager.isBlack(requireContext());

        View dialogRoot = dialogView.findViewById(R.id.dialogLogoutRoot);
        if (dialogRoot != null) {
            dialogRoot.setBackground(round(surface, 24));
        }

        TextView iconView = dialogView.findViewById(R.id.dialogLogoutIcon);
        if (iconView != null) {
            iconView.setBackground(round(Color.parseColor("#15EF4444"), 18));
            iconView.setTextColor(Color.parseColor("#EF4444"));
        }

        TextView titleView = dialogView.findViewById(R.id.dialogLogoutTitle);
        if (titleView != null) titleView.setTextColor(text);


        TextView btnCancel = dialogView.findViewById(R.id.btnDialogLogoutCancel);
        TextView btnConfirm = dialogView.findViewById(R.id.btnDialogLogoutConfirm);

        if (btnCancel != null) {
            btnCancel.setBackground(round(dark ? Color.rgb(44, 57, 78) : Color.rgb(232, 240, 255), 14));
            btnCancel.setTextColor(secondary);
        }

        if (btnConfirm != null) {
            btnConfirm.setBackground(round(Color.parseColor("#EF4444"), 14));
            btnConfirm.setTextColor(Color.WHITE);
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> {
                dialog.dismiss();
                Intent intent = new Intent(requireContext(), MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                if (getActivity() != null) {
                    getActivity().finish();
                }
            });
        }

        dialog.show();
    }

    @Override
    public void onStart() {
        super.onStart();
        ThemeManager.registerThemeChangeListener(this);
    }

    @Override
    public void onStop() {
        ThemeManager.unregisterThemeChangeListener(this);
        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getView() != null) {
            applyThemeColors(getView());
        }
    }

    @Override
    public void onThemeChanged() {
        if (getView() != null) {
            applyThemeColors(getView());
        }
    }

    private void applyThemeColors(View root) {
        if (root == null || getContext() == null) return;

        boolean dark = ThemeManager.isBlack(requireContext());
        int background = ThemeManager.getBackgroundColor(requireContext());
        int surface = ThemeManager.getSurfaceColor(requireContext());
        int text = ThemeManager.getTextColor(requireContext());
        int secondary = ThemeManager.getSecondaryTextColor(requireContext());
        int accent = ThemeManager.getAccentColor(requireContext());
        int iconSurface = dark ? Color.rgb(44, 57, 78) : Color.rgb(232, 240, 255);

        if (settingsScroll != null) {
            settingsScroll.setBackgroundColor(background);
        }

        View settingsRoot = root.findViewById(R.id.settingsRoot);
        if (settingsRoot != null) {
            settingsRoot.setBackgroundColor(background);
        }

        int[] surfaceIds = {
                R.id.btnBackSettings,
                R.id.settingThemes,
                R.id.settingProfile,
                R.id.settingNotifications,
                R.id.settingLogout
        };

        for (int id : surfaceIds) {
            View item = root.findViewById(id);
            if (item != null) {
                item.setBackground(round(surface, id == R.id.btnBackSettings ? 16 : 18));
            }
        }

        int[] iconIds = {
                R.id.iconThemes,
                R.id.iconProfile,
                R.id.iconNotifications
        };

        for (int id : iconIds) {
            View icon = root.findViewById(id);
            if (icon != null) {
                icon.setBackground(round(iconSurface, 14));
                if (icon instanceof TextView) {
                    ((TextView) icon).setTextColor(accent);
                } else if (icon instanceof android.widget.ImageView) {
                    ((android.widget.ImageView) icon).setColorFilter(accent);
                }
            }
        }

        TextView iconLogout = root.findViewById(R.id.iconLogout);
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
            TextView tv = root.findViewById(id);
            if (tv != null) tv.setTextColor(text);
        }

        int[] secondaryTextIds = {
                R.id.sectionAppearance,
                R.id.sectionAccount,
                R.id.sectionApp,
                R.id.descriptionThemes,
                R.id.descriptionProfile,
                R.id.descriptionNotifications
        };

        for (int id : secondaryTextIds) {
            TextView tv = root.findViewById(id);
            if (tv != null) tv.setTextColor(secondary);
        }
    }

    private GradientDrawable round(int color, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
    }

    private int dp(float value) {
        if (getContext() == null) return (int) value;
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
