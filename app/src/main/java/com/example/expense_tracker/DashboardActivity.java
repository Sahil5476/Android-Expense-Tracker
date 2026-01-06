package com.example.expense_tracker;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class DashboardActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private FloatingActionButton fabAdd;
    private TextView tvWelcome, tvTotalExpense, tvInvested, tvLoanDr, tvLoanCr;
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
        tvTotalExpense = findViewById(R.id.tvTotalExpense);
        tvInvested = findViewById(R.id.tvInvested);
        tvLoanDr = findViewById(R.id.tvLoanDr);
        tvLoanCr = findViewById(R.id.tvLoanCr);

        // 2. Design Fixes
        bottomNavigationView.setBackground(null);
        // This line disables the placeholder item in the middle (if using a curved bottom bar)
        bottomNavigationView.getMenu().getItem(2).setEnabled(false);

        // 3. Load User Name from Firebase
        if (mAuth.getCurrentUser() != null) {
            db.collection("users").document(mAuth.getCurrentUser().getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            tvWelcome.setText("Hello, " + documentSnapshot.getString("name"));
                        }
                    });
        }

        // 4. Navigation Logic
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) return true;
            if (id == R.id.nav_profile) {
                mAuth.signOut();
                startActivity(new Intent(DashboardActivity.this, LoginPage.class));
                finish();
                return true;
            }
            return false;
        });

        // 5. FIXED: FAB Click now opens the new Activity
        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, AddTransactionActivity.class);
            startActivity(intent);
        });
    }
}