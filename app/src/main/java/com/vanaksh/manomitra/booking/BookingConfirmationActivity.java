package com.vanaksh.manomitra.booking;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.vanaksh.manomitra.R;

public class BookingConfirmationActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_confirmation);

        String bookingId = getIntent().getStringExtra("BOOKING_ID");
        String bookingType = getIntent().getStringExtra("BOOKING_TYPE");
        String date = getIntent().getStringExtra("BOOKING_DATE");
        String time = getIntent().getStringExtra("BOOKING_TIME");

        // --- Populate summary ---
        TextView tvBookingId = findViewById(R.id.tvBookingId);
        TextView tvType = findViewById(R.id.tvType);
        TextView tvDate = findViewById(R.id.tvDate);
        TextView tvTime = findViewById(R.id.tvTime);

        tvBookingId.setText(bookingId != null ? bookingId : "—");
        tvType.setText(bookingType != null && bookingType.equals("counselor")
                ? "On-campus Counselor"
                : "Mental Health Helpline");
        tvDate.setText(date != null ? date : "—");
        tvTime.setText(time != null ? time : "—");

        // --- Buttons ---
        MaterialButton btnViewBookings = findViewById(R.id.btnViewBookings);
        btnViewBookings.setOnClickListener(v -> {
            startActivity(new Intent(this, BookingStatusActivity.class));
            finish();
        });

        MaterialButton btnDone = findViewById(R.id.btnDone);
        btnDone.setOnClickListener(v -> {
            // Return to booking selection
            Intent intent = new Intent(this, BookingActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });
    }

    @Override
    public void onBackPressed() {
        // Prevent back to date/time screen; go to selection instead
        Intent intent = new Intent(this, BookingActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
}
