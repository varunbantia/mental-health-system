package com.vanaksh.manomitra.wellness;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.vanaksh.manomitra.R;
import com.vanaksh.manomitra.ui.selfhelp.NatureActivity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class SleepActivity extends AppCompatActivity {

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sleep);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, systemBars.bottom);
            return insets;
        });

        prefs = getSharedPreferences("SleepBetterPrefs", MODE_PRIVATE);

        setupChecklist();
        setupCalculator();

        MaterialButton btnNature = findViewById(R.id.btn_open_nature);
        btnNature.setOnClickListener(v -> startActivity(new Intent(SleepActivity.this, NatureActivity.class)));
    }

    private void setupChecklist() {
        CheckBox cbCaffeine = findViewById(R.id.cb_caffeine);
        CheckBox cbScreens = findViewById(R.id.cb_screens);
        CheckBox cbLights = findViewById(R.id.cb_lights);
        CheckBox cbRead = findViewById(R.id.cb_read);

        cbCaffeine.setChecked(prefs.getBoolean("sleep_caffeine", false));
        cbScreens.setChecked(prefs.getBoolean("sleep_screens", false));
        cbLights.setChecked(prefs.getBoolean("sleep_lights", false));
        cbRead.setChecked(prefs.getBoolean("sleep_read", false));

        cbCaffeine.setOnCheckedChangeListener((bw, val) -> prefs.edit().putBoolean("sleep_caffeine", val).apply());
        cbScreens.setOnCheckedChangeListener((bw, val) -> prefs.edit().putBoolean("sleep_screens", val).apply());
        cbLights.setOnCheckedChangeListener((bw, val) -> prefs.edit().putBoolean("sleep_lights", val).apply());
        cbRead.setOnCheckedChangeListener((bw, val) -> prefs.edit().putBoolean("sleep_read", val).apply());
    }

    private void setupCalculator() {
        MaterialButton btnPickTime = findViewById(R.id.btn_pick_time);
        LinearLayout layoutResults = findViewById(R.id.layout_results);
        TextView tvCycle6 = findViewById(R.id.tv_cycle_6);
        TextView tvCycle5 = findViewById(R.id.tv_cycle_5);
        TextView tvCycle4 = findViewById(R.id.tv_cycle_4);

        btnPickTime.setOnClickListener(v -> {
            Calendar currentTime = Calendar.getInstance();
            int hour = currentTime.get(Calendar.HOUR_OF_DAY);
            int minute = currentTime.get(Calendar.MINUTE);

            TimePickerDialog dialog = new TimePickerDialog(this, (view, hourOfDay, minuteOfHour) -> {
                Calendar wakeCal = Calendar.getInstance();
                wakeCal.set(Calendar.HOUR_OF_DAY, hourOfDay);
                wakeCal.set(Calendar.MINUTE, minuteOfHour);
                
                SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                btnPickTime.setText("Waking up at " + sdf.format(wakeCal.getTime()));
                
                // Average human takes 15 mins to fall asleep. 
                // A full REM cycle is 90 mins.
                // Subtract cycles + 15 mins to find strictly when they should be in bed trying to sleep.
                
                // 6 Cycles (9 Hours) + 15 mins fall asleep time = subtract 9 hrs 15 mins
                Calendar cal6 = (Calendar) wakeCal.clone();
                cal6.add(Calendar.MINUTE, -(6 * 90 + 15));
                tvCycle6.setText("• " + sdf.format(cal6.getTime()) + " (6 Full Cycles - Recommended)");

                // 5 Cycles (7.5 Hours) + 15 mins
                Calendar cal5 = (Calendar) wakeCal.clone();
                cal5.add(Calendar.MINUTE, -(5 * 90 + 15));
                tvCycle5.setText("• " + sdf.format(cal5.getTime()) + " (5 Cycles)");

                // 4 Cycles (6 Hours) + 15 mins
                Calendar cal4 = (Calendar) wakeCal.clone();
                cal4.add(Calendar.MINUTE, -(4 * 90 + 15));
                tvCycle4.setText("• " + sdf.format(cal4.getTime()) + " (4 Cycles - Minimum)");

                layoutResults.setVisibility(View.VISIBLE);
                
            }, hour, minute, false);
            
            dialog.show();
        });
    }
}