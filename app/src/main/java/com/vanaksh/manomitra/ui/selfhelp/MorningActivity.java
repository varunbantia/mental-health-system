package com.vanaksh.manomitra.ui.selfhelp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.vanaksh.manomitra.R;

import java.util.Random;

public class MorningActivity extends AppCompatActivity {

    private SharedPreferences prefs;
    private CountDownTimer breathTimer;
    private boolean isBreathing = false;

    private String[] morningQuotes = {
        "Every morning we are born again. What we do today is what matters most.",
        "Write it on your heart that every day is the best day in the year.",
        "When you arise in the morning, think of what a precious privilege it is to be alive.",
        "Your future is created by what you do today, not tomorrow.",
        "Some days you just have to create your own sunshine.",
        "Rise up, start fresh, see the bright opportunity in each day."
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_morning);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, systemBars.bottom);
            return insets;
        });

        prefs = getSharedPreferences("MorningEnergyPrefs", MODE_PRIVATE);

        // 1. Setup Quote
        setupDailyQuote();

        // 2. Setup Intention Setter
        setupIntention();

        // 3. Setup Breathing Tool
        setupBreathingTool();

        // 4. Setup Gratitude Journal
        setupGratitude();

        // 5. Setup Habits
        setupHabits();
    }

    private void setupDailyQuote() {
        TextView tvQuote = findViewById(R.id.tv_daily_quote);
        int randomIndex = new Random().nextInt(morningQuotes.length);
        tvQuote.setText("\"" + morningQuotes[randomIndex] + "\"");
    }

    private void setupIntention() {
        EditText etIntention = findViewById(R.id.et_intention);
        MaterialButton btnSaveIntention = findViewById(R.id.btn_save_intention);

        etIntention.setText(prefs.getString("daily_intention", ""));

        btnSaveIntention.setOnClickListener(v -> {
            prefs.edit().putString("daily_intention", etIntention.getText().toString()).apply();
            Toast.makeText(this, "Intention saved for the day!", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupBreathingTool() {
        ProgressBar progressBreath = findViewById(R.id.progress_breath);
        TextView tvBreathInstruction = findViewById(R.id.tv_breath_instruction);
        MaterialButton btnStartBreathing = findViewById(R.id.btn_start_breathing);

        btnStartBreathing.setOnClickListener(v -> {
            if (isBreathing) {
                // Stop manually
                if (breathTimer != null) breathTimer.cancel();
                isBreathing = false;
                btnStartBreathing.setText("Start 60s Breathing");
                tvBreathInstruction.setText("Ready");
                progressBreath.setProgress(0);
                return;
            }

            isBreathing = true;
            btnStartBreathing.setText("Stop Breathing");

            // 60 Seconds Timer
            breathTimer = new CountDownTimer(60000, 50) {
                @Override
                public void onTick(long millisUntilFinished) {
                    float elapsed = 60000f - millisUntilFinished;
                    
                    // Simple Box-breathing model: 4s phases (16s full cycle)
                    long cycleTime = (long) (elapsed % 16000);
                    
                    if (cycleTime < 4000) {
                        tvBreathInstruction.setText("Inhale...");
                        progressBreath.setProgress((int) ((cycleTime / 4000f) * 100));
                    } else if (cycleTime < 8000) {
                        tvBreathInstruction.setText("Hold");
                        progressBreath.setProgress(100);
                    } else if (cycleTime < 12000) {
                        tvBreathInstruction.setText("Exhale...");
                        long exhaleTime = cycleTime - 8000;
                        progressBreath.setProgress(100 - (int) ((exhaleTime / 4000f) * 100));
                    } else {
                        tvBreathInstruction.setText("Hold");
                        progressBreath.setProgress(0);
                    }
                }

                @Override
                public void onFinish() {
                    isBreathing = false;
                    btnStartBreathing.setText("Start 60s Breathing");
                    tvBreathInstruction.setText("Done!");
                    progressBreath.setProgress(0);
                    Toast.makeText(MorningActivity.this, "Great job! Your nervous system is now awake.", Toast.LENGTH_LONG).show();
                }
            }.start();
        });
    }

    private void setupGratitude() {
        EditText etGrat1 = findViewById(R.id.et_gratitude_1);
        EditText etGrat2 = findViewById(R.id.et_gratitude_2);
        EditText etGrat3 = findViewById(R.id.et_gratitude_3);
        MaterialButton btnSaveGratitude = findViewById(R.id.btn_save_gratitude);

        etGrat1.setText(prefs.getString("grat_1", ""));
        etGrat2.setText(prefs.getString("grat_2", ""));
        etGrat3.setText(prefs.getString("grat_3", ""));

        btnSaveGratitude.setOnClickListener(v -> {
            prefs.edit()
                .putString("grat_1", etGrat1.getText().toString())
                .putString("grat_2", etGrat2.getText().toString())
                .putString("grat_3", etGrat3.getText().toString())
                .apply();
            Toast.makeText(this, "Gratitude saved! You're off to a great start.", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupHabits() {
        CheckBox cbWater = findViewById(R.id.cb_water);
        CheckBox cbSunlight = findViewById(R.id.cb_sunlight);
        CheckBox cbNoPhone = findViewById(R.id.cb_no_phone);
        CheckBox cbStretch = findViewById(R.id.cb_stretch);
        CheckBox cbMadeBed = findViewById(R.id.cb_made_bed);

        cbWater.setChecked(prefs.getBoolean("habit_water", false));
        cbSunlight.setChecked(prefs.getBoolean("habit_sunlight", false));
        cbNoPhone.setChecked(prefs.getBoolean("habit_nophone", false));
        cbStretch.setChecked(prefs.getBoolean("habit_stretch", false));
        cbMadeBed.setChecked(prefs.getBoolean("habit_madebed", false));

        cbWater.setOnCheckedChangeListener((buttonView, isChecked) -> prefs.edit().putBoolean("habit_water", isChecked).apply());
        cbSunlight.setOnCheckedChangeListener((buttonView, isChecked) -> prefs.edit().putBoolean("habit_sunlight", isChecked).apply());
        cbNoPhone.setOnCheckedChangeListener((buttonView, isChecked) -> prefs.edit().putBoolean("habit_nophone", isChecked).apply());
        cbStretch.setOnCheckedChangeListener((buttonView, isChecked) -> prefs.edit().putBoolean("habit_stretch", isChecked).apply());
        cbMadeBed.setOnCheckedChangeListener((buttonView, isChecked) -> prefs.edit().putBoolean("habit_madebed", isChecked).apply());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (breathTimer != null) {
            breathTimer.cancel();
        }
    }
}