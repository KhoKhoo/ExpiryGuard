package com.expiryguard.app;

import android.content.Context;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

public final class ExpiryUtils {
    public static final int SAFE = 0;
    public static final int SOON = 1;
    public static final int EXPIRED = 2;

    public static final String FOOD = "Food & beverages";
    public static final String COSMETICS = "Cosmetics";
    public static final String MEDICATION = "Medications";
    public static final String SKIN_CARE = "Skin care";
    public static final String OTHER = "Other";
    public static final String[] CATEGORIES = {FOOD, COSMETICS, MEDICATION, SKIN_CARE, OTHER};

    private static final DateTimeFormatter STORE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter SHOW =
            DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);

    private ExpiryUtils() { }

    public static LocalDate parse(String stored) { return LocalDate.parse(stored, STORE); }
    public static String store(LocalDate d) { return d.format(STORE); }
    public static String format(String stored) { return parse(stored).format(SHOW); }
    public static String format(LocalDate d) { return d.format(SHOW); }

    /** Expiry date minus today. Negative = already expired. */
    public static int daysLeft(Product p) {
        return (int) ChronoUnit.DAYS.between(LocalDate.now(), parse(p.expiryDate));
    }

    public static int status(int days, int warnDays) {
        if (days < 0) return EXPIRED;
        if (days <= warnDays) return SOON;
        return SAFE;
    }

    private static String plural(int n) { return n + (n == 1 ? " day" : " days"); }

    /** Text shown in the pill on the Home card. */
    public static String pillText(int days) {
        if (days < 0) return "Expired " + plural(-days) + " ago";
        if (days == 0) return "Today";
        return plural(days);
    }

    /** Large text on the Details screen. */
    public static String countdown(int days) {
        if (days < 0) return "Expired " + plural(-days) + " ago";
        if (days == 0) return "Expires today";
        return plural(days) + " left";
    }

    /** Text for one notification line. */
    public static String notifLine(Product p, int days) {
        if (days < 0) return p.name + " - expired " + plural(-days) + " ago";
        if (days == 0) return p.name + " - expires today";
        return p.name + " - expires in " + plural(days);
    }

    public static String statusLabel(Context c, int status) {
        switch (status) {
            case EXPIRED: return c.getString(R.string.expired);
            case SOON: return c.getString(R.string.expiring_soon);
            default: return c.getString(R.string.safe);
        }
    }

    public static int pillBackground(int status) {
        switch (status) {
            case EXPIRED: return R.drawable.bg_pill_expired;
            case SOON: return R.drawable.bg_pill_soon;
            default: return R.drawable.bg_pill_safe;
        }
    }

    public static int pillTextColor(int status) {
        switch (status) {
            case EXPIRED: return R.color.expired_text;
            case SOON: return R.color.soon_text;
            default: return R.color.safe_text;
        }
    }

    public static int categoryIcon(String cat) {
        if (MEDICATION.equals(cat)) return R.drawable.ic_medication;
        if (COSMETICS.equals(cat)) return R.drawable.ic_cosmetics;
        if (SKIN_CARE.equals(cat)) return R.drawable.ic_skincare;
        if (OTHER.equals(cat)) return R.drawable.ic_other;
        return R.drawable.ic_food;
    }

    public static int categoryTintBg(String cat) {
        if (MEDICATION.equals(cat)) return R.drawable.bg_tint_med;
        if (COSMETICS.equals(cat)) return R.drawable.bg_tint_cos;
        if (SKIN_CARE.equals(cat)) return R.drawable.bg_tint_skin;
        if (OTHER.equals(cat)) return R.drawable.bg_tint_other;
        return R.drawable.bg_tint_food;
    }

    public static int categoryFg(String cat) {
        if (MEDICATION.equals(cat)) return R.color.med_fg;
        if (COSMETICS.equals(cat)) return R.color.cos_fg;
        if (SKIN_CARE.equals(cat)) return R.color.skin_fg;
        if (OTHER.equals(cat)) return R.color.other_fg;
        return R.color.food_fg;
    }

    public static int categoryBgColor(String cat) {
        if (MEDICATION.equals(cat)) return R.color.med_bg;
        if (COSMETICS.equals(cat)) return R.color.cos_bg;
        if (SKIN_CARE.equals(cat)) return R.color.skin_bg;
        if (OTHER.equals(cat)) return R.color.other_bg;
        return R.color.food_bg;
    }
}
