package com.vanaksh.manomitra.data.model;

import com.google.firebase.Timestamp;

public class CrisisCase {
    private String caseId;
    private String studentId;
    private String studentName;
    private String counsellorId;
    private String message;
    private String priorityLevel; // "High", "Critical"
    private Timestamp createdAt;
    private String status; // "open", "resolved"

    public CrisisCase() {
    }

    public CrisisCase(String caseId, String studentId, String studentName, String counsellorId, String message,
            String priorityLevel) {
        this.caseId = caseId;
        this.studentId = studentId;
        this.studentName = studentName;
        this.counsellorId = counsellorId;
        this.message = message;
        this.priorityLevel = priorityLevel;
        this.createdAt = Timestamp.now();
        this.status = "open";
    }

    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getCounsellorId() {
        return counsellorId;
    }

    public void setCounsellorId(String counsellorId) {
        this.counsellorId = counsellorId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPriorityLevel() {
        return priorityLevel;
    }

    public void setPriorityLevel(String priorityLevel) {
        this.priorityLevel = priorityLevel;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
