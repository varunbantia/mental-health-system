package com.vanaksh.manomitra.booking;

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

public class BookingStatusAdapter extends RecyclerView.Adapter<BookingStatusAdapter.ViewHolder> {

    public interface OnCancelClickListener {
        void onCancel(Booking booking);
    }

    private final List<Booking> bookings;
    private final OnCancelClickListener cancelListener;

    public BookingStatusAdapter(List<Booking> bookings,
            OnCancelClickListener cancelListener) {
        this.bookings = bookings;
        this.cancelListener = cancelListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_booking_status, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Booking booking = bookings.get(position);

        holder.tvBookingId.setText(booking.getBookingId());

        // Type label
        String typeLabel = "counselor".equals(booking.getBookingType())
                ? "On-campus Counselor"
                : "Mental Health Helpline";
        holder.tvType.setText(typeLabel);

        // Date & Time
        String dateTime = (booking.getPreferredDate() != null ? booking.getPreferredDate() : "")
                + "  •  "
                + (booking.getPreferredTime() != null ? booking.getPreferredTime() : "");
        holder.tvDateTime.setText(dateTime);

        // Status chip
        String status = booking.getStatus();
        holder.chipStatus.setText(capitalize(status));

        // Status chip — use theme-resolved colors
        if (Booking.STATUS_CONFIRMED.equals(status)) {
            holder.chipStatus.setChipBackgroundColor(
                    android.content.res.ColorStateList.valueOf(
                            com.google.android.material.color.MaterialColors.getColor(
                                    holder.itemView, com.google.android.material.R.attr.colorPrimaryContainer)));
            holder.chipStatus.setTextColor(
                    com.google.android.material.color.MaterialColors.getColor(
                            holder.itemView, com.google.android.material.R.attr.colorOnPrimaryContainer));
        } else if (Booking.STATUS_CANCELLED.equals(status)) {
            holder.chipStatus.setChipBackgroundColor(
                    android.content.res.ColorStateList.valueOf(
                            com.google.android.material.color.MaterialColors.getColor(
                                    holder.itemView, com.google.android.material.R.attr.colorSecondaryContainer)));
            holder.chipStatus.setTextColor(
                    com.google.android.material.color.MaterialColors.getColor(
                            holder.itemView, com.google.android.material.R.attr.colorOnSecondaryContainer));
        } else {
            holder.chipStatus.setChipBackgroundColor(
                    android.content.res.ColorStateList.valueOf(
                            com.google.android.material.color.MaterialColors.getColor(
                                    holder.itemView, com.google.android.material.R.attr.colorTertiaryContainer)));
            holder.chipStatus.setTextColor(
                    com.google.android.material.color.MaterialColors.getColor(
                            holder.itemView, com.google.android.material.R.attr.colorOnTertiaryContainer));
        }

        // Hide cancel button for already-cancelled or confirmed bookings
        if (Booking.STATUS_CANCELLED.equals(status) || Booking.STATUS_CONFIRMED.equals(status)) {
            holder.btnCancel.setVisibility(View.GONE);
        } else {
            holder.btnCancel.setVisibility(View.VISIBLE);
            holder.btnCancel.setOnClickListener(v -> cancelListener.onCancel(booking));
        }
    }

    @Override
    public int getItemCount() {
        return bookings.size();
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty())
            return "Pending";
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvBookingId, tvType, tvDateTime;
        Chip chipStatus;
        MaterialButton btnCancel;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBookingId = itemView.findViewById(R.id.tvBookingId);
            tvType = itemView.findViewById(R.id.tvType);
            tvDateTime = itemView.findViewById(R.id.tvDateTime);
            chipStatus = itemView.findViewById(R.id.chipStatus);
            btnCancel = itemView.findViewById(R.id.btnCancel);
        }
    }
}
