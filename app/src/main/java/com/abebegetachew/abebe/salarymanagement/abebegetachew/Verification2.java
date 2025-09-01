package com.abebegetachew.abebe.salarymanagement.abebegetachew;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Base64;
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
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class Verification2 extends BaseActivity {

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
    private String password;
    private String firebaseIdToken;
    private AlertDialog progressDialog;
    SessionManager sessionManager;
    private static final String HMAC_SECRET = "a3f5c7e2d9a1b478e4f62d1349b8c51f3a7c45b6d29e8f7c8b9a1d2e3f4b5678";

    public static String hmacSHA256Hex(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_verification);


        email = getIntent().getStringExtra("Email");
        password = getIntent().getStringExtra("Password");
        sessionManager = new SessionManager(Verification2.this);

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
                if (!Verification2.this.isFinishing() && !Verification2.this.isDestroyed()) {
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

    /**
     * Downloads an image from a URL and saves it to the app's files directory.
     * @param imageUrl Cloudinary image URL
     * @param fileName Desired file name
     * @return Full local file path
     */
    private String downloadImageToLocal(String imageUrl, String fileName) {
        try {
            URL url = new URL(imageUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.connect();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return null;
            }

            // Create output file path
            File directory = new File(getFilesDir(), "profile_pics");
            if (!directory.exists()) directory.mkdirs();

            File file = new File(directory, fileName);
            try (InputStream input = connection.getInputStream();
                 OutputStream output = new FileOutputStream(file)) {

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = input.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }
            }
            connection.disconnect();

            return file.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void verifyCode() {
        if (isOtpExpired && !Verification2.this.isFinishing() && !Verification2.this.isDestroyed()) {
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
                URL url = new URL("https://otp-backend2-six.vercel.app/api/reVerify-otp");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "Bearer " + firebaseIdToken);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
                long timestamp = System.currentTimeMillis();
                String signature = hmacSHA256Hex(HMAC_SECRET, deviceId);

                String jsonBody = "{ " +
                        "\"email\": \"" + email + "\"," +
                        "\"enteredOtp\": \"" + enteredOtp + "\"," +
                        "\"deviceId\": \"" + deviceId + "\"," +
                        "\"timestamp\": " + timestamp + "," +
                        "\"signature\": \"" + signature + "\"" +
                        "}";
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
                    if (success) {
                        showProgressDialog();
                        if (countDownTimer != null) {
                            countDownTimer.cancel();
                        }
                        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                                .addOnCompleteListener(Verification2.this, new OnCompleteListener<AuthResult>() {
                                    @Override
                                    public void onComplete(@NonNull Task<AuthResult> task) {
                                        if (task.isSuccessful()) {
                                            FirebaseUser user = task.getResult().getUser();
                                            String uid = user.getUid();

                                            FirebaseFirestore.getInstance().collection("User")
                                                    .document(uid)
                                                    .get()
                                                    .addOnSuccessListener(doc -> {
                                                        if (doc.exists())
                                                        {
                                                            String name = doc.getString("Username");
                                                            String phone = doc.getString("Phone");
                                                            String imageUrl = doc.getString("Profile"); // stored in Firestore
                                                            String profile = "";


                                                            if (imageUrl != null && !imageUrl.isEmpty()) {
                                                                new Thread(() -> {
                                                                    String profilePath = downloadImageToLocal(imageUrl, "profile_picture.jpg");

                                                                    runOnUiThread(() -> {
                                                                        sessionManager.createSession(name, email, phone, imageUrl, profilePath);
                                                                        checkOrCreateKey(email);
                                                                        hideProgressDialog();
                                                                        Intent intent = new Intent(Verification2.this, Main.class);
                                                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                                        startActivity(intent);
                                                                        finish();
                                                                    });
                                                                }).start();
                                                            } else {
                                                                sessionManager.createSession(name, email, phone, "", "");
                                                                checkOrCreateKey(email);
                                                                hideProgressDialog();
                                                                Intent intent = new Intent(Verification2.this, Main.class);
                                                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                                startActivity(intent);
                                                                finish();
                                                            }
                                                        } else {
                                                            hideProgressDialog();
                                                            Toast.makeText(Verification2.this, "Something went wrong...!", Toast.LENGTH_SHORT).show();
                                                        }
                                                    });
                                        } else {
                                            hideProgressDialog();
                                            Toast.makeText(Verification2.this, "Something went wrong...!", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                    } else {
                        hideProgressDialog();
                        showOTPError();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    hideProgressDialog(); // Hide loading
                    Toast.makeText(Verification2.this, "Server error!", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }



    private void checkOrCreateKey(String email) {
        try {
            String nodeId = email.replace(".", ",");
            DatabaseReference ref = FirebaseDatabase.getInstance()
                    .getReference("Keys")
                    .child(nodeId);

            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists() && snapshot.child("Key").getValue() != null) {
                        String keyBase64 = snapshot.child("Key").getValue(String.class);
                        sessionManager.saveKeyToPrefs(keyBase64);
                        Log.d("KeyFlow", "Existing key loaded from RTDB and saved locally");
                    } else {
                        // Generate a new software AES key
                        SecretKey secretKey = KeyStoreHelper.getOrCreateKey(Verification2.this);
                        if (secretKey == null) {
                            Toast.makeText(Verification2.this, "Key generation error", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        String keyBase64 = Base64.encodeToString(secretKey.getEncoded(), Base64.NO_WRAP);

                        // Save locally
                        sessionManager.saveKeyToPrefs(keyBase64);

                        // Upload to RTDB
                        java.util.Map<String, Object> data = new java.util.HashMap<>();
                        data.put("Email", email);
                        data.put("Key", keyBase64);

                        ref.setValue(data)
                                .addOnSuccessListener(aVoid -> Log.d("KeyFlow", "New key generated, uploaded, and saved locally"))
                                .addOnFailureListener(e -> Toast.makeText(Verification2.this, "Failed to upload key!", Toast.LENGTH_SHORT).show());
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("KeyFlow", "Database error: " + error.getMessage());
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
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

            if (!Verification2.this.isFinishing() && !Verification2.this.isDestroyed()) {
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
                if (!Verification2.this.isFinishing() && !Verification2.this.isDestroyed()) {
                    showCustomDialog(true);
                }
            }
        }
    }



}