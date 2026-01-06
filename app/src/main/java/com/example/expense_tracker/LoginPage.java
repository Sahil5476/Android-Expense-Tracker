package com.example.expense_tracker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class LoginPage extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private ImageButton btnGoogle;
    private TextView tvForgotPassword, tvSignUp;

    // Firebase Variables
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            checkLoginExpiry();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login_page);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();

        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Link Views
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoogle = findViewById(R.id.imageButton2);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvSignUp = findViewById(R.id.tvSignUp);

        // Listeners
        btnLogin.setOnClickListener(v -> loginUser());
        btnGoogle.setOnClickListener(v -> signInWithGoogle());

        tvForgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(LoginPage.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });

        tvSignUp.setOnClickListener(v -> {
            Intent intent = new Intent(LoginPage.this, SignUpPageActivity.class);
            startActivity(intent);
        });
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Basic check for empty fields (before asking Firebase)
        if (TextUtils.isEmpty(email)) { etEmail.setError("Email is required"); return; }
        if (TextUtils.isEmpty(password)) { etPassword.setError("Password is required"); return; }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // SUCCESS
                        saveLoginTimestamp();
                        Toast.makeText(LoginPage.this, "Login Successful!", Toast.LENGTH_SHORT).show();
                        goToDashboard();
                    } else {
                        // FAIL: Show generic red error on BOTH fields
                        String genericError = "Incorrect Email or Password";

                        // This turns the field outline red and shows the exclamation icon
                        etEmail.setError(genericError);
                        etPassword.setError(genericError);

                        // Also focus on email so they can start over
                        etEmail.requestFocus();

                        Toast.makeText(LoginPage.this, "Login Failed: Incorrect Credentials", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        googleSignInLauncher.launch(signInIntent);
    }

    private final ActivityResultLauncher<Intent> googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Intent data = result.getData();
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        firebaseAuthWithGoogle(account.getIdToken());
                    } catch (ApiException e) {
                        Toast.makeText(LoginPage.this, "Google Sign In Failed", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        saveLoginTimestamp();
                        Toast.makeText(LoginPage.this, "Google Login Successful!", Toast.LENGTH_SHORT).show();
                        goToDashboard();
                    } else {
                        Toast.makeText(LoginPage.this, "Authentication Failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // --- HELPER METHODS ---
    private void saveLoginTimestamp() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong("login_timestamp", System.currentTimeMillis());
        editor.apply();
    }

    private void checkLoginExpiry() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        long lastLoginTime = prefs.getLong("login_timestamp", 0);
        long currentTime = System.currentTimeMillis();
        long thirtyDaysInMillis = 30L * 24 * 60 * 60 * 1000;

        if ((currentTime - lastLoginTime) < thirtyDaysInMillis) {
            goToDashboard();
        } else {
            mAuth.signOut();
            Toast.makeText(this, "Session Expired. Please login again.", Toast.LENGTH_LONG).show();
        }
    }

    private void goToDashboard() {
        Intent intent = new Intent(LoginPage.this, DashboardActivity.class);
        startActivity(intent);
        finish();
    }
}