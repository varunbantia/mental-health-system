package com.vanaksh.manomitra.data.model;

import com.google.firebase.Timestamp;
import java.util.List;

public class ChatSession {
    private String chatId;
    private List<String> participants; // [studentId, counsellorId]
    private String lastMessage;
    private Timestamp lastMessageTimestamp;
    private int unreadCount;
    private String studentName;
    private String counsellorName;
    private int studentUnreadCount;
    private int counsellorUnreadCount;
    private String id; // Field for chatbot history compatibility
    private Timestamp timestamp; // Field for chatbot history compatibility

    public ChatSession() {
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public List<String> getParticipants() {
        return participants;
    }

    public void setParticipants(List<String> participants) {
        this.participants = participants;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public Timestamp getLastMessageTimestamp() {
        return lastMessageTimestamp;
    }

    public void setLastMessageTimestamp(Timestamp lastMessageTimestamp) {
        this.lastMessageTimestamp = lastMessageTimestamp;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getCounsellorName() {
        return counsellorName;
    }

    public void setCounsellorName(String counsellorName) {
        this.counsellorName = counsellorName;
    }

    public int getStudentUnreadCount() {
        return studentUnreadCount;
    }

    public void setStudentUnreadCount(int studentUnreadCount) {
        this.studentUnreadCount = studentUnreadCount;
    }

    public int getCounsellorUnreadCount() {
        return counsellorUnreadCount;
    }

    public void setCounsellorUnreadCount(int counsellorUnreadCount) {
        this.counsellorUnreadCount = counsellorUnreadCount;
    }

    // Compatibility methods for Chatbot History
    public String getId() {
        return id != null ? id : chatId;
    }

    public void setId(String id) {
        this.id = id;
        if (this.chatId == null)
            this.chatId = id;
    }

    public java.util.Date getTimestamp() {
        if (timestamp != null)
            return timestamp.toDate();
        if (lastMessageTimestamp != null)
            return lastMessageTimestamp.toDate();
        return null;
    }

    public String getStudentId() {
        return (participants != null && participants.size() > 0) ? participants.get(0) : null;
    }

    public String getCounsellorId() {
        return (participants != null && participants.size() > 1) ? participants.get(1) : null;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
        if (this.lastMessageTimestamp == null)
            this.lastMessageTimestamp = timestamp;
    }
}
