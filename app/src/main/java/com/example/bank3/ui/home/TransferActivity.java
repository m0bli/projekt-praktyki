package com.example.bank3.ui.home;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.graphics.Typeface;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowInsetsControllerCompat;

import com.example.bank3.data.repository.KontoRepository;
import com.example.bank3.data.repository.TransakcjaRepository;
import com.example.bank3.model.KontoResponse;
import com.example.bank3.model.TransakcjaHistoriaResponse;
import com.example.bank3.model.TransakcjaRequest;
import com.example.bank3.model.TransakcjaResponse;
import com.example.bank3.ui.theme.ThemeManager;

import java.io.InputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TransferActivity extends AppCompatActivity
        implements ThemeManager.ThemeChangeListener {

    private long userId = -1L;
    private LinearLayout root;
    private LinearLayout accountCard;
    private LinearLayout formCard;
    private LinearLayout historyCard;
    private LinearLayout historyList;
    private Spinner accountSpinner;
    private EditText receiverNumber;
    private EditText receiverName;
    private EditText amountInput;
    private EditText titleInput;
    private TextView status;
    private TextView balance;
    private TextView historyState;
    private TextView accountState;
    private Button sendButton;
    private Button retryAccountsButton;
    private Button refreshHistoryButton;
    private ProgressBar progress;
    private final List<KontoResponse> accounts = new ArrayList<>();
    private TransakcjaRepository transactions;
    private KontoRepository accountRepository;
    private Call<?> accountsCall;
    private Call<?> historyCall;
    private Call<?> transferCall;
    private boolean accountSpinnerReady;
    private boolean transferInProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        userId = getIntent().getLongExtra("uzytkownikId", -1L);
        transactions = new TransakcjaRepository();
        accountRepository = new KontoRepository();
        buildUi();
        updateSystemBars();
        if (userId <= 0) {
            setStatus("Nie można ustalić użytkownika. Wróć do logowania i spróbuj ponownie.", true);
            sendButton.setEnabled(false);
            accountState.setText("Brak aktywnej sesji");
        } else {
            loadAccounts();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        ThemeManager.registerThemeChangeListener(this);
    }

    @Override
    protected void onStop() {
        ThemeManager.unregisterThemeChangeListener(this);
        super.onStop();
    }

    @Override
    public void onThemeChanged() {
        if (root == null) return;
        applyThemeColors(root);
        updateSystemBars();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(12), dp(20), dp(28));
        root.setTag("screen");
        scroll.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0, dp(4), 0, dp(18));
        TextView back = new TextView(this);
        back.setText("‹");
        back.setTextSize(34);
        back.setGravity(Gravity.CENTER);
        back.setContentDescription("Wróć");
        back.setTag("surface");
        back.setBackground(round(ThemeManager.getSurfaceColor(this), 16));
        header.addView(back, new LinearLayout.LayoutParams(dp(48), dp(48)));
        back.setOnClickListener(v -> finish());

        LinearLayout headingBox = new LinearLayout(this);
        headingBox.setOrientation(LinearLayout.VERTICAL);
        headingBox.setPadding(dp(14), 0, 0, 0);
        TextView heading = text("Przelewy", 25, true, "text");
        TextView subtitle = text("Wyślij środki i sprawdź historię", 13, false, "secondary");
        headingBox.addView(heading);
        headingBox.addView(subtitle);
        header.addView(headingBox, new LinearLayout.LayoutParams(0, -2, 1));
        root.addView(header);

        accountCard = card();
        TextView accountLabel = text("KONTO ŹRÓDŁOWE", 11, true, "secondary");
        accountCard.addView(accountLabel);
        accountSpinner = new Spinner(this);
        accountSpinner.setTag("surface");
        LinearLayout.LayoutParams spinnerParams = matchWrap();
        spinnerParams.topMargin = dp(4);
        accountCard.addView(accountSpinner, spinnerParams);
        balance = text("Saldo: —", 22, true, "text");
        LinearLayout.LayoutParams balanceParams = matchWrap();
        balanceParams.topMargin = dp(8);
        accountCard.addView(balance, balanceParams);
        accountState = text("Pobieranie kont…", 13, false, "secondary");
        LinearLayout.LayoutParams stateParams = matchWrap();
        stateParams.topMargin = dp(4);
        accountCard.addView(accountState, stateParams);
        retryAccountsButton = secondaryButton("Ponów pobieranie kont");
        accountCard.addView(retryAccountsButton, matchWrap());
        retryAccountsButton.setVisibility(View.GONE);
        retryAccountsButton.setOnClickListener(v -> loadAccounts());
        root.addView(accountCard, withBottomMargin(16));

        formCard = card();
        formCard.addView(text("Nowy przelew", 19, true, "text"));
        TextView helper = text("Sprawdź numer rachunku i dane odbiorcy przed wysłaniem.", 13, false, "secondary");
        LinearLayout.LayoutParams helperParams = matchWrap();
        helperParams.topMargin = dp(5);
        helperParams.bottomMargin = dp(12);
        formCard.addView(helper, helperParams);

        receiverNumber = field("Numer rachunku odbiorcy (26 cyfr)", InputType.TYPE_CLASS_NUMBER);
        receiverNumber.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(32)});
        addField(formCard, "Numer rachunku", receiverNumber);
        receiverName = field("Imię odbiorcy", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        addField(formCard, "Imię odbiorcy", receiverName);
        amountInput = field("0,00", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
        addField(formCard, "Kwota", amountInput);
        titleInput = field("Za co jest przelew?", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        titleInput.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(140)});
        addField(formCard, "Tytuł przelewu", titleInput);

        status = text("Wybierz konto i uzupełnij dane przelewu.", 13, false, "secondary");
        LinearLayout.LayoutParams statusParams = matchWrap();
        statusParams.topMargin = dp(8);
        statusParams.bottomMargin = dp(6);
        formCard.addView(status, statusParams);

        sendButton = new Button(this);
        sendButton.setText("Sprawdź i wykonaj przelew");
        sendButton.setAllCaps(false);
        formCard.addView(sendButton, matchWrap());
        sendButton.setOnClickListener(v -> sendTransfer());
        root.addView(formCard, withBottomMargin(16));

        progress = new ProgressBar(this);
        progress.setIndeterminate(true);
        progress.setVisibility(View.GONE);
        progress.setContentDescription("Trwa komunikacja z serwerem");
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(dp(36), dp(36));
        progressParams.gravity = Gravity.CENTER_HORIZONTAL;
        progressParams.bottomMargin = dp(12);
        root.addView(progress, progressParams);

        historyCard = card();
        LinearLayout historyHeader = new LinearLayout(this);
        historyHeader.setGravity(Gravity.CENTER_VERTICAL);
        TextView historyTitle = text("Historia konta", 19, true, "text");
        historyHeader.addView(historyTitle, new LinearLayout.LayoutParams(0, -2, 1));
        refreshHistoryButton = secondaryButton("Odśwież");
        historyHeader.addView(refreshHistoryButton, new LinearLayout.LayoutParams(-2, -2));
        refreshHistoryButton.setOnClickListener(v -> loadHistory());
        historyCard.addView(historyHeader);
        historyState = text("Wybierz konto, aby zobaczyć operacje.", 13, false, "secondary");
        LinearLayout.LayoutParams hs = matchWrap();
        hs.topMargin = dp(10);
        historyCard.addView(historyState, hs);
        historyList = new LinearLayout(this);
        historyList.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams listParams = matchWrap();
        listParams.topMargin = dp(8);
        historyCard.addView(historyList, listParams);
        root.addView(historyCard);

        accountSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (accountSpinnerReady && position >= 0 && position < accounts.size()) {
                    updateSelectedBalance();
                    loadHistory();
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {
                balance.setText("Saldo: —");
            }
        });
        applyThemeColors(root);
        setContentView(scroll);
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setTag("surface");
        card.setBackground(round(ThemeManager.getSurfaceColor(this), 20));
        card.setElevation(dp(2));
        return card;
    }

    private TextView text(String value, int size, boolean bold, String tag) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTag(tag);
        view.setTextColor(colorFor(tag));
        if (bold) view.setTypeface(null, Typeface.BOLD);
        return view;
    }

    private EditText field(String hint, int inputType) {
        EditText edit = new EditText(this);
        edit.setSingleLine(true);
        edit.setHint(hint);
        edit.setInputType(inputType);
        edit.setTextSize(15);
        edit.setPadding(dp(12), dp(10), dp(12), dp(10));
        edit.setTag("field");
        edit.setTextColor(ThemeManager.getTextColor(this));
        edit.setHintTextColor(ThemeManager.getSecondaryTextColor(this));
        edit.setBackground(round(ThemeManager.getBackgroundColor(this), 12));
        return edit;
    }

    private void addField(LinearLayout container, String label, EditText edit) {
        TextView caption = text(label, 13, true, "text");
        LinearLayout.LayoutParams cp = matchWrap();
        cp.topMargin = dp(8);
        container.addView(caption, cp);
        LinearLayout.LayoutParams ep = matchWrap();
        ep.topMargin = dp(4);
        ep.bottomMargin = dp(4);
        container.addView(edit, ep);
    }

    private Button secondaryButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(13);
        button.setTag("secondaryButton");
        return button;
    }

    private void applyThemeColors(View view) {
        if (view == null) return;
        Object tagObject = view.getTag();
        String tag = tagObject instanceof String ? (String) tagObject : "";
        if ("screen".equals(tag)) {
            Uri imageUri = ThemeManager.getBackgroundImage(this);
            boolean usedImage = false;
            if (imageUri != null) {
                try (InputStream input = getContentResolver().openInputStream(imageUri)) {
                    if (input != null) {
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inSampleSize = 3;
                        Bitmap bitmap = BitmapFactory.decodeStream(input, null, options);
                        if (bitmap != null) {
                            view.setBackground(new BitmapDrawable(getResources(), bitmap));
                            usedImage = true;
                        }
                    }
                } catch (Exception ignored) {
                    // If the saved image is no longer available, use the selected theme background.
                }
            }
            if (!usedImage) view.setBackgroundColor(ThemeManager.getBackgroundColor(this));
        } else if ("surface".equals(tag)) {
            view.setBackground(round(ThemeManager.getSurfaceColor(this), 18));
            if (view instanceof TextView) ((TextView) view).setTextColor(ThemeManager.getTextColor(this));
        } else if ("text".equals(tag)) {
            if (view instanceof TextView) ((TextView) view).setTextColor(ThemeManager.getTextColor(this));
        } else if ("secondary".equals(tag)) {
            if (view instanceof TextView) ((TextView) view).setTextColor(ThemeManager.getSecondaryTextColor(this));
        } else if ("field".equals(tag)) {
            EditText edit = (EditText) view;
            edit.setTextColor(ThemeManager.getTextColor(this));
            edit.setHintTextColor(ThemeManager.getSecondaryTextColor(this));
            edit.setBackground(round(ThemeManager.getBackgroundColor(this), 12));
        } else if ("secondaryButton".equals(tag) && view instanceof Button) {
            Button button = (Button) view;
            button.setTextColor(ThemeManager.getAccentColor(this));
            button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(ThemeManager.getAccentSoftColor(this)));
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) applyThemeColors(group.getChildAt(i));
        }
        if (view == sendButton && sendButton != null) {
            sendButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(ThemeManager.getAccentColor(this)));
            sendButton.setTextColor(ThemeManager.isBlack(this) ? 0xFF101827 : 0xFFFFFFFF);
        }
    }

    private int colorFor(String tag) {
        if ("secondary".equals(tag)) return ThemeManager.getSecondaryTextColor(this);
        if ("accent".equals(tag)) return ThemeManager.getAccentColor(this);
        return ThemeManager.getTextColor(this);
    }

    private void updateSystemBars() {
        int background = ThemeManager.getBackgroundColor(this);
        getWindow().setStatusBarColor(background);
        getWindow().setNavigationBarColor(background);
        WindowInsetsControllerCompat bars = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        bars.setAppearanceLightStatusBars(!ThemeManager.isBlack(this));
        bars.setAppearanceLightNavigationBars(!ThemeManager.isBlack(this));
    }

    private void loadAccounts() {
        if (userId <= 0 || transferInProgress) return;
        if (accountsCall != null) accountsCall.cancel();
        retryAccountsButton.setVisibility(View.GONE);
        accountState.setText("Pobieranie kont…");
        setBusy(true);
        Call<List<KontoResponse>> call = accountRepository.getKontaUzytkownika(userId);
        accountsCall = call;
        call.enqueue(new Callback<List<KontoResponse>>() {
            @Override public void onResponse(Call<List<KontoResponse>> call, Response<List<KontoResponse>> response) {
                if (isFinishing() || isDestroyed() || call.isCanceled()) return;
                setBusy(false);
                if (!response.isSuccessful() || response.body() == null) {
                    accounts.clear();
                    accountSpinnerReady = false;
                    accountState.setText(response.code() == 404
                            ? "Nie znaleziono kont dla tego użytkownika."
                            : "Nie udało się pobrać kont (HTTP " + response.code() + ").");
                    retryAccountsButton.setVisibility(View.VISIBLE);
                    sendButton.setEnabled(false);
                    return;
                }
                long previouslySelected = getSelectedAccountId();
                accounts.clear();
                accounts.addAll(response.body());
                if (accounts.isEmpty()) {
                    accountState.setText("Nie masz jeszcze konta, z którego można wykonać przelew.");
                    accountSpinner.setAdapter(null);
                    balance.setText("Saldo: —");
                    historyList.removeAllViews();
                    historyState.setText("Brak konta do wyświetlenia historii.");
                    sendButton.setEnabled(false);
                    return;
                }
                ArrayAdapter<KontoResponse> adapter = new ArrayAdapter<KontoResponse>(
                        TransferActivity.this, android.R.layout.simple_spinner_item, accounts) {
                    @Override public View getView(int position, View convertView, ViewGroup parent) {
                        TextView item = (TextView) super.getView(position, convertView, parent);
                        item.setTextColor(ThemeManager.getTextColor(TransferActivity.this));
                        item.setTextSize(15);
                        return item;
                    }
                    @Override public View getDropDownView(int position, View convertView, ViewGroup parent) {
                        TextView item = (TextView) super.getDropDownView(position, convertView, parent);
                        item.setTextColor(ThemeManager.getTextColor(TransferActivity.this));
                        item.setBackgroundColor(ThemeManager.getSurfaceColor(TransferActivity.this));
                        item.setPadding(dp(12), dp(12), dp(12), dp(12));
                        return item;
                    }
                };
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                accountSpinnerReady = false;
                accountSpinner.setAdapter(adapter);
                int selectedIndex = 0;
                for (int i = 0; i < accounts.size(); i++) {
                    if (accounts.get(i).getId() != null && accounts.get(i).getId() == previouslySelected) {
                        selectedIndex = i;
                        break;
                    }
                }
                accountSpinner.setSelection(selectedIndex, false);
                accountSpinnerReady = true;
                accountState.setText("Konto gotowe do przelewu");
                retryAccountsButton.setVisibility(View.GONE);
                sendButton.setEnabled(true);
                updateSelectedBalance();
                loadHistory();
            }

            @Override public void onFailure(Call<List<KontoResponse>> call, Throwable error) {
                if (call.isCanceled() || isFinishing() || isDestroyed()) return;
                setBusy(false);
                accountState.setText("Brak połączenia z serwerem. Sprawdź adres API i spróbuj ponownie.");
                retryAccountsButton.setVisibility(View.VISIBLE);
                sendButton.setEnabled(false);
            }
        });
    }

    private long getSelectedAccountId() {
        int position = accountSpinner == null ? -1 : accountSpinner.getSelectedItemPosition();
        if (position >= 0 && position < accounts.size() && accounts.get(position).getId() != null) {
            return accounts.get(position).getId();
        }
        return -1L;
    }

    private void updateSelectedBalance() {
        int position = accountSpinner.getSelectedItemPosition();
        if (position < 0 || position >= accounts.size()) {
            balance.setText("Saldo: —");
            return;
        }
        KontoResponse account = accounts.get(position);
        NumberFormat money = NumberFormat.getNumberInstance(new Locale("pl", "PL"));
        money.setMinimumFractionDigits(2);
        money.setMaximumFractionDigits(2);
        balance.setText("Saldo: " + money.format(account.getSaldo() == null ? BigDecimal.ZERO : account.getSaldo())
                + " " + (account.getWaluta() == null ? "" : account.getWaluta()));
    }

    private void sendTransfer() {
        if (transferInProgress) return;
        if (accounts.isEmpty() || accountSpinner.getSelectedItemPosition() < 0) {
            setStatus("Najpierw pobierz konto źródłowe.", true);
            return;
        }
        String number = receiverNumber.getText().toString().replaceAll("\\s+", "");
        String name = receiverName.getText().toString().trim();
        String amountText = amountInput.getText().toString().trim().replace(',', '.');
        String transferTitle = titleInput.getText().toString().trim();

        if (!number.matches("\\d{26}")) {
            receiverNumber.setError("Wpisz numer rachunku składający się z 26 cyfr");
            receiverNumber.requestFocus();
            return;
        }
        if (name.isEmpty()) {
            receiverName.setError("Podaj imię odbiorcy");
            receiverName.requestFocus();
            return;
        }
        if (transferTitle.isEmpty() || transferTitle.length() > 140) {
            titleInput.setError("Tytuł jest wymagany (maks. 140 znaków)");
            titleInput.requestFocus();
            return;
        }
        final BigDecimal value;
        try {
            if (amountText.isEmpty()) throw new NumberFormatException();
            value = new BigDecimal(amountText);
            if (value.compareTo(BigDecimal.ZERO) <= 0 || value.scale() > 2) throw new NumberFormatException();
        } catch (NumberFormatException error) {
            amountInput.setError("Podaj kwotę większą od zera, z maksymalnie 2 miejscami po przecinku");
            amountInput.requestFocus();
            return;
        }

        KontoResponse source = accounts.get(accountSpinner.getSelectedItemPosition());
        if (source.getId() == null || source.getWaluta() == null || source.getWaluta().isEmpty()) {
            setStatus("Dane konta są niekompletne. Odśwież listę kont.", true);
            return;
        }
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Potwierdź przelew")
                .setMessage("Kwota: " + NumberFormat.getNumberInstance(new Locale("pl", "PL")).format(value)
                        + " " + source.getWaluta() + "\nOdbiorca: " + name
                        + "\nRachunek: " + number + "\nTytuł: " + transferTitle
                        + "\n\nCzy na pewno wykonać przelew?")
                .setNegativeButton("Wróć", null)
                .setPositiveButton("Wykonaj przelew", (dialog, which) -> submitTransfer(source, number, name, value, transferTitle))
                .show();
    }

    private void submitTransfer(KontoResponse source, String number, String name, BigDecimal value, String transferTitle) {
        if (transferInProgress) return;
        hideKeyboard();
        TransakcjaRequest request = new TransakcjaRequest(
                userId, source.getId(), number, name, value, source.getWaluta(), transferTitle);
        transferInProgress = true;
        sendButton.setEnabled(false);
        setBusy(true);
        setStatus("Wysyłanie przelewu… nie zamykaj aplikacji.", false);
        Call<TransakcjaResponse> call = transactions.wykonajPrzelew(request);
        transferCall = call;
        call.enqueue(new Callback<TransakcjaResponse>() {
            @Override public void onResponse(Call<TransakcjaResponse> call, Response<TransakcjaResponse> response) {
                if (isFinishing() || isDestroyed() || call.isCanceled()) return;
                transferInProgress = false;
                setBusy(false);
                sendButton.setEnabled(!accounts.isEmpty());
                if (response.isSuccessful() && response.body() != null) {
                    setStatus("Przelew został potwierdzony przez serwer.", false);
                    receiverNumber.setText("");
                    receiverName.setText("");
                    amountInput.setText("");
                    titleInput.setText("");
                    loadAccounts();
                } else {
                    setStatus(errorMessage(response.code()), true);
                }
            }

            @Override public void onFailure(Call<TransakcjaResponse> call, Throwable error) {
                if (call.isCanceled() || isFinishing() || isDestroyed()) return;
                transferInProgress = false;
                setBusy(false);
                sendButton.setEnabled(!accounts.isEmpty());
                setStatus("Nie otrzymano potwierdzenia serwera. Nie wysyłaj ponownie w ciemno — odśwież historię, aby sprawdzić, czy przelew został zapisany.", true);
                loadHistory();
            }
        });
    }

    private String errorMessage(int code) {
        if (code == 400) return "Przelew odrzucony. Sprawdź dane odbiorcy, walutę i dostępne środki.";
        if (code == 404) return "Nie znaleziono konta źródłowego lub rachunku odbiorcy.";
        if (code == 409) return "Operacja jest w konflikcie z aktualnym stanem konta. Odśwież dane.";
        if (code >= 500) return "Serwer zgłosił błąd. Sprawdź historię przed ponowieniem operacji.";
        return "Nie udało się wykonać przelewu (HTTP " + code + ").";
    }

    private void loadHistory() {
        long accountId = getSelectedAccountId();
        if (accountId <= 0) return;
        if (historyCall != null) historyCall.cancel();
        historyState.setText("Pobieranie historii…");
        historyList.removeAllViews();
        Call<List<TransakcjaHistoriaResponse>> call = transactions.getHistoriaKonta(accountId);
        historyCall = call;
        call.enqueue(new Callback<List<TransakcjaHistoriaResponse>>() {
            @Override public void onResponse(Call<List<TransakcjaHistoriaResponse>> call, Response<List<TransakcjaHistoriaResponse>> response) {
                if (isFinishing() || isDestroyed() || call.isCanceled()) return;
                if (accountId != getSelectedAccountId()) return;
                if (!response.isSuccessful() || response.body() == null) {
                    historyState.setText(response.code() == 404
                            ? "Nie znaleziono wybranego konta."
                            : "Nie udało się pobrać historii (HTTP " + response.code() + ").");
                    refreshHistoryButton.setText("Spróbuj ponownie");
                    return;
                }
                refreshHistoryButton.setText("Odśwież");
                List<TransakcjaHistoriaResponse> items = response.body();
                historyList.removeAllViews();
                if (items.isEmpty()) {
                    historyState.setText("Brak transakcji na tym koncie. Wykonane przelewy pojawią się tutaj.");
                    return;
                }
                historyState.setText(items.size() + (items.size() == 1 ? " operacja" : " operacje"));
                for (int i = 0; i < items.size(); i++) {
                    addHistoryItem(items.get(i));
                    if (i < items.size() - 1) {
                        View divider = new View(TransferActivity.this);
                        divider.setBackgroundColor(ThemeManager.getAccentSoftColor(TransferActivity.this));
                        LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(-1, dp(1));
                        dp.topMargin = dp(10);
                        dp.bottomMargin = dp(10);
                        historyList.addView(divider, dp);
                    }
                }
            }

            @Override public void onFailure(Call<List<TransakcjaHistoriaResponse>> call, Throwable error) {
                if (call.isCanceled() || isFinishing() || isDestroyed()) return;
                historyState.setText("Brak połączenia podczas pobierania historii. Użyj „Spróbuj ponownie”.");
                refreshHistoryButton.setText("Spróbuj ponownie");
            }
        });
    }

    private void addHistoryItem(TransakcjaHistoriaResponse item) {
        boolean incoming = "WPLYW".equalsIgnoreCase(item.getKierunek());
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView marker = text(incoming ? "↓" : "↑", 22, true, incoming ? "positive" : "negative");
        marker.setGravity(Gravity.CENTER);
        marker.setBackground(round(ThemeManager.getAccentSoftColor(this), 14));
        row.addView(marker, new LinearLayout.LayoutParams(dp(44), dp(44)));

        LinearLayout details = new LinearLayout(this);
        details.setOrientation(LinearLayout.VERTICAL);
        details.setPadding(dp(12), 0, dp(8), 0);
        TextView title = text(nonEmpty(item.getTytul(), "Przelew"), 14, true, "text");
        TextView counterparty = text((incoming ? "Nadawca: " : "Odbiorca: ")
                + nonEmpty(item.getNumerKontaKontrahenta(), "brak danych"), 12, false, "secondary");
        TextView date = text(formatDate(item.getDataOperacji()), 11, false, "secondary");
        details.addView(title);
        LinearLayout.LayoutParams cp = matchWrap();
        cp.topMargin = dp(3);
        details.addView(counterparty, cp);
        LinearLayout.LayoutParams dateParams = matchWrap();
        dateParams.topMargin = dp(3);
        details.addView(date, dateParams);
        row.addView(details, new LinearLayout.LayoutParams(0, -2, 1));

        NumberFormat money = NumberFormat.getNumberInstance(new Locale("pl", "PL"));
        money.setMinimumFractionDigits(2);
        money.setMaximumFractionDigits(2);
        TextView amount = text((incoming ? "+" : "−")
                + money.format(item.getKwota() == null ? BigDecimal.ZERO : item.getKwota())
                + "\n" + nonEmpty(item.getWaluta(), ""), 13, true, incoming ? "positive" : "negative");
        amount.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        row.addView(amount, new LinearLayout.LayoutParams(-2, -2));
        historyList.addView(row, matchWrap());
    }

    private String formatDate(String value) {
        if (value == null || value.trim().isEmpty()) return "Data nieznana";
        try {
            SimpleDateFormat input = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ROOT);
            Date date = input.parse(value);
            if (date != null) return new SimpleDateFormat("dd.MM.yyyy, HH:mm", new Locale("pl", "PL")).format(date);
        } catch (ParseException ignored) { }
        return value.replace('T', ' ');
    }

    private String nonEmpty(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    private void setStatus(String message, boolean error) {
        status.setText(message);
        status.setTextColor(error ? 0xFFCF4545 : ThemeManager.getSecondaryTextColor(this));
    }

    private void setBusy(boolean busy) {
        progress.setVisibility(busy ? View.VISIBLE : View.GONE);
        if (busy) {
            sendButton.setEnabled(false);
            refreshHistoryButton.setEnabled(false);
        } else {
            sendButton.setEnabled(!accounts.isEmpty() && !transferInProgress);
            refreshHistoryButton.setEnabled(true);
        }
    }

    private void hideKeyboard() {
        try {
            InputMethodManager manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (manager != null) manager.hideSoftInputFromWindow(titleInput.getWindowToken(), 0);
        } catch (Exception ignored) { }
    }

    private String errorLabel(int code) {
        return "HTTP " + code;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams withBottomMargin(int margin) {
        LinearLayout.LayoutParams params = matchWrap();
        params.bottomMargin = dp(margin);
        return params;
    }

    private GradientDrawable round(int color, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        drawable.setStroke(dp(1), ThemeManager.getAccentSoftColor(this));
        return drawable;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onDestroy() {
        if (accountsCall != null) accountsCall.cancel();
        if (historyCall != null) historyCall.cancel();
        if (transferCall != null) transferCall.cancel();
        ThemeManager.unregisterThemeChangeListener(this);
        super.onDestroy();
    }
}
