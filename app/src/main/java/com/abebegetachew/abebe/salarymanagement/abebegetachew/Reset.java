package com.abebegetachew.abebe.salarymanagement.abebegetachew;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;

public class Reset extends BaseActivity {

    EditText email1;
    private AlertDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_reset);

        email1 = findViewById(R.id.email);
    }

    public void reset(View view) {
        String email = email1.getText().toString().trim();

        if (!InputValidator.isValidEmail(email)) {
            email1.setText("");
            email1.setHintTextColor(Color.parseColor("#FF80A0"));
            email1.setHint("Invalid email");
            return;
        }

        showProgressDialog();

        PasswordResetHelper.sendResetRequest(this, email, new PasswordResetHelper.ResetCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    hideProgressDialog();
                    // Only finish after success
                    finish();
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                runOnUiThread(() -> {
                    hideProgressDialog();
                    // Show error toast
                    Toast.makeText(Reset.this, errorMessage, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void showProgressDialog() {
        if (progressDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            View view = LayoutInflater.from(this).inflate(R.layout.dialog_loading, null);
            builder.setView(view);
            builder.setCancelable(false);
            progressDialog = builder.create();

            if (progressDialog.getWindow() != null) {
                progressDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
        }
        progressDialog.show();
    }

    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}
