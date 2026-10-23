package com.example.bank3.ui.home;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.example.bank3.data.repository.KontoRepository;
import com.example.bank3.ui.theme.ThemeManager;

import java.text.NumberFormat;
import java.util.Locale;

public class CardsActivity extends AppCompatActivity {

    private static final String PREFS_PREFIX = "demo_card_settings_";
    private long userId;
    private android.content.SharedPreferences preferences;
    private LinearLayout root;
    private TextView cardNumber;
    private TextView limitValue;
    private SwitchCompat blockedSwitch;
    private SwitchCompat onlineSwitch;
    private SwitchCompat contactlessSwitch;
    private SwitchCompat atmSwitch;
    private Button revealButton;
    private boolean numberRevealed;

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private int textColor() {
        return ThemeManager.getTextColor(this);
    }

    private int secondaryColor() {
        return ThemeManager.getSecondaryTextColor(this);
    }

    private int surfaceColor() {
        return ThemeManager.getSurfaceColor(this);
    }

    private int backgroundColor() {
        return ThemeManager.getBackgroundColor(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);

        userId = getIntent().getLongExtra("uzytkownikId", -1L);
        preferences = getSharedPreferences(PREFS_PREFIX + userId, MODE_PRIVATE);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(backgroundColor());

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(12), dp(20), dp(28));
        scroll.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        setContentView(scroll);
        buildScreen();
    }

    private void buildScreen() {
        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);

        TextView back = new TextView(this);
        back.setText("‹");
        back.setTextSize(38);
        back.setTextColor(ThemeManager.getAccentColor(this));
        back.setGravity(Gravity.CENTER);
        back.setContentDescription("Wróć");
        toolbar.addView(back, new LinearLayout.LayoutParams(dp(44), dp(48)));
        back.setOnClickListener(v -> finish());

        TextView heading = new TextView(this);
        heading.setText("Karty");
        heading.setTextSize(25);
        heading.setTypeface(null, Typeface.BOLD);
        heading.setTextColor(textColor());
        LinearLayout.LayoutParams headingParams = new LinearLayout.LayoutParams(0, -2, 1);
        headingParams.leftMargin = dp(8);
        toolbar.addView(heading, headingParams);
        root.addView(toolbar);

        TextView intro = text("Zarządzaj kartą i jej ustawieniami.", 14, secondaryColor());
        addGap(intro, 2);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(22), dp(20), dp(22), dp(20));
        card.setBackground(gradientCard());
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, dp(208));
        cardParams.topMargin = dp(18);
        root.addView(card, cardParams);

        TextView brand = text("DOOGIE  •  DEBIT", 14, Color.WHITE);
        brand.setTypeface(null, Typeface.BOLD);
        card.addView(brand);

        TextView demo = text("KARTA DEMONSTRACYJNA", 10, Color.rgb(220, 232, 255));
        demo.setLetterSpacing(0.12f);
        LinearLayout.LayoutParams demoParams = new LinearLayout.LayoutParams(-1, -2);
        demoParams.topMargin = dp(5);
        card.addView(demo, demoParams);

        View spacer = new View(this);
        card.addView(spacer, new LinearLayout.LayoutParams(1, 0, 1));

        cardNumber = text("••••  ••••  ••••  1024", 20, Color.WHITE);
        cardNumber.setTypeface(null, Typeface.BOLD);
        card.addView(cardNumber);

        LinearLayout cardBottom = new LinearLayout(this);
        cardBottom.setOrientation(LinearLayout.HORIZONTAL);
        cardBottom.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams bottomParams = new LinearLayout.LayoutParams(-1, -2);
        bottomParams.topMargin = dp(14);
        card.addView(cardBottom, bottomParams);

        TextView holder = text("UŻYTKOWNIK", 11, Color.rgb(220, 232, 255));
        holder.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));
        cardBottom.addView(holder);
        TextView expiry = text("WAŻNA DO 12/30", 10, Color.rgb(220, 232, 255));
        cardBottom.addView(expiry);

        revealButton = new Button(this);
        revealButton.setText("Pokaż dane");
        revealButton.setAllCaps(false);
        revealButton.setOnClickListener(v -> {
            numberRevealed = !numberRevealed;
            cardNumber.setText(numberRevealed
                    ? "DEMO  ••••  1024"
                    : "••••  ••••  ••••  1024");
            revealButton.setText(numberRevealed ? "Ukryj dane" : "Pokaż dane");
        });
        LinearLayout.LayoutParams revealParams = new LinearLayout.LayoutParams(-1, dp(48));
        revealParams.topMargin = dp(12);
        root.addView(revealButton, revealParams);

        addSectionTitle("Ustawienia karty");
        blockedSwitch = addSwitch("Zablokuj kartę", "Płatności kartą zostaną oznaczone jako wyłączone.");
        onlineSwitch = addSwitch("Płatności internetowe", "Zezwalaj na płatności online.");
        contactlessSwitch = addSwitch("Płatności zbliżeniowe", "Zezwalaj na płatności zbliżeniowe.");
        atmSwitch = addSwitch("Wypłaty z bankomatów", "Zezwalaj na wypłaty gotówki.");

        blockedSwitch.setChecked(preferences.getBoolean("blocked", false));
        onlineSwitch.setChecked(preferences.getBoolean("online", true));
        contactlessSwitch.setChecked(preferences.getBoolean("contactless", true));
        atmSwitch.setChecked(preferences.getBoolean("atm", true));

        blockedSwitch.setOnCheckedChangeListener((button, checked) -> {
            preferences.edit().putBoolean("blocked", checked).apply();
            updateControls();
            Toast.makeText(this, checked ? "Karta została zablokowana (demo)." : "Karta została odblokowana (demo).",
                    Toast.LENGTH_SHORT).show();
        });
        onlineSwitch.setOnCheckedChangeListener((button, checked) ->
                preferences.edit().putBoolean("online", checked).apply());
        contactlessSwitch.setOnCheckedChangeListener((button, checked) ->
                preferences.edit().putBoolean("contactless", checked).apply());
        atmSwitch.setOnCheckedChangeListener((button, checked) ->
                preferences.edit().putBoolean("atm", checked).apply());

        addSectionTitle("Dzienny limit płatności");
        LinearLayout limitPanel = panel();
        limitPanel.setOrientation(LinearLayout.HORIZONTAL);
        limitPanel.setGravity(Gravity.CENTER_VERTICAL);
        limitPanel.setPadding(dp(16), dp(12), dp(12), dp(12));

        LinearLayout limitTexts = new LinearLayout(this);
        limitTexts.setOrientation(LinearLayout.VERTICAL);
        limitValue = text("", 20, textColor());
        limitValue.setTypeface(null, Typeface.BOLD);
        limitTexts.addView(limitValue);
        TextView limitCaption = text("Limit demonstracyjny", 12, secondaryColor());
        LinearLayout.LayoutParams captionParams = new LinearLayout.LayoutParams(-1, -2);
        captionParams.topMargin = dp(3);
        limitTexts.addView(limitCaption, captionParams);
        limitPanel.addView(limitTexts, new LinearLayout.LayoutParams(0, -2, 1));

        Button changeLimit = new Button(this);
        changeLimit.setText("Zmień");
        changeLimit.setAllCaps(false);
        changeLimit.setOnClickListener(v -> showLimitDialog());
        limitPanel.addView(changeLimit);
        LinearLayout.LayoutParams limitParams = new LinearLayout.LayoutParams(-1, -2);
        limitParams.topMargin = dp(8);
        root.addView(limitPanel, limitParams);
        updateLimitText();

        addSectionTitle("Historia transakcji kartą");
        LinearLayout historyPanel = panel();
        historyPanel.setOrientation(LinearLayout.VERTICAL);
        historyPanel.setPadding(dp(16), dp(16), dp(16), dp(16));
        TextView historyTitle = text("Brak danych kartowych", 15, textColor());
        historyTitle.setTypeface(null, Typeface.BOLD);
        historyPanel.addView(historyTitle);
        TextView historyDescription = text(
                "Historia kartowa nie jest jeszcze udostępniana przez backend. Ustawienia na tym ekranie są zapisywane lokalnie na tym urządzeniu.",
                13, secondaryColor());
        LinearLayout.LayoutParams historyDescParams = new LinearLayout.LayoutParams(-1, -2);
        historyDescParams.topMargin = dp(6);
        historyPanel.addView(historyDescription, historyDescParams);
        LinearLayout.LayoutParams historyParams = new LinearLayout.LayoutParams(-1, -2);
        historyParams.topMargin = dp(8);
        root.addView(historyPanel, historyParams);

        TextView footer = text("Tryb demonstracyjny — nie jest to prawdziwa karta płatnicza.", 12, secondaryColor());
        footer.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams footerParams = new LinearLayout.LayoutParams(-1, -2);
        footerParams.topMargin = dp(20);
        root.addView(footer, footerParams);

        updateControls();
    }

    private SwitchCompat addSwitch(String title, String subtitle) {
        LinearLayout item = panel();
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setPadding(dp(14), dp(10), dp(10), dp(10));

        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        TextView name = text(title, 15, textColor());
        name.setTypeface(null, Typeface.BOLD);
        labels.addView(name);
        TextView detail = text(subtitle, 12, secondaryColor());
        LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(-1, -2);
        detailParams.topMargin = dp(3);
        labels.addView(detail, detailParams);
        item.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));

        SwitchCompat toggle = new SwitchCompat(this);
        toggle.setContentDescription(title);
        item.addView(toggle, new LinearLayout.LayoutParams(-2, -2));
        LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(-1, -2);
        itemParams.topMargin = dp(8);
        root.addView(item, itemParams);
        return toggle;
    }

    private void updateControls() {
        boolean blocked = blockedSwitch != null && blockedSwitch.isChecked();
        if (onlineSwitch != null) onlineSwitch.setEnabled(!blocked);
        if (contactlessSwitch != null) contactlessSwitch.setEnabled(!blocked);
        if (atmSwitch != null) atmSwitch.setEnabled(!blocked);
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
                .setMessage("Podaj limit w PLN.")
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
                        Toast.makeText(this, "Zapisano limit demonstracyjny.", Toast.LENGTH_SHORT).show();
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Podaj kwotę większą od 0 i nie większą niż 1 000 000 PLN.", Toast.LENGTH_LONG).show();
                    }
                })
                .show();
    }

    private void updateLimitText() {
        if (limitValue == null) return;
        float limit = preferences.getFloat("daily_limit", 1500f);
        NumberFormat format = NumberFormat.getNumberInstance(new Locale("pl", "PL"));
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);
        limitValue.setText(format.format(limit) + " PLN");
    }

    private void addSectionTitle(String title) {
        TextView heading = text(title, 19, textColor());
        heading.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(26);
        params.bottomMargin = dp(4);
        root.addView(heading, params);
    }

    private LinearLayout panel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setBackground(rounded(surfaceColor(), 18));
        return panel;
    }

    private TextView text(String value, float size, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        return view;
    }

    private void addGap(View view, int ignored) {
        root.addView(view);
    }

    private GradientDrawable rounded(int color, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
    }

    private GradientDrawable gradientCard() {
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(24, 54, 112), Color.rgb(45, 102, 190), Color.rgb(31, 125, 151)});
        drawable.setCornerRadius(dp(22));
        return drawable;
    }
}
