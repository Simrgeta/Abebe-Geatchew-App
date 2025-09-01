package com.abebegetachew.abebe.salarymanagement.abebegetachew;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

public class Verification extends BaseActivity {

    EditText num1,num2,num3,num4,num5,num6;
    EditText[] otpBoxes;
    TextView tries;

    private boolean isInErrorState = false;
    private CountDownTimer countDownTimer;
    private static final long OTP_VALIDITY_DURATION =  5 * 60 * 1000;;
    private boolean isOtpExpired = false;
    private long millisRemaining = OTP_VALIDITY_DURATION;


    private long endTime;


    private int no_of_tries = 3;
    private String email;
    private String firebaseIdToken;
    private AlertDialog progressDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_verification);


        email = getIntent().getStringExtra("Email");

        startOtpCountdown();

        num1 = findViewById(R.id.num1);
        num2 = findViewById(R.id.num2);
        num3 = findViewById(R.id.num3);
        num4 = findViewById(R.id.num4);
        num5 = findViewById(R.id.num5);
        num6 = findViewById(R.id.num6);
        tries = findViewById(R.id.tries);

        otpBoxes = new EditText[]{num1, num2, num3, num4, num5, num6};

        setupOtpBoxesListeners();

        FirebaseAuth.getInstance().getCurrentUser().getIdToken(true)
                .addOnSuccessListener(result -> firebaseIdToken = result.getToken())
                .addOnFailureListener(e -> {
                });


    }


    private void showProgressDialog() {
        if (progressDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            View view = LayoutInflater.from(this).inflate(R.layout.dialog_loading, null);
            builder.setView(view);
            builder.setCancelable(false);
            progressDialog = builder.create();

            // ✅ Make dialog background transparent and remove corners from window frame
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


    private void startOtpCountdown() {
        isOtpExpired = false;

        endTime = System.currentTimeMillis() + millisRemaining;

        countDownTimer = new CountDownTimer(millisRemaining, 1000) {
            public void onTick(long millisUntilFinished) {
                millisRemaining = millisUntilFinished;
            }

            public void onFinish() {
                isOtpExpired = true;
                for (EditText box : otpBoxes) {
                    box.setEnabled(false);
                }
                if (!Verification.this.isFinishing() && !Verification.this.isDestroyed()) {
                    showCustomDialog(true);
                }

            }
        }.start();
    }


    private void showCustomDialog(boolean for_expired) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.MyAlertDialogTheme);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.custom_dialog, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);


        // Handle Views
        Button okButton = dialogView.findViewById(R.id.dialog_button);

        if (for_expired) {
            TextView title = dialogView.findViewById(R.id.dialog_title);
            TextView message = dialogView.findViewById(R.id.dialog_message);
            TextView message2 = dialogView.findViewById(R.id.dialog_message2);
            title.setText("Code Expired!");
            message.setText("OTP has expired.");
            message2.setText("Please request a new one.");
        }

        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                finish();
            }
        });

        dialog.show();

    }



    private void setupOTPInput(EditText current, EditText next) {
        current.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                current.setBackgroundResource(R.drawable.v_bg);
            }
        });
        current.setFilters(new InputFilter[]{new InputFilter.LengthFilter(1)});
        current.addTextChangedListener(new TextWatcher() {
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 1 && next != null) {
                    next.requestFocus();
                }
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void afterTextChanged(Editable s) {}
        });
    }

    private void verifyCode() {
        if (isOtpExpired && !Verification.this.isFinishing() && !Verification.this.isDestroyed()) {
            showCustomDialog(true);
            return;
        }

        StringBuilder codeBuilder = new StringBuilder();
        for (EditText box : otpBoxes) {
            codeBuilder.append(box.getText().toString());
        }
        String enteredOtp = codeBuilder.toString().trim();

        if (enteredOtp.isEmpty()) {
            Toast.makeText(this, "Please enter the OTP", Toast.LENGTH_SHORT).show();
            return;
        }

        if (firebaseIdToken == null) {
            return;
        }

        showProgressDialog(); // Show loading

        new Thread(() -> {
            try {
                URL url = new URL("https://otp-backend2-six.vercel.app/api/verify-otp");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "Bearer " + firebaseIdToken);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                String jsonBody = "{ \"email\": \"" + email + "\", \"enteredOtp\": \"" + enteredOtp + "\" }";
                OutputStream os = conn.getOutputStream();
                os.write(jsonBody.getBytes());
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                InputStream inputStream = (responseCode == 200) ? conn.getInputStream() : conn.getErrorStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                conn.disconnect();

                String responseBody = response.toString();
                JSONObject responseJson = new JSONObject(responseBody);
                boolean success = responseJson.optBoolean("success", false);

                runOnUiThread(() -> {
                    hideProgressDialog(); // Hide loading
                    if (success) {
                        if (countDownTimer != null) {
                            countDownTimer.cancel();
                        }
                        Intent intent = new Intent(Verification.this, ProfilePicture.class);
                        intent.putExtra("Email", email);
                        startActivity(intent);
                        finish();
                    } else {
                        showOTPError();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    hideProgressDialog(); // Hide loading
                    Toast.makeText(Verification.this, "Server error!", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }




    private void setupOtpBoxesListeners() {
        for (int i = 0; i < otpBoxes.length; i++) {
            final int index = i;

            otpBoxes[i].setFilters(new InputFilter[]{new InputFilter.LengthFilter(1)});

            otpBoxes[i].addTextChangedListener(new TextWatcher() {
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    tries.setText("");
                    if (isInErrorState) {
                        for (EditText box : otpBoxes) {
                            box.setBackgroundResource(R.drawable.et_design);
                        }
                        isInErrorState = false;
                    }

                    if (s.length() == 1 && index < otpBoxes.length - 1) {
                        otpBoxes[index + 1].requestFocus();
                    }
                }

                public void afterTextChanged(Editable s) {
                    if (index == otpBoxes.length - 1 && s.length() == 1) {
                        otpBoxes[index].postDelayed(() -> verifyCode(), 100);
                    }
                }
            });

            otpBoxes[i].setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() == android.view.KeyEvent.ACTION_DOWN &&
                        keyCode == android.view.KeyEvent.KEYCODE_DEL) {
                    if (otpBoxes[index].getText().toString().isEmpty() && index > 0) {
                        otpBoxes[index - 1].setText("");
                        otpBoxes[index - 1].requestFocus();
                    }
                }
                return false;
            });

            otpBoxes[i].setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    for (int j = 0; j < index; j++) {
                        if (otpBoxes[j].getText().toString().isEmpty()) {
                            otpBoxes[j].requestFocus();
                            return;
                        }
                    }

                    for (int j = index; j < otpBoxes.length; j++) {
                        otpBoxes[j].setText("");
                    }
                }
            });
        }
    }

    private void showOTPError() {
        isInErrorState = true;

        for (EditText box : otpBoxes) {
            box.setText("");
            box.setBackgroundResource(R.drawable.error);
        }


        num1.requestFocus();

        no_of_tries -= 1;
        tries.setText("You have " + no_of_tries + " more trials left");

        if (no_of_tries == 0) {
            for (EditText box : otpBoxes) {
                box.setEnabled(false);
            }

            if (!Verification.this.isFinishing() && !Verification.this.isDestroyed()) {
                showCustomDialog(false);
            }
        }
    }

    public void contact_author(View view) {
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:+251911863424"));
        startActivity(intent);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
    @Override
    protected void onResume() {
        super.onResume();

        if (!isOtpExpired) {
            millisRemaining = endTime - System.currentTimeMillis();
            if (millisRemaining > 0) {
                startOtpCountdown();
            } else {
                isOtpExpired = true;
                for (EditText box : otpBoxes) {
                    box.setEnabled(false);
                }
                if (!Verification.this.isFinishing() && !Verification.this.isDestroyed()) {
                    showCustomDialog(true);
                }
            }
        }
    }



}