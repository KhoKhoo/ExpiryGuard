package com.expiryguard.app;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

public final class NotificationHelper {
    public static final String CHANNEL_ID = "expiry_reminders";
    private static final int NOTIF_ID = 1001;
    private static final int ALARM_CODE = 2001;

    private NotificationHelper() { }

    public static void ensureChannel(Context c) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(CHANNEL_ID,
                    c.getString(R.string.channel_name), NotificationManager.IMPORTANCE_DEFAULT);
            ch.setDescription(c.getString(R.string.channel_desc));
            c.getSystemService(NotificationManager.class).createNotificationChannel(ch);
        }
    }

    public static boolean hasPermission(Context c) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || ContextCompat.checkSelfPermission(c, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Looks for expired / expiring-soon products and posts one notification.
     * @return number of products that need attention (0 = nothing posted)
     */
    public static int checkAndNotify(Context c) {
        List<Product> all = new DatabaseHelper(c).getAllProducts();
        int warn = Prefs.getWarnDays(c);
        List<Product> urgent = new ArrayList<>();
        for (Product p : all) {
            if (ExpiryUtils.daysLeft(p) <= warn) urgent.add(p);
        }
        if (urgent.isEmpty()) return 0;

        // Most urgent first (most overdue / soonest)
        Collections.sort(urgent, (a, b) -> Integer.compare(ExpiryUtils.daysLeft(a), ExpiryUtils.daysLeft(b)));

        if (!hasPermission(c)) return urgent.size();
        ensureChannel(c);

        int n = urgent.size();
        String title = n == 1 ? c.getString(R.string.notif_one) : c.getString(R.string.notif_many, n);

        NotificationCompat.InboxStyle style = new NotificationCompat.InboxStyle().setBigContentTitle(title);
        StringBuilder summary = new StringBuilder();
        int shown = Math.min(3, n);
        for (int i = 0; i < shown; i++) {
            Product p = urgent.get(i);
            String line = ExpiryUtils.notifLine(p, ExpiryUtils.daysLeft(p));
            style.addLine(line);
            if (i > 0) summary.append(", ");
            summary.append(line);
        }
        if (n > 3) style.addLine(c.getString(R.string.notif_more, n - 3));

        Intent open = new Intent(c, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(c, 0, open,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder b = new NotificationCompat.Builder(c, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(ExpiryUtils.notifLine(urgent.get(0), ExpiryUtils.daysLeft(urgent.get(0))))
                .setStyle(style)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        try {
            NotificationManagerCompat.from(c).notify(NOTIF_ID, b.build());
        } catch (SecurityException ignored) { }
        return n;
    }

    /** Schedules the next daily check at the chosen hour (rescheduled by the receiver each day). */
    public static void scheduleDaily(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi = alarmIntent(c);
        if (am == null) return;
        am.cancel(pi);
        if (!Prefs.isEnabled(c)) return;

        Calendar t = Calendar.getInstance();
        t.set(Calendar.HOUR_OF_DAY, Prefs.getHour(c));
        t.set(Calendar.MINUTE, 0);
        t.set(Calendar.SECOND, 0);
        t.set(Calendar.MILLISECOND, 0);
        if (t.getTimeInMillis() <= System.currentTimeMillis()) t.add(Calendar.DAY_OF_YEAR, 1);
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t.getTimeInMillis(), pi);
    }

    public static void cancelDaily(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am != null) am.cancel(alarmIntent(c));
    }

    private static PendingIntent alarmIntent(Context c) {
        Intent i = new Intent(c, ExpiryReceiver.class);
        return PendingIntent.getBroadcast(c, ALARM_CODE, i,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
