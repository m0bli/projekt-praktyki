package com.example.bank3;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;


public class MainActivity extends AppCompatActivity {
    private TextInputLayout loginLayout;
    private TextInputLayout hasloLayout;

    private TextInputEditText loginInput;
    private TextInputEditText hasloInput;

    private TextView errorText;
    private TextView btnRegister;
    private Button btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Enable edge-to-edge
        EdgeToEdge.enable(this);
        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());

        controller.setAppearanceLightStatusBars(true);
        controller.setAppearanceLightNavigationBars(true);

        setContentView(R.layout.activity_main);

        // Handle status bar and navigation bar insets
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

        btnRegister.setOnClickListener(v -> {
            Intent intent = new Intent(
                    MainActivity.this,
                    RegisterActivity.class
            );
            startActivity(intent);
        });

        btnLogin.setOnClickListener(v -> {
            testApiConnection();

            String login = loginInput.getText() != null
                    ? loginInput.getText().toString().trim()
                    : "";

            String haslo = hasloInput.getText() != null
                    ? hasloInput.getText().toString()
                    : "";

            errorText.setText("");
            loginLayout.setError(null);
            hasloLayout.setError(null);

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

            // Blokujemy przycisk na czas requestu.
            btnLogin.setEnabled(false);

            LoginRequest request = new LoginRequest(login, haslo);

            AuthAPI authAPI = RetrofitClient.getInstance()
                    .create(AuthAPI.class);

            authAPI.login(request).enqueue(new retrofit2.Callback<AuthResponse>() {

                @Override
                public void onResponse(
                        retrofit2.Call<AuthResponse> call,
                        retrofit2.Response<AuthResponse> response) {

                    btnLogin.setEnabled(true);

                    AuthResponse body = response.body();

                    if (response.isSuccessful()
                            && body != null
                            && body.isSukces()) {

                        Intent intent = new Intent(
                                MainActivity.this,
                                MainMenuActivity.class
                        );

                        startActivity(intent);

                    } else if (response.code() == 401) {

                        errorText.setTextColor(
                                getColor(android.R.color.holo_red_dark)
                        );
                        errorText.setText("Nieprawidłowy login lub hasło.");

                    } else {

                        errorText.setTextColor(
                                getColor(android.R.color.holo_red_dark)
                        );
                        errorText.setText(
                                "Błąd serwera. Kod HTTP: " + response.code()
                        );
                    }
                }

                @Override
                public void onFailure(
                        retrofit2.Call<AuthResponse> call,
                        Throwable throwable) {

                    btnLogin.setEnabled(true);

                    errorText.setTextColor(
                            getColor(android.R.color.holo_red_dark)
                    );
                    errorText.setText(
                            "Nie można połączyć się z serwerem."
                    );
                }
            });
        });
    }

    private void testApiConnection() {
        UserAPI userAPI = RetrofitClient.getInstance().create(UserAPI.class);

        userAPI.getUsers().enqueue(new Callback<List<UserResponse>>() {
            @Override
            public void onResponse(Call<List<UserResponse>> call, Response<List<UserResponse>> response) {
                if (response.isSuccessful() && response.body() != null){
                    int liczbaUzytkownikow = response.body().size();

                    Toast.makeText(MainActivity.this, "dziala API. users: " + liczbaUzytkownikow, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(MainActivity.this, "nie dziala: HTTP " + response.code(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<List<UserResponse>> call, Throwable throwable) {
                Toast.makeText(MainActivity.this, "Brak połączenia: " + throwable.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}