package com.example.bank3.ui.home;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
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

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public class TransferActivity extends AppCompatActivity {

    private Spinner sourceAccountSpinner;
    private Spinner currencySpinner;
    private EditText destinationAccountInput;
    private EditText recipientNameInput;
    private EditText amountInput;
    private EditText titleInput;
    private Button submitButton;

    private final List<SourceAccount> sourceAccounts = new ArrayList<>();
    private long uzytkownikId = -1L;
    private SourceAccount selectedSourceAccount;
    private Call<List<KontoApiResponse>> accountsCall;
    private Call<Map<String, Object>> destinationCall;
    private Call<Map<String, Object>> transactionCall;
    private boolean submitting;

    private int backgroundColor;
    private int surfaceColor;
    private int textColor;
    private int secondaryTextColor;
    private int accentColor;

    // Odpowiedź API konta zawiera ID KONTA, numer konta, saldo i walutę.
    // Nie używamy pola uzytkownik_id z tabeli konta.
    private static class KontoApiResponse {
        Long id;
        String numerKonta;
        BigDecimal saldo;
        String waluta;
    }

    private static class SourceAccount {
        final long id;
        final String number;
        final String balance;
        final String currency;

        SourceAccount(long id, String number, String balance, String currency) {
            this.id = id;
            this.number = number;
            this.balance = balance;
            this.currency = currency;
        }

        String label() {
            return "ID konta: " + id;
        }
    }

    private interface TransakcjeAPI {
        @GET("api/konta/uzytkownik/{uzytkownikId}")
        Call<List<KontoApiResponse>> getAccountsForUser(
                @Path("uzytkownikId") long uzytkownikId
        );

        @GET("api/konta/{id}")
        Call<Map<String, Object>> getAccount(@Path("id") long accountId);

        @POST("api/transakcje")
        Call<Map<String, Object>> createTransaction(@Body Map<String, Object> transaction);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applyColors();
        uzytkownikId = getIntent().getLongExtra("uzytkownikId", -1L);

        setContentView(R.layout.activity_transfer);

        View back = findViewById(R.id.transferBack);
        back.setOnClickListener(v -> finish());

        sourceAccountSpinner = findViewById(R.id.sourceAccountSpinner);
        currencySpinner = findViewById(R.id.currencySpinner);
        destinationAccountInput = findViewById(R.id.destinationAccountInput);
        recipientNameInput = findViewById(R.id.recipientNameInput);
        amountInput = findViewById(R.id.amountInput);
        titleInput = findViewById(R.id.titleInput);
        submitButton = findViewById(R.id.submitTransferButton);

        sourceAccountSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < sourceAccounts.size()) {
                    selectedSourceAccount = sourceAccounts.get(position);
                    ustawWaluteZrodla();
                } else {
                    selectedSourceAccount = null;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedSourceAccount = null;
            }
        });

        destinationAccountInput.setFilters(new InputFilter[]{
                new InputFilter.LengthFilter(8)
        });

        findViewById(R.id.transferPage).setBackgroundColor(backgroundColor);
        findViewById(R.id.transferScrollView).setBackgroundColor(backgroundColor);
        findViewById(R.id.transferFormCard).setBackground(roundRect(surfaceColor, dp(22)));
        submitButton.setTextColor(Color.WHITE);
        submitButton.setBackground(roundRect(accentColor, dp(16)));

        int[] labelIds = {
                R.id.transferHeading, R.id.sourceAccountLabel,
                R.id.destinationAccountLabel, R.id.recipientNameLabel,
                R.id.amountLabel, R.id.currencyLabel, R.id.titleLabel
        };
        for (int id : labelIds) {
            TextView label = findViewById(id);
            label.setTextColor(textColor);
        }
        ((TextView) findViewById(R.id.transferBack)).setTextColor(accentColor);
        ((TextView) findViewById(R.id.transferSubtitle)).setTextColor(secondaryTextColor);
        ((TextView) findViewById(R.id.transferHint)).setTextColor(secondaryTextColor);

        EditText[] inputs = {
                destinationAccountInput, recipientNameInput, amountInput, titleInput
        };
        for (EditText input : inputs) {
            input.setTextColor(textColor);
            input.setHintTextColor(secondaryTextColor);
            input.setBackground(roundRect(backgroundColor, dp(12)));
        }
        sourceAccountSpinner.setBackground(roundRect(backgroundColor, dp(12)));
        currencySpinner.setBackground(roundRect(backgroundColor, dp(12)));

        submitButton.setOnClickListener(v -> sprawdzOdbiorce());

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
            pokazBladKontaZrodlowego("Brak ID zalogowanego użytkownika. Zaloguj się ponownie.");
            return;
        }

        TransakcjeAPI api = RetrofitClient.getInstance().create(TransakcjeAPI.class);
        accountsCall = api.getAccountsForUser(uzytkownikId);
        accountsCall.enqueue(new Callback<List<KontoApiResponse>>() {
            @Override
            public void onResponse(
                    Call<List<KontoApiResponse>> call,
                    Response<List<KontoApiResponse>> response
            ) {
                if (call.isCanceled() || isFinishing() || isDestroyed()) return;

                if (!response.isSuccessful() || response.body() == null) {
                    pokazBladKontaZrodlowego("Nie udało się pobrać kont użytkownika (HTTP "
                            + response.code() + "). " + odczytajBlad(response));
                    return;
                }

                sourceAccounts.clear();
                for (KontoApiResponse account : response.body()) {
                    if (account == null || account.id == null
                            || account.numerKonta == null || account.waluta == null) {
                        continue;
                    }

                    // To jest konto.id zwrócone przez API, a nie uzytkownik_id.
                    long accountId = account.id;
                    String number = account.numerKonta.trim();
                    String currency = account.waluta.trim().toUpperCase(Locale.ROOT);
                    String balance = account.saldo == null ? "—" : account.saldo.toPlainString();

                    if (accountId > 0 && !number.isEmpty() && currency.matches("[A-Z]{3}")) {
                        sourceAccounts.add(new SourceAccount(accountId, number, balance, currency));
                    }
                }

                if (sourceAccounts.isEmpty()) {
                    pokazBladKontaZrodlowego("API nie zwróciło żadnego konta przypisanego do tego użytkownika.");
                    return;
                }

                String[] labels = new String[sourceAccounts.size()];
                for (int i = 0; i < sourceAccounts.size(); i++) {
                    labels[i] = sourceAccounts.get(i).label();
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        TransferActivity.this, android.R.layout.simple_spinner_item, labels);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                sourceAccountSpinner.setAdapter(adapter);
                sourceAccountSpinner.setEnabled(sourceAccounts.size() > 1);
                sourceAccountSpinner.setSelection(0);
                selectedSourceAccount = sourceAccounts.get(0);
                ustawWaluteZrodla();
            }

            @Override
            public void onFailure(Call<List<KontoApiResponse>> call, Throwable t) {
                if (call.isCanceled() || isFinishing() || isDestroyed()) return;
                pokazBladKontaZrodlowego("Błąd połączenia podczas pobierania Twoich kont: "
                        + (t.getMessage() == null ? t.getClass().getSimpleName() : t.getMessage()));
            }
        });
    }

    private void ustawWaluteZrodla() {
        if (selectedSourceAccount == null) return;
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{selectedSourceAccount.currency}
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        currencySpinner.setAdapter(adapter);
        currencySpinner.setEnabled(false);
    }

    private void pokazBladKontaZrodlowego(String message) {
        selectedSourceAccount = null;
        sourceAccounts.clear();
        sourceAccountSpinner.setEnabled(false);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{"Nie udało się pobrać kont źródłowych"}
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sourceAccountSpinner.setAdapter(adapter);
        toast(message);
    }

    private void sprawdzOdbiorce() {
        if (submitting) return;
        if (selectedSourceAccount == null) {
            toast("Najpierw pobierz konto źródłowe. Nie można bezpiecznie wykonać przelewu.");
            return;
        }

        String destinationIdText = destinationAccountInput.getText().toString().trim();
        String recipientName = recipientNameInput.getText().toString().trim();
        String amountText = amountInput.getText().toString().trim().replace(',', '.');
        String title = titleInput.getText().toString().trim();

        if (destinationIdText.isEmpty() || recipientName.isEmpty()
                || amountText.isEmpty() || title.isEmpty()) {
            toast("Uzupełnij ID konta odbiorcy, imię odbiorcy, kwotę i tytuł.");
            return;
        }
        if (!destinationIdText.matches("[1-9][0-9]{0,7}")) {
            toast("ID konta odbiorcy musi być liczbą od 1 do 99 999 999 (maksymalnie 8 cyfr).");
            return;
        }
        if (title.length() > 140) {
            toast("Tytuł przelewu może mieć maksymalnie 140 znaków.");
            return;
        }

        final long destinationAccountId;
        final BigDecimal amount;
        try {
            destinationAccountId = Long.parseLong(destinationIdText);
            amount = new BigDecimal(amountText);
        } catch (NumberFormatException e) {
            toast("Wprowadź poprawne ID konta i kwotę.");
            return;
        }

        if (destinationAccountId <= 0) {
            toast("ID konta odbiorcy musi być większe od zera.");
            return;
        }
        if (destinationAccountId == selectedSourceAccount.id) {
            toast("Konto odbiorcy musi być inne niż konto źródłowe.");
            return;
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            toast("Kwota musi być większa od zera.");
            return;
        }
        if (amount.scale() > 2) {
            toast("Kwota może mieć maksymalnie dwa miejsca po przecinku.");
            return;
        }

        setSubmitting(true, "Sprawdzanie konta odbiorcy…");
        TransakcjeAPI api = RetrofitClient.getInstance().create(TransakcjeAPI.class);
        destinationCall = api.getAccount(destinationAccountId);
        destinationCall.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(
                    Call<Map<String, Object>> call,
                    Response<Map<String, Object>> response
            ) {
                if (call.isCanceled() || isFinishing() || isDestroyed()) return;

                if (!response.isSuccessful() || response.body() == null) {
                    setSubmitting(false, null);
                    toast("Nie znaleziono konta odbiorcy (HTTP " + response.code() + "). "
                            + odczytajBlad(response));
                    return;
                }

                Map<String, Object> account = response.body();
                Object idValue = account.get("id");
                Object numberValue = account.get("numerKonta");
                Object currencyValue = account.get("waluta");

                if (!(idValue instanceof Number) || numberValue == null || currencyValue == null) {
                    setSubmitting(false, null);
                    toast("API nie zwróciło pełnych danych konta odbiorcy.");
                    return;
                }

                long resolvedId = ((Number) idValue).longValue();
                String accountNumber = numberValue.toString().replaceAll("\\s+", "");
                String destinationCurrency = currencyValue.toString().trim().toUpperCase(Locale.ROOT);

                if (resolvedId != destinationAccountId) {
                    setSubmitting(false, null);
                    toast("API zwróciło inne konto niż wskazane.");
                    return;
                }
                if (!accountNumber.matches("\\d{26}")) {
                    setSubmitting(false, null);
                    toast("Numer tego konta w bazie nie ma 26 cyfr. Backend odrzuci taki przelew; sprawdź konto odbiorcy.");
                    return;
                }
                if (!selectedSourceAccount.currency.equals(destinationCurrency)) {
                    setSubmitting(false, null);
                    toast("Konta mają różne waluty. Źródło: " + selectedSourceAccount.currency
                            + ", odbiorca: " + destinationCurrency + ".");
                    return;
                }

                setSubmitting(false, null);
                pokazPodsumowanie(resolvedId, accountNumber, recipientName, amount, title,
                        selectedSourceAccount.id, selectedSourceAccount.currency);
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                if (call.isCanceled() || isFinishing() || isDestroyed()) return;
                setSubmitting(false, null);
                toast("Nie udało się sprawdzić konta odbiorcy: "
                        + (t.getMessage() == null ? t.getClass().getSimpleName() : t.getMessage()));
            }
        });
    }

    private void pokazPodsumowanie(
            long destinationId,
            String destinationNumber,
            String recipientName,
            BigDecimal amount,
            String title,
            long sourceId,
            String currency
    ) {
        String summary = "Konto źródłowe ID: " + sourceId
                + "\nOdbiorca: " + recipientName
                + "\nKonto odbiorcy ID: " + destinationId
                + "\nNumer konta odbiorcy: " + destinationNumber
                + "\nKwota: " + String.format(Locale.getDefault(), "%.2f %s", amount, currency)
                + "\nTyp: przelew"
                + "\nTytuł: " + title;

        new AlertDialog.Builder(this)
                .setTitle("Potwierdź przelew")
                .setMessage(summary)
                .setNegativeButton("Popraw", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("Wyślij", (dialog, which) ->
                        wyslijPrzelew(sourceId, destinationNumber, recipientName, amount, currency, title))
                .show();
    }

    private void wyslijPrzelew(
            long sourceId,
            String destinationNumber,
            String recipientName,
            BigDecimal amount,
            String currency,
            String title
    ) {
        if (submitting) return;
        setSubmitting(true, "Wysyłanie przelewu…");

        Map<String, Object> transaction = new HashMap<>();
        transaction.put("uzytkownikId", uzytkownikId);
        transaction.put("kontoZrodloweId", sourceId);
        transaction.put("numerKontaOdbiorcy", destinationNumber);
        transaction.put("imieOdbiorcy", recipientName);
        transaction.put("typ", "PRZELEW");
        transaction.put("kwota", amount);
        transaction.put("waluta", currency);
        transaction.put("tytul", title);

        TransakcjeAPI api = RetrofitClient.getInstance().create(TransakcjeAPI.class);
        transactionCall = api.createTransaction(transaction);
        transactionCall.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(
                    Call<Map<String, Object>> call,
                    Response<Map<String, Object>> response
            ) {
                if (call.isCanceled() || isFinishing() || isDestroyed()) return;
                setSubmitting(false, null);

                if (response.isSuccessful()) {
                    Map<String, Object> result = response.body();
                    String message = "Przelew został wykonany.";
                    if (result != null && result.get("id") != null) {
                        message += "\nNumer transakcji: " + result.get("id");
                    }
                    new AlertDialog.Builder(TransferActivity.this)
                            .setTitle("Przelew wykonany")
                            .setMessage(message)
                            .setPositiveButton("Gotowe", (dialog, which) -> finish())
                            .show();
                } else {
                    toast("Backend odrzucił przelew (HTTP " + response.code() + "). "
                            + odczytajBlad(response));
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                if (call.isCanceled() || isFinishing() || isDestroyed()) return;
                setSubmitting(false, null);
                toast("Błąd połączenia podczas wysyłania przelewu: "
                        + (t.getMessage() == null ? t.getClass().getSimpleName() : t.getMessage()));
            }
        });
    }

    private String odczytajBlad(Response<?> response) {
        try {
            if (response.errorBody() != null) {
                String error = response.errorBody().string();
                return error == null || error.trim().isEmpty() ? "" : error;
            }
        } catch (IOException ignored) {
        }
        return "";
    }

    private void setSubmitting(boolean value, String label) {
        submitting = value;
        submitButton.setEnabled(!value);
        submitButton.setText(value
                ? (label == null ? "Przetwarzanie…" : label)
                : "Dalej — podsumowanie przelewu");
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

    @Override
    protected void onDestroy() {
        if (accountsCall != null) accountsCall.cancel();
        if (destinationCall != null) destinationCall.cancel();
        if (transactionCall != null) transactionCall.cancel();
        super.onDestroy();
    }
}
