package com.vanaksh.manomitra.data.model;

import com.google.firebase.Timestamp;

public class Chat {
    private String appointmentId;
    private String studentId;
    private String counsellorId;
    private String lastMessage;
    private Timestamp lastTimestamp;

    public Chat() {
    }

    public Chat(String appointmentId, String studentId, String counsellorId) {
        this.appointmentId = appointmentId;
        this.studentId = studentId;
        this.counsellorId = counsellorId;
        this.lastTimestamp = Timestamp.now();
    }

    public String getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(String appointmentId) {
        this.appointmentId = appointmentId;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getCounsellorId() {
        return counsellorId;
    }

    public void setCounsellorId(String counsellorId) {
        this.counsellorId = counsellorId;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public Timestamp getLastTimestamp() {
        return lastTimestamp;
    }

    public void setLastTimestamp(Timestamp lastTimestamp) {
        this.lastTimestamp = lastTimestamp;
    }
}
