package com.example.bank3.ui.home;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

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

import okhttp3.ResponseBody;

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

        @GET("api/konta/email")
        Call<Map<String, Object>> getAccountByEmailQuery(@Query("email") String email);

        @GET("api/uzytkownicy")
        Call<List<UserResponse>> getAllUsers();

        @POST("api/transakcje")
        Call<Map<String, Object>> createTransaction(@Body Map<String, Object> transaction);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        uzytkownikId = requireActivity().getIntent().getLongExtra("uzytkownikId", -1L);

        View view = inflater.inflate(R.layout.activity_transfer, container, false);

        TextView back = view.findViewById(R.id.transferBack);
        if (back != null) {
            back.setOnClickListener(v -> {
                if (requireActivity() instanceof MainMenuActivity) {
                    ((MainMenuActivity) requireActivity()).selectTab(0);
                } else {
                    requireActivity().finish();
                }
            });
        }

        sourceAccountSpinner = view.findViewById(R.id.sourceAccountSpinner);
        destinationAccountInput = view.findViewById(R.id.destinationAccountInput);
        recipientNameInput = view.findViewById(R.id.recipientNameInput);
        amountInput = view.findViewById(R.id.amountInput);
        currencySpinner = view.findViewById(R.id.currencySpinner);
        titleInput = view.findViewById(R.id.titleInput);
        submitButton = view.findViewById(R.id.submitTransferButton);

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
            int accentColor = ThemeManager.getAccentColor(requireContext());
            submitButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(accentColor));
            submitButton.setOnClickListener(v -> sprawdzOdbiorce());
        }

        int accentColor = ThemeManager.getAccentColor(requireContext());
        com.google.android.material.textfield.TextInputLayout[] layouts = {
                view.findViewById(R.id.layoutDestinationAccount),
                view.findViewById(R.id.layoutRecipientName),
                view.findViewById(R.id.layoutAmount),
                view.findViewById(R.id.layoutTitle)
        };
        for (com.google.android.material.textfield.TextInputLayout layout : layouts) {
            if (layout != null) {
                layout.setBoxStrokeColor(accentColor);
                layout.setHintTextColor(android.content.res.ColorStateList.valueOf(accentColor));
            }
        }

        pobierzKontaUzytkownika();
        return view;
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

                ArrayAdapter<String> adapter = createSpinnerAdapter(requireContext(), labels);
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
                if (call.isCanceled() || getActivity() == null || requireActivity().isFinishing() || requireActivity().isDestroyed()) return;
                pokazBladKontaZrodlowego("Błąd połączenia podczas pobierania Twoich kont: "
                        + (t.getMessage() == null ? t.getClass().getSimpleName() : t.getMessage()));
            }
        });
    }

    private void ustawWaluteZrodla() {
        if (selectedSourceAccount == null || currencySpinner == null) return;
        ArrayAdapter<String> adapter = createSpinnerAdapter(requireContext(), new String[]{selectedSourceAccount.currency});
        currencySpinner.setAdapter(adapter);
        currencySpinner.setEnabled(false);
    }

    private void pokazBladKontaZrodlowego(String message) {
        selectedSourceAccount = null;
        sourceAccounts.clear();
        if (sourceAccountSpinner != null) {
            sourceAccountSpinner.setEnabled(false);
            ArrayAdapter<String> adapter = createSpinnerAdapter(requireContext(), new String[]{"Nie udało się pobrać kont źródłowych"});
            sourceAccountSpinner.setAdapter(adapter);
        }
        toast(message);
    }

    private ArrayAdapter<String> createSpinnerAdapter(Context context, String[] items) {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                context, android.R.layout.simple_spinner_item, items) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                view.setTextColor(ThemeManager.getTextColor(context));
                view.setTextSize(15);
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
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
                requireContext().getSharedPreferences(
                        "demo_card_settings_" + uzytkownikId,
                        Context.MODE_PRIVATE
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
                            .setPositiveButton("Gotowe", (dialog, which) -> {
                                if (requireActivity() instanceof MainMenuActivity) {
                                    ((MainMenuActivity) requireActivity()).selectTab(0);
                                }
                            })
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
        if (getContext() != null) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onDestroyView() {
        if (accountsCall != null) accountsCall.cancel();
        if (destinationCall != null) destinationCall.cancel();
        if (transactionCall != null) transactionCall.cancel();
        super.onDestroyView();
    }
}