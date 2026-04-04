package com.vanaksh.manomitra.ui.selfhelp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.vanaksh.manomitra.R;

import java.util.HashMap;
import java.util.Map;

public class NatureSoundService extends Service {

    public static final String ACTION_PLAY = "ACTION_PLAY";
    public static final String ACTION_PAUSE = "ACTION_PAUSE";
    public static final String ACTION_SET_VOLUME = "ACTION_SET_VOLUME";
    public static final String ACTION_STOP_ALL = "ACTION_STOP_ALL";

    public static final String EXTRA_SOUND_ID = "sound_id";
    public static final String EXTRA_RAW_RES_ID = "raw_res_id";
    public static final String EXTRA_VOLUME = "volume"; // 0-100

    // Broadcasts to UI
    public static final String BROADCAST_SOUND_STATE = "BROADCAST_SOUND_STATE";
    public static final String EXTRA_IS_PLAYING = "is_playing";

    private static final String CHANNEL_ID = "NatureSoundChannel";
    private static final int NOTIFICATION_ID = 1002;

    private HashMap<Integer, MediaPlayer> mediaPlayers = new HashMap<>();
    private HashMap<Integer, Integer> playerVolumes = new HashMap<>(); // Store volumes
    private boolean isForeground = false;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            int soundId = intent.getIntExtra(EXTRA_SOUND_ID, -1);
            int rawResId = intent.getIntExtra(EXTRA_RAW_RES_ID, -1);
            int volume = intent.getIntExtra(EXTRA_VOLUME, 100);

            switch (intent.getAction()) {
                case ACTION_PLAY:
                    if (soundId != -1 && rawResId != -1) {
                        playSound(soundId, rawResId, volume);
                    }
                    break;
                case ACTION_PAUSE:
                    if (soundId != -1) {
                        pauseSound(soundId);
                    }
                    break;
                case ACTION_SET_VOLUME:
                    if (soundId != -1) {
                        setSoundVolume(soundId, volume);
                    }
                    break;
                case ACTION_STOP_ALL:
                    stopAllSounds();
                    break;
            }
        }
        return START_NOT_STICKY; // Background mixes shouldn't auto-restart if system kills it for memory
    }

    private void playSound(int soundId, int rawResId, int volumeProgress) {
        MediaPlayer player = mediaPlayers.get(soundId);
        if (player == null) {
            player = MediaPlayer.create(this, rawResId);
            if (player == null) return; // Failsafe if file missing
            
            player.setLooping(true);
            mediaPlayers.put(soundId, player);
            playerVolumes.put(soundId, volumeProgress);
            
            float mappedVolume = volumeProgress / 100f;
            player.setVolume(mappedVolume, mappedVolume);
        }
        
        player.start();
        
        if (!isForeground) {
            startForeground(NOTIFICATION_ID, buildNotification());
            isForeground = true;
        }

        broadcastState(soundId, true, playerVolumes.get(soundId));
    }

    private void pauseSound(int soundId) {
        MediaPlayer player = mediaPlayers.get(soundId);
        if (player != null && player.isPlaying()) {
            player.pause();
        }
        int vol = playerVolumes.containsKey(soundId) ? playerVolumes.get(soundId) : 100;
        broadcastState(soundId, false, vol);

        checkIfShouldStopForeground();
    }

    private void setSoundVolume(int soundId, int volumeProgress) {
        MediaPlayer player = mediaPlayers.get(soundId);
        if (player != null) {
            float mappedVolume = volumeProgress / 100f;
            player.setVolume(mappedVolume, mappedVolume);
            playerVolumes.put(soundId, volumeProgress);
        }
    }

    private void stopAllSounds() {
        for (Map.Entry<Integer, MediaPlayer> entry : mediaPlayers.entrySet()) {
            MediaPlayer p = entry.getValue();
            if (p != null) {
                if (p.isPlaying()) p.stop();
                p.release();
            }
        }
        mediaPlayers.clear();
        playerVolumes.clear();
        if (isForeground) {
            stopForeground(true);
            isForeground = false;
        }
        stopSelf();
    }

    private void checkIfShouldStopForeground() {
        boolean anyPlaying = false;
        for (MediaPlayer p : mediaPlayers.values()) {
            if (p != null && p.isPlaying()) {
                anyPlaying = true;
                break;
            }
        }
        if (!anyPlaying) {
            stopForeground(true);
            isForeground = false;
        }
    }

    private void broadcastState(int soundId, boolean isPlaying, int volume) {
        Intent intent = new Intent(BROADCAST_SOUND_STATE);
        intent.putExtra(EXTRA_SOUND_ID, soundId);
        intent.putExtra(EXTRA_IS_PLAYING, isPlaying);
        intent.putExtra(EXTRA_VOLUME, volume);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private Notification buildNotification() {
        Intent stopIntent = new Intent(this, NatureSoundService.class);
        stopIntent.setAction(ACTION_STOP_ALL);
        
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, flags);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("ManoMitra Soundscape Active")
                .setContentText("Touch to return to your audio mixer.")
                .setOngoing(true)
                .addAction(android.R.drawable.ic_media_pause, "Stop Mixing", stopPendingIntent)
                .setContentIntent(getPendingIntent())
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private PendingIntent getPendingIntent() {
        Intent intent = new Intent(this, NatureActivity.class);
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
                    "Nature Sound Mixer",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Controls background nature sounds");
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopAllSounds();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
