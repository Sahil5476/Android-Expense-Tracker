package com.example.expense_tracker;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.io.Serializable;

@Entity(tableName = "transactions")
public class Transaction implements Serializable {

    @PrimaryKey(autoGenerate = true)
    public int id;

    // --- Common Fields ---
    public String type;       // "Expense", "Investment", "Liability", "Asset"
    public double amount;     // The final number shown in Wallet
    public String category;   // e.g., "Food", "Rent", "Debt", "Lent"
    public String date;       // Format: dd/mm/yyyy
    public String note;       // User's description
    public long timestamp;    // Used for sorting (Newest first)

    // --- Investment Specific ---
    public double investPricePerUnit;
    public double investQuantity;

    // --- Loan Specific ---
    public String loanPerson; // Name of Lender or Borrower
    public double loanPrincipal;
    public double loanInterestRate;
    public String loanStartDate;
    public String loanEndDate;

    // --- Constructor ---
    public Transaction() {
        // Empty constructor required by Room Database
    }

    // --- Optional: toString for easier debugging ---
    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + id +
                ", type='" + type + '\'' +
                ", amount=" + amount +
                ", category='" + category + '\'' +
                ", date='" + date + '\'' +
                '}';
    }
}