package com.abebegetachew.abebe.salarymanagement.abebegetachew;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
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
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.signature.ObjectKey;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
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
import javax.crypto.spec.SecretKeySpec;

import de.hdodenhof.circleimageview.CircleImageView;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class EditProfile extends BaseActivity {

    private static final String CLOUD_NAME = "df0bydveu";
    private static final String UPLOAD_PRESET = "unsigned_profile";
    private static final String CLOUDINARY_UPLOAD_URL =
            "https://api.cloudinary.com/v1_1/" + CLOUD_NAME + "/image/upload";

    private static final String HMAC_SECRET = "a3f5c7e2d9a1b478e4f62d1349b8c51f3a7c45b6d29e8f7c8b9a1d2e3f4b5678";

    private AlertDialog customProgressDialog;
    private ProgressBar uploadProgressBar;
    private TextView uploadProgressText;

    private CircleImageView profileImage;
    private EditText editUsername, editPhone;
    private ImageButton backButton;
    private Button saveButton;

    private Uri profileUri;
    private Uri cameraImageUri;

    private ActivityResultLauncher<String> permissionCameraLauncher;
    private ActivityResultLauncher<String> permissionReadLauncher;
    private ActivityResultLauncher<Uri> takePictureLauncher;
    private ActivityResultLauncher<String> pickFromGalleryLauncher;
    private ActivityResultLauncher<Intent> cropLauncher;

    private AlertDialog progressDialog;

    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_profile);

        sessionManager = new SessionManager(this);

        profileImage = findViewById(R.id.profileImage);
        editUsername = findViewById(R.id.editUsername);
        editPhone = findViewById(R.id.editPhone);
        backButton = findViewById(R.id.backButton);
        saveButton = findViewById(R.id.saveButton);

        backButton.setOnClickListener(v -> finish());

        initActivityResultLaunchers();
        preloadSessionData();

        profileImage.setOnClickListener(v -> showImagePickerBottomSheet());

        saveButton.setOnClickListener(v -> saveProfileChanges());
    }

    private void preloadSessionData() {
        // Load profile image
        String profilePath = sessionManager.getProfileImagePath();
        if (profilePath != null && !profilePath.isEmpty()) {
            Glide.with(this).load(profilePath)
                    .placeholder(R.drawable.picture)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .error(R.drawable.picture)
                    .into(profileImage);
        } else profileImage.setImageResource(R.drawable.picture);

        // Load username & phone
        String fullName = sessionManager.getSessionDetails("key_session_name");
        if (fullName != null) editUsername.setText(fullName.trim());

        String phone = sessionManager.getSessionDetails("key_session_phone");
        if (phone != null) editPhone.setText(phone.trim());
    }

    private void initActivityResultLaunchers() {
        permissionCameraLauncher =
                registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                    if (granted) openCamera();
                });

        permissionReadLauncher =
                registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                    if (granted) openGallery();
                });

        takePictureLauncher =
                registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                    if (success && cameraImageUri != null) startCrop(cameraImageUri);
                });

        pickFromGalleryLauncher =
                registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                    if (uri != null) startCrop(uri);
                });

        cropLauncher =
                registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri cropped = UCrop.getOutput(result.getData());
                        if (cropped != null) {
                            profileUri = cropped;
                            profileImage.setImageURI(profileUri);
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

    private void showCustomProgressDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.upload_progress_dialog, null);
        uploadProgressBar = dialogView.findViewById(R.id.progressBar);
        uploadProgressText = dialogView.findViewById(R.id.progressText);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
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


    private void requestCameraThenOpen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else permissionCameraLauncher.launch(Manifest.permission.CAMERA);
    }

    private void requestReadThenOpen() {
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(this, permission)
                == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            openGallery();
        } else permissionReadLauncher.launch(permission);
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

    // Inside saveProfileChanges() replace existing logic with:

    private void saveProfileChanges() {
        String newUsername = editUsername.getText().toString().trim();
        String newPhone = editPhone.getText().toString().trim();

        if (newUsername.isEmpty() || newPhone.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        showCustomProgressDialog();

        String oldProfileUrl = sessionManager.getProfileImageUri(); // old Cloudinary URL
        String localPath = sessionManager.getProfileImagePath();

        if (profileUri != null) {
            // Upload new image
            uploadToCloudinary(profileUri, uploadedUrl -> {
                // ✅ Delete old Cloudinary image if it's not the default placeholder
                if (oldProfileUrl != null
                        && !oldProfileUrl.isEmpty()
                        && !oldProfileUrl.equals("https://res.cloudinary.com/df0bydveu/image/upload/v1753610638/picture_ak2bqx.png")) {

                    String publicId = extractPublicIdFromUrl(oldProfileUrl);
                    if (publicId != null) {
                        deleteOldCloudinaryImage(publicId,
                                () -> Log.d("Cloudinary", "Old image deleted"),
                                () -> Log.w("Cloudinary", "Failed to delete old image"));
                    }
                }

                // Download image locally in background
                new Thread(() -> {
                    String savedPath = downloadImageToLocal(uploadedUrl, "profile_picture.jpg");

                    // Update session with new values
                    sessionManager.createSession(
                            newUsername,
                            sessionManager.getSessionDetails("key_session_email"),
                            newPhone,
                            uploadedUrl,   // New Cloudinary URL
                            savedPath      // Local path
                    );

                    // Update Firestore on UI thread
                    runOnUiThread(() -> updateFirestore(uploadedUrl, newUsername, newPhone));
                }).start();
            });
        } else {
            // ✅ No new image → just update username & phone in session
            sessionManager.createSession(
                    newUsername,
                    sessionManager.getSessionDetails("key_session_email"),
                    newPhone,
                    oldProfileUrl,
                    localPath
            );
            updateFirestore(oldProfileUrl, newUsername, newPhone);
        }
    }



    private void updateFirestore(String profileUrl, String username, String phone) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            dismissCustomProgressDialog();
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("User").document(currentUser.getUid())
                .update("Profile", profileUrl,
                        "Username", username,
                        "Phone", phone)
                .addOnSuccessListener(aVoid -> {
                    dismissCustomProgressDialog();
                    showSuccessDialog("Profile Updated!", "Your profile has been updated successfully.");

                    // Glide can load local path if offline
                    String localPath = sessionManager.getProfileImagePath();
                    Glide.with(EditProfile.this)
                            .load(localPath != null && !localPath.isEmpty() ? localPath : profileUrl)
                            .placeholder(R.drawable.picture)
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .skipMemoryCache(true)
                            .error(R.drawable.picture)
                            .into(profileImage);
                })
                .addOnFailureListener(e -> {
                    dismissCustomProgressDialog();
                    Toast.makeText(EditProfile.this, "Failed to update profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void showSuccessDialog(String title, String message) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_success, null);

        TextView dialogTitle = dialogView.findViewById(R.id.dialogTitle);
        TextView dialogMessage = dialogView.findViewById(R.id.dialogMessage);
        Button btnOk = dialogView.findViewById(R.id.btnOk);

        dialogTitle.setText(title);
        dialogMessage.setText(message);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        btnOk.setOnClickListener(v -> {
            dialog.dismiss();
            finish();
        });
        dialog.show();
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

    // Helper method to extract public_id from URL
    private String extractPublicIdFromUrl(String url) {
        try {
            String filename = url.substring(url.lastIndexOf("/") + 1); // e6y8vdaxztkrvmdm2nci.jpg
            return filename.substring(0, filename.lastIndexOf(".")); // e6y8vdaxztkrvmdm2nci
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Delete old Cloudinary image using unsigned API (requires admin endpoint or signed call)
    private void deleteOldCloudinaryImage(String publicId, Runnable onDeleted, Runnable onFailure) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            if (onFailure != null) onFailure.run();
            return;
        }

        currentUser.getIdToken(true).addOnCompleteListener(task -> {
            if (!task.isSuccessful() || task.getResult() == null || task.getResult().getToken() == null) {
                if (onFailure != null) onFailure.run();
                return;
            }
            String idToken = task.getResult().getToken();

            new Thread(() -> {
                try {
                    URL url = new URL("https://otp-backend2-six.vercel.app/api/deleteCloudinaryImage");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setRequestProperty("Authorization", "Bearer " + idToken);
                    conn.setDoOutput(true);

                    JSONObject json = new JSONObject();
                    json.put("publicId", publicId);

                    try (OutputStream os = conn.getOutputStream()) {
                        os.write(json.toString().getBytes(StandardCharsets.UTF_8));
                    }

                    int responseCode = conn.getResponseCode();
                    conn.disconnect();

                    if (responseCode == 200) {
                        runOnUiThread(onDeleted); // Only proceed if deletion succeeds
                    } else {
                        runOnUiThread(onFailure); // Stop if deletion fails
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(onFailure); // Stop if exception occurs
                }
            }).start();
        });
    }

    private void uploadToCloudinary(Uri imageUri, CloudinaryCallback callback) {
        File file;
        try {
            file = fileFromContentUri(this, imageUri);
        } catch (IOException e) {
            e.printStackTrace();
            dismissCustomProgressDialog();
            Toast.makeText(this, "Unable to read selected image", Toast.LENGTH_SHORT).show();
            return;
        }

        OkHttpClient client = new OkHttpClient();

        ProgressRequestBody fileBody = new ProgressRequestBody(file, "image/*", percent -> runOnUiThread(() -> updateCustomProgress(percent)));

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
                    dismissCustomProgressDialog();
                    Toast.makeText(EditProfile.this, "Upload failed", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> {
                        dismissCustomProgressDialog();
                        Toast.makeText(EditProfile.this, "Upload failed: " + response.code(), Toast.LENGTH_LONG).show();
                    });
                    return;
                }

                String responseData = response.body().string();
                try {
                    JSONObject json = new JSONObject(responseData);
                    String imageUrl = json.getString("secure_url");
                    runOnUiThread(() -> callback.onUploadComplete(imageUrl));
                } catch (JSONException e) {
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        dismissCustomProgressDialog();
                        Toast.makeText(EditProfile.this, "Parsing error", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private File fileFromContentUri(Context context, Uri contentUri) throws IOException {
        InputStream inputStream = context.getContentResolver().openInputStream(contentUri);
        if (inputStream == null) throw new IOException("Unable to open input stream from URI");

        File tempFile = File.createTempFile("upload_", ".jpg", context.getCacheDir());
        try (FileOutputStream out = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[8 * 1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) out.write(buffer, 0, bytesRead);
        }
        inputStream.close();
        return tempFile;
    }

    private void showProgressDialog() {
        if (progressDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            View view = LayoutInflater.from(this).inflate(R.layout.dialog_loading, null);
            builder.setView(view);
            builder.setCancelable(false);
            progressDialog = builder.create();
            if (progressDialog.getWindow() != null)
                progressDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        progressDialog.show();
    }

    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) progressDialog.dismiss();
    }

    interface CloudinaryCallback {
        void onUploadComplete(String url);
    }

    public static class ProgressRequestBody extends RequestBody {
        public interface UploadCallback { void onProgress(int percent); }
        private final File file;
        private final String contentType;
        private final UploadCallback callback;
        private static final int DEFAULT_BUFFER_SIZE = 2048;

        public ProgressRequestBody(File file, String contentType, UploadCallback callback) {
            this.file = file; this.contentType = contentType; this.callback = callback;
        }

        @Override public long contentLength() { return file.length(); }
        @Override public MediaType contentType() { return MediaType.parse(contentType); }

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
                    if (callback != null) callback.onProgress((int)((100*uploaded)/fileLength));
                }
            }
        }
    }
}
