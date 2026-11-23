package com.expiryguard.app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.VH> {

    public interface Listener {
        void onEdit(Product p);
        void onDetails(Product p);
        void onDelete(Product p);
        void onPhoto(Product p);
    }

    private final List<Product> items = new ArrayList<>();
    private final Listener listener;
    private int warnDays = Prefs.DEFAULT_WARN;
    private long openId = -1; // only one card shows its options row at a time

    public ProductAdapter(Listener listener) { this.listener = listener; }

    public void submit(List<Product> list, int warnDays) {
        items.clear();
        items.addAll(list);
        this.warnDays = warnDays;
        boolean stillThere = false;
        for (Product p : items) if (p.id == openId) { stillThere = true; break; }
        if (!stillThere) openId = -1;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Context c = h.itemView.getContext();
        Product p = items.get(position);
        int days = ExpiryUtils.daysLeft(p);
        int status = ExpiryUtils.status(days, warnDays);

        h.tvName.setText(p.name);
        h.tvMeta.setText(p.category + " \u00B7 " + ExpiryUtils.format(p.expiryDate));

        h.tvStatus.setText(ExpiryUtils.pillText(days));
        h.tvStatus.setBackgroundResource(ExpiryUtils.pillBackground(status));
        h.tvStatus.setTextColor(ContextCompat.getColor(c, ExpiryUtils.pillTextColor(status)));

        h.btnCategoryIcon.setBackgroundResource(ExpiryUtils.categoryTintBg(p.category));
        h.ivCategory.setImageResource(ExpiryUtils.categoryIcon(p.category));
        h.ivCategory.setColorFilter(ContextCompat.getColor(c, ExpiryUtils.categoryFg(p.category)));
        h.ivPhotoBadge.setVisibility(p.hasPhoto() ? View.VISIBLE : View.GONE);
        h.btnCategoryIcon.setContentDescription(c.getString(R.string.view_photo_of, p.name));

        boolean open = p.id == openId;
        h.layoutOptions.setVisibility(open ? View.VISIBLE : View.GONE);
        h.cardRoot.setBackgroundResource(open ? R.drawable.bg_card_open : R.drawable.bg_card);

        h.rowMain.setOnClickListener(v -> {
            openId = (openId == p.id) ? -1 : p.id;
            notifyDataSetChanged();
        });
        h.btnCategoryIcon.setOnClickListener(v -> listener.onPhoto(p));
        h.btnEdit.setOnClickListener(v -> listener.onEdit(p));
        h.btnDetails.setOnClickListener(v -> listener.onDetails(p));
        h.btnDelete.setOnClickListener(v -> listener.onDelete(p));
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final View cardRoot, rowMain, btnCategoryIcon, layoutOptions, btnEdit, btnDetails, btnDelete;
        final TextView tvName, tvMeta, tvStatus;
        final ImageView ivCategory, ivPhotoBadge;

        VH(@NonNull View v) {
            super(v);
            cardRoot = v.findViewById(R.id.cardRoot);
            rowMain = v.findViewById(R.id.rowMain);
            btnCategoryIcon = v.findViewById(R.id.btnCategoryIcon);
            layoutOptions = v.findViewById(R.id.layoutOptions);
            btnEdit = v.findViewById(R.id.btnEdit);
            btnDetails = v.findViewById(R.id.btnDetails);
            btnDelete = v.findViewById(R.id.btnDelete);
            tvName = v.findViewById(R.id.tvName);
            tvMeta = v.findViewById(R.id.tvMeta);
            tvStatus = v.findViewById(R.id.tvStatus);
            ivCategory = v.findViewById(R.id.ivCategory);
            ivPhotoBadge = v.findViewById(R.id.ivPhotoBadge);
        }
    }
}
