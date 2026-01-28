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
import com.google.firebase.auth.FirebaseUser;

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

    // Data Sources
    private FirebaseAuth mAuth;
    private AppDatabase localDb; // Replaced Firestore with Local Room DB for speed & offline support

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // 1. Initialize Data Sources
        mAuth = FirebaseAuth.getInstance();
        localDb = AppDatabase.getDatabase(this);

        // 2. Link Views
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

        // 3. Setup RecyclerView
        recyclerRecent = findViewById(R.id.recyclerRecent);
        recyclerRecent.setLayoutManager(new LinearLayoutManager(this));

        // Initialize with empty adapter to avoid logs
        recentAdapter = new TransactionAdapter(this, new ArrayList<>(), null);
        recentAdapter.setReadOnly(true);
        recyclerRecent.setAdapter(recentAdapter);

        // 4. Navigation Bar Design
        bottomNavigationView.setBackground(null);
        bottomNavigationView.getMenu().getItem(2).setEnabled(false); // Disable placeholder

        // 5. Load Data Initially
        loadUserData();

        // 6. Navigation Logic
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                // Show Home Dashboard
                dashboardContent.setVisibility(View.VISIBLE);
                fragmentContainer.setVisibility(View.GONE);
                refreshDashboardData();
                // Also reload name in case it changed
                loadUserData();
                return true;
            } else if (id == R.id.nav_stats) {
                loadFragment(new StatsFragment());
                return true;
            } else if (id == R.id.nav_wallet) {
                loadFragment(new WalletFragment());
                return true;
            } else if (id == R.id.nav_profile) {
                loadFragment(new ProfileFragment());
                return true;
            }
            return false;
        });

        // 7. FAB Logic
        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, AddTransactionActivity.class);
            startActivity(intent);
        });

        // 8. "View All" Logic -> Redirect to Wallet
        tvViewAll.setOnClickListener(v -> {
            bottomNavigationView.setSelectedItemId(R.id.nav_wallet);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data whenever the dashboard becomes visible again
        if (dashboardContent.getVisibility() == View.VISIBLE) {
            refreshDashboardData();
            loadUserData(); // FIX: Update name immediately if changed in Profile
        }
    }

    private void loadFragment(Fragment fragment) {
        dashboardContent.setVisibility(View.GONE);
        fragmentContainer.setVisibility(View.VISIBLE);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    // --- FIX: Robust Name Loading (Local DB -> Google -> Email) ---
    private void loadUserData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        Executors.newSingleThreadExecutor().execute(() -> {
            // Priority 1: Check Local Database (User Edited Name)
            UserProfile savedProfile = localDb.userProfileDao().getUserProfile();

            String nameToDisplay = "User"; // Default

            if (savedProfile != null && savedProfile.name != null && !savedProfile.name.isEmpty()) {
                nameToDisplay = savedProfile.name;
            }
            // Priority 2: Google Account Name
            else if (user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
                nameToDisplay = user.getDisplayName();
            }
            // Priority 3: Extract from Email (e.g. "john" from "john@gmail.com")
            else if (user.getEmail() != null && user.getEmail().contains("@")) {
                nameToDisplay = user.getEmail().split("@")[0];
            }

            String finalName = nameToDisplay;
            runOnUiThread(() -> {
                tvWelcome.setText("Hello, " + capitalize(finalName));
            });
        });
    }

    // Helper to make names look nice (john -> John)
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    // --- Core Logic: Fetch Data and Update UI ---
    private void refreshDashboardData() {
        Executors.newSingleThreadExecutor().execute(() -> {
            // 1. Get Data from Room Database
            List<Transaction> allTransactions = localDb.transactionDao().getAllTransactions();

            // 2. Calculate Totals
            double totalExp = 0, totalInv = 0, totalLiability = 0, totalAsset = 0;

            for (Transaction t : allTransactions) {
                if ("Expense".equals(t.type)) totalExp += t.amount;
                else if ("Investment".equals(t.type)) totalInv += t.amount;
                else if ("Liability".equals(t.type)) totalLiability += t.amount;
                else if ("Asset".equals(t.type)) totalAsset += t.amount;
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