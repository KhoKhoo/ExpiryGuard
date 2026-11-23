package com.expiryguard.app;

import android.app.DatePickerDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.android.material.chip.ChipGroup;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;

public class AddEditProductActivity extends AppCompatActivity {
    public static final String EXTRA_PRODUCT_ID = "product_id";

    private DatabaseHelper db;
    private Product editing;          // null in Add mode
    private String originalPhoto;     // photo already saved with the product
    private String currentPhoto;      // photo currently shown (may be new, unsaved)
    private File pendingCameraFile;
    private LocalDate selectedDate;
    private boolean saved = false;

    private EditText etName, etDate;
    private TextView tvNameError, tvDateError, tvNoPhoto;
    private ImageView ivPhoto;
    private ChipGroup chipGroup;

    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), r -> {
                if (r.getResultCode() == RESULT_OK && pendingCameraFile != null
                        && pendingCameraFile.length() > 0) {
                    setPhoto(pendingCameraFile.getAbsolutePath());
                } else if (pendingCameraFile != null) {
                    PhotoUtils.delete(pendingCameraFile.getAbsolutePath());
                }
                pendingCameraFile = null;
            });

    private final ActivityResultLauncher<Intent> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), r -> {
                Uri uri = r.getData() != null ? r.getData().getData() : null;
                if (r.getResultCode() != RESULT_OK || uri == null) return;
                try {
                    setPhoto(PhotoUtils.copyToPrivate(this, uri).getAbsolutePath());
                } catch (IOException e) {
                    Toast.makeText(this, R.string.msg_photo_failed, Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_product);
        db = new DatabaseHelper(this);

        etName = findViewById(R.id.etName);
        etDate = findViewById(R.id.etDate);
        tvNameError = findViewById(R.id.tvNameError);
        tvDateError = findViewById(R.id.tvDateError);
        tvNoPhoto = findViewById(R.id.tvNoPhoto);
        ivPhoto = findViewById(R.id.ivPhoto);
        chipGroup = findViewById(R.id.chipGroup);
        tvNameError.setVisibility(View.GONE);
        tvDateError.setVisibility(View.GONE);

        long id = getIntent().getLongExtra(EXTRA_PRODUCT_ID, -1);
        if (id >= 0) {
            editing = db.getProduct(id);
            if (editing == null) { finish(); return; }
            ((TextView) findViewById(R.id.tvTitle)).setText(R.string.title_edit);
            originalPhoto = editing.photoPath;
            currentPhoto = editing.photoPath;
            etName.setText(editing.name);
            selectedDate = ExpiryUtils.parse(editing.expiryDate);
            checkCategory(editing.category);
        }

        if (savedInstanceState != null) {
            etName.setText(savedInstanceState.getString("name", etName.getText().toString()));
            String d = savedInstanceState.getString("date");
            if (d != null) selectedDate = ExpiryUtils.parse(d);
            currentPhoto = savedInstanceState.getString("photo", currentPhoto);
            String cam = savedInstanceState.getString("camera");
            if (cam != null) pendingCameraFile = new File(cam);
            String cat = savedInstanceState.getString("category");
            if (cat != null) checkCategory(cat);
        }

        showDate();
        showPhoto();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnCancel).setOnClickListener(v -> finish());
        findViewById(R.id.btnSave).setOnClickListener(v -> save());
        findViewById(R.id.btnCamera).setOnClickListener(v -> openCamera());
        findViewById(R.id.btnGallery).setOnClickListener(v -> openGallery());
        etDate.setOnClickListener(v -> pickDate());
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putString("name", etName.getText().toString());
        out.putString("category", selectedCategory());
        if (selectedDate != null) out.putString("date", ExpiryUtils.store(selectedDate));
        if (currentPhoto != null) out.putString("photo", currentPhoto);
        if (pendingCameraFile != null) out.putString("camera", pendingCameraFile.getAbsolutePath());
    }

    @Override
    protected void onDestroy() {
        // Leaving without saving: remove a newly taken photo so it does not pile up
        if (!saved && isFinishing() && currentPhoto != null && !currentPhoto.equals(originalPhoto)) {
            PhotoUtils.delete(currentPhoto);
        }
        super.onDestroy();
    }

    // ---- category ----
    private void checkCategory(String cat) {
        int id = R.id.chipFood;
        if (ExpiryUtils.MEDICATION.equals(cat)) id = R.id.chipMedication;
        else if (ExpiryUtils.COSMETICS.equals(cat)) id = R.id.chipCosmetics;
        else if (ExpiryUtils.SKIN_CARE.equals(cat)) id = R.id.chipSkinCare;
        else if (ExpiryUtils.OTHER.equals(cat)) id = R.id.chipOther;
        chipGroup.check(id);
    }

    private String selectedCategory() {
        int id = chipGroup.getCheckedChipId();
        if (id == R.id.chipMedication) return ExpiryUtils.MEDICATION;
        if (id == R.id.chipCosmetics) return ExpiryUtils.COSMETICS;
        if (id == R.id.chipSkinCare) return ExpiryUtils.SKIN_CARE;
        if (id == R.id.chipOther) return ExpiryUtils.OTHER;
        return ExpiryUtils.FOOD;
    }

    // ---- date ----
    private void showDate() {
        etDate.setText(selectedDate == null ? "" : ExpiryUtils.format(selectedDate));
    }

    private void pickDate() {
        LocalDate start = selectedDate != null ? selectedDate : LocalDate.now();
        DatePickerDialog dp = new DatePickerDialog(this, (view, y, m, d) -> {
            selectedDate = LocalDate.of(y, m + 1, d);
            tvDateError.setVisibility(View.GONE);
            etDate.setBackgroundResource(R.drawable.bg_field);
            showDate();
        }, start.getYear(), start.getMonthValue() - 1, start.getDayOfMonth());
        if (editing == null) { // Add mode: today or later only
            dp.getDatePicker().setMinDate(
                    LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
        }
        dp.show();
    }

    // ---- photo ----
    private void setPhoto(String path) {
        if (currentPhoto != null && !currentPhoto.equals(originalPhoto) && !currentPhoto.equals(path)) {
            PhotoUtils.delete(currentPhoto); // replaced an unsaved photo
        }
        currentPhoto = path;
        showPhoto();
    }

    private void showPhoto() {
        Bitmap bmp = currentPhoto == null ? null : PhotoUtils.load(currentPhoto, 800);
        if (bmp != null) {
            ivPhoto.setImageBitmap(bmp);
            ivPhoto.setVisibility(View.VISIBLE);
            tvNoPhoto.setVisibility(View.GONE);
        } else {
            ivPhoto.setVisibility(View.GONE);
            tvNoPhoto.setVisibility(View.VISIBLE);
        }
    }

    private void openCamera() {
        try {
            pendingCameraFile = PhotoUtils.createImageFile(this);
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", pendingCameraFile);
            Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    .putExtra(MediaStore.EXTRA_OUTPUT, uri)
                    .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            cameraLauncher.launch(i);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.msg_no_camera, Toast.LENGTH_SHORT).show();
        }
    }

    private void openGallery() {
        try {
            galleryLauncher.launch(new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.msg_no_gallery, Toast.LENGTH_SHORT).show();
        }
    }

    // ---- save ----
    private void save() {
        String name = etName.getText().toString().trim();
        boolean ok = true;

        if (name.isEmpty()) {
            tvNameError.setText(R.string.err_name);
            tvNameError.setVisibility(View.VISIBLE);
            etName.setBackgroundResource(R.drawable.bg_field_error);
            ok = false;
        } else {
            tvNameError.setVisibility(View.GONE);
            etName.setBackgroundResource(R.drawable.bg_field);
        }

        if (selectedDate == null) {
            showDateError(R.string.err_date_empty);
            ok = false;
        } else if (editing == null && selectedDate.isBefore(LocalDate.now())) {
            showDateError(R.string.err_date_past);
            ok = false;
        } else {
            tvDateError.setVisibility(View.GONE);
            etDate.setBackgroundResource(R.drawable.bg_field);
        }
        if (!ok) return;

        if (editing == null) {
            db.addProduct(new Product(0, name, selectedCategory(),
                    ExpiryUtils.store(selectedDate), currentPhoto));
            Toast.makeText(this, R.string.msg_saved, Toast.LENGTH_SHORT).show();
        } else {
            editing.name = name;
            editing.category = selectedCategory();
            editing.expiryDate = ExpiryUtils.store(selectedDate);
            editing.photoPath = currentPhoto;
            db.updateProduct(editing);
            if (originalPhoto != null && !originalPhoto.equals(currentPhoto)) {
                PhotoUtils.delete(originalPhoto);
            }
            Toast.makeText(this, R.string.msg_changes_saved, Toast.LENGTH_SHORT).show();
        }
        saved = true;
        finish();
    }

    private void showDateError(int res) {
        tvDateError.setText(res);
        tvDateError.setVisibility(View.VISIBLE);
        etDate.setBackgroundResource(R.drawable.bg_field_error);
    }
}
