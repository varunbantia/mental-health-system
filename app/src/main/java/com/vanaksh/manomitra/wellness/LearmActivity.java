package com.vanaksh.manomitra.wellness;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.vanaksh.manomitra.R;
import com.vanaksh.manomitra.ui.crisis.EmergencyActivity;

public class LearmActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_learn);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, systemBars.bottom);
            return insets;
        });

        setupInteractions();
    }

    private void setupInteractions() {
        // Anxiety Pro-Tip
        View tipAnxiety = findViewById(R.id.ll_pro_tip_anxiety);
        TextView btnAnxiety = findViewById(R.id.tv_btn_anxiety);
        btnAnxiety.setOnClickListener(v -> toggleTip(tipAnxiety, btnAnxiety));

        // Sleep Pro-Tip
        View tipSleep = findViewById(R.id.ll_pro_tip_sleep);
        TextView btnSleep = findViewById(R.id.tv_btn_sleep);
        btnSleep.setOnClickListener(v -> toggleTip(tipSleep, btnSleep));

        // Reframing Pro-Tip
        View tipReframe = findViewById(R.id.ll_pro_tip_reframe);
        TextView btnReframe = findViewById(R.id.tv_btn_reframe);
        btnReframe.setOnClickListener(v -> toggleTip(tipReframe, btnReframe));

        // Emergency Link
        findViewById(R.id.btn_emergency_link).setOnClickListener(v -> {
            startActivity(new Intent(this, EmergencyActivity.class));
        });
    }

    private void toggleTip(View tipView, TextView btn) {
        if (tipView.getVisibility() == View.VISIBLE) {
            tipView.setVisibility(View.GONE);
            btn.setText("See Pro-Tip");
        } else {
            tipView.setVisibility(View.VISIBLE);
            btn.setText("Hide Pro-Tip");
        }
    }
}