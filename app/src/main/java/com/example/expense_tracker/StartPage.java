package com.example.expense_tracker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;

public class StartPage extends AppCompatActivity {

    // Animation & Views
    Animation popAnim;
    ImageView logoImage;
    TextView sloganText;

    // Time to wait before changing screens (in milliseconds)
    private static int SPLASH_SCREEN_TIMEOUT = 3000; // 3 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Hide Title Bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_flash_screen);

        // 1. Hooks
        logoImage = findViewById(R.id.imageView);
        sloganText = findViewById(R.id.textView);

        // 2. Animations
        popAnim = AnimationUtils.loadAnimation(this, R.anim.pop_up);

        // Run Animations
        if (logoImage != null) {
            logoImage.startAnimation(popAnim);
        }

        if (sloganText != null) {
            // Delay text appearance slightly so it pops after logo
            new Handler().postDelayed(() -> {
                sloganText.setVisibility(View.VISIBLE);
                sloganText.startAnimation(popAnim);
            }, 800);
        }

        // 3. Automatic Navigation Logic
        // This handler waits 3 seconds, then decides where to go
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // Determine destination
                checkLoginAndNavigate();
            }
        }, SPLASH_SCREEN_TIMEOUT);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void checkLoginAndNavigate() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        boolean isLoggedIn = false;

        // Check Firebase User
        if (auth.getCurrentUser() != null) {
            SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
            long lastLoginTime = prefs.getLong("login_timestamp", 0);
            long currentTime = System.currentTimeMillis();
            long thirtyDaysInMillis = 30L * 24 * 60 * 60 * 1000;

            // Check 30 Day Expiry
            if ((currentTime - lastLoginTime) < thirtyDaysInMillis) {
                isLoggedIn = true;
            } else {
                // Expired: Sign out
                auth.signOut();
                isLoggedIn = false;
            }
        }

        // Navigate based on result
        if (isLoggedIn) {
            // User is valid -> Dashboard
            Intent intent = new Intent(StartPage.this, DashboardActivity.class);
            startActivity(intent);
        } else {
            // User not logged in (or expired) -> Login Page
            Intent intent = new Intent(StartPage.this, LoginPage.class);
            startActivity(intent);
        }

        // Close the Splash Screen so user can't go back to it
        finish();
    }
}