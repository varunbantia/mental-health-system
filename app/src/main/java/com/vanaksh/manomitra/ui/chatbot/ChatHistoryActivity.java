package com.vanaksh.manomitra.ui.chatbot;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.vanaksh.manomitra.R;
import com.vanaksh.manomitra.data.model.ChatSession;

import java.util.ArrayList;
import java.util.List;

public class ChatHistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ChatHistoryAdapter adapter;
    private ProgressBar progressBar;
    private TextView tvEmptyState;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_history);

        // Setup Toolbar

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Initialize Views
        recyclerView = findViewById(R.id.rvChatHistory);
        progressBar = findViewById(R.id.progressBar);
        tvEmptyState = findViewById(R.id.tvEmptyState);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChatHistoryAdapter();
        recyclerView.setAdapter(adapter);

        // Item Click Listener
        // Inside onCreate in ChatHistoryActivity.java
        adapter.setOnItemClickListener(session -> {
            Intent resultIntent = new Intent();
            // Use "CHAT_ID" as the key to match common intent standards
            resultIntent.putExtra("CHAT_ID", session.getId());
            setResult(RESULT_OK, resultIntent);
            finish();
        });

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        setupSwipeToDelete();
        loadChatHistory();
    }
    private void setupSwipeToDelete() {
        ItemTouchHelper.SimpleCallback itemTouchHelperCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false; // We don't need drag-and-drop
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                ChatSession sessionToDelete = adapter.getChatSessions().get(position);

                deleteChatFromFirebase(sessionToDelete.getId(), position);
            }
        };

        new ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(recyclerView);
    }

    private void deleteChatFromFirebase(String chatId, int position) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        db.collection("users").document(user.getUid())
                .collection("chats").document(chatId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // Remove from adapter list and notify
                    adapter.getChatSessions().remove(position);
                    adapter.notifyItemRemoved(position);

                    Toast.makeText(this, "Conversation deleted", Toast.LENGTH_SHORT).show();

                    if (adapter.getChatSessions().isEmpty()) {
                        tvEmptyState.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    // Revert swipe if deletion fails
                    adapter.notifyItemChanged(position);
                    Toast.makeText(this, "Error deleting chat", Toast.LENGTH_SHORT).show();
                });
    }
    private void loadChatHistory() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        tvEmptyState.setVisibility(View.GONE);

        db.collection("users")
                .document(currentUser.getUid())
                .collection("chats")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    progressBar.setVisibility(View.GONE);
                    List<ChatSession> sessions = new ArrayList<>();

                    if (queryDocumentSnapshots.isEmpty()) {
                        tvEmptyState.setVisibility(View.VISIBLE);
                    } else {
                        recyclerView.setVisibility(View.VISIBLE);
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            ChatSession session = doc.toObject(ChatSession.class);
                            if (session != null) {
                                session.setId(doc.getId()); // Ensure ID is set
                                sessions.add(session);
                            }
                        }
                        adapter.setChatSessions(sessions);
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to load chats: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
