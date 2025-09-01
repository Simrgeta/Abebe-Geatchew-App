package com.abebegetachew.abebe.salarymanagement.abebegetachew;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
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

public class RepayDebt extends AppCompatActivity {

    private DatabaseHelper db;
    private TextInputEditText etRepayAmount;
    private TextInputEditText etRepayDate;
    private TextInputLayout tilRepayDate;
    private String debtId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_repay_debt);

        db = new DatabaseHelper(this);

        debtId = getIntent().getStringExtra("DEBT_ID");
        String borrowerName = getIntent().getStringExtra("BORROWER_NAME");

        ConstraintLayout root = findViewById(R.id.root);
        MaterialCardView card = findViewById(R.id.card);
        TextInputEditText etDebtId = findViewById(R.id.etDebtId);
        TextInputEditText etBorrower = findViewById(R.id.etBorrower);
        etRepayAmount = findViewById(R.id.etRepayAmount);
        etRepayDate = findViewById(R.id.etRepayDate);
        tilRepayDate = findViewById(R.id.tilRepayDate);
        MaterialToolbar topAppBar = findViewById(R.id.topAppBar);

        etDebtId.setText(debtId);
        etBorrower.setText(borrowerName);

        topAppBar.setNavigationOnClickListener(v -> finish());

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

        DatePickerDialog.OnDateSetListener dateSetListener = (view, year, month, dayOfMonth) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            etRepayDate.setText(sdf.format(calendar.getTime()));
        };

        etRepayDate.setOnClickListener(v -> {
            DatePickerDialog picker = new DatePickerDialog(
                    RepayDebt.this,
                    dateSetListener,
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            picker.show();
        });

        tilRepayDate.setEndIconOnClickListener(v -> {
            DatePickerDialog picker = new DatePickerDialog(
                    RepayDebt.this,
                    dateSetListener,
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            picker.show();
        });
    }

    public void submitRepayment(View view) {
        TextInputEditText etBorrower = findViewById(R.id.etBorrower);
        String name = etBorrower.getText().toString();

        if (etRepayAmount.getText() == null || etRepayAmount.getText().toString().isEmpty()) {
            Toast.makeText(this, "Enter repayment amount", Toast.LENGTH_SHORT).show();
            return;
        }
        else if (etRepayDate.getText() == null || etRepayDate.getText().toString().isEmpty()) {
            Toast.makeText(this, "Enter repayment date", Toast.LENGTH_SHORT).show();
            return;
        }
        else if (etRepayAmount.getText() == null || etRepayAmount.getText().toString().isEmpty() || etRepayDate.getText() == null || etRepayDate.getText().toString().isEmpty()) {
            Toast.makeText(this, "Invalid Input!!", Toast.LENGTH_SHORT).show();
            return;
        }

        double enteredAmount = Double.parseDouble(etRepayAmount.getText().toString());

        int result = db.addRepayment(debtId, enteredAmount, etRepayDate.getText().toString());

        if (result == 1) {
            showSuccessDialog(enteredAmount, name);
        } else if (result == -2) {
            showExceedDialog(name);
        } else {
            Toast.makeText(this, "Repayment failed. Try again.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showExceedDialog(String name) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_debt_added, null);
        TextView tvMessage = dialogView.findViewById(R.id.tvMessage);
        LottieAnimationView lottieSuccess = dialogView.findViewById(R.id.lottieSuccess);
        MaterialButton btnOk = dialogView.findViewById(R.id.btnOk);
        tvMessage.setText("Repayment exceeds remaining debt for " + name);
        lottieSuccess.setAnimation(R.raw.failure);

        final androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        btnOk.setOnClickListener(v -> {
            dialog.dismiss();
        });

        dialog.show();
        lottieSuccess.playAnimation();
    }

    private void showSuccessDialog(double amount, String borrower) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_debt_added, null);
        TextView tvMessage = dialogView.findViewById(R.id.tvMessage);
        MaterialButton btnOk = dialogView.findViewById(R.id.btnOk);
        LottieAnimationView lottieSuccess = dialogView.findViewById(R.id.lottieSuccess);

        tvMessage.setText("Payment of " + amount + " made for " + borrower + "!");
        lottieSuccess.setAnimation(R.raw.success);


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
        lottieSuccess.playAnimation();
    }
}
