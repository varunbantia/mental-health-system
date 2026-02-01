package com.vanaksh.manomitra;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;


import com.vanaksh.manomitra.R;
import com.vanaksh.manomitra.databinding.ActivityMainBinding;
import com.vanaksh.manomitra.ui.dashboard.DashboardActivity;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Match the modern Edge-to-Edge look of your other activities
        EdgeToEdge.enable(this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Handle system bars padding for the modern UI
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setupLogic();
    }

    private void setupLogic() {
        // 1. Checkbox Logic: Button only becomes clickable when checkbox is checked
        binding.cbAgree.setOnCheckedChangeListener((buttonView, isChecked) -> {
            binding.btnContinue.setEnabled(isChecked);

            // Optional: Change alpha to give visual feedback
            if (isChecked) {
                binding.btnContinue.setAlpha(1.0f);
            } else {
                binding.btnContinue.setAlpha(0.6f);
            }
        });

        // 2. Continue Button Logic
        binding.btnContinue.setOnClickListener(v -> {
            if (binding.cbAgree.isChecked()) {
                markDisclaimerAsAccepted();
                navigateToMain();
            } else {
                Toast.makeText(this, "Please agree to the terms to continue", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void markDisclaimerAsAccepted() {
        // Save to SharedPreferences so the MainActivity knows we are clear
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("disclaimer_accepted", true);
        editor.apply();
    }

    private void navigateToMain() {
        Intent intent = new Intent(MainActivity.this, DashboardActivity.class);
        // Clear activity stack so user can't "Go Back" to the disclaimer
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}