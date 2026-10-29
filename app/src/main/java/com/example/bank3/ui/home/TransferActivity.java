package com.example.bank3.ui.home;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.bank3.R;
import com.example.bank3.data.api.UserAPI;
import com.example.bank3.model.UserResponse;
import com.example.bank3.network.RetrofitClient;
import com.example.bank3.ui.theme.ThemeManager;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;

import okhttp3.ResponseBody;
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
import retrofit2.http.Query;

public class TransferActivity extends AppCompatActivity {

    private String userFullName = "";

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

    private static class KontoApiResponse {
        Long id;
        String numerKonta;
        BigDecimal saldo;
        String waluta;
    }

    private static class SourceAccount {
        final long id;
        final String number;
        final BigDecimal balance;
        final String currency;
        String ownerName;

        SourceAccount(long id, String number, BigDecimal balance, String currency, String ownerName) {
            this.id = id;
            this.number = number;
            this.balance = balance;
            this.currency = currency;
            this.ownerName = ownerName;
        }

        String label() {
            if (ownerName != null && !ownerName.trim().isEmpty()) {
                return ownerName;
            }
            return "Konto (" + currency + ")";
        }
    }

    private interface TransakcjeAPI {
        @GET("api/konta/uzytkownik/{uzytkownikId}")
        Call<List<KontoApiResponse>> getAccountsForUser(
                @Path("uzytkownikId") long uzytkownikId
        );

        @GET("api/konta/email")
        Call<Map<String, Object>> getAccountByEmailQuery(@Query("email") String email);

        @GET("api/uzytkownicy")
        Call<List<UserResponse>> getAllUsers();

        @POST("api/transakcje")
        Call<Map<String, Object>> createTransaction(@Body Map<String, Object> transaction);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_transfer);

        uzytkownikId = getIntent().getLongExtra("uzytkownikId", -1L);

        View root = findViewById(R.id.transferRoot);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
                androidx.core.graphics.Insets bars = windowInsets.getInsets(
                        WindowInsetsCompat.Type.systemBars()
                );
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                return windowInsets;
            });
        }

        TextView back = findViewById(R.id.transferBack);
        if (back != null) {
            back.setOnClickListener(v -> finish());
        }

        sourceAccountSpinner = findViewById(R.id.sourceAccountSpinner);
        destinationAccountInput = findViewById(R.id.destinationAccountInput);
        recipientNameInput = findViewById(R.id.recipientNameInput);
        amountInput = findViewById(R.id.amountInput);
        currencySpinner = findViewById(R.id.currencySpinner);
        titleInput = findViewById(R.id.titleInput);
        submitButton = findViewById(R.id.submitTransferButton);

        if (sourceAccountSpinner != null) {
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
        }

        if (amountInput != null) {
            amountInput.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) { }

                @Override
                public void afterTextChanged(Editable s) {
                    walidujKwoteWzgledemSalda(false);
                }
            });
        }

        if (submitButton != null) {
            int accentColor = ThemeManager.getAccentColor(this);
            submitButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(accentColor));
            submitButton.setOnClickListener(v -> sprawdzOdbiorce());
        }

        int accentColor = ThemeManager.getAccentColor(this);
        com.google.android.material.textfield.TextInputLayout[] layouts = {
                findViewById(R.id.layoutDestinationAccount),
                findViewById(R.id.layoutRecipientName),
                findViewById(R.id.layoutAmount),
                findViewById(R.id.layoutTitle)
        };
        for (com.google.android.material.textfield.TextInputLayout layout : layouts) {
            if (layout != null) {
                layout.setBoxStrokeColor(accentColor);
                layout.setHintTextColor(android.content.res.ColorStateList.valueOf(accentColor));
            }
        }

        pobierzDaneUzytkownika();
        pobierzKontaUzytkownika();
    }

    private void pobierzDaneUzytkownika() {
        if (uzytkownikId <= 0) return;

        UserAPI userAPI = RetrofitClient.getInstance().create(UserAPI.class);
        userAPI.getUserById(uzytkownikId).enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                if (isFinishing() || isDestroyed() || call.isCanceled()) return;
                if (response.isSuccessful() && response.body() != null) {
                    UserResponse u = response.body();
                    String imie = u.getImie() != null ? u.getImie().trim() : "";
                    String nazwisko = u.getNazwisko() != null ? u.getNazwisko().trim() : "";
                    userFullName = (imie + " " + nazwisko).trim();
                    if (userFullName.isEmpty()) {
                        userFullName = u.getLogin() != null ? u.getLogin() : "";
                    }
                    updateSourceAccountsLabels();
                }
            }

            @Override
            public void onFailure(Call<UserResponse> call, Throwable t) {}
        });
    }

    private void updateSourceAccountsLabels() {
        if (sourceAccounts.isEmpty()) return;
        for (SourceAccount sa : sourceAccounts) {
            sa.ownerName = userFullName;
        }
        String[] labels = new String[sourceAccounts.size()];
        for (int i = 0; i < sourceAccounts.size(); i++) {
            labels[i] = sourceAccounts.get(i).label();
        }
        ArrayAdapter<String> adapter = createSpinnerAdapter(this, labels);
        if (sourceAccountSpinner != null) {
            int selectedPos = sourceAccountSpinner.getSelectedItemPosition();
            sourceAccountSpinner.setAdapter(adapter);
            if (selectedPos >= 0 && selectedPos < sourceAccounts.size()) {
                sourceAccountSpinner.setSelection(selectedPos);
            }
        }
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

                    long accountId = account.id;
                    String number = account.numerKonta.trim();
                    String currency = account.waluta.trim().toUpperCase(Locale.ROOT);
                    if (accountId > 0 && !number.isEmpty() && currency.matches("[A-Z]{3}")) {
                        sourceAccounts.add(new SourceAccount(accountId, number, account.saldo, currency, userFullName));
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

                ArrayAdapter<String> adapter = createSpinnerAdapter(TransferActivity.this, labels);
                if (sourceAccountSpinner != null) {
                    sourceAccountSpinner.setAdapter(adapter);
                    sourceAccountSpinner.setEnabled(sourceAccounts.size() > 1);
                    sourceAccountSpinner.setSelection(0);
                }
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
        if (selectedSourceAccount == null || currencySpinner == null) return;
        ArrayAdapter<String> adapter = createSpinnerAdapter(this, new String[]{selectedSourceAccount.currency});
        currencySpinner.setAdapter(adapter);
        currencySpinner.setEnabled(false);
    }

    private void pokazBladKontaZrodlowego(String message) {
        selectedSourceAccount = null;
        sourceAccounts.clear();
        if (sourceAccountSpinner != null) {
            sourceAccountSpinner.setEnabled(false);
            ArrayAdapter<String> adapter = createSpinnerAdapter(this, new String[]{"Nie udało się pobrać kont źródłowych"});
            sourceAccountSpinner.setAdapter(adapter);
        }
        toast(message);
    }

    private ArrayAdapter<String> createSpinnerAdapter(android.content.Context context, String[] items) {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                context, android.R.layout.simple_spinner_item, items) {
            @Override
            public View getView(int position, View convertView, android.view.ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                view.setTextColor(ThemeManager.getTextColor(context));
                view.setTextSize(15);
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, android.view.ViewGroup parent) {
                TextView view = (TextView) super.getDropDownView(position, convertView, parent);
                view.setTextColor(ThemeManager.getTextColor(context));
                view.setBackgroundColor(ThemeManager.getSurfaceColor(context));
                int pad = (int) (12 * context.getResources().getDisplayMetrics().density + 0.5f);
                view.setPadding(pad, pad, pad, pad);
                return view;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return adapter;
    }

    private void sprawdzOdbiorce() {
        if (submitting) return;
        if (selectedSourceAccount == null) {
            toast("Najpierw pobierz konto źródłowe. Nie można bezpiecznie wykonać przelewu.");
            return;
        }

        SharedPreferences cardPrefs =
                getSharedPreferences("demo_card_settings_" + uzytkownikId, MODE_PRIVATE);
        if (cardPrefs.getBoolean("blocked", false)) {
            toast("Twoje konto jest zablokowane. Zmień ustawienia w zakładce Karty, aby odblokować przelewy.");
            return;
        }

        String recipientEmail = destinationAccountInput.getText().toString().trim();
        String recipientName = recipientNameInput.getText().toString().trim();
        String amountText = amountInput.getText().toString().trim().replace(',', '.');
        String title = titleInput.getText().toString().trim();

        if (recipientEmail.isEmpty() || recipientName.isEmpty()
                || amountText.isEmpty() || title.isEmpty()) {
            toast("Uzupełnij e-mail odbiorcy, imię odbiorcy, kwotę i tytuł.");
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(recipientEmail).matches()) {
            toast("Podaj poprawny adres e-mail odbiorcy.");
            return;
        }
        if (title.length() > 140) {
            toast("Tytuł przelewu może mieć maksymalnie 140 znaków.");
            return;
        }

        final BigDecimal amount;
        try {
            amount = new BigDecimal(amountText);
        } catch (NumberFormatException e) {
            toast("Wprowadź poprawną kwotę.");
            return;
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            toast("Kwota musi być większa od zera.");
            return;
        }
        if (amount.scale() > 2) {
            amountInput.setError("Kwota może mieć maksymalnie dwa miejsca po przecinku.");
            amountInput.requestFocus();
            return;
        }

        float dailyLimit = cardPrefs.getFloat("daily_limit", 1500f);
        BigDecimal maxLimit = new BigDecimal(Float.toString(dailyLimit));
        if (amount.compareTo(maxLimit) > 0) {
            amountInput.setError("Kwota przekracza Twój dzienny limit płatności ("
                    + String.format(Locale.getDefault(), "%.2f", dailyLimit)
                    + " PLN). Zmień limit w zakładce Karty.");
            amountInput.requestFocus();
            return;
        }

        if (selectedSourceAccount.balance != null
                && amount.compareTo(selectedSourceAccount.balance) > 0) {
            amountInput.setError("Kwota przekracza dostępne saldo: "
                    + selectedSourceAccount.balance.toPlainString() + " "
                    + selectedSourceAccount.currency + ".");
            amountInput.requestFocus();
            return;
        }

        setSubmitting(true, "Sprawdzanie konta odbiorcy…");
        szukajKontaOdbiorce(recipientEmail, recipientName, amount, title);
    }

    private void szukajKontaOdbiorce(
            String recipientEmail,
            String recipientName,
            BigDecimal amount,
            String title
    ) {
        TransakcjeAPI api = RetrofitClient.getInstance().create(TransakcjeAPI.class);
        destinationCall = api.getAccountByEmailQuery(recipientEmail);
        destinationCall.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(
                    Call<Map<String, Object>> call,
                    Response<Map<String, Object>> response
            ) {
                if (call.isCanceled() || isFinishing() || isDestroyed()) return;

                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> account = response.body();
                    Object numberValue = account.get("numerKonta");
                    Object currencyValue = account.get("waluta");

                    if (numberValue != null && currencyValue != null) {
                        String accountNumber = numberValue.toString().replaceAll("\\s+", "");
                        String destinationCurrency = currencyValue.toString().trim().toUpperCase(Locale.ROOT);

                        if (accountNumber.matches("\\d{26}")
                                && selectedSourceAccount.currency.equals(destinationCurrency)) {
                            setSubmitting(false, null);
                            pokazPodsumowanie(recipientEmail, accountNumber, recipientName, amount, title,
                                    selectedSourceAccount.id, selectedSourceAccount.currency);
                            return;
                        }
                    }
                }

                szukajWUzytkownikach(api, recipientEmail, recipientName, amount, title);
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                if (call.isCanceled() || isFinishing() || isDestroyed()) return;
                szukajWUzytkownikach(api, recipientEmail, recipientName, amount, title);
            }
        });
    }

    private void szukajWUzytkownikach(
            TransakcjeAPI api,
            String recipientEmail,
            String recipientName,
            BigDecimal amount,
            String title
    ) {
        api.getAllUsers().enqueue(new Callback<List<UserResponse>>() {
            @Override
            public void onResponse(
                    Call<List<UserResponse>> call,
                    Response<List<UserResponse>> response
            ) {
                if (call.isCanceled() || isFinishing() || isDestroyed()) return;

                if (!response.isSuccessful() || response.body() == null) {
                    setSubmitting(false, null);
                    toast("Nie znaleziono odbiorcy dla e-maila: " + recipientEmail);
                    return;
                }

                UserResponse matchingUser = null;
                for (UserResponse user : response.body()) {
                    if (user != null && user.getEmail() != null
                            && user.getEmail().equalsIgnoreCase(recipientEmail)) {
                        matchingUser = user;
                        break;
                    }
                }

                if (matchingUser == null || matchingUser.getId() == null) {
                    setSubmitting(false, null);
                    toast("Nie znaleziono użytkownika dla e-maila: " + recipientEmail);
                    return;
                }

                long recipientUserId = matchingUser.getId();
                pobierzKontoUzytkownika(api, recipientUserId, recipientEmail, recipientName, amount, title);
            }

            @Override
            public void onFailure(Call<List<UserResponse>> call, Throwable t) {
                if (call.isCanceled() || isFinishing() || isDestroyed()) return;
                setSubmitting(false, null);
                toast("Błąd połączenia podczas wyszukiwania odbiorcy: "
                        + (t.getMessage() == null ? t.getClass().getSimpleName() : t.getMessage()));
            }
        });
    }

    private void pobierzKontoUzytkownika(
            TransakcjeAPI api,
            long recipientUserId,
            String recipientEmail,
            String recipientName,
            BigDecimal amount,
            String title
    ) {
        api.getAccountsForUser(recipientUserId).enqueue(new Callback<List<KontoApiResponse>>() {
            @Override
            public void onResponse(
                    Call<List<KontoApiResponse>> call,
                    Response<List<KontoApiResponse>> response
            ) {
                if (call.isCanceled() || isFinishing() || isDestroyed()) return;
                setSubmitting(false, null);

                if (!response.isSuccessful() || response.body() == null || response.body().isEmpty()) {
                    toast("Użytkownik " + recipientEmail + " nie posiada jeszcze zarejestrowanego konta.");
                    return;
                }

                KontoApiResponse matchingAccount = null;
                for (KontoApiResponse acc : response.body()) {
                    if (acc != null && acc.waluta != null
                            && selectedSourceAccount.currency.equalsIgnoreCase(acc.waluta.trim())) {
                        matchingAccount = acc;
                        break;
                    }
                }

                if (matchingAccount == null || matchingAccount.numerKonta == null) {
                    toast("Użytkownik " + recipientEmail + " nie posiada konta w walucie "
                            + selectedSourceAccount.currency + ".");
                    return;
                }

                String destinationNumber = matchingAccount.numerKonta.replaceAll("\\s+", "");
                if (!destinationNumber.matches("\\d{26}")) {
                    toast("Numer konta odbiorcy w bazie nie ma 26 cyfr.");
                    return;
                }

                pokazPodsumowanie(recipientEmail, destinationNumber, recipientName, amount, title,
                        selectedSourceAccount.id, selectedSourceAccount.currency);
            }

            @Override
            public void onFailure(Call<List<KontoApiResponse>> call, Throwable t) {
                if (call.isCanceled() || isFinishing() || isDestroyed()) return;
                setSubmitting(false, null);
                toast("Błąd połączenia podczas pobierania konta odbiorcy.");
            }
        });
    }

    private String formatAccountNumber(String raw) {
        if (raw == null) return "";
        String clean = raw.replaceAll("\\s+", "");
        if (clean.length() == 26) {
            return clean.substring(0, 2) + " "
                    + clean.substring(2, 6) + " "
                    + clean.substring(6, 10) + " "
                    + clean.substring(10, 14) + " "
                    + clean.substring(14, 18) + " "
                    + clean.substring(18, 22) + " "
                    + clean.substring(22, 26);
        }
        return raw;
    }

    private void pokazPodsumowanie(
            String recipientEmail,
            String destinationNumber,
            String recipientName,
            BigDecimal amount,
            String title,
            long sourceId,
            String currency
    ) {
        Dialog dialog = new Dialog(this, R.style.NotificationDialogTheme);
        View view = getLayoutInflater().inflate(R.layout.dialog_confirm_transfer, null);
        dialog.setContentView(view);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.CENTER);
        }

        View root = view.findViewById(R.id.dialogConfirmRoot);
        if (root != null) {
            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.RECTANGLE);
            shape.setColor(ThemeManager.getSurfaceColor(this));
            shape.setCornerRadius(dp(20));
            root.setBackground(shape);
        }

        TextView txtAmount = view.findViewById(R.id.txtConfirmAmount);
        TextView txtRecipient = view.findViewById(R.id.txtConfirmRecipient);
        TextView txtAccount = view.findViewById(R.id.txtConfirmAccount);
        TextView txtTitle = view.findViewById(R.id.txtConfirmTitle);
        TextView btnCancel = view.findViewById(R.id.btnConfirmCancel);
        TextView btnSubmit = view.findViewById(R.id.btnConfirmSubmit);

        if (txtAmount != null) {
            txtAmount.setText(String.format(Locale.US, "%.2f %s", amount, currency));
            txtAmount.setTextColor(ThemeManager.getAccentColor(this));
        }
        if (txtRecipient != null) {
            String recipient = (recipientName != null && !recipientName.trim().isEmpty())
                    ? recipientName + " (" + recipientEmail + ")" : recipientEmail;
            txtRecipient.setText(recipient);
            txtRecipient.setTextColor(ThemeManager.getTextColor(this));
        }
        if (txtAccount != null) {
            txtAccount.setText(formatAccountNumber(destinationNumber));
            txtAccount.setTextColor(ThemeManager.getSecondaryTextColor(this));
        }
        if (txtTitle != null) {
            txtTitle.setText(title);
            txtTitle.setTextColor(ThemeManager.getTextColor(this));
        }

        if (btnCancel != null) {
            btnCancel.setTextColor(ThemeManager.getTextColor(this));
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }
        if (btnSubmit != null) {
            btnSubmit.setOnClickListener(v -> {
                dialog.dismiss();
                wyslijPrzelew(sourceId, recipientEmail, destinationNumber, recipientName, amount, currency, title);
            });
        }

        dialog.show();
    }

    private void wyslijPrzelew(
            long sourceId,
            String recipientEmail,
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
        transaction.put("email", recipientEmail);
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
                    long txId = System.currentTimeMillis();
                    String formattedTxId = String.valueOf(txId);

                    if (result != null && result.get("id") != null) {
                        Object idObj = result.get("id");
                        if (idObj instanceof Number) {
                            long idLong = ((Number) idObj).longValue();
                            txId = idLong;
                            formattedTxId = String.valueOf(idLong);
                        } else {
                            formattedTxId = String.valueOf(idObj).replaceAll("\\.0+$", "");
                        }
                    }

                    String formattedAmount = String.format(Locale.US, "%.2f", amount.doubleValue());
                    String targetName = (recipientName != null && !recipientName.trim().isEmpty())
                            ? recipientName : recipientEmail;

                    NotificationHelper.sendTransferNotification(
                            TransferActivity.this,
                            uzytkownikId,
                            getIntent().getStringExtra("nick"),
                            "Przelew wysłany",
                            "Wysłałeś " + formattedAmount + " " + currency + " do " + targetName,
                            (int) (txId & 0x7FFFFFFF)
                    );

                    SharedPreferences prefs = getSharedPreferences("notifications_prefs", MODE_PRIVATE);
                    String prefKey = "notified_tx_ids_" + uzytkownikId;
                    java.util.Set<String> notifiedIds = new java.util.HashSet<>(prefs.getStringSet(prefKey, new java.util.HashSet<>()));
                    notifiedIds.add(String.valueOf(txId));
                    prefs.edit().putStringSet(prefKey, notifiedIds).apply();

                    pokazSukcesDialog(formattedTxId, formattedAmount, currency, targetName, () -> finish());
                } else {
                    String backendError = odczytajBlad(response);
                    String normalizedError = backendError == null
                            ? "" : backendError.toLowerCase(Locale.ROOT);
                    if (response.code() == 400 && (
                            normalizedError.contains("saldo")
                                    || normalizedError.contains("środk")
                                    || normalizedError.contains("srodk")
                                    || normalizedError.contains("balance")
                                    || normalizedError.contains("insufficient"))) {
                        if (amountInput != null) {
                            amountInput.setError("Brak wystarczających środków na koncie. Zmniejsz kwotę przelewu.");
                            amountInput.requestFocus();
                        }
                    } else {
                        toast("Nie udało się wykonać przelewu. Sprawdź dane i spróbuj ponownie.");
                    }
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

    private void walidujKwoteWzgledemSalda(boolean focusOnError) {
        if (amountInput == null) return;

        String value = amountInput.getText().toString().trim().replace(',', '.');
        if (value.isEmpty() || selectedSourceAccount == null
                || selectedSourceAccount.balance == null) {
            amountInput.setError(null);
            return;
        }

        try {
            BigDecimal amount = new BigDecimal(value);
            if (amount.compareTo(selectedSourceAccount.balance) > 0) {
                amountInput.setError("Kwota przekracza dostępne saldo: "
                        + selectedSourceAccount.balance.toPlainString() + " "
                        + selectedSourceAccount.currency + ".");
                if (focusOnError) amountInput.requestFocus();
            } else {
                amountInput.setError(null);
            }
        } catch (NumberFormatException ignored) {
            amountInput.setError(null);
        }
    }

    private String odczytajBlad(Response<?> response) {
        if (response != null && response.errorBody() != null) {
            try (ResponseBody errorBody = response.errorBody()) {
                String error = errorBody.string();
                return error == null ? "" : error.trim();
            } catch (IOException ignored) {
            }
        }
        return "";
    }

    private void setSubmitting(boolean value, String label) {
        submitting = value;
        if (submitButton != null) {
            submitButton.setEnabled(!value);
            submitButton.setText(value
                    ? (label == null ? "Przetwarzanie…" : label)
                    : "PRZELEJ");
        }
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void pokazSukcesDialog(
            String txIdFormatted,
            String amountFormatted,
            String currency,
            String recipient,
            Runnable onDone
    ) {
        Dialog dialog = new Dialog(this, R.style.NotificationDialogTheme);
        View view = getLayoutInflater().inflate(R.layout.dialog_transfer_success, null);
        dialog.setContentView(view);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.CENTER);
        }

        View root = view.findViewById(R.id.dialogSuccessRoot);
        if (root != null) {
            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.RECTANGLE);
            shape.setColor(ThemeManager.getSurfaceColor(this));
            shape.setCornerRadius(dp(20));
            root.setBackground(shape);
        }

        TextView iconSuccess = view.findViewById(R.id.iconSuccess);
        if (iconSuccess != null) {
            GradientDrawable iconBg = new GradientDrawable();
            iconBg.setShape(GradientDrawable.OVAL);
            iconBg.setColor(Color.parseColor("#1510B981"));
            iconBg.setStroke(dp(2), Color.parseColor("#10B981"));
            iconSuccess.setBackground(iconBg);
        }

        TextView txtTxId = view.findViewById(R.id.txtSuccessTxId);
        TextView txtAmount = view.findViewById(R.id.txtSuccessAmount);
        TextView txtRecipient = view.findViewById(R.id.txtSuccessRecipient);
        TextView btnDone = view.findViewById(R.id.btnSuccessDone);

        if (txtTxId != null) {
            txtTxId.setText("#" + txIdFormatted);
            txtTxId.setTextColor(ThemeManager.getTextColor(this));
        }
        if (txtAmount != null) {
            txtAmount.setText(amountFormatted + " " + currency);
            txtAmount.setTextColor(ThemeManager.getAccentColor(this));
        }
        if (txtRecipient != null) {
            txtRecipient.setText(recipient);
            txtRecipient.setTextColor(ThemeManager.getTextColor(this));
        }

        if (btnDone != null) {
            btnDone.setOnClickListener(v -> {
                dialog.dismiss();
                if (onDone != null) onDone.run();
            });
        }

        dialog.show();
    }

    @Override
    protected void onDestroy() {
        if (accountsCall != null) accountsCall.cancel();
        if (destinationCall != null) destinationCall.cancel();
        if (transactionCall != null) transactionCall.cancel();
        super.onDestroy();
    }
}