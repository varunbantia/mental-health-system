package com.vanaksh.manomitra.data.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.ServerTimestamp;

/**
 * Firestore-compatible POJO for a confidential booking.
 * No personally identifiable information is stored — only a hashed UID
 * reference.
 */
public class Booking {

    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_CONFIRMED = "confirmed";
    public static final String STATUS_CANCELLED = "cancelled";

    private String bookingId;
    private String userRef; // SHA-256 hash of Firebase UID
    private String bookingType; // "counselor" or "helpline"
    private String counsellorId; // New field
    private String studentId;
    private String studentName;
    private String concernCategory; // optional
    private String preferredDate; // yyyy-MM-dd
    private String preferredTime; // e.g. "10:00 AM"
    private String status; // pending | confirmed | cancelled
    private boolean crisisFlag;
    private Timestamp lastActivity;

    @ServerTimestamp
    private Timestamp createdAt;

    public Booking() {
        // Required empty constructor for Firestore deserialization
    }

    public Booking(String bookingId, String userRef, String bookingType,
            String counsellorId, String concernCategory, String preferredDate,
            String preferredTime, String status) {
        this.bookingId = bookingId;
        this.userRef = userRef;
        this.bookingType = bookingType;
        this.counsellorId = counsellorId;
        this.concernCategory = concernCategory;
        this.preferredDate = preferredDate;
        this.preferredTime = preferredTime;
        this.status = status;
    }

    // --- Getters ---
    public String getBookingId() {
        return bookingId;
    }

    public String getUserRef() {
        return userRef;
    }

    public String getBookingType() {
        return bookingType;
    }

    public String getCounsellorId() {
        return counsellorId;
    }

    public String getStudentId() {
        return studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public String getConcernCategory() {
        return concernCategory;
    }

    public String getPreferredDate() {
        return preferredDate;
    }

    public String getPreferredTime() {
        return preferredTime;
    }

    public String getStatus() {
        return status;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public boolean isCrisisFlag() {
        return crisisFlag;
    }

    public Timestamp getLastActivity() {
        return lastActivity;
    }

    // --- Setters ---
    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    public void setUserRef(String userRef) {
        this.userRef = userRef;
    }

    public void setBookingType(String bookingType) {
        this.bookingType = bookingType;
    }

    public void setCounsellorId(String counsellorId) {
        this.counsellorId = counsellorId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public void setConcernCategory(String concernCategory) {
        this.concernCategory = concernCategory;
    }

    public void setPreferredDate(String preferredDate) {
        this.preferredDate = preferredDate;
    }

    public void setPreferredTime(String preferredTime) {
        this.preferredTime = preferredTime;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public void setCrisisFlag(boolean crisisFlag) {
        this.crisisFlag = crisisFlag;
    }

    public void setLastActivity(Timestamp lastActivity) {
        this.lastActivity = lastActivity;
    }
}
