
package com.example.bank3;

import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private TextInputLayout imieLayout;
    private TextInputLayout nazwiskoLayout;
    private TextInputLayout loginLayout;
    private TextInputLayout emailLayout;
    private TextInputLayout hasloLayout;
    private TextInputLayout potwierdzHasloLayout;

    private TextInputEditText imieInput;
    private TextInputEditText nazwiskoInput;
    private TextInputEditText loginInput;
    private TextInputEditText emailInput;
    private TextInputEditText hasloInput;
    private TextInputEditText potwierdzHasloInput;

    private CheckBox checkboxRegister;
    private Button btnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(
                        getWindow(),
                        getWindow().getDecorView()
                );

        controller.setAppearanceLightStatusBars(true);
        controller.setAppearanceLightNavigationBars(true);

        View main = findViewById(R.id.register);

        ViewCompat.setOnApplyWindowInsetsListener(main, (view, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
            );

            view.setPadding(
                    bars.left,
                    bars.top,
                    bars.right,
                    bars.bottom
            );

            return insets;
        });

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        View toolbarContainer = findViewById(R.id.toolbarContainer);

        ViewCompat.setOnApplyWindowInsetsListener(
                toolbarContainer,
                (view, insets) -> {
                    Insets status = insets.getInsets(
                            WindowInsetsCompat.Type.statusBars()
                    );

                    view.setPadding(
                            view.getPaddingLeft(),
                            status.top,
                            view.getPaddingRight(),
                            view.getPaddingBottom()
                    );

                    return insets;
                }
        );

        ViewCompat.requestApplyInsets(toolbarContainer);

        imieLayout = findViewById(R.id.layoutRegisterImie);
        nazwiskoLayout = findViewById(R.id.layoutRegisterNazwisko);
        loginLayout = findViewById(R.id.layoutRegisterLogin);
        emailLayout = findViewById(R.id.layoutRegisterEmail);
        hasloLayout = findViewById(R.id.layoutRegisterHaslo);
        potwierdzHasloLayout = findViewById(R.id.layoutRegisterPotwierdzHaslo);

        imieInput = findViewById(R.id.editRegisterImie);
        nazwiskoInput = findViewById(R.id.editRegisterNazwisko);
        loginInput = findViewById(R.id.editRegisterLogin);
        emailInput = findViewById(R.id.editRegisterEmail);
        hasloInput = findViewById(R.id.editRegisterHaslo);
        potwierdzHasloInput = findViewById(R.id.editRegisterPotwierdzHaslo);

        checkboxRegister = findViewById(R.id.checkboxRegister);
        btnRegister = findViewById(R.id.btnRegister);

        btnRegister.setOnClickListener(v -> registerUser());
    }

    private void clearErrors() {
        imieLayout.setError(null);
        nazwiskoLayout.setError(null);
        loginLayout.setError(null);
        emailLayout.setError(null);
        hasloLayout.setError(null);
        potwierdzHasloLayout.setError(null);
    }

    private String valueOf(TextInputEditText input) {
        return input.getText() == null
                ? ""
                : input.getText().toString().trim();
    }

    private void registerUser() {
        clearErrors();

        String imie = valueOf(imieInput);
        String nazwisko = valueOf(nazwiskoInput);
        String login = valueOf(loginInput);
        String email = valueOf(emailInput);
        String haslo = hasloInput.getText() == null
                ? ""
                : hasloInput.getText().toString();
        String potwierdzHaslo =
                potwierdzHasloInput.getText() == null
                        ? ""
                        : potwierdzHasloInput.getText().toString();

        if (imie.isEmpty()) {
            imieLayout.setError("Podaj imię.");
            imieInput.requestFocus();
            return;
        }

        if (nazwisko.isEmpty()) {
            nazwiskoLayout.setError("Podaj nazwisko.");
            nazwiskoInput.requestFocus();
            return;
        }

        if (login.isEmpty()) {
            loginLayout.setError("Podaj login.");
            loginInput.requestFocus();
            return;
        }

        if (login.length() < 3) {
            loginLayout.setError(
                    "Login musi mieć co najmniej 3 znaki."
            );
            loginInput.requestFocus();
            return;
        }

        if (email.isEmpty()
                || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.setError("Podaj poprawny adres e-mail.");
            emailInput.requestFocus();
            return;
        }

        if (haslo.length() < 8) {
            hasloLayout.setError(
                    "Hasło musi mieć co najmniej 8 znaków."
            );
            hasloInput.requestFocus();
            return;
        }

        if (!haslo.equals(potwierdzHaslo)) {
            potwierdzHasloLayout.setError(
                    "Hasła nie są takie same."
            );
            potwierdzHasloInput.requestFocus();
            return;
        }

        if (!checkboxRegister.isChecked()) {
            Toast.makeText(
                    this,
                    "Zaakceptuj politykę prywatności.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        btnRegister.setEnabled(false);
        btnRegister.setText("Jesteś Rejestrowany...");

        UserRequest request = new UserRequest(
                imie,
                nazwisko,
                login,
                email,
                haslo
        );

        UserAPI userAPI = RetrofitClient.getInstance().create(UserAPI.class);

        userAPI.createUser(request).enqueue(new Callback<UserResponse>() {

            @Override
            public void onResponse(
                    Call<UserResponse> call,
                    Response<UserResponse> response
            ) {
                btnRegister.setEnabled(true);
                btnRegister.setText("Utwórz konto");

                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(
                            RegisterActivity.this,
                            "Konto utworzone. Zaloguj się.",
                            Toast.LENGTH_LONG
                    ).show();

                    // Powrót
                    finish();
                    return;
                }

                if (response.code() == 409) {
                    loginLayout.setError(
                            "Ten login jest już zajęty."
                    );
                    loginInput.requestFocus();
                } else if (response.code() == 400) {
                    Toast.makeText(
                            RegisterActivity.this,
                            "Sprawdź poprawność podanych danych.",
                            Toast.LENGTH_LONG
                    ).show();
                } else {
                    Toast.makeText(
                            RegisterActivity.this,
                            "Błąd serwera. HTTP "
                                    + response.code(),
                            Toast.LENGTH_LONG
                    ).show();
                }
            }

            @Override
            public void onFailure(
                    Call<UserResponse> call,
                    Throwable throwable
            ) {
                btnRegister.setEnabled(true);
                btnRegister.setText("Utwórz konto");

                Toast.makeText(
                        RegisterActivity.this,
                        "Nie można połączyć się z backendem. "
                                + "Sprawdź serwer i połączenie ADB.",
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }
}
