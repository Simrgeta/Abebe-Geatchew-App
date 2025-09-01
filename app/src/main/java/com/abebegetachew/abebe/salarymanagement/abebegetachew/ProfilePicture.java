package com.abebegetachew.abebe.salarymanagement.abebegetachew;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.yalantis.ucrop.UCrop;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import android.util.Base64;

// If you want to show the picture from URL, add Glide dependency and import it:
// implementation 'com.github.bumptech.glide:glide:4.16.0'
// import com.bumptech.glide.Glide;

public class ProfilePicture extends BaseActivity {

    // ====== Cloudinary Config ======
    private static final String CLOUD_NAME = "df0bydveu";           // <-- put yours
    private static final String UPLOAD_PRESET = "unsigned_profile";  // <-- create in Cloudinary (unsigned)
    private static final String CLOUDINARY_UPLOAD_URL =
            "https://api.cloudinary.com/v1_1/" + CLOUD_NAME + "/image/upload";

    private Uri profile_uri; // Will hold the cropped image uri (as you asked)
    private ImageView profileImage;
    private Uri cameraImageUri;

    // --- Launchers ---
    private ActivityResultLauncher<String> permissionCameraLauncher;
    private ActivityResultLauncher<String> permissionReadLauncher;
    private ActivityResultLauncher<Uri> takePictureLauncher;
    private ActivityResultLauncher<String> pickFromGalleryLauncher;
    private ActivityResultLauncher<Intent> cropLauncher;

    private String email;

    SessionManager sessionManager;
    private AlertDialog progressDialog;

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
        setContentView(R.layout.activity_profile_picture);

        profileImage = findViewById(R.id.profileImage);

        initActivityResultLaunchers();

        profileImage.setOnClickListener(v -> showImagePickerBottomSheet());

        sessionManager = new SessionManager(ProfilePicture.this);

        email = getIntent().getStringExtra("Email");

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



    private void initActivityResultLaunchers() {
        // Camera permission
        permissionCameraLauncher =
                registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                    if (granted) openCamera();
                });

        // Read permission
        permissionReadLauncher =
                registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                    if (granted) openGallery();
                });

        // Camera capture
        takePictureLauncher =
                registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                    if (success && cameraImageUri != null) {
                        startCrop(cameraImageUri);
                    }
                });

        // Gallery picker
        pickFromGalleryLauncher =
                registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                    if (uri != null) startCrop(uri);
                });

        // uCrop result
        cropLauncher =
                registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri cropped = UCrop.getOutput(result.getData());
                        if (cropped != null) {
                            // >>> set profile_uri to the cropped one (what you asked)
                            profile_uri = cropped;
                            profileImage.setImageURI(profile_uri);
                        }
                    }
                });
    }

    private void showImagePickerBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_image_picker, null);
        dialog.setContentView(view);

        Button btnCamera = view.findViewById(R.id.btnCamera);
        Button btnGallery = view.findViewById(R.id.btnGallery);

        btnCamera.setOnClickListener(v -> {
            dialog.dismiss();
            requestCameraThenOpen();
        });

        btnGallery.setOnClickListener(v -> {
            dialog.dismiss();
            requestReadThenOpen();
        });

        dialog.show();
    }

    private void requestCameraThenOpen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else {
            permissionCameraLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void requestReadThenOpen() {
        String permission =
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                        ? Manifest.permission.READ_MEDIA_IMAGES
                        : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(this, permission)
                == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            openGallery();
        } else {
            permissionReadLauncher.launch(permission);
        }
    }

    private void openCamera() {
        try {
            File imageFile = File.createTempFile("profile_", ".jpg", getCacheDir());
            cameraImageUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".provider",
                    imageFile
            );
            takePictureLauncher.launch(cameraImageUri);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openGallery() {
        pickFromGalleryLauncher.launch("image/*");
    }

    private void startCrop(@NonNull Uri sourceUri) {
        Uri destinationUri = Uri.fromFile(new File(getCacheDir(),
                "cropped_" + System.currentTimeMillis() + ".jpg"));

        UCrop.Options options = new UCrop.Options();
        options.setCompressionFormat(Bitmap.CompressFormat.JPEG);
        options.setFreeStyleCropEnabled(true);
        options.setHideBottomControls(false);
        options.setToolbarTitle("Edit image");

        Intent uCrop = UCrop.of(sourceUri, destinationUri)
                .withOptions(options)
                .withAspectRatio(1, 1)
                .getIntent(this);

        cropLauncher.launch(uCrop);
    }

    // Call this from your button (e.g., "Continue/Upload" button) in XML onClick
    public void go_to_verification(View view) {
        if (profile_uri == null) {
            // Use default profile image URL
            showProgressDialog();
            String defaultUrl = "https://res.cloudinary.com/df0bydveu/image/upload/v1753610638/picture_ak2bqx.png";
            addToDB(defaultUrl);
        } else {
            uploadToCloudinary(profile_uri);
        }
    }

    private android.app.AlertDialog customProgressDialog;
    private ProgressBar uploadProgressBar;
    private TextView uploadProgressText;

    private void showCustomProgressDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.upload_progress_dialog, null);
        uploadProgressBar = dialogView.findViewById(R.id.progressBar);
        uploadProgressText = dialogView.findViewById(R.id.progressText);

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setView(dialogView);
        builder.setCancelable(false);
        customProgressDialog = builder.create();
        customProgressDialog.show();
    }

    private void updateCustomProgress(int percent) {
        if (uploadProgressBar != null && uploadProgressText != null) {
            uploadProgressBar.setProgress(percent);
            uploadProgressText.setText(percent + "%");
        }
    }

    private void dismissCustomProgressDialog() {
        if (customProgressDialog != null && customProgressDialog.isShowing()) {
            customProgressDialog.dismiss();
        }
    }


    private void uploadToCloudinary(Uri imageUri) {
        File file;
        try {
            file = fileFromContentUri(this, imageUri);

            // Rename file to avoid conflicts if multiple uploads happen
            String uniqueName = "upload_" + System.currentTimeMillis() + "_" + file.getName();
            File renamedFile = new File(file.getParent(), uniqueName);
            if (!file.renameTo(renamedFile)) {
                Toast.makeText(this, "Failed to prepare image for upload.", Toast.LENGTH_SHORT).show();
                return;
            }
            file = renamedFile;

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Unable to read selected image.", Toast.LENGTH_SHORT).show();
            return;
        }

        showCustomProgressDialog();

        OkHttpClient client = new OkHttpClient();

        ProgressRequestBody fileBody = new ProgressRequestBody(file, "image/*", percent ->
                runOnUiThread(() -> updateCustomProgress(percent))
        );

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.getName(), fileBody)
                .addFormDataPart("upload_preset", UPLOAD_PRESET)
                .build();

        Request request = new Request.Builder()
                .url(CLOUDINARY_UPLOAD_URL)
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(ProfilePicture.this, "Upload failed", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> {
                        Toast.makeText(ProfilePicture.this, "Upload failed: " + response.code(), Toast.LENGTH_LONG).show();
                    });
                    return;
                }

                String responseData = response.body().string();
                try {
                    JSONObject json = new JSONObject(responseData);
                    String imageUrl = json.getString("secure_url");
                    Log.d("Cloudinary", "Image URL: " + imageUrl);

                    runOnUiThread(() -> {
                        addToDB(imageUrl);
                    });

                } catch (JSONException e) {
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        Toast.makeText(ProfilePicture.this, "Parsing error", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    /**
     * Copies the Uri's content to a temp file so we can get a real File to upload (works for all API levels).
     */
    private File fileFromContentUri(Context context, Uri contentUri) throws IOException {
        ContentResolver resolver = context.getContentResolver();
        InputStream inputStream = resolver.openInputStream(contentUri);
        if (inputStream == null) throw new IOException("Unable to open input stream from URI");

        File tempFile = File.createTempFile("upload_", ".jpg", context.getCacheDir());
        FileOutputStream out = new FileOutputStream(tempFile);

        byte[] buffer = new byte[8 * 1024];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
        }
        out.flush();
        out.close();
        inputStream.close();

        return tempFile;
    }

    // (Optional) Kept for backward compatibility if you still want it; not used now.
    @Deprecated
    private String getRealPathFromURI(Uri contentUri) {
        String[] proj = { MediaStore.Images.Media.DATA };
        Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null);
        if (cursor == null) return contentUri.getPath();
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String path = cursor.getString(column_index);
        cursor.close();
        return path;
    }


    private void addToDB(String profileUri) {
        if (email == null || email.isEmpty()) {
            Toast.makeText(this, "Something went wrong!", Toast.LENGTH_SHORT).show();
            return;
        }

        String password = sessionManager.getTempPassword();
        if (password == null || password.isEmpty()) {
            Toast.makeText(this, "Something went wrong!", Toast.LENGTH_SHORT).show();
            return;
        }

        dismissCustomProgressDialog();
        showProgressDialog();

        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser != null && currentUser.isAnonymous()) {
            currentUser.delete().addOnCompleteListener(deleteTask -> {
                if (deleteTask.isSuccessful()) {
                    createUserAndRegister(profileUri, password);
                } else {
                    hideProgressDialog();
                    }
            });
        } else {
            createUserAndRegister(profileUri, password);
        }
    }

    private void createUserAndRegister(String profileUri, String password) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(authTask -> {
                    if (authTask.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user == null) {
                            hideProgressDialog();
                            Toast.makeText(this, "Registration failed", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // ✅ Get Firebase ID token
                        user.getIdToken(true).addOnCompleteListener(tokenTask -> {
                            if (!tokenTask.isSuccessful()) {
                                hideProgressDialog();
                                return;
                            }

                            String idToken = tokenTask.getResult().getToken();

                            String uid = user.getUid();
                            String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
                            long timestamp = System.currentTimeMillis();
                            String signature = hmacSHA256Hex(HMAC_SECRET, deviceId);

                            if (signature == null) {
                                hideProgressDialog();
                                return;
                            }

                            JSONObject payload = new JSONObject();
                            try {
                                payload.put("email", email);
                                payload.put("uid", uid);  // Include UID
                                payload.put("profileUri", profileUri);
                                payload.put("deviceId", deviceId);
                                payload.put("timestamp", timestamp);
                                payload.put("signature", signature);
                            } catch (JSONException e) {
                                e.printStackTrace();
                                hideProgressDialog();
                                return;
                            }

                            // ✅ Call backend with token in header
                            postToVercelAPI(payload, idToken, profileUri);
                        });

                    } else {
                        hideProgressDialog();
                        Toast.makeText(this, "Authentication failed: " + authTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void postToVercelAPI(JSONObject payload, String idToken, String profile) {
        OkHttpClient client = new OkHttpClient();
        RequestBody body = RequestBody.create(payload.toString(), MediaType.parse("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url("https://otp-backend2-six.vercel.app/api/registerUser") // ✅ Your backend URL
                .addHeader("Authorization", "Bearer " + idToken)             // ✅ Pass ID token in header
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    hideProgressDialog();
                    Toast.makeText(ProfilePicture.this, "Network error. Try again.", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> Toast.makeText(ProfilePicture.this, "Server error: " + response.code(), Toast.LENGTH_LONG).show());
                    return;
                }

                try {
                    String respStr = response.body().string();
                    JSONObject respJson = new JSONObject(respStr);
                    boolean success = respJson.optBoolean("success", false);
                    if (success) {
                        if (profile != null && !profile.isEmpty()) {
                            // ✅ Download and save to local directory
                            String savedPath = downloadImageToLocal(profile, "profile_picture.jpg");

                            runOnUiThread(() -> {
                                sessionManager.promoteTempToSession(profile, savedPath); // ✅ Mark user as fully registered
                                checkOrCreateKey(email);

                                hideProgressDialog();

                                Intent intent = new Intent(ProfilePicture.this, Main.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            });
                        } else {
                            runOnUiThread(() -> Toast.makeText(ProfilePicture.this, "No profile picture URL found", Toast.LENGTH_LONG).show());
                        }
                    } else {
                        String error = respJson.optString("error", "Unknown error");
                        runOnUiThread(() -> Toast.makeText(ProfilePicture.this, error, Toast.LENGTH_LONG).show());
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    runOnUiThread(() -> Toast.makeText(ProfilePicture.this, "Invalid response from server", Toast.LENGTH_SHORT).show());
                }
            }
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




    // ---- Helper to track upload progress with OkHttp ----
    public static class ProgressRequestBody extends RequestBody {

        public interface UploadCallback {
            void onProgress(int percent);
        }

        private final File file;
        private final String contentType;
        private final UploadCallback callback;
        private static final int DEFAULT_BUFFER_SIZE = 2048;

        public ProgressRequestBody(File file, String contentType, UploadCallback callback) {
            this.file = file;
            this.contentType = contentType;
            this.callback = callback;
        }

        @Override
        public long contentLength() {
            return file.length();
        }

        @Override
        public MediaType contentType() {
            return MediaType.parse(contentType);
        }

        @Override
        public void writeTo(@NonNull okio.BufferedSink sink) throws IOException {
            long fileLength = file.length();
            byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
            try (java.io.FileInputStream in = new java.io.FileInputStream(file)) {
                long uploaded = 0;
                int read;
                while ((read = in.read(buffer)) != -1) {
                    uploaded += read;
                    sink.write(buffer, 0, read);
                    int progress = (int) ((100 * uploaded) / fileLength);
                    if (callback != null) callback.onProgress(progress);
                }
            }
        }
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
                        SecretKey secretKey = KeyStoreHelper.getOrCreateKey(ProfilePicture.this);
                        if (secretKey == null) {
                            Toast.makeText(ProfilePicture.this, "Key generation error", Toast.LENGTH_SHORT).show();
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
                                .addOnFailureListener(e -> Toast.makeText(ProfilePicture.this, "Failed to upload key!", Toast.LENGTH_SHORT).show());
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
