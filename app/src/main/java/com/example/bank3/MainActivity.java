package com.example.bank3;


import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class MainActivity extends AppCompatActivity {

    private TextInputLayout loginLayout;
    private TextInputLayout hasloLayout;
    //aaaa
    //kkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkk
    //nic

    private TextInputEditText loginInput;
    private TextInputEditText hasloInput;

    private TextView errorText;

    private TextView btnRegister;
    private Button btnLogin;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loginInput = findViewById(R.id.editLogin);
        hasloInput = findViewById(R.id.editHaslo);

        loginLayout = findViewById(R.id.loginLayout);
        hasloLayout = findViewById(R.id.hasloLayout);

        errorText = findViewById(R.id.errorText);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);

        btnRegister.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(intent);
        });



        btnLogin.setOnClickListener(v -> {

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
                loginLayout.setBoxStrokeErrorColor(ColorStateList.valueOf(Color.RED));
                loginLayout.setErrorTextColor(ColorStateList.valueOf(Color.RED));
                loginInput.requestFocus();
                return;
            }

            if (haslo.isEmpty()) {
                hasloLayout.setError("Podaj hasło.");
                hasloLayout.setBoxStrokeErrorColor(ColorStateList.valueOf(Color.RED));
                hasloLayout.setErrorTextColor(ColorStateList.valueOf(Color.RED));
                hasloInput.requestFocus();
                return;
            }

            if (login.equals("admin") && haslo.equals("aa")) {

                Intent intent = new Intent(MainActivity.this, MainMenuActivity.class);
                startActivity(intent);

            } else {

                errorText.setTextColor(
                        getColor(android.R.color.holo_red_dark)
                );

                errorText.setText("Nieprawidłowy login lub hasło.");
            }
        });
    }


}