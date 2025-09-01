package com.abebegetachew.abebe.salarymanagement.abebegetachew;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageButton;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Register extends BaseActivity {

    AppCompatImageButton toggleBtn;
    EditText passwordEditText, name1, email1, phone1;
    FirebaseAuth auth;
    SessionManager sessionManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        toggleBtn = findViewById(R.id.toggleButton);
        passwordEditText = findViewById(R.id.password);
        name1 = findViewById(R.id.name);
        email1 = findViewById(R.id.email);
        phone1 = findViewById(R.id.mobile_number);

        auth = FirebaseAuth.getInstance();
        sessionManager = new SessionManager(Register.this);

        final Typeface originalTypeface = passwordEditText.getTypeface();
        final float originalTextSize = passwordEditText.getTextSize();
        final int originalTextColor = passwordEditText.getCurrentTextColor();

        toggleBtn.setOnClickListener(v -> {
            int inputType = passwordEditText.getInputType();
            boolean isPasswordVisible = (inputType == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD));

            if (isPasswordVisible) {
                passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                toggleBtn.setImageResource(R.drawable.show);
            } else {
                passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                toggleBtn.setImageResource(R.drawable.hide);
            }

            passwordEditText.setTypeface(originalTypeface);
            passwordEditText.setTextSize(TypedValue.COMPLEX_UNIT_PX, originalTextSize);
            passwordEditText.setTextColor(originalTextColor);
            passwordEditText.setSelection(passwordEditText.getText().length());
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

    public void go_to_login(View view) {
        Intent intent = new Intent(Register.this, Login.class);
        startActivity(intent);
    }

    public void register(View view) {
        String name = name1.getText().toString();
        String password = passwordEditText.getText().toString();
        String email = email1.getText().toString();
        String phone = phone1.getText().toString();

        if (!InputValidator.isValidUsername(name)) {
            name1.setText("");
            name1.setHintTextColor(Color.parseColor("#FF80A0"));
            name1.setHint("Invalid username");
            return;
        }

        if (!InputValidator.isValidEmail(email)) {
            email1.setText("");
            email1.setHintTextColor(Color.parseColor("#FF80A0"));
            email1.setHint("Invalid email");
            return;
        }

        if (!InputValidator.isValidPassword(password)) {
            passwordEditText.setText("");
            passwordEditText.setHintTextColor(Color.parseColor("#FF80A0"));
            passwordEditText.setHint("Weak password");
            Toast.makeText(getApplicationContext(), "Weak password", Toast.LENGTH_LONG).show();
            return;
        }

        if (!InputValidator.isValidPhone(phone)) {
            phone1.setText("");
            phone1.setHintTextColor(Color.parseColor("#FF80A0"));
            phone1.setHint("Invalid phone number");
            return;
        }

        if (InputValidator.isPasswordEmpty(password)) {
            passwordEditText.setText("");
            passwordEditText.setHintTextColor(Color.parseColor("#FF80A0"));
            passwordEditText.setHint("Invalid password");
            return;
        }

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            progressDialog.dismiss();
            Toast.makeText(this, "Registration Failed...", Toast.LENGTH_SHORT).show();
            return;
        }

        currentUser.getIdToken(true)
                .addOnSuccessListener(getTokenResult -> {
                    String idToken = getTokenResult.getToken();

                    OkHttpClient client = new OkHttpClient();

                    JSONObject json = new JSONObject();
                    try {
                        json.put("email", email);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        progressDialog.dismiss();
                        return;
                    }

                    RequestBody body = RequestBody.create(json.toString(), MediaType.parse("application/json"));
                    Request request = new Request.Builder()
                            .url("https://otp-backend2-six.vercel.app/api/checkUser")
                            .addHeader("Authorization", "Bearer " + idToken)
                            .post(body)
                            .build();

                    client.newCall(request).enqueue(new Callback() {
                        @Override
                        public void onFailure(@NonNull Call call, @NonNull IOException e) {
                            runOnUiThread(() -> {
                                progressDialog.dismiss();
                                Toast.makeText(Register.this, "Server error", Toast.LENGTH_SHORT).show();
                            });
                        }

                        @Override
                        public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                            String resString = response.body().string();

                            if (!response.isSuccessful()) {
                                runOnUiThread(() -> {
                                    progressDialog.dismiss();
                                    Toast.makeText(Register.this, "Server error", Toast.LENGTH_SHORT).show();
                                });
                                return;
                            }

                            try {
                                JSONObject resJson = new JSONObject(resString);
                                boolean allow = resJson.getBoolean("allow");

                                runOnUiThread(() -> {
                                    if (!allow) {
                                        progressDialog.dismiss();
                                        String message = resJson.optString("message", "Registration blocked");
                                        Toast.makeText(Register.this, message, Toast.LENGTH_LONG).show();
                                        return;
                                    }

                                    // Device allowed: proceed with Firebase registration logic
                                    proceedWithFirebaseTempUserLogic(progressDialog, email, name, phone, password);
                                });
                            } catch (JSONException e) {
                                e.printStackTrace();
                                runOnUiThread(() -> {
                                    progressDialog.dismiss();
                                    Toast.makeText(Register.this, "Invalid server response", Toast.LENGTH_SHORT).show();
                                });
                            }
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(Register.this, "Failed to get token", Toast.LENGTH_SHORT).show();
                });
    }

    private void proceedWithFirebaseTempUserLogic(ProgressDialog progressDialog, String email, String name, String phone, String password) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference docRef = db.collection("TempUser").document(email);

        docRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                if (documentSnapshot.contains("is_paid")) {
                    FirebaseAuth.getInstance().getCurrentUser().getIdToken(true)
                            .addOnCompleteListener(tokenTask -> {
                                if (tokenTask.isSuccessful()) {
                                    OTPRequestSender.sendOTPRequest(Register.this, email, name);

                                    docRef.update("newOTP", true)
                                            .addOnSuccessListener(unused -> {
                                                progressDialog.dismiss();
                                                sessionManager.add_temp(name, password, phone, email, false);
                                                Intent intent = new Intent(Register.this, Verification.class);
                                                intent.putExtra("Email", email);
                                                startActivity(intent);
                                            })
                                            .addOnFailureListener(e -> progressDialog.dismiss());

                                } else {
                                    progressDialog.dismiss();
                                }
                            });
                } else {
                    progressDialog.dismiss();
                    sessionManager.add_temp(name, password, phone, email, false);
                    Intent intent = new Intent(Register.this, Pending.class);
                    intent.putExtra("Email", email);
                    startActivity(intent);
                }
            }
        }).addOnFailureListener(e -> {
            HashMap<String, Object> user = new HashMap<>();
            user.put("Username", name);
            user.put("Email", email);
            user.put("Phone", phone);
            user.put("Profile", "");
            user.put("newOTP", false);

            docRef.set(user).addOnCompleteListener(task -> {
                progressDialog.dismiss();
                if (task.isSuccessful()) {
                    sessionManager.add_temp(name, password, phone, email, false);
                    Intent intent = new Intent(Register.this, Pending.class);
                    intent.putExtra("Email", email);
                    startActivity(intent);
                } else {
                    Toast.makeText(Register.this, "Something went wrong!", Toast.LENGTH_LONG).show();
                }
            });
        });
    }
}
