package com.abebegetachew.abebe.salarymanagement.abebegetachew;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PasswordResetHelper {

    private static final String RESET_API_URL = "https://otp-backend2-six.vercel.app/api/requestReset";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    // Renamed interface to avoid clash with okhttp3.Callback
    public interface ResetCallback {
        void onSuccess();
        void onFailure(String errorMessage);
    }

    public static void sendResetRequest(Context context, String email, ResetCallback callback) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show();
            if (callback != null) callback.onFailure("User not logged in");
            return;
        }

        auth.getCurrentUser().getIdToken(true)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Toast.makeText(context, "Failed to get auth token", Toast.LENGTH_SHORT).show();
                        if (callback != null) callback.onFailure("Failed to get auth token");
                        return;
                    }

                    String idToken = task.getResult().getToken();

                    OkHttpClient client = new OkHttpClient();

                    JSONObject jsonBody = new JSONObject();
                    try {
                        jsonBody.put("email", email);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(context, "Invalid email", Toast.LENGTH_SHORT).show();
                        if (callback != null) callback.onFailure("Invalid email");
                        return;
                    }

                    RequestBody body = RequestBody.create(jsonBody.toString(), JSON);

                    Request request = new Request.Builder()
                            .url(RESET_API_URL)
                            .post(body)
                            .addHeader("Authorization", "Bearer " + idToken)
                            .build();

                    client.newCall(request).enqueue(new okhttp3.Callback() {  // Explicit okhttp3.Callback
                        @Override
                        public void onFailure(Call call, IOException e) {
                            e.printStackTrace();
                            ((android.app.Activity) context).runOnUiThread(() -> {
                                Toast.makeText(context, "Network error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                if (callback != null) callback.onFailure("Network error: " + e.getMessage());
                            });
                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            String respBody = response.body().string();

                            if (response.isSuccessful()) {
                                ((android.app.Activity) context).runOnUiThread(() -> {
                                    Intent intent = new Intent(context, ResetPasswordActivity.class);
                                    intent.putExtra("email", email);
                                    context.startActivity(intent);
                                    if (callback != null) callback.onSuccess();
                                });
                            } else {
                                String errorMsg = "Failed to send reset request";
                                try {
                                    JSONObject json = new JSONObject(respBody);
                                    if (json.has("error")) {
                                        errorMsg = json.getString("error");
                                    }
                                } catch (JSONException ignored) {}

                                final String finalErrorMsg = errorMsg;
                                ((android.app.Activity) context).runOnUiThread(() -> {
                                    Toast.makeText(context, finalErrorMsg, Toast.LENGTH_LONG).show();
                                    if (callback != null) callback.onFailure(finalErrorMsg);
                                });
                            }
                        }
                    });
                });
    }
}
