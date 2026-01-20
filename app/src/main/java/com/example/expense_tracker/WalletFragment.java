package com.example.expense_tracker;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class WalletFragment extends Fragment {

    private RecyclerView recyclerView;
    private TransactionAdapter adapter;
    private List<Transaction> transactionList = new ArrayList<>();
    private TextView tvEmptyState;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_wallet, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewWallet);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Initial Load
        loadTransactions();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh data whenever we come back to this screen
        loadTransactions();
    }

    private void loadTransactions() {
        // Run database operations in background thread
        Executors.newSingleThreadExecutor().execute(() -> {
            transactionList = AppDatabase.getDatabase(getContext()).transactionDao().getAllTransactions();

            // Update UI on Main Thread
            getActivity().runOnUiThread(() -> {
                if (transactionList.isEmpty()) {
                    tvEmptyState.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    tvEmptyState.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                    setupAdapter();
                }
            });
        });
    }

    private void setupAdapter() {
        adapter = new TransactionAdapter(getContext(), transactionList, transaction -> {
            // This runs when Delete button is clicked
            showDeleteConfirmation(transaction);
        });
        recyclerView.setAdapter(adapter);
    }

    private void showDeleteConfirmation(Transaction transaction) {
        new AlertDialog.Builder(getContext())
                .setTitle("Delete Transaction")
                .setMessage("Are you sure you want to remove this record?")
                .setPositiveButton("Yes", (dialog, which) -> deleteTransactionFromDB(transaction))
                .setNegativeButton("No", null)
                .show();
    }

    private void deleteTransactionFromDB(Transaction transaction) {
        Executors.newSingleThreadExecutor().execute(() -> {
            // 1. Delete from Database
            AppDatabase.getDatabase(getContext()).transactionDao().deleteTransaction(transaction);

            // 2. Remove from List and Update UI
            getActivity().runOnUiThread(() -> {
                transactionList.remove(transaction);
                adapter.notifyDataSetChanged();

                if (transactionList.isEmpty()) {
                    tvEmptyState.setVisibility(View.VISIBLE);
                }
                Toast.makeText(getContext(), "Transaction Deleted", Toast.LENGTH_SHORT).show();
            });
        });
    }
}