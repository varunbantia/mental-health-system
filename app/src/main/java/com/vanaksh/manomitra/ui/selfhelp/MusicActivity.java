package com.vanaksh.manomitra.ui.selfhelp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.RecyclerView;

import com.vanaksh.manomitra.R;

import java.util.ArrayList;
import java.util.List;

public class MusicActivity extends AppCompatActivity {

    private NatureSoundAdapter adapter;
    private List<SoundModel> songList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_music);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, systemBars.bottom);
            return insets;
        });

        setupSongList();

        // Register receiver for background player updates
        LocalBroadcastManager.getInstance(this).registerReceiver(soundStateReceiver,
                new IntentFilter(NatureSoundService.BROADCAST_SOUND_STATE));
    }

    private void setupSongList() {
        RecyclerView recycler = findViewById(R.id.recycler_relax_songs);
        songList = new ArrayList<>();

        // 5 Specific Relaxation Songs
        songList.add(new SoundModel(201, "Zen Garden", android.R.drawable.ic_media_play, R.raw.relax1_voice));
        songList.add(new SoundModel(202, "Deep Space", android.R.drawable.ic_media_play, R.raw.relax2_voice));
        songList.add(new SoundModel(203, "Mountain Air", android.R.drawable.ic_media_play, R.raw.relax3_voice));
        songList.add(new SoundModel(204, "Midnight Rain", android.R.drawable.ic_media_play, R.raw.relax4_voice));
        songList.add(new SoundModel(205, "Healing Frequencies", android.R.drawable.ic_media_play, R.raw.relax5_voice));

        adapter = new NatureSoundAdapter(songList, new NatureSoundAdapter.OnSoundInteractionListener() {
            @Override
            public void onPlayPauseClicked(SoundModel sound) {
                Intent intent = new Intent(MusicActivity.this, NatureSoundService.class);

                if (sound.isPlaying()) {
                    // Standard player behavior: Stop all other sounds before playing the new one
                    Intent stopIntent = new Intent(MusicActivity.this, NatureSoundService.class);
                    stopIntent.setAction(NatureSoundService.ACTION_STOP_ALL);
                    startService(stopIntent);

                    // Delay slightly to allow stop to process, then play
                    new Handler().postDelayed(() -> {
                        intent.setAction(NatureSoundService.ACTION_PLAY);
                        intent.putExtra(NatureSoundService.EXTRA_SOUND_ID, sound.getId());
                        intent.putExtra(NatureSoundService.EXTRA_RAW_RES_ID, sound.getRawResId());
                        intent.putExtra(NatureSoundService.EXTRA_VOLUME, sound.getVolume());
                        startService(intent);
                    }, 50);
                } else {
                    intent.setAction(NatureSoundService.ACTION_PAUSE);
                    intent.putExtra(NatureSoundService.EXTRA_SOUND_ID, sound.getId());
                    startService(intent);
                }
            }

            @Override
            public void onVolumeChanged(SoundModel sound, int volume) {
                Intent intent = new Intent(MusicActivity.this, NatureSoundService.class);
                intent.setAction(NatureSoundService.ACTION_SET_VOLUME);
                intent.putExtra(NatureSoundService.EXTRA_SOUND_ID, sound.getId());
                intent.putExtra(NatureSoundService.EXTRA_VOLUME, volume);
                startService(intent);
            }
        });

        recycler.setAdapter(adapter);
    }

    private BroadcastReceiver soundStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (NatureSoundService.BROADCAST_SOUND_STATE.equals(intent.getAction())) {
                int soundId = intent.getIntExtra(NatureSoundService.EXTRA_SOUND_ID, -1);
                boolean isPlaying = intent.getBooleanExtra(NatureSoundService.EXTRA_IS_PLAYING, false);
                int volume = intent.getIntExtra(NatureSoundService.EXTRA_VOLUME, 100);

                if (soundId != -1 && adapter != null) {
                    adapter.updateSoundState(soundId, isPlaying, volume);
                }
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(soundStateReceiver);
    }
}