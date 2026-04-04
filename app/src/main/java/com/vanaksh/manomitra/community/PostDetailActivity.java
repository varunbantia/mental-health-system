package com.vanaksh.manomitra.community;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.vanaksh.manomitra.R;
import com.vanaksh.manomitra.data.model.Post;
import com.vanaksh.manomitra.data.model.Reply;
import com.vanaksh.manomitra.data.model.User;
import com.vanaksh.manomitra.data.repository.PeerSupportRepository;
import com.vanaksh.manomitra.databinding.ActivityPostDetailBinding;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class PostDetailActivity extends AppCompatActivity {

    private ActivityPostDetailBinding binding;
    private PeerSupportRepository repository;
    private Post post;
    private ReplyAdapter adapter;
    private String currentUserId;
    private User currentUser;
    private Reply replyingToReply = null;
    private boolean isPostSupported = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPostDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = new PeerSupportRepository();
        currentUserId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        post = (Post) getIntent().getSerializableExtra("post");

        if (post == null) {
            finish();
            return;
        }

        fetchCurrentUser();
        setupUI();
        setupRecyclerView();
        loadReplies();
    }

    private void fetchCurrentUser() {
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users").document(currentUserId)
                .get().addOnSuccessListener(snapshot -> {
                    currentUser = snapshot.toObject(User.class);
                });
    }

    private void loadPostDetails() {
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("posts").document(post.getPostId())
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null || !snapshot.exists()) return;
                    Post updatedPost = snapshot.toObject(Post.class);
                    if (updatedPost != null) {
                        int supportCount = PostAdapter.getDirtySupportCount(post.getPostId(), updatedPost.getSupportCount());
                        binding.tvSupportCount.setText(supportCount + " supports");
                        
                        // Update local post object for future actions
                        post.setSupportCount(updatedPost.getSupportCount());
                        post.setReplyCount(updatedPost.getReplyCount());
                        
                        // If we are not currently replying (where we manually increment), 
                        // update the replies count label too from server if it's more accurate
                        int replyCount = PostAdapter.getDirtyReplyCount(post.getPostId(), updatedPost.getReplyCount());
                        binding.tvRepliesCount.setText("Replies (" + replyCount + ")");
                    }
                });
    }

    private void setupUI() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        
        binding.tvTitle.setText(post.getTitle());
        binding.tvDescription.setText(post.getDescription());
        binding.tvCategory.setText(post.getCategory());
        int displaySupport = PostAdapter.getDirtySupportCount(post.getPostId(), post.getSupportCount());
        binding.tvSupportCount.setText(displaySupport + " supports");
        
        int displayReply = PostAdapter.getDirtyReplyCount(post.getPostId(), post.getReplyCount());
        binding.tvRepliesCount.setText("Replies (" + displayReply + ")");
        
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
        binding.tvTime.setText(sdf.format(new Date(post.getTimestamp())));
        
        loadPostDetails();

        if (post.isAnonymous()) {
            binding.tvUsername.setText("Anonymous");
            binding.ivAvatar.setImageResource(R.drawable.ic_anonymous);
        } else {
            // In a real app, we'd fetch the poster's name. 
            // For now, if not anonymous, we show "Student User" if name isn't in Post model.
            binding.tvUsername.setText("Student User");
            binding.ivAvatar.setImageResource(R.drawable.ic_person);
        }

        binding.btnSendReply.setOnClickListener(v -> {
            String message = binding.etReply.getText().toString().trim();
            if (!message.isEmpty()) {
                submitReply(message);
            }
        });

        binding.btnCancelReplyTo.setOnClickListener(v -> {
            replyingToReply = null;
            binding.layoutReplyingTo.setVisibility(View.GONE);
        });
        
        binding.btnReport.setOnClickListener(v -> {
            Toast.makeText(this, "Post reported for review.", Toast.LENGTH_SHORT).show();
            // In real app, show a dialog to choose reason
        });

        // Initialize support state from global set
        isPostSupported = PostAdapter.isPostSupported(post.getPostId());
        if (isPostSupported) {
            binding.ivHeart.setImageResource(R.drawable.ic_heart_filled);
            binding.ivHeart.setImageTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#E91E63")));
        }

        binding.btnSupport.setOnClickListener(v -> {
            if (!isPostSupported) {
                isPostSupported = true;
                repository.supportPost(post.getPostId());
                PostAdapter.setPostSupported(post.getPostId(), true);
                
                post.setSupportCount(post.getSupportCount() + 1);
                PostAdapter.setDirtySupportCount(post.getPostId(), post.getSupportCount());
                
                binding.tvSupportCount.setText(post.getSupportCount() + " supports");
                binding.ivHeart.setImageResource(R.drawable.ic_heart_filled);
                binding.ivHeart.setImageTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#E91E63")));
            } else {
                isPostSupported = false;
                repository.unsupportPost(post.getPostId());
                PostAdapter.setPostSupported(post.getPostId(), false);
                
                post.setSupportCount(Math.max(0, post.getSupportCount() - 1));
                PostAdapter.setDirtySupportCount(post.getPostId(), post.getSupportCount());
                
                binding.tvSupportCount.setText(post.getSupportCount() + " supports");
                binding.ivHeart.setImageResource(R.drawable.ic_heart);
                binding.ivHeart.setImageTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.md_theme_light_primary)));
            }
        });
    }

    private void setupRecyclerView() {
        adapter = new ReplyAdapter();
        adapter.setOnReplyClickListener(parentReply -> {
            replyingToReply = parentReply;
            binding.layoutReplyingTo.setVisibility(View.VISIBLE);
            binding.tvReplyingToName.setText("Replying to " + parentReply.getUserName());
            binding.etReply.requestFocus();
            // Show keyboard? Usually automatic on focus
        });

        adapter.setOnSupportClickListener(new ReplyAdapter.OnSupportClickListener() {
            @Override
            public void onSupportClick(Reply reply) {
                repository.supportReply(reply.getReplyId());
            }

            @Override
            public void onUnsupportClick(Reply reply) {
                repository.unsupportReply(reply.getReplyId());
            }
        });
        binding.recyclerViewReplies.setAdapter(adapter);
    }

    private void loadReplies() {
        repository.getRepliesForPost(post.getPostId()).addSnapshotListener((value, error) -> {
            if (error != null) return;
            
            List<Reply> replies = new ArrayList<>();
            for (DocumentSnapshot doc : value.getDocuments()) {
                Reply reply = doc.toObject(Reply.class);
                if (reply != null) replies.add(reply);
            }
            adapter.setReplies(replies);
            binding.tvRepliesCount.setText("Replies (" + replies.size() + ")");
        });
    }

    private void submitReply(String message) {
        String role = "user";
        String userName = "Anonymous";
        
        if (currentUser != null) {
            role = currentUser.getRole();
            userName = currentUser.getName();
        }

        String parentId = replyingToReply != null ? replyingToReply.getReplyId() : null;
        String parentName = replyingToReply != null ? replyingToReply.getUserName() : null;

        Reply reply = new Reply(null, post.getPostId(), currentUserId, message, 
                System.currentTimeMillis(), role, userName, parentId, parentName);
        
        repository.addReply(reply);
        repository.incrementReplyCount(post.getPostId());
        
        // Optimistic UI update
        post.setReplyCount(post.getReplyCount() + 1);
        PostAdapter.setDirtyReplyCount(post.getPostId(), post.getReplyCount());
        binding.tvRepliesCount.setText("Replies (" + post.getReplyCount() + ")");
        
        binding.etReply.setText("");
        replyingToReply = null;
        binding.layoutReplyingTo.setVisibility(View.GONE);
        Toast.makeText(this, "Reply posted!", Toast.LENGTH_SHORT).show();
    }
}
