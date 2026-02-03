package com.vanaksh.manomitra.ui.chatbot;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.media.MediaPlayer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.vanaksh.manomitra.R;
import com.vanaksh.manomitra.data.model.ChatMessage;


import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<ChatMessage> chatMessages;
    private final OnPlayButtonClickListener playButtonClickListener;

    // --- 1. Define View Types (including LOADING) ---
    // These must match the TYPE constants in your ChatMessage.java
    private static final int VIEW_TYPE_USER_TEXT = 0;
    private static final int VIEW_TYPE_BOT_TEXT = 1;
    private static final int VIEW_TYPE_USER_VOICE = 2;
    private static final int VIEW_TYPE_LOADING = 3; // The loading indicator

    public interface OnPlayButtonClickListener {
        void onPlayButtonClick(String filePath, ImageButton playButton, SeekBar seekBar, TextView duration);
        void onSpeakerIconClick(String textToSpeak);
    }

    public ChatAdapter(List<ChatMessage> chatMessages, Context context) {
        this.chatMessages = chatMessages;
        // This cast requires your ChatbotActivity to implement OnPlayButtonClickListener
        this.playButtonClickListener = (OnPlayButtonClickListener) context;
    }

    // Helper method for copying text
    private void copyTextToClipboard(String text, Context context) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            ClipData clip = ClipData.newPlainText("Copied Text", text);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show();
        }
    }

    // --- 2. Override getItemViewType (with LOADING) ---
    @Override
    public int getItemViewType(int position) {
        // Use the type defined in the ChatMessage object
        int type = chatMessages.get(position).getType();
        switch (type) {
            case ChatMessage.TYPE_USER_TEXT:
                return VIEW_TYPE_USER_TEXT;
            case ChatMessage.TYPE_BOT:
                return VIEW_TYPE_BOT_TEXT;
            case ChatMessage.TYPE_USER_VOICE:
                return VIEW_TYPE_USER_VOICE;
            case ChatMessage.TYPE_LOADING:
                return VIEW_TYPE_LOADING; // Return our new type
            default:
                return VIEW_TYPE_BOT_TEXT; // Fallback
        }
    }

    // --- 3. Update onCreateViewHolder (with LOADING) ---
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case VIEW_TYPE_USER_TEXT:
                return new UserTextViewHolder(inflater.inflate(R.layout.item_chat_user_text, parent, false));
            case VIEW_TYPE_USER_VOICE:
                return new UserVoiceViewHolder(inflater.inflate(R.layout.item_chat_user_voice, parent, false));

            // --- ADDED: Case for the loading bubble ---
            case VIEW_TYPE_LOADING:
                return new LoadingViewHolder(inflater.inflate(R.layout.item_chat_loading, parent, false));

            case VIEW_TYPE_BOT_TEXT: // Fallback/default
            default:
                return new BotViewHolder(inflater.inflate(R.layout.chat_item_bot, parent, false));
        }
    }

    // --- 4. Update onBindViewHolder (with LOADING) ---
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = chatMessages.get(position);
        switch (holder.getItemViewType()) {
            case VIEW_TYPE_USER_TEXT:
                ((UserTextViewHolder) holder).bind(message);
                break;
            case VIEW_TYPE_USER_VOICE:
                ((UserVoiceViewHolder) holder).bind(message);
                break;
            case VIEW_TYPE_BOT_TEXT:
                ((BotViewHolder) holder).bind(message);
                break;
            // --- ADDED: Case for loading bubble ---
            case VIEW_TYPE_LOADING:
                // No data to bind, it just animates by itself
                ((LoadingViewHolder) holder).bind();
                break;
        }
    }

    @Override
    public int getItemCount() {
        return chatMessages.size();
    }

    // --- 5. ViewHolders (including new LoadingViewHolder) ---

    class BotViewHolder extends RecyclerView.ViewHolder {
        TextView tvBotMessage;
        ImageButton btnSpeak;

        BotViewHolder(View itemView) {
            super(itemView);
            tvBotMessage = itemView.findViewById(R.id.tvBotMessage);
            btnSpeak = itemView.findViewById(R.id.btnSpeak);

            // Long-click listener for copying text
            tvBotMessage.setOnLongClickListener(v -> {
                copyTextToClipboard(tvBotMessage.getText().toString(), v.getContext());
                return true; // Handle the click
            });
        }

        void bind(ChatMessage message) {
            tvBotMessage.setText(message.getMessage());
            btnSpeak.setOnClickListener(v -> {
                if (playButtonClickListener != null) {
                    playButtonClickListener.onSpeakerIconClick(message.getMessage());
                }
            });
        }
    }

    class UserTextViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserMessage;
        UserTextViewHolder(View itemView) {
            super(itemView);
            tvUserMessage = itemView.findViewById(R.id.tvUserMessage);

            // Long-click listener for copying text
            tvUserMessage.setOnLongClickListener(v -> {
                copyTextToClipboard(tvUserMessage.getText().toString(), v.getContext());
                return true; // Handle the click
            });
        }
        void bind(ChatMessage message) {
            tvUserMessage.setText(message.getMessage());
        }
    }

    class UserVoiceViewHolder extends RecyclerView.ViewHolder {
        ImageButton btnPlayPauseBubble;
        SeekBar seekBarBubble;
        TextView tvDurationBubble;

        UserVoiceViewHolder(View itemView) {
            super(itemView);
            btnPlayPauseBubble = itemView.findViewById(R.id.btnPlayPauseBubble);
            seekBarBubble = itemView.findViewById(R.id.seekBarBubble);
            tvDurationBubble = itemView.findViewById(R.id.tvDurationBubble);
        }

        void bind(ChatMessage message) {
            btnPlayPauseBubble.setImageResource(R.drawable.ic_play_arrow);
            seekBarBubble.setProgress(0);

            // Use a background thread or MediaPlayer.prepareAsync()
            // to avoid blocking the UI thread if files are large.
            // For simplicity, this follows your original implementation:
            MediaPlayer mp = new MediaPlayer();
            try {
                mp.setDataSource(message.getAudioFilePath());
                mp.prepare();
                int duration = mp.getDuration();
                tvDurationBubble.setText(String.format(Locale.getDefault(), "%d:%02d", (duration / 1000) / 60, (duration / 1000) % 60));
            } catch (IOException e) {
                tvDurationBubble.setText("E:RR");
                e.printStackTrace();
            } finally {
                mp.release();
            }

            btnPlayPauseBubble.setOnClickListener(v -> {
                if (playButtonClickListener != null) {
                    playButtonClickListener.onPlayButtonClick(message.getAudioFilePath(), btnPlayPauseBubble, seekBarBubble, tvDurationBubble);
                }
            });
        }
    }

    // --- ADDED: ViewHolder for the loading indicator ---
    public static class LoadingViewHolder extends RecyclerView.ViewHolder {
        // You can add the LottieAnimationView here if needed
        // LottieAnimationView lottieAnimationView;

        public LoadingViewHolder(@NonNull View itemView) {
            super(itemView);
            // lottieAnimationView = itemView.findViewById(R.id.lottieAnimationView);
        }

        void bind() {
            // Nothing to bind, the Lottie animation in item_chat_loading.xml
            // should have app:lottie_autoPlay="true"
        }
    }
}