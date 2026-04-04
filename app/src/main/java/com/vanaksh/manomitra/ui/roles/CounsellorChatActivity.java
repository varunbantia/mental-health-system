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
import com.vanaksh.manomitra.data.model.Message;
import com.vanaksh.manomitra.util.LocalNotificationHelper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CounsellorChatActivity extends AppCompatActivity {

    public static boolean isVisible = false;
    private RecyclerView rvMessages;
    private MessageAdapter adapter;
    private List<Message> messages = new ArrayList<>();
    private EditText etMessage;
    private TextView tvName, tvCategory;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String appointmentId, studentId, counsellorId;
    private String studentName;
    private ListenerRegistration messageListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        counsellorId = mAuth.getUid();

        appointmentId = getIntent().getStringExtra("appointmentId");
        studentId = getIntent().getStringExtra("studentId"); // Pass from previous screen

        if (appointmentId == null || studentId == null) {
            Toast.makeText(this, "Something went wrong", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        verifyAccessAndCounsellorData();
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

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);

        adapter = new MessageAdapter(messages, counsellorId);
        rvMessages.setAdapter(adapter);

        tvName = findViewById(R.id.tvChatName);
        tvCategory = findViewById(R.id.tvChatSubtitle);
        if (tvCategory != null)
            tvCategory.setVisibility(View.GONE); // No category for student usually

        findViewById(R.id.btnSend).setOnClickListener(v -> sendMessage());
    }

    private void verifyAccessAndCounsellorData() {
        db.collection("bookings").document(appointmentId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Booking b = doc.toObject(Booking.class);
                        if (b != null) {
                            if (!counsellorId.equals(b.getCounsellorId())
                                    || !Booking.STATUS_CONFIRMED.equals(b.getStatus())) {
                                Toast.makeText(this, "Access denied", Toast.LENGTH_SHORT).show();
                                finish();
                            } else {
                                studentName = b.getStudentName();
                                if (tvName != null) {
                                    tvName.setText(studentName != null ? studentName : "Student");
                                } else if (getSupportActionBar() != null) {
                                    getSupportActionBar().setTitle(studentName != null ? studentName : "Student");
                                }
                            }
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

                                if (!isVisible && !msg.getSenderId().equals(counsellorId)) {
                                    LocalNotificationHelper.showChatNotification(this,
                                            studentName != null ? studentName : "Student",
                                            msg.getMessage(), appointmentId, studentId, false);
                                }

                                if (isVisible && !msg.getSenderId().equals(counsellorId) && !msg.isRead()) {
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
                .update("counsellorUnreadCount", 0);
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
        Message msg = new Message(appointmentId, counsellorId, studentId, content);

        // Ensure participants metadata exists for security rules
        List<String> participants = new ArrayList<>();
        participants.add(studentId);
        participants.add(counsellorId);

        Map<String, Object> chatMeta = new HashMap<>();
        chatMeta.put("participants", participants);
        chatMeta.put("lastMessage", content);
        chatMeta.put("lastTimestamp", Timestamp.now());
        chatMeta.put("studentUnreadCount", FieldValue.increment(1));

        db.collection("messages").document(appointmentId).set(chatMeta,
                com.google.firebase.firestore.SetOptions.merge());

        db.collection("messages").document(appointmentId).collection("history").add(msg);
    }
}
