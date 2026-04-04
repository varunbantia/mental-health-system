package com.vanaksh.manomitra.data.model;

import com.google.firebase.firestore.PropertyName;
import java.io.Serializable;

public class Reply implements Serializable {
    private String replyId;
    private String postId;
    private String userId;
    private String message;
    private long timestamp;
    private String role; // "user", "volunteer", "moderator"
    private String userName; // Name of the user (or "Anonymous")
    private String parentReplyId; // ID of the parent reply (null if top-level)
    private String parentUserName; // Name of the user being replied to
    private int supportCount;

    public Reply() {
        // Default constructor required for Firestore
    }

    public Reply(String replyId, String postId, String userId, String message, long timestamp, String role, String userName, String parentReplyId, String parentUserName) {
        this.replyId = replyId;
        this.postId = postId;
        this.userId = userId;
        this.message = message;
        this.timestamp = timestamp;
        this.role = role;
        this.userName = userName;
        this.parentReplyId = parentReplyId;
        this.parentUserName = parentUserName;
        this.supportCount = 0;
    }

    @PropertyName("replyId")
    public String getReplyId() { return replyId; }
    @PropertyName("replyId")
    public void setReplyId(String replyId) { this.replyId = replyId; }

    @PropertyName("postId")
    public String getPostId() { return postId; }
    @PropertyName("postId")
    public void setPostId(String postId) { this.postId = postId; }

    @PropertyName("userId")
    public String getUserId() { return userId; }
    @PropertyName("userId")
    public void setUserId(String userId) { this.userId = userId; }

    @PropertyName("message")
    public String getMessage() { return message; }
    @PropertyName("message")
    public void setMessage(String message) { this.message = message; }

    @PropertyName("timestamp")
    public long getTimestamp() { return timestamp; }
    @PropertyName("timestamp")
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    @PropertyName("role")
    public String getRole() { return role; }
    @PropertyName("role")
    public void setRole(String role) { this.role = role; }

    @PropertyName("userName")
    public String getUserName() { return userName; }
    @PropertyName("userName")
    public void setUserName(String userName) { this.userName = userName; }

    @PropertyName("parentReplyId")
    public String getParentReplyId() { return parentReplyId; }
    @PropertyName("parentReplyId")
    public void setParentReplyId(String parentReplyId) { this.parentReplyId = parentReplyId; }

    @PropertyName("parentUserName")
    public String getParentUserName() { return parentUserName; }
    @PropertyName("parentUserName")
    public void setParentUserName(String parentUserName) { this.parentUserName = parentUserName; }

    @PropertyName("supportCount")
    public int getSupportCount() { return supportCount; }
    @PropertyName("supportCount")
    public void setSupportCount(int supportCount) { this.supportCount = supportCount; }
}
