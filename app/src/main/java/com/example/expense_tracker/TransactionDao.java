package com.example.expense_tracker;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface TransactionDao {

    @Insert
    void insertTransaction(Transaction transaction);

    // --- ADD THESE NEW METHODS ---

    // Get all transactions sorted by newest first
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    List<Transaction> getAllTransactions();

    // Delete a specific transaction
    @Delete
    void deleteTransaction(Transaction transaction);
}