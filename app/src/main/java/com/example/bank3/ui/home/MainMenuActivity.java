package com.example.bank3.ui.home;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import com.example.bank3.R;
import com.example.bank3.data.repository.KontoRepository;
import com.example.bank3.model.SaldoResponse;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.http.GET;
import retrofit2.http.Path;
import com.example.bank3.ui.auth.MainActivity;
import com.example.bank3.ui.theme.ThemeManager;

import java.io.InputStream;

public class MainMenuActivity extends AppCompatActivity
        implements ThemeManager.ThemeChangeListener {

    private interface HistoriaAPI {
        @GET("api/konta/uzytkownik/{uzytkownikId}")
        Call<List<Map<String, Object>>> getAccounts(@Path("uzytkownikId") long userId);

        @GET("api/transakcje/konto/{kontoId}")
        Call<List<Map<String, Object>>> getHistory(@Path("kontoId") long accountId);
    }

    private DrawerLayout drawerLayout;
    private FrameLayout contentFrame;
    private View homeScroll;
    private SwipeRefreshLayout homeSwipeRefresh;

    private TextView txtWelcome;
    private TextView txtBalance;
    private TextView txtAccountId;
    private LinearLayout recentTransactionsContainer;
    private Call<List<Map<String, Object>>> recentAccountsCall;
    private final List<Call<List<Map<String, Object>>>> recentHistoryCalls = new ArrayList<>();
    private KontoRepository kontoRepository;
    private long uzytkownikId = -1L;
    private Call<SaldoResponse> saldoCall;
    private boolean saldoLoading;
    private boolean saldoCanRetry;

    private View notificationBadge;
    private final List<Map<String, Object>> allTransactionsList = new ArrayList<>();

    private final android.os.Handler autoRefreshHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable autoRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isFinishing() && !isDestroyed()) {
                pobierzSaldo();
                loadRecentTransactions();
                autoRefreshHandler.postDelayed(this, 10000);
            }
        }
    };

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
                loadRecentTransactions();
            }
        });

        navHome = findViewById(R.id.navHome);
        navPayments = findViewById(R.id.navPayments);
        navCards = findViewById(R.id.navCards);
        navMore = findViewById(R.id.navMore);

        txtWelcome = findViewById(R.id.txtWelcome);
        txtBalance = findViewById(R.id.txtBalance);
        txtAccountId = findViewById(R.id.txtAccountId);
        recentTransactionsContainer = findViewById(R.id.recentTransactionsContainer);

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
        if (mainContent != null) {
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
        }

        View profileDrawer = findViewById(R.id.profileDrawer);
        if (profileDrawer != null) {
            int pStart = profileDrawer.getPaddingStart();
            int pTop = profileDrawer.getPaddingTop();
            int pEnd = profileDrawer.getPaddingEnd();
            int pBottom = profileDrawer.getPaddingBottom();

            ViewCompat.setOnApplyWindowInsetsListener(
                    profileDrawer,
                    (view, windowInsets) -> {
                        androidx.core.graphics.Insets bars =
                                windowInsets.getInsets(
                                        WindowInsetsCompat.Type.systemBars()
                                );

                        view.setPadding(
                                pStart + bars.left,
                                pTop + bars.top,
                                pEnd,
                                pBottom + bars.bottom
                        );

                        return windowInsets;
                    }
            );
        }

        findViewById(R.id.btnProfile).setOnClickListener(v ->
                drawerLayout.openDrawer(GravityCompat.START)
        );

        findViewById(R.id.drawerProfile).setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            Intent intent = new Intent(MainMenuActivity.this, ProfileActivity.class);
            intent.putExtra("uzytkownikId", uzytkownikId);
            intent.putExtra("nick", getIntent().getStringExtra("nick"));
            startActivity(intent);
        });

        findViewById(R.id.drawerSettings).setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);

            Intent intent = new Intent(
                    MainMenuActivity.this,
                    SettingsActivity.class
            );
            intent.putExtra("uzytkownikId", uzytkownikId);
            intent.putExtra("nick", getIntent().getStringExtra("nick"));
            startActivity(intent);
        });

        notificationBadge = findViewById(R.id.notificationBadge);
        findViewById(R.id.btnNotifications).setOnClickListener(v ->
                showNotificationsDialog()
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

        findViewById(R.id.actionHistory).setOnClickListener(v -> {
            selectTab(0);
            loadRecentTransactions();
            if (recentTransactionsContainer != null) {
                recentTransactionsContainer.requestFocus();
            }
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            private long backPressedTime = 0;

            @Override
            public void handleOnBackPressed() {
                if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                    return;
                }

                if (selectedTab != 0) {
                    selectTab(0);
                    return;
                }

                if (backPressedTime + 2000 > System.currentTimeMillis()) {
                    moveTaskToBack(true);
                } else {
                    Toast.makeText(
                            MainMenuActivity.this,
                            "Naciśnij ponownie wstecz, aby wyjść z aplikacji",
                            Toast.LENGTH_SHORT
                    ).show();
                }
                backPressedTime = System.currentTimeMillis();
            }
        });

        refreshTheme();
        int openTab = getIntent().getIntExtra("openTab", 0);
        selectTab(openTab);
        pobierzSaldo();
        loadRecentTransactions();
    }

    private void loadRecentTransactions() {
        if (recentTransactionsContainer == null) return;
        recentTransactionsContainer.removeAllViews();
        addTransactionMessage("Ładowanie ostatnich transakcji…");

        if (uzytkownikId <= 0) {
            showRecentTransactionsMessage("Nie udało się ustalić zalogowanego użytkownika.");
            return;
        }

        HistoriaAPI api = com.example.bank3.network.RetrofitClient.getInstance()
                .create(HistoriaAPI.class);
        if (recentAccountsCall != null) recentAccountsCall.cancel();
        for (Call<List<Map<String, Object>>> call : recentHistoryCalls) call.cancel();
        recentHistoryCalls.clear();

        recentAccountsCall = api.getAccounts(uzytkownikId);
        recentAccountsCall.enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> call,
                                   Response<List<Map<String, Object>>> response) {
                if (call.isCanceled() || isFinishing() || isDestroyed()) return;
                if (!response.isSuccessful() || response.body() == null) {
                    showRecentTransactionsMessage("Nie udało się pobrać kont (HTTP "
                            + response.code() + ").");
                    return;
                }

                if (txtAccountId != null && !response.body().isEmpty()) {
                    Object firstAccountId = response.body().get(0) == null
                            ? null : response.body().get(0).get("id");
                    if (firstAccountId instanceof Number
                            && ((Number) firstAccountId).longValue() > 0) {
                        txtAccountId.setText("#" + ((Number) firstAccountId).longValue());
                    } else {
                        txtAccountId.setText("#—");
                    }
                }

                List<Long> accountIds = new ArrayList<>();
                for (Map<String, Object> account : response.body()) {
                    Object id = account == null ? null : account.get("id");
                    if (id instanceof Number && ((Number) id).longValue() > 0) {
                        accountIds.add(((Number) id).longValue());
                    }
                }
                if (accountIds.isEmpty()) {
                    if (txtAccountId != null) txtAccountId.setText("#—");
                    showRecentTransactionsMessage("Brak kont do wyświetlenia historii.");
                    return;
                }

                List<Map<String, Object>> all = Collections.synchronizedList(new ArrayList<>());
                int[] remaining = {accountIds.size()};
                boolean[] anySuccess = {false};
                for (Long accountId : accountIds) {
                    Call<List<Map<String, Object>>> historyCall = api.getHistory(accountId);
                    recentHistoryCalls.add(historyCall);
                    historyCall.enqueue(new Callback<List<Map<String, Object>>>() {
                        @Override
                        public void onResponse(Call<List<Map<String, Object>>> history,
                                               Response<List<Map<String, Object>>> result) {
                            if (history.isCanceled() || isFinishing() || isDestroyed()) return;
                            if (result.isSuccessful() && result.body() != null) {
                                anySuccess[0] = true;
                                all.addAll(result.body());
                            }
                            if (--remaining[0] == 0) {
                                if (!anySuccess[0]) {
                                    showRecentTransactionsMessage("Nie udało się pobrać historii. Spróbuj ponownie.");
                                } else {
                                    renderRecentTransactions(all);
                                }
                            }
                        }

                        @Override
                        public void onFailure(Call<List<Map<String, Object>>> history, Throwable t) {
                            if (history.isCanceled() || isFinishing() || isDestroyed()) return;
                            if (--remaining[0] == 0) {
                                if (!anySuccess[0]) {
                                    showRecentTransactionsMessage("Błąd połączenia podczas pobierania historii.");
                                } else {
                                    renderRecentTransactions(all);
                                }
                            }
                        }
                    });
                }
            }

            @Override
            public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                if (call.isCanceled() || isFinishing() || isDestroyed()) return;
                showRecentTransactionsMessage("Błąd połączenia. Dotknij odświeżania ekranu, aby spróbować ponownie.");
            }
        });
    }

    private void renderRecentTransactions(List<Map<String, Object>> transactions) {
        allTransactionsList.clear();
        allTransactionsList.addAll(transactions);

        Collections.sort(allTransactionsList, (a, b) -> {
            String dateA = String.valueOf(a.get("dataOperacji"));
            String dateB = String.valueOf(b.get("dataOperacji"));
            return dateB.compareTo(dateA);
        });

        checkUnreadNotifications();
        checkNewIncomingTransactions(allTransactionsList);

        recentTransactionsContainer.removeAllViews();
        if (allTransactionsList.isEmpty()) {
            addTransactionMessage("Nie masz jeszcze żadnych transakcji.");
            return;
        }

        int count = Math.min(3, allTransactionsList.size());
        for (int i = 0; i < count; i++) {
            Map<String, Object> transaction = allTransactionsList.get(i);
            String direction = String.valueOf(transaction.get("kierunek"));
            String title = String.valueOf(transaction.get("tytul"));
            String currency = transaction.get("waluta") == null ? "PLN" : String.valueOf(transaction.get("waluta"));
            String amountText = formatTransactionAmount(transaction.get("kwota"));
            boolean income = "WPLYW".equalsIgnoreCase(direction);
            String sign = income ? "+" : "−";
            String counterparty = transaction.get("imieKontrahenta") == null
                    ? "" : String.valueOf(transaction.get("imieKontrahenta"));
            String date = formatTransactionDate(transaction.get("dataOperacji"));

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(dp(10), dp(12), dp(10), dp(12));

            LinearLayout top = new LinearLayout(this);
            top.setOrientation(LinearLayout.HORIZONTAL);
            top.setGravity(Gravity.CENTER_VERTICAL);

            TextView titleView = new TextView(this);
            titleView.setText(title == null || "null".equals(title) || title.trim().isEmpty()
                    ? "Przelew" : title);
            titleView.setTextColor(ThemeManager.getTextColor(this));
            titleView.setTextSize(14);
            titleView.setTypeface(null, android.graphics.Typeface.BOLD);
            titleView.setMaxLines(2);
            titleView.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));

            TextView amountView = new TextView(this);
            amountView.setText(sign + amountText + " " + currency);
            amountView.setTextColor(income ? Color.rgb(35, 150, 90) : ThemeManager.getTextColor(this));
            amountView.setTextSize(14);
            amountView.setTypeface(null, android.graphics.Typeface.BOLD);
            top.addView(titleView);
            top.addView(amountView);
            row.addView(top);

            TextView details = new TextView(this);
            details.setText((income ? "Wpływ" : "Wydatek")
                    + (counterparty.isEmpty() ? "" : " · konto: " + counterparty)
                    + (date.isEmpty() ? "" : " · " + date));
            details.setTextColor(ThemeManager.getSecondaryTextColor(this));
            details.setTextSize(12);
            LinearLayout.LayoutParams detailsParams = new LinearLayout.LayoutParams(-1, -2);
            detailsParams.topMargin = dp(5);
            row.addView(details, detailsParams);
            recentTransactionsContainer.addView(row);

            if (i < count - 1) {
                View divider = new View(this);
                divider.setBackgroundColor(ThemeManager.getSecondaryTextColor(this));
                recentTransactionsContainer.addView(divider, new LinearLayout.LayoutParams(-1, dp(1)));
            }
        }
    }

    private String formatTransactionAmount(Object amount) {
        try {
            NumberFormat format = NumberFormat.getNumberInstance(Locale.forLanguageTag("pl-PL"));
            format.setMinimumFractionDigits(2);
            format.setMaximumFractionDigits(2);
            return format.format(amount instanceof Number
                    ? ((Number) amount).doubleValue() : Double.parseDouble(String.valueOf(amount)));
        } catch (Exception ignored) {
            return String.valueOf(amount);
        }
    }

    private String formatTransactionDate(Object value) {
        if (value == null) return "";
        String raw = String.valueOf(value);
        if (raw.length() >= 16) {
            return raw.substring(0, 10) + " " + raw.substring(11, 16);
        }
        return raw;
    }

    private void showRecentTransactionsMessage(String message) {
        recentTransactionsContainer.removeAllViews();
        addTransactionMessage(message);
    }

    private void addTransactionMessage(String message) {
        TextView view = new TextView(this);
        view.setText(message);
        view.setTextColor(ThemeManager.getSecondaryTextColor(this));
        view.setTextSize(14);
        view.setPadding(dp(10), dp(12), dp(10), dp(12));
        recentTransactionsContainer.addView(view);
    }

    private void checkNewIncomingTransactions(List<Map<String, Object>> transactions) {
        if (transactions == null || transactions.isEmpty() || uzytkownikId <= 0) return;

        android.content.SharedPreferences prefs = getSharedPreferences("notifications_prefs", MODE_PRIVATE);
        String prefKey = "notified_tx_ids_" + uzytkownikId;
        java.util.Set<String> notifiedIds = new java.util.HashSet<>(prefs.getStringSet(prefKey, new java.util.HashSet<>()));
        boolean firstRun = !prefs.contains(prefKey);

        boolean updated = false;

        for (Map<String, Object> tx : transactions) {
            if (tx == null || tx.get("id") == null) continue;
            String txId = String.valueOf(tx.get("id"));
            String direction = String.valueOf(tx.get("kierunek"));
            boolean isIncome = "WPLYW".equalsIgnoreCase(direction);

            if (firstRun) {
                notifiedIds.add(txId);
                updated = true;
            } else if (isIncome && !notifiedIds.contains(txId)) {
                String amountText = formatTransactionAmount(tx.get("kwota"));
                String currency = tx.get("waluta") == null ? "PLN" : String.valueOf(tx.get("waluta"));
                String counterparty = tx.get("imieKontrahenta") == null
                        ? "" : String.valueOf(tx.get("imieKontrahenta"));

                String title = "Przelew przychodzący";
                String message = "Otrzymałeś " + amountText + " " + currency
                        + (counterparty.isEmpty() ? "" : " od: " + counterparty);

                int notifId = txId.hashCode();

                NotificationHelper.sendTransferNotification(
                        this,
                        uzytkownikId,
                        getIntent().getStringExtra("nick"),
                        title,
                        message,
                        notifId
                );

                notifiedIds.add(txId);
                updated = true;
            }
        }

        if (updated) {
            prefs.edit().putStringSet(prefKey, notifiedIds).apply();
        }
    }

    private void checkUnreadNotifications() {
        if (notificationBadge == null) return;
        if (allTransactionsList.isEmpty()) {
            notificationBadge.setVisibility(View.GONE);
            return;
        }

        String newestDate = String.valueOf(allTransactionsList.get(0).get("dataOperacji"));
        android.content.SharedPreferences prefs = getSharedPreferences("notifications_prefs", MODE_PRIVATE);
        String lastSeenDate = prefs.getString("last_seen_date_" + uzytkownikId, "");

        if (lastSeenDate.isEmpty() || newestDate.compareTo(lastSeenDate) > 0) {
            notificationBadge.setVisibility(View.VISIBLE);
        } else {
            notificationBadge.setVisibility(View.GONE);
        }
    }

    private void markNotificationsAsSeen() {
        if (notificationBadge != null) {
            notificationBadge.setVisibility(View.GONE);
        }
        if (!allTransactionsList.isEmpty()) {
            String newestDate = String.valueOf(allTransactionsList.get(0).get("dataOperacji"));
            getSharedPreferences("notifications_prefs", MODE_PRIVATE)
                    .edit()
                    .putString("last_seen_date_" + uzytkownikId, newestDate)
                    .apply();
        }
    }

    private void showNotificationsDialog() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this, R.style.NotificationDialogTheme);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_notifications, null);
        bottomSheetDialog.setContentView(dialogView);

        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        int targetHeight = (int) (screenHeight * 0.75);

        View dialogRoot = dialogView.findViewById(R.id.dialogRoot);
        if (dialogRoot != null) {
            ViewGroup.LayoutParams params = dialogRoot.getLayoutParams();
            if (params != null) {
                params.height = targetHeight;
                dialogRoot.setLayoutParams(params);
            }

            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.RECTANGLE);
            shape.setColor(ThemeManager.getSurfaceColor(this));
            float radius = dp(24);
            shape.setCornerRadii(new float[]{radius, radius, radius, radius, 0, 0, 0, 0});
            dialogRoot.setBackground(shape);
        }

        TextView dialogTitle = dialogView.findViewById(R.id.dialogTitle);
        TextView btnClose = dialogView.findViewById(R.id.btnCloseDialog);
        View headerDivider = dialogView.findViewById(R.id.headerDivider);
        LinearLayout container = dialogView.findViewById(R.id.notificationsContainer);

        if (dialogTitle != null) {
            dialogTitle.setTextColor(ThemeManager.getTextColor(this));
        }
        if (btnClose != null) {
            btnClose.setTextColor(ThemeManager.getSecondaryTextColor(this));
            btnClose.setOnClickListener(v -> bottomSheetDialog.dismiss());
        }
        if (headerDivider != null) {
            headerDivider.setBackgroundColor(androidx.core.content.ContextCompat.getColor(this, R.color.theme_divider));
        }

        populateNotificationsList(container);
        markNotificationsAsSeen();

        bottomSheetDialog.show();

        View parentView = (View) dialogView.getParent();
        if (parentView != null) {
            parentView.setBackgroundColor(Color.TRANSPARENT);
            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(parentView);
            behavior.setHideable(true);
            behavior.setPeekHeight(targetHeight);
            behavior.setMaxHeight(targetHeight);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            behavior.setSkipCollapsed(true);
            behavior.setDraggable(true);

            behavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
                private float lastDraggedFraction = 0f;

                @Override
                public void onStateChanged(@androidx.annotation.NonNull View bottomSheet, int newState) {
                    if (newState == BottomSheetBehavior.STATE_DRAGGING) {
                        lastDraggedFraction = 0f;
                    } else if (newState == BottomSheetBehavior.STATE_SETTLING || newState == BottomSheetBehavior.STATE_HIDDEN) {
                        int initialTop = screenHeight - targetHeight;
                        float draggedDistance = bottomSheet.getTop() - initialTop;
                        float currentFraction = draggedDistance / (float) targetHeight;
                        float fraction = Math.max(currentFraction, lastDraggedFraction);

                        if (fraction < 0.70f) {
                            bottomSheet.post(() -> behavior.setState(BottomSheetBehavior.STATE_EXPANDED));
                        }
                    }
                }

                @Override
                public void onSlide(@androidx.annotation.NonNull View bottomSheet, float slideOffset) {
                    int initialTop = screenHeight - targetHeight;
                    float draggedDistance = bottomSheet.getTop() - initialTop;
                    lastDraggedFraction = draggedDistance / (float) targetHeight;
                }
            });
        }
    }

    private void populateNotificationsList(LinearLayout container) {
        if (container == null) return;
        container.removeAllViews();

        if (allTransactionsList.isEmpty()) {
            LinearLayout emptyLayout = new LinearLayout(this);
            emptyLayout.setOrientation(LinearLayout.VERTICAL);
            emptyLayout.setGravity(Gravity.CENTER);
            emptyLayout.setPadding(dp(20), dp(40), dp(20), dp(40));

            TextView iconView = new TextView(this);
            iconView.setText("🔔");
            iconView.setTextSize(40);
            iconView.setGravity(Gravity.CENTER);

            TextView emptyTitle = new TextView(this);
            emptyTitle.setText("Brak powiadomień");
            emptyTitle.setTextColor(ThemeManager.getTextColor(this));
            emptyTitle.setTextSize(18);
            emptyTitle.setTypeface(null, android.graphics.Typeface.BOLD);
            emptyTitle.setGravity(Gravity.CENTER);
            emptyTitle.setPadding(0, dp(12), 0, dp(4));

            TextView emptySub = new TextView(this);
            emptySub.setText("Powiadomienia o Twoich przelewach pojawią się tutaj.");
            emptySub.setTextColor(ThemeManager.getSecondaryTextColor(this));
            emptySub.setTextSize(14);
            emptySub.setGravity(Gravity.CENTER);

            emptyLayout.addView(iconView);
            emptyLayout.addView(emptyTitle);
            emptyLayout.addView(emptySub);
            container.addView(emptyLayout);
            return;
        }

        android.content.SharedPreferences prefs = getSharedPreferences("notifications_prefs", MODE_PRIVATE);
        String lastSeenDate = prefs.getString("last_seen_date_" + uzytkownikId, "");

        for (Map<String, Object> tx : allTransactionsList) {
            String direction = String.valueOf(tx.get("kierunek"));
            boolean income = "WPLYW".equalsIgnoreCase(direction);
            String rawTitle = String.valueOf(tx.get("tytul"));
            String title = (rawTitle == null || "null".equals(rawTitle) || rawTitle.trim().isEmpty())
                    ? (income ? "Przelew przychodzący" : "Przelew wychodzący")
                    : rawTitle;

            String currency = tx.get("waluta") == null ? "PLN" : String.valueOf(tx.get("waluta"));
            String amountText = formatTransactionAmount(tx.get("kwota"));
            String counterparty = tx.get("imieKontrahenta") == null
                    ? "" : String.valueOf(tx.get("imieKontrahenta"));
            String dateStr = formatTransactionDate(tx.get("dataOperacji"));
            String txDate = String.valueOf(tx.get("dataOperacji"));

            boolean isUnread = !lastSeenDate.isEmpty() && txDate.compareTo(lastSeenDate) > 0;

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(16), dp(14), dp(16), dp(14));

            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            cardParams.bottomMargin = dp(10);
            card.setLayoutParams(cardParams);

            GradientDrawable cardBg = new GradientDrawable();
            cardBg.setShape(GradientDrawable.RECTANGLE);
            cardBg.setCornerRadius(dp(16));
            cardBg.setColor(ThemeManager.getSurfaceColor(this));
            cardBg.setStroke(dp(1), isUnread ? ThemeManager.getAccentColor(this) : androidx.core.content.ContextCompat.getColor(this, R.color.theme_divider));
            card.setBackground(cardBg);

            LinearLayout topRow = new LinearLayout(this);
            topRow.setOrientation(LinearLayout.HORIZONTAL);
            topRow.setGravity(Gravity.CENTER_VERTICAL);

            TextView circleIcon = new TextView(this);
            circleIcon.setLayoutParams(new LinearLayout.LayoutParams(dp(40), dp(40)));
            circleIcon.setGravity(Gravity.CENTER);
            circleIcon.setText(income ? "↙" : "↗");
            circleIcon.setTextSize(18);
            circleIcon.setTypeface(null, android.graphics.Typeface.BOLD);
            circleIcon.setTextColor(income ? Color.parseColor("#10B981") : Color.parseColor("#EF4444"));

            GradientDrawable circleBg = new GradientDrawable();
            circleBg.setShape(GradientDrawable.OVAL);
            circleBg.setColor(income ? Color.parseColor("#1510B981") : Color.parseColor("#15EF4444"));
            circleIcon.setBackground(circleBg);

            LinearLayout middle = new LinearLayout(this);
            middle.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams middleParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            middleParams.setMargins(dp(12), 0, dp(8), 0);
            middle.setLayoutParams(middleParams);

            TextView titleTv = new TextView(this);
            titleTv.setText(title);
            titleTv.setTextColor(ThemeManager.getTextColor(this));
            titleTv.setTextSize(15);
            titleTv.setTypeface(null, android.graphics.Typeface.BOLD);
            titleTv.setMaxLines(2);

            TextView detailsTv = new TextView(this);
            String detailsText = (income ? "Otrzymano od: " : "Wysłano do: ")
                    + (counterparty.isEmpty() ? "Brak danych" : counterparty);
            detailsTv.setText(detailsText);
            detailsTv.setTextColor(ThemeManager.getSecondaryTextColor(this));
            detailsTv.setTextSize(12);
            detailsTv.setPadding(0, dp(2), 0, 0);

            TextView dateTv = new TextView(this);
            dateTv.setText(dateStr);
            dateTv.setTextColor(ThemeManager.getSecondaryTextColor(this));
            dateTv.setTextSize(11);
            dateTv.setPadding(0, dp(2), 0, 0);

            middle.addView(titleTv);
            middle.addView(detailsTv);
            middle.addView(dateTv);

            TextView amountTv = new TextView(this);
            String formattedAmountText = amountText + " " + currency;
            amountTv.setText(formattedAmountText);
            amountTv.setTextSize(15);
            amountTv.setTypeface(null, android.graphics.Typeface.BOLD);
            amountTv.setTextColor(income ? Color.parseColor("#10B981") : Color.parseColor("#EF4444"));

            topRow.addView(circleIcon);
            topRow.addView(middle);
            topRow.addView(amountTv);

            card.addView(topRow);
            container.addView(card);
        }
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
                            Locale.forLanguageTag("pl-PL")
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
        if (recentAccountsCall != null) recentAccountsCall.cancel();
        for (Call<List<Map<String, Object>>> call : recentHistoryCalls) call.cancel();
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
        autoRefreshHandler.removeCallbacks(autoRefreshRunnable);
        autoRefreshHandler.postDelayed(autoRefreshRunnable, 10000);
    }

    @Override
    protected void onPause() {
        autoRefreshHandler.removeCallbacks(autoRefreshRunnable);
        super.onPause();
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
                            text
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
            applyTextColors((ViewGroup) drawer, text);
        }

        colorQuickAction(R.id.actionTransfer, accent);
        colorQuickAction(R.id.actionCards, accent);
        colorQuickAction(R.id.actionHistory, accent);

        updateNavigationColors();
    }

    private void applyTextColors(
            ViewGroup parent,
            int textColor
    ) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);

            if (child instanceof TextView) {
                TextView tv = (TextView) child;
                int id = tv.getId();

                if (id == R.id.btnLogout) {
                    continue;
                }

                tv.setTextColor(textColor);
            } else if (child instanceof ImageView) {
                ImageView iv = (ImageView) child;
                int id = iv.getId();
                if (id == R.id.btnProfile || id == R.id.imgNotificationIcon) {
                    iv.setImageTintList(ColorStateList.valueOf(ThemeManager.getAccentColor(this)));
                }
            } else if (child instanceof ViewGroup) {
                applyTextColors(
                        (ViewGroup) child,
                        textColor
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

    public void selectTab(int position) {
        if (position < 0 || position > 3) return;
        selectedTab = position;
        updateNavigationColors();

        androidx.fragment.app.Fragment current =
                getSupportFragmentManager().findFragmentById(R.id.contentFrame);

        if (position == 0) {
            if (current != null) {
                getSupportFragmentManager().beginTransaction()
                        .remove(current).commitNow();
            }
            showHome();
            return;
        }

        androidx.fragment.app.Fragment next;
        if (position == 1) {
            next = new TransferFragment();
        } else if (position == 2) {
            next = new CardsFragment();
        } else {
            if (current != null) {
                getSupportFragmentManager().beginTransaction()
                        .remove(current).commitNow();
            }
            showPlaceholder("Więcej", "Dodatkowe funkcje aplikacji.");
            return;
        }

        // Bez animacji: zakładki zmieniają się natychmiast.
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.contentFrame, next)
                .commit();
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
        androidx.fragment.app.Fragment current =
                getSupportFragmentManager().findFragmentById(R.id.contentFrame);
        if (current != null) {
            getSupportFragmentManager().beginTransaction().remove(current).commitNow();
        }
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