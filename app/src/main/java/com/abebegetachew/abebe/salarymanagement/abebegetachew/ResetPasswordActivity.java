package com.abebegetachew.abebe.salarymanagement.abebegetachew;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ResetPasswordActivity extends BaseActivity {

    private EditText newPasswordInput, confirmPasswordInput, otpInput;
    private Button resetButton;
    private ProgressBar progressBar;
    private TextView emailText;

    private static final String CONFIRM_RESET_API_URL = "https://otp-backend2-six.vercel.app/api/confirmReset";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private String email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        newPasswordInput = findViewById(R.id.newPasswordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
        otpInput = findViewById(R.id.otpInput);
        resetButton = findViewById(R.id.resetButton);
        progressBar = findViewById(R.id.progressBar);
        emailText = findViewById(R.id.emailText);

        // Get email from Intent
        email = getIntent().getStringExtra("email");
        emailText.setText("Reset password for: \n" + email);

        resetButton.setOnClickListener(v -> resetPassword());
    }

    private void resetPassword() {
        String newPassword = newPasswordInput.getText().toString().trim();
        String confirmPassword = confirmPasswordInput.getText().toString().trim();
        String otp = otpInput.getText().toString().trim();


        if (InputValidator.isPasswordEmpty(newPassword)) {
            newPasswordInput.setText("");
            newPasswordInput.setHintTextColor(Color.parseColor("#FF80A0"));
            newPasswordInput.setHint("Invalid password");
            return;
        }


        if (!InputValidator.isValidPassword(newPassword)) {
            newPasswordInput.setText("");
            newPasswordInput.setHintTextColor(Color.parseColor("#FF80A0"));
            newPasswordInput.setHint("Weak password");
            Toast.makeText(getApplicationContext(), "Weak password", Toast.LENGTH_LONG).show();
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }
        if (otp.isEmpty()) {
            Toast.makeText(this, "Please enter OTP", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        resetButton.setEnabled(false);

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("email", email);
            jsonBody.put("otp", otp);
            jsonBody.put("newPassword", newPassword);
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "Invalid input data", Toast.LENGTH_SHORT).show();
            return;
        }

        OkHttpClient client = new OkHttpClient();
        RequestBody body = RequestBody.create(jsonBody.toString(), JSON);
        Request request = new Request.Builder()
                .url(CONFIRM_RESET_API_URL)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    resetButton.setEnabled(true);
                    Toast.makeText(ResetPasswordActivity.this, "Network error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String respBody = response.body().string();
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    resetButton.setEnabled(true);
                    if (response.isSuccessful()) {
                        Toast.makeText(ResetPasswordActivity.this, "Password reset successful! You can now log in.", Toast.LENGTH_LONG).show();
                        finish(); // close this activity and return to login
                    } else {
                        String errorMsg = "Failed to reset password";
                        try {
                            JSONObject json = new JSONObject(respBody);
                            if (json.has("error")) {
                                errorMsg = json.optString("error");
                            }
                        } catch (JSONException ignored) {}
                        Toast.makeText(ResetPasswordActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }
}
