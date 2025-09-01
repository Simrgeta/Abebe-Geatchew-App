package com.abebegetachew.abebe.salarymanagement.abebegetachew;

import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DatabaseReference;
import android.util.Base64;
import android.util.Log;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.Uri;
import com.google.firebase.firestore.FirebaseFirestore;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.FileProvider;

import com.dropbox.core.android.Auth;
import com.dropbox.core.oauth.DbxCredential;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.files.FileMetadata;
import com.sanojpunchihewa.glowbutton.GlowButton;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;

public class Export extends AppCompatActivity {

    private RadioGroup exportTypeGroup, cloudChoiceGroup;
    private RadioButton radioLocal, radioCloud, radioGoogleDrive, radioDropbox;
    private LinearLayout localLayout;
    private GlowButton btnExport;
    private EditText editFileName;
    private ImageButton backButton;
    private AlertDialog progressDialog;

    private byte[] pendingExportData;
    private String backupFileName;

    private static final String DROPBOX_APP_KEY = "pyyrjk8se1edfgq";
    private DbxClientV2 dropboxClient;
    private DbxCredential dropboxCredential;
    private boolean isExporting = false;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private ActivityResultLauncher<String> createFileLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_export);

        bindViews();
        animateCard();

        DbxCredential dbxCredential = loadDropboxCredential();
        if (dbxCredential != null) initDropboxClient(dbxCredential);

        backButton.setOnClickListener(v -> finish());

        exportTypeGroup.setOnCheckedChangeListener((group, checkedId) ->
                updateLayoutForExportType(checkedId == R.id.radioCloud)
        );

        // Register modern ActivityResult launcher for local export
        createFileLauncher = registerForActivityResult(
                new ActivityResultContracts.CreateDocument("application/x-abms"),
                uri -> {
                    if (uri != null && pendingExportData != null) {
                        executor.execute(() -> saveToUri(uri));
                    } else {
                        resetExport();
                    }
                }
        );

        btnExport.setOnClickListener(v -> startExport());
    }

    private void bindViews() {
        exportTypeGroup = findViewById(R.id.exportTypeGroup);
        radioLocal = findViewById(R.id.radioLocal);
        radioCloud = findViewById(R.id.radioCloud);
        localLayout = findViewById(R.id.localLayout);
        btnExport = findViewById(R.id.btnExport);
        editFileName = findViewById(R.id.editFileName);
        backButton = findViewById(R.id.backButton);
        cloudChoiceGroup = findViewById(R.id.cloudChoiceGroup);
        radioGoogleDrive = findViewById(R.id.radioGoogleDrive);
        radioDropbox = findViewById(R.id.radioDropbox);
    }

    private void animateCard() {
        CardView exportCard = findViewById(R.id.exportCard);
        Animation cardAnim = AnimationUtils.loadAnimation(this, R.anim.card_slide_fade_in);
        exportCard.startAnimation(cardAnim);
    }

    private void startExport() {
        if (isExporting) return;

        try {
            pendingExportData = getEncryptedDatabaseBytes();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to prepare backup: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }

        backupFileName = getBackupFileName();
        isExporting = true;
        btnExport.setEnabled(false);

        if (radioLocal.isChecked()) exportLocalFile();
        else if (radioCloud.isChecked()) {
            if (radioGoogleDrive.isChecked()) exportToGoogleDrive();
            else if (radioDropbox.isChecked()) {
                if (!isConnected()) {
                    showNoInternetDialog();
                    resetExport();
                    return;
                }
                exportToDropboxSafe();
            } else {
                Toast.makeText(this, "Select a cloud service", Toast.LENGTH_SHORT).show();
                resetExport();
            }
        }
    }

    // ---------------- Local Export ----------------
    private void exportLocalFile() {
        createFileLauncher.launch(backupFileName);
    }

    private void saveToUri(Uri uri) {
        try (OutputStream os = getContentResolver().openOutputStream(uri)) {
            os.write(pendingExportData);
            os.flush();
            runOnUiThread(() -> showExportSuccessDialog("Export Successful", "Backup saved successfully!", true));
        } catch (Exception e) {
            e.printStackTrace();
            runOnUiThread(() -> {
                Toast.makeText(this, "Failed to save file: " + e.getMessage(), Toast.LENGTH_LONG).show();
                resetExport();
            });
        }
    }

    // ---------------- Google Drive ----------------
    private void exportToGoogleDrive() {
        executor.execute(() -> {
            try {
                File tempFile = new File(getCacheDir(), backupFileName);
                try (OutputStream os = new java.io.FileOutputStream(tempFile)) {
                    os.write(pendingExportData);
                    os.flush();
                }

                Uri fileUri = FileProvider.getUriForFile(
                        this,
                        getPackageName() + ".provider",
                        tempFile
                );

                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("application/x-abms");
                shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                runOnUiThread(() -> {
                    startActivity(Intent.createChooser(shareIntent, "Export Backup"));
                    showExportSuccessDialog(
                            "Backup Ready",
                            "Your file is prepared. Finish upload in Google Drive, Gmail, or any cloud.",
                            false
                    );
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    resetExport();
                });
            }
        });
    }

    // ---------------- Dropbox ----------------
    private void exportToDropboxSafe() {
        executor.execute(() -> {
            try {
                exportToDropbox();
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(this, "Dropbox export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    resetExport();
                });
            }
        });
    }

    private void exportToDropbox() throws Exception {
        if (pendingExportData == null) {
            runOnUiThread(() -> Toast.makeText(this, "No backup data to upload.", Toast.LENGTH_SHORT).show());
            resetExport();
            return;
        }

        DbxCredential savedCred = loadDropboxCredential();
        if (savedCred == null) {
            Auth.startOAuth2Authentication(this, DROPBOX_APP_KEY);
            return;
        }

        if (dropboxClient == null) initDropboxClient(savedCred);
        else uploadToDropbox();
    }

    private boolean isConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NetworkCapabilities caps = cm.getNetworkCapabilities(cm.getActiveNetwork());
            return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
        } else {
            NetworkInfo netInfo = cm.getActiveNetworkInfo();
            return netInfo != null && netInfo.isConnected();
        }
    }

    private void showNoInternetDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_no_internet, null);
        TextView tvTitle = dialogView.findViewById(R.id.tvTitle);
        TextView tvMessage = dialogView.findViewById(R.id.tvMessage);
        Button btnOk = dialogView.findViewById(R.id.btnOk);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation; // Add fade-in animation
        }

        btnOk.setOnClickListener(v -> dialog.dismiss());

        // Apply fade-in animation programmatically
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.dialog_fade_in);
        dialogView.startAnimation(fadeIn);

        dialog.show();
    }


    private void initDropboxClient(DbxCredential credential) {
        dropboxCredential = credential;
        dropboxClient = new DbxClientV2(new DbxRequestConfig("Abebe Getachew"), credential);

        executor.execute(() -> {
            try {
                dropboxClient.users().getCurrentAccount();
                saveDropboxCredential(dropboxCredential);
                if (pendingExportData != null) {
                    uploadToDropbox();
                }
            } catch (Exception e) { e.printStackTrace(); }
        });
    }

    private void uploadToDropbox() {
        runOnUiThread(() -> showProgressDialog("Uploading backup to Dropbox..."));
        executor.execute(() -> {
            try {
                dropboxClient.files()
                        .uploadBuilder("/" + backupFileName)
                        .withMode(com.dropbox.core.v2.files.WriteMode.OVERWRITE)
                        .uploadAndFinish(new ByteArrayInputStream(pendingExportData));

                runOnUiThread(() -> {
                    hideProgressDialog();
                    showExportSuccessDialog("Export Successful", "Backup uploaded to Dropbox!", true);
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    hideProgressDialog();
                    Toast.makeText(this, "Dropbox upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    resetExport();
                });
            }
        });
    }

    private void resetExport() {
        isExporting = false;
        pendingExportData = null;
        runOnUiThread(() -> btnExport.setEnabled(true));
    }

    private void updateLayoutForExportType(boolean isCloud) {
        cloudChoiceGroup.setVisibility(isCloud ? View.VISIBLE : View.GONE);
        localLayout.setVisibility(isCloud ? View.GONE : View.VISIBLE);
        editFileName.setVisibility(isCloud ? View.GONE : View.VISIBLE);
    }

    private void showExportSuccessDialog(String title, String message, boolean finishActivity) {
        resetExport();
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_success, null);
        TextView dialogTitle = dialogView.findViewById(R.id.dialogTitle);
        TextView dialogMessage = dialogView.findViewById(R.id.dialogMessage);
        Button btnOk = dialogView.findViewById(R.id.btnOk);

        dialogTitle.setText(title);
        dialogMessage.setText(message);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView).create();
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        btnOk.setOnClickListener(v -> {
            dialog.dismiss();
            if (finishActivity) finish();
        });
        dialog.show();
    }

    private void showProgressDialog(String message) {
        if (progressDialog == null) {
            View view = getLayoutInflater().inflate(R.layout.dialog_progress, null);
            TextView tvMessage = view.findViewById(R.id.progressMessage);
            tvMessage.setText(message);

            progressDialog = new AlertDialog.Builder(this)
                    .setView(view)
                    .setCancelable(false)
                    .create();
            if (progressDialog.getWindow() != null)
                progressDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        progressDialog.show();
    }

    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) progressDialog.dismiss();
    }

    // ---------------- Helpers ----------------
    private String getBackupFileName() {
        String baseName = editFileName.getText().toString().trim();
        if (baseName.isEmpty()) baseName = "backup";
        int lastDot = baseName.lastIndexOf('.');
        if (lastDot > 0) baseName = baseName.substring(0, lastDot);
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        return baseName + "_" + timestamp + ".abms";
    }

    private byte[] getEncryptedDatabaseBytes() throws Exception {
        // 1️⃣ Ensure the database file exists
        File dbFile = getDatabasePath("DebtsDB");
        if (!dbFile.exists()) {
            throw new Exception("Database file not found!");
        }

        // 2️⃣ Get or generate secret key from SessionManager
        SessionManager sessionManager = new SessionManager(this);
        String keyBase64 = sessionManager.getSavedKey();
        SecretKey secretKey;

        if (keyBase64 != null) {
            byte[] keyBytes = Base64.decode(keyBase64, Base64.NO_WRAP);
            secretKey = new javax.crypto.spec.SecretKeySpec(keyBytes, "AES");
        } else {
            // No key yet → generate new one
            secretKey = KeyStoreHelper.getOrCreateKey(Export.this);
            KeyStoreHelper.saveKeyToPrefs(this, secretKey);
            sessionManager.saveKeyToPrefs(Base64.encodeToString(secretKey.getEncoded(), Base64.NO_WRAP));
        }

        // 3️⃣ Encrypt database
        try (FileInputStream fis = new FileInputStream(dbFile);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);

            // Save IV first (needed for decryption later)
            baos.write(cipher.getIV());

            // Encrypt the database file
            try (CipherOutputStream cos = new CipherOutputStream(baos, cipher)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    cos.write(buffer, 0, bytesRead);
                }
            }

            return baos.toByteArray();
        }
    }


    // ---------------- Dropbox Credentials ----------------
    private void saveDropboxCredential(DbxCredential credential) {
        getSharedPreferences("app_prefs", MODE_PRIVATE)
                .edit()
                .putString("dropbox_credential", credential.toString())
                .apply();
    }

    private DbxCredential loadDropboxCredential() {
        String json = getSharedPreferences("app_prefs", MODE_PRIVATE)
                .getString("dropbox_credential", null);
        if (json != null) {
            try {
                return DbxCredential.Reader.readFully(json);
            } catch (Exception e) { e.printStackTrace(); }
        }
        return null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (dropboxClient == null) {
            DbxCredential newCred = Auth.getDbxCredential();
            if (newCred != null) {
                saveDropboxCredential(newCred);
                initDropboxClient(newCred);
            }
        }
    }
}
