package com.example.expense_tracker;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions; // Required for merging data

import java.util.HashMap;
import java.util.Map;

public class ProfileFragment extends Fragment {

    // UI Components
    private ImageView ivProfileImage;
    private FloatingActionButton btnEditPhoto;
    private TextInputEditText etName, etEmail, etAge;
    private Button btnSignOut, btnSave; // Added btnSave

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // 1. Link Views
        ivProfileImage = view.findViewById(R.id.ivProfileImage);
        btnEditPhoto = view.findViewById(R.id.btnEditPhoto);
        etName = view.findViewById(R.id.etName);
        etEmail = view.findViewById(R.id.etEmail);
        etAge = view.findViewById(R.id.etAge);
        btnSignOut = view.findViewById(R.id.btnSignOut);

        // --- NEW: Link Save Button ---
        btnSave = view.findViewById(R.id.btnSave);

        // 2. Setup Image Picker Logic
        setupImagePicker();

        // 3. Load User Data
        loadProfileData();

        // 4. Set Click Listeners
        btnEditPhoto.setOnClickListener(v -> openGallery());

        // --- NEW: Save Button Listener ---
        btnSave.setOnClickListener(v -> saveProfileData());

        btnSignOut.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(getActivity(), LoginPage.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        return view;
    }

    // --- NEW: Method to Save Age to Firestore ---
    private void saveProfileData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        String ageText = etAge.getText().toString().trim();

        if (ageText.isEmpty()) {
            Toast.makeText(getContext(), "Please enter your age", Toast.LENGTH_SHORT).show();
            return;
        }

        // Prepare data
        Map<String, Object> userData = new HashMap<>();
        userData.put("age", ageText);

        // Optional: If you want to allow saving the name as well
        // userData.put("name", etName.getText().toString().trim());

        // Write to Firestore (Merge to keep other fields like email/photo safe)
        db.collection("users").document(user.getUid())
                .set(userData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Profile Saved!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadProfileData() {
        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {
            etEmail.setText(user.getEmail());

            if (user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
                etName.setText(user.getDisplayName());
            }

            if (user.getPhotoUrl() != null) {
                Glide.with(this)
                        .load(user.getPhotoUrl())
                        .circleCrop()
                        .placeholder(R.drawable.ic_person)
                        .into(ivProfileImage);
            }

            db.collection("users").document(user.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            // Get Name
                            String dbName = documentSnapshot.getString("name");
                            if (dbName != null && !dbName.isEmpty()) {
                                etName.setText(dbName);
                            }

                            // Get Age
                            Object age = documentSnapshot.get("age");
                            if (age != null) {
                                etAge.setText(String.valueOf(age));
                            }

                            // Get Photo
                            String dbPhotoUrl = documentSnapshot.getString("photoUrl");
                            if (dbPhotoUrl != null && !dbPhotoUrl.isEmpty()) {
                                Glide.with(this)
                                        .load(dbPhotoUrl)
                                        .circleCrop()
                                        .placeholder(R.drawable.ic_person)
                                        .into(ivProfileImage);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Failed to fetch details", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null) {
                            Glide.with(this)
                                    .load(selectedImageUri)
                                    .circleCrop()
                                    .into(ivProfileImage);

                            // NOTE: In a complete app, you would upload this image to Firebase Storage here
                            Toast.makeText(getContext(), "Image selected", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }
}