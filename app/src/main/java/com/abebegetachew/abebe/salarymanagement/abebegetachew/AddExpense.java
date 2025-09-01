package com.abebegetachew.abebe.salarymanagement.abebegetachew;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
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
import java.util.Calendar;
import java.util.Locale;

public class AddExpense extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_expense);

        ConstraintLayout root = findViewById(R.id.root);
        MaterialCardView card = findViewById(R.id.card);
        ImageView blobTopLeft = findViewById(R.id.blobTopLeft);
        ImageView blobBottomRight = findViewById(R.id.blobBottomRight);
        LottieAnimationView lottieBadge = findViewById(R.id.lottieBadge);

        float ty = 24f * getResources().getDisplayMetrics().density; // 24dp in px

        // Durations for animations
        long durationCard = 1100L;
        long durationBadge = 950L;
        long durationBlobs = 1600L;

        Interpolator interp = AnimationUtils.loadInterpolator(this, android.R.interpolator.fast_out_slow_in);

        // Initial states
        card.setAlpha(0f);
        card.setTranslationY(ty);

        blobTopLeft.setAlpha(0f);
        blobTopLeft.setScaleX(0.9f);
        blobTopLeft.setScaleY(0.9f);

        blobBottomRight.setAlpha(0f);
        blobBottomRight.setScaleX(0.9f);
        blobBottomRight.setScaleY(0.9f);

        lottieBadge.setAlpha(0f);

        // Animate after layout is ready
        root.post(() -> {
            card.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(durationCard)
                    .setInterpolator(interp)
                    .start();

            lottieBadge.animate()
                    .alpha(1f)
                    .setStartDelay(300)
                    .setDuration(durationBadge)
                    .setInterpolator(interp)
                    .start();

            blobTopLeft.animate()
                    .alpha(0.25f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setStartDelay(500)
                    .setDuration(durationBlobs)
                    .setInterpolator(interp)
                    .start();

            blobBottomRight.animate()
                    .alpha(0.20f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setStartDelay(750)
                    .setDuration(durationBlobs)
                    .setInterpolator(interp)
                    .start();
        });

        // Date Picker Setup
        TextInputEditText etDate = findViewById(R.id.etDate);
        TextInputLayout tilDate = findViewById(R.id.tilDate);

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

        DatePickerDialog.OnDateSetListener dateSetListener = (view, year, month, dayOfMonth) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            etDate.setText(sdf.format(calendar.getTime()));
        };

        etDate.setOnClickListener(v -> {
            DatePickerDialog picker = new DatePickerDialog(
                    AddExpense.this,
                    dateSetListener,
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            picker.show();
        });

        tilDate.setEndIconOnClickListener(v -> {
            DatePickerDialog picker = new DatePickerDialog(
                    AddExpense.this,
                    dateSetListener,
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            picker.show();
        });

        // Toolbar back button
        MaterialToolbar topAppBar = findViewById(R.id.topAppBar);
        topAppBar.setNavigationOnClickListener(v -> finish());

        AutoCompleteTextView categoryDropdown = findViewById(R.id.etCategory);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.expense_categories,
                android.R.layout.simple_dropdown_item_1line
        );
        categoryDropdown.setAdapter(adapter);

    }

    public void submit(View view) {
        TextInputEditText etAmount = findViewById(R.id.etAmount);
        TextInputEditText etDate = findViewById(R.id.etDate);
        TextInputEditText etReason = findViewById(R.id.etReason);
        AutoCompleteTextView etCategory = findViewById(R.id.etCategory);

        String amountStr = etAmount.getText() != null ? etAmount.getText().toString().trim() : "";
        String date = etDate.getText() != null ? etDate.getText().toString().trim() : "";
        String reason = etReason.getText() != null ? etReason.getText().toString().trim() : "";
        String category = etCategory.getText() != null ? etCategory.getText().toString().trim() : "";

        // Validate inputs
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

        if (category.isEmpty()) {
            etCategory.setError("Category is required");
            shakeView(etCategory);
            etCategory.requestFocus();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
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

        DatabaseHelper db = new DatabaseHelper(AddExpense.this);
        String expenseId = Utils.generateUniqueId();
        long rowID = db.insertExpense(expenseId, amount, reason, date, category);

        if (rowID != -1) {
            showExpenseAddedDialog();
        } else {
            Toast.makeText(AddExpense.this, "Failed to add expense. Try again.", Toast.LENGTH_SHORT).show();
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


    private void showExpenseAddedDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_income_added, null);

        MaterialButton btnOk = dialogView.findViewById(R.id.btnOk);
        TextView tvMessage = dialogView.findViewById(R.id.tvMessage);

        tvMessage.setText(R.string.entry_recorded_successfully);

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
}