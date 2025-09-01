package com.abebegetachew.abebe.salarymanagement.abebegetachew;

import android.content.Context;
import android.util.Base64;

import java.security.SecureRandom;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class KeyStoreHelper {
    private static final String PREF_KEY = "hsgcfzdzd6788943HHANLOO0065FTrshfhv";

    // Generate and save key if not exists
    public static SecretKey getOrCreateKey(Context context) {
        SecretKey key = getKeyFromPrefs(context);
        if (key != null) return key; // key already exists

        try {
            // Use a more robust SecureRandom initialization
            SecureRandom secureRandom = SecureRandom.getInstanceStrong();
            byte[] keyBytes = new byte[32]; // 256 bits
            secureRandom.nextBytes(keyBytes);
            key = new SecretKeySpec(keyBytes, "AES");

            saveKeyToPrefs(context, key); // save for future
            return key;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Save key to SharedPreferences (Base64)
    public static void saveKeyToPrefs(Context context, SecretKey key) {
        try {
            String keyBase64 = Base64.encodeToString(key.getEncoded(), Base64.NO_WRAP);
            context.getSharedPreferences("APP_PREFS", Context.MODE_PRIVATE)
                    .edit()
                    .putString(PREF_KEY, keyBase64)
                    .apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Retrieve key from SharedPreferences

    public static SecretKey getKeyFromPrefs(Context context) {
        try {
            // Use the same SharedPreferences name and key as getSavedKey()
            String keyBase64 = context.getSharedPreferences("new_session", Context.MODE_PRIVATE)
                    .getString("encryption_key", null);
            if (keyBase64 == null) return null;

            byte[] keyBytes = Base64.decode(keyBase64, Base64.NO_WRAP);
            return new SecretKeySpec(keyBytes, "AES");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
