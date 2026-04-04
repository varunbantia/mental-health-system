package com.vanaksh.manomitra.ui.selfhelp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.vanaksh.manomitra.R;

import java.util.List;

public class NatureSoundAdapter extends RecyclerView.Adapter<NatureSoundAdapter.SoundViewHolder> {

    private List<SoundModel> soundList;
    private OnSoundInteractionListener listener;

    public interface OnSoundInteractionListener {
        void onPlayPauseClicked(SoundModel sound);
        void onVolumeChanged(SoundModel sound, int volume);
    }

    public NatureSoundAdapter(List<SoundModel> soundList, OnSoundInteractionListener listener) {
        this.soundList = soundList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SoundViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sound_card, parent, false);
        return new SoundViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SoundViewHolder holder, int position) {
        SoundModel sound = soundList.get(position);

        holder.tvSoundName.setText(sound.getName());
        holder.ivSoundIcon.setImageResource(sound.getIconResId());
        
        // Update Play/Pause UI
        int btnIcon = sound.isPlaying() ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play;
        holder.btnPlayPause.setImageResource(btnIcon);
        
        // Update Volume UI
        holder.seekbarVolume.setProgress(sound.getVolume());
        holder.seekbarVolume.setVisibility(sound.isPlaying() ? View.VISIBLE : View.GONE);

        // Click Listeners
        holder.btnPlayPause.setOnClickListener(v -> {
            sound.setPlaying(!sound.isPlaying());
            notifyItemChanged(position); // Re-bind to update icon and seekbar visibility
            if (listener != null) {
                listener.onPlayPauseClicked(sound);
            }
        });

        holder.seekbarVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    sound.setVolume(progress);
                    if (listener != null) {
                        listener.onVolumeChanged(sound, progress);
                    }
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    @Override
    public int getItemCount() {
        return soundList.size();
    }
    
    // Helper to update specific items when BroadcastReceiver receives state from Background Service
    public void updateSoundState(int soundId, boolean isPlaying, int volume) {
        for (int i = 0; i < soundList.size(); i++) {
            if (soundList.get(i).getId() == soundId) {
                soundList.get(i).setPlaying(isPlaying);
                soundList.get(i).setVolume(volume);
                notifyItemChanged(i);
                break;
            }
        }
    }

    static class SoundViewHolder extends RecyclerView.ViewHolder {
        ImageView ivSoundIcon;
        TextView tvSoundName;
        ImageButton btnPlayPause;
        SeekBar seekbarVolume;

        public SoundViewHolder(@NonNull View itemView) {
            super(itemView);
            ivSoundIcon = itemView.findViewById(R.id.iv_sound_icon);
            tvSoundName = itemView.findViewById(R.id.tv_sound_name);
            btnPlayPause = itemView.findViewById(R.id.btn_play_pause);
            seekbarVolume = itemView.findViewById(R.id.seekbar_volume);
        }
    }
}
