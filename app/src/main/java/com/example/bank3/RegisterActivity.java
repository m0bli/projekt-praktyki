package com.example.bank3;

import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.appbar.MaterialToolbar;

public class RegisterActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Enable edge-to-edge
        EdgeToEdge.enable(this);
        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());

        controller.setAppearanceLightStatusBars(true);
        controller.setAppearanceLightNavigationBars(true);


        // Handle status bar and navigation bar insets
        View main = findViewById(R.id.register);

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

        // Toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);

        // Back button on toolbar
        toolbar.setNavigationOnClickListener(v -> {
            getOnBackPressedDispatcher().onBackPressed();
        });

        // Toolbar container
        View toolbarContainer = findViewById(R.id.toolbarContainer);

        // Handle Android status bar / notification bar
        ViewCompat.setOnApplyWindowInsetsListener(
                toolbarContainer,
                (view, windowInsets) -> {

                    Insets statusBarInsets =
                            windowInsets.getInsets(
                                    WindowInsetsCompat.Type.statusBars()
                            );

                    view.setPadding(
                            view.getPaddingLeft(),
                            statusBarInsets.top,
                            view.getPaddingRight(),
                            view.getPaddingBottom()
                    );

                    return windowInsets;
                }
        );

        // Apply the insets immediately
        ViewCompat.requestApplyInsets(toolbarContainer);
    }
}
