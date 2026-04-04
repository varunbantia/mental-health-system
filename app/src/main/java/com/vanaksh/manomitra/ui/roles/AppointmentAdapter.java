package com.vanaksh.manomitra.ui.roles;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.vanaksh.manomitra.R;
import com.vanaksh.manomitra.data.model.Booking;
import java.util.List;

public class AppointmentAdapter extends RecyclerView.Adapter<AppointmentAdapter.ViewHolder> {

    private List<Booking> appointments;
    private OnAppointmentActionListener listener;
    private boolean isManageMode;

    public interface OnAppointmentActionListener {
        void onAccept(Booking booking);

        void onReject(Booking booking);
    }

    public AppointmentAdapter(List<Booking> appointments, boolean isManageMode, OnAppointmentActionListener listener) {
        this.appointments = appointments;
        this.isManageMode = isManageMode;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_appointment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Booking booking = appointments.get(position);

        String displayName = booking.getStudentName();
        if (displayName == null || displayName.isEmpty()) {
            displayName = "User: "
                    + (booking.getUserRef() != null
                            ? (booking.getUserRef().length() > 8 ? booking.getUserRef().substring(0, 8) + "..."
                                    : booking.getUserRef())
                            : "Anonymous");
        }
        holder.tvStudentName.setText(displayName);

        String dateTime = (booking.getPreferredDate() != null ? booking.getPreferredDate() : "N/A")
                + " • " + (booking.getPreferredTime() != null ? booking.getPreferredTime() : "N/A");
        holder.tvDateTime.setText(dateTime);

        holder.chipStatus.setText(booking.getStatus().toUpperCase());

        // Status Colors
        if ("confirmed".equals(booking.getStatus())) {
            holder.chipStatus.setChipBackgroundColorResource(android.R.color.holo_green_light);
        } else if ("pending".equals(booking.getStatus())) {
            holder.chipStatus.setChipBackgroundColorResource(android.R.color.holo_orange_light);
        } else {
            holder.chipStatus.setChipBackgroundColorResource(android.R.color.darker_gray);
        }

        // Crisis Badge
        holder.layoutCrisis.setVisibility(booking.isCrisisFlag() ? View.VISIBLE : View.GONE);

        // Manage Mode Actions
        if (isManageMode && "pending".equals(booking.getStatus())) {
            holder.layoutActions.setVisibility(View.VISIBLE);
            holder.btnAccept.setOnClickListener(v -> listener.onAccept(booking));
            holder.btnReject.setOnClickListener(v -> listener.onReject(booking));
        } else {
            holder.layoutActions.setVisibility(View.GONE);
        }

        // Chat Button logic for confirmed appointments
        if ("confirmed".equals(booking.getStatus())) {
            holder.btnChat.setVisibility(View.VISIBLE);
            holder.btnChat.setOnClickListener(v -> {
                android.content.Intent intent = new android.content.Intent(v.getContext(),
                        CounsellorChatActivity.class);
                intent.putExtra("appointmentId", booking.getBookingId());
                intent.putExtra("studentId", booking.getStudentId());
                v.getContext().startActivity(intent);
            });
        } else {
            holder.btnChat.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return appointments.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvStudentName, tvDateTime;
        Chip chipStatus;
        View layoutCrisis, layoutActions;
        MaterialButton btnAccept, btnReject, btnChat;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStudentName = itemView.findViewById(R.id.tvStudentName);
            tvDateTime = itemView.findViewById(R.id.tvDateTime);
            chipStatus = itemView.findViewById(R.id.chipStatus);
            layoutCrisis = itemView.findViewById(R.id.layoutCrisis);
            layoutActions = itemView.findViewById(R.id.layoutActions);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnReject = itemView.findViewById(R.id.btnReject);
            btnChat = itemView.findViewById(R.id.btnChat);
        }
    }
}
