package com.vanaksh.manomitra.community;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.vanaksh.manomitra.R;
import com.vanaksh.manomitra.data.model.Post;
import com.vanaksh.manomitra.databinding.ItemPostBinding;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private static final java.util.Set<String> supportedPosts = new java.util.HashSet<>();
    private static final java.util.Map<String, Integer> dirtySupportCounts = new java.util.HashMap<>();
    private static final java.util.Map<String, Integer> dirtyReplyCounts = new java.util.HashMap<>();
    private List<Post> postList = new ArrayList<>();
    private OnPostClickListener listener;

    public interface OnPostClickListener {
        void onPostClick(Post post);
    }

    public interface OnSupportClickListener {
        void onSupportClick(Post post);
        void onUnsupportClick(Post post);
    }

    public void setOnPostClickListener(OnPostClickListener listener) {
        this.listener = listener;
    }

    private OnSupportClickListener supportListener;
    public void setOnSupportClickListener(OnSupportClickListener listener) {
        this.supportListener = listener;
    }

    public void setPosts(List<Post> posts) {
        this.postList = posts;
        notifyDataSetChanged();
    }

    public Post getPostAt(int position) {
        if (position >= 0 && position < postList.size()) {
            return postList.get(position);
        }
        return null;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPostBinding binding = ItemPostBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new PostViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = postList.get(position);
        holder.bind(post);
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    class PostViewHolder extends RecyclerView.ViewHolder {
        private final ItemPostBinding binding;

        public PostViewHolder(ItemPostBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            binding.btnSupport.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && supportListener != null) {
                    Post post = postList.get(pos);
                    int currentCount = dirtySupportCounts.getOrDefault(post.getPostId(), post.getSupportCount());
                    if (!supportedPosts.contains(post.getPostId())) {
                        supportedPosts.add(post.getPostId());
                        supportListener.onSupportClick(post);
                        dirtySupportCounts.put(post.getPostId(), currentCount + 1);
                    } else {
                        supportedPosts.remove(post.getPostId());
                        supportListener.onUnsupportClick(post);
                        dirtySupportCounts.put(post.getPostId(), Math.max(0, currentCount - 1));
                    }
                    notifyItemChanged(pos);
                }
            });

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.onPostClick(postList.get(pos));
                }
            });
        }

        public void bind(Post post) {
            binding.tvTitle.setText(post.getTitle());
            binding.tvPreview.setText(post.getDescription());
            binding.tvCategory.setText(post.getCategory());
            
            int displaySupport = dirtySupportCounts.getOrDefault(post.getPostId(), post.getSupportCount());
            binding.tvSupportCount.setText(String.valueOf(displaySupport));
            
            int displayReply = dirtyReplyCounts.getOrDefault(post.getPostId(), post.getReplyCount());
            binding.tvReplyCount.setText(String.valueOf(displayReply));
            
            if (supportedPosts.contains(post.getPostId())) {
                binding.ivHeart.setImageResource(R.drawable.ic_heart_filled);
                binding.ivHeart.setImageTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#E91E63")));
            } else {
                binding.ivHeart.setImageResource(R.drawable.ic_heart);
                binding.ivHeart.setImageTintList(android.content.res.ColorStateList.valueOf(binding.getRoot().getContext().getColor(R.color.md_theme_light_primary)));
            }
            
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
            binding.tvTime.setText(sdf.format(new Date(post.getTimestamp())));

            // If not anonymous, we might show actual name/avatar (if we had those in Post)
            // For now, prompt requirements said "Anonymous avatar icon" as a bullet point.
            if (post.isAnonymous()) {
                binding.ivAvatar.setImageResource(R.drawable.ic_anonymous);
            } else {
                binding.ivAvatar.setImageResource(R.drawable.ic_person);
            }
        }
    }

    public static int getDirtySupportCount(String postId, int serverCount) {
        return dirtySupportCounts.getOrDefault(postId, serverCount);
    }

    public static int getDirtyReplyCount(String postId, int serverCount) {
        return dirtyReplyCounts.getOrDefault(postId, serverCount);
    }

    public static void setDirtyReplyCount(String postId, int count) {
        dirtyReplyCounts.put(postId, count);
    }

    public static void setDirtySupportCount(String postId, int count) {
        dirtySupportCounts.put(postId, count);
    }

    public static boolean isPostSupported(String postId) {
        return supportedPosts.contains(postId);
    }

    public static void setPostSupported(String postId, boolean supported) {
        if (supported) supportedPosts.add(postId);
        else supportedPosts.remove(postId);
    }
}
