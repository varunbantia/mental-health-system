package com.vanaksh.manomitra.wellness;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.vanaksh.manomitra.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class RelaxActivity extends AppCompatActivity {

    private String[] pmrInstructions = {
        "1. Tense your toes and feet as hard as you can... (Hold for 5s)",
        "Now Release. Feel the tension draining away.",
        "2. Tense your calves and thighs... (Hold for 5s)",
        "Now Release. Your legs feel heavy and relaxed.",
        "3. Tense your stomach and chest... (Hold for 5s)",
        "Now Release. Breathe deeply and calmly.",
        "4. Clench your fists and tense your arms... (Hold for 5s)",
        "Now Release. Your arms are loose and still.",
        "5. Shrug your shoulders to your ears... (Hold for 5s)",
        "Now Release. All the weight of the day is gone.",
        "6. Close your eyes tight and scrunch your face... (Hold for 5s)",
        "Release. Your face is soft and expressionless.",
        "Take a deep breath. Your entire body is now relaxed.",
        "Goodnight. You are ready for restful sleep."
    };

    private int currentPmrIndex = -1;
    private Handler pmrHandler = new Handler(Looper.getMainLooper());
    private Runnable pmrRunnable;
    private TextView tvPmr;
    private MaterialButton btnPmr;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_relax);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, systemBars.bottom);
            return insets;
        });

        prefs = getSharedPreferences("EveningPrefs", MODE_PRIVATE);
        tvPmr = findViewById(R.id.tv_pmr_instruction);
        btnPmr = findViewById(R.id.btn_start_pmr);

        btnPmr.setOnClickListener(v -> startPmrSession());

        setupGratitudeJournal();
        setupMoodReflection();
        checkDailyReset();
    }

    private void startPmrSession() {
        btnPmr.setEnabled(false);
        btnPmr.setText("Guidance In Progress...");
        currentPmrIndex = 0;
        tvPmr.setText(pmrInstructions[currentPmrIndex]);

        pmrRunnable = new Runnable() {
            @Override
            public void run() {
                currentPmrIndex++;
                if (currentPmrIndex < pmrInstructions.length) {
                    tvPmr.setText(pmrInstructions[currentPmrIndex]);
                    pmrHandler.postDelayed(this, 6000); // 6 Sec per phase
                } else {
                    btnPmr.setEnabled(true);
                    btnPmr.setText("Restart Guidance");
                    tvPmr.setText("Guided relaxation complete. You are ready for sleep.");
                }
            }
        };
        pmrHandler.postDelayed(pmrRunnable, 6000);
    }

    private void setupGratitudeJournal() {
        EditText et1 = findViewById(R.id.et_gratitude_1);
        EditText et2 = findViewById(R.id.et_gratitude_2);
        EditText et3 = findViewById(R.id.et_gratitude_3);

        // Load saved entries
        et1.setText(prefs.getString("grat_1", ""));
        et2.setText(prefs.getString("grat_2", ""));
        et3.setText(prefs.getString("grat_3", ""));

        // Save on change
        TextWatcher tw = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                prefs.edit()
                    .putString("grat_1", et1.getText().toString())
                    .putString("grat_2", et2.getText().toString())
                    .putString("grat_3", et3.getText().toString())
                    .apply();
            }
        };

        et1.addTextChangedListener(tw);
        et2.addTextChangedListener(tw);
        et3.addTextChangedListener(tw);
    }

    private void setupMoodReflection() {
        View sad = findViewById(R.id.tv_mood_sad);
        View neutral = findViewById(R.id.tv_mood_neutral);
        View happy = findViewById(R.id.tv_mood_happy);
        View amazing = findViewById(R.id.tv_mood_amazing);

        View[] views = {sad, neutral, happy, amazing};

        for (View v : views) {
            v.setOnClickListener(view -> {
                for (View other : views) other.setAlpha(0.3f);
                view.setAlpha(1.0f);
                prefs.edit().putInt("selected_mood", view.getId()).apply();
            });
        }

        // Restore mood if saved
        int savedMoodId = prefs.getInt("selected_mood", -1);
        if (savedMoodId != -1) {
            findViewById(savedMoodId).setAlpha(1.0f);
        }
    }

    private void checkDailyReset() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().getTime());
        String lastDate = prefs.getString("last_relax_date", "");

        if (!today.equals(lastDate)) {
            // New day, clear gratitude and mood
            prefs.edit()
                .putString("grat_1", "")
                .putString("grat_2", "")
                .putString("grat_3", "")
                .putInt("selected_mood", -1)
                .putString("last_relax_date", today)
                .apply();
            
            recreate(); // Refresh UI to show empty state
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pmrHandler != null && pmrRunnable != null) {
            pmrHandler.removeCallbacks(pmrRunnable);
        }
    }
}