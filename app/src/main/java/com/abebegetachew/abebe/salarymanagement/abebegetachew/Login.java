package com.abebegetachew.abebe.salarymanagement.abebegetachew;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.util.Base64;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageButton;
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
import java.util.ArrayList;
import java.util.Objects;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class Login extends BaseActivity {


    private static final String HMAC_SECRET = "a3f5c7e2d9a1b478e4f62d1349b8c51f3a7c45b6d29e8f7c8b9a1d2e3f4b5678";
    AppCompatImageButton toggleBtn;
    EditText passwordEditText, email1;
    final boolean[] isPasswordVisible = {false};

    FirebaseAuth auth;
    SessionManager sessionManager;
    private AlertDialog progressDialog;
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
        setContentView(R.layout.activity_login);

        toggleBtn = findViewById(R.id.toggleButton);
        email1 = findViewById(R.id.email);
        passwordEditText = findViewById(R.id.password);

        auth = FirebaseAuth.getInstance();
        sessionManager = new SessionManager(Login.this);

        // Store original font settings
        final Typeface originalTypeface = passwordEditText.getTypeface();
        final float originalTextSize = passwordEditText.getTextSize();
        final int originalTextColor = passwordEditText.getCurrentTextColor();


        toggleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int inputType = passwordEditText.getInputType();
                boolean isPasswordVisible = (inputType == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD));

                // Toggle visibility
                if (isPasswordVisible) {
                    passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    toggleBtn.setImageResource(R.drawable.show);
                } else {
                    passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    toggleBtn.setImageResource(R.drawable.hide);
                }

                // Restore styling
                passwordEditText.setTypeface(originalTypeface);
                passwordEditText.setTextSize(TypedValue.COMPLEX_UNIT_PX, originalTextSize);
                passwordEditText.setTextColor(originalTextColor);

                // Optional: Keep cursor at end
                passwordEditText.setSelection(passwordEditText.getText().length());
            }
        });

        if (auth.getCurrentUser() == null) {
            auth.signInAnonymously().addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    Toast.makeText(this, "Initialization failed...", Toast.LENGTH_SHORT).show();
                }
            });
        } else if (!auth.getCurrentUser().isAnonymous()) {
            auth.signOut();
            auth.signInAnonymously().addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    Toast.makeText(this, "Initialization failed...", Toast.LENGTH_SHORT).show();
                }
            });
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                FirebaseUser user = auth.getCurrentUser();

                if (user != null && user.isAnonymous()) {
                    // Sign out first (optional)
                    auth.signOut();

                    // Then delete the user
                    user.delete()
                            .addOnCompleteListener(task -> {
                                // Regardless of success or failure, finish the activity
                                finish();
                            });
                } else {
                    // Not anonymous, just finish
                    finish();
                }
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


    public void go_to_register(View view) {
        Intent intent = new Intent(Login.this, Register.class);
        startActivity(intent);
    }

    public void reset_password(View view) {
        startActivity(new Intent(Login.this, Reset.class));
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

    public void login(View view) {
        String email = email1.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (!InputValidator.isValidEmail(email)) {
            email1.setText("");
            email1.setHintTextColor(Color.parseColor("#FF80A0"));
            email1.setHint("Invalid email");
            return;
        }

        if (InputValidator.isPasswordEmpty(password)) {
            passwordEditText.setText("");
            passwordEditText.setHintTextColor(Color.parseColor("#FF80A0"));
            passwordEditText.setHint("Invalid password");
            return;
        }

        if (InputValidator.isEmailEmpty(email)) {
            email1.setText("");
            email1.setHintTextColor(Color.parseColor("#FF80A0"));
            email1.setHint("Empty email");
            return;
        }

        showProgressDialog();

        String deviceId = android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
        String signature = hmacSHA256Hex(HMAC_SECRET, deviceId);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            hideProgressDialog();
            Toast.makeText(this, "Initialization error", Toast.LENGTH_SHORT).show();
            return;
        }

        currentUser.getIdToken(true).addOnSuccessListener(getIdTokenResult -> {
            String idToken = getIdTokenResult.getToken();

            // Build JSON body
            JSONObject body = new JSONObject();
            try {
                body.put("email", email);
                body.put("deviceId", deviceId);
                body.put("signature", signature);
            } catch (Exception e) {
                hideProgressDialog();
                Toast.makeText(this, "Error preparing request", Toast.LENGTH_SHORT).show();
                return;
            }

            new Thread(() -> {
                try {
                    URL url = new URL("https://otp-backend2-six.vercel.app/api/loginUser"); // Replace with your actual endpoint
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Authorization", "Bearer " + idToken);
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);

                    OutputStream os = conn.getOutputStream();
                    os.write(body.toString().getBytes());
                    os.flush();
                    os.close();

                    int responseCode = conn.getResponseCode();
                    InputStream is = (responseCode < 400) ? conn.getInputStream() : conn.getErrorStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    JSONObject responseJson = new JSONObject(response.toString());
                    boolean allow = responseJson.optBoolean("allow", false);

                    runOnUiThread(() -> {
                        if (allow) {
                            // ✅ Clean up anonymous user
                            if (currentUser.isAnonymous()) {
                                currentUser.delete().addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        FirebaseAuth.getInstance().signOut();
                                        proceedToLogin(email, password);
                                    } else {
                                        hideProgressDialog();
                                    }
                                });
                            } else {
                                FirebaseAuth.getInstance().signOut();
                                proceedToLogin(email, password);
                            }
                        } else {
                            hideProgressDialog();
                            String message = responseJson.optString("message", "Login denied");
                            // Inflate the custom layout
                            View dialogView = getLayoutInflater().inflate(R.layout.dialog_login_denied, null);

                            TextView textMessage = dialogView.findViewById(R.id.textMessage);
                            textMessage.setText(message);

                            androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(Login.this, R.style.MyAlertDialogTheme)
                                    .setView(dialogView)
                                    .setCancelable(true)
                                    .create();

                            AppCompatButton buttonContact = dialogView.findViewById(R.id.buttonContact);
                            if ("Login denied: unauthorized device".equalsIgnoreCase(message.trim())) {
                                buttonContact.setText("Verify Device");
                                buttonContact.setOnClickListener(v -> {
                                    dialog.dismiss();
                                    OTPRequestResender.sendOTPRequest(Login.this, email);
                                    Intent intent = new Intent(Login.this, Verification2.class);
                                    intent.putExtra("Email", email);
                                    intent.putExtra("Password", password);
                                    startActivity(intent);
                                });
                            } else {
                                buttonContact.setOnClickListener(v -> dialog.dismiss());
                            }
                            dialog.show();
                        }
                    });

                } catch (Exception e) {
                    runOnUiThread(() -> {
                        hideProgressDialog();
                        Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show();
                    });
                    e.printStackTrace();
                }
            }).start();

        }).addOnFailureListener(e -> {
            hideProgressDialog();
        });
    }

    private void proceedToLogin(String email, String password) {
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(Login.this, new OnCompleteListener<AuthResult>() {
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
                                                        Intent intent = new Intent(Login.this, Main.class);
                                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                        startActivity(intent);
                                                        finish();
                                                    });
                                                }).start();
                                            } else {
                                                sessionManager.createSession(name, email, phone, "", "");
                                                checkOrCreateKey(email);
                                                hideProgressDialog();
                                                Intent intent = new Intent(Login.this, Main.class);
                                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                startActivity(intent);
                                                finish();
                                            }

                                        } else {
                                            hideProgressDialog();
                                            Toast.makeText(Login.this, "Something went wrong...!", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        } else {
                            hideProgressDialog();
                            Toast.makeText(Login.this, "Something went wrong...!", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
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
                        SecretKey secretKey = KeyStoreHelper.getOrCreateKey(Login.this);
                        if (secretKey == null) {
                            Toast.makeText(Login.this, "Key generation error", Toast.LENGTH_SHORT).show();
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
                                .addOnFailureListener(e -> Toast.makeText(Login.this, "Failed to upload key!", Toast.LENGTH_SHORT).show());
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



}