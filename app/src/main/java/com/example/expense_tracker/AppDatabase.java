package com.example.expense_tracker;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {Transaction.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {

    public abstract TransactionDao transactionDao();

    // 'volatile' ensures that if one thread creates the database,
    // all other threads see it immediately.
    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "expense_tracker_db")
                            // Allows reading/writing on the main UI thread (Good for simple apps)
                            .allowMainThreadQueries()
                            // If you change the Transaction columns, this wipes the old DB to prevent crashes
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}