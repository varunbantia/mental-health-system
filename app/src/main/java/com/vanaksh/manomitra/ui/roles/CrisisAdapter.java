package com.vanaksh.manomitra.ui.roles;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.vanaksh.manomitra.R;
import com.vanaksh.manomitra.data.model.Booking;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class CrisisAdapter extends RecyclerView.Adapter<CrisisAdapter.ViewHolder> {

    private List<Booking> crisisList;
    private OnCrisisActionListener listener;

    public interface OnCrisisActionListener {
        void onResolve(Booking booking);
    }

    public CrisisAdapter(List<Booking> crisisList, OnCrisisActionListener listener) {
        this.crisisList = crisisList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_crisis, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Booking item = crisisList.get(position);

        String displayName = item.getStudentName();
        if (displayName == null || displayName.isEmpty()) {
            displayName = "User: " + (item.getUserRef() != null
                    ? (item.getUserRef().length() > 8 ? item.getUserRef().substring(0, 8) + "..." : item.getUserRef())
                    : "Anonymous");
        }
        holder.tvStudentName.setText(displayName);

        holder.tvPriority.setText("CRITICAL"); // Default for crisis Flag entries
        holder.tvMessage.setText(item.getConcernCategory() != null ? item.getConcernCategory() : "Mental Health Alert");

        if (item.getCreatedAt() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
            holder.tvTime.setText(sdf.format(item.getCreatedAt().toDate()));
        }

        holder.btnResolve.setOnClickListener(v -> listener.onResolve(item));
    }

    @Override
    public int getItemCount() {
        return crisisList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvStudentName, tvPriority, tvMessage, tvTime;
        MaterialButton btnResolve;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStudentName = itemView.findViewById(R.id.tvStudentName);
            tvPriority = itemView.findViewById(R.id.tvPriority);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            btnResolve = itemView.findViewById(R.id.btnResolve);
        }
    }
}
