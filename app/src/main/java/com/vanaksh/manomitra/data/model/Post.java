package com.vanaksh.manomitra.data.model;

import com.google.firebase.firestore.PropertyName;
import java.io.Serializable;
import java.util.Map;

public class Post implements Serializable {
    private String postId;
    private String userId;
    private String title;
    private String description;
    private String category;
    private long timestamp;
    private String status; // "pending", "approved", "flagged", "escalated"
    private int supportCount;
    private int replyCount;
    private boolean anonymous;

    public Post() {
        // Default constructor required for Firestore
    }

    public Post(String postId, String userId, String title, String description, String category, long timestamp, String status, boolean anonymous) {
        this.postId = postId;
        this.userId = userId;
        this.title = title;
        this.description = description;
        this.category = category;
        this.timestamp = timestamp;
        this.status = status;
        this.anonymous = anonymous;
        this.supportCount = 0;
        this.replyCount = 0;
    }

    @PropertyName("postId")
    public String getPostId() { return postId; }
    @PropertyName("postId")
    public void setPostId(String postId) { this.postId = postId; }

    @PropertyName("userId")
    public String getUserId() { return userId; }
    @PropertyName("userId")
    public void setUserId(String userId) { this.userId = userId; }

    @PropertyName("title")
    public String getTitle() { return title; }
    @PropertyName("title")
    public void setTitle(String title) { this.title = title; }

    @PropertyName("description")
    public String getDescription() { return description; }
    @PropertyName("description")
    public void setDescription(String description) { this.description = description; }

    @PropertyName("category")
    public String getCategory() { return category; }
    @PropertyName("category")
    public void setCategory(String category) { this.category = category; }

    @PropertyName("timestamp")
    public long getTimestamp() { return timestamp; }
    @PropertyName("timestamp")
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    @PropertyName("status")
    public String getStatus() { return status; }
    @PropertyName("status")
    public void setStatus(String status) { this.status = status; }

    @PropertyName("supportCount")
    public int getSupportCount() { return supportCount; }
    @PropertyName("supportCount")
    public void setSupportCount(int supportCount) { this.supportCount = supportCount; }

    @PropertyName("replyCount")
    public int getReplyCount() { return replyCount; }
    @PropertyName("replyCount")
    public void setReplyCount(int replyCount) { this.replyCount = replyCount; }

    @PropertyName("anonymous")
    public boolean isAnonymous() { return anonymous; }
    @PropertyName("anonymous")
    public void setAnonymous(boolean anonymous) { this.anonymous = anonymous; }
}
