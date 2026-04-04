package com.vanaksh.manomitra.data.repository;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.vanaksh.manomitra.data.model.Booking;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BookingRepository {

    private static final String TAG = "BookingRepository";
    private static final String COLLECTION = "bookings";
    private final FirebaseFirestore db;

    public BookingRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    // --- SECURITY: Hash the UID so raw identity is never stored in booking records
    // ---
    public String hashUid(String uid) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(uid.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "SHA-256 not available, using UID prefix as fallback");
            return uid.substring(0, Math.min(uid.length(), 8));
        }
    }

    public String generateBookingId() {
        return "MNM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public String getCurrentUserRef() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null)
            return null;
        return hashUid(user.getUid());
    }

    public String getCurrentUid() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return (user != null) ? user.getUid() : null;
    }

    public interface BookingCallback {
        void onSuccess(Booking booking);

        void onError(String error);
    }

    public interface BookingListCallback {
        void onSuccess(List<Booking> bookings);

        void onError(String error);
    }

    public void createBooking(Booking booking, BookingCallback callback) {
        db.collection(COLLECTION)
                .document(booking.getBookingId())
                .set(booking)
                .addOnSuccessListener(aVoid -> callback.onSuccess(booking))
                .addOnFailureListener(e -> callback.onError("Could not create booking: " + e.getMessage()));
    }

    public void getBookingsByStudent(String studentId, BookingListCallback callback) {
        db.collection(COLLECTION)
                .whereEqualTo("studentId", studentId)
                .get()
                .addOnSuccessListener(snapshots -> {
                    List<Booking> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Booking b = doc.toObject(Booking.class);
                        if (b != null) {
                            b.setBookingId(doc.getId());
                            list.add(b);
                        }
                    }
                    callback.onSuccess(list);
                })
                .addOnFailureListener(e -> callback.onError("Could not load bookings: " + e.getMessage()));
    }

    public void getBookingsByUser(String userRef, BookingListCallback callback) {
        db.collection(COLLECTION)
                .whereEqualTo("userRef", userRef)
                .get()
                .addOnSuccessListener(snapshots -> {
                    List<Booking> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        list.add(doc.toObject(Booking.class));
                    }
                    callback.onSuccess(list);
                })
                .addOnFailureListener(e -> callback.onError("Could not load bookings: " + e.getMessage()));
    }

    public void getBookingById(String bookingId, BookingCallback callback) {
        db.collection(COLLECTION)
                .document(bookingId)
                .get()
                .addOnSuccessListener(doc -> {
                    Booking b = doc.toObject(Booking.class);
                    if (b != null) {
                        callback.onSuccess(b);
                    } else {
                        callback.onError("Booking not found");
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void cancelBooking(String bookingId, BookingCallback callback) {
        db.collection(COLLECTION)
                .document(bookingId)
                .update("status", Booking.STATUS_CANCELLED)
                .addOnSuccessListener(aVoid -> {
                    Booking b = new Booking();
                    b.setBookingId(bookingId);
                    b.setStatus(Booking.STATUS_CANCELLED);
                    callback.onSuccess(b);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }
}
