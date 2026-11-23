package com.expiryguard.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.util.UUID;

public final class PhotoUtils {
    private PhotoUtils() { }

    private static File dir(Context c) {
        File d = new File(c.getFilesDir(), "photos");
        if (!d.exists()) //noinspection ResultOfMethodCallIgnored
            d.mkdirs();
        return d;
    }

    public static File createImageFile(Context c) {
        return new File(dir(c), "img_" + UUID.randomUUID() + ".jpg");
    }

    /** Copies a picked gallery image into private storage so it stays available. */
    public static File copyToPrivate(Context c, Uri src) throws IOException {
        File out = createImageFile(c);
        try (InputStream in = c.getContentResolver().openInputStream(src);
             OutputStream os = new FileOutputStream(out)) {
            if (in == null) throw new IOException("No stream");
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) os.write(buf, 0, n);
        }
        return out;
    }

    public static void delete(String path) {
        if (path == null) return;
        File f = new File(path);
        if (f.exists()) //noinspection ResultOfMethodCallIgnored
            f.delete();
    }

    /** Loads a down-sampled, correctly rotated bitmap, or null if it cannot be read. */
    public static Bitmap load(String path, int maxPx) {
        if (path == null) return null;
        File f = new File(path);
        if (!f.exists() || f.length() == 0) return null;

        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, o);
        int sample = 1;
        while (o.outWidth / (sample * 2) >= maxPx && o.outHeight / (sample * 2) >= maxPx) sample *= 2;

        o = new BitmapFactory.Options();
        o.inSampleSize = sample;
        Bitmap bmp = BitmapFactory.decodeFile(path, o);
        if (bmp == null) return null;

        int degrees = 0;
        try {
            int orientation = new ExifInterface(path)
                    .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            if (orientation == ExifInterface.ORIENTATION_ROTATE_90) degrees = 90;
            else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) degrees = 180;
            else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) degrees = 270;
        } catch (IOException ignored) { }

        if (degrees != 0) {
            Matrix m = new Matrix();
            m.postRotate(degrees);
            Bitmap rotated = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), m, true);
            if (rotated != bmp) bmp.recycle();
            bmp = rotated;
        }
        return bmp;
    }
}
