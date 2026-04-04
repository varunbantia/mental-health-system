package com.vanaksh.manomitra.community;

import android.content.Intent;
import com.vanaksh.manomitra.R;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.DocumentSnapshot;
import com.vanaksh.manomitra.data.model.Post;
import com.vanaksh.manomitra.data.repository.PeerSupportRepository;
import com.vanaksh.manomitra.databinding.ActivityPeerSupportBinding;
import java.util.ArrayList;
import java.util.List;

public class PeerSupportActivity extends AppCompatActivity {

    private ActivityPeerSupportBinding binding;
    private PeerSupportRepository repository;
    private PostAdapter adapter;
    private List<Post> allLoadedPosts = new ArrayList<>();
    private com.vanaksh.manomitra.data.model.User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPeerSupportBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = new PeerSupportRepository();
        fetchCurrentUser();
        setupRecyclerView();
        setupListeners();
        loadPosts("All");
    }

    private void fetchCurrentUser() {
        String uid = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
        if (uid != null) {
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users").document(uid)
                    .get().addOnSuccessListener(snapshot -> {
                        currentUser = snapshot.toObject(com.vanaksh.manomitra.data.model.User.class);
                    });
        }
    }


    private void setupRecyclerView() {
        adapter = new PostAdapter();
        adapter.setOnPostClickListener(post -> {
            Intent intent = new Intent(this, PostDetailActivity.class);
            intent.putExtra("post", post);
            startActivity(intent);
        });
        adapter.setOnSupportClickListener(new PostAdapter.OnSupportClickListener() {
            @Override
            public void onSupportClick(Post post) {
                repository.supportPost(post.getPostId());
            }

            @Override
            public void onUnsupportClick(Post post) {
                repository.unsupportPost(post.getPostId());
            }
        });
        binding.recyclerViewPosts.setAdapter(adapter);
    }

    private void setupListeners() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        binding.fabNewPost.setOnClickListener(v -> {
            startActivity(new Intent(this, CreatePostActivity.class));
        });

        binding.chipGroupCategories.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            
            int checkedId = checkedIds.get(0);
            String category = "All";
            
            if (checkedId == binding.chipAnxiety.getId()) category = "Anxiety";
            else if (checkedId == binding.chipDepression.getId()) category = "Depression";
            else if (checkedId == binding.chipAcademic.getId()) category = "Academic Stress";
            else if (checkedId == binding.chipRelationships.getId()) category = "Relationships";
            else if (checkedId == binding.chipFamily.getId()) category = "Family";
            else if (checkedId == binding.chipGeneral.getId()) category = "General Support";
            
            loadPosts(category);
        });
        

        binding.etSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                performSearch(s.toString());
            }
            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
    }

    private void performSearch(String query) {
        if (query.isEmpty()) {
            adapter.setPosts(allLoadedPosts);
            return;
        }
        
        List<Post> filtered = new ArrayList<>();
        for (Post post : allLoadedPosts) {
            if (post.getTitle().toLowerCase().contains(query.toLowerCase()) || 
                post.getDescription().toLowerCase().contains(query.toLowerCase())) {
                filtered.add(post);
            }
        }
        adapter.setPosts(filtered);
    }

    private void loadPosts(String category) {
        if (category.equals("All")) {
            repository.getApprovedPosts().addSnapshotListener((value, error) -> {
                if (error != null) return;
                processSnapshots(value.getDocuments());
            });
        } else {
            repository.getPostsByCategory(category).addSnapshotListener((value, error) -> {
                if (error != null) return;
                processSnapshots(value.getDocuments());
            });
        }
    }

    private void processSnapshots(List<DocumentSnapshot> documents) {
        allLoadedPosts.clear();
        for (DocumentSnapshot doc : documents) {
            Post post = doc.toObject(Post.class);
            if (post != null) allLoadedPosts.add(post);
        }
        // Sort by timestamp descending (newest first)
        java.util.Collections.sort(allLoadedPosts, (p1, p2) -> Long.compare(p2.getTimestamp(), p1.getTimestamp()));
        
        // Re-apply current search if any
        String currentSearch = binding.etSearch.getText().toString();
        if (!currentSearch.isEmpty()) {
            performSearch(currentSearch);
        } else {
            adapter.setPosts(allLoadedPosts);
        }
    }
}
