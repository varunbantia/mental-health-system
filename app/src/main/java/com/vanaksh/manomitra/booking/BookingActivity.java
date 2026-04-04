package com.vanaksh.manomitra.booking;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.vanaksh.manomitra.R;
import com.vanaksh.manomitra.data.model.Counsellor;
import com.vanaksh.manomitra.ui.crisis.EmergencyActivity;

import java.util.ArrayList;
import java.util.List;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class BookingActivity extends AppCompatActivity {

    private RecyclerView rvCounsellors;
    private View llCounsellorList;
    private TextView tvListTitle, tvNoCounsellors;
    private ProgressBar pbCounsellors;
    private CounsellorAdapter adapter;
    private List<Counsellor> counsellorList = new ArrayList<>();
    private FirebaseFirestore db;
    private com.google.firebase.firestore.ListenerRegistration counsellorListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_booking);

        db = FirebaseFirestore.getInstance();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();

        // --- Card click handlers ---
        MaterialCardView cardCounselor = findViewById(R.id.cardCounselor);
        MaterialCardView cardHelpline = findViewById(R.id.cardHelpline);

        cardCounselor.setOnClickListener(v -> fetchCounsellors("on_campus"));
        cardHelpline.setOnClickListener(v -> fetchCounsellors("helpline"));

        // --- My Bookings ---
        findViewById(R.id.btnMyBookings).setOnClickListener(v -> {
            startActivity(new Intent(this, BookingStatusActivity.class));
        });

        // --- Emergency shortcut ---
        findViewById(R.id.btnEmergency).setOnClickListener(v -> {
            startActivity(new Intent(this, EmergencyActivity.class));
        });
    }

    private void initViews() {
        llCounsellorList = findViewById(R.id.llCounsellorList);
        rvCounsellors = findViewById(R.id.rvCounsellors);
        tvListTitle = findViewById(R.id.tvListTitle);
        tvNoCounsellors = findViewById(R.id.tvNoCounsellors);
        pbCounsellors = findViewById(R.id.pbCounsellors);

        rvCounsellors.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CounsellorAdapter();
        rvCounsellors.setAdapter(adapter);
    }

    private void fetchCounsellors(String category) {
        if (counsellorListener != null) {
            counsellorListener.remove();
        }

        llCounsellorList.setVisibility(View.VISIBLE);
        pbCounsellors.setVisibility(View.VISIBLE);
        rvCounsellors.setVisibility(View.GONE);
        tvNoCounsellors.setVisibility(View.GONE);

        tvListTitle.setText(category.equals("on_campus") ? "On-Campus Specialists" : "Helpline Specialists");

        counsellorListener = db.collection("counsellors")
                .whereEqualTo("category", category)
                .whereEqualTo("profileCompleted", true)
                .whereEqualTo("active", true)
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    pbCounsellors.setVisibility(View.GONE);
                    if (e != null) {
                        Toast.makeText(this, "Error syncing specialists: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    counsellorList.clear();
                    if (queryDocumentSnapshots != null) {
                        for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                            Counsellor counsellor = doc.toObject(Counsellor.class);
                            if (counsellor != null) {
                                counsellor.setId(doc.getId());
                                counsellorList.add(counsellor);
                            }
                        }
                    }

                    if (counsellorList.isEmpty()) {
                        tvNoCounsellors.setVisibility(View.VISIBLE);
                        rvCounsellors.setVisibility(View.GONE);
                    } else {
                        tvNoCounsellors.setVisibility(View.GONE);
                        rvCounsellors.setVisibility(View.VISIBLE);
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (counsellorListener != null) {
            counsellorListener.remove();
        }
    }

    private void openDateTimeSelection(String counsellorId, String category) {
        Intent intent = new Intent(this, DateTimeSelectionActivity.class);
        intent.putExtra("COUNSELLOR_ID", counsellorId);
        intent.putExtra("BOOKING_TYPE", category.equals("on_campus") ? "counselor" : "helpline");
        startActivity(intent);
    }

    private class CounsellorAdapter extends RecyclerView.Adapter<CounsellorAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_counsellor, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Counsellor counsellor = counsellorList.get(position);
            holder.tvName.setText(counsellor.getName());
            holder.tvSpecialization.setText(counsellor.getSpecialization());
            holder.tvExperience.setText(counsellor.getExperienceYears() + " years experience");
            holder.tvAvailability.setText("Available: " + counsellor.getAvailability());

            // Category Label
            if ("on_campus".equals(counsellor.getCategory())) {
                holder.tvCategoryLabel.setText("On-Campus Counselor");
                holder.tvUniversity.setVisibility(View.VISIBLE);
                holder.tvUniversity.setText(counsellor.getUniversityName());
            } else {
                holder.tvCategoryLabel.setText("Mental Health Helpline");
                holder.tvUniversity.setVisibility(View.GONE);
            }

            // Verification Status
            if (counsellor.isTrusted()) {
                holder.tvBadgeText.setText("Verified");
                holder.badgeVerification.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFE8F5E9)); // Light
                                                                                                                        // Green
                holder.tvBadgeText.setTextColor(0xFF2E7D32); // Dark Green
                holder.ivBadgeIcon.setColorFilter(0xFF2E7D32);
                holder.ivBadgeIcon.setImageResource(R.drawable.ic_check_circle);
            } else {
                holder.tvBadgeText.setText("Verification Pending");
                holder.badgeVerification.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFFFF3E0)); // Light
                                                                                                                        // Orange
                holder.tvBadgeText.setTextColor(0xFFEF6C00); // Dark Orange
                holder.ivBadgeIcon.setColorFilter(0xFFEF6C00);
                holder.ivBadgeIcon.setImageResource(R.drawable.ic_person);
            }

            holder.itemView.setOnClickListener(v -> {
                if (counsellor.isTrusted()) {
                    openDateTimeSelection(counsellor.getId(), counsellor.getCategory());
                } else {
                    Toast.makeText(BookingActivity.this, "Specialist verification in progress. Please try again later.",
                            Toast.LENGTH_LONG).show();
                }
            });
            holder.itemView.setEnabled(counsellor.isTrusted());
            holder.itemView.setAlpha(counsellor.isTrusted() ? 1.0f : 0.7f);
        }

        @Override
        public int getItemCount() {
            return counsellorList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvSpecialization, tvExperience, tvUniversity, tvAvailability, tvCategoryLabel, tvBadgeText;
            ImageView ivBadgeIcon;
            View badgeVerification;

            ViewHolder(View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvName);
                tvSpecialization = itemView.findViewById(R.id.tvSpecialization);
                tvExperience = itemView.findViewById(R.id.tvExperience);
                tvUniversity = itemView.findViewById(R.id.tvUniversity);
                tvAvailability = itemView.findViewById(R.id.tvAvailability);
                tvCategoryLabel = itemView.findViewById(R.id.tvCategoryLabel);
                tvBadgeText = itemView.findViewById(R.id.tvBadgeText);
                ivBadgeIcon = itemView.findViewById(R.id.ivBadgeIcon);
                badgeVerification = itemView.findViewById(R.id.badgeVerification);
            }
        }
    }
}