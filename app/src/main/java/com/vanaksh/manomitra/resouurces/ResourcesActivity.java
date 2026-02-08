package com.vanaksh.manomitra.resouurces;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import androidx.appcompat.app.AppCompatActivity;
import com.vanaksh.manomitra.R;
import com.vanaksh.manomitra.ui.selfhelp.AffirmationsActivity;
import com.vanaksh.manomitra.ui.selfhelp.BreathingActivity;
import com.vanaksh.manomitra.ui.selfhelp.ExamStressActivity;
import com.vanaksh.manomitra.ui.selfhelp.FocusActivity;
import com.vanaksh.manomitra.ui.selfhelp.MorningActivity;
import com.vanaksh.manomitra.ui.selfhelp.MusicActivity;
import com.vanaksh.manomitra.ui.selfhelp.NatureActivity;
import com.vanaksh.manomitra.ui.selfhelp.OceanActivity;
import com.vanaksh.manomitra.selftips.SelfCareActivity;
import com.vanaksh.manomitra.wellness.SleepActivity;

import java.util.HashMap;
import java.util.Map;

public class ResourcesActivity extends AppCompatActivity {

    private AutoCompleteTextView langDropdown;
    private Map<String, String> langCodeMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resources);

        initLanguageDropdown();
        setupClickListeners();
    }

    private void initLanguageDropdown() {
        langDropdown = findViewById(R.id.langDropdown);

        // 1. Define Display Names
        String[] languages = {"English", "Hindi", "Bengali", "Marathi", "Tamil", "Telugu"};

        // 2. Map Display Names to Language Codes (Useful for fetching regional data later)
        langCodeMap = new HashMap<>();
        langCodeMap.put("English", "en");
        langCodeMap.put("Hindi", "hi");
        langCodeMap.put("Bengali", "bn");
        langCodeMap.put("Marathi", "mr");
        langCodeMap.put("Tamil", "ta");
        langCodeMap.put("Telugu", "te");

        // 3. Set the Adapter
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, languages);
        langDropdown.setAdapter(adapter);

        // Optional: Set default selection
        langDropdown.setText(languages[0], false);
        // Shows the dropdown immediately on click
        langDropdown.setOnClickListener(v -> langDropdown.showDropDown());
    }

    private void setupClickListeners() {
        // Featured & Bento Grid
        findViewById(R.id.btnPlay).setOnClickListener(v -> navigateTo(BreathingActivity.class));
        findViewById(R.id.cardBreathing).setOnClickListener(v -> navigateTo(BreathingActivity.class));
        findViewById(R.id.cardExam).setOnClickListener(v -> navigateTo(ExamStressActivity.class));
        findViewById(R.id.cardSleep).setOnClickListener(v -> navigateTo(SleepActivity.class));
        findViewById(R.id.cardEvening).setOnClickListener(v -> navigateTo(BreathingActivity.class));
        findViewById(R.id.cardSelfCare).setOnClickListener(v -> navigateTo(SelfCareActivity.class));
        findViewById(R.id.cardMorning).setOnClickListener(v -> navigateTo(MorningActivity.class));
        // Explore More (Horizontal)
        findViewById(R.id.cardMusic).setOnClickListener(v -> navigateTo(MusicActivity.class));
        findViewById(R.id.cardFocus).setOnClickListener(v -> navigateTo(FocusActivity.class));
        findViewById(R.id.cardMorningEnergy).setOnClickListener(v -> navigateTo(MorningActivity.class));
        findViewById(R.id.cardAffirmations).setOnClickListener(v -> navigateTo(AffirmationsActivity.class));
        findViewById(R.id.cardSelfCareBoost).setOnClickListener(v -> navigateTo(NatureActivity.class));
        findViewById(R.id.cardSleepSounds).setOnClickListener(v -> navigateTo(OceanActivity.class));
    }

    private void navigateTo(Class<?> targetActivity) {
        String selectedName = langDropdown.getText().toString();
        // Get the code (e.g., "hi") from the name (e.g., "Hindi")
        String langCode = langCodeMap.getOrDefault(selectedName, "en");

        Intent intent = new Intent(ResourcesActivity.this, targetActivity);
        intent.putExtra("LANG_CODE", langCode); // Pass the code to the next Activity
        startActivity(intent);
    }
}