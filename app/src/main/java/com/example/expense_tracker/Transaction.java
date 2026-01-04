package com.example.expense_tracker;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "transactions")
public class Transaction {
    @PrimaryKey(autoGenerate = true)
    public int id;

    // Common Fields
    public String type;       // "Expense", "Investment", "Liability", "Asset"
    public double amount;     // Final calculated amount
    public String category;
    public String date;       // dd/mm/yyyy
    public String note;
    public long timestamp;    // For sorting

    // Investment Specific Fields
    public double investPricePerUnit;
    public double investQuantity;

    // Loan Specific Fields
    public String loanPerson; // Lender or Borrower Name
    public double loanPrincipal;
    public double loanInterestRate;
    public String loanStartDate;
    public String loanEndDate;

    // Empty Constructor
    public Transaction() {}
}