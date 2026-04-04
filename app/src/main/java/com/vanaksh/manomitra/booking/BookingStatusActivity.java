package com.vanaksh.manomitra.booking;

import com.vanaksh.manomitra.ui.roles.StudentChatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.vanaksh.manomitra.R;
import com.vanaksh.manomitra.data.model.Booking;
import com.vanaksh.manomitra.data.repository.BookingRepository;

import java.util.ArrayList;
import java.util.List;

public class BookingStatusActivity extends AppCompatActivity {

    private RecyclerView recyclerBookings;
    private LinearLayout layoutEmpty;
    private BookingRepository repository;
    private BookingStatusAdapter adapter;
    private List<Booking> bookingList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_status);

        repository = new BookingRepository();
        recyclerBookings = findViewById(R.id.recyclerBookings);
        layoutEmpty = findViewById(R.id.layoutEmpty);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        bookingList = new ArrayList<>();
        adapter = new BookingStatusAdapter(bookingList, this::onCancelBooking);
        recyclerBookings.setLayoutManager(new LinearLayoutManager(this));
        recyclerBookings.setAdapter(adapter);

        loadBookings();
    }

    private void loadBookings() {
        String studentId = repository.getCurrentUid();
        if (studentId == null) {
            showEmpty();
            return;
        }

        repository.getBookingsByStudent(studentId, new BookingRepository.BookingListCallback() {
            @Override
            public void onSuccess(List<Booking> bookings) {
                bookingList.clear();
                bookingList.addAll(bookings);
                adapter.notifyDataSetChanged();

                if (bookings.isEmpty()) {
                    showEmpty();
                } else {
                    layoutEmpty.setVisibility(View.GONE);
                    recyclerBookings.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(BookingStatusActivity.this,
                        "Could not load bookings", Toast.LENGTH_SHORT).show();
                showEmpty();
            }
        });
    }

    private void onCancelBooking(Booking booking) {
        repository.cancelBooking(booking.getBookingId(), new BookingRepository.BookingCallback() {
            @Override
            public void onSuccess(Booking b) {
                new BookingNotificationHelper(BookingStatusActivity.this)
                        .notifyBookingStatusChanged(b.getBookingId());
                loadBookings(); // Refresh list
            }

            @Override
            public void onError(String error) {
                Toast.makeText(BookingStatusActivity.this,
                        "Could not cancel booking", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showEmpty() {
        layoutEmpty.setVisibility(View.VISIBLE);
        recyclerBookings.setVisibility(View.GONE);
    }
}
