package com.vanaksh.manomitra.data.model;

import com.google.firebase.firestore.PropertyName;
import java.io.Serializable;

public class Report implements Serializable {
    private String reportId;
    private String postId;
    private String reportedBy;
    private String reason;
    private long timestamp;

    public Report() {
        // Default constructor required for Firestore
    }

    public Report(String reportId, String postId, String reportedBy, String reason, long timestamp) {
        this.reportId = reportId;
        this.postId = postId;
        this.reportedBy = reportedBy;
        this.reason = reason;
        this.timestamp = timestamp;
    }

    @PropertyName("reportId")
    public String getReportId() { return reportId; }
    @PropertyName("reportId")
    public void setReportId(String reportId) { this.reportId = reportId; }

    @PropertyName("postId")
    public String getPostId() { return postId; }
    @PropertyName("postId")
    public void setPostId(String postId) { this.postId = postId; }

    @PropertyName("reportedBy")
    public String getReportedBy() { return reportedBy; }
    @PropertyName("reportedBy")
    public void setReportedBy(String reportedBy) { this.reportedBy = reportedBy; }

    @PropertyName("reason")
    public String getReason() { return reason; }
    @PropertyName("reason")
    public void setReason(String reason) { this.reason = reason; }

    @PropertyName("timestamp")
    public long getTimestamp() { return timestamp; }
    @PropertyName("timestamp")
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
