package com.vanaksh.manomitra.ui.roles;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.chip.Chip;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.vanaksh.manomitra.R;
import com.vanaksh.manomitra.data.model.Counsellor;
import com.vanaksh.manomitra.ui.auth.LoginActivity;
import com.vanaksh.manomitra.utils.RoleManager;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import java.util.Objects;

public class CounsellorActivity extends AppCompatActivity {

        private View containerDashboard, layoutProfileForm;
        private MaterialCardView cardProfileWarning, cardOnCampus, cardHelpline;
        private Chip chipVerificationStatus;
        private TextInputLayout tilFullName, tilPhone, tilUniversity, tilSpecialization, tilExperience, tilBio,
                        tilAvailability;
        private TextInputEditText etFullName, etPhone, etUniversity, etSpecialization, etExperience, etBio,
                        etAvailability;
        private MaterialCheckBox cbAccuracy;
        private MaterialButton btnSaveProfile, btnCompleteProfile, btnCancelProfile;
        private TextView tvWelcomeTitle, tvCountPending, tvCountAccepted, tvCountCrisis, tvCategoryLabel, tvProDetails;

        private String selectedCategory = "";
        private Counsellor currentCounsellor;
        private FirebaseFirestore db;
        private FirebaseAuth mAuth;
        private ListenerRegistration dashboardListener, pendingListener, acceptedListener, crisisListener;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);
                EdgeToEdge.enable(this);
                setContentView(R.layout.activity_counsellor);

                db = FirebaseFirestore.getInstance();
                mAuth = FirebaseAuth.getInstance();

                initViews();
                setupToolbar();
                setupCards();
                setupProfileForm();
                fetchCounsellorData();
                setupRealTimeListeners();
        }

        private void initViews() {
                containerDashboard = findViewById(R.id.containerDashboard);
                layoutProfileForm = findViewById(R.id.layoutProfileForm);
                cardProfileWarning = findViewById(R.id.cardProfileWarning);
                chipVerificationStatus = findViewById(R.id.chipVerificationStatus);

                cardOnCampus = findViewById(R.id.cardOnCampus);
                cardHelpline = findViewById(R.id.cardHelpline);

                tilFullName = findViewById(R.id.tilFullName);
                tilPhone = findViewById(R.id.tilPhone);
                tilUniversity = findViewById(R.id.tilUniversity);
                tilSpecialization = findViewById(R.id.tilSpecialization);
                tilExperience = findViewById(R.id.tilExperience);
                tilBio = findViewById(R.id.tilBio);
                tilAvailability = findViewById(R.id.tilAvailability);

                etFullName = (TextInputEditText) tilFullName.getEditText();
                etPhone = (TextInputEditText) tilPhone.getEditText();
                etUniversity = (TextInputEditText) tilUniversity.getEditText();
                etSpecialization = (TextInputEditText) tilSpecialization.getEditText();
                etExperience = (TextInputEditText) tilExperience.getEditText();
                etBio = (TextInputEditText) tilBio.getEditText();
                etAvailability = (TextInputEditText) tilAvailability.getEditText();

                cbAccuracy = findViewById(R.id.cbAccuracy);
                btnSaveProfile = findViewById(R.id.btnSaveProfile);
                btnCancelProfile = findViewById(R.id.btnCancelProfile);
                btnCompleteProfile = findViewById(R.id.btnCompleteProfile);

                tvWelcomeTitle = findViewById(R.id.tvWelcomeTitle);
                tvCountPending = findViewById(R.id.tvCountPending);
                tvCountAccepted = findViewById(R.id.tvCountAccepted);
                tvCountCrisis = findViewById(R.id.tvCountCrisis);
                tvCategoryLabel = findViewById(R.id.tvCategoryLabel);
                tvProDetails = findViewById(R.id.tvProDetails);
        }

        private void setupToolbar() {
                MaterialToolbar toolbar = findViewById(R.id.toolbar);
                setSupportActionBar(toolbar);
        }

        private void fetchCounsellorData() {
                String uid = mAuth.getUid();
                if (uid == null)
                        return;

                db.collection("counsellors").document(uid).addSnapshotListener((documentSnapshot, e) -> {
                        if (e != null) {
                                Toast.makeText(this, "Error syncing profile: " + e.getMessage(), Toast.LENGTH_SHORT)
                                                .show();
                                return;
                        }

                        if (documentSnapshot != null && documentSnapshot.exists()) {
                                currentCounsellor = documentSnapshot.toObject(Counsellor.class);
                                if (currentCounsellor != null) {
                                        updateUIStatus();
                                }
                        } else {
                                // New counsellor entry might need creation from user record if missing
                                currentCounsellor = new Counsellor();
                                currentCounsellor.setId(uid);
                                currentCounsellor.setProfileCompleted(false);
                                currentCounsellor.setTrusted(false);
                                currentCounsellor.setActive(true);
                                updateUIStatus();
                        }
                });
        }

        private void updateUIStatus() {
                // Verification Badge
                if (currentCounsellor.isTrusted()) {
                        chipVerificationStatus.setText("Trusted Counsellor");
                        chipVerificationStatus.setChipBackgroundColorResource(android.R.color.holo_green_dark);
                } else {
                        chipVerificationStatus.setText("Pending Verification");
                        chipVerificationStatus.setChipBackgroundColorResource(android.R.color.holo_orange_dark);
                }

                // Warning Card
                cardProfileWarning.setVisibility(currentCounsellor.isProfileCompleted() ? View.GONE : View.VISIBLE);

                // Professional Summary
                if (currentCounsellor.isProfileCompleted()) {
                        tvWelcomeTitle.setText("Welcome, Dr. " + currentCounsellor.getName() + " 🩺");
                        tvCategoryLabel.setText(
                                        currentCounsellor.getCategory().equals("on_campus") ? "On-Campus Specialist"
                                                        : "Helpline Specialist");
                        tvProDetails.setText(currentCounsellor.getSpecialization() + " • "
                                        + currentCounsellor.getAvailability());
                        findViewById(R.id.cardProSummary).setVisibility(View.VISIBLE);
                } else {
                        tvWelcomeTitle.setText("Welcome, Counsellor 🩺");
                        findViewById(R.id.cardProSummary).setVisibility(View.GONE);
                }

                // Professional Cards Interaction
                boolean canAccept = currentCounsellor.isTrusted() && currentCounsellor.isProfileCompleted();
                findViewById(R.id.cardSessions).setAlpha(canAccept ? 1.0f : 0.5f);
                findViewById(R.id.cardCrisisAlerts).setAlpha(canAccept ? 1.0f : 0.5f);
                findViewById(R.id.cardChat).setAlpha(canAccept ? 1.0f : 0.5f);
        }

        private void setupRealTimeListeners() {
                String uid = mAuth.getUid();
                if (uid == null)
                        return;

                // Pending Counter
                pendingListener = db.collection("bookings")
                                .whereEqualTo("counsellorId", uid)
                                .whereEqualTo("status", "pending")
                                .addSnapshotListener((snapshots, e) -> {
                                        if (snapshots != null)
                                                tvCountPending.setText(String.valueOf(snapshots.size()));
                                });

                // Accepted Counter
                acceptedListener = db.collection("bookings")
                                .whereEqualTo("counsellorId", uid)
                                .whereEqualTo("status", "confirmed")
                                .addSnapshotListener((snapshots, e) -> {
                                        if (snapshots != null)
                                                tvCountAccepted.setText(String.valueOf(snapshots.size()));
                                });

                // Crisis Counter
                crisisListener = db.collection("bookings")
                                .whereEqualTo("counsellorId", uid)
                                .whereEqualTo("status", "confirmed")
                                .whereEqualTo("crisisFlag", true)
                                .addSnapshotListener((snapshots, e) -> {
                                        if (snapshots != null)
                                                tvCountCrisis.setText(String.valueOf(snapshots.size()));
                                });
        }

        private void setupProfileForm() {
                btnCompleteProfile.setOnClickListener(v -> toggleProfileForm(true));
                btnCancelProfile.setOnClickListener(v -> toggleProfileForm(false));

                cardOnCampus.setOnClickListener(v -> selectCategory("on_campus"));
                cardHelpline.setOnClickListener(v -> selectCategory("helpline"));

                btnSaveProfile.setOnClickListener(v -> saveProfile());
        }

        private void toggleProfileForm(boolean show) {
                layoutProfileForm.setVisibility(show ? View.VISIBLE : View.GONE);
                containerDashboard.setVisibility(show ? View.GONE : View.VISIBLE);
                cardProfileWarning.setVisibility(
                                show || currentCounsellor.isProfileCompleted() ? View.GONE : View.VISIBLE);
                findViewById(R.id.cardWelcome).setVisibility(show ? View.GONE : View.VISIBLE);

                if (show && currentCounsellor != null) {
                        populateForm();
                }
        }

        private void populateForm() {
                etFullName.setText(currentCounsellor.getName());
                etPhone.setText(currentCounsellor.getPhone());
                etSpecialization.setText(currentCounsellor.getSpecialization());
                etExperience.setText(String.valueOf(currentCounsellor.getExperienceYears()));
                etBio.setText(currentCounsellor.getBio());
                etAvailability.setText(currentCounsellor.getAvailability());
                if (!TextUtils.isEmpty(currentCounsellor.getCategory())) {
                        selectCategory(currentCounsellor.getCategory());
                }
                if (!TextUtils.isEmpty(currentCounsellor.getUniversityName())) {
                        etUniversity.setText(currentCounsellor.getUniversityName());
                }
        }

        private void selectCategory(String category) {
                selectedCategory = category;
                int primaryColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnPrimary,
                                ContextCompat.getColor(this, R.color.md_theme_light_primary));
                int outlineColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOutline,
                                ContextCompat.getColor(this, R.color.md_theme_light_outline));

                cardOnCampus.setStrokeColor(category.equals("on_campus") ? primaryColor : outlineColor);
                cardOnCampus.setStrokeWidth(category.equals("on_campus") ? 4 : 2);

                cardHelpline.setStrokeColor(category.equals("helpline") ? primaryColor : outlineColor);
                cardHelpline.setStrokeWidth(category.equals("helpline") ? 4 : 2);

                tilUniversity.setVisibility(category.equals("on_campus") ? View.VISIBLE : View.GONE);
        }

        private void saveProfile() {
                if (!validateForm())
                        return;

                currentCounsellor.setName(etFullName.getText().toString());
                currentCounsellor.setPhone(etPhone.getText().toString());
                currentCounsellor.setCategory(selectedCategory);
                currentCounsellor.setSpecialization(etSpecialization.getText().toString());
                currentCounsellor.setExperienceYears(Integer.parseInt(etExperience.getText().toString()));
                currentCounsellor.setBio(etBio.getText().toString());
                currentCounsellor.setAvailability(etAvailability.getText().toString());
                currentCounsellor.setUniversityName(
                                selectedCategory.equals("on_campus") ? etUniversity.getText().toString() : "");
                currentCounsellor.setProfileCompleted(true);
                currentCounsellor.setUpdatedAt(Timestamp.now());

                db.collection("counsellors").document(currentCounsellor.getId()).set(currentCounsellor)
                                .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(this, "Profile Updated Successfully", Toast.LENGTH_SHORT).show();
                                        toggleProfileForm(false);
                                        updateUIStatus();
                                })
                                .addOnFailureListener(e -> Toast
                                                .makeText(this, "Save Failed: " + e.getMessage(), Toast.LENGTH_SHORT)
                                                .show());
        }

        private boolean validateForm() {
                if (TextUtils.isEmpty(selectedCategory)) {
                        Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show();
                        return false;
                }
                if (TextUtils.isEmpty(etFullName.getText())) {
                        tilFullName.setError("Required");
                        return false;
                }
                if (TextUtils.isEmpty(etPhone.getText())) {
                        tilPhone.setError("Required");
                        return false;
                }
                if (selectedCategory.equals("on_campus") && TextUtils.isEmpty(etUniversity.getText())) {
                        tilUniversity.setError("Required for On-Campus");
                        return false;
                }
                if (!cbAccuracy.isChecked()) {
                        Toast.makeText(this, "Please confirm the accuracy of information", Toast.LENGTH_SHORT).show();
                        return false;
                }
                return true;
        }

        @Override
        public boolean onCreateOptionsMenu(Menu menu) {
                getMenuInflater().inflate(R.menu.menu_role_toolbar, menu);
                return true;
        }

        @Override
        public boolean onOptionsItemSelected(@NonNull MenuItem item) {
                if (item.getItemId() == R.id.action_logout) {
                        performLogout();
                        return true;
                }
                return super.onOptionsItemSelected(item);
        }

        private void performLogout() {
                RoleManager.performLogout(this);
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
        }

        @SuppressWarnings("deprecation")
        @Override
        public void onBackPressed() {
                if (layoutProfileForm.getVisibility() == View.VISIBLE) {
                        toggleProfileForm(false);
                } else {
                        moveTaskToBack(true);
                }
        }

        private void setupCards() {
                findViewById(R.id.cardAppointments).setOnClickListener(v -> {
                        startActivity(new Intent(this, ViewAppointmentsActivity.class));
                });

                findViewById(R.id.cardSessions).setOnClickListener(v -> {
                        if (currentCounsellor != null && currentCounsellor.isTrusted()
                                        && currentCounsellor.isProfileCompleted()) {
                                startActivity(new Intent(this, ManageSessionsActivity.class));
                        } else {
                                Toast.makeText(this, "Access Denied: Verification Required", Toast.LENGTH_SHORT).show();
                        }
                });

                findViewById(R.id.cardCrisisAlerts).setOnClickListener(v -> {
                        if (currentCounsellor != null && currentCounsellor.isTrusted()
                                        && currentCounsellor.isProfileCompleted()) {
                                startActivity(new Intent(this, CrisisAlertsActivity.class));
                        } else {
                                Toast.makeText(this, "Access Denied: Verification Required", Toast.LENGTH_SHORT).show();
                        }
                });

                findViewById(R.id.cardChat).setOnClickListener(v -> {
                        if (currentCounsellor != null && currentCounsellor.isTrusted()
                                        && currentCounsellor.isProfileCompleted()) {
                                startActivity(new Intent(this, CounsellorChatListActivity.class));
                        } else {
                                Toast.makeText(this, "Access Denied: Verification Required", Toast.LENGTH_SHORT).show();
                        }
                });
        }

        @Override
        protected void onDestroy() {
                super.onDestroy();
                if (dashboardListener != null)
                        dashboardListener.remove();
                if (pendingListener != null)
                        pendingListener.remove();
                if (acceptedListener != null)
                        acceptedListener.remove();
                if (crisisListener != null)
                        crisisListener.remove();
        }
}
