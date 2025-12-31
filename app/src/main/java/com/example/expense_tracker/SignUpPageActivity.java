package com.example.expense_tracker;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SignUpPageActivity extends AppCompatActivity {

    private EditText etFirstName, etEmail, etPassword;
    private Button btnSignUp;
    private TextView tvLoginHere;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up_page);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etFirstName = findViewById(R.id.etFirstName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnSignUp = findViewById(R.id.btnSignUp);
        tvLoginHere = findViewById(R.id.tvLoginHere);

        btnSignUp.setOnClickListener(v -> registerUser());

        tvLoginHere.setOnClickListener(v -> {
            Intent intent = new Intent(SignUpPageActivity.this, LoginPage.class);
            startActivity(intent);
            finish();
        });
    }

    private void registerUser() {
        String name = etFirstName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(name)) { etFirstName.setError("Name required"); return; }
        if (TextUtils.isEmpty(email)) { etEmail.setError("Email required"); return; }
        if (TextUtils.isEmpty(password)) { etPassword.setError("Password required"); return; }
        if (password.length() < 6) { etPassword.setError("Min 6 chars"); return; }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // 1. Save Name to Auth Profile (Easy access later)
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(name)
                                    .build();
                            user.updateProfile(profileUpdates);

                            // 2. Save Name/Email to Firestore Database (For data features)
                            saveUserToDatabase(user.getUid(), name, email);
                        }
                    } else {
                        Toast.makeText(SignUpPageActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserToDatabase(String userId, String name, String email) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("name", name);
        userMap.put("email", email);
        userMap.put("userId", userId);

        db.collection("users").document(userId)
                .set(userMap)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(SignUpPageActivity.this, "Account Created!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(SignUpPageActivity.this, DashboardActivity.class);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(SignUpPageActivity.this, "Failed to save database: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}