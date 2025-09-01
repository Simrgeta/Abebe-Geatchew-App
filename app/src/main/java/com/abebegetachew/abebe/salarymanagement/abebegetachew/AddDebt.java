package com.abebegetachew.abebe.salarymanagement.abebegetachew;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import android.app.DatePickerDialog;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class AddDebt extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_debt);

        ConstraintLayout root = findViewById(R.id.root);
        MaterialCardView card = findViewById(R.id.card);
        ImageView blobTopLeft = findViewById(R.id.blobTopLeft);
        ImageView blobBottomRight = findViewById(R.id.blobBottomRight);
        LottieAnimationView lottieBadge = findViewById(R.id.lottieBadge);

        float ty = 24f * getResources().getDisplayMetrics().density; // 24dp in px

        // Slower durations for premium feel
        long durationCard = 1100L; // Card: slower slide/fade
        long durationBadge = 950L; // Badge: gentle fade
        long durationBlobs = 1600L; // Blobs: very slow so user notices

        Interpolator interp = AnimationUtils.loadInterpolator(this, android.R.interpolator.fast_out_slow_in);

        // Initial state
        card.setAlpha(0f);
        card.setTranslationY(ty);

        blobTopLeft.setAlpha(0f);
        blobTopLeft.setScaleX(0.9f); // subtle scale start
        blobTopLeft.setScaleY(0.9f);

        blobBottomRight.setAlpha(0f);
        blobBottomRight.setScaleX(0.9f);
        blobBottomRight.setScaleY(0.9f);

        lottieBadge.setAlpha(0f);

        // Animate after layout is ready
        root.post(() -> {
            // Card first
            card.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(0)
                    .setDuration(durationCard)
                    .setInterpolator(interp)
                    .start();

            // Badge after card
            lottieBadge.animate()
                    .alpha(1f)
                    .setStartDelay(300) // noticeable gap
                    .setDuration(durationBadge)
                    .setInterpolator(interp)
                    .start();

            // Top blob
            blobTopLeft.animate()
                    .alpha(0.25f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setStartDelay(500) // starts after badge appears
                    .setDuration(durationBlobs)
                    .setInterpolator(interp)
                    .start();

            // Bottom blob
            blobBottomRight.animate()
                    .alpha(0.20f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setStartDelay(750) // last, slowest
                    .setDuration(durationBlobs)
                    .setInterpolator(interp)
                    .start();
        });

        TextInputEditText etDate = findViewById(R.id.etDate);
        TextInputLayout tilDate = findViewById(R.id.tilDate);

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()); // e.g., 15 Aug 2025

        DatePickerDialog.OnDateSetListener dateSetListener = (view, year, month, dayOfMonth) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

            // Update EditText with formatted date
            etDate.setText(sdf.format(calendar.getTime()));
        };

        etDate.setOnClickListener(v -> {
            DatePickerDialog picker = new DatePickerDialog(
                    AddDebt.this,
                    dateSetListener,
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            picker.show();
        });

        tilDate.setEndIconOnClickListener(v -> {
            DatePickerDialog picker = new DatePickerDialog(
                    AddDebt.this,
                    dateSetListener,
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            picker.show();
        });

        MaterialAutoCompleteTextView etBorrower = findViewById(R.id.etBorrower);
        DatabaseHelper db = new DatabaseHelper(this);

        BorrowerAdapter adapter = new BorrowerAdapter(this, new ArrayList<>());
        etBorrower.setAdapter(adapter);
        etBorrower.setThreshold(1);

        etBorrower.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ArrayList<String> suggestions = db.getBorrowerSuggestions(s.toString());
                adapter.clear();
                adapter.addAll(suggestions);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });


        MaterialToolbar topAppBar = findViewById(R.id.topAppBar);
        topAppBar.setNavigationOnClickListener(v -> finish());

    }

    public void submit(View view) {
        MaterialAutoCompleteTextView etBorrower = findViewById(R.id.etBorrower);
        TextInputEditText etAmount = findViewById(R.id.etAmount);
        TextInputEditText etDate = findViewById(R.id.etDate);
        TextInputEditText etReason = findViewById(R.id.etReason);

        String borrower = etBorrower.getText() != null ? etBorrower.getText().toString().trim() : "";
        String amountStr = etAmount.getText() != null ? etAmount.getText().toString().trim() : "";
        String date = etDate.getText() != null ? etDate.getText().toString().trim() : "";
        String reason = etReason.getText() != null ? etReason.getText().toString().trim() : "";

        // Validate inputs
        if (borrower.isEmpty()) {
            etBorrower.setError("Borrower is required");
            shakeView(etBorrower);
            etBorrower.requestFocus();
            return;
        }

        if (amountStr.isEmpty()) {
            etAmount.setError("Amount is required");
            shakeView(etAmount);
            etAmount.requestFocus();
            return;
        }

        if (date.isEmpty()) {
            etDate.setError("Date is required");
            shakeView(etDate);
            etDate.requestFocus();
            return;
        }

        double amountOwed;
        try {
            amountOwed = Double.parseDouble(amountStr);
            if (amountOwed <= 0) {
                etAmount.setError("Amount must be greater than 0");
                shakeView(etAmount);
                etAmount.requestFocus();
                return;
            }
        } catch (NumberFormatException e) {
            etAmount.setError("Invalid number");
            shakeView(etAmount);
            etAmount.requestFocus();
            return;
        }

        DatabaseHelper db = new DatabaseHelper(AddDebt.this);
        String debtID = Utils.generateUniqueId();
        double amountPaid = 0.0;
        double remainingDebt = amountOwed - amountPaid;

        long rowID = db.insertDebt(debtID, borrower, amountOwed, amountPaid, remainingDebt, reason, date);
        if(rowID != -1){
            db.insertNameIfNotExists(borrower);
            showDebtAddedDialog(borrower, date, debtID);
        } else {
            Toast.makeText(AddDebt.this, "Failed to add debt. Try again.", Toast.LENGTH_SHORT).show();
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
                                .setDuration(50)
                        )
                )
                .start();
    }


    private void showDebtAddedDialog(String borrower, String date, String debtID) {
        // Capitalize name
        String nameCapitalized = borrower.substring(0, 1).toUpperCase() + borrower.substring(1);

        // Inflate dialog layout
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_debt_added, null);

        MaterialButton btnOk = dialogView.findViewById(R.id.btnOk);
        TextView tvMessage = dialogView.findViewById(R.id.tvMessage);

        tvMessage.setText("Debt added for " + nameCapitalized + " on " + date + " with ID " + debtID + "!");

        final androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        // ✅ Make dialog background transparent and remove corners from window frame
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        btnOk.setOnClickListener(v -> {
            dialog.dismiss();
            finish();
        });


        dialog.show();
    }

}