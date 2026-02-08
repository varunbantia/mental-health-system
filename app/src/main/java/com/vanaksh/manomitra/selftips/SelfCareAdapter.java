package com.vanaksh.manomitra.selftips;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.vanaksh.manomitra.R;
import java.util.List;

public class SelfCareAdapter extends RecyclerView.Adapter<SelfCareAdapter.ViewHolder> {

    private List<SelfCareTip> tips;

    public SelfCareAdapter(List<SelfCareTip> tips) {
        this.tips = tips;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SelfCareTip tip = tips.get(position);
        holder.textView.setText(tip.getText());
        holder.categoryView.setText(tip.getCategory());

        try {
            holder.cardBackground.setBackgroundColor(Color.parseColor(tip.getBackgroundColor()));
        } catch (Exception e) {
            holder.cardBackground.setBackgroundColor(Color.parseColor("#D1F2E5"));
        }
    }

    @Override
    public int getItemCount() { return tips.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView, categoryView;
        LinearLayout cardBackground;

        ViewHolder(View view) {
            super(view);
            textView = view.findViewById(R.id.item_text);
            categoryView = view.findViewById(R.id.item_category);
            cardBackground = view.findViewById(R.id.card_background);
        }
    }
}