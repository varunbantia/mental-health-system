package com.vanaksh.manomitra.community;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.vanaksh.manomitra.R;
import com.vanaksh.manomitra.data.model.Reply;
import com.vanaksh.manomitra.databinding.ItemReplyBinding;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReplyAdapter extends RecyclerView.Adapter<ReplyAdapter.ReplyViewHolder> {

    private final java.util.Set<String> supportedReplies = new java.util.HashSet<>();
    private final java.util.Map<String, Integer> dirtySupportCounts = new java.util.HashMap<>();
    private List<Reply> replyList = new ArrayList<>();
    private OnReplyClickListener listener;

    public interface OnReplyClickListener {
        void onReplyClick(Reply parentReply);
    }

    public interface OnSupportClickListener {
        void onSupportClick(Reply reply);
        void onUnsupportClick(Reply reply);
    }

    public void setOnReplyClickListener(OnReplyClickListener listener) {
        this.listener = listener;
    }

    private OnSupportClickListener supportListener;
    public void setOnSupportClickListener(OnSupportClickListener listener) {
        this.supportListener = listener;
    }

    public void setReplies(List<Reply> replies) {
        // Sort all replies by timestamp ascending first
        java.util.Collections.sort(replies, (r1, r2) -> Long.compare(r1.getTimestamp(), r2.getTimestamp()));
        this.replyList = sortThreaded(replies);
        notifyDataSetChanged();
    }

    private List<Reply> sortThreaded(List<Reply> replies) {
        List<Reply> sorted = new ArrayList<>();
        List<Reply> topLevel = new ArrayList<>();
        java.util.Map<String, List<Reply>> childrenMap = new java.util.HashMap<>();

        for (Reply r : replies) {
            if (r.getParentReplyId() == null) {
                topLevel.add(r);
            } else {
                childrenMap.computeIfAbsent(r.getParentReplyId(), k -> new ArrayList<>()).add(r);
            }
        }

        for (Reply top : topLevel) {
            addChild(top, childrenMap, sorted);
        }
        return sorted;
    }

    private void addChild(Reply parent, java.util.Map<String, List<Reply>> childrenMap, List<Reply> sorted) {
        sorted.add(parent);
        List<Reply> children = childrenMap.get(parent.getReplyId());
        if (children != null) {
            for (Reply child : children) {
                addChild(child, childrenMap, sorted);
            }
        }
    }

    @NonNull
    @Override
    public ReplyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemReplyBinding binding = ItemReplyBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ReplyViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ReplyViewHolder holder, int position) {
        Reply reply = replyList.get(position);
        holder.bind(reply);
    }

    @Override
    public int getItemCount() {
        return replyList.size();
    }

    class ReplyViewHolder extends RecyclerView.ViewHolder {
        private final ItemReplyBinding binding;

        public ReplyViewHolder(ItemReplyBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            binding.btnReply.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.onReplyClick(replyList.get(pos));
                }
            });

            binding.btnSupport.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && supportListener != null) {
                    Reply reply = replyList.get(pos);
                    int currentCount = dirtySupportCounts.getOrDefault(reply.getReplyId(), reply.getSupportCount());
                    if (!supportedReplies.contains(reply.getReplyId())) {
                        supportedReplies.add(reply.getReplyId());
                        supportListener.onSupportClick(reply);
                        dirtySupportCounts.put(reply.getReplyId(), currentCount + 1);
                    } else {
                        supportedReplies.remove(reply.getReplyId());
                        supportListener.onUnsupportClick(reply);
                        dirtySupportCounts.put(reply.getReplyId(), Math.max(0, currentCount - 1));
                    }
                    notifyItemChanged(pos);
                }
            });
        }

        public void bind(Reply reply) {
            binding.tvUsername.setText(reply.getUserName());
            int displayCount = dirtySupportCounts.getOrDefault(reply.getReplyId(), reply.getSupportCount());
            binding.tvSupportCount.setText(String.valueOf(displayCount));
            
            if (supportedReplies.contains(reply.getReplyId())) {
                binding.ivHeart.setImageResource(R.drawable.ic_heart_filled);
                binding.ivHeart.setImageTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#E91E63")));
            } else {
                binding.ivHeart.setImageResource(R.drawable.ic_heart);
                binding.ivHeart.setImageTintList(android.content.res.ColorStateList.valueOf(binding.getRoot().getContext().getColor(R.color.md_theme_light_primary)));
            }
            
            if (reply.getParentUserName() != null) {
                binding.tvMessage.setText("@" + reply.getParentUserName() + " " + reply.getMessage());
            } else {
                binding.tvMessage.setText(reply.getMessage());
            }
            
            // Indentation
            int level = getIndentationLevel(reply);
            int paddingStart = (int) (20 * itemView.getContext().getResources().getDisplayMetrics().density);
            int indent = (int) (16 * level * itemView.getContext().getResources().getDisplayMetrics().density);
            itemView.setPadding(paddingStart + indent, itemView.getPaddingTop(), 
                    itemView.getPaddingEnd(), itemView.getPaddingBottom());

            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
            binding.tvTime.setText(sdf.format(new Date(reply.getTimestamp())));
            
            // ... (rest of the badge logic)

            if ("volunteer".equalsIgnoreCase(reply.getRole())) {
                binding.tvBadge.setVisibility(View.VISIBLE);
                binding.tvBadge.setText(R.string.badge_volunteer);
                binding.tvBadge.setBackgroundTintList(itemView.getContext().getColorStateList(R.color.md_theme_light_primaryContainer));
            } else if ("moderator".equalsIgnoreCase(reply.getRole())) {
                binding.tvBadge.setVisibility(View.VISIBLE);
                binding.tvBadge.setText(R.string.badge_moderator);
                binding.tvBadge.setBackgroundTintList(itemView.getContext().getColorStateList(R.color.md_theme_light_tertiaryContainer));
            } else {
                binding.tvBadge.setVisibility(View.GONE);
            }
            
            // Set avatar (placeholder logic)
            if ("Anonymous".equalsIgnoreCase(reply.getUserName())) {
                binding.ivAvatar.setImageResource(R.drawable.ic_anonymous);
            } else {
                binding.ivAvatar.setImageResource(R.drawable.ic_person);
            }
        }

        private int getIndentationLevel(Reply reply) {
            int level = 0;
            String parentId = reply.getParentReplyId();
            while (parentId != null) {
                level++;
                // This is slightly inefficient as we search for parent in list
                // But since replies per post are usually < 100, it's fine.
                Reply parent = findReplyById(parentId);
                if (parent == null) break;
                parentId = parent.getParentReplyId();
                if (level > 3) break; // Max indentation
            }
            return level;
        }

        private Reply findReplyById(String id) {
            for (Reply r : replyList) {
                if (id.equals(r.getReplyId())) return r;
            }
            return null;
        }
    }
}
