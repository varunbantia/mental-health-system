package com.vanaksh.manomitra.ui.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.vanaksh.manomitra.BuildConfig;
import com.vanaksh.manomitra.R;
import com.vanaksh.manomitra.ui.auth.LoginActivity;
import com.vanaksh.manomitra.utils.RoleManager;

public class SettingsActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Back button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // App Version
        TextView tvAppVersion = findViewById(R.id.tvAppVersion);
        tvAppVersion.setText(BuildConfig.VERSION_NAME);

        // Log Out
        findViewById(R.id.cardLogout).setOnClickListener(v -> showLogoutDialog());

        // Delete Account
        findViewById(R.id.cardDeleteAccount).setOnClickListener(v -> showDeleteAccountDialog());
    }

    private void showLogoutDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Log Out")
                .setMessage("Are you sure you want to log out? You can sign back in anytime.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Log Out", (dialog, which) -> performLogout())
                .show();
    }

    private void performLogout() {
        // Clear local session data
        getSharedPreferences("manomitra_prefs", MODE_PRIVATE).edit().clear().apply();

        // Clear cached role + sign out Firebase
        RoleManager.performLogout(this);

        // Redirect to Login
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showDeleteAccountDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Account")
                .setMessage(
                        "This action is permanent and cannot be undone. All your data — mood entries, chat history, bookings, and saved tips — will be permanently removed.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> performDeleteAccount())
                .show();
    }

    private void performDeleteAccount() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "No user signed in", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = user.getUid();

        // Step 1: Delete user Firestore data (chats, tips, bookings sub-path)
        deleteUserFirestoreData(uid, () -> {
            // Step 2: Delete Firebase Auth account
            user.delete()
                    .addOnSuccessListener(aVoid -> {
                        getSharedPreferences("manomitra_prefs", MODE_PRIVATE).edit().clear().apply();
                        RoleManager.clearCachedRole(this);

                        Toast.makeText(this, "Account deleted", Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(this, LoginActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        // Re-auth likely required for email users
                        Toast.makeText(this,
                                "Please log out and log back in, then try again.",
                                Toast.LENGTH_LONG).show();
                    });
        });
    }

    private void deleteUserFirestoreData(String uid, Runnable onComplete) {
        // Delete /users/{uid}/chats sub-collection documents
        db.collection("users").document(uid).collection("chats")
                .get()
                .addOnSuccessListener(snapshots -> {
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snapshots) {
                        // Delete messages inside each chat
                        db.collection("users").document(uid)
                                .collection("chats").document(doc.getId())
                                .collection("messages")
                                .get()
                                .addOnSuccessListener(msgSnaps -> {
                                    for (com.google.firebase.firestore.DocumentSnapshot msg : msgSnaps)
                                        msg.getReference().delete();
                                });
                        doc.getReference().delete();
                    }

                    // Delete /users/{uid}/tips
                    db.collection("users").document(uid).collection("tips")
                            .get()
                            .addOnSuccessListener(tipSnaps -> {
                                for (com.google.firebase.firestore.DocumentSnapshot doc : tipSnaps)
                                    doc.getReference().delete();

                                // Delete the user document itself
                                db.collection("users").document(uid).delete()
                                        .addOnCompleteListener(task -> onComplete.run());
                            })
                            .addOnFailureListener(e -> onComplete.run());
                })
                .addOnFailureListener(e -> onComplete.run());
    }
}