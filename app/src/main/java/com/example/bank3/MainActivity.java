package com.example.bank3;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class MainActivity extends AppCompatActivity {

    private TextInputLayout loginLayout;
    private TextInputLayout hasloLayout;
    //aaaa

    private TextInputEditText loginInput;
    private TextInputEditText hasloInput;

    private TextView errorText;
    private Button btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Pola tekstowe
        loginInput = findViewById(R.id.editLogin);
        hasloInput = findViewById(R.id.editHaslo);

        // Kontenery pól (jeśli masz je w XML)
        loginLayout = findViewById(R.id.loginLayout);
        hasloLayout = findViewById(R.id.hasloLayout);

        // Pozostałe elementy
        errorText = findViewById(R.id.errorText);
        btnLogin = findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(v -> {

            String login = loginInput.getText() != null
                    ? loginInput.getText().toString().trim()
                    : "";

            String haslo = hasloInput.getText() != null
                    ? hasloInput.getText().toString()
                    : "";

            // Czyścimy poprzednie błędy
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

            // Przykładowe dane logowania
            if (login.equals("admin") && haslo.equals("1234")) {

                errorText.setTextColor(
                        getColor(android.R.color.holo_green_dark)
                );

                errorText.setText("Zalogowano pomyślnie!");

            } else {

                errorText.setTextColor(
                        getColor(android.R.color.holo_red_dark)
                );

                errorText.setText("Nieprawidłowy login lub hasło.");
            }
        });
    }


}