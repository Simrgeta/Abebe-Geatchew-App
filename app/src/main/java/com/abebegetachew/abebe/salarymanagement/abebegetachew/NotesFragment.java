package com.abebegetachew.abebe.salarymanagement.abebegetachew;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.io.File;

import de.hdodenhof.circleimageview.CircleImageView;

public class NotesFragment extends Fragment {


    private RecentNoteDateViewModel viewModel;
    private TextView latest_note;
    private TypeWriter bannerText;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notes, container, false);

        // TypeWriter setup
        bannerText = view.findViewById(R.id.bannerText);
        bannerText.setShadowLayer(8f, 0, 0, 0xFFFFFFFF);
        bannerText.setCharacterDelay(50);     // speed of typing
        bannerText.setRestartDelay(2000);     // delay before restarting loop

        // Get UI elements
        CircleImageView profileImage = view.findViewById(R.id.profileImage);
        TextView usernameText = view.findViewById(R.id.username);
        ConstraintLayout itemContainer2 = view.findViewById(R.id.itemContainer2);
        ConstraintLayout itemContainer = view.findViewById(R.id.itemContainer);
        latest_note = view.findViewById(R.id.latest_note);

        latest_note.setSelected(true);


        // Init ViewModel
        viewModel = new ViewModelProvider(this).get(RecentNoteDateViewModel.class);
        viewModel.getRecentDate().observe(getViewLifecycleOwner(),
                recentDate -> latest_note.setText(recentDate)
        );


        // Get saved local path from SessionManager
        SessionManager sessionManager = new SessionManager(requireContext());
        String profilePath = sessionManager.getProfileImagePath(); // Implement this getter in SessionManager

        // Load profile image if exists
        if (profilePath != null && !profilePath.isEmpty()) {
            Glide.with(this)
                    .load(profilePath)  // Local path or URL
                    .placeholder(R.drawable.picture) // Fallback
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .error(R.drawable.picture)
                    .into(profileImage);
        }

        profileImage.setOnClickListener(v -> {
            if (requireActivity() instanceof Main) {
                ((Main) requireActivity()).setCurrentPage(4); // Assuming settings page is at index 4
            }
        });

        // Get and format username
        String fullName = sessionManager.getSessionDetails("key_session_name");
        String formattedName = formatName(fullName);
        usernameText.setText("Welcome,\n" + formattedName);

        // Click listeners
        itemContainer2.setOnClickListener(v -> startActivity(new Intent(requireContext(), ShowNote.class)));
        itemContainer.setOnClickListener(v -> startActivity(new Intent(requireContext(), AddNote.class)));

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) viewModel.calculateRecentDate();

        // ✅ Reload profile info from SessionManager
        SessionManager sessionManager = new SessionManager(requireContext());
        String profilePath = sessionManager.getProfileImagePath();
        String fullName = sessionManager.getSessionDetails("key_session_name");

        CircleImageView profileImage = requireView().findViewById(R.id.profileImage);
        TextView usernameText = requireView().findViewById(R.id.username);

        // Reload profile image with Glide (no cache)
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

        // Reload username
        usernameText.setText("Welcome,\n" + formatName(fullName));

        // Restart typing loop animation
        if (bannerText != null) {
            bannerText.animateText("Log today’s market moves — jot descriptions, track amounts, capture insights.");
        }
    }



    @Override
    public void onPause() {
        super.onPause();
        // Stop animation to prevent memory leaks
        if (bannerText != null) {
            bannerText.stopAnimation();
        }
    }

    // Method to format full name -> "FirstName L."
    // Method to format full name -> "FirstName LastName"
    private String formatName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return "";
        }

        // Remove extra spaces and split into words
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) {
            return capitalize(parts[0]); // Only one name provided
        }

        String firstName = capitalize(parts[0]);
        String lastName = capitalize(parts[parts.length - 1]); // Full last name, not initial

        return firstName + " " + lastName;
    }

    // Capitalize first letter of a word
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return "";
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

}
