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

        // 2. Setup the Adapter (This loads the 3 Fragments)
        ViewPagerAdapter adapter = new ViewPagerAdapter(this);
        viewPager.setAdapter(adapter);

        // 3. Connect Tabs to Fragments (This names the tabs)
        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> {
                    switch (position) {
                        case 0:
                            tab.setText("Expense"); // Label for Tab 1
                            break;
                        case 1:
                            tab.setText("Loan");    // Label for Tab 2
                            break;
                        case 2:
                            tab.setText("Investment"); // Label for Tab 3
                            break;
                    }
                }
        ).attach(); // <--- This assumes the Sticky Tabs are attached to the ViewPager

        // 4. Back Button Logic
        btnBack.setOnClickListener(v -> finish());
    }
}