package com.vanaksh.manomitra.data.model;

import com.google.firebase.firestore.PropertyName;

import java.io.Serializable;

public class User implements Serializable {
    private String userId;
    private String name;
    private String email;
    private String role; // "user", "volunteer", "counsellor", "moderator", "admin"
    private String universityId;
    private String profileImage;
    private String phoneNumber;

    // Default constructor required for calls to DataSnapshot.getValue(User.class)
    public User() {
    }

    public User(String userId, String name, String email, String role) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.role = role;
    }

    @PropertyName("userId")
    public String getUserId() {
        return userId;
    }

    @PropertyName("userId")
    public void setUserId(String userId) {
        this.userId = userId;
    }

    @PropertyName("name")
    public String getName() {
        return name;
    }

    @PropertyName("name")
    public void setName(String name) {
        this.name = name;
    }

    @PropertyName("email")
    public String getEmail() {
        return email;
    }

    @PropertyName("email")
    public void setEmail(String email) {
        this.email = email;
    }

    @PropertyName("role")
    public String getRole() {
        return role;
    }

    @PropertyName("role")
    public void setRole(String role) {
        this.role = role;
    }

    @PropertyName("universityId")
    public String getUniversityId() {
        return universityId;
    }

    @PropertyName("universityId")
    public void setUniversityId(String universityId) {
        this.universityId = universityId;
    }

    @PropertyName("profileImage")
    public String getProfileImage() {
        return profileImage;
    }

    @PropertyName("profileImage")
    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    @PropertyName("phoneNumber")
    public String getPhoneNumber() {
        return phoneNumber;
    }

    @PropertyName("phoneNumber")
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}
