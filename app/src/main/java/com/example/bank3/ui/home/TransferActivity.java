package com.example.bank3.ui.home;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.bank3.network.RetrofitClient;
import com.example.bank3.ui.theme.ThemeManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.GET;
import retrofit2.http.Path;

import java.util.ArrayList;
import java.util.List;

public class TransferActivity extends AppCompatActivity {

    private Spinner sourceAccountSpinner;
    private final List<Long> sourceAccountIds = new ArrayList<>();
    private long uzytkownikId = -1L;
    private EditText destinationAccountInput;
    private EditText amountInput;
    private EditText titleInput;
    private Spinner currencySpinner;
    private Spinner typeSpinner;
    private Button submitButton;

    private int backgroundColor;
    private int surfaceColor;
    private int textColor;
    private int secondaryTextColor;
    private int accentColor;

    private interface TransakcjeAPI {
        @POST("api/transakcje")
        Call<Map<String, Object>> createTransaction(@Body Map<String, Object> transaction);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applyColors();
        uzytkownikId = getIntent().getLongExtra("uzytkownikId", -1L);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(backgroundColor);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(22), dp(18), dp(22), dp(28));
        scrollView.addView(page);

        TextView back = new TextView(this);
        back.setText("‹  Wróć");
        back.setTextSize(16);
        back.setTextColor(accentColor);
        back.setPadding(0, dp(4), 0, dp(18));
        back.setOnClickListener(v -> finish());
        page.addView(back);

        TextView heading = new TextView(this);
        heading.setText("Nowy przelew");
        heading.setTextSize(28);
        heading.setTypeface(null, android.graphics.Typeface.BOLD);
        heading.setTextColor(textColor);
        page.addView(heading);

        TextView subtitle = new TextView(this);
        subtitle.setText("Wypełnij dane odbiorcy i szczegóły płatności.");
        subtitle.setTextSize(14);
        subtitle.setTextColor(secondaryTextColor);
        LinearLayout.LayoutParams subtitleParams = params(-1, -2);
        subtitleParams.topMargin = dp(7);
        subtitleParams.bottomMargin = dp(22);
        page.addView(subtitle, subtitleParams);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(20), dp(18), dp(20));
        card.setBackground(roundRect(surfaceColor, dp(22)));
        page.addView(card, params(-1, -2));

        addLabel(card, "Konto źródłowe");
        sourceAccountSpinner = addSpinner(card, new String[]{"Pobieranie Twoich kont..."});
        sourceAccountSpinner.setEnabled(false);
        destinationAccountInput = addInput(card, "Konto docelowe (ID)", "Np. 2", InputType.TYPE_CLASS_NUMBER);
        amountInput = addInput(card, "Kwota przelewu", "0,00", InputType.TYPE_CLASS_NUMBER
                | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        addLabel(card, "Waluta");
        currencySpinner = addSpinner(card, new String[]{"PLN", "EUR", "USD", "GBP"});
        addLabel(card, "Typ przelewu");
        typeSpinner = addSpinner(card, new String[]{"zwykly", "natychmiastowy", "wlasny"});
        titleInput = addInput(card, "Tytuł przelewu", "Np. Rachunki za październik",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);

        TextView hint = new TextView(this);
        hint.setText("Sprawdź numer konta i kwotę przed zatwierdzeniem.");
        hint.setTextSize(12);
        hint.setTextColor(secondaryTextColor);
        LinearLayout.LayoutParams hintParams = params(-1, -2);
        hintParams.topMargin = dp(14);
        card.addView(hint, hintParams);

        submitButton = new Button(this);
        submitButton.setText("Dalej — podsumowanie przelewu");
        submitButton.setAllCaps(false);
        submitButton.setTextColor(Color.WHITE);
        submitButton.setTextSize(15);
        submitButton.setBackground(roundRect(accentColor, dp(16)));
        LinearLayout.LayoutParams buttonParams = params(-1, dp(54));
        buttonParams.topMargin = dp(20);
        page.addView(submitButton, buttonParams);
        submitButton.setOnClickListener(v -> confirmTransfer());

        setContentView(scrollView);
        pobierzKontaUzytkownika();
    }

    private void applyColors() {
        backgroundColor = ThemeManager.getBackgroundColor(this);
        surfaceColor = ThemeManager.getSurfaceColor(this);
        textColor = ThemeManager.getTextColor(this);
        secondaryTextColor = ThemeManager.getSecondaryTextColor(this);
        accentColor = ThemeManager.getAccentColor(this);
    }

    private EditText addInput(LinearLayout parent, String label, String placeholder, int inputType) {
        addLabel(parent, label);
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setTextSize(15);
        input.setTextColor(textColor);
        input.setHintTextColor(secondaryTextColor);
        input.setHint(placeholder);
        input.setInputType(inputType);
        input.setPadding(dp(14), 0, dp(14), 0);
        input.setBackground(roundRect(backgroundColor, dp(12)));
        parent.addView(input, params(-1, dp(50)));
        return input;
    }

    private void addLabel(LinearLayout parent, String value) {
        TextView label = new TextView(this);
        label.setText(value);
        label.setTextSize(13);
        label.setTypeface(null, android.graphics.Typeface.BOLD);
        label.setTextColor(textColor);
        LinearLayout.LayoutParams lp = params(-1, -2);
        lp.topMargin = dp(13);
        lp.bottomMargin = dp(7);
        parent.addView(label, lp);
    }

    private Spinner addSpinner(LinearLayout parent, String[] values) {
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_item, values) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                view.setTextColor(textColor);
                view.setTextSize(15);
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getDropDownView(position, convertView, parent);
                view.setTextColor(textColor);
                view.setBackgroundColor(surfaceColor);
                view.setPadding(dp(12), dp(10), dp(12), dp(10));
                return view;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setPadding(dp(8), dp(3), dp(8), dp(3));
        spinner.setBackground(roundRect(backgroundColor, dp(12)));
        parent.addView(spinner, params(-1, dp(50)));
        return spinner;
    }


    private void pobierzKontaUzytkownika() {
        if (uzytkownikId <= 0) {
            sourceAccountSpinner.setEnabled(false);
            toast("Nie udało się ustalić zalogowanego użytkownika. Zaloguj się ponownie.");
            return;
        }

        TransakcjeAPI api = RetrofitClient.getInstance().create(TransakcjeAPI.class);
        api.getAccounts(uzytkownikId).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(
                    Call<List<Map<String, Object>>> call,
                    Response<List<Map<String, Object>>> response
            ) {
                if (!response.isSuccessful() || response.body() == null) {
                    sourceAccountSpinner.setEnabled(false);
                    toast("Nie udało się pobrać Twoich kont (HTTP " + response.code()
                            + "). Sprawdź endpoint GET /api/konta/uzytkownik/{id}.");
                    return;
                }

                List<String> labels = new ArrayList<>();
                sourceAccountIds.clear();

                for (Map<String, Object> account : response.body()) {
                    Object idValue = account.get("id");
                    if (!(idValue instanceof Number)) {
                        continue;
                    }

                    long accountId = ((Number) idValue).longValue();
                    sourceAccountIds.add(accountId);

                    Object number = account.get("numer_konta");
                    Object currency = account.get("waluta");
                    Object balance = account.get("saldo");

                    String label = "Konto " + (number != null ? number : ("#" + accountId));
                    if (balance != null || currency != null) {
                        label += "  ·  " + (balance != null ? balance : "")
                                + " " + (currency != null ? currency : "");
                    }
                    labels.add(label);
                }

                if (labels.isEmpty()) {
                    sourceAccountIds.clear();
                    labels.add("Brak kont przypisanych do użytkownika");
                    sourceAccountSpinner.setEnabled(false);
                    toast("Nie znaleziono kont dla zalogowanego użytkownika.");
                } else {
                    sourceAccountSpinner.setEnabled(true);
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        TransferActivity.this,
                        android.R.layout.simple_spinner_item,
                        labels
                );
                adapter.setDropDownViewResource(
                        android.R.layout.simple_spinner_dropdown_item
                );
                sourceAccountSpinner.setAdapter(adapter);
            }

            @Override
            public void onFailure(
                    Call<List<Map<String, Object>>> call,
                    Throwable t
            ) {
                sourceAccountSpinner.setEnabled(false);
                toast("Nie udało się pobrać kont. Sprawdź połączenie z serwerem.");
            }
        });
    }

    private void confirmTransfer() {
        String destination = destinationAccountInput.getText().toString().trim();
        String amountText = amountInput.getText().toString().trim().replace(',', '.');
        String title = titleInput.getText().toString().trim();

        if (destination.isEmpty() || amountText.isEmpty()) {
            toast("Uzupełnij konto docelowe i kwotę.");
            return;
        }

        final long sourceId;
        final long destinationId;
        final double amount;
        try {
            if (sourceAccountIds.isEmpty()
                    || sourceAccountSpinner.getSelectedItemPosition() < 0) {
                toast("Nie znaleziono konta źródłowego przypisanego do Twojego użytkownika.");
                return;
            }
            sourceId = sourceAccountIds.get(
                    sourceAccountSpinner.getSelectedItemPosition()
            );
            destinationId = Long.parseLong(destination);
            amount = Double.parseDouble(amountText);
        } catch (NumberFormatException e) {
            toast("Wprowadź poprawne numery kont i kwotę.");
            return;
        }

        if (sourceId <= 0 || destinationId <= 0) {
            toast("ID kont musi być większe od zera.");
            return;
        }
        if (sourceId == destinationId) {
            toast("Konto źródłowe i docelowe muszą być różne.");
            return;
        }
        if (!Double.isFinite(amount) || amount <= 0) {
            toast("Kwota musi być większa od zera.");
            return;
        }

        String currency = currencySpinner.getSelectedItem().toString();
        String type = typeSpinner.getSelectedItem().toString();
        String transferTitle = title.isEmpty() ? "Przelew" : title;
        String summary = "Z konta: " + sourceId
                + "
Na konto: " + destinationId
                + "
Kwota: " + String.format(Locale.getDefault(), "%.2f %s", amount, currency)
                + "
Typ: " + type
                + "
Tytuł: " + transferTitle;

        new AlertDialog.Builder(this)
                .setTitle("Potwierdź przelew")
                .setMessage(summary)
                .setNegativeButton("Popraw", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("Wyślij", (dialog, which) ->
                        sendTransfer(sourceId, destinationId, amount, currency, type, transferTitle))
                .show();
    }

    private void sendTransfer(long sourceId, long destinationId, double amount,
                              String currency, String type, String title) {
        submitButton.setEnabled(false);
        submitButton.setText("Wysyłanie…");

        Map<String, Object> transaction = new HashMap<>();
        transaction.put("kontro_zrodlowe_id", sourceId);
        transaction.put("konto_docelowe_id", destinationId);
        transaction.put("typ", type);
        transaction.put("kwota", amount);
        transaction.put("waluta", currency);
        transaction.put("tytul", title);
        transaction.put("data_operacji", new SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(new Date()));

        TransakcjeAPI api = RetrofitClient.getInstance().create(TransakcjeAPI.class);
        api.createTransaction(transaction).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call,
                                   Response<Map<String, Object>> response) {
                submitButton.setEnabled(true);
                submitButton.setText("Dalej — podsumowanie przelewu");
                if (response.isSuccessful()) {
                    new AlertDialog.Builder(TransferActivity.this)
                            .setTitle("Przelew wysłany")
                            .setMessage("Żądanie przelewu zostało przekazane do systemu.")
                            .setPositiveButton("Gotowe", (dialog, which) -> finish())
                            .show();
                } else {
                    String details = "";
                    try {
                        if (response.errorBody() != null) {
                            details = response.errorBody().string();
                        }
                    } catch (java.io.IOException ignored) {
                    }
                    toast("Nie udało się wykonać przelewu (HTTP "
                            + response.code() + "). " + details);
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                submitButton.setEnabled(true);
                submitButton.setText("Dalej — podsumowanie przelewu");
                toast("Błąd połączenia z serwerem. Sprawdź adres API i spróbuj ponownie.");
            }
        });
    }

    private GradientDrawable roundRect(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        return drawable;
    }

    private LinearLayout.LayoutParams params(int width, int height) {
        return new LinearLayout.LayoutParams(width, height);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
