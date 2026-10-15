package com.example.bank3.ui.home;
import com.example.bank3.ui.theme.ThemeManager;
import com.example.bank3.ui.home.SettingsActivity;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
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
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.bank3.R;
import com.example.bank3.ui.auth.MainActivity;

public class MainMenuActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private FrameLayout contentFrame;
    private View homeScroll;

    private TextView txtWelcome;

    private TextView txtBalance;

    private LinearLayout navHome;
    private LinearLayout navPayments;
    private LinearLayout navCards;
    private LinearLayout navMore;

    private final int navy = Color.rgb(23, 59, 112);
    private final int activeBlue = Color.rgb(224, 237, 255);
    private final int gray = Color.rgb(119, 132, 154);

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        ThemeManager.applyTheme(this);

        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main_menu);
        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(
                        getWindow(),
                        getWindow().getDecorView()
                );

        boolean dark = ThemeManager.isBlack(this);

        controller.setAppearanceLightStatusBars(!dark);
        controller.setAppearanceLightNavigationBars(!dark);

        drawerLayout = findViewById(R.id.drawerLayout);
        contentFrame = findViewById(R.id.contentFrame);
        homeScroll = findViewById(R.id.homeScroll);

        navHome = findViewById(R.id.navHome);
        navPayments = findViewById(R.id.navPayments);
        navCards = findViewById(R.id.navCards);
        navMore = findViewById(R.id.navMore);

        txtWelcome = findViewById(R.id.txtWelcome);

        txtBalance = findViewById(R.id.txtBalance);
        txtBalance.setText("Waluta nie połączona z bazą!");

        String nick = getIntent().getStringExtra("nick");

        if (nick != null && !nick.trim().isEmpty()){
            txtWelcome.setText("Witaj, " + nick + "!");
        }

        // Edge-to-edge: bezpieczne odstępy dla systemowych pasków.
        View mainContent = drawerLayout.getChildAt(0);

        ViewCompat.setOnApplyWindowInsetsListener(
                mainContent,
                (view, windowInsets) -> {
                    Insets bars = windowInsets.getInsets(
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

        // Panel profilu z lewej strony.
        findViewById(R.id.btnProfile).setOnClickListener(v ->
                drawerLayout.openDrawer(GravityCompat.START));

        findViewById(R.id.drawerProfile).setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            showPlaceholder("Moje dane",
                    "Tutaj wyświetlimy dane Twojego konta.");
        });

        findViewById(R.id.drawerSettings).setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);

            Intent intent = new Intent(
                    MainMenuActivity.this,
                    SettingsActivity.class
            );

            startActivity(intent);
        });

        findViewById(R.id.drawerPrivacy).setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            showPlaceholder("Bezpieczeństwo",
                    "Ustawienia bezpieczeństwa konta.");
        });

        // Powiadomienia.
        findViewById(R.id.btnNotifications).setOnClickListener(v ->
                Toast.makeText(
                        this,
                        "Brak nowych powiadomień",
                        Toast.LENGTH_SHORT
                ).show());

        // Wylogowanie.
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

        // Dolna nawigacja.
        navHome.setOnClickListener(v -> selectTab(0));
        navPayments.setOnClickListener(v -> selectTab(1));
        navCards.setOnClickListener(v -> selectTab(2));
        navMore.setOnClickListener(v -> selectTab(3));

        // Szybkie akcje.
        findViewById(R.id.actionTransfer).setOnClickListener(v ->
                selectTab(1));

        findViewById(R.id.actionCards).setOnClickListener(v ->
                selectTab(2));

        findViewById(R.id.actionHistory).setOnClickListener(v ->
                showPlaceholder(
                        "Historia transakcji",
                        "Historia jest jeszcze w trakcie przygotowania."
                ));

        selectTab(0);
    }

    private void selectTab(int position) {
        LinearLayout[] tabs = {
                navHome, navPayments, navCards, navMore
        };

        for (int i = 0; i < tabs.length; i++) {
            final LinearLayout tab = tabs[i];
            boolean selected = i == position;

            tab.setBackground(createRoundedBackground(
                    selected ? activeBlue : Color.WHITE,
                    18
            ));

            TextView icon = (TextView) tab.getChildAt(0);
            TextView label = (TextView) tab.getChildAt(1);

            int color = selected ? navy : gray;
            icon.setTextColor(color);
            label.setTextColor(color);

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
                                        .start())
                        .start();
            } else {
                tab.setScaleX(1f);
                tab.setScaleY(1f);
            }
        }

        switch (position) {
            case 0:
                showHome();
                break;
            case 1:
                showPlaceholder("Płatności",
                        "Tutaj dodamy funkcję przelewów.");
                break;
            case 2:
                showPlaceholder("Karty",
                        "Tutaj pojawią się karty użytkownika.");
                break;
            case 3:
                showPlaceholder("Więcej",
                        "Dodatkowe funkcje aplikacji.");
                break;
        }
    }

    private void showHome() {
        contentFrame.removeAllViews();

        if (homeScroll.getParent() instanceof ViewGroup) {
            ((ViewGroup) homeScroll.getParent()).removeView(homeScroll);
        }

        contentFrame.addView(
                homeScroll,
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                )
        );
    }

    private void showPlaceholder(String title, String description) {
        contentFrame.removeAllViews();

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setGravity(Gravity.CENTER);
        page.setPadding(28, 28, 28, 28);

        TextView heading = new TextView(this);
        heading.setText(title);
        heading.setTextSize(25);
        heading.setTextColor(navy);
        heading.setGravity(Gravity.CENTER);
        heading.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView info = new TextView(this);
        info.setText(description);
        info.setTextSize(15);
        info.setTextColor(gray);
        info.setGravity(Gravity.CENTER);
        info.setPadding(0, 12, 0, 0);

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
}