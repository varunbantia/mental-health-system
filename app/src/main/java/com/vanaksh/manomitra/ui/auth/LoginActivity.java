package com.vanaksh.manomitra.ui.auth;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;

import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.vanaksh.manomitra.MainActivity;
import com.vanaksh.manomitra.R;
import com.vanaksh.manomitra.databinding.ActivityLoginBinding;
import com.vanaksh.manomitra.ui.dashboard.DashboardActivity;
import com.vanaksh.manomitra.utils.RoleManager;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private ActivityLoginBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private CredentialManager credentialManager;

    // --- ROLE SELECTION ---
    private String selectedRole = null;
    private ChipGroup chipGroupRole;
    private TextView tvRoleLabel;
    private TextView tvRoleStatus;

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            // ── RETURNING USER: skip role selection entirely ──
            String cachedRole = RoleManager.getCachedRole(this);
            if (cachedRole != null) {
                // Fast path: use cached role
                navigateByRole(cachedRole);
            } else {
                // Cache missing (cleared on logout) — fetch from Firestore
                fetchRoleFromFirestoreAndNavigate(currentUser.getUid());
            }
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

        // --- Setup Role Selection ---
        setupRoleSelection();

        // Click Listeners
        binding.btnLogin.setOnClickListener(v -> handleLogin());
        binding.btnAnonymous.setOnClickListener(v -> handleAnonymousLogin());
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

    // ==========================================
    // ROLE SELECTION SETUP
    // ==========================================

    private void setupRoleSelection() {
        chipGroupRole = binding.getRoot().findViewById(R.id.chipGroupRole);
        tvRoleLabel = binding.getRoot().findViewById(R.id.tvRoleLabel);
        tvRoleStatus = binding.getRoot().findViewById(R.id.tvRoleStatus);

        // Initially hide the "Registered as" status
        if (tvRoleStatus != null) {
            tvRoleStatus.setVisibility(View.GONE);
        }

        // --- CHANGE: Buttons are ENABLED by default ---
        // Login and Google don't require role selection upfront for existing users.
        setButtonsEnabled(true);

        chipGroupRole.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                selectedRole = null;
                // Anonymous still requires role selection
                binding.btnAnonymous.setEnabled(false);
                binding.btnAnonymous.setAlpha(0.6f);
            } else {
                int checkedId = checkedIds.get(0);
                selectedRole = mapChipIdToRole(checkedId);
                // Enable anonymous if it was disabled
                binding.btnAnonymous.setEnabled(true);
                binding.btnAnonymous.setAlpha(1.0f);
            }
        });

        // Anonymous starts disabled as it requires "Student" role selection
        binding.btnAnonymous.setEnabled(false);
        binding.btnAnonymous.setAlpha(0.6f);
    }

    private void setButtonsEnabled(boolean enabled) {
        float alpha = enabled ? 1.0f : 0.6f;
        binding.btnLogin.setEnabled(enabled);
        binding.btnLogin.setAlpha(alpha);
        binding.btnGoogle.setEnabled(enabled);
        binding.btnGoogle.setAlpha(alpha);
        binding.btnAnonymous.setEnabled(enabled);
        binding.btnAnonymous.setAlpha(alpha);
    }

    /**
     * Hide the role ChipGroup and show "Registered as: [ROLE]" status.
     * Used when user already has a Firestore role.
     */
    private void showRegisteredRoleStatus(String storedRole) {
        // Hide chips + label
        if (chipGroupRole != null)
            chipGroupRole.setVisibility(View.GONE);
        if (tvRoleLabel != null)
            tvRoleLabel.setVisibility(View.GONE);

        // Show "Registered as" status
        if (tvRoleStatus != null) {
            tvRoleStatus.setVisibility(View.VISIBLE);
            String displayName = RoleManager.getRoleDisplayName(storedRole);
            tvRoleStatus.setText("Registered as: " + displayName);
        }

        // Enable all buttons (role comes from Firestore, not UI)
        setButtonsEnabled(true);
    }

    private String mapChipIdToRole(int chipId) {
        if (chipId == R.id.chipStudent)
            return RoleManager.ROLE_USER;
        if (chipId == R.id.chipVolunteer)
            return RoleManager.ROLE_VOLUNTEER;
        if (chipId == R.id.chipCounsellor)
            return RoleManager.ROLE_COUNSELLOR;
        if (chipId == R.id.chipModerator)
            return RoleManager.ROLE_MODERATOR;
        if (chipId == R.id.chipAdmin)
            return RoleManager.ROLE_ADMIN;
        return RoleManager.ROLE_USER;
    }

    // ==========================================
    // ANONYMOUS LOGIN — Student role only
    // ==========================================

    private void handleAnonymousLogin() {
        // Anonymous login STILL requires role selection (Student only)
        if (selectedRole == null) {
            Toast.makeText(this, R.string.role_required_message, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!RoleManager.isAnonymousAllowed(selectedRole)) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.role_anonymous_error_title)
                    .setMessage(R.string.role_anonymous_error_message)
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        mAuth.signInAnonymously().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = mAuth.getCurrentUser();
                if (user != null) {
                    java.util.Map<String, Object> userData = new java.util.HashMap<>();
                    userData.put("role", RoleManager.ROLE_USER);
                    userData.put("isActive", true);
                    userData.put("isAnonymous", true);
                    userData.put("createdAt", com.google.firebase.Timestamp.now());

                    db.collection("users").document(user.getUid())
                            .set(userData)
                            .addOnSuccessListener(aVoid -> {
                                RoleManager.saveRole(this, RoleManager.ROLE_USER);
                                navigateByRole(RoleManager.ROLE_USER);
                            })
                            .addOnFailureListener(e -> {
                                RoleManager.saveRole(this, RoleManager.ROLE_USER);
                                navigateByRole(RoleManager.ROLE_USER);
                            });
                }
            }
        });
    }

    // ==========================================
    // EMAIL LOGIN (existing logic preserved)
    // ==========================================

    private void handleLogin() {
        // --- CHANGE: No role selection check here anymore ---
        // We fetch role from Firestore AFTER auth succeeds.

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
                    if (task.isSuccessful())
                        verifyRoleAndNavigate();
                    else
                        Toast.makeText(this, "Auth Failed", Toast.LENGTH_SHORT).show();
                });
    }

    // ==========================================
    // GOOGLE SIGN-IN (existing logic preserved)
    // ==========================================

    private void launchGoogleSignIn() {
        // --- CHANGE: No role selection check here anymore ---
        // We check Firestore after auth succeeds.

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
                    ensureGoogleUserHasProfile(task.getResult().getUser());
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error parsing Google Credential", e);
        }
    }

    private void ensureGoogleUserHasProfile(FirebaseUser user) {
        db.collection("users").document(user.getUid()).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        // ── EXISTING USER: enforce Firestore role ──
                        verifyRoleAndNavigate();
                    } else {
                        // ── NEW GOOGLE USER: require role selection now ──
                        if (selectedRole == null) {
                            Toast.makeText(this, "New account! Please select a role above to continue.",
                                    Toast.LENGTH_LONG).show();
                            // Scroll to role selection or pulse the chips? For now, just show toast.
                            return;
                        }
                        java.util.Map<String, Object> userData = new java.util.HashMap<>();
                        userData.put("name", user.getDisplayName());
                        userData.put("email", user.getEmail());
                        userData.put("phoneNumber", user.getPhoneNumber() != null ? user.getPhoneNumber() : "");
                        userData.put("profilePic", user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "");
                        userData.put("role", selectedRole);
                        userData.put("isActive", true);
                        userData.put("createdAt", com.google.firebase.Timestamp.now());

                        db.collection("users").document(user.getUid())
                                .set(userData)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "Google registration with role: " + selectedRole);
                                    RoleManager.saveRole(this, selectedRole);
                                    navigateByRole(selectedRole);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Google registration failed", e);
                                    RoleManager.saveRole(this, selectedRole);
                                    navigateByRole(selectedRole);
                                });
                    }
                });
    }

    // ==========================================
    // ROLE ENFORCEMENT — always trust Firestore
    // ==========================================

    /**
     * Fetches the stored role from Firestore.
     * ALWAYS uses the Firestore role. Ignores UI selection.
     * If user is new (no Firestore doc), uses selectedRole from UI.
     */
    private void verifyRoleAndNavigate() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null)
            return;

        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String storedRole = documentSnapshot.getString("role");

                        if (storedRole == null || storedRole.isEmpty()) {
                            // Legacy user with no role — treat as student
                            storedRole = RoleManager.ROLE_USER;
                        }

                        // ── ALWAYS enforce Firestore role, ignore UI selection ──
                        RoleManager.saveRole(this, storedRole);
                        navigateByRole(storedRole);

                    } else {
                        // No Firestore doc — this is a first-time email login
                        // Role should have been set during signup.
                        // Fallback to student.
                        RoleManager.saveRole(this, RoleManager.ROLE_USER);
                        navigateByRole(RoleManager.ROLE_USER);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch user role", e);
                    Toast.makeText(this, "Error verifying role. Please try again.", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Used by onStart() for returning users when cache is missing.
     * Fetches role from Firestore, caches it, and redirects.
     */
    private void fetchRoleFromFirestoreAndNavigate(String uid) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String storedRole = documentSnapshot.getString("role");
                        if (storedRole == null || storedRole.isEmpty()) {
                            storedRole = RoleManager.ROLE_USER;
                        }
                        RoleManager.saveRole(this, storedRole);
                        navigateByRole(storedRole);
                    } else {
                        // No Firestore doc — fallback
                        navigateByRole(RoleManager.ROLE_USER);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firestore fetch failed on auto-login", e);
                    // Can't verify — stay on login screen
                });
    }

    private void registerFcmToken() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            FirebaseMessaging.getInstance().getToken()
                    .addOnSuccessListener(token -> {
                        db.collection("users").document(user.getUid())
                                .update("fcmToken", token)
                                .addOnSuccessListener(aVoid -> Log.d(TAG, "FCM Token registered"))
                                .addOnFailureListener(e -> Log.e(TAG, "FCM Token registration failed", e));
                    });
        }
    }

    // ==========================================
    // NAVIGATION — role-based + disclaimer check
    // ==========================================

    private void navigateByRole(String role) {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        boolean isAccepted = prefs.getBoolean("disclaimer_accepted", false);

        Intent intent;
        if (!isAccepted) {
            intent = new Intent(LoginActivity.this, MainActivity.class);
        } else {
            registerFcmToken(); // Register token before navigating to dashboard
            Class<?> target = RoleManager.getTargetActivity(role);
            intent = new Intent(LoginActivity.this, target);
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}