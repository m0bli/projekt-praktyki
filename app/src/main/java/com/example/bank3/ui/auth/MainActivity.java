package com.example.bank3.ui.auth;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.example.bank3.R;
import com.example.bank3.data.repository.AuthRepository;
import com.example.bank3.model.AuthResponse;
import com.example.bank3.model.LoginRequest;
import com.example.bank3.ui.home.MainMenuActivity;
import com.example.bank3.ui.theme.ThemeManager;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private TextInputLayout loginLayout;
    private TextInputLayout hasloLayout;

    private TextInputEditText loginInput;
    private TextInputEditText hasloInput;

    private TextView errorText;
    private TextView btnRegister;
    private Button btnLogin;

    private AuthRepository authRepository;
    private Call<AuthResponse> loginCall;
    private boolean isLoading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_main);

        View main = findViewById(R.id.main);

        ViewCompat.setOnApplyWindowInsetsListener(main, (view, insets) -> {
            Insets systemBars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
            );

            view.setPadding(
                    systemBars.left,
                    systemBars.top,
                    systemBars.right,
                    systemBars.bottom
            );

            return insets;
        });

        loginInput = findViewById(R.id.editLogin);
        hasloInput = findViewById(R.id.editHaslo);

        loginLayout = findViewById(R.id.loginLayout);
        hasloLayout = findViewById(R.id.hasloLayout);

        errorText = findViewById(R.id.errorText);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);

        authRepository = new AuthRepository();

        btnRegister.setOnClickListener(v -> {
            Intent intent = new Intent(
                    MainActivity.this,
                    RegisterActivity.class
            );

            startActivity(intent);
        });

        btnLogin.setOnClickListener(v -> login());

        applyDynamicTheme();
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyDynamicTheme();
    }

    private void applyDynamicTheme() {
        boolean dark = ThemeManager.isBlack(this);
        int backgroundColor = ThemeManager.getBackgroundColor(this);
        int textColor = ThemeManager.getTextColor(this);
        int accentColor = ThemeManager.getAccentColor(this);

        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(
                        getWindow(),
                        getWindow().getDecorView()
                );

        controller.setAppearanceLightStatusBars(!dark);
        controller.setAppearanceLightNavigationBars(!dark);

        View main = findViewById(R.id.main);
        if (main != null) {
            main.setBackgroundColor(backgroundColor);
        }

        TextView bankTitle = findViewById(R.id.bank);
        if (bankTitle != null) {
            bankTitle.setTextColor(textColor);
        }

        if (btnRegister != null) {
            btnRegister.setTextColor(accentColor);
        }

        if (btnLogin != null) {
            btnLogin.setBackgroundTintList(
                    ColorStateList.valueOf(accentColor)
            );
        }

        if (loginLayout != null) {
            loginLayout.setBoxStrokeColor(accentColor);
            loginLayout.setHintTextColor(
                    ColorStateList.valueOf(accentColor)
            );
        }

        if (hasloLayout != null) {
            hasloLayout.setBoxStrokeColor(accentColor);
            hasloLayout.setHintTextColor(
                    ColorStateList.valueOf(accentColor)
            );
        }
    }

    private void login() {
        if (isLoading) {
            return;
        }

        String input = loginInput.getText() != null
                ? loginInput.getText().toString().trim()
                : "";

        String haslo = hasloInput.getText() != null
                ? hasloInput.getText().toString()
                : "";

        clearErrors();

        if (input.isEmpty()) {
            loginLayout.setError("Podaj login lub e-mail.");
            loginInput.requestFocus();
            return;
        }

        if (haslo.isEmpty()) {
            hasloLayout.setError("Podaj hasło.");
            hasloInput.requestFocus();
            return;
        }

        setLoading(true);

        attemptLogin(input, haslo, true);
    }

    private void attemptLogin(String input, String haslo, boolean tryUserLookupOnFail) {
        LoginRequest request = new LoginRequest(input, haslo);

        loginCall = authRepository.login(request);
        loginCall.enqueue(new Callback<AuthResponse>() {

            @Override
            public void onResponse(
                    Call<AuthResponse> call,
                    Response<AuthResponse> response
            ) {
                if (isFinishing() || isDestroyed() || call.isCanceled()) {
                    return;
                }

                AuthResponse body = response.body();

                if (response.isSuccessful()
                        && body != null
                        && body.isSukces()) {

                    setLoading(false);
                    Intent intent = new Intent(
                            MainActivity.this,
                            MainMenuActivity.class
                    );

                    intent.putExtra("nick", input);
                    if (body.getUzytkownikId() != null) {
                        intent.putExtra("uzytkownikId", body.getUzytkownikId());
                    }
                    startActivity(intent);
                    finish();

                } else if (tryUserLookupOnFail) {
                    lookupUserAndRetryLogin(input, haslo);
                } else {
                    setLoading(false);
                    showError("Nieprawidłowy login, e-mail lub hasło.");
                    btnLogin.setText("Spróbuj ponownie");
                }
            }

            @Override
            public void onFailure(
                    Call<AuthResponse> call,
                    Throwable throwable
            ) {
                if (call.isCanceled() || isFinishing() || isDestroyed()) {
                    return;
                }
                setLoading(false);
                showError("Nie można połączyć się z serwerem. Sprawdź połączenie i spróbuj ponownie.");
                btnLogin.setText("Spróbuj ponownie");
            }
        });
    }

    private void lookupUserAndRetryLogin(String input, String haslo) {
        com.example.bank3.data.api.UserAPI userAPI = com.example.bank3.network.RetrofitClient.getInstance()
                .create(com.example.bank3.data.api.UserAPI.class);

        userAPI.getUsers().enqueue(new Callback<java.util.List<com.example.bank3.model.UserResponse>>() {
            @Override
            public void onResponse(
                    Call<java.util.List<com.example.bank3.model.UserResponse>> call,
                    Response<java.util.List<com.example.bank3.model.UserResponse>> response
            ) {
                if (isFinishing() || isDestroyed() || call.isCanceled()) return;

                if (response.isSuccessful() && response.body() != null) {
                    com.example.bank3.model.UserResponse matchedUser = null;
                    for (com.example.bank3.model.UserResponse user : response.body()) {
                        if (user != null) {
                            if ((user.getEmail() != null && user.getEmail().equalsIgnoreCase(input))
                                    || (user.getLogin() != null && user.getLogin().equalsIgnoreCase(input))) {
                                matchedUser = user;
                                break;
                            }
                        }
                    }

                    if (matchedUser != null && matchedUser.getLogin() != null
                            && !matchedUser.getLogin().equalsIgnoreCase(input)) {
                        attemptLogin(matchedUser.getLogin(), haslo, false);
                        return;
                    }
                }

                setLoading(false);
                showError("Nieprawidłowy login, e-mail lub hasło.");
                btnLogin.setText("Spróbuj ponownie");
            }

            @Override
            public void onFailure(
                    Call<java.util.List<com.example.bank3.model.UserResponse>> call,
                    Throwable t
            ) {
                if (isFinishing() || isDestroyed() || call.isCanceled()) return;
                setLoading(false);
                showError("Nieprawidłowy login, e-mail lub hasło.");
                btnLogin.setText("Spróbuj ponownie");
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (loginCall != null && !loginCall.isCanceled()) {
            loginCall.cancel();
        }
        super.onDestroy();
    }

    private void clearErrors() {
        errorText.setText("");
        loginLayout.setError(null);
        hasloLayout.setError(null);
    }

    private void showError(String message) {
        errorText.setTextColor(
                getColor(android.R.color.holo_red_dark)
        );
        errorText.setText(message);
    }

    private void setLoading(boolean loading) {
        isLoading = loading;
        btnLogin.setEnabled(!loading);
        btnRegister.setEnabled(!loading);

        btnLogin.setText(
                loading ? "Logowanie..." : "Zaloguj się"
        );
    }
}