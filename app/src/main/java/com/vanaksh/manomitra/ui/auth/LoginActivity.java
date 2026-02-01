package com.vanaksh.manomitra.ui.auth;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
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
import com.vanaksh.manomitra.databinding.ActivityLoginBinding;
import com.vanaksh.manomitra.ui.dashboard.DashboardActivity;

import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private CredentialManager credentialManager;

    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is already signed in
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            checkDisclaimerAndNavigate();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        credentialManager = CredentialManager.create(this);

        // Click Listeners
        binding.btnLogin.setOnClickListener(v -> handleLogin());
        binding.btnAnonymous.setOnClickListener(v -> loginAnonymously());
        binding.btnGoogle.setOnClickListener(v -> launchGoogleSignIn());

        binding.tvCreateAccount.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
            startActivity(intent);
        });

        binding.tvForgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });
    }

    private void handleLogin() {
        String input = binding.etEmailPhone.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(input) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "All fields required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (Patterns.EMAIL_ADDRESS.matcher(input).matches()) {
            signInWithEmail(input, password);
        } else {
            findEmailAndSignIn(input, password);
        }
    }

    private void findEmailAndSignIn(String phone, String password) {
        db.collection("users")
                .whereEqualTo("phoneNumber", phone)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        String email = queryDocumentSnapshots.getDocuments().get(0).getString("email");
                        signInWithEmail(email, password);
                    } else {
                        Toast.makeText(this, "No account found. Redirecting to Signup...", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, SignupActivity.class));
                    }
                });
    }

    private void signInWithEmail(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) checkDisclaimerAndNavigate();
                    else Toast.makeText(this, "Auth Failed", Toast.LENGTH_SHORT).show();
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
                        runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Cancelled", Toast.LENGTH_SHORT).show());
                    }
                });
    }

    private void handleGoogleResponse(Credential credential) {
        try {
            GoogleIdTokenCredential tokenCredential = GoogleIdTokenCredential.createFrom(credential.getData());
            AuthCredential firebaseCred = GoogleAuthProvider.getCredential(tokenCredential.getIdToken(), null);

            mAuth.signInWithCredential(firebaseCred).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // Instead of just checking, we ensure the profile exists
                    ensureGoogleUserHasProfile(task.getResult().getUser());
                }
            });
        } catch (Exception e) {
            Log.e("AUTH", "Error parsing Google Credential", e);
        }
    }

    private void ensureGoogleUserHasProfile(FirebaseUser user) {
        db.collection("users").document(user.getUid()).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        // Profile already exists, proceed to logic gate
                        checkDisclaimerAndNavigate();
                    } else {
                        // NEW GOOGLE USER: Auto-create profile in Firestore
                        java.util.Map<String, Object> userData = new java.util.HashMap<>();
                        userData.put("name", user.getDisplayName());
                        userData.put("email", user.getEmail());
                        userData.put("phoneNumber", user.getPhoneNumber() != null ? user.getPhoneNumber() : "");
                        userData.put("profilePic", user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "");
                        userData.put("createdAt", com.google.firebase.Timestamp.now());

                        db.collection("users").document(user.getUid())
                                .set(userData)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d("AUTH", "Auto-registration successful");
                                    checkDisclaimerAndNavigate();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("AUTH", "Auto-registration failed", e);
                                    // Fallback: if auto-reg fails, we might still need the signup screen
                                    checkDisclaimerAndNavigate();
                                });
                    }
                });
    }

    private void checkIfUserExistsInFirestore(FirebaseUser user) {
        db.collection("users").document(user.getUid()).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        checkDisclaimerAndNavigate();
                    } else {
                        Toast.makeText(this, "Please complete your registration", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(this, SignupActivity.class);
                        intent.putExtra("email", user.getEmail());
                        startActivity(intent);
                    }
                });
    }

    private void loginAnonymously() {
        mAuth.signInAnonymously().addOnCompleteListener(task -> {
            if (task.isSuccessful()) checkDisclaimerAndNavigate();
        });
    }

    /**
     * LOGIC GATE: Checks if disclaimer is accepted before going to Dashboard
     */
    private void checkDisclaimerAndNavigate() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        boolean isAccepted = prefs.getBoolean("disclaimer_accepted", false);

        Intent intent;
        if (isAccepted) {
            // User is safe, go to Dashboard (MainActivity)
            intent = new Intent(LoginActivity.this, DashboardActivity.class);
        } else {
            // User hasn't agreed, go to Disclaimer
            intent = new Intent(LoginActivity.this, MainActivity.class);
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}