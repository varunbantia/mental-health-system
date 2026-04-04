package com.vanaksh.manomitra.ui.roles;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.vanaksh.manomitra.R;
import com.vanaksh.manomitra.data.model.ChatSession;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CounsellorChatListActivity extends AppCompatActivity {

    private RecyclerView rvChats;
    private CounsellorChatAdapter adapter;
    private List<ChatSession> chatSessions = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private View progressBar, llEmptyState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_appointments); // Reusing layout

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        setupToolbar();
        initViews();
        fetchChatSessions();
    }

    private void setupToolbar() {
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Student Chats 💬");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }

    private void initViews() {
        rvChats = findViewById(R.id.rvAppointments);
        progressBar = findViewById(R.id.progressBar);
        llEmptyState = findViewById(R.id.llEmptyState);

        rvChats.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CounsellorChatAdapter(chatSessions);
        rvChats.setAdapter(adapter);
    }

    private void fetchChatSessions() {
        String uid = mAuth.getUid();
        if (uid == null)
            return;

        progressBar.setVisibility(View.VISIBLE);
        db.collection("messages") // Or chat_sessions if you prefer a separate collection
                .whereArrayContains("participants", uid)
                .addSnapshotListener((snapshots, e) -> {
                    progressBar.setVisibility(View.GONE);
                    if (e != null) {
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    chatSessions.clear();
                    if (snapshots != null) {
                        for (com.google.firebase.firestore.DocumentSnapshot doc : snapshots) {
                            ChatSession session = doc.toObject(ChatSession.class);
                            if (session != null) {
                                session.setChatId(doc.getId());
                                chatSessions.add(session);
                            }
                        }

                        // Sort in-memory to avoid index requirement
                        Collections.sort(chatSessions, (s1, s2) -> {
                            if (s1.getLastMessageTimestamp() == null || s2.getLastMessageTimestamp() == null)
                                return 0;
                            return s2.getLastMessageTimestamp().compareTo(s1.getLastMessageTimestamp()); // Descending
                        });
                    }

                    llEmptyState.setVisibility(chatSessions.isEmpty() ? View.VISIBLE : View.GONE);
                    adapter.notifyDataSetChanged();
                });
    }
}
