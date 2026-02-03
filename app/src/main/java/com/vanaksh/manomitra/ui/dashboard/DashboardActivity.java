package com.vanaksh.manomitra.ui.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.Button; // or MaterialButton
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.vanaksh.manomitra.R;
import com.vanaksh.manomitra.booking.BookingActivity;
import com.vanaksh.manomitra.community.CommunityActivity;
import com.vanaksh.manomitra.resouurces.ResourcesActivity;
import com.vanaksh.manomitra.ui.chatbot.ChatbotActivity;
import com.vanaksh.manomitra.ui.crisis.EmergencyActivity;
import com.vanaksh.manomitra.wellness.LearmActivity;
import com.vanaksh.manomitra.wellness.RelaxActivity;
import com.vanaksh.manomitra.wellness.SleepActivity;
import com.vanaksh.manomitra.wellness.WellnessHubActivity;

// Import your Booking Activity
// import com.vanaksh.manomitra.booking.BookingActivity;

public class DashboardActivity extends AppCompatActivity {
    private TextView tvEmergencySupport;
    private TextView tvMoodStatusMessage;
    private ImageButton[] moodButtons;
    private View cardStartChat;
    private BottomNavigationView bottomNavigation;

    // Wellness Tool Cards
    private CardView cardRelax, cardLearn, cardSleep;

    // Support Section Buttons
    private View btnBookNow, btnFindCommunity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        checkUserAuthentication();
        setupMoodTracker();
        setupWellnessTools();
        setupNavigation();
        setupSupportActions(); // New method for the Book/Find buttons
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bottomNavigation != null) {
            bottomNavigation.getMenu().findItem(R.id.nav_home).setChecked(true);
        }
    }

    private void initViews() {
        tvMoodStatusMessage = findViewById(R.id.tvMoodStatusMessage);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        cardStartChat = findViewById(R.id.cardStartChat);
        tvEmergencySupport = findViewById(R.id.tvEmergencySupport);
        moodButtons = new ImageButton[]{
                findViewById(R.id.btnMoodGood),
                findViewById(R.id.btnMoodOkay),
                findViewById(R.id.btnMoodLow),
                findViewById(R.id.btnMoodVeryLow),
        };

        cardRelax = findViewById(R.id.cardRelax);
        cardLearn = findViewById(R.id.cardLearn);
        cardSleep = findViewById(R.id.cardSleep);

        // Initialize Book and Find buttons from the included layouts
        btnBookNow = findViewById(R.id.btnBookNow);
        btnFindCommunity = findViewById(R.id.btnFindCommunity);
    }

    private void setupSupportActions() {
        // "Book" button logic
        if (btnBookNow != null) {
            btnBookNow.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                // Replace with your actual BookingActivity class
                startActivity(new Intent(this, BookingActivity.class));
            });
        }

        // "Find" button logic
        if (btnFindCommunity != null) {
            btnFindCommunity.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                startActivity(new Intent(this, CommunityActivity.class));
            });
        }
    }

    private void checkUserAuthentication() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            boolean isActuallyAnonymous = true;
            for (UserInfo profile : user.getProviderData()) {
                if (profile.getProviderId().equals("google.com") || profile.getProviderId().equals("password")) {
                    isActuallyAnonymous = false;
                    break;
                }
            }
            if (!isActuallyAnonymous) {
                View anonymousView = findViewById(R.id.chipAnonymous);
                if (anonymousView != null) {
                    anonymousView.setVisibility(View.GONE);
                }
            }
        }
    }

    private void setupWellnessTools() {
        cardRelax.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            startActivity(new Intent(this, RelaxActivity.class));
        });

        cardLearn.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            startActivity(new Intent(this, LearmActivity.class));
        });

        cardSleep.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            startActivity(new Intent(this, SleepActivity.class));
        });
    }

    private void setupMoodTracker() {
        moodButtons[0].setOnClickListener(v -> updateMoodSelection(0, "That's wonderful! 🌟"));
        moodButtons[1].setOnClickListener(v -> updateMoodSelection(1, "Glad you're doing well. 😊"));
        moodButtons[2].setOnClickListener(v -> updateMoodSelection(2, "It's okay to feel 'just okay'. 🧘"));
        moodButtons[3].setOnClickListener(v -> updateMoodSelection(3, "I'm sorry things are tough. 🤝"));
    }

    private void updateMoodSelection(int index, String message) {
        boolean isAlreadySelected = moodButtons[index].isSelected();
        for (ImageButton btn : moodButtons) {
            btn.setSelected(false);
        }
        if (isAlreadySelected) {
            tvMoodStatusMessage.setText("Select a mood to see how Manomitra can help you today.");
        } else {
            moodButtons[index].setSelected(true);
            tvMoodStatusMessage.setText(message);
            moodButtons[index].performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        }
    }

    private void setupNavigation() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) return true;

            Intent intent;
            if (id == R.id.nav_resources) intent = new Intent(this, ResourcesActivity.class);
            else if (id == R.id.nav_book) intent = new Intent(this, BookingActivity.class);
            else if (id == R.id.nav_community) intent = new Intent(this, CommunityActivity.class);
            else if (id == R.id.nav_settings) intent = new Intent(this, SettingsActivity.class);
            else return false;

            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            return true;
        });
        if (tvEmergencySupport != null) {
            tvEmergencySupport.setOnClickListener(v -> {
                // High intensity haptic feedback for emergency
                v.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);

                // Option A: Open your Emergency Activity
                Intent intent = new Intent(this, EmergencyActivity.class);
                startActivity(intent);

                // Option B: Directly open Phone Dialer with a helpline number
                // String phone = "911"; // Replace with actual helpline
                // Intent intent = new Intent(Intent.ACTION_DIAL, android.net.Uri.parse("tel:" + phone));
                // startActivity(intent);
            });
            cardStartChat.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                startActivity(new Intent(this, ChatbotActivity.class));
            });
        }
    }
}