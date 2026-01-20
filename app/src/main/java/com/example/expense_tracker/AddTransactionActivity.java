package com.example.expense_tracker;

import android.os.Bundle;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class AddTransactionActivity extends AppCompatActivity {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private ImageButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_transaction);

        // 1. Link Views
        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);
        btnBack = findViewById(R.id.btnBack);

        // 2. Setup the Adapter
        ViewPagerAdapter adapter = new ViewPagerAdapter(this);
        viewPager.setAdapter(adapter);

        // --- IMPROVEMENT: OFFSCREEN PAGE LIMIT ---
        // This keeps fragments in memory to avoid "View not attached" errors
        // when performing background database saves.
        viewPager.setOffscreenPageLimit(2);

        // 3. Connect Tabs to Fragments
        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> {
                    switch (position) {
                        case 0:
                            tab.setText("Expense");
                            break;
                        case 1:
                            tab.setText("Loan");
                            break;
                        case 2:
                            tab.setText("Investment");
                            break;
                    }
                }
        ).attach();

        // 4. Back Button Logic
        btnBack.setOnClickListener(v -> {
            // Simply close the activity and return to Dashboard
            finish();
        });
    }

    // --- SAFETY MEASURE ---
    // If the user presses the hardware back button, ensure we finish properly
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}