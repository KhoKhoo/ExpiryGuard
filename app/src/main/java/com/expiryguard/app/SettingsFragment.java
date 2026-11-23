package com.expiryguard.app;

import android.Manifest;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.switchmaterial.SwitchMaterial;

public class SettingsFragment extends Fragment {
    private static final int FIRST_HOUR = 7; // matches the first entry of R.array.check_times

    private SwitchMaterial sw;
    private TextView tvStatus, tvDays;
    private ImageButton btnMinus, btnPlus;
    private boolean spinnerReady = false;
    private boolean runTestAfterPermission = false;

    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (!granted) {
                    toast(getString(R.string.msg_permission_denied));
                } else if (runTestAfterPermission) {
                    runTest();
                }
                runTestAfterPermission = false;
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle state) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle state) {
        super.onViewCreated(v, state);
        sw = v.findViewById(R.id.switchNotifications);
        tvStatus = v.findViewById(R.id.tvNotifStatus);
        tvDays = v.findViewById(R.id.tvDays);
        btnMinus = v.findViewById(R.id.btnDaysMinus);
        btnPlus = v.findViewById(R.id.btnDaysPlus);
        Spinner spinner = v.findViewById(R.id.spinnerTime);

        // switch
        sw.setChecked(Prefs.isEnabled(requireContext()));
        renderStatus();
        sw.setOnCheckedChangeListener((b, on) -> {
            Prefs.setEnabled(requireContext(), on);
            renderStatus();
            if (on) {
                NotificationHelper.scheduleDaily(requireContext());
                if (!NotificationHelper.hasPermission(requireContext())) requestPermission();
            } else {
                NotificationHelper.cancelDaily(requireContext());
            }
            toast(getString(on ? R.string.notifications_on : R.string.notifications_off));
        });

        // stepper
        renderDays();
        btnMinus.setOnClickListener(x -> changeDays(-1));
        btnPlus.setOnClickListener(x -> changeDays(+1));

        // time spinner
        ArrayAdapter<CharSequence> ad = ArrayAdapter.createFromResource(requireContext(),
                R.array.check_times, android.R.layout.simple_spinner_item);
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(ad);
        int idx = Math.max(0, Math.min(ad.getCount() - 1, Prefs.getHour(requireContext()) - FIRST_HOUR));
        spinner.setSelection(idx, false);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                if (!spinnerReady) { spinnerReady = true; return; } // skip the initial callback
                Prefs.setHour(requireContext(), FIRST_HOUR + pos);
                NotificationHelper.scheduleDaily(requireContext());
                toast(getString(R.string.msg_check_set, String.valueOf(parent.getItemAtPosition(pos))));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        v.findViewById(R.id.btnTestNotification).setOnClickListener(x -> runTest());
    }

    private void renderStatus() {
        boolean on = sw.isChecked();
        tvStatus.setText(on ? R.string.notifications_on : R.string.notifications_off);
        tvStatus.setTextColor(ContextCompat.getColor(requireContext(),
                on ? R.color.primary_dark : R.color.text_muted));
    }

    private void renderDays() {
        int d = Prefs.getWarnDays(requireContext());
        tvDays.setText(String.valueOf(d));
        btnMinus.setEnabled(d > Prefs.MIN_WARN);
        btnMinus.setAlpha(d > Prefs.MIN_WARN ? 1f : 0.4f);
        btnPlus.setEnabled(d < Prefs.MAX_WARN);
        btnPlus.setAlpha(d < Prefs.MAX_WARN ? 1f : 0.4f);
    }

    private void changeDays(int delta) {
        Prefs.setWarnDays(requireContext(), Prefs.getWarnDays(requireContext()) + delta);
        renderDays();
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= 33) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
    }

    private void runTest() {
        if (!Prefs.isEnabled(requireContext())) {
            toast(getString(R.string.notifications_off));
            return;
        }
        if (!NotificationHelper.hasPermission(requireContext())) {
            runTestAfterPermission = true;
            requestPermission();
            return;
        }
        int n = NotificationHelper.checkAndNotify(requireContext());
        if (n == 0) toast("No products need attention");
    }

    private void toast(String s) {
        Toast.makeText(requireContext(), s, Toast.LENGTH_SHORT).show();
    }
}
