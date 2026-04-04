package com.vanaksh.manomitra.booking;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.vanaksh.manomitra.R;
import com.vanaksh.manomitra.data.model.Booking;
import com.vanaksh.manomitra.data.repository.BookingRepository;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class DateTimeSelectionActivity extends AppCompatActivity {

    private String bookingType;
    private String counsellorId;
    private String selectedDate = null;
    private String selectedTime = null;
    private String selectedConcern = null;

    private MaterialButton btnPickDate;
    private ChipGroup chipGroupTime;
    private AutoCompleteTextView actvConcern;
    private BookingRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_date_time_selection);

        repository = new BookingRepository();
        bookingType = getIntent().getStringExtra("BOOKING_TYPE");
        counsellorId = getIntent().getStringExtra("COUNSELLOR_ID");
        if (bookingType == null)
            bookingType = "counselor";

        // --- Header ---
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        TextView tvBookingType = findViewById(R.id.tvBookingType);
        tvBookingType.setText(bookingType.equals("counselor")
                ? "On-campus Counselor"
                : "Mental Health Helpline");

        // --- Date Picker ---
        btnPickDate = findViewById(R.id.btnPickDate);
        btnPickDate.setOnClickListener(v -> showDatePicker());

        // --- Time Chips ---
        chipGroupTime = findViewById(R.id.chipGroupTime);
        chipGroupTime.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                Chip chip = group.findViewById(checkedIds.get(0));
                selectedTime = chip != null ? chip.getText().toString() : null;
            } else {
                selectedTime = null;
            }
        });

        // --- Concern Category Dropdown ---
        actvConcern = findViewById(R.id.actvConcern);
        String[] concerns = { "Anxiety", "Depression", "Academic Stress", "Relationship Issues",
                "Sleep Problems", "Loneliness", "Other" };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, concerns);
        actvConcern.setAdapter(adapter);

        // --- Submit ---
        MaterialButton btnSubmit = findViewById(R.id.btnSubmit);
        btnSubmit.setOnClickListener(v -> submitBooking());
    }

    private void showDatePicker() {
        CalendarConstraints constraints = new CalendarConstraints.Builder()
                .setValidator(DateValidatorPointForward.now())
                .build();

        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select appointment date")
                .setCalendarConstraints(constraints)
                .build();

        picker.addOnPositiveButtonClickListener(selection -> {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            selectedDate = sdf.format(new Date(selection));

            SimpleDateFormat display = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            display.setTimeZone(TimeZone.getTimeZone("UTC"));
            btnPickDate.setText(display.format(new Date(selection)));
        });

        picker.show(getSupportFragmentManager(), "DATE_PICKER");
    }

    private void submitBooking() {
        if (selectedDate == null) {
            Toast.makeText(this, "Please select a date", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedTime == null) {
            Toast.makeText(this, "Please select a time slot", Toast.LENGTH_SHORT).show();
            return;
        }

        String userRef = repository.getCurrentUserRef();
        if (userRef == null) {
            Toast.makeText(this, "Please sign in to book", Toast.LENGTH_SHORT).show();
            return;
        }

        selectedConcern = actvConcern.getText().toString().trim();
        if (selectedConcern.isEmpty())
            selectedConcern = "Not specified";

        String bookingId = repository.generateBookingId();

        Booking booking = new Booking(
                bookingId, userRef, bookingType,
                counsellorId, selectedConcern, selectedDate, selectedTime,
                Booking.STATUS_PENDING);
        booking.setStudentId(repository.getCurrentUid());

        repository.createBooking(booking, new BookingRepository.BookingCallback() {
            @Override
            public void onSuccess(Booking b) {
                // Fire local notification
                new BookingNotificationHelper(DateTimeSelectionActivity.this)
                        .notifyBookingCreated(b.getBookingId());

                // Navigate to confirmation
                Intent intent = new Intent(DateTimeSelectionActivity.this,
                        BookingConfirmationActivity.class);
                intent.putExtra("BOOKING_ID", b.getBookingId());
                intent.putExtra("BOOKING_TYPE", bookingType);
                intent.putExtra("BOOKING_DATE", selectedDate);
                intent.putExtra("BOOKING_TIME", selectedTime);
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(DateTimeSelectionActivity.this,
                        "Booking failed. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
