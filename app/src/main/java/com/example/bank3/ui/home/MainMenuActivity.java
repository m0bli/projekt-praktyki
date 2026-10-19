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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.bank3.R;
import com.example.bank3.data.repository.KontoRepository;
import com.example.bank3.model.SaldoResponse;

import java.text.NumberFormat;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.example.bank3.ui.auth.MainActivity;
import com.example.bank3.ui.theme.ThemeManager;

import java.io.InputStream;

public class MainMenuActivity extends AppCompatActivity
        implements ThemeManager.ThemeChangeListener {

    private DrawerLayout drawerLayout;
    private FrameLayout contentFrame;
    private View homeScroll;
    private SwipeRefreshLayout homeSwipeRefresh;

    private TextView txtWelcome;
    private TextView txtBalance;
    private KontoRepository kontoRepository;
    private long uzytkownikId = -1L;
    private Call<SaldoResponse> saldoCall;
    private boolean saldoLoading;
    private boolean saldoCanRetry;

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
        homeScroll = findViewById(R.id.homeSwipeRefresh);
        homeSwipeRefresh = findViewById(R.id.homeSwipeRefresh);
        homeSwipeRefresh.setColorSchemeResources(R.color.theme_primary);
        homeSwipeRefresh.setOnRefreshListener(() -> {
            if (saldoLoading) {
                homeSwipeRefresh.setRefreshing(false);
            } else {
                pobierzSaldo();
            }
        });

        navHome = findViewById(R.id.navHome);
        navPayments = findViewById(R.id.navPayments);
        navCards = findViewById(R.id.navCards);
        navMore = findViewById(R.id.navMore);

        txtWelcome = findViewById(R.id.txtWelcome);
        txtBalance = findViewById(R.id.txtBalance);

        txtBalance.setText("Ładowanie salda...");
        txtBalance.setOnClickListener(v -> {
            if (saldoCanRetry && !saldoLoading) {
                pobierzSaldo();
            }
        });
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
        if (saldoLoading) {
            return;
        }
        if (uzytkownikId <= 0) {
            saldoCanRetry = false;
            txtBalance.setText("Nie udało się ustalić użytkownika");
            txtBalance.setContentDescription("Nie udało się ustalić użytkownika");
            return;
        }

        saldoLoading = true;
        saldoCanRetry = false;
        txtBalance.setText("Ładowanie salda...");
        txtBalance.setContentDescription("Ładowanie salda");

        saldoCall = kontoRepository.getSaldoUzytkownika(uzytkownikId);
        saldoCall.enqueue(new Callback<SaldoResponse>() {
            @Override
            public void onResponse(
                    Call<SaldoResponse> call,
                    Response<SaldoResponse> response
            ) {
                if (call.isCanceled() || isFinishing() || isDestroyed()) {
                    return;
                }
                saldoLoading = false;
                homeSwipeRefresh.setRefreshing(false);

                SaldoResponse saldoResponse = response.body();
                if (response.isSuccessful() && saldoResponse != null
                        && saldoResponse.getSaldo() != null) {
                    NumberFormat format = NumberFormat.getNumberInstance(
                            new Locale("pl", "PL")
                    );
                    format.setMinimumFractionDigits(2);
                    format.setMaximumFractionDigits(2);
                    String waluta = saldoResponse.getWaluta() == null
                            ? "PLN" : saldoResponse.getWaluta();
                    txtBalance.setText(
                            format.format(saldoResponse.getSaldo()) + " " + waluta
                    );
                    txtBalance.setContentDescription("Dostępne saldo: " + txtBalance.getText());
                    saldoCanRetry = false;
                } else if (response.code() == 404) {
                    txtBalance.setText("Nie znaleziono konta");
                    txtBalance.setContentDescription("Nie znaleziono konta");
                    saldoCanRetry = false;
                } else {
                    showSaldoRetryState(response.code() >= 500
                            ? "Serwer chwilowo niedostępny."
                            : "Nie udało się pobrać salda.");
                }
            }

            @Override
            public void onFailure(Call<SaldoResponse> call, Throwable throwable) {
                if (call.isCanceled() || isFinishing() || isDestroyed()) {
                    return;
                }
                saldoLoading = false;
                homeSwipeRefresh.setRefreshing(false);
                showSaldoRetryState("Błąd połączenia z serwerem.");
            }
        });
    }

    private void showSaldoRetryState(String message) {
        saldoCanRetry = true;
        txtBalance.setText(message + " Dotknij, aby ponowić.");
        txtBalance.setContentDescription(message + " Dotknij, aby ponowić pobieranie salda.");
    }

    @Override
    protected void onDestroy() {
        if (saldoCall != null && !saldoCall.isCanceled()) {
            saldoCall.cancel();
        }
        super.onDestroy();
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
                showPlaceholder(
                        "Płatności",
                        "Tutaj dodamy funkcję przelewów."
                );
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