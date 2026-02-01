package com.vanaksh.manomitra.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;

import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.vanaksh.manomitra.MainActivity;
import com.vanaksh.manomitra.R;
import com.vanaksh.manomitra.databinding.ActivitySignupBinding;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

public class SignupActivity extends AppCompatActivity {

    private ActivitySignupBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private CredentialManager credentialManager;

    // Password requirements: 1 Upper, 1 Lower, 1 Special, 1 Digit, min 6 chars
    private static final String PASSWORD_PATTERN =
            "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{6,}$";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        credentialManager = CredentialManager.create(this);

        setupTextWatchers();

        binding.btnSignup.setOnClickListener(v -> validateAndCreateAccount());
        binding.btnGoogleSignup.setOnClickListener(v -> launchGoogleSignIn());
        binding.tvLoginRedirect.setOnClickListener(v -> finish());
    }

    private void validateAndCreateAccount() {
        String email = binding.etEmail.getText().toString().trim();
        String phone = binding.etPhone.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();
        String confirmPass = binding.etConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(phone) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.setError("Enter a valid email");
            return;
        }

        if (phone.length() < 10) {
            binding.tilPhone.setError("Enter a valid 10-digit phone number");
            return;
        }

        if (!password.matches(PASSWORD_PATTERN)) {
            binding.tilPassword.setError("Password too weak (need Upper, Lower, Special, Digit, 6+ chars)");
            return;
        }

        if (!password.equals(confirmPass)) {
            binding.tilConfirmPassword.setError("Passwords do not match");
            return;
        }

        checkDuplicatePhoneAndRegister(email, phone, password);
    }

    private void checkDuplicatePhoneAndRegister(String email, String phone, String password) {
        // Ensure we are searching for the trimmed, clean string
        String cleanPhone = phone.trim();

        db.collection("users")
                .whereEqualTo("phoneNumber", cleanPhone)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult() != null && !task.getResult().isEmpty()) {
                            // Match found! Stop registration
                            Log.d("AUTH_CHECK", "Duplicate found for: " + cleanPhone);
                            binding.tilPhone.setError("This phone number is already registered");
                            Toast.makeText(this, "Phone number already in use", Toast.LENGTH_SHORT).show();
                        } else {
                            // No match found, proceed
                            Log.d("AUTH_CHECK", "No duplicate found. Proceeding...");
                            registerUser(email, cleanPhone, password);
                        }
                    } else {
                        Log.e("AUTH_CHECK", "Error checking duplication", task.getException());
                        Toast.makeText(this, "Database error. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void registerUser(String email, String phone, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Manual Signup: pass false for isGoogleUser
                        saveUserToFirestore(email, phone, false);
                    } else {
                        Toast.makeText(this, "Auth Failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void launchGoogleSignIn() {
        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(getString(R.string.default_web_client_id))
                .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        credentialManager.getCredentialAsync(this, request, null, Executors.newSingleThreadExecutor(),
                new androidx.credentials.CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse response) {
                        handleGoogleResponse(response.getCredential());
                    }

                    @Override
                    public void onError(GetCredentialException e) {
                        runOnUiThread(() -> Toast.makeText(SignupActivity.this, "Cancelled", Toast.LENGTH_SHORT).show());
                    }
                });
    }

    private void handleGoogleResponse(Credential credential) {
        try {
            GoogleIdTokenCredential tokenCredential = GoogleIdTokenCredential.createFrom(credential.getData());
            AuthCredential firebaseCred = GoogleAuthProvider.getCredential(tokenCredential.getIdToken(), null);

            mAuth.signInWithCredential(firebaseCred).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    FirebaseUser user = task.getResult().getUser();
                    if (user != null) {
                        checkExistingUserInFirestore(user);
                    }
                }
            });
        } catch (Exception e) {
            Log.e("AUTH", "Google parsing error", e);
        }
    }

    private void checkExistingUserInFirestore(FirebaseUser user) {
        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        navigateToHome();
                    } else {
                        // Google Signup: pass true for isGoogleUser
                        saveUserToFirestore(user.getEmail(), "", true);
                    }
                });
    }

    private void saveUserToFirestore(String email, String phone, boolean isGoogleUser) {
        String uid = mAuth.getCurrentUser().getUid();
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("email", email);
        userMap.put("phoneNumber", phone);
        userMap.put("uid", uid);
        userMap.put("createdAt", com.google.firebase.Timestamp.now());

        db.collection("users").document(uid).set(userMap)
                .addOnSuccessListener(aVoid -> {
                    if (isGoogleUser) {
                        navigateToHome();
                    } else {
                        // Manual user: Sign out and return to LoginActivity
                        mAuth.signOut();
                        Toast.makeText(this, "Signup successful! Please login.", Toast.LENGTH_LONG).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Database error", Toast.LENGTH_SHORT).show());
    }

    private void navigateToHome() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setupTextWatchers() {
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.tilEmail.setError(null);
                binding.tilPhone.setError(null);
                binding.tilPassword.setError(null);
                binding.tilConfirmPassword.setError(null);
            }
            @Override public void afterTextChanged(Editable s) {}
        };
        binding.etEmail.addTextChangedListener(watcher);
        binding.etPhone.addTextChangedListener(watcher);
        binding.etPassword.addTextChangedListener(watcher);
        binding.etConfirmPassword.addTextChangedListener(watcher);
    }
}