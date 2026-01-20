package com.example.expense_tracker;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {

    private Context context;
    private List<Transaction> transactionList;
    private OnDeleteClickListener deleteListener;

    // --- NEW FLAG: Controls if the Delete button is visible ---
    private boolean isReadOnly = false;

    // Interface to handle delete clicks
    public interface OnDeleteClickListener {
        void onDeleteClick(Transaction transaction);
    }

    public TransactionAdapter(Context context, List<Transaction> transactionList, OnDeleteClickListener listener) {
        this.context = context;
        this.transactionList = transactionList;
        this.deleteListener = listener;
    }

    // --- NEW METHOD: Call this to hide the delete button (for Stats screen) ---
    public void setReadOnly(boolean readOnly) {
        this.isReadOnly = readOnly;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_transaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Transaction transaction = transactionList.get(position);

        holder.tvCategory.setText(transaction.category);
        holder.tvDate.setText(transaction.date);

        // Show Note if it exists, otherwise hide
        if (transaction.note != null && !transaction.note.isEmpty()) {
            holder.tvNote.setText(transaction.note);
            holder.tvNote.setVisibility(View.VISIBLE);
        } else {
            holder.tvNote.setVisibility(View.GONE);
        }

        // Color Coding: Expense/Liability = RED, Investment/Asset = GREEN
        // Added safety check for null type
        String type = transaction.type != null ? transaction.type : "Expense";

        if (type.equals("Expense") || type.equals("Liability")) {
            holder.tvAmount.setText("- ₹ " + transaction.amount);
            holder.tvAmount.setTextColor(Color.parseColor("#E53935")); // Red
        } else {
            holder.tvAmount.setText("+ ₹ " + transaction.amount);
            holder.tvAmount.setTextColor(Color.parseColor("#43A047")); // Green
        }

        // --- UPDATED DELETE BUTTON LOGIC ---
        if (isReadOnly) {
            // Stats Screen: Hide the button so user cannot delete
            holder.btnDelete.setVisibility(View.GONE);
            holder.btnDelete.setClickable(false);
        } else {
            // Wallet Screen: Show button and enable click
            holder.btnDelete.setVisibility(View.VISIBLE);
            holder.btnDelete.setClickable(true);
            holder.btnDelete.setOnClickListener(v -> {
                if (deleteListener != null) {
                    deleteListener.onDeleteClick(transaction);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return transactionList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategory, tvDate, tvAmount, tvNote;
        ImageButton btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvNote = itemView.findViewById(R.id.tvNote);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}