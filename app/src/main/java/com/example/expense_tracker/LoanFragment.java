package com.example.expense_tracker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.tabs.TabLayout;

public class LoanFragment extends Fragment {

    private TabLayout tabLayout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_loan, container, false);

        tabLayout = view.findViewById(R.id.tabLayoutLoan);

        // Load Default Fragment (Liability)
        if (savedInstanceState == null) {
            getChildFragmentManager().beginTransaction()
                    .replace(R.id.loan_fragment_container, new LoanLiabilityFragment())
                    .commit();
        }

        // Handle Tab Clicks
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                Fragment selectedFragment = null;
                if (tab.getPosition() == 0) {
                    selectedFragment = new LoanLiabilityFragment(); // Borrowed
                } else {
                    selectedFragment = new LoanAssetFragment(); // Lent
                }

                if (selectedFragment != null) {
                    getChildFragmentManager().beginTransaction()
                            .replace(R.id.loan_fragment_container, selectedFragment)
                            .commit();
                }
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        return view;
    }
}