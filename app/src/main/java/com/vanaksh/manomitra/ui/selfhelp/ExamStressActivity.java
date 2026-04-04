package com.vanaksh.manomitra.ui.selfhelp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.vanaksh.manomitra.R;

import java.util.Random;

public class ExamStressActivity extends AppCompatActivity {

    private String[] mantras = {
        "One exam does not define my intelligence or my future.",
        "I am exactly where I need to be, doing what I need to do.",
        "I have prepared well, and I trust my knowledge.",
        "Focus on progress, not perfection.",
        "I am capable of handling this challenge calmly.",
        "My worth is not tied to a single test score.",
        "Deep breaths. I can do this, one question at a time.",
        "I've succeeded before, and I will succeed again."
    };

    private SharedPreferences prefs;
    private TextView tvMantra;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_exam_stress);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, systemBars.bottom);
            return insets;
        });

        prefs = getSharedPreferences("ExamStressPrefs", MODE_PRIVATE);
        tvMantra = findViewById(R.id.tv_mantra);
        MaterialButton btnNewMantra = findViewById(R.id.btn_new_mantra);

        // Show a random mantra on start
        showRandomMantra();

        btnNewMantra.setOnClickListener(v -> showRandomMantra());

        setupGroundingChecklist();
    }

    private void showRandomMantra() {
        int index = new Random().nextInt(mantras.length);
        tvMantra.setText(mantras[index]);
    }

    private void setupGroundingChecklist() {
        CheckBox see = findViewById(R.id.cb_5_see);
        CheckBox touch = findViewById(R.id.cb_4_touch);
        CheckBox hear = findViewById(R.id.cb_3_hear);
        CheckBox smell = findViewById(R.id.cb_2_smell);
        CheckBox taste = findViewById(R.id.cb_1_taste);

        // Load state
        see.setChecked(prefs.getBoolean("ground_see", false));
        touch.setChecked(prefs.getBoolean("ground_touch", false));
        hear.setChecked(prefs.getBoolean("ground_hear", false));
        smell.setChecked(prefs.getBoolean("ground_smell", false));
        taste.setChecked(prefs.getBoolean("ground_taste", false));

        // Save state on change
        see.setOnCheckedChangeListener((b, val) -> prefs.edit().putBoolean("ground_see", val).apply());
        touch.setOnCheckedChangeListener((b, val) -> prefs.edit().putBoolean("ground_touch", val).apply());
        hear.setOnCheckedChangeListener((b, val) -> prefs.edit().putBoolean("ground_hear", val).apply());
        smell.setOnCheckedChangeListener((b, val) -> prefs.edit().putBoolean("ground_smell", val).apply());
        taste.setOnCheckedChangeListener((b, val) -> prefs.edit().putBoolean("ground_taste", val).apply());
    }
}