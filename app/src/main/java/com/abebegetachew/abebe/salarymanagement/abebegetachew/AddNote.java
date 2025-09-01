package com.abebegetachew.abebe.salarymanagement.abebegetachew;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AddNote extends AppCompatActivity {

    private TextInputEditText etDate;
    private LinearLayout rowsContainer;
    private final List<RowHolder> rowList = new ArrayList<>();
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_note);

        dbHelper = new DatabaseHelper(this);

        ConstraintLayout root = findViewById(R.id.root);
        MaterialCardView card = findViewById(R.id.card);
        ImageView blobTopLeft = findViewById(R.id.blobTopLeft);
        ImageView blobBottomRight = findViewById(R.id.blobBottomRight);
        LottieAnimationView lottieBadge = findViewById(R.id.lottieBadge);

        etDate = findViewById(R.id.etDate);
        TextInputLayout tilDate = findViewById(R.id.tilDate);
        rowsContainer = findViewById(R.id.rowsContainer);

        findViewById(R.id.btnAddRow).setOnClickListener(v -> addRow());
        findViewById(R.id.btnRemoveRow).setOnClickListener(v -> removeRow());
        findViewById(R.id.btnSaveNote).setOnClickListener(v -> saveNotes());

        // Animate entry
        float ty = 24f * getResources().getDisplayMetrics().density;
        long durationCard = 1100L, durationBadge = 950L, durationBlobs = 1600L;
        Interpolator interp = AnimationUtils.loadInterpolator(this, android.R.interpolator.fast_out_slow_in);

        card.setAlpha(0f);
        card.setTranslationY(ty);
        blobTopLeft.setAlpha(0f);
        blobTopLeft.setScaleX(0.9f);
        blobTopLeft.setScaleY(0.9f);
        blobBottomRight.setAlpha(0f);
        blobBottomRight.setScaleX(0.9f);
        blobBottomRight.setScaleY(0.9f);
        lottieBadge.setAlpha(0f);

        root.post(() -> {
            card.animate().alpha(1f).translationY(0f).setDuration(durationCard).setInterpolator(interp).start();
            lottieBadge.animate().alpha(1f).setStartDelay(300).setDuration(durationBadge).setInterpolator(interp).start();
            blobTopLeft.animate().alpha(0.25f).scaleX(1f).scaleY(1f).setStartDelay(500).setDuration(durationBlobs).setInterpolator(interp).start();
            blobBottomRight.animate().alpha(0.20f).scaleX(1f).scaleY(1f).setStartDelay(750).setDuration(durationBlobs).setInterpolator(interp).start();
        });

        // Date Picker Setup
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

        DatePickerDialog.OnDateSetListener dateSetListener = (view, year, month, dayOfMonth) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            etDate.setText(sdf.format(calendar.getTime()));
        };

        etDate.setOnClickListener(v -> showDatePicker(calendar, dateSetListener));
        tilDate.setEndIconOnClickListener(v -> showDatePicker(calendar, dateSetListener));

        // Toolbar back button
        MaterialToolbar topAppBar = findViewById(R.id.topAppBar);
        topAppBar.setNavigationOnClickListener(v -> finish());
    }

    private void showDatePicker(Calendar calendar, DatePickerDialog.OnDateSetListener listener) {
        new DatePickerDialog(
                AddNote.this,
                listener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void addRow() {
        View rowView = LayoutInflater.from(this).inflate(R.layout.row_note_item, rowsContainer, false);

        TextInputLayout tilDesc = rowView.findViewById(R.id.tilDescription);
        TextInputLayout tilAmount = rowView.findViewById(R.id.tilAmount);
        TextInputEditText etDesc = rowView.findViewById(R.id.etDescription);
        TextInputEditText etAmount = rowView.findViewById(R.id.etAmount);

        rowsContainer.addView(rowView);
        rowList.add(new RowHolder(tilDesc, tilAmount, etDesc, etAmount));

        // Request focus on the new row's description field
        etDesc.requestFocus();

    }

    private void removeRow() {
        int count = rowsContainer.getChildCount();
        if (count > 0) {
            rowsContainer.removeViewAt(count - 1);
            rowList.remove(rowList.size() - 1);
        }
    }

    private void saveNotes() {
        String date = etDate.getText() != null ? etDate.getText().toString().trim() : "";
        if (date.isEmpty()) {
            etDate.setError("Date is required");
            shakeView(etDate);
            etDate.requestFocus();
            return;
        }

        boolean inserted = false;
        for (RowHolder holder : rowList) {
            String desc = holder.etDescription.getText().toString().trim();
            String amtStr = holder.etAmount.getText().toString().trim();

            if (desc.isEmpty() || amtStr.isEmpty()) continue;

            double amt;
            try {
                amt = Double.parseDouble(amtStr);
                if (amt <= 0) {
                    holder.etAmount.setError("Amount must be > 0");
                    shakeView(holder.etAmount);
                    holder.etAmount.requestFocus();
                    return;
                }
            } catch (NumberFormatException e) {
                holder.etAmount.setError("Invalid number");
                shakeView(holder.etAmount);
                holder.etAmount.requestFocus();
                return;
            }

            dbHelper.insertNote(desc, amt, date);
            inserted = true;
        }

        if (inserted) {
            showNoteAddedDialog();
        } else {
            Toast.makeText(this, "No valid notes to save.", Toast.LENGTH_SHORT).show();
        }
    }

    private void shakeView(View view) {
        view.animate()
                .translationXBy(10f)
                .setDuration(50)
                .withEndAction(() -> view.animate()
                        .translationXBy(-20f)
                        .setDuration(50)
                        .withEndAction(() -> view.animate()
                                .translationXBy(10f)
                                .setDuration(50)))
                .start();
    }

    private void showNoteAddedDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_income_added, null);

        MaterialButton btnOk = dialogView.findViewById(R.id.btnOk);
        TextView tvMessage = dialogView.findViewById(R.id.tvMessage);
        tvMessage.setText(R.string.notes_saved_successfully);

        final androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        btnOk.setOnClickListener(v -> {
            dialog.dismiss();
            finish();
        });

        dialog.show();
    }

    private static class RowHolder {
        TextInputLayout tilDescription, tilAmount;
        TextInputEditText etDescription, etAmount;

        RowHolder(TextInputLayout tilDesc, TextInputLayout tilAmt, TextInputEditText etDesc, TextInputEditText etAmt) {
            this.tilDescription = tilDesc;
            this.tilAmount = tilAmt;
            this.etDescription = etDesc;
            this.etAmount = etAmt;
        }
    }

}
