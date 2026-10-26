package com.example.bank3.ui.home;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.bank3.R;
import com.example.bank3.ui.theme.ThemeManager;

import java.text.NumberFormat;
import java.util.Locale;

public class CardsActivity extends AppCompatActivity {

    private static final String PREFS_PREFIX = "demo_card_settings_";
    private long userId;
    private SharedPreferences preferences;

    private TextView cardNumber;
    private TextView limitValue;
    private SwitchCompat blockedSwitch;
    private Button revealButton;
    private boolean numberRevealed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_cards);

        setupEdgeToEdge();

        userId = getIntent().getLongExtra("uzytkownikId", -1L);
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

        // Card reveal button logic
        revealButton.setOnClickListener(v -> {
            numberRevealed = !numberRevealed;
            cardNumber.setText(numberRevealed
                    ? "4532  8910  2341  1024"
                    : "••••  ••••  ••••  1024");
            revealButton.setText(numberRevealed ? "Ukryj dane karty" : "Pokaż dane karty");
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

    private void showLimitDialog() {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setSingleLine(true);
        input.setHint("np. 1500");
        input.setText(String.valueOf(preferences.getFloat("daily_limit", 1500f)).replace('.', ','));
        input.setSelection(input.getText().length());

        new AlertDialog.Builder(this)
                .setTitle("Dzienny limit płatności")
                .setMessage("Podaj maksymalną kwotę przelewu w PLN.")
                .setView(input)
                .setNegativeButton("Anuluj", null)
                .setPositiveButton("Zapisz", (dialog, which) -> {
                    try {
                        float limit = Float.parseFloat(input.getText().toString().trim().replace(',', '.'));
                        if (!Float.isFinite(limit) || limit <= 0 || limit > 1000000) {
                            throw new NumberFormatException();
                        }
                        preferences.edit().putFloat("daily_limit", limit).apply();
                        updateLimitText();
                        Toast.makeText(this, "Zapisano dzienny limit płatności.", Toast.LENGTH_SHORT).show();
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Podaj kwotę większą od 0 i nie większą niż 1 000 000 PLN.", Toast.LENGTH_LONG).show();
                    }
                })
                .show();
    }

    private void updateLimitText() {
        if (limitValue == null) {
            return;
        }
        float limit = preferences.getFloat("daily_limit", 1500f);
        NumberFormat format = NumberFormat.getNumberInstance(new Locale("pl", "PL"));
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);
        limitValue.setText(format.format(limit) + " PLN");
    }
}