package com.vanaksh.manomitra.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.vanaksh.manomitra.data.model.User;
import com.vanaksh.manomitra.utils.RoleManager;

public class AuthRepository {
    private final FirebaseAuth firebaseAuth;
    private final FirebaseFirestore firestore;

    public AuthRepository() {
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.firestore = FirebaseFirestore.getInstance();
    }

    public LiveData<String> signIn(String email, String password) {
        MutableLiveData<String> result = new MutableLiveData<>();
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    fetchUserRole(authResult.getUser().getUid(), result);
                })
                .addOnFailureListener(e -> {
                    result.setValue("ERROR: " + e.getMessage());
                });
        return result;
    }

    public LiveData<String> signUp(String email, String password, String name) {
        MutableLiveData<String> result = new MutableLiveData<>();
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser != null) {
                        User user = new User(firebaseUser.getUid(), name, email, RoleManager.ROLE_USER);
                        saveUserToFirestore(user, result);
                    }
                })
                .addOnFailureListener(e -> {
                    result.setValue("ERROR: " + e.getMessage());
                });
        return result;
    }

    private void saveUserToFirestore(User user, MutableLiveData<String> result) {
        firestore.collection("users").document(user.getUserId())
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    result.setValue("SUCCESS:" + RoleManager.ROLE_USER);
                })
                .addOnFailureListener(e -> {
                    result.setValue("ERROR: " + e.getMessage());
                });
    }

    public void fetchUserRole(String uid, MutableLiveData<String> result) {
        firestore.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String role = documentSnapshot.getString("role");
                        if (role == null)
                            role = RoleManager.ROLE_USER;
                        result.setValue("SUCCESS:" + role);
                    } else {
                        result.setValue("ERROR: User profile not found");
                    }
                })
                .addOnFailureListener(e -> {
                    result.setValue("ERROR: " + e.getMessage());
                });
    }

    public FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }

    public void logout() {
        firebaseAuth.signOut();
    }
}
