package com.abebegetachew.abebe.salarymanagement.abebegetachew;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.sanojpunchihewa.glowbutton.GlowButton;
import com.yalantis.ucrop.util.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

public class Import extends AppCompatActivity {

    private Uri lastFileUri = null;
    private ActivityResultLauncher<Intent> filePickerLauncher;
    private SessionManager sessionManager;
    private ImageButton backButton;
    private static boolean isImportScreenOpen = false; // Prevent multiple imports
    private boolean openedExternally = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Only allow one import screen at a time
        if (isImportScreenOpen) {
            Toast.makeText(this, "Import screen is already open!", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        isImportScreenOpen = true;

        // Initialize session manager
        sessionManager = new SessionManager(this);
        if (!sessionManager.isLoggedIn()) {
            redirectToSplash();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_import);

        backButton = findViewById(R.id.backButton);
        GlowButton btnOpenFile = findViewById(R.id.btnOpenFile);
        GlowButton btnImport = findViewById(R.id.btnImport);

        // File picker
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            handleIncomingFile(uri, false);
                            findViewById(R.id.btnOpenFile).setVisibility(View.GONE);
                        }
                    }
                }
        );

        btnOpenFile.setOnClickListener(v -> {
            Intent pickerIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            pickerIntent.addCategory(Intent.CATEGORY_OPENABLE);

            // Only show .abms files
            pickerIntent.setType("*/*"); // must be generic type first
            String[] mimeTypes = {"application/octet-stream"}; // generic binary type
            pickerIntent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);

            filePickerLauncher.launch(pickerIntent);
        });

        Intent intent = getIntent();
        String action = intent.getAction();
        openedExternally = Intent.ACTION_VIEW.equals(action);

        if (openedExternally) {
            backButton.setVisibility(View.INVISIBLE);
            btnOpenFile.setVisibility(View.GONE);
        } else {
            backButton.setVisibility(View.VISIBLE);
            btnOpenFile.setVisibility(View.VISIBLE);
            backButton.setOnClickListener(v -> finish());
        }


        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (openedExternally) {
                    redirectToSplash(); // only go to splash if opened externally
                } else {
                    finish(); // otherwise just finish without resetting UI
                }
            }
        });


        // Handle incoming file if any
        handleIncomingFile(intent.getData(), Intent.ACTION_VIEW.equals(action));
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        resetFileState();
        handleIncomingFile(intent.getData(), true);
        lastFileUri = intent.getData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Uri currentUri = getIntent().getData();
        if (currentUri != null && !currentUri.equals(lastFileUri)) {
            resetFileState();
            handleIncomingFile(currentUri, true);
            lastFileUri = currentUri;
        }
    }

    private void handleIncomingFile(Uri fileUri, boolean openedExternally) {
        if (fileUri == null) return;

        lastFileUri = fileUri;
        String name = "Unknown";
        long size = 0;
        long lastModified = 0;

        if ("file".equals(fileUri.getScheme())) {
            File file = new File(fileUri.getPath());
            if (file.exists()) {
                name = file.getName();
                size = file.length();
                lastModified = file.lastModified();
            }
        } else if ("content".equals(fileUri.getScheme())) {
            try (android.database.Cursor cursor = getContentResolver().query(fileUri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    int sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE);
                    if (nameIndex >= 0) name = cursor.getString(nameIndex);
                    if (sizeIndex >= 0) size = cursor.getLong(sizeIndex);
                }
            } catch (Exception e) { e.printStackTrace(); }

            try {
                File tempFile = new File(FileUtils.getPath(this, fileUri));
                if (tempFile.exists()) lastModified = tempFile.lastModified();
            } catch (Exception e) { lastModified = System.currentTimeMillis(); }
        }

        if (!name.toLowerCase().endsWith(".abms")) {
            showErrorDialog("Invalid File", "This is not a valid .abms file.", openedExternally);
            return;
        }

        showFilePreview(name, size, lastModified, fileUri, openedExternally);
    }

    private void showFilePreview(String name, long size, long lastModified, Uri fileUri, boolean openedExternally) {
        LinearLayout filePreviewLayout = findViewById(R.id.filePreviewLayout);
        GlowButton btnImport = findViewById(R.id.btnImport);
        GlowButton btnOpenFile = findViewById(R.id.btnOpenFile);
        TextView fileName = findViewById(R.id.fileName);
        TextView fileSize = findViewById(R.id.fileSize);
        TextView fileDate = findViewById(R.id.fileDate);

        fileName.setText(name);
        fileSize.setText(String.format("Size: %.2f MB", size / (1024.0 * 1024.0)));
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd MMM yyyy");
        fileDate.setText("Modified: " + sdf.format(lastModified));

        filePreviewLayout.setVisibility(View.VISIBLE);
        btnImport.setVisibility(View.VISIBLE);
        if (!openedExternally) btnOpenFile.setVisibility(View.GONE);

        btnImport.setOnClickListener(v -> importBackup(fileUri));
    }

    private void importBackup(Uri backupUri) {
        SecretKey key = KeyStoreHelper.getKeyFromPrefs(this);
        if (key == null) {
            Toast.makeText(this, "Encryption key not found!", Toast.LENGTH_LONG).show();
            return;
        }

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Importing backup...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        new Thread(() -> {
            try {
                File tempDbFile = decryptBackupToTempFile(backupUri, key);
                SQLiteDatabase tempDb = SQLiteDatabase.openDatabase(tempDbFile.getPath(), null, SQLiteDatabase.OPEN_READONLY);
                SQLiteDatabase mainDb = new DatabaseHelper(this).getWritableDatabase();

                mergeTable(mainDb, tempDb, "Debts", "DebtID");
                mergeTable(mainDb, tempDb, "RepaidDebts", "RepaidID");
                mergeTable(mainDb, tempDb, "Income", "IncomeID");
                mergeTable(mainDb, tempDb, "Expense", "ExpenseID");
                mergeTable(mainDb, tempDb, "Notes", "NoteId");

                tempDb.close();
                tempDbFile.delete();

                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    showSuccessDialog("Import Completed", "Backup imported successfully!");
                    resetFileState();
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Import failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void showSuccessDialog(String title, String message) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_success, null);
        TextView tvTitle = dialogView.findViewById(R.id.dialogTitle);
        TextView tvMessage = dialogView.findViewById(R.id.dialogMessage);
        tvTitle.setText(title);
        tvMessage.setText(message);

        final androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        Button btnOk = dialogView.findViewById(R.id.btnOk);
        btnOk.setOnClickListener(v -> {
            dialog.dismiss();
            finish(); // finish Import activity after success
        });

        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        dialog.show();
    }

    private void resetFileState() {
        lastFileUri = null;
        LinearLayout filePreviewLayout = findViewById(R.id.filePreviewLayout);
        filePreviewLayout.setVisibility(View.GONE);
        findViewById(R.id.btnImport).setVisibility(View.GONE);
        findViewById(R.id.btnOpenFile).setVisibility(View.VISIBLE);
    }

    private void redirectToSplash() {
        Intent splashIntent = new Intent(Import.this, Splash.class);
        splashIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_SINGLE_TOP |
                Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(splashIntent);
        finishAffinity();
    }

    private File decryptBackupToTempFile(Uri fileUri, SecretKey secretKey) throws Exception {
        File tempFile = new File(getCacheDir(), "temp_import.db");
        try (InputStream is = getContentResolver().openInputStream(fileUri);
             FileOutputStream fos = new FileOutputStream(tempFile)) {

            byte[] iv = new byte[16];
            if (is.read(iv) != 16) throw new Exception("Invalid backup file!");

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));

            try (CipherInputStream cis = new CipherInputStream(is, cipher)) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = cis.read(buffer)) != -1) fos.write(buffer, 0, len);
            }
        }
        return tempFile;
    }

    private void mergeTable(SQLiteDatabase mainDb, SQLiteDatabase backupDb, String tableName, String uniqueCol) {
        try (android.database.Cursor cursor = backupDb.query(tableName, null, null, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    ContentValues values = new ContentValues();
                    for (int i = 0; i < cursor.getColumnCount(); i++) {
                        String col = cursor.getColumnName(i);
                        int type = cursor.getType(i);
                        switch (type) {
                            case android.database.Cursor.FIELD_TYPE_INTEGER:
                                values.put(col, cursor.getLong(i));
                                break;
                            case android.database.Cursor.FIELD_TYPE_STRING:
                                values.put(col, cursor.getString(i));
                                break;
                            case android.database.Cursor.FIELD_TYPE_FLOAT:
                                values.put(col, cursor.getDouble(i));
                                break;
                            case android.database.Cursor.FIELD_TYPE_BLOB:
                                values.put(col, cursor.getBlob(i));
                                break;
                            default:
                                values.putNull(col);
                        }
                    }

                    // Generic logic for all tables (including Notes now)
                    String uniqueValue = values.getAsString(uniqueCol);
                    long existingCount = DatabaseUtils.queryNumEntries(mainDb, tableName,
                            uniqueCol + " = ?", new String[]{uniqueValue});

                    if (existingCount == 0) {
                        mainDb.insert(tableName, null, values);
                    } else {
                        mainDb.update(tableName, values, uniqueCol + " = ?", new String[]{uniqueValue});
                    }

                } while (cursor.moveToNext());
            }
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        isImportScreenOpen = false; // reset flag
    }


    private void showErrorDialog(String title, String message, boolean openedExternally) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_error, null);
        TextView tvTitle = dialogView.findViewById(R.id.dialogTitle);
        TextView tvMessage = dialogView.findViewById(R.id.dialogMessage);
        tvTitle.setText(title);
        tvMessage.setText(message);

        final androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        Button btnOk = dialogView.findViewById(R.id.btnOk);
        btnOk.setOnClickListener(v -> {
            dialog.dismiss();
            if (openedExternally) {
                redirectToSplash(); // go to splash if opened externally
            } else {
                finish(); // finish activity if opened from in-app
            }
        });

        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        dialog.show();
    }

}
