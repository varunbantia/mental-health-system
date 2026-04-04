package com.vanaksh.manomitra.booking;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.vanaksh.manomitra.R;

/**
 * Local notification helper for the booking system.
 * Uses only Android's built-in NotificationManager — no FCM.
 * All notification text is deliberately neutral to protect user privacy.
 */
public class BookingNotificationHelper {

    private static final String CHANNEL_ID = "manomitra_booking_channel";
    private static final String CHANNEL_NAME = "Appointment Updates";
    private static final String CHANNEL_DESC = "Updates about your appointment requests";

    private final Context context;

    public BookingNotificationHelper(Context context) {
        this.context = context.getApplicationContext();
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(CHANNEL_DESC);
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    /** Notify when a booking is successfully created. */
    public void notifyBookingCreated(String bookingId) {
        // PRIVACY: No sensitive details in notification text
        sendNotification(
                bookingId.hashCode(),
                "Appointment Requested",
                "Your appointment request has been submitted successfully.",
                bookingId);
    }

    /** Notify when booking status changes (confirmed / cancelled). */
    public void notifyBookingStatusChanged(String bookingId) {
        sendNotification(
                bookingId.hashCode() + 1,
                "Appointment Update",
                "Your appointment request has been updated.",
                bookingId);
    }

    private void sendNotification(int notificationId, String title, String text, String bookingId) {
        Intent intent = new Intent(context, BookingStatusActivity.class);
        intent.putExtra("BOOKING_ID", bookingId);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, notificationId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build());
        } catch (SecurityException e) {
            // POST_NOTIFICATIONS permission not granted on Android 13+; fail silently
        }
    }
}
