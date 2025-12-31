package com.example.expense_tracker;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.auth.FirebaseAuth;

public class DashboardActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private FloatingActionButton fabAdd;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // 1. Initialize Firebase
        mAuth = FirebaseAuth.getInstance();

        // 2. Link Views
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        fabAdd = findViewById(R.id.fab_add);

        // --- CRITICAL DESIGN FIXES ---
        // This removes the background so the "Cradle" curve is visible
        bottomNavigationView.setBackground(null);
        // This disables the empty middle icon (Placeholder) so it can't be clicked
        bottomNavigationView.getMenu().getItem(2).setEnabled(false);
        // -----------------------------

        // 3. Handle Bottom Navigation Clicks
        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();

                if (id == R.id.nav_home) {
                    Toast.makeText(DashboardActivity.this, "Home Clicked", Toast.LENGTH_SHORT).show();
                    // Later: loadFragment(new HomeFragment());
                    return true;
                }
                else if (id == R.id.nav_stats) {
                    Toast.makeText(DashboardActivity.this, "Stats Clicked", Toast.LENGTH_SHORT).show();
                    // Later: loadFragment(new StatsFragment());
                    return true;
                }
                else if (id == R.id.nav_wallet) {
                    Toast.makeText(DashboardActivity.this, "Wallet Clicked", Toast.LENGTH_SHORT).show();
                    // Later: loadFragment(new WalletFragment());
                    return true;
                }
                else if (id == R.id.nav_profile) {
                    // For now, we use Profile button to LOGOUT
                    mAuth.signOut();
                    Intent intent = new Intent(DashboardActivity.this, LoginPage.class);
                    startActivity(intent);
                    finish();
                    return true;
                }

                return false;
            }
        });

        // 4. Handle Add Button (FAB) Click
        fabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(DashboardActivity.this, "Add Transaction Clicked", Toast.LENGTH_SHORT).show();
                // Later: Intent intent = new Intent(DashboardActivity.this, AddTransactionActivity.class);
                // startActivity(intent);
            }
        });
    }
}