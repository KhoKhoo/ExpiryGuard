package com.expiryguard.app;

import android.content.Context;
import android.content.SharedPreferences;

public final class Prefs {
    private static final String FILE = "expiryguard_prefs";
    private static final String K_ENABLED = "notifications_enabled";
    private static final String K_WARN = "warn_days";
    private static final String K_HOUR = "check_hour";

    public static final int DEFAULT_WARN = 7;
    public static final int MIN_WARN = 1;
    public static final int MAX_WARN = 30;
    public static final int DEFAULT_HOUR = 9;

    private Prefs() { }

    private static SharedPreferences sp(Context c) {
        return c.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public static boolean isEnabled(Context c) { return sp(c).getBoolean(K_ENABLED, true); }
    public static void setEnabled(Context c, boolean v) { sp(c).edit().putBoolean(K_ENABLED, v).apply(); }

    public static int getWarnDays(Context c) { return sp(c).getInt(K_WARN, DEFAULT_WARN); }
    public static void setWarnDays(Context c, int d) {
        sp(c).edit().putInt(K_WARN, Math.max(MIN_WARN, Math.min(MAX_WARN, d))).apply();
    }

    public static int getHour(Context c) { return sp(c).getInt(K_HOUR, DEFAULT_HOUR); }
    public static void setHour(Context c, int h) { sp(c).edit().putInt(K_HOUR, h).apply(); }
}
