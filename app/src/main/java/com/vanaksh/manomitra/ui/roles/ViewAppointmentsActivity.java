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
import java.util.Comparator;
import java.util.List;

public class ViewAppointmentsActivity extends AppCompatActivity {

    private RecyclerView rvAppointments;
    private AppointmentAdapter adapter;
    private List<Booking> appointmentList = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private View progressBar, llEmptyState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_appointments);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        setupToolbar();
        initViews();
        fetchAppointments();
    }

    private void setupToolbar() {
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }

    private void initViews() {
        rvAppointments = findViewById(R.id.rvAppointments);
        progressBar = findViewById(R.id.progressBar);
        llEmptyState = findViewById(R.id.llEmptyState);

        rvAppointments.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AppointmentAdapter(appointmentList, false, null);
        rvAppointments.setAdapter(adapter);
    }

    private void fetchAppointments() {
        String uid = mAuth.getUid();
        if (uid == null)
            return;

        progressBar.setVisibility(View.VISIBLE);
        db.collection("bookings")
                .whereEqualTo("counsellorId", uid)
                .whereIn("status", List.of("confirmed", "completed", "cancelled"))
                .addSnapshotListener((snapshots, e) -> {
                    progressBar.setVisibility(View.GONE);
                    if (e != null) {
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    appointmentList.clear();
                    if (snapshots != null) {
                        for (com.google.firebase.firestore.DocumentSnapshot doc : snapshots) {
                            Booking booking = doc.toObject(Booking.class);
                            if (booking != null) {
                                booking.setBookingId(doc.getId());
                                appointmentList.add(booking);
                            }
                        }

                        // Sort in-memory to avoid index requirement
                        Collections.sort(appointmentList, (b1, b2) -> {
                            if (b1.getCreatedAt() == null || b2.getCreatedAt() == null)
                                return 0;
                            return b2.getCreatedAt().compareTo(b1.getCreatedAt()); // Descending
                        });
                    }

                    llEmptyState.setVisibility(appointmentList.isEmpty() ? View.VISIBLE : View.GONE);
                    adapter.notifyDataSetChanged();
                });
    }
}
