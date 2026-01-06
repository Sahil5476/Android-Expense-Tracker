package com.example.expense_tracker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;

public class StartPage extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --- 1. AUTO-LOGIN CHECK (30 DAYS LOGIC) ---
        checkAutoLogin();
        // -------------------------------------------

        // Hide Title Bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_start_page);

        Button startButton = findViewById(R.id.button);

        // --- 2. BUTTON REDIRECT ---
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(StartPage.this, LoginPage.class);
                startActivity(intent);
                // We do NOT call finish() here, so the user can come back if they want
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void checkAutoLogin() {
        FirebaseAuth auth = FirebaseAuth.getInstance();

        // Check if user is logged into Firebase
        if (auth.getCurrentUser() != null) {
            // User is technically logged in, now check the 30-day timer
            SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
            long lastLoginTime = prefs.getLong("login_timestamp", 0);
            long currentTime = System.currentTimeMillis();

            // 30 Days in Milliseconds: 30 * 24 hours * 60 min * 60 sec * 1000 ms
            long thirtyDaysInMillis = 30L * 24 * 60 * 60 * 1000;

            if ((currentTime - lastLoginTime) < thirtyDaysInMillis) {
                // SUCCESS: It has been less than 30 days. Go to Dashboard directly.
                Intent intent = new Intent(StartPage.this, DashboardActivity.class);
                startActivity(intent);
                finish(); // Close StartPage so back button doesn't bring them here
            } else {
                // EXPIRED: It has been more than 30 days. Force Logout.
                auth.signOut();
                // Stay on StartPage so they have to click "Get Started" and Login again.
            }
        }
    }
}