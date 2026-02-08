package com.vanaksh.manomitra.ui.selfhelp;

import android.graphics.Color;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.vanaksh.manomitra.R;

import java.util.Locale;

public class FocusActivity extends AppCompatActivity {

    // Default time selection (1 hour)
    private static final long DEFAULT_TIME_MILLIS = 3600000;
    private long mStartTimeInMillis = DEFAULT_TIME_MILLIS;
    private long mTimeLeftInMillis = mStartTimeInMillis;
    private boolean mTimerRunning;

    private TextView tvTimer, tvHint;
    private ExtendedFloatingActionButton btnStartPause;
    private FloatingActionButton btnStop, btnRestart;
    private ProgressBar progressBar;
    private CountDownTimer countDownTimer;

    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_focus);

        // Initialize Views
        tvTimer = findViewById(R.id.tv_timer_display);
        tvHint = findViewById(R.id.tv_hint);
        btnStartPause = findViewById(R.id.btn_start_pause);
        btnStop = findViewById(R.id.btn_stop);
        btnRestart = findViewById(R.id.btn_restart);
        progressBar = findViewById(R.id.focusProgressBar);

        progressBar.setMax(1000);

        tvTimer.setOnClickListener(v -> {
            if (!mTimerRunning) showTimePicker();
            else Toast.makeText(this, "Stop timer to change time", Toast.LENGTH_SHORT).show();
        });

        btnStartPause.setOnClickListener(v -> {
            if (mTimerRunning) pauseTimer();
            else startTimer();
        });

        btnStop.setOnClickListener(v -> stopTimer());
        btnRestart.setOnClickListener(v -> restartTimer());

        updateInterface();
    }

    private void showTimePicker() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(50, 40, 50, 40);

        final NumberPicker hp = createPicker(0, 12, (int) (mStartTimeInMillis / 3600000));
        final NumberPicker mp = createPicker(0, 59, (int) ((mStartTimeInMillis % 3600000) / 60000));
        final NumberPicker sp = createPicker(0, 59, (int) ((mStartTimeInMillis % 60000) / 1000));

        layout.addView(hp);
        layout.addView(createLabel("h"));
        layout.addView(mp);
        layout.addView(createLabel("m"));
        layout.addView(sp);
        layout.addView(createLabel("s"));

        new AlertDialog.Builder(this)
                .setTitle("Set Focus Time")
                .setView(layout)
                .setPositiveButton("Set", (d, w) -> {
                    long totalMillis = (hp.getValue() * 3600000L) +
                            (mp.getValue() * 60000L) +
                            (sp.getValue() * 1000L);

                    if (totalMillis > 0) {
                        mStartTimeInMillis = totalMillis;
                        mTimeLeftInMillis = mStartTimeInMillis;
                        updateInterface();
                    } else {
                        Toast.makeText(this, "Time cannot be zero", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Reset to Default", (d, w) -> {
                    mStartTimeInMillis = DEFAULT_TIME_MILLIS;
                    mTimeLeftInMillis = mStartTimeInMillis;
                    updateInterface();
                })
                .show();
    }

    private NumberPicker createPicker(int min, int max, int current) {
        NumberPicker picker = new NumberPicker(this);
        picker.setMinValue(min);
        picker.setMaxValue(max);
        picker.setValue(current);
        return picker;
    }

    private TextView createLabel(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setPadding(8, 0, 15, 0);
        tv.setTextColor(Color.BLACK);
        tv.setTypeface(null, Typeface.BOLD);
        return tv;
    }

    private void startTimer() {
        // PREVENT SLEEP: Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        countDownTimer = new CountDownTimer(mTimeLeftInMillis, 100) {
            @Override
            public void onTick(long l) {
                mTimeLeftInMillis = l;
                updateInterface();
            }

            @Override
            public void onFinish() {
                mTimerRunning = false;
                // ALLOW SLEEP: Timer finished
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                btnStartPause.setText("Start");
                btnStartPause.setIconResource(R.drawable.ic_play);
                tvHint.setVisibility(View.VISIBLE);
                progressBar.setProgress(0);
                playCompletionSound();
            }
        }.start();

        mTimerRunning = true;
        tvHint.setVisibility(View.INVISIBLE);
        btnStartPause.setText("Pause");
        btnStartPause.setIconResource(R.drawable.ic_pause);
    }

    private void playCompletionSound() {
        try {
            // Ensure success_voice.mp3 is in res/raw/
            mediaPlayer = MediaPlayer.create(this, R.raw.success_voice);
            if (mediaPlayer != null) {
                mediaPlayer.start();
                mediaPlayer.setOnCompletionListener(MediaPlayer::release);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void pauseTimer() {
        if (countDownTimer != null) countDownTimer.cancel();
        mTimerRunning = false;

        // ALLOW SLEEP: App is paused
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        btnStartPause.setText("Resume");
        btnStartPause.setIconResource(R.drawable.ic_play);
    }

    private void stopTimer() {
        if (countDownTimer != null) countDownTimer.cancel();
        if (mediaPlayer != null && mediaPlayer.isPlaying()) mediaPlayer.stop();

        // ALLOW SLEEP: Timer stopped
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mTimerRunning = false;
        mTimeLeftInMillis = mStartTimeInMillis;
        tvHint.setVisibility(View.VISIBLE);
        updateInterface();
        btnStartPause.setText("Start");
        btnStartPause.setIconResource(R.drawable.ic_play);
    }

    private void restartTimer() {
        stopTimer();
        startTimer();
    }

    private void updateInterface() {
        long secondsLeft = mTimeLeftInMillis / 1000;
        int hours = (int) (secondsLeft / 3600);
        int minutes = (int) ((secondsLeft % 3600) / 60);
        int seconds = (int) (secondsLeft % 60);

        String timeLeftFormatted = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
        tvTimer.setText(timeLeftFormatted);

        int progress = (int) ((mTimeLeftInMillis * 1000) / mStartTimeInMillis);
        progressBar.setProgress(progress);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        // Cleanup flags just in case
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
}