package com.example.travelapp;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

// Notifikacija "Vaš plan putovanja je spreman".
public final class NotificationHelper {

    private static final String CHANNEL_ID = "trip_plans";

    private NotificationHelper() {
    }

    // Od Androida 8 svaka notifikacija mora pripadati kanalu. Ponovno kreiranje istog kanala je bezbedno.
    private static void createChannel(Context context) {
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT);
        context.getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }

    public static void showPlanReady(Context context, long tripId, String destination) {
        createChannel(context);
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.notification_title))
                .setContentText(destination)
                .setContentIntent(createOpenTripIntent(context, tripId))
                .setAutoCancel(true);

        NotificationManagerCompat.from(context).notify((int) tripId, builder.build());
    }

    // PendingIntent dozvoljava sistemu da kasnije, na dodir, otvori MainActivity sa ID-jem putovanja.
    private static PendingIntent createOpenTripIntent(Context context, long tripId) {
        Intent intent = new Intent(context, MainActivity.class)
                .putExtra(MainActivity.EXTRA_TRIP_ID, tripId)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(context, (int) tripId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
