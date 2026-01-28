package com.example.expense_tracker;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "user_profile")
public class UserProfile {
    // We use a fixed ID = 1 because there is only one user for this app
    @PrimaryKey
    public int id = 1;

    public String name;
    public String email;
    public String age; // Storing age as String to match EditText
    public String photoUri; // To store the image path locally

    // Constructor
    public UserProfile(String name, String email, String age, String photoUri) {
        this.name = name;
        this.email = email;
        this.age = age;
        this.photoUri = photoUri;
    }
}