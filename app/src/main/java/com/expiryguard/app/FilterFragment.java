package com.expiryguard.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomsheet.BottomSheetDialog;

/** Status tiles (Safe / Expiring soon / Expired) plus the Category and Sort buttons. */
public class FilterFragment extends Fragment {

    public interface Listener {
        void onFilterChanged();
    }

    public static final int SORT_SOONEST = 0, SORT_LATEST = 1, SORT_AZ = 2, SORT_ZA = 3;
    private static final int NO_STATUS = -1;

    private int statusFilter = NO_STATUS;
    private String category = null; // null = all categories
    private int sort = SORT_SOONEST;
    private int cSafe, cSoon, cExpired;

    private View tileSafe, tileSoon, tileExpired;
    private TextView tvCountSafe, tvCountSoon, tvCountExpired, tvCategoryText, tvSortText;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle state) {
        return inflater.inflate(R.layout.fragment_filter, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle state) {
        super.onViewCreated(v, state);
        if (state != null) {
            statusFilter = state.getInt("status", NO_STATUS);
            category = state.getString("category");
            sort = state.getInt("sort", SORT_SOONEST);
        }
        tileSafe = v.findViewById(R.id.tileSafe);
        tileSoon = v.findViewById(R.id.tileSoon);
        tileExpired = v.findViewById(R.id.tileExpired);
        tvCountSafe = v.findViewById(R.id.tvCountSafe);
        tvCountSoon = v.findViewById(R.id.tvCountSoon);
        tvCountExpired = v.findViewById(R.id.tvCountExpired);
        tvCategoryText = v.findViewById(R.id.tvCategoryText);
        tvSortText = v.findViewById(R.id.tvSortText);

        tileSafe.setOnClickListener(x -> toggle(ExpiryUtils.SAFE));
        tileSoon.setOnClickListener(x -> toggle(ExpiryUtils.SOON));
        tileExpired.setOnClickListener(x -> toggle(ExpiryUtils.EXPIRED));
        v.findViewById(R.id.btnCategory).setOnClickListener(x -> showCategorySheet());
        v.findViewById(R.id.btnSort).setOnClickListener(x -> showSortSheet());

        render();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle out) {
        super.onSaveInstanceState(out);
        out.putInt("status", statusFilter);
        out.putString("category", category);
        out.putInt("sort", sort);
    }

    public int getStatusFilter() { return statusFilter; }
    public String getCategory() { return category; }
    public int getSort() { return sort; }

    public void setCounts(int safe, int soon, int expired) {
        cSafe = safe;
        cSoon = soon;
        cExpired = expired;
        if (tvCountSafe != null) render();
    }

    private void toggle(int status) {
        statusFilter = (statusFilter == status) ? NO_STATUS : status;
        render();
        notifyChanged();
    }

    private void notifyChanged() {
        if (getActivity() instanceof Listener) ((Listener) getActivity()).onFilterChanged();
    }

    private void render() {
        tvCountSafe.setText(String.valueOf(cSafe));
        tvCountSoon.setText(String.valueOf(cSoon));
        tvCountExpired.setText(String.valueOf(cExpired));
        tileSafe.setSelected(statusFilter == ExpiryUtils.SAFE);
        tileSoon.setSelected(statusFilter == ExpiryUtils.SOON);
        tileExpired.setSelected(statusFilter == ExpiryUtils.EXPIRED);

        tvCategoryText.setText(category == null
                ? getString(R.string.category_all_button)
                : getString(R.string.category) + ": " + category);

        int shortRes;
        switch (sort) {
            case SORT_LATEST: shortRes = R.string.sort_latest_short; break;
            case SORT_AZ: shortRes = R.string.sort_az_short; break;
            case SORT_ZA: shortRes = R.string.sort_za_short; break;
            default: shortRes = R.string.sort_soonest_short;
        }
        tvSortText.setText(getString(R.string.sort_prefix, getString(shortRes)));
    }

    private void showSortSheet() {
        BottomSheetDialog d = new BottomSheetDialog(requireContext());
        View s = getLayoutInflater().inflate(R.layout.sheet_sort, null);
        int[] ids = {R.id.optSoon, R.id.optLate, R.id.optAz, R.id.optZa};
        for (int i = 0; i < ids.length; i++) {
            final int value = i;
            ViewGroup row = s.findViewById(ids[i]);
            row.getChildAt(1).setVisibility(i == sort ? View.VISIBLE : View.INVISIBLE);
            row.setOnClickListener(x -> {
                sort = value;
                d.dismiss();
                render();
                notifyChanged();
            });
        }
        d.setContentView(s);
        d.show();
    }

    private void showCategorySheet() {
        BottomSheetDialog d = new BottomSheetDialog(requireContext());
        View s = getLayoutInflater().inflate(R.layout.sheet_category, null);
        int[] ids = {R.id.optAll, R.id.optFood, R.id.optCosmetics, R.id.optMedication,
                R.id.optSkinCare, R.id.optOther};
        String[] values = {null, ExpiryUtils.FOOD, ExpiryUtils.COSMETICS,
                ExpiryUtils.MEDICATION, ExpiryUtils.SKIN_CARE, ExpiryUtils.OTHER};
        for (int i = 0; i < ids.length; i++) {
            final String value = values[i];
            ViewGroup row = s.findViewById(ids[i]);
            boolean selected = (value == null && category == null)
                    || (value != null && value.equals(category));
            row.getChildAt(1).setVisibility(selected ? View.VISIBLE : View.INVISIBLE);
            row.setOnClickListener(x -> {
                category = value;
                d.dismiss();
                render();
                notifyChanged();
            });
        }
        d.setContentView(s);
        d.show();
    }
}
