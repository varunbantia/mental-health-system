package com.vanaksh.manomitra.ui.roles;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.vanaksh.manomitra.R;
import com.vanaksh.manomitra.data.model.Booking;
import com.vanaksh.manomitra.data.model.Chat;
import com.vanaksh.manomitra.data.model.Counsellor;
import com.vanaksh.manomitra.data.model.Message;
import com.vanaksh.manomitra.util.LocalNotificationHelper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudentChatActivity extends AppCompatActivity {

    public static boolean isVisible = false;
    private RecyclerView rvMessages;
    private MessageAdapter adapter;
    private List<Message> messages = new ArrayList<>();
    private EditText etMessage;
    private TextView tvName, tvCategory;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String appointmentId, counsellorId, studentId;
    private String counsellorName;
    private ListenerRegistration messageListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        studentId = mAuth.getUid();

        appointmentId = getIntent().getStringExtra("appointmentId");
        counsellorId = getIntent().getStringExtra("counsellorId");

        if (appointmentId == null || counsellorId == null) {
            Toast.makeText(this, "Invalid chat session", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        fetchCounsellorInfo();
        verifyAppointmentStatus();
        listenForMessages();
    }

    @Override
    protected void onStart() {
        super.onStart();
        isVisible = true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        isVisible = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messageListener != null)
            messageListener.remove();
    }

    private void initViews() {
        rvMessages = findViewById(R.id.rvMessages);
        etMessage = findViewById(R.id.etMessage);

        // Toolbar setup
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        tvName = findViewById(R.id.tvChatName);
        tvCategory = findViewById(R.id.tvChatSubtitle);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);

        adapter = new MessageAdapter(messages, studentId);
        rvMessages.setAdapter(adapter);

        findViewById(R.id.btnSend).setOnClickListener(v -> sendMessage());
    }

    private void fetchCounsellorInfo() {
        db.collection("counsellors").document(counsellorId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Counsellor c = doc.toObject(Counsellor.class);
                        if (c != null) {
                            counsellorName = c.getName();
                            updateToolbarUI(c);
                        }
                    }
                });
    }

    private void updateToolbarUI(Counsellor c) {
        // Since we are reusing activity_chat, we might need to find these views
        // if they were added, or use the default title if not.
        if (tvName != null)
            tvName.setText(c.getName());
        else if (getSupportActionBar() != null)
            getSupportActionBar().setTitle(c.getName());

        if (tvCategory != null)
            tvCategory.setText(c.getCategory());
    }

    private void verifyAppointmentStatus() {
        db.collection("bookings").document(appointmentId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Booking b = doc.toObject(Booking.class);
                        if (b != null && !Booking.STATUS_CONFIRMED.equals(b.getStatus())) {
                            Toast.makeText(this, "Chat is only available for accepted sessions", Toast.LENGTH_LONG)
                                    .show();
                            finish();
                        }
                    }
                });
    }

    private void listenForMessages() {
        messageListener = db.collection("messages").document(appointmentId).collection("history")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null)
                        return;
                    if (snapshots != null) {
                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            if (dc.getType() == DocumentChange.Type.ADDED) {
                                Message msg = dc.getDocument().toObject(Message.class);
                                messages.add(msg);

                                // Local notification if app backgrounded or activity not visible
                                if (!isVisible && !msg.getSenderId().equals(studentId)) {
                                    LocalNotificationHelper.showChatNotification(this,
                                            counsellorName != null ? counsellorName : "Counsellor",
                                            msg.getMessage(), appointmentId, counsellorId, true);
                                }

                                // Mark as read if receiving from other person while visible
                                if (isVisible && !msg.getSenderId().equals(studentId) && !msg.isRead()) {
                                    markMessageAsRead(dc.getDocument().getId());
                                }
                            }
                        }
                        adapter.notifyDataSetChanged();
                        rvMessages.scrollToPosition(messages.size() - 1);
                        resetUnreadCount();
                    }
                });
    }

    private void resetUnreadCount() {
        db.collection("messages").document(appointmentId)
                .update("studentUnreadCount", 0);
    }

    private void markMessageAsRead(String messageId) {
        db.collection("messages").document(appointmentId).collection("history").document(messageId).update("isRead",
                true);
    }

    private void sendMessage() {
        String content = etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(content))
            return;

        etMessage.setText("");
        Message msg = new Message(appointmentId, studentId, counsellorId, content);

        // Ensure participants metadata exists for security rules
        List<String> participants = new ArrayList<>();
        participants.add(studentId);
        participants.add(counsellorId);

        Map<String, Object> chatMeta = new HashMap<>();
        chatMeta.put("participants", participants);
        chatMeta.put("lastMessage", content);
        chatMeta.put("lastTimestamp", Timestamp.now());
        chatMeta.put("counsellorUnreadCount", FieldValue.increment(1));

        db.collection("messages").document(appointmentId).set(chatMeta,
                com.google.firebase.firestore.SetOptions.merge());

        db.collection("messages").document(appointmentId).collection("history").add(msg);
    }
}
