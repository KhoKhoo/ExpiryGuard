package com.expiryguard.app;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.android.material.card.MaterialCardView;

public class ProductDetailsActivity extends AppCompatActivity {
    public static final String EXTRA_PRODUCT_ID = "product_id";

    private DatabaseHelper db;
    private long productId;
    private Product product;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_details);
        db = new DatabaseHelper(this);
        productId = getIntent().getLongExtra(EXTRA_PRODUCT_ID, -1);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnEdit).setOnClickListener(v ->
                startActivity(new Intent(this, AddEditProductActivity.class)
                        .putExtra(AddEditProductActivity.EXTRA_PRODUCT_ID, productId)));
        findViewById(R.id.btnDelete).setOnClickListener(v -> {
            if (product == null) return;
            DialogUtils.showDelete(this, product, () -> {
                db.deleteProduct(product.id);
                PhotoUtils.delete(product.photoPath);
                Toast.makeText(this, R.string.msg_deleted, Toast.LENGTH_SHORT).show();
                finish();
            });
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        product = db.getProduct(productId); // reload: it may have been edited
        if (product == null) { finish(); return; }

        int days = ExpiryUtils.daysLeft(product);
        int status = ExpiryUtils.status(days, Prefs.getWarnDays(this));

        ((TextView) findViewById(R.id.tvName)).setText(product.name);
        TextView pill = findViewById(R.id.tvStatus);
        pill.setText(ExpiryUtils.statusLabel(this, status));
        pill.setBackgroundResource(ExpiryUtils.pillBackground(status));
        pill.setTextColor(ContextCompat.getColor(this, ExpiryUtils.pillTextColor(status)));

        ((TextView) findViewById(R.id.tvCountdown)).setText(ExpiryUtils.countdown(days));
        ((TextView) findViewById(R.id.tvCategory)).setText(product.category);
        ((TextView) findViewById(R.id.tvExpiryDate)).setText(ExpiryUtils.format(product.expiryDate));
        ((TextView) findViewById(R.id.tvPhotoState)).setText(
                product.hasPhoto() ? R.string.photo_attached : R.string.photo_none);

        MaterialCardView hero = findViewById(R.id.heroCard);
        ImageView iv = findViewById(R.id.ivHero);
        Bitmap bmp = product.hasPhoto() ? PhotoUtils.load(product.photoPath, 1024) : null;
        if (bmp != null) {
            iv.setPadding(0, 0, 0, 0);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.clearColorFilter();
            iv.setImageBitmap(bmp);
        } else {
            int pad = (int) (56 * getResources().getDisplayMetrics().density);
            iv.setPadding(pad, pad, pad, pad);
            iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
            iv.setImageResource(ExpiryUtils.categoryIcon(product.category));
            iv.setColorFilter(ContextCompat.getColor(this, ExpiryUtils.categoryFg(product.category)));
            hero.setCardBackgroundColor(ContextCompat.getColor(this, ExpiryUtils.categoryBgColor(product.category)));
        }
    }
}
