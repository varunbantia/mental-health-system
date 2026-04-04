package com.vanaksh.manomitra.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import com.vanaksh.manomitra.R;
import com.vanaksh.manomitra.ui.roles.StudentChatActivity;
import com.vanaksh.manomitra.ui.roles.CounsellorChatActivity;

public class LocalNotificationHelper {

    private static final String CHANNEL_ID = "chat_messages";
    private static final String CHANNEL_NAME = "Chat Messages";
    private static final int NOTIFICATION_ID = 101;

    public static void showChatNotification(Context context, String senderName, String messagePreview,
            String appointmentId, String otherUserId, boolean isStudent) {
        NotificationManager notificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        Class<?> activityClass = isStudent ? StudentChatActivity.class : CounsellorChatActivity.class;
        Intent intent = new Intent(context, activityClass);
        intent.putExtra("appointmentId", appointmentId);
        intent.putExtra(isStudent ? "counsellorId" : "studentId", otherUserId);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_chat) // Ensure this exists or use a fallback
                .setContentTitle("New message from " + senderName)
                .setContentText(messagePreview)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }
}
