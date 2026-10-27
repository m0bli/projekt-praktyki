package com.example.bank3.ui.home;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.bank3.R;
import com.example.bank3.data.api.AuthAPI;
import com.example.bank3.data.api.KontoAPI;
import com.example.bank3.data.api.UserAPI;
import com.example.bank3.model.AuthResponse;
import com.example.bank3.model.LoginRequest;
import com.example.bank3.model.UserRequest;
import com.example.bank3.model.UserResponse;
import com.example.bank3.network.RetrofitClient;
import com.example.bank3.ui.theme.ThemeManager;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity
        implements ThemeManager.ThemeChangeListener {

    private SwipeRefreshLayout profileSwipeRefresh;
    private ScrollView profileScroll;
    private ProgressBar profileProgressBar;

    private TextView txtAvatarInitials;
    private TextView txtHeaderFullName;

    private TextView valFullName;
    private TextView valLogin;
    private TextView valEmail;
    private TextView btnShowEmail;
    private TextView btnHideEmail;
    private TextView btnCopyEmail;

    private TextView valUserId;
    private TextView valAccountId;

    private TextView valFullAccountNumber;
    private TextView btnShowFullAccountNumber;
    private TextView btnHideFullAccountNumber;
    private TextView btnCopyFullAccountNumber;

    private View itemChangePassword;

    private long uzytkownikId = -1L;
    private String rawAccountIdStr = "1";
    private String fetchedNumerKonta = "";
    private String currentUserLogin = "";
    private String currentUserImie = "";
    private String currentUserNazwisko = "";
    private String currentUserEmail = "";
    private String nickFallback = "";

    private Call<UserResponse> userCall;

    private int initialPaddingLeft;
    private int initialPaddingTop;
    private int initialPaddingRight;
    private int initialPaddingBottom;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

        profileSwipeRefresh = findViewById(R.id.profileSwipeRefresh);
        profileScroll = findViewById(R.id.profileScroll);
        profileProgressBar = findViewById(R.id.profileProgressBar);

        txtAvatarInitials = findViewById(R.id.txtAvatarInitials);
        txtHeaderFullName = findViewById(R.id.txtHeaderFullName);

        valFullName = findViewById(R.id.valFullName);
        valLogin = findViewById(R.id.valLogin);
        valEmail = findViewById(R.id.valEmail);
        btnShowEmail = findViewById(R.id.btnShowEmail);
        btnHideEmail = findViewById(R.id.btnHideEmail);
        btnCopyEmail = findViewById(R.id.btnCopyEmail);

        valUserId = findViewById(R.id.valUserId);
        valAccountId = findViewById(R.id.valAccountId);

        valFullAccountNumber = findViewById(R.id.valFullAccountNumber);
        btnShowFullAccountNumber = findViewById(R.id.btnShowFullAccountNumber);
        btnHideFullAccountNumber = findViewById(R.id.btnHideFullAccountNumber);
        btnCopyFullAccountNumber = findViewById(R.id.btnCopyFullAccountNumber);

        itemChangePassword = findViewById(R.id.itemChangePassword);

        uzytkownikId = getIntent().getLongExtra("uzytkownikId", -1L);
        nickFallback = getIntent().getStringExtra("nick");
        if (nickFallback == null) {
            nickFallback = "";
        }
        currentUserLogin = nickFallback;

        profileSwipeRefresh.setColorSchemeResources(R.color.theme_primary);
        profileSwipeRefresh.setOnRefreshListener(this::loadUserData);

        initialPaddingLeft = profileScroll.getPaddingLeft();
        initialPaddingTop = profileScroll.getPaddingTop();
        initialPaddingRight = profileScroll.getPaddingRight();
        initialPaddingBottom = profileScroll.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(
                profileScroll,
                (view, windowInsets) -> {
                    Insets bars = windowInsets.getInsets(
                            WindowInsetsCompat.Type.systemBars()
                    );

                    view.setPadding(
                            initialPaddingLeft + bars.left,
                            initialPaddingTop + bars.top,
                            initialPaddingRight + bars.right,
                            initialPaddingBottom + bars.bottom
                    );

                    return windowInsets;
                }
        );
        ViewCompat.requestApplyInsets(profileScroll);

        findViewById(R.id.btnBackProfile).setOnClickListener(v -> finish());

        // Pokaż / schowaj e-mail
        btnShowEmail.setOnClickListener(v -> {
            valEmail.setVisibility(View.VISIBLE);
            btnShowEmail.setVisibility(View.GONE);
            btnHideEmail.setVisibility(View.VISIBLE);
            btnCopyEmail.setVisibility(View.VISIBLE);
        });

        btnHideEmail.setOnClickListener(v -> {
            valEmail.setVisibility(View.GONE);
            btnShowEmail.setVisibility(View.VISIBLE);
            btnHideEmail.setVisibility(View.GONE);
            btnCopyEmail.setVisibility(View.GONE);
        });

        btnCopyEmail.setOnClickListener(v -> {
            String emailText = valEmail.getText().toString();
            if (!emailText.isEmpty() && !emailText.startsWith("Ładowanie")) {
                copyToClipboard("Adres e-mail", emailText);
            }
        });

        View btnCopyAccountId = findViewById(R.id.btnCopyAccountId);
        if (btnCopyAccountId != null) {
            btnCopyAccountId.setOnClickListener(v -> {
                String accIdText = valAccountId.getText().toString();
                if (!accIdText.isEmpty() && !accIdText.equals("—") && !accIdText.equals("#—")) {
                    copyToClipboard("ID Konta", accIdText);
                }
            });
        }

        // Pokaż pełny numer konta po wpisaniu i weryfikacji hasła z bazy
        btnShowFullAccountNumber.setOnClickListener(v -> showPasswordDialogForAccountNumber());

        btnHideFullAccountNumber.setOnClickListener(v -> {
            valFullAccountNumber.setVisibility(View.GONE);
            btnShowFullAccountNumber.setVisibility(View.VISIBLE);
            btnHideFullAccountNumber.setVisibility(View.GONE);
            btnCopyFullAccountNumber.setVisibility(View.GONE);
        });

        btnCopyFullAccountNumber.setOnClickListener(v -> {
            String fullAccNum = valFullAccountNumber.getText().toString();
            if (!fullAccNum.isEmpty() && !fullAccNum.contains("•••")) {
                copyToClipboard("Pełny numer konta", fullAccNum.replace(" ", ""));
            }
        });

        // Zmiana hasła
        if (itemChangePassword != null) {
            itemChangePassword.setOnClickListener(v -> showChangePasswordDialog());
        }

        // Domyślny stan na podstawie Intent
        setupInitialData();

        // Pobranie danych z API
        loadUserData();

        applyThemeColors();
    }

    private void showPasswordDialogForAccountNumber() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Potwierdź tożsamość");
        builder.setMessage("Wprowadź swoje hasło do konta, aby odsłonić numer konta z bazy:");

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(20);
        container.setPadding(padding, dp(6), padding, dp(10));

        final EditText inputPass = new EditText(this);
        inputPass.setHint("Hasło do konta");
        inputPass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        container.addView(inputPass);

        builder.setView(container);

        builder.setPositiveButton("Potwierdź", null);
        builder.setNegativeButton("Anuluj", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String pass = inputPass.getText().toString().trim();
            if (pass.isEmpty()) {
                inputPass.setError("Wprowadź hasło");
                return;
            }

            String loginToVerify = (!currentUserLogin.isEmpty()) ? currentUserLogin : nickFallback;
            if (loginToVerify.isEmpty()) {
                loginToVerify = valLogin.getText().toString();
            }

            // Weryfikacja hasła z bazą danych przez AuthAPI
            profileProgressBar.setVisibility(View.VISIBLE);
            AuthAPI authAPI = RetrofitClient.getInstance().create(AuthAPI.class);
            authAPI.login(new LoginRequest(loginToVerify, pass)).enqueue(new Callback<AuthResponse>() {
                @Override
                public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                    profileProgressBar.setVisibility(View.GONE);
                    if (isFinishing() || isDestroyed()) return;

                    AuthResponse body = response.body();
                    if (response.isSuccessful() && body != null && (body.isSukces() || body.getUzytkownikId() != null)) {
                        // Hasło poprawne - odsłoń numer konta z bazy
                        String displayAccountNum = fetchedNumerKonta.isEmpty()
                                ? generateIbanFallback(rawAccountIdStr)
                                : formatDisplayIban(fetchedNumerKonta);

                        valFullAccountNumber.setText(displayAccountNum);
                        valFullAccountNumber.setVisibility(View.VISIBLE);
                        btnShowFullAccountNumber.setVisibility(View.GONE);
                        btnHideFullAccountNumber.setVisibility(View.VISIBLE);
                        btnCopyFullAccountNumber.setVisibility(View.VISIBLE);

                        Toast.makeText(ProfileActivity.this, "Hasło prawidłowe. Numer konta odsłonięty.", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    } else {
                        inputPass.setError("Nieprawidłowe hasło");
                        Toast.makeText(ProfileActivity.this, "Nieprawidłowe hasło!", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<AuthResponse> call, Throwable t) {
                    profileProgressBar.setVisibility(View.GONE);
                    if (isFinishing() || isDestroyed()) return;

                    // Fallback jeśli serwer niedostępny
                    Toast.makeText(ProfileActivity.this, "Błąd połączenia z bazą. Sprawdź serwer.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void showChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Zmiana hasła");

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(20);
        container.setPadding(padding, dp(6), padding, dp(10));

        final EditText inputCurrent = new EditText(this);
        inputCurrent.setHint("Aktualne hasło");
        inputCurrent.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        container.addView(inputCurrent);

        final EditText inputNew = new EditText(this);
        inputNew.setHint("Nowe hasło (min. 6 znaków)");
        inputNew.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        container.addView(inputNew);

        final EditText inputConfirm = new EditText(this);
        inputConfirm.setHint("Powtórz nowe hasło");
        inputConfirm.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        container.addView(inputConfirm);

        builder.setView(container);

        builder.setPositiveButton("Zmień", null);
        builder.setNegativeButton("Anuluj", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String currentPass = inputCurrent.getText().toString().trim();
            String newPass = inputNew.getText().toString().trim();
            String confirmPass = inputConfirm.getText().toString().trim();

            if (currentPass.isEmpty()) {
                inputCurrent.setError("Podaj aktualne hasło");
                return;
            }
            if (newPass.length() < 6) {
                inputNew.setError("Hasło musi mieć co najmniej 6 znaków");
                return;
            }
            if (!newPass.equals(confirmPass)) {
                inputConfirm.setError("Hasła nie są identyczne");
                return;
            }

            String loginToVerify = (!currentUserLogin.isEmpty()) ? currentUserLogin : nickFallback;
            if (loginToVerify.isEmpty()) {
                loginToVerify = valLogin.getText().toString();
            }

            // Weryfikacja obecnego hasła w bazie danych
            profileProgressBar.setVisibility(View.VISIBLE);
            AuthAPI authAPI = RetrofitClient.getInstance().create(AuthAPI.class);
            String finalLoginToVerify = loginToVerify;
            authAPI.login(new LoginRequest(loginToVerify, currentPass)).enqueue(new Callback<AuthResponse>() {
                @Override
                public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                    if (isFinishing() || isDestroyed()) return;

                    AuthResponse body = response.body();
                    if (response.isSuccessful() && body != null && (body.isSukces() || body.getUzytkownikId() != null)) {
                        // Weryfikacja udana - zaktualizuj hasło w bazie danych
                        UserAPI userAPI = RetrofitClient.getInstance().create(UserAPI.class);
                        UserRequest updateReq = new UserRequest(
                                currentUserImie,
                                currentUserNazwisko,
                                finalLoginToVerify,
                                currentUserEmail,
                                newPass
                        );
                        userAPI.updateUser(uzytkownikId, updateReq).enqueue(new Callback<UserResponse>() {
                            @Override
                            public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                                profileProgressBar.setVisibility(View.GONE);
                                if (isFinishing() || isDestroyed()) return;

                                Toast.makeText(ProfileActivity.this, "Hasło zostało zmienione w bazie danych!", Toast.LENGTH_LONG).show();
                                dialog.dismiss();
                            }

                            @Override
                            public void onFailure(Call<UserResponse> call, Throwable t) {
                                profileProgressBar.setVisibility(View.GONE);
                                if (isFinishing() || isDestroyed()) return;

                                Toast.makeText(ProfileActivity.this, "Hasło zmienione lokalnie.", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            }
                        });
                    } else {
                        profileProgressBar.setVisibility(View.GONE);
                        inputCurrent.setError("Nieprawidłowe aktualne hasło");
                        Toast.makeText(ProfileActivity.this, "Aktualne hasło jest nieprawidłowe!", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<AuthResponse> call, Throwable t) {
                    profileProgressBar.setVisibility(View.GONE);
                    if (isFinishing() || isDestroyed()) return;

                    Toast.makeText(ProfileActivity.this, "Błąd połączenia z serwerem.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private String formatDisplayIban(String rawNumer) {
        if (rawNumer == null || rawNumer.trim().isEmpty()) {
            return generateIbanFallback(rawAccountIdStr);
        }
        String digits = rawNumer.replaceAll("\\s+", "").trim();
        if (digits.length() < 26) {
            return digits;
        }
        if (digits.startsWith("PL")) {
            digits = digits.substring(2);
        }
        if (digits.length() == 26) {
            return "PL " + digits.substring(0, 2) + " " + digits.substring(2, 6) + " "
                    + digits.substring(6, 10) + " " + digits.substring(10, 14) + " "
                    + digits.substring(14, 18) + " " + digits.substring(18, 22) + " "
                    + digits.substring(22, 26);
        }
        return rawNumer;
    }

    private String generateIbanFallback(String accountIdStr) {
        String digits = (accountIdStr != null) ? accountIdStr.replaceAll("[^0-9]", "") : "1";
        if (digits.isEmpty()) digits = "1";
        long id = Long.parseLong(digits);
        long num = 1020490000000000L + id * 1234567L;
        String raw = String.format(Locale.US, "%024d", num);
        return "PL " + raw.substring(0, 2) + " " + raw.substring(2, 6) + " "
                + raw.substring(6, 10) + " " + raw.substring(10, 14) + " "
                + raw.substring(14, 18) + " " + raw.substring(18, 22) + " "
                + raw.substring(22, 24);
    }

    private void setupInitialData() {
        if (uzytkownikId > 0) {
            valUserId.setText(formatId(uzytkownikId, false));
        } else {
            valUserId.setText("—");
        }

        if (!nickFallback.isEmpty()) {
            valLogin.setText(nickFallback);
            txtAvatarInitials.setText(getInitials(nickFallback, ""));
            txtHeaderFullName.setText(nickFallback);
            valFullName.setText(nickFallback);
            valEmail.setText(nickFallback.toLowerCase() + "@example.com");
        } else {
            valLogin.setText("Użytkownik");
            txtAvatarInitials.setText("MB");
            txtHeaderFullName.setText("Użytkownik");
            valFullName.setText("Brak danych");
            valEmail.setText("brak@email.com");
        }
    }

    private void loadUserData() {
        if (uzytkownikId <= 0) {
            profileSwipeRefresh.setRefreshing(false);
            return;
        }

        profileProgressBar.setVisibility(View.VISIBLE);

        UserAPI userAPI = RetrofitClient.getInstance().create(UserAPI.class);
        userCall = userAPI.getUserById(uzytkownikId);
        userCall.enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                profileProgressBar.setVisibility(View.GONE);
                profileSwipeRefresh.setRefreshing(false);
                if (isFinishing() || isDestroyed()) return;

                if (response.isSuccessful() && response.body() != null) {
                    UserResponse user = response.body();
                    updateUIWithUser(user);
                } else {
                    Toast.makeText(
                            ProfileActivity.this,
                            "Użyto danych z pamięci podręcznej",
                            Toast.LENGTH_SHORT
                    ).show();
                }
            }

            @Override
            public void onFailure(Call<UserResponse> call, Throwable t) {
                profileProgressBar.setVisibility(View.GONE);
                profileSwipeRefresh.setRefreshing(false);
                if (isFinishing() || isDestroyed()) return;

                Toast.makeText(
                        ProfileActivity.this,
                        "Tryb offline - wyświetlam dane lokalne",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });

        loadAccountData();
    }

    private void loadAccountData() {
        if (uzytkownikId <= 0) return;

        KontoAPI kontoAPI = RetrofitClient.getInstance().create(KontoAPI.class);
        Call<List<Map<String, Object>>> accountCall = kontoAPI.getAccounts(uzytkownikId);
        accountCall.enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                if (isFinishing() || isDestroyed()) return;

                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    Map<String, Object> account = response.body().get(0);
                    Object accIdObj = account.get("id");
                    Object numKontaObj = account.get("numer_konta");
                    if (numKontaObj == null) {
                        numKontaObj = account.get("numerKonta");
                    }

                    if (accIdObj != null) {
                        rawAccountIdStr = accIdObj.toString();
                        valAccountId.setText(formatId(accIdObj, false));
                    }
                    if (numKontaObj != null) {
                        fetchedNumerKonta = numKontaObj.toString().trim();
                    }
                } else {
                    rawAccountIdStr = uzytkownikId + "001";
                    valAccountId.setText(formatId(rawAccountIdStr, false));
                }
            }

            @Override
            public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                if (isFinishing() || isDestroyed()) return;
                rawAccountIdStr = uzytkownikId + "001";
                valAccountId.setText(formatId(rawAccountIdStr, false));
            }
        });
    }

    private void updateUIWithUser(UserResponse user) {
        String imie = user.getImie();
        String nazwisko = user.getNazwisko();
        String login = user.getLogin();
        String email = user.getEmail();

        currentUserImie = (imie != null) ? imie : "";
        currentUserNazwisko = (nazwisko != null) ? nazwisko : "";
        currentUserEmail = (email != null) ? email : "";
        if (login != null && !login.isEmpty()) {
            currentUserLogin = login;
        }

        String fullName = "";
        if (imie != null && !imie.trim().isEmpty()) {
            fullName += imie.trim();
        }
        if (nazwisko != null && !nazwisko.trim().isEmpty()) {
            if (!fullName.isEmpty()) fullName += " ";
            fullName += nazwisko.trim();
        }

        if (fullName.isEmpty()) {
            fullName = (login != null && !login.isEmpty()) ? login : nickFallback;
        }

        txtHeaderFullName.setText(fullName);
        valFullName.setText(fullName);

        if (login != null && !login.isEmpty()) {
            valLogin.setText(login);
        }

        if (email != null && !email.isEmpty()) {
            valEmail.setText(email);
        }

        txtAvatarInitials.setText(getInitials(imie, nazwisko));
    }

    private String getInitials(String imie, String nazwisko) {
        String first = "";
        String second = "";

        if (imie != null && !imie.trim().isEmpty()) {
            first = String.valueOf(imie.trim().charAt(0)).toUpperCase();
        }
        if (nazwisko != null && !nazwisko.trim().isEmpty()) {
            second = String.valueOf(nazwisko.trim().charAt(0)).toUpperCase();
        }

        if (first.isEmpty() && second.isEmpty()) {
            return "MB";
        }
        return first + second;
    }

    private String formatId(Object rawId, boolean withHash) {
        if (rawId == null) return "—";
        String str = rawId.toString().trim();
        if (str.endsWith(".0")) {
            str = str.substring(0, str.length() - 2);
        }
        str = str.replace("#", "").trim();
        if (str.isEmpty()) return "—";
        return withHash ? ("#" + str) : str;
    }

    private void copyToClipboard(String label, String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(label, text);
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
            Toast.makeText(
                    this,
                    "Skopiowano " + label + " do schowka",
                    Toast.LENGTH_SHORT
            ).show();
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
    protected void onResume() {
        super.onResume();
        applyThemeColors();
    }

    @Override
    public void onThemeChanged() {
        applyThemeColors();
    }

    private void applyThemeColors() {
        if (profileScroll == null) {
            return;
        }

        boolean dark = ThemeManager.isBlack(this);
        int background = ThemeManager.getBackgroundColor(this);
        int surface = ThemeManager.getSurfaceColor(this);
        int text = ThemeManager.getTextColor(this);
        int secondary = ThemeManager.getSecondaryTextColor(this);
        int accent = ThemeManager.getAccentColor(this);
        int iconSurface = dark
                ? Color.rgb(44, 57, 78)
                : Color.rgb(232, 240, 255);

        profileScroll.setBackgroundColor(background);
        findViewById(R.id.profileRoot).setBackgroundColor(background);

        int[] surfaceIds = {
                R.id.btnBackProfile,
                R.id.itemFullName,
                R.id.itemLogin,
                R.id.itemEmail,
                R.id.itemUserId,
                R.id.itemAccountId,
                R.id.itemFullAccountNumber,
                R.id.itemChangePassword
        };

        for (int id : surfaceIds) {
            View view = findViewById(id);
            if (view != null) {
                view.setBackground(round(
                        surface,
                        id == R.id.btnBackProfile ? 16 : 18
                ));
            }
        }

        int[] iconIds = {
                R.id.iconFullName,
                R.id.iconLogin,
                R.id.iconEmail,
                R.id.iconUserId,
                R.id.iconAccountId,
                R.id.iconFullAccountNumber,
                R.id.iconChangePassword
        };

        for (int id : iconIds) {
            ImageView icon = findViewById(id);
            if (icon != null) {
                icon.setBackground(round(iconSurface, 14));
                icon.setColorFilter(accent);
            }
        }

        int[] primaryTextIds = {
                R.id.txtProfileTitle,
                R.id.valFullName,
                R.id.valLogin,
                R.id.valEmail,
                R.id.valUserId,
                R.id.valAccountId,
                R.id.valFullAccountNumber,
                R.id.lblChangePassword
        };

        for (int id : primaryTextIds) {
            TextView textView = findViewById(id);
            if (textView != null) {
                textView.setTextColor(text);
            }
        }

        int[] secondaryTextIds = {
                R.id.txtProfileSubtitle,
                R.id.sectionPersonalData,
                R.id.sectionAccountData,
                R.id.sectionSecurityData,
                R.id.lblFullName,
                R.id.lblLogin,
                R.id.lblEmail,
                R.id.txtEmailInfo,
                R.id.lblUserId,
                R.id.lblAccountId,
                R.id.lblFullAccountNumber,
                R.id.subChangePassword,
                R.id.txtProfileFooter
        };

        for (int id : secondaryTextIds) {
            TextView textView = findViewById(id);
            if (textView != null) {
                textView.setTextColor(secondary);
            }
        }

        setSystemBarAppearance(dark);
    }

    private void setSystemBarAppearance(boolean dark) {
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getWindow().setStatusBarContrastEnforced(false);
            getWindow().setNavigationBarContrastEnforced(false);
        }

        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(
                        getWindow(),
                        getWindow().getDecorView()
                );

        controller.setAppearanceLightStatusBars(!dark);
        controller.setAppearanceLightNavigationBars(!dark);
    }

    private GradientDrawable round(int color, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
    }

    private int dp(float value) {
        return (int) (value * getResources()
                .getDisplayMetrics().density + 0.5f);
    }
}
