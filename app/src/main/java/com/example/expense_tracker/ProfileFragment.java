package com.example.expense_tracker;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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

import java.util.concurrent.Executors;

public class ProfileFragment extends Fragment {

    // UI Components
    private ImageView ivProfileImage;
    private FloatingActionButton btnEditPhoto;
    private TextInputEditText etName, etEmail, etAge;
    private Button btnSignOut, btnSave;

    // Data Sources
    private FirebaseAuth mAuth;      // For Login/Email
    private AppDatabase localDb;     // For Saving Age/Name (Room)

    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private Uri currentImageUri = null; // To track selected image

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Initialize Data Sources
        mAuth = FirebaseAuth.getInstance();
        localDb = AppDatabase.getDatabase(getContext());

        // 1. Link Views
        ivProfileImage = view.findViewById(R.id.ivProfileImage);
        btnEditPhoto = view.findViewById(R.id.btnEditPhoto);
        etName = view.findViewById(R.id.etName);
        etEmail = view.findViewById(R.id.etEmail);
        etAge = view.findViewById(R.id.etAge);
        btnSignOut = view.findViewById(R.id.btnSignOut);
        btnSave = view.findViewById(R.id.btnSave);

        // Allow name editing since we are saving locally
        etName.setEnabled(true);

        // 2. Setup Image Picker
        setupImagePicker();

        // 3. Load User Data (Robust Logic)
        loadProfileData();

        // 4. Click Listeners
        btnEditPhoto.setOnClickListener(v -> openGallery());
        btnSave.setOnClickListener(v -> saveProfileData());

        btnSignOut.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(getActivity(), LoginPage.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        return view;
    }

    // --- SAVE DATA TO ROOM DATABASE ---
    private void saveProfileData() {
        String name = etName.getText().toString().trim();
        String age = etAge.getText().toString().trim();

        FirebaseUser user = mAuth.getCurrentUser();
        String email = (user != null) ? user.getEmail() : "";
        String photoUriString = (currentImageUri != null) ? currentImageUri.toString() : null;

        if (name.isEmpty()) {
            etName.setError("Name is required");
            etName.requestFocus();
            return;
        }

        // Create Profile Object
        UserProfile profile = new UserProfile(name, email, age, photoUriString);

        // Save in Background Thread
        Executors.newSingleThreadExecutor().execute(() -> {
            localDb.userProfileDao().saveProfile(profile);

            if (getActivity() != null) {
                getActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Profile Saved Locally!", Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    // --- LOAD DATA (Robust Version) ---
    private void loadProfileData() {
        FirebaseUser user = mAuth.getCurrentUser();

        // 1. Set Email immediately (It's always available)
        if (user != null) {
            etEmail.setText(user.getEmail());
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            // Fetch Local Data
            UserProfile savedProfile = localDb.userProfileDao().getUserProfile();

            // Prepare data variables
            String nameToShow = "";
            String ageToShow = "";
            Uri photoUriToShow = null;

            // STEP A: Check Local Database First
            if (savedProfile != null) {
                if (savedProfile.name != null && !savedProfile.name.isEmpty()) {
                    nameToShow = savedProfile.name;
                }
                if (savedProfile.age != null) {
                    ageToShow = savedProfile.age;
                }
                if (savedProfile.photoUri != null) {
                    photoUriToShow = Uri.parse(savedProfile.photoUri);
                }
            }

            // STEP B: If Name is still empty, use Google/Firebase Name
            if (nameToShow.isEmpty() && user != null) {
                if (user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
                    nameToShow = user.getDisplayName();
                } else {
                    // Fallback: Use part before "@" if no name exists
                    String email = user.getEmail();
                    if (email != null && email.contains("@")) {
                        nameToShow = email.split("@")[0];
                    }
                }
            }

            // STEP C: Update UI on Main Thread
            String finalName = nameToShow;
            String finalAge = ageToShow;
            Uri finalPhoto = photoUriToShow;

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    etName.setText(finalName);
                    etAge.setText(finalAge);

                    // Logic for Photo: Local -> Google -> Placeholder
                    if (finalPhoto != null) {
                        currentImageUri = finalPhoto;
                        Glide.with(this).load(finalPhoto).circleCrop().into(ivProfileImage);
                    } else if (user != null && user.getPhotoUrl() != null) {
                        Glide.with(this).load(user.getPhotoUrl()).circleCrop().into(ivProfileImage);
                    } else {
                        // Default Placeholder if nothing exists
                        ivProfileImage.setImageResource(R.drawable.ic_person);
                    }
                });
            }
        });
    }

    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri selectedUri = result.getData().getData();
                        if (selectedUri != null) {
                            // FIX: Take persistent permission so image loads after restart
                            try {
                                final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
                                requireContext().getContentResolver().takePersistableUriPermission(selectedUri, takeFlags);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            currentImageUri = selectedUri;
                            Glide.with(this)
                                    .load(currentImageUri)
                                    .circleCrop()
                                    .into(ivProfileImage);
                        }
                    }
                }
        );
    }

    private void openGallery() {
        // FIX: Use OPEN_DOCUMENT for persistent access
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }
}