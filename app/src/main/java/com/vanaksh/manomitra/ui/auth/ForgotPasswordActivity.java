package com.vanaksh.manomitra.ui.auth;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.vanaksh.manomitra.databinding.ActivityForgotPasswordBinding;

public class ForgotPasswordActivity extends AppCompatActivity {

    private ActivityForgotPasswordBinding binding;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Enabling Edge-to-Edge to match your LoginActivity aesthetic
        EdgeToEdge.enable(this);

        binding = ActivityForgotPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();

        // Setup listeners
        setupListeners();
        setupTextWatcher();
    }

    private void setupListeners() {
        // Handle Reset Button Click
        binding.btnReset.setOnClickListener(v -> validateAndSendResetEmail());

        // Handle Back to Login Click
        binding.tvBackToLogin.setOnClickListener(v -> finish());
    }

    private void validateAndSendResetEmail() {
        String email = binding.etEmail.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            binding.tilEmail.setError("Email is required to reset password");
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.setError("Please enter a valid email address");
            return;
        }

        // Disable UI during network call
        toggleLoading(true);

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    toggleLoading(false);
                    if (task.isSuccessful()) {
                        // Success: Show a professional confirmation
                        Toast.makeText(this,
                                "Success! A reset link has been sent to " + email,
                                Toast.LENGTH_LONG).show();

                        // Small delay before closing so user can read the toast
                        binding.getRoot().postDelayed(this::finish, 2000);
                    } else {
                        // Failure: Handle common Firebase Auth errors
                        String errorMsg = task.getException() != null ?
                                task.getException().getMessage() : "Failed to send reset email";

                        binding.tilEmail.setError(errorMsg);
                        Toast.makeText(this, "Error: " + errorMsg, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Toggles the button state to prevent double-clicks during the Firebase call
     */
    private void toggleLoading(boolean isLoading) {
        binding.btnReset.setEnabled(!isLoading);
        binding.btnReset.setText(isLoading ? "Sending..." : "Send Reset Link");
        // If you add a ProgressBar to your XML, toggle its visibility here:
        // binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    /**
     * Clears error messages as the user types
     */
    private void setupTextWatcher() {
        binding.etEmail.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.tilEmail.setError(null); // Clear error on typing
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }
}