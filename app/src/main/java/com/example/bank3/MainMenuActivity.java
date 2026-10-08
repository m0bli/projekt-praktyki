package com.example.bank3;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;

public class MainMenuActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

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
