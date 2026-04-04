package com.vanaksh.manomitra.ui.roles;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.DocumentSnapshot;
import com.vanaksh.manomitra.community.PostAdapter;
import com.vanaksh.manomitra.data.model.Post;
import com.vanaksh.manomitra.data.repository.PeerSupportRepository;
import com.vanaksh.manomitra.databinding.ActivityModeratorDashboardBinding;
import java.util.ArrayList;
import java.util.List;

public class ModeratorDashboardActivity extends AppCompatActivity {

    private ActivityModeratorDashboardBinding binding;
    private PeerSupportRepository repository;
    private PostAdapter adapter;
    private int currentTab = 0; // 0: Pending, 1: Flagged, 2: Escalated

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityModeratorDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = new PeerSupportRepository();
        setupRecyclerView();
        setupListeners();
        loadModerationPosts();
    }

    private void setupRecyclerView() {
        adapter = new PostAdapter();
        // We override click to do nothing or open detail for review
        adapter.setOnPostClickListener(post -> {
            // Option: Open detail for deep review
        });
        binding.recyclerViewModeration.setAdapter(adapter);

        // Swipe to Action
        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                // Assumed: Swipe Right = Approve, Swipe Left = Remove/Flag
                // This is simplified. In real app, show icons during swipe.
                handleModerationAction(position, direction);
            }
        };
        new ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.recyclerViewModeration);
    }

    private void handleModerationAction(int position, int direction) {
        Post post = adapter.getPostAt(position); // Need to add this method to adapter
        if (post == null) return;

        if (direction == ItemTouchHelper.RIGHT) {
            // Approve
            repository.updatePostStatus(post.getPostId(), "approved");
            Toast.makeText(this, "Post Approved", Toast.LENGTH_SHORT).show();
        } else {
            // Remove/Reject (Delete from firestore or set to removed)
            repository.updatePostStatus(post.getPostId(), "removed");
            Toast.makeText(this, "Post Removed", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupListeners() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                loadModerationPosts();
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void loadModerationPosts() {
        if (currentTab == 0) {
            repository.getPendingPosts().addSnapshotListener((value, error) -> updateList(value.getDocuments()));
        } else if (currentTab == 1) {
            repository.getFlaggedPosts().addSnapshotListener((value, error) -> updateList(value.getDocuments()));
        } else {
            // Escalated logic (placeholder)
            updateList(new ArrayList<>());
        }
    }

    private void updateList(List<DocumentSnapshot> documents) {
        List<Post> posts = new ArrayList<>();
        for (DocumentSnapshot doc : documents) {
            Post post = doc.toObject(Post.class);
            if (post != null) posts.add(post);
        }
        adapter.setPosts(posts);
        binding.tvEmpty.setVisibility(posts.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
