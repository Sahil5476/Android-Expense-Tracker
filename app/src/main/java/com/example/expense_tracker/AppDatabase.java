package com.example.expense_tracker;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

// FIX 1: Add UserProfile.class to entities & Increment version to 2
@Database(entities = {Transaction.class, UserProfile.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract TransactionDao transactionDao();

    // FIX 2: Add this method so "localDb.userProfileDao()" works
    public abstract UserProfileDao userProfileDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "expense_tracker_db")
                            .allowMainThreadQueries()
                            // This handles the upgrade from V1 (Transactions only) to V2 (Transactions + Profile)
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}