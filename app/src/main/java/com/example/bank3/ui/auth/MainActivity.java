package com.example.bank3.ui.auth;

import android.content.Intent;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);

        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(
                        getWindow(),
                        getWindow().getDecorView()
                );

        controller.setAppearanceLightStatusBars(true);
        controller.setAppearanceLightNavigationBars(true);

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
    }

    private void login() {
        String login = loginInput.getText() != null
                ? loginInput.getText().toString().trim()
                : "";

        String haslo = hasloInput.getText() != null
                ? hasloInput.getText().toString()
                : "";

        clearErrors();

        if (login.isEmpty()) {
            loginLayout.setError("Podaj login.");
            loginInput.requestFocus();
            return;
        }

        if (haslo.isEmpty()) {
            hasloLayout.setError("Podaj hasło.");
            hasloInput.requestFocus();
            return;
        }

        setLoading(true);

        LoginRequest request = new LoginRequest(login, haslo);

        authRepository.login(request).enqueue(new Callback<AuthResponse>() {

            @Override
            public void onResponse(
                    Call<AuthResponse> call,
                    Response<AuthResponse> response
            ) {
                setLoading(false);

                AuthResponse body = response.body();

                if (response.isSuccessful()
                        && body != null
                        && body.isSukces()) {

                    Intent intent = new Intent(
                            MainActivity.this,
                            MainMenuActivity.class
                    );

                    intent.putExtra("nick", login);
                    startActivity(intent);

                } else if (response.code() == 401) {
                    showError("Nieprawidłowy login lub hasło.");

                } else if (body != null
                        && body.getKomunikat() != null
                        && !body.getKomunikat().isEmpty()) {

                    showError(body.getKomunikat());

                } else {
                    showError(
                            "Błąd serwera. Kod HTTP: " + response.code()
                    );
                }
            }

            @Override
            public void onFailure(
                    Call<AuthResponse> call,
                    Throwable throwable
            ) {
                setLoading(false);
                showError("Nie można połączyć się z serwerem.");
            }
        });
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
        btnLogin.setEnabled(!loading);
        btnRegister.setEnabled(!loading);

        btnLogin.setText(
                loading ? "Logowanie..." : "Zaloguj się"
        );
    }
}