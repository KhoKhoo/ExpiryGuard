package com.expiryguard.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "expiryguard.db";
    private static final int DB_VERSION = 2;
    private static final String TABLE = "products";

    public DatabaseHelper(Context context) {
        super(context.getApplicationContext(), DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "name TEXT NOT NULL, "
                + "category TEXT NOT NULL, "
                + "expiry_date TEXT NOT NULL, "
                + "photo_path TEXT)");
        seedIfEmpty(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("UPDATE " + TABLE + " SET category = 'Food & beverages' WHERE category = 'Food'");
            db.execSQL("UPDATE " + TABLE + " SET category = 'Medications' WHERE category = 'Medication'");
        }
        seedIfEmpty(db);
    }

    private void seedIfEmpty(SQLiteDatabase db) {
        try (Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + TABLE, null)) {
            if (c.moveToFirst() && c.getInt(0) > 0) return;
        }
        String[][] samples = {
                {"Fresh milk", ExpiryUtils.FOOD, "3"},
                {"Greek yogurt", ExpiryUtils.FOOD, "5"},
                {"Orange juice", ExpiryUtils.FOOD, "21"},
                {"Paracetamol", ExpiryUtils.MEDICATION, "120"},
                {"Vitamin C", ExpiryUtils.MEDICATION, "-10"},
                {"Face moisturizer", ExpiryUtils.SKIN_CARE, "60"},
                {"Mascara", ExpiryUtils.COSMETICS, "-30"},
        };
        LocalDate today = LocalDate.now();
        for (String[] s : samples) {
            ContentValues v = new ContentValues();
            v.put("name", s[0]);
            v.put("category", s[1]);
            v.put("expiry_date", ExpiryUtils.store(today.plusDays(Long.parseLong(s[2]))));
            db.insert(TABLE, null, v);
        }
    }

    private ContentValues values(Product p) {
        ContentValues v = new ContentValues();
        v.put("name", p.name);
        v.put("category", p.category);
        v.put("expiry_date", p.expiryDate);
        v.put("photo_path", p.photoPath);
        return v;
    }

    public long addProduct(Product p) {
        return getWritableDatabase().insert(TABLE, null, values(p));
    }

    public int updateProduct(Product p) {
        return getWritableDatabase().update(TABLE, values(p), "id = ?",
                new String[]{String.valueOf(p.id)});
    }

    public int deleteProduct(long id) {
        return getWritableDatabase().delete(TABLE, "id = ?", new String[]{String.valueOf(id)});
    }

    public Product getProduct(long id) {
        try (Cursor c = getReadableDatabase().query(TABLE, null, "id = ?",
                new String[]{String.valueOf(id)}, null, null, null)) {
            return c.moveToFirst() ? fromCursor(c) : null;
        }
    }

    public List<Product> getAllProducts() {
        List<Product> list = new ArrayList<>();
        try (Cursor c = getReadableDatabase().query(TABLE, null, null, null, null, null, "id ASC")) {
            while (c.moveToNext()) list.add(fromCursor(c));
        }
        return list;
    }

    private Product fromCursor(Cursor c) {
        return new Product(
                c.getLong(c.getColumnIndexOrThrow("id")),
                c.getString(c.getColumnIndexOrThrow("name")),
                c.getString(c.getColumnIndexOrThrow("category")),
                c.getString(c.getColumnIndexOrThrow("expiry_date")),
                c.getString(c.getColumnIndexOrThrow("photo_path")));
    }
}
