package com.example.bank3.ui.home;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.bank3.R;
import com.example.bank3.data.api.AuthAPI;
import com.example.bank3.model.AuthResponse;
import com.example.bank3.model.LoginRequest;
import com.example.bank3.network.RetrofitClient;
import com.example.bank3.ui.theme.ThemeManager;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.NumberFormat;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CardsActivity extends AppCompatActivity {

    private static final String PREFS_PREFIX = "demo_card_settings_";
    private long userId;
    private String userNick = "";
    private SharedPreferences preferences;

    private TextView cardNumber;
    private TextView limitValue;
    private SwitchCompat blockedSwitch;
    private Button revealButton;
    private boolean numberRevealed;
    private boolean isCardVerifiedSession = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_cards);

        setupEdgeToEdge();

        userId = getIntent().getLongExtra("uzytkownikId", -1L);
        userNick = getIntent().getStringExtra("nick");
        if (userNick == null) {
            userNick = "";
        }
        preferences = getSharedPreferences(PREFS_PREFIX + userId, MODE_PRIVATE);

        setupViews();
    }

    private void setupEdgeToEdge() {
        View cardsScrollView = findViewById(R.id.cardsScrollView);
        if (cardsScrollView == null) {
            return;
        }

        int initialPaddingLeft = cardsScrollView.getPaddingLeft();
        int initialPaddingTop = cardsScrollView.getPaddingTop();
        int initialPaddingRight = cardsScrollView.getPaddingRight();
        int initialPaddingBottom = cardsScrollView.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(
                cardsScrollView,
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
        ViewCompat.requestApplyInsets(cardsScrollView);
    }

    private void setupViews() {
        findViewById(R.id.btnBackCards).setOnClickListener(v -> finish());

        cardNumber = findViewById(R.id.txtCardNumber);
        revealButton = findViewById(R.id.btnRevealCard);
        blockedSwitch = findViewById(R.id.switchBlocked);
        limitValue = findViewById(R.id.txtLimitValue);

        // Card reveal button logic with password verification (once per screen session)
        revealButton.setOnClickListener(v -> {
            if (numberRevealed) {
                toggleCardReveal(false);
            } else {
                if (isCardVerifiedSession) {
                    toggleCardReveal(true);
                } else {
                    showPasswordDialogForCard(() -> toggleCardReveal(true));
                }
            }
        });

        // Blocked switch logic
        boolean isBlocked = preferences.getBoolean("blocked", false);
        blockedSwitch.setChecked(isBlocked);

        blockedSwitch.setOnCheckedChangeListener((button, checked) -> {
            preferences.edit().putBoolean("blocked", checked).apply();
            Toast.makeText(
                    this,
                    checked ? "Konto zostało zablokowane. Przelewy są teraz wyłączone."
                            : "Konto zostało odblokowane.",
                    Toast.LENGTH_SHORT
            ).show();
        });

        // Limit change logic
        findViewById(R.id.btnChangeLimit).setOnClickListener(v -> showLimitDialog());
        updateLimitText();
    }

    private void toggleCardReveal(boolean reveal) {
        numberRevealed = reveal;
        cardNumber.setText(numberRevealed
                ? "4532  8910  2341  1024"
                : "••••  ••••  ••••  1024");
        revealButton.setText(numberRevealed ? "Ukryj dane karty" : "Pokaż dane karty");
    }

    private void showPasswordDialogForCard(Runnable onVerified) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_verify_password, null);

        int surface = ThemeManager.getSurfaceColor(this);
        int text = ThemeManager.getTextColor(this);
        int secondary = ThemeManager.getSecondaryTextColor(this);
        int accent = ThemeManager.getAccentColor(this);
        boolean dark = ThemeManager.isBlack(this);

        View dialogRoot = dialogView.findViewById(R.id.dialogVerifyRoot);
        if (dialogRoot != null) {
            dialogRoot.setBackground(round(surface, 24));
        }

        ImageView iconView = dialogView.findViewById(R.id.dialogVerifyIcon);
        if (iconView != null) {
            int iconSurface = dark ? Color.rgb(44, 57, 78) : Color.rgb(232, 240, 255);
            iconView.setBackground(round(iconSurface, 18));
            iconView.setColorFilter(accent);
        }

        TextView titleView = dialogView.findViewById(R.id.dialogVerifyTitle);
        if (titleView != null) {
            titleView.setText("Potwierdź tożsamość");
            titleView.setTextColor(text);
        }

        TextView subtitleView = dialogView.findViewById(R.id.dialogVerifySubtitle);
        if (subtitleView != null) {
            subtitleView.setText("Podaj swoje hasło do konta, aby odsłonić dane karty bankowej.");
            subtitleView.setTextColor(secondary);
        }

        final TextInputLayout inputLayout = dialogView.findViewById(R.id.dialogVerifyPasswordLayout);
        final TextInputEditText editPassword = dialogView.findViewById(R.id.editDialogVerifyPassword);

        TextView txtError = dialogView.findViewById(R.id.txtDialogVerifyError);
        TextView btnCancel = dialogView.findViewById(R.id.btnDialogVerifyCancel);
        TextView btnConfirm = dialogView.findViewById(R.id.btnDialogVerifyConfirm);

        if (btnCancel != null) {
            btnCancel.setBackground(round(dark ? Color.rgb(44, 57, 78) : Color.rgb(232, 240, 255), 14));
            btnCancel.setTextColor(secondary);
        }

        if (btnConfirm != null) {
            btnConfirm.setBackground(round(accent, 14));
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialog.show();

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> {
                String pass = (editPassword != null && editPassword.getText() != null)
                        ? editPassword.getText().toString().trim() : "";

                if (pass.isEmpty()) {
                    if (inputLayout != null) inputLayout.setError("Wprowadź hasło");
                    return;
                }
                if (inputLayout != null) inputLayout.setError(null);
                if (txtError != null) txtError.setVisibility(View.GONE);

                String loginToVerify = userNick;
                if (loginToVerify.isEmpty()) {
                    loginToVerify = "user";
                }

                AuthAPI authAPI = RetrofitClient.getInstance().create(AuthAPI.class);
                authAPI.login(new LoginRequest(loginToVerify, pass)).enqueue(new Callback<AuthResponse>() {
                    @Override
                    public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                        if (isFinishing() || isDestroyed()) return;

                        AuthResponse body = response.body();
                        if (response.isSuccessful() && body != null && (body.isSukces() || body.getUzytkownikId() != null)) {
                            isCardVerifiedSession = true;
                            if (onVerified != null) {
                                onVerified.run();
                            }
                            dialog.dismiss();
                        } else {
                            if (inputLayout != null) inputLayout.setError("Nieprawidłowe hasło");
                            if (txtError != null) {
                                txtError.setText("Nieprawidłowe hasło do konta");
                                txtError.setVisibility(View.VISIBLE);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<AuthResponse> call, Throwable t) {
                        if (isFinishing() || isDestroyed()) return;
                        Toast.makeText(CardsActivity.this, "Błąd połączenia z bazą danych.", Toast.LENGTH_SHORT).show();
                    }
                });
            });
        }
    }

    private void showLimitDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_change_limit, null);

        int surface = ThemeManager.getSurfaceColor(this);
        int text = ThemeManager.getTextColor(this);
        int secondary = ThemeManager.getSecondaryTextColor(this);
        int accent = ThemeManager.getAccentColor(this);
        boolean dark = ThemeManager.isBlack(this);

        View dialogRoot = dialogView.findViewById(R.id.dialogLimitRoot);
        if (dialogRoot != null) {
            dialogRoot.setBackground(round(surface, 24));
        }

        ImageView iconView = dialogView.findViewById(R.id.dialogLimitIcon);
        if (iconView != null) {
            int iconSurface = dark ? Color.rgb(44, 57, 78) : Color.rgb(232, 240, 255);
            iconView.setBackground(round(iconSurface, 18));
            iconView.setColorFilter(accent);
        }

        TextView titleView = dialogView.findViewById(R.id.dialogLimitTitle);
        if (titleView != null) titleView.setTextColor(text);

        TextView subtitleView = dialogView.findViewById(R.id.dialogLimitSubtitle);
        if (subtitleView != null) subtitleView.setTextColor(secondary);

        final TextInputLayout inputLayout = dialogView.findViewById(R.id.layoutLimitInput);
        final TextInputEditText editLimit = dialogView.findViewById(R.id.editLimitValue);

        TextView txtError = dialogView.findViewById(R.id.txtDialogLimitError);
        TextView btnCancel = dialogView.findViewById(R.id.btnDialogLimitCancel);
        TextView btnConfirm = dialogView.findViewById(R.id.btnDialogLimitConfirm);

        if (editLimit != null) {
            float currentLimit = preferences.getFloat("daily_limit", 1500f);
            editLimit.setText(String.valueOf(currentLimit).replace('.', ','));
            if (editLimit.getText() != null) {
                editLimit.setSelection(editLimit.getText().length());
            }
        }

        if (btnCancel != null) {
            btnCancel.setBackground(round(dark ? Color.rgb(44, 57, 78) : Color.rgb(232, 240, 255), 14));
            btnCancel.setTextColor(secondary);
        }

        if (btnConfirm != null) {
            btnConfirm.setBackground(round(accent, 14));
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialog.show();

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> {
                if (editLimit == null) return;
                String valStr = editLimit.getText() != null ? editLimit.getText().toString().trim().replace(',', '.') : "";
                try {
                    float limit = Float.parseFloat(valStr);
                    if (!Float.isFinite(limit) || limit <= 0 || limit > 1000000) {
                        throw new NumberFormatException();
                    }
                    preferences.edit().putFloat("daily_limit", limit).apply();
                    updateLimitText();
                    dialog.dismiss();
                } catch (NumberFormatException e) {
                    if (inputLayout != null) {
                        inputLayout.setError("Podaj kwotę > 0 i <= 1 000 000 PLN");
                    }
                    if (txtError != null) {
                        txtError.setText("Podaj kwotę większą od 0 i nie większą niż 1 000 000 PLN.");
                        txtError.setVisibility(View.VISIBLE);
                    }
                }
            });
        }
    }

    private void updateLimitText() {
        if (limitValue == null) {
            return;
        }
        float limit = preferences.getFloat("daily_limit", 1500f);
        NumberFormat format = NumberFormat.getNumberInstance(Locale.forLanguageTag("pl-PL"));
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);
        limitValue.setText(format.format(limit) + " PLN");
    }

    private GradientDrawable round(int color, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius((int) (radiusDp * getResources().getDisplayMetrics().density + 0.5f));
        return drawable;
    }
}
