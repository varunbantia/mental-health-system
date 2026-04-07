package com.vanaksh.manomitra.resouurces;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
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
import com.vanaksh.manomitra.wellness.RelaxActivity;
import com.vanaksh.manomitra.wellness.SleepActivity;

import java.util.HashMap;
import java.util.Map;

public class ResourcesActivity extends AppCompatActivity {

    private AutoCompleteTextView langDropdown;
    private Map<String, String> langCodeMap;
    
    // UI Elements for translation
    private TextView tvHeader, tvLabel, tvTitle, tvDesc, tvForYouToday, tvExploreMore;
    private TextView tvBreathingTitle, tvExamTitle, tvSleepTitleCard, tvSleepDescCard;
    private TextView tvEveningTitle, tvSelfCareTitle, tvMorningTitle, tvMorningDesc;
    private TextView tvMusicTitle, tvFocusTitle, tvMorningEnergyTitle, tvAffirmationsTitle, tvNatureTitle;
    private com.google.android.material.button.MaterialButton btnPlay;

    private static class TranslationSet {
        String header, label, fTitle, fDesc, play, s1, breathing, exam, sleep, sleepSub, evening, selfCare, morning, morningSub, s2, music, focus, energy, affirmations, nature;

        TranslationSet(String... s) {
            header=s[0]; label=s[1]; fTitle=s[2]; fDesc=s[3]; play=s[4]; s1=s[5]; breathing=s[6]; exam=s[7]; 
            sleep=s[8]; sleepSub=s[9]; evening=s[10]; selfCare=s[11]; morning=s[12]; morningSub=s[13]; 
            s2=s[14]; music=s[15]; focus=s[16]; energy=s[17]; affirmations=s[18]; nature=s[19];
        }
    }

    private Map<String, TranslationSet> translations;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resources);

        initViews();
        initTranslations();
        initLanguageDropdown();
        setupClickListeners();
        
        // Default language
        applyTranslation("en");
    }

    private void initViews() {
        tvHeader = findViewById(R.id.tvHeader);
        tvLabel = findViewById(R.id.tvLabel);
        tvTitle = findViewById(R.id.tvTitle);
        tvDesc = findViewById(R.id.tvDesc);
        btnPlay = findViewById(R.id.btnPlay);
        tvForYouToday = findViewById(R.id.tvForYouToday);
        tvExploreMore = findViewById(R.id.tvExploreMore);
        
        tvBreathingTitle = findViewById(R.id.tvBreathingTitle);
        tvExamTitle = findViewById(R.id.tvExamTitle);
        tvSleepTitleCard = findViewById(R.id.tvSleepTitleCard);
        tvSleepDescCard = findViewById(R.id.tvSleepDescCard);
        tvEveningTitle = findViewById(R.id.tvEveningTitle);
        tvSelfCareTitle = findViewById(R.id.tvSelfCareTitle);
        tvMorningTitle = findViewById(R.id.tvMorningTitle);
        tvMorningDesc = findViewById(R.id.tvMorningDesc);
        
        tvMusicTitle = findViewById(R.id.tvMusicTitle);
        tvFocusTitle = findViewById(R.id.tvFocusTitle);
        tvMorningEnergyTitle = findViewById(R.id.tvMorningEnergyTitle);
        tvAffirmationsTitle = findViewById(R.id.tvAffirmationsTitle);
        tvNatureTitle = findViewById(R.id.tvNatureTitle);
    }

    private void initTranslations() {
        translations = new HashMap<>();
        
        translations.put("en", new TranslationSet(
            "Wellness Resources", "RESOURCE OF THE DAY", "5-Minute Calm\nBreathing", "Guided relaxation for stress relief", "Play Now",
            "For You Today", "Deep Breathing\nExercise", "Managing Exam\nStress", "Sleep Better\nTonight", "Simple night routine",
            "Evening Wind\nDown", "Self-Care Tips", "Mindful Morning Routine", "Start your day with intention",
            "Explore More Resources", "Relaxing\nMusic", "Focus &\nStudy", "Morning\nEnergy", "Positive\nAffirmations", "Nature\nSounds"
        ));

        translations.put("hi", new TranslationSet(
            "स्वास्थ्य संसाधन", "आज का संसाधन", "5-मिनट शांत\nश्वास", "तनाव राहत के लिए निर्देशित विश्राम", "अभी शुरू करें",
            "आज आपके लिए", "गहरी साँस लेने का\nव्यायाम", "परीक्षा तनाव का\nप्रबंधन", "आज रात बेहतर\nसोएं", "सरल रात्रि दिनचर्या",
            "शाम का\nआराम", "स्व-देखभाल टिप्स", "माइंडफुल मॉर्निंग रूटीन", "इरादे के साथ दिन शुरू करें",
            "और संसाधन खोजें", "आरामदायक\nसंगीत", "फोकस और\nअध्ययन", "सुबह की\nऊर्जा", "सकारात्मक\nपुष्टि", "प्रकृति की\nआवाज़ें"
        ));
        
        // Stubs for others - using English for now, can be filled later
        translations.put("bn", translations.get("en"));
        translations.put("mr", translations.get("hi")); // Using Hindi as proxy for Marathi for now
        translations.put("ta", translations.get("en"));
        translations.put("te", translations.get("en"));
    }

    private void applyTranslation(String langCode) {
        TranslationSet set = translations.get(langCode);
        if (set == null) return;

        tvHeader.setText(set.header);
        tvLabel.setText(set.label);
        tvTitle.setText(set.fTitle);
        tvDesc.setText(set.fDesc);
        btnPlay.setText(set.play);
        tvForYouToday.setText(set.s1);
        
        if (tvBreathingTitle != null) tvBreathingTitle.setText(set.breathing);
        if (tvExamTitle != null) tvExamTitle.setText(set.exam);
        if (tvSleepTitleCard != null) tvSleepTitleCard.setText(set.sleep);
        if (tvSleepDescCard != null) tvSleepDescCard.setText(set.sleepSub);
        if (tvEveningTitle != null) tvEveningTitle.setText(set.evening);
        if (tvSelfCareTitle != null) tvSelfCareTitle.setText(set.selfCare);
        if (tvMorningTitle != null) tvMorningTitle.setText(set.morning);
        if (tvMorningDesc != null) tvMorningDesc.setText(set.morningSub);
        
        tvExploreMore.setText(set.s2);
        
        if (tvMusicTitle != null) tvMusicTitle.setText(set.music);
        if (tvFocusTitle != null) tvFocusTitle.setText(set.focus);
        if (tvMorningEnergyTitle != null) tvMorningEnergyTitle.setText(set.energy);
        if (tvAffirmationsTitle != null) tvAffirmationsTitle.setText(set.affirmations);
        if (tvNatureTitle != null) tvNatureTitle.setText(set.nature);
    }

    private void initLanguageDropdown() {
        langDropdown = findViewById(R.id.langDropdown);
        String[] languages = {"English", "Hindi", "Bengali", "Marathi", "Tamil", "Telugu"};

        langCodeMap = new HashMap<>();
        langCodeMap.put("English", "en");
        langCodeMap.put("Hindi", "hi");
        langCodeMap.put("Bengali", "bn");
        langCodeMap.put("Marathi", "mr");
        langCodeMap.put("Tamil", "ta");
        langCodeMap.put("Telugu", "te");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, languages);
        langDropdown.setAdapter(adapter);

        langDropdown.setOnItemClickListener((parent, view, position, id) -> {
            String selectedName = (String) parent.getItemAtPosition(position);
            String langCode = langCodeMap.getOrDefault(selectedName, "en");
            applyTranslation(langCode);
        });

        langDropdown.setOnClickListener(v -> langDropdown.showDropDown());
    }

    private void setupClickListeners() {
        findViewById(R.id.btnPlay).setOnClickListener(v -> navigateTo(BreathingActivity.class));
        findViewById(R.id.cardBreathing).setOnClickListener(v -> navigateTo(BreathingActivity.class));
        findViewById(R.id.cardExam).setOnClickListener(v -> navigateTo(ExamStressActivity.class));
        findViewById(R.id.cardSleep).setOnClickListener(v -> navigateTo(SleepActivity.class));
        findViewById(R.id.cardEvening).setOnClickListener(v -> navigateTo(RelaxActivity.class));
        findViewById(R.id.cardSelfCare).setOnClickListener(v -> navigateTo(SelfCareActivity.class));
        findViewById(R.id.cardMorning).setOnClickListener(v -> navigateTo(MorningActivity.class));
        
        findViewById(R.id.cardMusic).setOnClickListener(v -> navigateTo(MusicActivity.class));
        findViewById(R.id.cardFocus).setOnClickListener(v -> navigateTo(FocusActivity.class));
        findViewById(R.id.cardMorningEnergy).setOnClickListener(v -> navigateTo(MorningActivity.class));
        findViewById(R.id.cardAffirmations).setOnClickListener(v -> navigateTo(AffirmationsActivity.class));
        findViewById(R.id.cardSelfCareBoost).setOnClickListener(v -> navigateTo(NatureActivity.class));
    }

    private void navigateTo(Class<?> targetActivity) {
        String selectedName = langDropdown.getText().toString();
        String langCode = langCodeMap.getOrDefault(selectedName, "en");

        Intent intent = new Intent(ResourcesActivity.this, targetActivity);
        intent.putExtra("LANG_CODE", langCode);
        startActivity(intent);
    }
}