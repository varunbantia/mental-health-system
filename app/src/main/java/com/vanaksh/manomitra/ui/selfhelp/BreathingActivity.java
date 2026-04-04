package com.vanaksh.manomitra.ui.selfhelp;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.vanaksh.manomitra.R;

public class BreathingActivity extends AppCompatActivity {

    private View breathingSphere;
    private TextView tvBreathingState;
    private MaterialButton btnToggle;

    private AnimatorSet currentAnimationSet;
    private boolean isBreathing = false;
    private int currentPhase = 0; // 0=Inhale, 1=Hold, 2=Exhale, 3=Hold

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_breathing);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, systemBars.bottom);
            return insets;
        });

        // Initialize UI
        breathingSphere = findViewById(R.id.view_breathing_sphere);
        tvBreathingState = findViewById(R.id.tv_breathing_state);
        btnToggle = findViewById(R.id.btn_toggle_breathing);

        btnToggle.setOnClickListener(v -> toggleBreathing());
    }

    private void toggleBreathing() {
        if (isBreathing) {
            stopBreathing();
        } else {
            startBreathing();
        }
    }

    private void startBreathing() {
        isBreathing = true;
        btnToggle.setText("Stop Exercise");
        currentPhase = 0;
        runBreathingCycle();
    }

    private void stopBreathing() {
        isBreathing = false;
        btnToggle.setText("Begin Exercise");
        tvBreathingState.setText("Ready");

        if (currentAnimationSet != null) {
            currentAnimationSet.cancel();
        }

        // Reset sphere to normal size smoothly
        breathingSphere.animate()
            .scaleX(1.0f)
            .scaleY(1.0f)
            .setDuration(500)
            .start();
    }

    private void runBreathingCycle() {
        if (!isBreathing) return;

        ObjectAnimator scaleX;
        ObjectAnimator scaleY;
        long duration = 4000;

        switch (currentPhase) {
            case 0: // Inhale
                tvBreathingState.setText("Inhale...");
                scaleX = ObjectAnimator.ofFloat(breathingSphere, "scaleX", breathingSphere.getScaleX(), 2.2f);
                scaleY = ObjectAnimator.ofFloat(breathingSphere, "scaleY", breathingSphere.getScaleY(), 2.2f);
                break;
            case 1: // Hold
                tvBreathingState.setText("Hold...");
                scaleX = ObjectAnimator.ofFloat(breathingSphere, "scaleX", 2.2f, 2.2f);
                scaleY = ObjectAnimator.ofFloat(breathingSphere, "scaleY", 2.2f, 2.2f);
                break;
            case 2: // Exhale
                tvBreathingState.setText("Exhale...");
                scaleX = ObjectAnimator.ofFloat(breathingSphere, "scaleX", 2.2f, 1.0f);
                scaleY = ObjectAnimator.ofFloat(breathingSphere, "scaleY", 2.2f, 1.0f);
                break;
            case 3: // Hold Empty
            default:
                tvBreathingState.setText("Hold...");
                scaleX = ObjectAnimator.ofFloat(breathingSphere, "scaleX", 1.0f, 1.0f);
                scaleY = ObjectAnimator.ofFloat(breathingSphere, "scaleY", 1.0f, 1.0f);
                break;
        }

        currentAnimationSet = new AnimatorSet();
        currentAnimationSet.playTogether(scaleX, scaleY);
        currentAnimationSet.setDuration(duration);
        
        currentAnimationSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (isBreathing) {
                    currentPhase = (currentPhase + 1) % 4; // Loop through the 4 phases
                    runBreathingCycle();
                }
            }
        });

        currentAnimationSet.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (currentAnimationSet != null) {
            currentAnimationSet.cancel();
        }
    }
}