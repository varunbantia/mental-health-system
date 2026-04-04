package com.vanaksh.manomitra.ui.selfhelp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.vanaksh.manomitra.R;

import java.util.Locale;

public class FocusTimerService extends Service {

    public static final String ACTION_START = "ACTION_START";
    public static final String ACTION_PAUSE = "ACTION_PAUSE";
    public static final String ACTION_STOP = "ACTION_STOP";
    public static final String ACTION_RESTART = "ACTION_RESTART";

    public static final String ACTION_TIMER_TICK = "ACTION_TIMER_TICK";
    public static final String ACTION_TIMER_FINISH = "ACTION_TIMER_FINISH";

    public static final String EXTRA_TIME_LEFT = "time_left";
    public static final String EXTRA_TOTAL_TIME = "total_time";
    
    // For when asking Service status directly
    public static final String ACTION_REQUEST_STATUS = "ACTION_REQUEST_STATUS";

    // For Notification toggling
    public static final String ACTION_APP_BACKGROUNDED = "ACTION_APP_BACKGROUNDED";
    public static final String ACTION_APP_FOREGROUNDED = "ACTION_APP_FOREGROUNDED";

    private static final String CHANNEL_ID = "FocusTimerChannel";
    private static final int NOTIFICATION_ID = 1001;

    private CountDownTimer countDownTimer;
    // Defaulting to 1 hour to prevent Divide by Zero if requested prematurely
    private long mStartTimeInMillis = 3600000;
    private long mTimeLeftInMillis = 3600000;
    private boolean mTimerRunning = false;
    private boolean isAppInForeground = true;
    
    private MediaPlayer mediaPlayer;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            String action = intent.getAction();
            switch (action) {
                case ACTION_START:
                    mStartTimeInMillis = intent.getLongExtra(EXTRA_TOTAL_TIME, 0);
                    mTimeLeftInMillis = intent.getLongExtra(EXTRA_TIME_LEFT, mStartTimeInMillis);
                    startTimer();
                    break;
                case ACTION_PAUSE:
                    pauseTimer();
                    break;
                case ACTION_STOP:
                    stopTimer();
                    break;
                case ACTION_RESTART:
                    stopTimer();
                    startTimer();
                    break;
                case ACTION_REQUEST_STATUS:
                    broadcastUpdate(ACTION_TIMER_TICK);
                    break;
                case ACTION_APP_BACKGROUNDED:
                    isAppInForeground = false;
                    if (mTimerRunning) {
                        startForeground(NOTIFICATION_ID, buildNotification("Focus Timer Active", mTimeLeftInMillis));
                    }
                    break;
                case ACTION_APP_FOREGROUNDED:
                    isAppInForeground = true;
                    if (mTimerRunning) {
                        stopForeground(true);
                    }
                    break;
            }
        }
        return START_NOT_STICKY;
    }

    private void startTimer() {
        if (mTimeLeftInMillis <= 0) return;

        if (!isAppInForeground) {
            startForeground(NOTIFICATION_ID, buildNotification("Focus Timer Active", mTimeLeftInMillis));
        }
        
        countDownTimer = new CountDownTimer(mTimeLeftInMillis, 100) {
            private long lastNotificationUpdateTime = 0;

            @Override
            public void onTick(long millisUntilFinished) {
                mTimeLeftInMillis = millisUntilFinished;
                mTimerRunning = true;
                
                broadcastUpdate(ACTION_TIMER_TICK);

                // Update notification once a second if backgrounded
                if (!isAppInForeground && (System.currentTimeMillis() - lastNotificationUpdateTime >= 1000)) {
                    updateNotification(mTimeLeftInMillis);
                    lastNotificationUpdateTime = System.currentTimeMillis();
                }
            }

            @Override
            public void onFinish() {
                mTimerRunning = false;
                mTimeLeftInMillis = 0;
                broadcastUpdate(ACTION_TIMER_FINISH);
                playCompletionSound();
                stopForeground(false);
                updateNotificationFinished();
            }
        }.start();
    }

    private void pauseTimer() {
        if (countDownTimer != null) countDownTimer.cancel();
        mTimerRunning = false;
        
        // Update notification to indicate paused if backgrounded
        if (!isAppInForeground) {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_pause)
                    .setContentTitle("Focus Timer Paused")
                    .setContentText(formatTime(mTimeLeftInMillis) + " remaining")
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setContentIntent(getPendingIntent());
                    
            getSystemService(NotificationManager.class).notify(NOTIFICATION_ID, builder.build());
        } else {
            stopForeground(true); // Dismiss entirely if open
        }
        
        broadcastUpdate(ACTION_TIMER_TICK);
    }

    private void stopTimer() {
        if (countDownTimer != null) countDownTimer.cancel();
        mTimerRunning = false;
        mTimeLeftInMillis = mStartTimeInMillis;
        broadcastUpdate(ACTION_TIMER_TICK);
        stopForeground(true);
        stopSelf();
    }

    private void broadcastUpdate(String action) {
        Intent intent = new Intent(action);
        intent.putExtra(EXTRA_TIME_LEFT, mTimeLeftInMillis);
        intent.putExtra(EXTRA_TOTAL_TIME, mStartTimeInMillis);
        intent.putExtra("is_running", mTimerRunning);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private Notification buildNotification(String title, long millis) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(formatTime(millis) + " remaining")
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(getPendingIntent())
                .build();
    }

    private void updateNotification(long millis) {
        Notification notification = buildNotification("Focus Timer Active", millis);
        getSystemService(NotificationManager.class).notify(NOTIFICATION_ID, notification);
    }

    private void updateNotificationFinished() {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Focus Session Complete!")
                .setContentText("Great job staying focused.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(getPendingIntent())
                .build();
        getSystemService(NotificationManager.class).notify(NOTIFICATION_ID, notification);
    }

    private PendingIntent getPendingIntent() {
        Intent intent = new Intent(this, FocusActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getActivity(this, 0, intent, flags);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Focus Timer Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Shows active focus timer countdown");
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    private void playCompletionSound() {
        try {
            mediaPlayer = MediaPlayer.create(this, R.raw.success_voice);
            if (mediaPlayer != null) {
                mediaPlayer.start();
                mediaPlayer.setOnCompletionListener(MediaPlayer::release);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String formatTime(long millis) {
        int seconds = (int) (millis / 1000) % 60;
        int minutes = (int) ((millis / (1000 * 60)) % 60);
        int hours = (int) ((millis / (1000 * 60 * 60)) % 24);
        
        if (hours > 0) {
            return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) countDownTimer.cancel();
        if (mediaPlayer != null) mediaPlayer.release();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // Using startService and LocalBroadcastManager instead of binding
    }
}
