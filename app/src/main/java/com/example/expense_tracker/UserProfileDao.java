package com.example.expense_tracker;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface UserProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void saveProfile(UserProfile profile);

    @Query("SELECT * FROM user_profile WHERE id = 1")
    UserProfile getUserProfile();
}