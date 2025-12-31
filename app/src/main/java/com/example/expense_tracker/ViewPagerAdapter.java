package com.example.expense_tracker;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class ViewPagerAdapter extends FragmentStateAdapter {

    public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // This switches the screens based on which tab is clicked
        switch (position) {
            case 0:
                return new ExpenseFragment();
            case 1:
                return new LoanFragment();
            case 2:
                return new InvestmentFragment();
            default:
                return new ExpenseFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 3; // We have 3 tabs: Expense, Loan, Investment
    }
}