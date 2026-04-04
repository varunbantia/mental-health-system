package com.vanaksh.manomitra.ui.chatbot;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.vanaksh.manomitra.R;
import com.vanaksh.manomitra.data.model.ChatSession;

import java.util.ArrayList;
import java.util.List;

public class ChatHistoryAdapter extends RecyclerView.Adapter<ChatHistoryAdapter.ViewHolder> {

    private List<ChatSession> chatSessions = new ArrayList<>();
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(ChatSession session);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setChatSessions(List<ChatSession> chatSessions) {
        this.chatSessions = chatSessions;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatSession session = chatSessions.get(position);
        holder.bind(session, listener);
    }

    @Override
    public int getItemCount() {
        return chatSessions.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvDate;
        private final TextView tvPreview;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            tvDate = itemView.findViewById(R.id.tvChatDate);
            tvPreview = itemView.findViewById(R.id.tvChatPreview);
        }

        public void bind(final ChatSession session, final OnItemClickListener listener) {
            // Set Title (fallback to "New Chat" if empty)
            String preview = session.getLastMessage();
            if (preview == null || preview.isEmpty()) {
                preview = "No messages yet";
            }
            tvPreview.setText(preview);

            // Set Date
            if (session.getTimestamp() != null) {
                CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(
                        session.getTimestamp().getTime(),
                        System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS);
                tvDate.setText(timeAgo);
            } else {
                tvDate.setText("Just now");
            }

            // Click Listener
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(session);
                }
            });
        }
    }
    public List<ChatSession> getChatSessions() {
        return chatSessions; // or whatever your list name is
    }
}
