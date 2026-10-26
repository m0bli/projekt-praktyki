package com.example.bank3.ui.home;

import static android.content.Context.MODE_PRIVATE;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import androidx.fragment.app.Fragment;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
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






import com.example.bank3.R;
import com.example.bank3.model.UserResponse;
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
import retrofit2.http.Query;

public class TransferFragment extends Fragment {

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
        final BigDecimal balance;
        final String currency;

        SourceAccount(long id, String number, BigDecimal balance, String currency) {
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

        @GET("api/konta/email/{email}")
        Call<Map<String, Object>> getAccountByEmailPath(@Path("email") String email);

        @GET("api/konta/email")
        Call<Map<String, Object>> getAccountByEmailQuery(@Query("email") String email);

        @GET("api/uzytkownicy")
        Call<List<UserResponse>> getAllUsers();

        @POST("api/transakcje")
        Call<Map<String, Object>> createTransaction(@Body Map<String, Object> transaction);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        applyColors();
        uzytkownikId = requireActivity().getIntent().getLongExtra("uzytkownikId", -1L);

        ScrollView scrollView = new ScrollView(requireContext());
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(backgroundColor);

        LinearLayout page = new LinearLayout(requireContext());
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(22), dp(18), dp(22), dp(28));
        scrollView.addView(page);

        TextView back = new TextView(requireContext());
        back.setText("‹  Wróć");
        back.setTextSize(16);
        back.setTextColor(accentColor);
        back.setPadding(0, dp(4), 0, dp(18));
        back.setOnClickListener(v -> ((MainMenuActivity) requireActivity()).selectTab(0));
        page.addView(back);

        TextView heading = new TextView(requireContext());
        heading.setText("Nowy przelew");
        heading.setTextSize(28);
        heading.setTypeface(null, android.graphics.Typeface.BOLD);
        heading.setTextColor(textColor);
        page.addView(heading);

        TextView subtitle = new TextView(requireContext());
        subtitle.setText("Wybierz swoje konto i podaj dane odbiorcy.");
        subtitle.setTextSize(14);
        subtitle.setTextColor(secondaryTextColor);
        LinearLayout.LayoutParams subtitleParams = params(-1, -2);
        subtitleParams.topMargin = dp(7);
        subtitleParams.bottomMargin = dp(22);
        page.addView(subtitle, subtitleParams);

        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(20), dp(18), dp(20));
        card.setBackground(roundRect(surfaceColor, dp(22)));
        page.addView(card, params(-1, -2));

        addLabel(card, "Konto źródłowe");
        sourceAccountSpinner = addSpinner(card, new String[]{"Pobieranie kont..."});
        sourceAccountSpinner.setEnabled(false);
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

        destinationAccountInput = addInput(
                card, "E-mail odbiorcy", "E-mail odbiorcy",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        );
        recipientNameInput = addInput(
                card, "Imię odbiorcy", "Imię odbiorcy",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS
        );
        amountInput = addInput(
                card, "Kwota przelewu", "0,00",
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL
        );
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

        addLabel(card, "Waluta konta źródłowego");
        currencySpinner = addSpinner(card, new String[]{"Pobieranie waluty..."});
        currencySpinner.setEnabled(false);

        titleInput = addInput(
                card, "Tytuł przelewu", "Tytuł przelewu",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        );

        TextView hint = new TextView(requireContext());
        hint.setText("Odbiorca musi mieć konto w tej samej walucie. Backend wymaga 26-cyfrowego numeru konta.");
        hint.setTextSize(12);
        hint.setTextColor(secondaryTextColor);
        LinearLayout.LayoutParams hintParams = params(-1, -2);
        hintParams.topMargin = dp(14);
        card.addView(hint, hintParams);

        submitButton = new Button(requireContext());
        submitButton.setText("PRZELEJ");
        submitButton.setAllCaps(false);
        submitButton.setTextColor(Color.WHITE);
        submitButton.setTextSize(15);
        submitButton.setBackground(roundRect(accentColor, dp(16)));
        LinearLayout.LayoutParams buttonParams = params(-1, dp(54));
        buttonParams.topMargin = dp(20);
        page.addView(submitButton, buttonParams);
        submitButton.setOnClickListener(v -> sprawdzOdbiorce());

        pobierzKontaUzytkownika();
        return scrollView;
    }

    private void applyColors() {
        backgroundColor = ThemeManager.getBackgroundColor(requireContext());
        surfaceColor = ThemeManager.getSurfaceColor(requireContext());
        textColor = ThemeManager.getTextColor(requireContext());
        secondaryTextColor = ThemeManager.getSecondaryTextColor(requireContext());
        accentColor = ThemeManager.getAccentColor(requireContext());
    }

    private EditText addInput(LinearLayout parent, String label, String placeholder, int inputType) {
        addLabel(parent, label);
        EditText input = new EditText(requireContext());
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
        TextView label = new TextView(requireContext());
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
        Spinner spinner = new Spinner(requireContext());
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                requireContext(), android.R.layout.simple_spinner_item, values) {
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
                if (call.isCanceled() || getActivity() == null || requireActivity().isFinishing() || requireActivity().isDestroyed()) return;

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
                    if (accountId > 0 && !number.isEmpty() && currency.matches("[A-Z]{3}")) {
                        sourceAccounts.add(new SourceAccount(accountId, number, account.saldo, currency));
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
                        requireContext(), android.R.layout.simple_spinner_item, labels);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                sourceAccountSpinner.setAdapter(adapter);
                sourceAccountSpinner.setEnabled(sourceAccounts.size() > 1);
                sourceAccountSpinner.setSelection(0);
                selectedSourceAccount = sourceAccounts.get(0);
                ustawWaluteZrodla();
            }

            @Override
            public void onFailure(Call<List<KontoApiResponse>> call, Throwable t) {
                if (call.isCanceled() || getActivity() == null || requireActivity().isFinishing() || requireActivity().isDestroyed()) return;
                pokazBladKontaZrodlowego("Błąd połączenia podczas pobierania Twoich kont: "
                        + (t.getMessage() == null ? t.getClass().getSimpleName() : t.getMessage()));
            }
        });
    }

    private void ustawWaluteZrodla() {
        if (selectedSourceAccount == null) return;
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
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
                requireContext(),
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

        android.content.SharedPreferences cardPrefs =
                requireContext().getSharedPreferences(
                        "demo_card_settings_" + uzytkownikId,
                        android.content.Context.MODE_PRIVATE
                );

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

        // Sprawdź dzienny limit płatności z ustawień Karty
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
                if (call.isCanceled() || getActivity() == null || requireActivity().isFinishing() || requireActivity().isDestroyed()) return;

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

                // Fallback: szukaj w użytkownikach i ich kontach
                szukajWUzytkownikach(api, recipientEmail, recipientName, amount, title);
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                if (call.isCanceled() || getActivity() == null || requireActivity().isFinishing() || requireActivity().isDestroyed()) return;
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
                if (call.isCanceled() || getActivity() == null || requireActivity().isFinishing() || requireActivity().isDestroyed()) return;

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
                if (call.isCanceled() || getActivity() == null || requireActivity().isFinishing() || requireActivity().isDestroyed()) return;
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
                if (call.isCanceled() || getActivity() == null || requireActivity().isFinishing() || requireActivity().isDestroyed()) return;
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
                if (call.isCanceled() || getActivity() == null || requireActivity().isFinishing() || requireActivity().isDestroyed()) return;
                setSubmitting(false, null);
                toast("Błąd połączenia podczas pobierania konta odbiorcy.");
            }
        });
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
        String summary = "Konto źródłowe ID: " + sourceId
                + "\nOdbiorca: " + recipientName
                + "\nE-mail odbiorcy: " + recipientEmail
                + "\nNumer konta odbiorcy: " + destinationNumber
                + "\nKwota: " + String.format(Locale.getDefault(), "%.2f %s", amount, currency)
                + "\nTyp: przelew"
                + "\nTytuł: " + title;

        new AlertDialog.Builder(requireContext())
                .setTitle("Potwierdź przelew")
                .setMessage(summary)
                .setNegativeButton("Popraw", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("Wyślij", (dialog, which) ->
                        wyslijPrzelew(sourceId, recipientEmail, destinationNumber, recipientName, amount, currency, title))
                .show();
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
                if (call.isCanceled() || getActivity() == null || requireActivity().isFinishing() || requireActivity().isDestroyed()) return;
                setSubmitting(false, null);

                if (response.isSuccessful()) {
                    Map<String, Object> result = response.body();
                    String message = "Przelew został wykonany.";
                    if (result != null && result.get("id") != null) {
                        message += "\nNumer transakcji: " + result.get("id");
                    }
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Przelew wykonany")
                            .setMessage(message)
                            .setPositiveButton("Gotowe", (dialog, which) -> ((MainMenuActivity) requireActivity()).selectTab(0))
                            .show();
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
                        amountInput.setError("Brak wystarczających środków na koncie. Zmniejsz kwotę przelewu.");
                        amountInput.requestFocus();
                    } else {
                        toast("Nie udało się wykonać przelewu. Sprawdź dane i spróbuj ponownie.");
                    }
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                if (call.isCanceled() || getActivity() == null || requireActivity().isFinishing() || requireActivity().isDestroyed()) return;
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
            // Pole jest w trakcie edycji; pełna walidacja nastąpi po zatwierdzeniu.
            amountInput.setError(null);
        }
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
                ? (label == null ? "Przetwarzanie..." : label)
                : "PRZELEJ");
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
        return (int) (value * requireContext().getResources().getDisplayMetrics().density + 0.5f);
    }

    private void toast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDestroy() {
        if (accountsCall != null) accountsCall.cancel();
        if (destinationCall != null) destinationCall.cancel();
        if (transactionCall != null) transactionCall.cancel();
        super.onDestroy();
    }
}
