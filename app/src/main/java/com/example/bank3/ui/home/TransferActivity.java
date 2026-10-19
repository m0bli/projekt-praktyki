package com.example.bank3.ui.home;

import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.bank3.data.api.KontoAPI;
import com.example.bank3.data.repository.TransakcjaRepository;
import com.example.bank3.data.repository.KontoRepository;
import com.example.bank3.model.KontoResponse;
import com.example.bank3.model.TransakcjaHistoriaResponse;
import com.example.bank3.model.TransakcjaRequest;
import com.example.bank3.network.RetrofitClient;
import com.example.bank3.R;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TransferActivity extends AppCompatActivity {
    private long userId;
    private Spinner accountSpinner;
    private EditText receiverNumber;
    private EditText receiverName;
    private EditText amount;
    private EditText title;
    private TextView status;
    private TextView history;
    private Button sendButton;
    private ProgressBar progress;
    private final List<KontoResponse> accounts = new ArrayList<>();
    private TransakcjaRepository transactions;
    private KontoRepository accountRepository;
    private Call<?> activeCall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        userId = getIntent().getLongExtra("uzytkownikId", -1L);
        transactions = new TransakcjaRepository();
        accountRepository = new KontoRepository();
        buildUi();
        if (userId <= 0) {
            status.setText("Nie udało się ustalić użytkownika. Zaloguj się ponownie.");
            sendButton.setEnabled(false);
        } else {
            loadAccounts();
        }
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(20), dp(22), dp(24));
        root.setBackgroundColor(0xFFF6F8FC);
        scroll.addView(root);

        TextView heading = new TextView(this);
        heading.setText("Nowy przelew");
        heading.setTextSize(26);
        heading.setTextColor(0xFF102A56);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        heading.setPadding(0, 0, 0, dp(18));
        root.addView(heading);

        label(root, "Konto źródłowe");
        accountSpinner = new Spinner(this);
        root.addView(accountSpinner, matchWrap());
        receiverNumber = field(root, "Numer konta odbiorcy (26 cyfr)", InputType.TYPE_CLASS_NUMBER);
        receiverName = field(root, "Imię odbiorcy", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        amount = field(root, "Kwota", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        title = field(root, "Tytuł przelewu", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);

        sendButton = new Button(this);
        sendButton.setText("Wykonaj przelew");
        root.addView(sendButton, matchWrap());
        progress = new ProgressBar(this);
        progress.setVisibility(View.GONE);
        root.addView(progress);
        status = new TextView(this);
        status.setTextColor(0xFF45546C);
        status.setPadding(0, dp(12), 0, dp(8));
        root.addView(status);

        Button historyButton = new Button(this);
        historyButton.setText("Odśwież historię konta");
        root.addView(historyButton, matchWrap());
        history = new TextView(this);
        history.setTextColor(0xFF26364F);
        history.setTextSize(15);
        history.setPadding(0, dp(10), 0, dp(16));
        root.addView(history);

        sendButton.setOnClickListener(v -> sendTransfer());
        historyButton.setOnClickListener(v -> loadHistory());
        accountSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (!accounts.isEmpty()) loadHistory();
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        });
        setContentView(scroll);
    }

    private void label(LinearLayout root, String text) {
        TextView label = new TextView(this);
        label.setText(text);
        label.setTextColor(0xFF33435D);
        label.setTextSize(14);
        label.setPadding(0, dp(10), 0, dp(4));
        root.addView(label);
    }

    private EditText field(LinearLayout root, String hint, int inputType) {
        EditText field = new EditText(this);
        field.setSingleLine(true);
        field.setHint(hint);
        field.setInputType(inputType);
        field.setTextColor(0xFF15243B);
        field.setHintTextColor(0xFF68778E);
        root.addView(field, matchWrap());
        return field;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void loadAccounts() {
        progress.setVisibility(View.VISIBLE);
        Call<List<KontoResponse>> call = accountRepository.getKontaUzytkownika(userId);
        activeCall = call;
        call.enqueue(new Callback<List<KontoResponse>>() {
            @Override public void onResponse(Call<List<KontoResponse>> call, Response<List<KontoResponse>> response) {
                progress.setVisibility(View.GONE);
                if (isFinishing() || isDestroyed()) return;
                if (!response.isSuccessful() || response.body() == null || response.body().isEmpty()) {
                    status.setText(response.code() == 404 ? "Nie znaleziono konta użytkownika." : "Nie udało się pobrać kont. Spróbuj ponownie.");
                    return;
                }
                accounts.clear();
                accounts.addAll(response.body());
                accountSpinner.setAdapter(new ArrayAdapter<>(TransferActivity.this, android.R.layout.simple_spinner_dropdown_item, accounts));
                status.setText("Wybierz konto, uzupełnij dane odbiorcy i potwierdź przelew.");
                loadHistory();
            }
            @Override public void onFailure(Call<List<KontoResponse>> call, Throwable t) {
                progress.setVisibility(View.GONE);
                if (!call.isCanceled() && !isFinishing() && !isDestroyed()) status.setText("Błąd połączenia. Sprawdź serwer i ponów próbę.");
            }
        });
    }

    private void sendTransfer() {
        if (accounts.isEmpty() || accountSpinner.getSelectedItemPosition() < 0) {
            status.setText("Najpierw pobierz konto źródłowe.");
            return;
        }
        String number = receiverNumber.getText().toString().replaceAll("\\s+", "");
        String name = receiverName.getText().toString().trim();
        String amountText = amount.getText().toString().trim().replace(',', '.');
        String transferTitle = title.getText().toString().trim();
        if (!number.matches("\\d{26}")) { receiverNumber.setError("Wpisz 26 cyfr"); return; }
        if (name.isEmpty()) { receiverName.setError("Podaj imię odbiorcy"); return; }
        if (transferTitle.isEmpty() || transferTitle.length() > 140) { title.setError("Tytuł jest wymagany (maks. 140 znaków)"); return; }
        final BigDecimal value;
        try {
            value = new BigDecimal(amountText);
            if (value.compareTo(BigDecimal.ZERO) <= 0 || value.scale() > 2) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            amount.setError("Podaj kwotę większą od zera, maks. 2 miejsca po przecinku");
            return;
        }
        KontoResponse source = accounts.get(accountSpinner.getSelectedItemPosition());
        TransakcjaRequest request = new TransakcjaRequest(userId, source.getId(), number, name, value, source.getWaluta(), transferTitle);
        sendButton.setEnabled(false);
        progress.setVisibility(View.VISIBLE);
        status.setText("Wysyłanie przelewu…");
        Call<com.example.bank3.model.TransakcjaResponse> call = RetrofitClient.getInstance()
                .create(com.example.bank3.data.api.TransakcjaAPI.class).wykonajPrzelew(request);
        activeCall = call;
        call.enqueue(new Callback<com.example.bank3.model.TransakcjaResponse>() {
            @Override public void onResponse(Call<com.example.bank3.model.TransakcjaResponse> call, Response<com.example.bank3.model.TransakcjaResponse> response) {
                progress.setVisibility(View.GONE);
                sendButton.setEnabled(true);
                if (isFinishing() || isDestroyed()) return;
                if (response.isSuccessful() && response.body() != null) {
                    status.setText("Przelew wykonany. Historia i saldo zostały zaktualizowane.");
                    receiverNumber.setText("");
                    receiverName.setText("");
                    amount.setText("");
                    title.setText("");
                    loadAccounts();
                    loadHistory();
                } else {
                    status.setText(errorMessage(response.code()));
                }
            }
            @Override public void onFailure(Call<com.example.bank3.model.TransakcjaResponse> call, Throwable t) {
                progress.setVisibility(View.GONE);
                sendButton.setEnabled(true);
                if (!call.isCanceled() && !isFinishing() && !isDestroyed()) status.setText("Nie udało się połączyć z serwerem. Przelew nie został potwierdzony — sprawdź historię przed ponowieniem.");
            }
        });
    }

    private String errorMessage(int code) {
        if (code == 400) return "Przelew odrzucony. Sprawdź dane, walutę i dostępne środki.";
        if (code == 404) return "Nie znaleziono konta lub odbiorcy.";
        if (code >= 500) return "Serwer chwilowo niedostępny. Spróbuj później.";
        return "Nie udało się wykonać przelewu (HTTP " + code + ").";
    }

    private void loadHistory() {
        if (accounts.isEmpty() || accountSpinner.getSelectedItemPosition() < 0) return;
        long accountId = accounts.get(accountSpinner.getSelectedItemPosition()).getId();
        history.setText("Ładowanie historii…");
        Call<List<TransakcjaHistoriaResponse>> call = transactions.getHistoriaKonta(accountId);
        activeCall = call;
        call.enqueue(new Callback<List<TransakcjaHistoriaResponse>>() {
            @Override public void onResponse(Call<List<TransakcjaHistoriaResponse>> call, Response<List<TransakcjaHistoriaResponse>> response) {
                if (isFinishing() || isDestroyed()) return;
                if (!response.isSuccessful() || response.body() == null) {
                    history.setText("Nie udało się pobrać historii. Dotknij „Odśwież historię konta”, aby spróbować ponownie.");
                    return;
                }
                if (response.body().isEmpty()) { history.setText("Brak transakcji na tym koncie."); return; }
                StringBuilder text = new StringBuilder("Historia konta\n\n");
                NumberFormat format = NumberFormat.getNumberInstance(new Locale("pl", "PL"));
                format.setMinimumFractionDigits(2);
                format.setMaximumFractionDigits(2);
                for (TransakcjaHistoriaResponse item : response.body()) {
                    boolean incoming = "WPLYW".equals(item.getKierunek());
                    text.append(incoming ? "WPŁYW  +" : "WYDATEK  −")
                            .append(format.format(item.getKwota() == null ? BigDecimal.ZERO : item.getKwota()))
                            .append(" ").append(item.getWaluta() == null ? "" : item.getWaluta()).append("\n")
                            .append(item.getTytul() == null ? "Przelew" : item.getTytul()).append("\n")
                            .append(incoming ? "Nadawca: " : "Odbiorca: ")
                            .append(item.getNumerKontaKontrahenta() == null ? "—" : item.getNumerKontaKontrahenta()).append("\n")
                            .append(item.getDataOperacji() == null ? "" : item.getDataOperacji().replace('T', ' ')).append("\n\n");
                }
                history.setText(text.toString());
            }
            @Override public void onFailure(Call<List<TransakcjaHistoriaResponse>> call, Throwable t) {
                if (!call.isCanceled() && !isFinishing() && !isDestroyed()) history.setText("Błąd połączenia podczas pobierania historii. Spróbuj ponownie.");
            }
        });
    }

    @Override protected void onDestroy() {
        if (activeCall != null && !activeCall.isCanceled()) activeCall.cancel();
        super.onDestroy();
    }
}
