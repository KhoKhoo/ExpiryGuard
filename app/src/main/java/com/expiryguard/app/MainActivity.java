package com.expiryguard.app;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements FilterFragment.Listener, ProductAdapter.Listener {

    private DatabaseHelper db;
    private ProductAdapter adapter;
    private TextView tvEmpty;

    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (!granted) {
                    Toast.makeText(this, R.string.msg_permission_denied, Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        db = new DatabaseHelper(this);

        RecyclerView rv = findViewById(R.id.rvProducts);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ProductAdapter(this);
        rv.setAdapter(adapter);
        tvEmpty = findViewById(R.id.tvEmpty);

        findViewById(R.id.fabAdd).setOnClickListener(v ->
                startActivity(new Intent(this, AddEditProductActivity.class)));
        findViewById(R.id.btnSettings).setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));

        NotificationHelper.ensureChannel(this);
        NotificationHelper.scheduleDaily(this);
        if (savedInstanceState == null && Build.VERSION.SDK_INT >= 33
                && Prefs.isEnabled(this) && !NotificationHelper.hasPermission(this)) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    public void onFilterChanged() { refresh(); }

    private FilterFragment filter() {
        return (FilterFragment) getSupportFragmentManager().findFragmentById(R.id.filterFragment);
    }

    private void refresh() {
        FilterFragment f = filter();
        if (f == null) return;

        int warn = Prefs.getWarnDays(this);
        List<Product> all = db.getAllProducts();

        int safe = 0, soon = 0, expired = 0;
        for (Product p : all) {
            int s = ExpiryUtils.status(ExpiryUtils.daysLeft(p), warn);
            if (s == ExpiryUtils.SAFE) safe++;
            else if (s == ExpiryUtils.SOON) soon++;
            else expired++;
        }
        f.setCounts(safe, soon, expired);

        List<Product> shown = new ArrayList<>();
        for (Product p : all) {
            if (f.getStatusFilter() >= 0
                    && ExpiryUtils.status(ExpiryUtils.daysLeft(p), warn) != f.getStatusFilter()) continue;
            if (f.getCategory() != null && !f.getCategory().equals(p.category)) continue;
            shown.add(p);
        }

        Comparator<Product> byDate = (a, b) -> Integer.compare(ExpiryUtils.daysLeft(a), ExpiryUtils.daysLeft(b));
        Comparator<Product> byName = (a, b) -> a.name.compareToIgnoreCase(b.name);
        switch (f.getSort()) {
            case FilterFragment.SORT_LATEST: Collections.sort(shown, Collections.reverseOrder(byDate)); break;
            case FilterFragment.SORT_AZ: Collections.sort(shown, byName); break;
            case FilterFragment.SORT_ZA: Collections.sort(shown, Collections.reverseOrder(byName)); break;
            default: Collections.sort(shown, byDate);
        }

        adapter.submit(shown, warn);
        if (shown.isEmpty()) {
            tvEmpty.setText(all.isEmpty() ? R.string.empty_no_products : R.string.empty_no_match);
            tvEmpty.setVisibility(View.VISIBLE);
        } else {
            tvEmpty.setVisibility(View.GONE);
        }
    }

    @Override
    public void onEdit(Product p) {
        startActivity(new Intent(this, AddEditProductActivity.class)
                .putExtra(AddEditProductActivity.EXTRA_PRODUCT_ID, p.id));
    }

    @Override
    public void onDetails(Product p) {
        startActivity(new Intent(this, ProductDetailsActivity.class)
                .putExtra(ProductDetailsActivity.EXTRA_PRODUCT_ID, p.id));
    }

    @Override
    public void onDelete(Product p) {
        DialogUtils.showDelete(this, p, () -> {
            db.deleteProduct(p.id);
            PhotoUtils.delete(p.photoPath);
            Toast.makeText(this, R.string.msg_deleted, Toast.LENGTH_SHORT).show();
            refresh();
        });
    }

    @Override
    public void onPhoto(Product p) { DialogUtils.showPhoto(this, p); }
}
