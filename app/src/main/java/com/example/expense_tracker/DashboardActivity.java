package com.example.expense_tracker;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.concurrent.Executors;

public class DashboardActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private FloatingActionButton fabAdd;
    private TextView tvWelcome, tvTotalExpense, tvInvested, tvLoanDr, tvLoanCr;
    private View dashboardContent, fragmentContainer;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // 1. Link Views
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        fabAdd = findViewById(R.id.fab_add);
        tvWelcome = findViewById(R.id.tvWelcome);

        // Cards
        tvTotalExpense = findViewById(R.id.tvTotalExpense);
        tvInvested = findViewById(R.id.tvInvested);
        tvLoanDr = findViewById(R.id.tvLoanDr);
        tvLoanCr = findViewById(R.id.tvLoanCr);

        // Layout Containers (For switching tabs)
        dashboardContent = findViewById(R.id.dashboard_content);
        fragmentContainer = findViewById(R.id.fragment_container);

        // 2. Disable Placeholder Item (The middle item, index 2)
        bottomNavigationView.setBackground(null);
        bottomNavigationView.getMenu().getItem(2).setEnabled(false);

        // 3. Load User Name (Firebase)
        if (mAuth.getCurrentUser() != null) {
            db.collection("users").document(mAuth.getCurrentUser().getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            tvWelcome.setText("Hello, " + documentSnapshot.getString("name"));
                        }
                    });
        }

        // 4. Navigation Logic (Matches your menu XML IDs)
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                // Show Dashboard, Hide Fragments
                dashboardContent.setVisibility(View.VISIBLE);
                fragmentContainer.setVisibility(View.GONE);
                refreshDashboardTotals();
                return true;
            }
            else if (id == R.id.nav_stats) {
                // Open Statistics
                loadFragment(new StatsFragment());
                return true;
            }
            else if (id == R.id.nav_wallet) {
                // Open Wallet History
                loadFragment(new WalletFragment());
                return true;
            }
            else if (id == R.id.nav_profile) {
                // Logout Logic
                mAuth.signOut();
                startActivity(new Intent(DashboardActivity.this, LoginPage.class));
                finish();
                return true;
            }
            return false;
        });

        // 5. FAB Click (Add Transaction)
        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, AddTransactionActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh totals whenever we come back to the dashboard
        if (dashboardContent.getVisibility() == View.VISIBLE) {
            refreshDashboardTotals();
        }
    }

    // --- Helper: Switch Fragments cleanly ---
    private void loadFragment(Fragment fragment) {
        dashboardContent.setVisibility(View.GONE);
        fragmentContainer.setVisibility(View.VISIBLE);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    // --- Helper: Calculate Totals from Room DB ---
    private void refreshDashboardTotals() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Transaction> allTransactions = AppDatabase.getDatabase(this).transactionDao().getAllTransactions();

            double totalExp = 0;
            double totalInv = 0;
            double totalLiability = 0;
            double totalAsset = 0;

            for (Transaction t : allTransactions) {
                if (t.type.equals("Expense")) {
                    totalExp += t.amount;
                } else if (t.type.equals("Investment")) {
                    totalInv += t.amount;
                } else if (t.type.equals("Liability")) {
                    totalLiability += t.amount;
                } else if (t.type.equals("Asset")) {
                    totalAsset += t.amount;
                }
            }

            double finalTotalExp = totalExp;
            double finalTotalInv = totalInv;
            double finalTotalLiability = totalLiability;
            double finalTotalAsset = totalAsset;

            runOnUiThread(() -> {
                tvTotalExpense.setText(String.format("₹ %.2f", finalTotalExp));
                tvInvested.setText(String.format("₹ %.2f", finalTotalInv));
                tvLoanDr.setText(String.format("₹ %.2f", finalTotalLiability));
                tvLoanCr.setText(String.format("₹ %.2f", finalTotalAsset));
            });
        });
    }
}