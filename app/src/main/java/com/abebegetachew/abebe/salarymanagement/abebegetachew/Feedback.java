package com.abebegetachew.abebe.salarymanagement.abebegetachew;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class Feedback extends BaseActivity {

    private Handler handler = new Handler();
    private boolean reverse = false;

    private DatabaseReference dbRef;
    private AlertDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_feedback);

        // 🔹 Firebase reference
        dbRef = FirebaseDatabase.getInstance().getReference("feedbacks");

        // UI refs
        Button btnSubmit = findViewById(R.id.btnSubmit);
        ImageButton backButton = findViewById(R.id.backButton);
        RatingBar ratingBar = findViewById(R.id.ratingBar);
        TextInputEditText etFeedback = findViewById(R.id.etFeedback);

        // Animate gradient button
        TransitionDrawable drawable = (TransitionDrawable) btnSubmit.getBackground();
        Runnable loopAnimation = new Runnable() {
            @Override
            public void run() {
                if (reverse) {
                    drawable.reverseTransition(2000);
                } else {
                    drawable.startTransition(2000);
                }
                reverse = !reverse;
                handler.postDelayed(this, 2000);
            }
        };
        handler.post(loopAnimation);

        // Back button
        backButton.setOnClickListener(v -> finish());

        // Submit button
        btnSubmit.setOnClickListener(v -> {
            String feedback = etFeedback.getText() != null ? etFeedback.getText().toString() : "";
            float rating = ratingBar.getRating();

            String email = FirebaseAuth.getInstance().getCurrentUser() != null
                    ? FirebaseAuth.getInstance().getCurrentUser().getEmail()
                    : "user@example.com";

            if (feedback.isEmpty()) {
                Toast.makeText(this, "Please enter your feedback!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (email == null) {
                Toast.makeText(this, "You must be logged in to submit feedback!", Toast.LENGTH_SHORT).show();
                return;
            }

            String emailKey = email.replace(".", ",");

            FeedbackModel feedbackModel = new FeedbackModel(email, feedback, rating);

            showProgressDialog("Submitting feedback...");

            // ✅ use push() so every submission is new
            dbRef.child(emailKey).push().setValue(feedbackModel)
                    .addOnSuccessListener(unused -> {
                        hideProgressDialog();
                        showSuccessDialog("Thank You!", "Your feedback has been submitted successfully.", true);
                    })
                    .addOnFailureListener(e -> {
                        hideProgressDialog();
                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });
    }

    // 🔹 Progress dialog
    private void showProgressDialog(String message) {
        if (progressDialog == null) {
            View view = getLayoutInflater().inflate(R.layout.dialog_loading, null);
            TextView tvMessage = view.findViewById(R.id.please_wait_message);
            tvMessage.setText(message);

            progressDialog = new AlertDialog.Builder(this)
                    .setView(view)
                    .setCancelable(false)
                    .create();
            if (progressDialog.getWindow() != null)
                progressDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        progressDialog.show();
    }

    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) progressDialog.dismiss();
    }

    // 🔹 Success dialog
    private void showSuccessDialog(String title, String message, boolean finishActivity) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_success, null);
        TextView dialogTitle = dialogView.findViewById(R.id.dialogTitle);
        TextView dialogMessage = dialogView.findViewById(R.id.dialogMessage);
        Button btnOk = dialogView.findViewById(R.id.btnOk);

        dialogTitle.setText(title);
        dialogMessage.setText(message);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView).create();
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        btnOk.setOnClickListener(v -> {
            dialog.dismiss();
            if (finishActivity) finish();
        });

        dialog.show();
    }
}
