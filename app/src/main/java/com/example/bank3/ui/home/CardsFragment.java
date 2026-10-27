package com.example.bank3.ui.home;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.example.bank3.R;

import java.text.NumberFormat;
import java.util.Locale;

public class CardsFragment extends Fragment {

    private static final String PREFS_PREFIX = "demo_card_settings_";
    private SharedPreferences preferences;

    private TextView cardNumber;
    private TextView limitValue;
    private Button revealButton;
    private boolean numberRevealed;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.activity_cards, container, false);

        long userId = requireActivity().getIntent().getLongExtra("uzytkownikId", -1L);
        preferences = requireContext().getSharedPreferences(PREFS_PREFIX + userId, Context.MODE_PRIVATE);

        setupViews(root);
        return root;
    }

    private void setupViews(View root) {
        root.findViewById(R.id.btnBackCards).setOnClickListener(v -> ((MainMenuActivity) requireActivity()).selectTab(0));

        cardNumber = root.findViewById(R.id.txtCardNumber);
        revealButton = root.findViewById(R.id.btnRevealCard);
        SwitchCompat blockedSwitch = root.findViewById(R.id.switchBlocked);
        limitValue = root.findViewById(R.id.txtLimitValue);

        revealButton.setOnClickListener(v -> {
            numberRevealed = !numberRevealed;
            cardNumber.setText(numberRevealed
                    ? "4532  8910  2341  1024"
                    : "••••  ••••  ••••  1024");
            revealButton.setText(numberRevealed ? "Ukryj dane karty" : "Pokaż dane karty");
        });

        boolean isBlocked = preferences.getBoolean("blocked", false);
        if (blockedSwitch != null) {
            blockedSwitch.setChecked(isBlocked);

            blockedSwitch.setOnCheckedChangeListener((button, checked) -> {
                preferences.edit().putBoolean("blocked", checked).apply();
                Toast.makeText(
                        requireContext(),
                        checked ? "Konto zostało zablokowane. Przelewy są teraz wyłączone."
                                : "Konto zostało odblokowane.",
                        Toast.LENGTH_SHORT
                ).show();
            });
        }

        root.findViewById(R.id.btnChangeLimit).setOnClickListener(v -> showLimitDialog());
        updateLimitText();
    }

    private void showLimitDialog() {
        EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setSingleLine(true);
        input.setHint("np. 1500");
        input.setText(String.valueOf(preferences.getFloat("daily_limit", 1500f)).replace('.', ','));
        input.setSelection(input.getText().length());

        new AlertDialog.Builder(requireContext())
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
                        Toast.makeText(requireContext(), "Zapisano dzienny limit płatności.", Toast.LENGTH_SHORT).show();
                    } catch (NumberFormatException e) {
                        Toast.makeText(requireContext(), "Podaj kwotę większą od 0 i nie większą niż 1 000 000 PLN.", Toast.LENGTH_LONG).show();
                    }
                })
                .show();
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
}