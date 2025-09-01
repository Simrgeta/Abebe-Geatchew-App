package com.abebegetachew.abebe.salarymanagement.abebegetachew;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;

import com.google.firebase.auth.FirebaseAuth;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class SessionManager {

    Context context;

    SharedPreferences sp;

    private final String KEY_NAME = "key_session_name";

    private final String PREF_FILE_NAME = "new_session";

    private final int PRIVATE_MODE = 0;

    private final String KEY_IF_LOGGED_IN = "key_if_logged_in";
    private final String KEY_EMAIL = "key_session_email";
    private final String KEY_PHONE = "key_session_phone";
    private final String KEY_PROFILE_URI = "key_session_profile_uri";
    private final String KEY_PROFILE = "key_session_profile";
    private static final String HMAC_SECRET = "a3f5c7e2d9a1b478e4f62d1349b8c51f3a7c45b6d29e8f7c8b9a1d2e3f4b5678";

    public static String hmacSHA256Hex(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b & 0xff));
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    SharedPreferences.Editor editor;

    public SessionManager(Context context)
    {
        this.context = context;
        sp = context.getSharedPreferences(PREF_FILE_NAME, PRIVATE_MODE);
        editor = sp.edit();
    }

    public void checkSession() {
        // Check if user is logged in first
        boolean isLoggedIn = sp.getBoolean(KEY_IF_LOGGED_IN, false);
        if (!isLoggedIn) {
            Intent intent = new Intent(context, Login.class);
            context.startActivity(intent);
            ((Activity) context).finish();
            return;
        }

        // Logged in → run device verification first, then proceed
        new Thread(() -> {
            try {
                String email = sp.getString(KEY_EMAIL, null);
                if (email == null) throw new Exception("Email missing");

                String deviceId = android.provider.Settings.Secure.getString(context.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);

                String signature = hmacSHA256Hex(HMAC_SECRET, deviceId);
                if (signature == null) throw new Exception("Signature failed");


                final String[] idTokenHolder = new String[1];
                final Exception[] tokenException = new Exception[1];
                final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);

                FirebaseAuth.getInstance().getCurrentUser().getIdToken(true)
                        .addOnSuccessListener(result -> {
                            idTokenHolder[0] = result.getToken();
                            latch.countDown();
                        })
                        .addOnFailureListener(e -> {
                            tokenException[0] = e;
                            latch.countDown();
                        });

                latch.await(); // Wait for token retrieval

                if (tokenException[0] != null || idTokenHolder[0] == null) {
                    throw new Exception("Failed to get Firebase token");
                }

                String idToken = idTokenHolder[0];

                URL url = new URL("https://otp-backend2-six.vercel.app/api/splashCheck");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "Bearer " + idToken);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JSONObject json = new JSONObject();
                json.put("email", email);
                json.put("deviceId", deviceId);
                json.put("signature", signature);

                OutputStream os = conn.getOutputStream();
                os.write(json.toString().getBytes());
                os.close();

                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();

                JSONObject resp = new JSONObject(sb.toString());
                boolean allow = resp.optBoolean("allow", false);

                new Handler(context.getMainLooper()).post(() -> {
                    if (allow) {
                        Intent intent = new Intent(context, Main.class);
                        context.startActivity(intent);
                        ((Activity) context).finish();
                    } else {
                        // Device not allowed → logout + go to splash/login
                        FirebaseAuth.getInstance().signOut();
                        logoutSession();
                    }
                });

            } catch (Exception e) {
                Intent intent = new Intent(context, Login.class);
                context.startActivity(intent);
                ((Activity) context).finish();
            }
        }).start();
    }


    public void createSession(String name, String email, String phone, String profileUri, String profile)
    {
        editor.putString(KEY_NAME, name);
        editor.putString(KEY_EMAIL, email);
        editor.putString(KEY_PHONE, phone);
        editor.putString(KEY_PROFILE_URI, profileUri);
        editor.putString(KEY_PROFILE, profile);
        editor.putBoolean(KEY_IF_LOGGED_IN, true);
        editor.commit();
    }

    public void add_temp(String name, String password, String phone, String email, boolean is_paid) {
        editor.putString("name", name);
        editor.putString("password", password);
        editor.putString("email", email);
        editor.putString("phone", phone);
        editor.putBoolean("is_paid", is_paid);
        editor.commit();
    }

    public String getProfileImagePath() {
        return sp.getString(KEY_PROFILE, "");
    }

    public String getProfileImageUri() {
        return sp.getString(KEY_PROFILE_URI, "");
    }


    public void promoteTempToSession(String profileUri, String profile) {
        String name = sp.getString("name", null);
        String email = sp.getString("email", null);
        String phone = sp.getString("phone", null);

        if (name != null && email != null && phone != null && profile != null && profileUri != null) {
            createSession(name, email, phone, profileUri, profile);

            // Clear temp data
            editor.remove("name");
            editor.remove("password");
            editor.remove("email");
            editor.remove("phone");
            editor.remove("is_paid");
            editor.commit();
        }
    }

    // Added getter for temp password
    public String getTempPassword() {
        return sp.getString("password", null);
    }

    // Added getter for temp email (optional convenience)
    public String getTempEmail() {
        return sp.getString("email", null);
    }

    public void logoutSession()
    {
        editor.clear();
        editor.commit();
        Intent intent = new Intent(context, Login.class);
        context.startActivity(intent);
        ((Activity) context).finish();

    }

    public void logout()
    {
        editor.clear();
        editor.commit();
        Intent intent = new Intent(context, Splash.class);
        context.startActivity(intent);
        ((Activity) context).finish();

    }

    public String getSessionDetails(String key)
    {
        return sp.getString(key, null);
    }

    public boolean isLoggedIn()
    {
        return sp.getBoolean("key_if_logged_in", false);
    }

    public void clearSession() {
        editor.clear();
        editor.commit();
    }

    // Update only name
    public void updateName(String name) {
        if (name != null) {
            editor.putString(KEY_NAME, name);
            editor.apply();
        }
    }

    // Update only phone
    public void updatePhone(String phone) {
        if (phone != null) {
            editor.putString(KEY_PHONE, phone);
            editor.apply();
        }
    }

    // Update only profile (url + optionally cloudinary public_id)
    public void updateProfile(String profileUrl) {
        if (profileUrl != null) {
            editor.putString(KEY_PROFILE, profileUrl);
            editor.apply();
        }
    }

    // If you want to track Cloudinary public_id for deletion later
    public void updateProfile(String profileUrl, String publicId) {
        if (profileUrl != null) editor.putString(KEY_PROFILE, profileUrl);
        if (publicId != null) editor.putString("profile_public_id", publicId);
        editor.apply();
    }

    // Save encryption key to SharedPreferences
    public void saveKeyToPrefs(String keyBase64) {
        if (keyBase64 != null) {
            editor.putString("encryption_key", keyBase64);
            editor.apply();
        }
    }

    // Retrieve encryption key from SharedPreferences
    public String getSavedKey() {
        return sp.getString("encryption_key", null);
    }

}
