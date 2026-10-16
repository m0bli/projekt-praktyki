package com.example.bank3.ui.home;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.text.InputType;
import android.widget.ScrollView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.bank3.R;
import com.example.bank3.data.repository.KontoRepository;
import com.example.bank3.data.repository.TransakcjaRepository;
import com.example.bank3.model.SaldoResponse;
import com.example.bank3.model.TransakcjaRequest;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import okhttp3.ResponseBody;
import com.example.bank3.ui.auth.MainActivity;
import com.example.bank3.ui.theme.ThemeManager;

import java.io.InputStream;

public class MainMenuActivity extends AppCompatActivity
        implements ThemeManager.ThemeChangeListener {

    private DrawerLayout drawerLayout;
    private FrameLayout contentFrame;
    private View homeScroll;

    private TextView txtWelcome;
    private TextView txtBalance;
    private KontoRepository kontoRepository;
    private long uzytkownikId = -1L;
    private long kontoZrodloweId = -1L;
    private String walutaKonta = "PLN";
    private final TransakcjaRepository transakcjaRepository = new TransakcjaRepository();

    private LinearLayout navHome;
    private LinearLayout navPayments;
    private LinearLayout navCards;
    private LinearLayout navMore;

    private int selectedTab = 0;
    private boolean isRefreshingTheme = false;

    private int getNavy() {
        return ThemeManager.getTextColor(this);
    }

    private int getActiveBackground() {
        return ThemeManager.getAccentSoftColor(this);
    }

    private int getGray() {
        return ThemeManager.getSecondaryTextColor(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main_menu);

        drawerLayout = findViewById(R.id.drawerLayout);
        contentFrame = findViewById(R.id.contentFrame);
        homeScroll = findViewById(R.id.homeScroll);

        navHome = findViewById(R.id.navHome);
        navPayments = findViewById(R.id.navPayments);
        navCards = findViewById(R.id.navCards);
        navMore = findViewById(R.id.navMore);

        txtWelcome = findViewById(R.id.txtWelcome);
        txtBalance = findViewById(R.id.txtBalance);

        txtBalance.setText("Ładowanie salda...");
        kontoRepository = new KontoRepository();
        uzytkownikId = getIntent().getLongExtra("uzytkownikId", -1L);

        String nick = getIntent().getStringExtra("nick");
        if (nick != null && !nick.trim().isEmpty()) {
            txtWelcome.setText("Witaj, " + nick + "!");
        }

        View mainContent = drawerLayout.getChildAt(0);

        ViewCompat.setOnApplyWindowInsetsListener(
                mainContent,
                (view, windowInsets) -> {
                    androidx.core.graphics.Insets bars =
                            windowInsets.getInsets(
                                    WindowInsetsCompat.Type.systemBars()
                            );

                    view.setPadding(
                            bars.left,
                            bars.top,
                            bars.right,
                            bars.bottom
                    );

                    return windowInsets;
                }
        );

        findViewById(R.id.btnProfile).setOnClickListener(v ->
                drawerLayout.openDrawer(GravityCompat.START)
        );

        findViewById(R.id.drawerProfile).setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            showPlaceholder(
                    "Moje dane",
                    "Tutaj wyświetlimy dane Twojego konta."
            );
        });

        findViewById(R.id.drawerSettings).setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);

            startActivity(new Intent(
                    MainMenuActivity.this,
                    SettingsActivity.class
            ));
        });

        findViewById(R.id.drawerPrivacy).setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            showPlaceholder(
                    "Bezpieczeństwo",
                    "Ustawienia bezpieczeństwa konta."
            );
        });

        findViewById(R.id.btnNotifications).setOnClickListener(v ->
                Toast.makeText(
                        this,
                        "Brak nowych powiadomień",
                        Toast.LENGTH_SHORT
                ).show()
        );

        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            Intent intent = new Intent(
                    MainMenuActivity.this,
                    MainActivity.class
            );

            intent.setFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TASK
            );

            startActivity(intent);
            finish();
        });

        navHome.setOnClickListener(v -> selectTab(0));
        navPayments.setOnClickListener(v -> selectTab(1));
        navCards.setOnClickListener(v -> selectTab(2));
        navMore.setOnClickListener(v -> selectTab(3));

        findViewById(R.id.actionTransfer).setOnClickListener(v ->
                selectTab(1)
        );

        findViewById(R.id.actionCards).setOnClickListener(v ->
                selectTab(2)
        );

        findViewById(R.id.actionHistory).setOnClickListener(v ->
                showPlaceholder(
                        "Historia transakcji",
                        "Historia jest jeszcze w trakcie przygotowania."
                )
        );

        refreshTheme();
        selectTab(0);
        pobierzSaldo();
    }

    private void pobierzSaldo() {
        if (uzytkownikId <= 0) {
            txtBalance.setText("Nie udało się ustalić użytkownika");
            return;
        }

        kontoRepository.getSaldoUzytkownika(uzytkownikId).enqueue(
                new Callback<SaldoResponse>() {
                    @Override
                    public void onResponse(
                            Call<SaldoResponse> call,
                            Response<SaldoResponse> response
                    ) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        SaldoResponse saldoResponse = response.body();
                        if (response.isSuccessful() && saldoResponse != null
                                && saldoResponse.getSaldo() != null) {
                            NumberFormat format = NumberFormat.getNumberInstance(
                                    new Locale("pl", "PL")
                            );
                            format.setMinimumFractionDigits(2);
                            format.setMaximumFractionDigits(2);
                            kontoZrodloweId = saldoResponse.getKontoId() == null ? -1L : saldoResponse.getKontoId();
                            walutaKonta = saldoResponse.getWaluta() == null ? "PLN" : saldoResponse.getWaluta();
                            String waluta = walutaKonta;
                            txtBalance.setText(
                                    format.format(saldoResponse.getSaldo()) + " " + waluta
                            );
                        } else if (response.code() == 404) {
                            txtBalance.setText("Nie znaleziono konta");
                        } else {
                            txtBalance.setText("Nie udało się pobrać salda");
                        }
                    }

                    @Override
                    public void onFailure(Call<SaldoResponse> call, Throwable throwable) {
                        if (!isFinishing() && !isDestroyed()) {
                            txtBalance.setText("Błąd połączenia z serwerem");
                        }
                    }
                }
        );
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
    protected void onResume() {
        super.onResume();
        refreshTheme();
    }

    @Override
    public void onThemeChanged() {
        refreshTheme();
    }

    private void refreshTheme() {
        if (drawerLayout == null || isRefreshingTheme) {
            return;
        }

        isRefreshingTheme = true;

        try {
            boolean dark = ThemeManager.isBlack(this);

            WindowInsetsControllerCompat controller =
                    new WindowInsetsControllerCompat(
                            getWindow(),
                            getWindow().getDecorView()
                    );

            controller.setAppearanceLightStatusBars(!dark);
            controller.setAppearanceLightNavigationBars(!dark);

            applyHomeBackground();
            applyDynamicColors();
        } finally {
            isRefreshingTheme = false;
        }
    }

    private void applyDynamicColors() {
        int background = ThemeManager.getBackgroundColor(this);
        int surface = ThemeManager.getSurfaceColor(this);
        int text = ThemeManager.getTextColor(this);
        int secondary = ThemeManager.getSecondaryTextColor(this);
        int accent = ThemeManager.getAccentColor(this);

        View mainContent = drawerLayout.getChildAt(0);

        if (mainContent instanceof ViewGroup) {
            ViewGroup mainGroup = (ViewGroup) mainContent;
            mainContent.setBackgroundColor(background);

            if (mainGroup.getChildCount() > 0) {
                View topBar = mainGroup.getChildAt(0);
                topBar.setBackgroundColor(surface);

                if (topBar instanceof ViewGroup) {
                    applyTextColors(
                            (ViewGroup) topBar,
                            text,
                            secondary
                    );
                }
            }
        }

        contentFrame.setBackgroundColor(background);

        View bottomNavigation = findViewById(R.id.bottomNavigation);
        if (bottomNavigation != null) {
            bottomNavigation.setBackgroundColor(surface);
        }

        if (txtWelcome != null) {
            txtWelcome.setTextColor(text);
        }

        if (txtBalance != null) {
            txtBalance.setTextColor(Color.WHITE);
        }

        View drawer = findViewById(R.id.profileDrawer);
        if (drawer instanceof ViewGroup) {
            drawer.setBackgroundColor(surface);
            applyTextColors((ViewGroup) drawer, text, secondary);
        }

        colorQuickAction(R.id.actionTransfer, accent);
        colorQuickAction(R.id.actionCards, accent);
        colorQuickAction(R.id.actionHistory, accent);

        updateNavigationColors();
    }

    private void applyTextColors(
            ViewGroup parent,
            int textColor,
            int secondaryColor
    ) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);

            if (child instanceof TextView) {
                TextView tv = (TextView) child;
                int id = tv.getId();

                if (id == R.id.btnLogout) {
                    continue;
                }

                if (id == R.id.btnProfile
                        || id == R.id.btnNotifications) {
                    tv.setTextColor(ThemeManager.getAccentColor(this));
                } else {
                    tv.setTextColor(textColor);
                }
            } else if (child instanceof ViewGroup) {
                applyTextColors(
                        (ViewGroup) child,
                        textColor,
                        secondaryColor
                );
            }
        }
    }

    private void colorQuickAction(int id, int accent) {
        View view = findViewById(id);

        if (view instanceof TextView) {
            ((TextView) view).setTextColor(accent);
        }
    }

    private void updateNavigationColors() {
        LinearLayout[] tabs = {
                navHome, navPayments, navCards, navMore
        };

        for (int i = 0; i < tabs.length; i++) {
            LinearLayout tab = tabs[i];

            if (tab == null || tab.getChildCount() < 2) {
                continue;
            }

            boolean selected = i == selectedTab;

            tab.setBackground(createRoundedBackground(
                    selected
                            ? getActiveBackground()
                            : ThemeManager.getSurfaceColor(this),
                    18
            ));

            int color = selected
                    ? ThemeManager.getAccentColor(this)
                    : getGray();

            View icon = tab.getChildAt(0);
            View label = tab.getChildAt(1);

            if (icon instanceof TextView) {
                ((TextView) icon).setTextColor(color);
            }

            if (label instanceof TextView) {
                ((TextView) label).setTextColor(color);
            }

            tab.animate().cancel();

            if (selected) {
                tab.animate()
                        .scaleX(1.05f)
                        .scaleY(1.05f)
                        .setDuration(150)
                        .setInterpolator(new DecelerateInterpolator())
                        .withEndAction(() ->
                                tab.animate()
                                        .scaleX(1f)
                                        .scaleY(1f)
                                        .setDuration(100)
                                        .start()
                        )
                        .start();
            } else {
                tab.setScaleX(1f);
                tab.setScaleY(1f);
            }
        }
    }

    private void selectTab(int position) {
        selectedTab = position;
        updateNavigationColors();

        switch (position) {
            case 0:
                showHome();
                break;

            case 1:
                showTransferForm();
                break;

            case 2:
                showPlaceholder(
                        "Karty",
                        "Tutaj pojawią się karty użytkownika."
                );
                break;

            case 3:
                showPlaceholder(
                        "Więcej",
                        "Dodatkowe funkcje aplikacji."
                );
                break;
        }
    }


    private void showTransferForm() {
        contentFrame.removeAllViews();
        ScrollView scroll = new ScrollView(this);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(22), dp(22), dp(22), dp(28));
        page.setBackgroundColor(ThemeManager.getBackgroundColor(this));
        scroll.addView(page);

        TextView heading = new TextView(this);
        heading.setText("Nowy przelew");
        heading.setTextSize(25);
        heading.setTypeface(null, android.graphics.Typeface.BOLD);
        heading.setTextColor(getNavy());
        page.addView(heading);

        TextView subtitle = new TextView(this);
        subtitle.setText("Przelew krajowy · bez przewalutowania");
        subtitle.setTextSize(14);
        subtitle.setTextColor(getGray());
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subtitleParams.topMargin = dp(6);
        page.addView(subtitle, subtitleParams);

        TextView currencyInfo = new TextView(this);
        currencyInfo.setText("Waluta konta: " + walutaKonta);
        currencyInfo.setTextColor(getGray());
        currencyInfo.setTextSize(14);
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        infoParams.topMargin = dp(18);
        page.addView(currencyInfo, infoParams);

        EditText accountInput = addTransferInput(page, "Numer konta odbiorcy (26 cyfr)", InputType.TYPE_CLASS_NUMBER);
        EditText nameInput = addTransferInput(page, "Imię odbiorcy", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        EditText amountInput = addTransferInput(page, "Kwota", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText titleInput = addTransferInput(page, "Tytuł przelewu", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);

        Button sendButton = new Button(this);
        sendButton.setText("Sprawdź i wykonaj przelew");
        sendButton.setTextColor(Color.WHITE);
        sendButton.setBackground(createRoundedBackground(ThemeManager.getAccentColor(this), 14));
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        buttonParams.topMargin = dp(20);
        page.addView(sendButton, buttonParams);

        TextView hint = new TextView(this);
        hint.setText("Backend sprawdzi odbiorcę, saldo i zgodność waluty przed wykonaniem przelewu.");
        hint.setTextColor(getGray());
        hint.setTextSize(12);
        LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hintParams.topMargin = dp(12);
        page.addView(hint, hintParams);

        sendButton.setOnClickListener(v -> {
            String numer = accountInput.getText().toString().replaceAll("\\s+", "");
            String imie = nameInput.getText().toString().trim();
            String kwotaText = amountInput.getText().toString().trim().replace(',', '.');
            String tytul = titleInput.getText().toString().trim();

            if (uzytkownikId <= 0 || kontoZrodloweId <= 0) {
                Toast.makeText(this, "Nie udało się pobrać konta źródłowego. Odśwież aplikację.", Toast.LENGTH_LONG).show();
                return;
            }
            if (!numer.matches("\\d{26}")) {
                accountInput.setError("Wpisz 26 cyfr numeru konta");
                return;
            }
            if (imie.isEmpty()) {
                nameInput.setError("Wpisz imię odbiorcy");
                return;
            }
            if (kwotaText.isEmpty()) {
                amountInput.setError("Wpisz kwotę");
                return;
            }
            final BigDecimal kwota;
            try {
                kwota = new BigDecimal(kwotaText);
            } catch (NumberFormatException ex) {
                amountInput.setError("Nieprawidłowa kwota");
                return;
            }
            if (kwota.compareTo(BigDecimal.ZERO) <= 0 || kwota.scale() > 2) {
                amountInput.setError("Kwota musi być dodatnia i mieć maksymalnie 2 miejsca po przecinku");
                return;
            }
            if (tytul.isEmpty()) {
                titleInput.setError("Wpisz tytuł przelewu");
                return;
            }
            if (tytul.length() > 140) {
                titleInput.setError("Maksymalnie 140 znaków");
                return;
            }

            sendButton.setEnabled(false);
            sendButton.setText("Wysyłanie...");
            TransakcjaRequest request = new TransakcjaRequest(
                    uzytkownikId, kontoZrodloweId, numer, imie, kwota, walutaKonta, tytul);
            transakcjaRepository.wykonajPrzelew(request).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (isFinishing() || isDestroyed()) return;
                    if (response.isSuccessful()) {
                        Toast.makeText(MainMenuActivity.this, "Przelew wykonany", Toast.LENGTH_LONG).show();
                        pobierzSaldo();
                        showTransferForm();
                    } else {
                        sendButton.setEnabled(true);
                        sendButton.setText("Sprawdź i wykonaj przelew");
                        Toast.makeText(MainMenuActivity.this,
                                "Przelew odrzucony (HTTP " + response.code() + "). Sprawdź dane, saldo i walutę.",
                                Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable throwable) {
                    if (isFinishing() || isDestroyed()) return;
                    sendButton.setEnabled(true);
                    sendButton.setText("Sprawdź i wykonaj przelew");
                    Toast.makeText(MainMenuActivity.this, "Błąd połączenia z backendem", Toast.LENGTH_LONG).show();
                }
            });
        });

        contentFrame.addView(scroll, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private EditText addTransferInput(LinearLayout parent, String label, int inputType) {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint(label);
        input.setTextColor(ThemeManager.getTextColor(this));
        input.setHintTextColor(ThemeManager.getSecondaryTextColor(this));
        input.setInputType(inputType);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(12);
        parent.addView(input, params);
        return input;
    }

    private void showHome() {
        contentFrame.removeAllViews();

        if (homeScroll.getParent() instanceof ViewGroup) {
            ((ViewGroup) homeScroll.getParent())
                    .removeView(homeScroll);
        }

        contentFrame.addView(
                homeScroll,
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                )
        );

        applyHomeBackground();
    }

    private void showPlaceholder(
            String title,
            String description
    ) {
        contentFrame.removeAllViews();

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setGravity(Gravity.CENTER);
        page.setPadding(dp(28), dp(28), dp(28), dp(28));
        page.setBackgroundColor(
                ThemeManager.getBackgroundColor(this)
        );

        TextView heading = new TextView(this);
        heading.setText(title);
        heading.setTextSize(25);
        heading.setTextColor(getNavy());
        heading.setGravity(Gravity.CENTER);
        heading.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );

        TextView info = new TextView(this);
        info.setText(description);
        info.setTextSize(15);
        info.setTextColor(getGray());
        info.setGravity(Gravity.CENTER);
        info.setPadding(0, dp(12), 0, 0);

        page.addView(heading);
        page.addView(info);

        contentFrame.addView(
                page,
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                )
        );
    }

    private GradientDrawable createRoundedBackground(
            int color,
            int radiusDp
    ) {
        float density = getResources()
                .getDisplayMetrics().density;

        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radiusDp * density);

        return drawable;
    }

    private void applyHomeBackground() {
        if (homeScroll == null) {
            return;
        }

        Uri imageUri = ThemeManager.getBackgroundImage(this);

        if (imageUri == null) {
            homeScroll.setBackgroundColor(
                    ThemeManager.getBackgroundColor(this)
            );
            return;
        }

        try (InputStream stream = getContentResolver()
                .openInputStream(imageUri)) {

            if (stream == null) {
                throw new IllegalStateException(
                        "Nie można otworzyć zdjęcia"
                );
            }

            Bitmap bitmap = BitmapFactory.decodeStream(stream);

            if (bitmap == null) {
                throw new IllegalStateException(
                        "Nie można odczytać zdjęcia"
                );
            }

            BitmapDrawable drawable = new BitmapDrawable(
                    getResources(),
                    bitmap
            );

            drawable.setGravity(Gravity.FILL);
            homeScroll.setBackground(drawable);

        } catch (Exception e) {
            ThemeManager.clearBackgroundImage(this);
            homeScroll.setBackgroundColor(
                    ThemeManager.getBackgroundColor(this)
            );
        }
    }

    private int dp(float value) {
        return (int) (value * getResources()
                .getDisplayMetrics().density + 0.5f);
    }
}