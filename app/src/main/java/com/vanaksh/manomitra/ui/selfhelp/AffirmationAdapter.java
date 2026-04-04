package com.vanaksh.manomitra.ui.selfhelp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.vanaksh.manomitra.R;

import java.util.List;

public class AffirmationAdapter extends RecyclerView.Adapter<AffirmationAdapter.AffirmationViewHolder> {

    private List<String> affirmations;

    public AffirmationAdapter(List<String> affirmations) {
        this.affirmations = affirmations;
    }

    @NonNull
    @Override
    public AffirmationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_affirmation_card, parent, false);
        return new AffirmationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AffirmationViewHolder holder, int position) {
        holder.tvAffirmation.setText(affirmations.get(position));
    }

    @Override
    public int getItemCount() {
        return affirmations.size();
    }

    static class AffirmationViewHolder extends RecyclerView.ViewHolder {
        TextView tvAffirmation;

        public AffirmationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAffirmation = itemView.findViewById(R.id.tv_affirmation);
        }
    }
}
