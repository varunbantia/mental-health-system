package com.vanaksh.manomitra.ui.roles;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.vanaksh.manomitra.R;
import com.vanaksh.manomitra.data.model.ChatSession;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class StudentChatListAdapter extends RecyclerView.Adapter<StudentChatListAdapter.ViewHolder> {

    private List<ChatSession> chatSessions;

    public StudentChatListAdapter(List<ChatSession> chatSessions) {
        this.chatSessions = chatSessions;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatSession session = chatSessions.get(position);
        holder.tvCounsellorName
                .setText(session.getCounsellorName() != null ? session.getCounsellorName() : "Support Specialist");
        holder.tvLastMessage.setText(session.getLastMessage());

        if (session.getLastMessageTimestamp() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            holder.tvTime.setText(sdf.format(session.getLastMessageTimestamp().toDate()));
        }

        if (session.getStudentUnreadCount() > 0) {
            holder.tvUnreadCount.setVisibility(View.VISIBLE);
            holder.tvUnreadCount.setText(String.valueOf(session.getStudentUnreadCount()));
        } else {
            holder.tvUnreadCount.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), StudentChatActivity.class);
            intent.putExtra("appointmentId", session.getChatId());
            intent.putExtra("counsellorId", session.getCounsellorId());
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return chatSessions.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCounsellorName, tvLastMessage, tvTime, tvUnreadCount;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCounsellorName = itemView.findViewById(R.id.tvStudentName); // Reusing ID from item_chat_list
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvUnreadCount = itemView.findViewById(R.id.tvUnreadCount);
        }
    }
}
