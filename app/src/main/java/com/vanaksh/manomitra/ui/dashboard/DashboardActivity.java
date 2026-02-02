package com.vanaksh.manomitra.ui.dashboard;

import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.vanaksh.manomitra.R;

public class DashboardActivity extends AppCompatActivity {

    private TextView tvMoodStatusMessage;
    private ImageButton[] moodButtons;
    private BottomNavigationView bottomNavigation;
    private MaterialCardView cardStartChat;

    // Wellness Tool Cards
    private CardView cardRelax, cardLearn, cardSleep;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard);

        // Handle Window Insets for Edge-to-Edge
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        setupMoodTracker();
        setupWellnessTools(); // Initialize your new horizontal tools
        setupNavigation();
    }

    private void initViews() {
        tvMoodStatusMessage = findViewById(R.id.tvMoodStatusMessage);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        cardStartChat = findViewById(R.id.cardStartChat);

        // Mood Buttons initialization
        moodButtons = new ImageButton[]{
                findViewById(R.id.btnMoodGood),
                findViewById(R.id.btnMoodOkay),
                findViewById(R.id.btnMoodLow),
                findViewById(R.id.btnMoodVeryLow),
        };

        // Wellness Cards initialization (from your included layout)
        cardRelax = findViewById(R.id.cardRelax);
        cardLearn = findViewById(R.id.cardLearn);
        cardSleep = findViewById(R.id.cardSleep);
    }

    private void setupWellnessTools() {
        // Relax Card Click
        cardRelax.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            Toast.makeText(this, "Opening Relaxing Audio...", Toast.LENGTH_SHORT).show();
            // Start Activity: startActivity(new Intent(this, RelaxActivity.class));
        });

        // Learn Card Click
        cardLearn.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            Toast.makeText(this, "Opening Wellness Guides...", Toast.LENGTH_SHORT).show();
        });

        // Sleep Card Click
        cardSleep.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            Toast.makeText(this, "Opening Sleep Tips...", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupMoodTracker() {
        moodButtons[0].setOnClickListener(v ->
                updateMoodSelection(0, "That's wonderful! Let's keep that positive energy flowing! 🌟"));

        moodButtons[1].setOnClickListener(v ->
                updateMoodSelection(1, "Glad you're doing well. It's a great day to be productive! 😊"));

        moodButtons[2].setOnClickListener(v ->
                updateMoodSelection(2, "It's okay to feel 'just okay'. Take a deep breath with us. 🧘"));

        moodButtons[3].setOnClickListener(v ->
                updateMoodSelection(3, "I'm sorry things are tough. Manomitra is here to listen. 🤝"));
    }

    private void updateMoodSelection(int index, String message) {
        boolean isAlreadySelected = moodButtons[index].isSelected();

        // Reset all buttons
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

            // Example of Toast feedback for other nav items
            String title = item.getTitle().toString();
            Toast.makeText(this, "Opening " + title + "...", Toast.LENGTH_SHORT).show();
            return true;
        });

        cardStartChat.setOnClickListener(v -> {
            Toast.makeText(this, "Starting Chat Session...", Toast.LENGTH_SHORT).show();
        });
    }
}