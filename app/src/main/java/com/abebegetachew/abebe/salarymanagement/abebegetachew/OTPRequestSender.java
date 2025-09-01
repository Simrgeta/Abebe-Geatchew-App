package com.abebegetachew.abebe.salarymanagement.abebegetachew;

import android.content.Context;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class OTPRequestSender {
    private static final String TAG = "OTPRequestSender";
    private static final String PIPE_URL = "https://otp-backend2-six.vercel.app/api/send-otp";

    public static void sendOTPRequest(Context context, String email, String username) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            // 👻 Sign in anonymously first
            auth.signInAnonymously()
                    .addOnSuccessListener(authResult -> {
                        FirebaseUser user = authResult.getUser();
                        sendToPipe(email, username, user);
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Anonymous sign-in failed", e));
        } else {
            sendToPipe(email, username, currentUser);
        }
    }

    private static void sendToPipe(String email, String username, FirebaseUser user) {
        user.getIdToken(true).addOnSuccessListener(result -> {
            String idToken = result.getToken();
            new Thread(() -> postToPipedream(email, username, idToken)).start();
        }).addOnFailureListener(e -> Log.e(TAG, "Failed to get ID token", e));
    }

    private static void postToPipedream(String email, String username, String idToken) {
        try {
            URL url = new URL(PIPE_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + idToken);
            conn.setDoOutput(true);

            JSONObject json = new JSONObject();
            json.put("email", email);
            json.put("username", username);

            OutputStream os = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
            writer.write(json.toString());
            writer.flush();
            writer.close();
            os.close();

            int responseCode = conn.getResponseCode();
            Log.d(TAG, "Response Code: " + responseCode);

        } catch (Exception e) {
            Log.e(TAG, "Error sending OTP request", e);
        }
    }
}
