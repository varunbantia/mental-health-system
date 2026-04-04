package com.vanaksh.manomitra.ui.selfhelp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
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
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

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

    private BroadcastReceiver timerUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (FocusTimerService.ACTION_TIMER_TICK.equals(intent.getAction())) {
                mTimeLeftInMillis = intent.getLongExtra(FocusTimerService.EXTRA_TIME_LEFT, mStartTimeInMillis);
                mStartTimeInMillis = intent.getLongExtra(FocusTimerService.EXTRA_TOTAL_TIME, mStartTimeInMillis);
                mTimerRunning = intent.getBooleanExtra("is_running", false);
                updateInterface();

                if (mTimerRunning) {
                    tvHint.setVisibility(View.INVISIBLE);
                    btnStartPause.setText("Pause");
                    btnStartPause.setIconResource(R.drawable.ic_pause);
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                } else {
                    btnStartPause.setText("Resume");
                    btnStartPause.setIconResource(R.drawable.ic_play);
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }
            } else if (FocusTimerService.ACTION_TIMER_FINISH.equals(intent.getAction())) {
                mTimerRunning = false;
                mTimeLeftInMillis = 0;
                btnStartPause.setText("Start");
                btnStartPause.setIconResource(R.drawable.ic_play);
                tvHint.setVisibility(View.VISIBLE);
                progressBar.setProgress(0);
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                updateInterface();
            }
        }
    };

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

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction(FocusTimerService.ACTION_TIMER_TICK);
        filter.addAction(FocusTimerService.ACTION_TIMER_FINISH);
        LocalBroadcastManager.getInstance(this).registerReceiver(timerUpdateReceiver, filter);

        // Tell service we are visible
        Intent foregroundIntent = new Intent(this, FocusTimerService.class);
        foregroundIntent.setAction(FocusTimerService.ACTION_APP_FOREGROUNDED);
        startService(foregroundIntent);

        // Request latest status from service so UI syncs instantly
        Intent statusIntent = new Intent(this, FocusTimerService.class);
        statusIntent.setAction(FocusTimerService.ACTION_REQUEST_STATUS);
        startService(statusIntent);
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(timerUpdateReceiver);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Tell service we are backgrounded (so it can show the notification)
        Intent backgroundIntent = new Intent(this, FocusTimerService.class);
        backgroundIntent.setAction(FocusTimerService.ACTION_APP_BACKGROUNDED);
        startService(backgroundIntent);
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
        Intent serviceIntent = new Intent(this, FocusTimerService.class);
        serviceIntent.setAction(FocusTimerService.ACTION_START);
        serviceIntent.putExtra(FocusTimerService.EXTRA_TOTAL_TIME, mStartTimeInMillis);
        serviceIntent.putExtra(FocusTimerService.EXTRA_TIME_LEFT, mTimeLeftInMillis);

        // We use startService here instead of startForegroundService to prevent
        // immediate Android 8+ crashes, since we only want foreground notification in the background.
        startService(serviceIntent);

        mTimerRunning = true;
        tvHint.setVisibility(View.INVISIBLE);
        btnStartPause.setText("Pause");
        btnStartPause.setIconResource(R.drawable.ic_pause);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void pauseTimer() {
        Intent serviceIntent = new Intent(this, FocusTimerService.class);
        serviceIntent.setAction(FocusTimerService.ACTION_PAUSE);
        startService(serviceIntent);
        
        mTimerRunning = false;
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        btnStartPause.setText("Resume");
        btnStartPause.setIconResource(R.drawable.ic_play);
    }

    private void stopTimer() {
        Intent serviceIntent = new Intent(this, FocusTimerService.class);
        serviceIntent.setAction(FocusTimerService.ACTION_STOP);
        startService(serviceIntent);

        mTimerRunning = false;
        mTimeLeftInMillis = mStartTimeInMillis;
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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
        // FAILSAFE: Prevent Divide By Zero Exception
        if (mStartTimeInMillis <= 0) {
            mStartTimeInMillis = 1;
        }

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
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
}