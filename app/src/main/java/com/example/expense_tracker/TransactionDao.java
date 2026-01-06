package com.example.expense_tracker;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface TransactionDao {
    @Insert
    void insertTransaction(Transaction transaction);

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    List<Transaction> getAllTransactions();

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'Expense'")
    double getTotalExpense();
}