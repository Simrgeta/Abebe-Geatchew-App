package com.abebegetachew.abebe.salarymanagement.abebegetachew;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import de.hdodenhof.circleimageview.CircleImageView;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ChangePassword extends BaseActivity {

    private ImageButton backButton;
    private com.sanojpunchihewa.glowbutton.GlowButton saveButton;

    private AlertDialog progressDialog; // Loading dialog
    private TextInputEditText editCurrentPassword, editNewPassword, editConfirmPassword;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_change_password);

        CircleImageView profileImage = findViewById(R.id.profileImage);
        TextView usernameTextView = findViewById(R.id.username);
        TextView emailTextview = findViewById(R.id.emailTextview);
        backButton = findViewById(R.id.backButton);
        saveButton = findViewById(R.id.saveButton);
        editCurrentPassword = findViewById(R.id.editCurrentPassword);
        editNewPassword = findViewById(R.id.editNewPassword);
        editConfirmPassword = findViewById(R.id.editConfirmPassword);

        backButton.setOnClickListener(v -> finish());

        // Load session data
        SessionManager sessionManager = new SessionManager(getApplicationContext());
        String profilePath = sessionManager.getProfileImagePath();
        String fullName = sessionManager.getSessionDetails("key_session_name");
        String email = sessionManager.getSessionDetails("key_session_email");

        // Load profile image
        if (profilePath != null && !profilePath.isEmpty()) {
            Glide.with(this)
                    .load(profilePath)
                    .placeholder(R.drawable.picture)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .error(R.drawable.picture)
                    .into(profileImage);
        }

        // Load and style name
        if (fullName != null && !fullName.trim().isEmpty()) {
            fullName = fullName.trim();
            String[] nameParts = fullName.split("\\s+");
            String firstName = nameParts[0];
            String lastName = nameParts.length > 1 ? nameParts[nameParts.length - 1] : "";
            String combined = firstName + (lastName.isEmpty() ? "" : " " + lastName);
            SpannableString spannable = new SpannableString(combined);
            android.graphics.Typeface mediumFont = ResourcesCompat.getFont(getApplicationContext(), R.font.helvetica_neue_medium);
            android.graphics.Typeface thinFont = ResourcesCompat.getFont(getApplicationContext(), R.font.helvetica_neue_thin);
            spannable.setSpan(new CustomTypefaceSpan(mediumFont), 0, firstName.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            if (!lastName.isEmpty()) {
                int lastNameStart = combined.indexOf(lastName);
                spannable.setSpan(new CustomTypefaceSpan(thinFont), lastNameStart, lastNameStart + lastName.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            usernameTextView.setText(spannable);
        } else {
            usernameTextView.setText("");
        }

        emailTextview.setText(email);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        saveButton.setOnClickListener(v -> changePassword());
    }

    private void changePassword() {
        String currentPassword = editCurrentPassword.getText().toString().trim();
        String newPassword = editNewPassword.getText().toString().trim();
        String confirmPassword = editConfirmPassword.getText().toString().trim();

        // Validation
        if (TextUtils.isEmpty(currentPassword) || TextUtils.isEmpty(newPassword) || TextUtils.isEmpty(confirmPassword)) {
            showFieldErrors();
            return;
        }

        if (!InputValidator.isValidPassword(newPassword)) {
            editNewPassword.setText("");
            editNewPassword.setHintTextColor(Color.parseColor("#FF80A0"));
            editNewPassword.setHint("Weak password");
            editNewPassword.setError("Weak Password!");
            shakeView(editNewPassword);
            editNewPassword.requestFocus();
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            editConfirmPassword.setError("Passwords do not match!");
            shakeView(editConfirmPassword);
            editConfirmPassword.requestFocus();
            return;
        }

        if (currentUser == null || currentUser.getEmail() == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading dialog
        showProgressDialog();

        AuthCredential credential = EmailAuthProvider.getCredential(currentUser.getEmail(), currentPassword);
        currentUser.reauthenticate(credential).addOnCompleteListener(authTask -> {
            if (authTask.isSuccessful()) {
                currentUser.updatePassword(newPassword).addOnCompleteListener(updateTask -> {
                    hideProgressDialog();
                    if (updateTask.isSuccessful()) {
                        showSuccessDialog("Password Changed!", "Your password has been updated successfully.");
                    } else {
                        Toast.makeText(ChangePassword.this, "Failed: " + updateTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            } else {
                hideProgressDialog();
                editCurrentPassword.setError("Current password is incorrect!");
                shakeView(editCurrentPassword);
                editCurrentPassword.requestFocus();
            }
        });
    }

    private void showFieldErrors() {
        if (TextUtils.isEmpty(editCurrentPassword.getText().toString().trim())) {
            editCurrentPassword.setError("Please fill this field!");
            shakeView(editCurrentPassword);
            editCurrentPassword.requestFocus();
        }

        if (TextUtils.isEmpty(editNewPassword.getText().toString().trim())) {
            editNewPassword.setError("Please fill this field!");
            shakeView(editNewPassword);
            editNewPassword.requestFocus();
        }

        if (TextUtils.isEmpty(editConfirmPassword.getText().toString().trim())) {
            editConfirmPassword.setError("Please fill this field!");
            shakeView(editConfirmPassword);
            editConfirmPassword.requestFocus();
        }
    }


    private void shakeView(View view) {
        view.animate().translationXBy(10f).setDuration(50)
                .withEndAction(() -> view.animate().translationXBy(-20f).setDuration(50)
                        .withEndAction(() -> view.animate().translationXBy(10f).setDuration(50)))
                .start();
    }

    // ----------------- Loading & Success Dialogs -----------------
    private void showProgressDialog() {
        if (progressDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            View view = getLayoutInflater().inflate(R.layout.dialog_loading, null);
            builder.setView(view);
            builder.setCancelable(false);
            progressDialog = builder.create();
            if (progressDialog.getWindow() != null)
                progressDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        progressDialog.show();
    }

    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) progressDialog.dismiss();
    }

    private void showSuccessDialog(String title, String message) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_success, null);
        TextView dialogTitle = dialogView.findViewById(R.id.dialogTitle);
        TextView dialogMessage = dialogView.findViewById(R.id.dialogMessage);
        Button btnOk = dialogView.findViewById(R.id.btnOk);

        dialogTitle.setText(title);
        dialogMessage.setText(message);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        btnOk.setOnClickListener(v -> {
            dialog.dismiss();
            finish();
        });
        dialog.show();
    }
}
