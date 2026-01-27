package com.example.expense_tracker;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class DashboardActivity extends AppCompatActivity {

    // UI Components
    private BottomNavigationView bottomNavigationView;
    private FloatingActionButton fabAdd;
    private TextView tvWelcome, tvTotalExpense, tvInvested, tvLoanDr, tvLoanCr, tvViewAll;
    private View dashboardContent, fragmentContainer;

    // Recent List Components
    private RecyclerView recyclerRecent;
    private TransactionAdapter recentAdapter;

    // Firebase
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
        tvViewAll = findViewById(R.id.tvViewAll);

        // Layouts
        dashboardContent = findViewById(R.id.dashboard_content);
        fragmentContainer = findViewById(R.id.fragment_container);

        // 2. Setup RecyclerView
        recyclerRecent = findViewById(R.id.recyclerRecent);
        recyclerRecent.setLayoutManager(new LinearLayoutManager(this));

        // Initialize with empty adapter to avoid logs
        recentAdapter = new TransactionAdapter(this, new ArrayList<>(), null);
        recentAdapter.setReadOnly(true);
        recyclerRecent.setAdapter(recentAdapter);

        // 3. Navigation Bar Design
        bottomNavigationView.setBackground(null);
        bottomNavigationView.getMenu().getItem(2).setEnabled(false); // Disable placeholder

        // 4. Load User Name
        loadUserData();

        // 5. Navigation Logic
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                // Show Home Dashboard
                dashboardContent.setVisibility(View.VISIBLE);
                fragmentContainer.setVisibility(View.GONE);
                refreshDashboardData();
                return true;
            } else if (id == R.id.nav_stats) {
                loadFragment(new StatsFragment());
                return true;
            } else if (id == R.id.nav_wallet) {
                loadFragment(new WalletFragment());
                return true;
            } else if (id == R.id.nav_profile) {
                // --- UPDATE: Load ProfileFragment instead of direct logout ---
                loadFragment(new ProfileFragment());
                return true;
            }
            return false;
        });

        // 6. FAB Logic
        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, AddTransactionActivity.class);
            startActivity(intent);
        });

        // 7. "View All" Logic -> Redirect to Wallet
        tvViewAll.setOnClickListener(v -> {
            bottomNavigationView.setSelectedItemId(R.id.nav_wallet);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Only refresh data if the dashboard is visible
        if (dashboardContent.getVisibility() == View.VISIBLE) {
            refreshDashboardData();
        }
    }

    private void loadFragment(Fragment fragment) {
        dashboardContent.setVisibility(View.GONE);
        fragmentContainer.setVisibility(View.VISIBLE);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    private void loadUserData() {
        if (mAuth.getCurrentUser() != null) {
            db.collection("users").document(mAuth.getCurrentUser().getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            tvWelcome.setText("Hello, " + documentSnapshot.getString("name"));
                        }
                    });
        }
    }

    // --- Core Logic: Fetch Data and Update UI ---
    private void refreshDashboardData() {
        Executors.newSingleThreadExecutor().execute(() -> {
            // 1. Get Data from Room Database
            List<Transaction> allTransactions = AppDatabase.getDatabase(this).transactionDao().getAllTransactions();

            // 2. Calculate Totals
            double totalExp = 0, totalInv = 0, totalLiability = 0, totalAsset = 0;

            for (Transaction t : allTransactions) {
                if (t.type.equals("Expense")) totalExp += t.amount;
                else if (t.type.equals("Investment")) totalInv += t.amount;
                else if (t.type.equals("Liability")) totalLiability += t.amount;
                else if (t.type.equals("Asset")) totalAsset += t.amount;
            }

            // 3. Prepare Recent List (Top 5 only)
            List<Transaction> recentList = new ArrayList<>();
            int limit = Math.min(allTransactions.size(), 5);
            for (int i = 0; i < limit; i++) {
                recentList.add(allTransactions.get(i));
            }

            // Final variables for UI thread
            double fExp = totalExp;
            double fInv = totalInv;
            double fLiab = totalLiability;
            double fAsset = totalAsset;

            // 4. Update UI
            runOnUiThread(() -> {
                tvTotalExpense.setText(String.format("₹ %.2f", fExp));
                tvInvested.setText(String.format("₹ %.2f", fInv));
                tvLoanDr.setText(String.format("₹ %.2f", fLiab));
                tvLoanCr.setText(String.format("₹ %.2f", fAsset));

                // Create Adapter with Click Listener -> Redirects to Wallet
                recentAdapter = new TransactionAdapter(this, recentList, transaction -> {
                    bottomNavigationView.setSelectedItemId(R.id.nav_wallet);
                });

                // IMPORTANT: Hide Delete Button
                recentAdapter.setReadOnly(true);

                recyclerRecent.setAdapter(recentAdapter);
            });
        });
    }
}