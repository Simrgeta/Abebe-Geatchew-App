package com.abebegetachew.abebe.salarymanagement.abebegetachew;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONObject;

import java.io.File;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import de.hdodenhof.circleimageview.CircleImageView;

public class SettingsFragment extends Fragment {

    SessionManager sessionManager;
    private AlertDialog progressDialog;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        // UI elements
        CircleImageView profileImage = view.findViewById(R.id.profileImage);
        TextView usernameTextView = view.findViewById(R.id.username);
        TextView emailTextview = view.findViewById(R.id.emailTextview);
        MaterialCardView myButton = view.findViewById(R.id.myButton);
        MaterialCardView myButton2 = view.findViewById(R.id.myButton2);
        MaterialCardView myButton3 = view.findViewById(R.id.myButton3);
        MaterialCardView myButton4 = view.findViewById(R.id.myButton4);
        MaterialCardView myButton5 = view.findViewById(R.id.myButton5);
        MaterialCardView myButton6 = view.findViewById(R.id.myButton6);
        MaterialCardView myButton7 = view.findViewById(R.id.myButton7);
        MaterialCardView myButton8 = view.findViewById(R.id.myButton8);
        MaterialCardView myButton9 = view.findViewById(R.id.myButton9);
        MaterialCardView myButton10 = view.findViewById(R.id.myButton10);
        MaterialCardView myButton11 = view.findViewById(R.id.myButton11);
        MaterialCardView myButton12 = view.findViewById(R.id.myButton12);
        MaterialCardView myButton13 = view.findViewById(R.id.myButton13);
        usernameTextView.setTextSize(14); // keep text size uniform

        // Session data
        sessionManager = new SessionManager(requireContext());
        String profilePath = sessionManager.getProfileImagePath();
        String fullName = sessionManager.getSessionDetails("key_session_name");
        String email = sessionManager.getSessionDetails("key_session_email");

        // Load profile image
        if (profilePath != null && !profilePath.isEmpty()) {
            Glide.with(this)
                    .load(profilePath)
                    .placeholder(R.drawable.picture)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .error(R.drawable.picture)
                    .into(profileImage);
        }

        // Load and style name
        if (fullName != null && !fullName.trim().isEmpty()) {
            fullName = fullName.trim();

            String[] nameParts = fullName.split("\\s+");
            String firstName = nameParts[0];
            String lastName = "";
            if (nameParts.length > 1) {
                lastName = nameParts[nameParts.length - 1]; // full last name
            }

            String combined = firstName + (lastName.isEmpty() ? "" : " " + lastName);
            SpannableString spannable = new SpannableString(combined);

            // Load custom fonts
            android.graphics.Typeface mediumFont = ResourcesCompat.getFont(requireContext(), R.font.helvetica_neue_medium);
            android.graphics.Typeface thinFont = ResourcesCompat.getFont(requireContext(), R.font.helvetica_neue_thin);

            // Apply medium font to first name
            spannable.setSpan(new CustomTypefaceSpan(mediumFont), 0, firstName.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            // Apply thin font to last name
            if (!lastName.isEmpty()) {
                int lastNameStart = combined.indexOf(lastName);
                int lastNameEnd = lastNameStart + lastName.length();
                spannable.setSpan(new CustomTypefaceSpan(thinFont), lastNameStart, lastNameEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            usernameTextView.setText(spannable);
        } else {
            usernameTextView.setText("");
        }

        emailTextview.setText(email);

        myButton.setOnClickListener(v -> startActivity(new Intent(requireContext(), EditProfile.class)));
        myButton2.setOnClickListener(v -> startActivity(new Intent(requireContext(), ChangePassword.class)));
        myButton3.setOnClickListener(v -> startActivity(new Intent(requireContext(), Export.class)));
        myButton4.setOnClickListener(v -> startActivity(new Intent(requireContext(), Import.class)));
        myButton5.setOnClickListener(v -> showWarningDialog("Confirm Reset", "Are you sure you want to reset all data?", ActionType.RESET_DATA));
        myButton6.setOnClickListener(v -> {
            if (!isConnected()) {
                showNoInternetDialog();
            } else {
                showWarningDialog(
                        "Delete Account",
                        "This action is permanent. Please enter your password to confirm account deletion.",
                        ActionType.DELETE_ACCOUNT
                );
            }
        });

        myButton7.setOnClickListener(v -> {
            if (!isConnected()) {
                showNoInternetDialog();
            } else {
                showWarningDialog("Log Out", "Are you sure you want to log out?", ActionType.LOG_OUT);
            }
        });
        myButton8.setOnClickListener(v -> startActivity(new Intent(requireContext(), About.class)));
        myButton9.setOnClickListener(v -> startActivity(new Intent(requireContext(), DeveloperInfo.class)));
        myButton10.setOnClickListener(v -> startActivity(new Intent(requireContext(), Feedback.class)));
        myButton11.setOnClickListener(v -> startActivity(new Intent(requireContext(), Help.class)));

        return view;
    }

    // Add this helper method inside your SettingsFragment
    private boolean isConnected() {
        ConnectivityManager cm = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NetworkCapabilities caps = cm.getNetworkCapabilities(cm.getActiveNetwork());
            return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
        } else {
            NetworkInfo netInfo = cm.getActiveNetworkInfo();
            return netInfo != null && netInfo.isConnected();
        }
    }

    // Add this helper method to show no-internet warning
    private void showNoInternetDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_no_internet, null);
        TextView tvTitle = dialogView.findViewById(R.id.tvTitle);
        TextView tvMessage = dialogView.findViewById(R.id.tvMessage);
        Button btnOk = dialogView.findViewById(R.id.btnOk);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        btnOk.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }


    private void showWarningDialog(String title, String message, ActionType actionType) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_error, null);
        TextView tvTitle = dialogView.findViewById(R.id.dialogTitle);
        TextView tvMessage = dialogView.findViewById(R.id.dialogMessage);
        Button btnOk = dialogView.findViewById(R.id.btnOk);
        View spacer1 = dialogView.findViewById(R.id.spacer1);
        EditText delPass = dialogView.findViewById(R.id.delPass);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel); // Add a cancel button in your layout if not present

        btnCancel.setVisibility(View.VISIBLE);
        spacer1.setVisibility(View.VISIBLE);

        tvTitle.setText(title);
        tvMessage.setText(message);

        final androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(false)
                .create();

        // Set button text & action dynamically
        switch (actionType) {
            case RESET_DATA:
                delPass.setVisibility(View.GONE);
                btnOk.setText("Reset");
                btnOk.setOnClickListener(v -> {
                    dialog.dismiss();
                    resetAllData(); // your method for reset
                });
                break;

            case DELETE_ACCOUNT:
                delPass.setVisibility(View.VISIBLE);
                btnOk.setText("Delete");
                btnOk.setOnClickListener(v -> {
                    String password = delPass.getText().toString().trim();
                    if (password.isEmpty()) {
                        delPass.setError("Password required");
                        return;
                    }

                    // ✅ Re-authenticate user with email + password
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user == null) {
                        Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        return;
                    }

                    String email = user.getEmail();
                    AuthCredential credential = EmailAuthProvider.getCredential(email, password);

                    user.reauthenticate(credential).addOnCompleteListener(authTask -> {
                        if (authTask.isSuccessful()) {
                            // Get fresh token AFTER re-authentication
                            user.getIdToken(true).addOnCompleteListener(tokenTask -> {
                                if (tokenTask.isSuccessful() && tokenTask.getResult() != null) {
                                    String idToken = tokenTask.getResult().getToken();

                                    dialog.dismiss(); // close password dialog

                                    // Delete Cloudinary image with fresh token
                                    String oldProfileUrl = sessionManager.getProfileImageUri();
                                    if (oldProfileUrl != null && !oldProfileUrl.isEmpty()) {
                                        String publicId = extractPublicIdFromUrl(oldProfileUrl);
                                        if (publicId != null) {
                                            deleteOldCloudinaryImage(publicId,
                                                    () -> Log.d("Cloudinary", "Old image deleted"),
                                                    () -> Log.w("Cloudinary", "Failed to delete old image"),
                                                    idToken // <-- pass fresh token
                                            );
                                        }
                                    }

                                    // Now delete Firebase user
                                    deleteAccount(sessionManager);
                                } else {
                                    delPass.setError("Failed to get valid token");
                                }
                            });
                        } else {
                            // ❌ Wrong password
                            delPass.setError("Incorrect password");
                            delPass.requestFocus();
                        }
                    });
                });
                break;

            case LOG_OUT:
                delPass.setVisibility(View.GONE);
                btnOk.setText("Log Out");
                btnOk.setOnClickListener(v -> {
                    dialog.dismiss();
                    logoutUser(sessionManager); // your method for logout
                });
                break;
        }

        // Cancel button action
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        dialog.show();
    }

    // Enum for different actions
    public enum ActionType {
        RESET_DATA,
        DELETE_ACCOUNT,
        LOG_OUT
    }

    private void showSuccessDialog(String title, String message, String buttonText, Runnable action) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_success, null);
        TextView tvTitle = dialogView.findViewById(R.id.dialogTitle);
        TextView tvMessage = dialogView.findViewById(R.id.dialogMessage);
        Button btnOk = dialogView.findViewById(R.id.btnOk);

        tvTitle.setText(title);
        tvMessage.setText(message);
        btnOk.setText(buttonText);

        final androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(false)
                .create();

        btnOk.setOnClickListener(v -> {
            dialog.dismiss();
            if (action != null) action.run(); // perform specific action
        });

        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        dialog.show();
    }


    private void resetAllData() {
        DatabaseHelper dbHelper = new DatabaseHelper(requireContext());
        dbHelper.clearAllData();
        showSuccessDialog(
                "Reset Successful",
                "All data has been reset successfully.",
                "OK",
                () -> {
                }
        );
    }


    private void showProgressDialog() {
        if (progressDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_loading, null);
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

    private void deleteAccount(SessionManager sessionManager) {
        showProgressDialog();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String uid = user.getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Step 0: Delete Firestore user document
        db.collection("User").document(uid).delete().addOnCompleteListener(firestoreTask -> {
            if (firestoreTask.isSuccessful()) {
                // Step 1: Delete FirebaseAuth user
                user.delete().addOnCompleteListener(authTask -> {
                    if (authTask.isSuccessful()) {
                        // Step 2: Clear local session and SQLite
                        sessionManager.clearSession();
                        DatabaseHelper dbHelper = new DatabaseHelper(requireContext());
                        dbHelper.clearAllData();

                        // Step 3: Sign out
                        FirebaseAuth.getInstance().signOut();

                        hideProgressDialog();
                        // Step 4: Go to login
                        showSuccessDialog(
                                "Account Deleted",
                                "Your account has been permanently deleted.",
                                "Close",
                                this::goToSplash
                        );
                    } else {
                        hideProgressDialog();
                        Toast.makeText(requireContext(),
                                "Failed to delete Firebase account: " + authTask.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
            } else {
                hideProgressDialog();
                Toast.makeText(requireContext(),
                        "Failed to delete Firestore record: " + firestoreTask.getException().getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private String extractPublicIdFromUrl(String url) {
        try {
            String filename = url.substring(url.lastIndexOf("/") + 1); // e6y8vdaxztkrvmdm2nci.jpg
            return filename.substring(0, filename.lastIndexOf(".")); // e6y8vdaxztkrvmdm2nci
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void deleteOldCloudinaryImage(String publicId, Runnable onDeleted, Runnable onFailure, String idToken) {
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

                if (getActivity() == null) return;

                if (responseCode == 200) {
                    if (onDeleted != null) getActivity().runOnUiThread(onDeleted);
                } else {
                    if (onFailure != null) getActivity().runOnUiThread(onFailure);
                }

            } catch (Exception e) {
                e.printStackTrace();
                if (getActivity() != null && onFailure != null) getActivity().runOnUiThread(onFailure);
            }
        }).start();
    }



    private void logoutUser(SessionManager sessionManager) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            auth.signOut();
        }
        // Email not found in Firestore, still clear local session
        sessionManager.clearSession();
        DatabaseHelper dbHelper = new DatabaseHelper(requireContext());
        dbHelper.clearAllData();


        // go to login screen
        showSuccessDialog(
                "Logged Out",
                "You have been logged out successfully.",
                "Login Again",
                this::goToSplash
        );
    }

    private void goToSplash() {
        Intent splashIntent = new Intent(requireContext(), Splash.class);
        splashIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_SINGLE_TOP |
                Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(splashIntent);
        requireActivity().finishAffinity();
    }

    @Override
    public void onResume() {
        super.onResume();

        // Reload session data
        SessionManager sessionManager = new SessionManager(requireContext());
        String profilePath = sessionManager.getProfileImagePath();
        String fullName = sessionManager.getSessionDetails("key_session_name");
        String email = sessionManager.getSessionDetails("key_session_email");

        CircleImageView profileImage = requireView().findViewById(R.id.profileImage);
        TextView usernameTextView = requireView().findViewById(R.id.username);
        TextView emailTextView = requireView().findViewById(R.id.emailTextview);

        // Reload profile image
        if (profilePath != null && !profilePath.isEmpty()) {
            File imgFile = new File(profilePath);
            if (imgFile.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                profileImage.setImageBitmap(bitmap);
            } else {
                profileImage.setImageResource(R.drawable.picture);
            }
        } else {
            profileImage.setImageResource(R.drawable.picture);
        }

        // Reload and style username
        if (fullName != null && !fullName.trim().isEmpty()) {
            fullName = fullName.trim();
            String[] nameParts = fullName.split("\\s+");
            String firstName = nameParts[0];
            String lastName = nameParts.length > 1 ? nameParts[nameParts.length - 1] : "";

            String combined = firstName + (lastName.isEmpty() ? "" : " " + lastName);
            SpannableString spannable = new SpannableString(combined);

            android.graphics.Typeface mediumFont = ResourcesCompat.getFont(requireContext(), R.font.helvetica_neue_medium);
            android.graphics.Typeface thinFont = ResourcesCompat.getFont(requireContext(), R.font.helvetica_neue_thin);

            spannable.setSpan(new CustomTypefaceSpan(mediumFont), 0, firstName.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            if (!lastName.isEmpty()) {
                int lastNameStart = combined.indexOf(lastName);
                int lastNameEnd = lastNameStart + lastName.length();
                spannable.setSpan(new CustomTypefaceSpan(thinFont), lastNameStart, lastNameEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            usernameTextView.setText(spannable);
        } else {
            usernameTextView.setText("");
        }

        // Reload email
        emailTextView.setText(email);
    }

}
