package com.vanaksh.manomitra.ui.roles;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.vanaksh.manomitra.R;
import com.vanaksh.manomitra.data.model.Booking;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ManageSessionsActivity extends AppCompatActivity
        implements AppointmentAdapter.OnAppointmentActionListener {

    private RecyclerView rvAppointments;
    private AppointmentAdapter adapter;
    private List<Booking> pendingSessions = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private View progressBar, llEmptyState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_appointments); // Reusing layout

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        setupToolbar();
        initViews();
        fetchPendingSessions();
    }

    private void setupToolbar() {
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Manage Sessions");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }

    private void initViews() {
        rvAppointments = findViewById(R.id.rvAppointments);
        progressBar = findViewById(R.id.progressBar);
        llEmptyState = findViewById(R.id.llEmptyState);

        rvAppointments.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AppointmentAdapter(pendingSessions, true, this);
        rvAppointments.setAdapter(adapter);
    }

    private void fetchPendingSessions() {
        String uid = mAuth.getUid();
        if (uid == null)
            return;

        progressBar.setVisibility(View.VISIBLE);
        db.collection("bookings")
                .whereEqualTo("counsellorId", uid)
                .whereEqualTo("status", "pending")
                .addSnapshotListener((snapshots, e) -> {
                    progressBar.setVisibility(View.GONE);
                    if (e != null) {
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    pendingSessions.clear();
                    if (snapshots != null) {
                        for (com.google.firebase.firestore.DocumentSnapshot doc : snapshots) {
                            Booking booking = doc.toObject(Booking.class);
                            if (booking != null) {
                                booking.setBookingId(doc.getId());
                                pendingSessions.add(booking);
                            }
                        }

                        // Sort in-memory to avoid index requirement
                        Collections.sort(pendingSessions, (b1, b2) -> {
                            if (b1.getCreatedAt() == null || b2.getCreatedAt() == null)
                                return 0;
                            return b2.getCreatedAt().compareTo(b1.getCreatedAt()); // Descending
                        });
                    }

                    llEmptyState.setVisibility(pendingSessions.isEmpty() ? View.VISIBLE : View.GONE);
                    adapter.notifyDataSetChanged();
                });
    }

    @Override
    public void onAccept(Booking booking) {
        updateStatus(booking, "confirmed");
    }

    @Override
    public void onReject(Booking booking) {
        updateStatus(booking, "rejected");
    }

    private void updateStatus(Booking booking, String newStatus) {
        db.collection("bookings").document(booking.getBookingId())
                .update("status", newStatus)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Session " + newStatus, Toast.LENGTH_SHORT).show())
                .addOnFailureListener(
                        e -> Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
