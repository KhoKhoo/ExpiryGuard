package com.expiryguard.app;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class DialogUtils {
    private DialogUtils() { }

    private static Dialog base(Activity a, int layout) {
        Dialog d = new Dialog(a);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        d.setContentView(layout);
        Window w = d.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int margin = (int) (24 * a.getResources().getDisplayMetrics().density);
            int width = a.getResources().getDisplayMetrics().widthPixels - 2 * margin;
            w.setLayout(Math.min(width, (int) (420 * a.getResources().getDisplayMetrics().density)),
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        return d;
    }

    public static void showDelete(Activity a, Product p, Runnable onConfirm) {
        Dialog d = base(a, R.layout.dialog_delete);
        ((TextView) d.findViewById(R.id.tvDeleteMessage))
                .setText(a.getString(R.string.delete_message, p.name));
        d.findViewById(R.id.btnCancel).setOnClickListener(v -> d.dismiss());
        d.findViewById(R.id.btnConfirmDelete).setOnClickListener(v -> {
            d.dismiss();
            onConfirm.run();
        });
        d.show();
    }

    public static void showPhoto(Activity a, Product p) {
        Dialog d = base(a, R.layout.dialog_photo);
        ((TextView) d.findViewById(R.id.tvPhotoTitle)).setText(p.name);
        ImageView big = d.findViewById(R.id.ivPhotoLarge);
        LinearLayout none = d.findViewById(R.id.noPhotoGroup);
        ImageView icon = d.findViewById(R.id.ivPhotoIcon);

        Bitmap bmp = p.hasPhoto() ? PhotoUtils.load(p.photoPath, 1024) : null;
        if (bmp != null) {
            big.setImageBitmap(bmp);
            big.setVisibility(View.VISIBLE);
            none.setVisibility(View.GONE);
        } else {
            big.setVisibility(View.GONE);
            none.setVisibility(View.VISIBLE);
            icon.setImageResource(ExpiryUtils.categoryIcon(p.category));
        }
        d.findViewById(R.id.btnClose).setOnClickListener(v -> d.dismiss());
        d.show();
    }
}
