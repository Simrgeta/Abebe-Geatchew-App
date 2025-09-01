package com.abebegetachew.abebe.salarymanagement.abebegetachew;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class Pending extends AppCompatActivity {

    private FirebaseFirestore db;
    private String email;
    private Handler handler = new Handler();
    private Runnable checkRunnable;

    private final String authorPhoneNumber = "tel:+251911863424"; // Replace with actual author number

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_pending);

        db = FirebaseFirestore.getInstance();

        email = getIntent().getStringExtra("Email");
        if (email == null) {
            Toast.makeText(this, "Something went wrong!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        startChecking();
    }

    private void startChecking() {
        checkRunnable = new Runnable() {
            @Override
            public void run() {
                db.collection("TempUser")
                        .document(email)
                        .get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                Boolean isPaid = documentSnapshot.getBoolean("is_paid");
                                if (isPaid != null && isPaid) {
                                    // Move to verification screen with same email
                                    Intent intent = new Intent(Pending.this, Verification.class);
                                    intent.putExtra("Email", email);
                                    startActivity(intent);
                                    finish();
                                } else {
                                    // Repeat check after delay (e.g. 10 seconds)
                                    handler.postDelayed(this, 10000);
                                }
                            } else {
                                Toast.makeText(Pending.this, "User data not found", Toast.LENGTH_SHORT).show();
                                handler.postDelayed(this, 10000);
                            }
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(Pending.this, "Failed to fetch data", Toast.LENGTH_SHORT).show();
                            handler.postDelayed(this, 10000);
                        });
            }
        };

        handler.post(checkRunnable);
    }

    public void showAlert(View view) {
        if (isFinishing()) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.MyAlertDialogTheme);
        // Inflate custom layout
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_payment_pending, null);
        builder.setView(dialogView);
        builder.setCancelable(true);

        AlertDialog dialog = builder.create();

        // Setup dialog views and button
        TextView message = dialogView.findViewById(R.id.textMessage);
        Button contactBtn = dialogView.findViewById(R.id.buttonContact);

        message.setText("Pay or if you paid, your request is being processed. If it took too long, contact the author.");

        contactBtn.setOnClickListener(v -> {
            Intent dialIntent = new Intent(Intent.ACTION_DIAL);
            dialIntent.setData(Uri.parse(authorPhoneNumber)); // authorPhoneNumber is your String like "tel:+1234567890"
            startActivity(dialIntent);
            dialog.dismiss();
        });

        dialog.show();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(checkRunnable);
    }

}
