package com.vanaksh.manomitra.ui.roles;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.vanaksh.manomitra.R;
import com.vanaksh.manomitra.data.model.Message;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView rvMessages;
    private MessageAdapter adapter;
    private List<Message> messages = new ArrayList<>();
    private EditText etMessage;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String chatId, otherUserId, otherUserName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        chatId = getIntent().getStringExtra("chatId");
        otherUserId = getIntent().getStringExtra("otherUserId");
        otherUserName = getIntent().getStringExtra("otherUserName");

        setupToolbar();
        initViews();
        listenForMessages();
    }

    private void setupToolbar() {
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(otherUserName != null ? otherUserName : "Chat");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }

    private void initViews() {
        rvMessages = findViewById(R.id.rvMessages);
        etMessage = findViewById(R.id.etMessage);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);

        adapter = new MessageAdapter(messages, mAuth.getUid());
        rvMessages.setAdapter(adapter);

        findViewById(R.id.btnSend).setOnClickListener(v -> sendMessage());
    }

    private void listenForMessages() {
        if (chatId == null)
            return;

        db.collection("messages").document(chatId).collection("history")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null)
                        return;
                    if (snapshots != null) {
                        messages.clear();
                        for (com.google.firebase.firestore.DocumentSnapshot doc : snapshots) {
                            Message msg = doc.toObject(Message.class);
                            if (msg != null)
                                messages.add(msg);
                        }
                        adapter.notifyDataSetChanged();
                        rvMessages.scrollToPosition(messages.size() - 1);
                    }
                });
    }

    private void sendMessage() {
        String content = etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(content))
            return;

        String senderId = mAuth.getUid();
        if (senderId == null || chatId == null || otherUserId == null)
            return;

        Message message = new Message(chatId, senderId, otherUserId, content);
        etMessage.setText("");

        db.collection("messages").document(chatId).collection("history")
                .add(message)
                .addOnSuccessListener(documentReference -> {
                    updateChatMetadata(content);
                });
    }

    private void updateChatMetadata(String lastMessage) {
        Map<String, Object> update = new HashMap<>();
        update.put("lastMessage", lastMessage);
        update.put("lastMessageTimestamp", Timestamp.now());
        update.put("unreadCount", FieldValue.increment(1));

        db.collection("messages").document(chatId).update(update);
    }
}