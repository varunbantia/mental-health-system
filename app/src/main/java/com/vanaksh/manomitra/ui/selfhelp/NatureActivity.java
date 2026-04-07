package com.vanaksh.manomitra.ui.selfhelp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.widget.ImageButton;

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

public class NatureActivity extends AppCompatActivity {

    private NatureSoundAdapter adapter;
    private List<SoundModel> soundList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_nature);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, systemBars.bottom);
            return insets;
        });

        setupRecyclerView();

        // Register receiver to get updates if the service is already running
        // independently
        LocalBroadcastManager.getInstance(this).registerReceiver(soundStateReceiver,
                new IntentFilter(NatureSoundService.BROADCAST_SOUND_STATE));
    }

    private void setupRecyclerView() {
        RecyclerView recycler = findViewById(R.id.recycler_nature_sounds);
        soundList = new ArrayList<>();

        // Populate exactly 10 track placeholders for the mixer
        soundList.add(new SoundModel(1, "Soft Rain", android.R.drawable.ic_media_play, R.raw.softrain_voice));
        soundList.add(
                new SoundModel(2, "Heavy Thunderstorm", android.R.drawable.ic_media_play, R.raw.heavythunder_voice));
        soundList.add(new SoundModel(3, "Ocean Waves", android.R.drawable.ic_media_play, R.raw.oceanwaves_voice));
        soundList.add(new SoundModel(4, "Deep Forest", android.R.drawable.ic_media_play, R.raw.deepforest_voice));
        soundList.add(
                new SoundModel(5, "Crackling Fireplace", android.R.drawable.ic_media_play, R.raw.cracklingfire_voice));
        soundList.add(
                new SoundModel(6, "Wind Through Leaves", android.R.drawable.ic_media_play, R.raw.windleaves_voice));
        soundList.add(new SoundModel(7, "Flowing River", android.R.drawable.ic_media_play, R.raw.flowingriver_voice));
        soundList.add(
                new SoundModel(8, "Crickets at Night", android.R.drawable.ic_media_play, R.raw.cricketnight_voice));
        soundList.add(new SoundModel(9, "White Noise", android.R.drawable.ic_media_play, R.raw.whitenoise_voice));
        soundList
                .add(new SoundModel(10, "Binaural Focus Focus", android.R.drawable.ic_media_play,
                        R.raw.binauralfocus_voice));

        adapter = new NatureSoundAdapter(soundList, new NatureSoundAdapter.OnSoundInteractionListener() {
            @Override
            public void onPlayPauseClicked(SoundModel sound) {
                Intent intent = new Intent(NatureActivity.this, NatureSoundService.class);
                intent.setAction(sound.isPlaying() ? NatureSoundService.ACTION_PLAY : NatureSoundService.ACTION_PAUSE);
                intent.putExtra(NatureSoundService.EXTRA_SOUND_ID, sound.getId());
                intent.putExtra(NatureSoundService.EXTRA_RAW_RES_ID, sound.getRawResId());
                intent.putExtra(NatureSoundService.EXTRA_VOLUME, sound.getVolume());
                startService(intent);
            }

            @Override
            public void onVolumeChanged(SoundModel sound, int volume) {
                Intent intent = new Intent(NatureActivity.this, NatureSoundService.class);
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
        // We do NOT stop the NatureSoundService here! It runs persistently in the
        // background like requested.
    }
}