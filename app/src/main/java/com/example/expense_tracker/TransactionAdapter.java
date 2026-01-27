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
import java.util.Locale; // Import Locale for formatting

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {

    private Context context;
    private List<Transaction> transactionList;
    private OnDeleteClickListener deleteListener;

    // Flag to Hide/Show Delete Button (For Dashboard vs Wallet)
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

    // Method to set read-only mode (Hides Delete Button)
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

        // Show Note if it exists
        if (transaction.note != null && !transaction.note.isEmpty()) {
            holder.tvNote.setText(transaction.note);
            holder.tvNote.setVisibility(View.VISIBLE);
        } else {
            holder.tvNote.setVisibility(View.GONE);
        }

        // Safety check for null type
        String type = transaction.type != null ? transaction.type : "Expense";

        // --- FIXED NUMBER FORMATTING LOGIC ---
        // We use String.format("%.2f", value) to show only 2 decimal places (e.g. 10.50)

        // Negative (-) : Expense AND Asset (Money Out)
        if (type.equals("Expense") || type.equals("Asset")) {
            holder.tvAmount.setText(String.format(Locale.getDefault(), "- ₹ %.2f", transaction.amount));
            holder.tvAmount.setTextColor(Color.parseColor("#E53935")); // Red
        }
        // Positive (+) : Income AND Liability (Money In)
        else {
            holder.tvAmount.setText(String.format(Locale.getDefault(), "+ ₹ %.2f", transaction.amount));
            holder.tvAmount.setTextColor(Color.parseColor("#43A047")); // Green
        }

        // --- DELETE BUTTON LOGIC ---
        if (isReadOnly) {
            // Hide Delete button on Dashboard
            holder.btnDelete.setVisibility(View.GONE);
            holder.btnDelete.setClickable(false);
        } else {
            // Show Delete button in Wallet
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