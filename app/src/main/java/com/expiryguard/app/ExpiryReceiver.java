package com.expiryguard.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ExpiryReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        boolean boot = intent != null && Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction());
        if (!boot && Prefs.isEnabled(context)) {
            NotificationHelper.checkAndNotify(context);
        }
        NotificationHelper.scheduleDaily(context);
    }
}
