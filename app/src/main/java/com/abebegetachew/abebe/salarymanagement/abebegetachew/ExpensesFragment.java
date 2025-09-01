package com.abebegetachew.abebe.salarymanagement.abebegetachew;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.io.File;

import de.hdodenhof.circleimageview.CircleImageView;

public class ExpensesFragment extends Fragment {

    private IncomeExpenseViewModel viewModel;
    private TextView netIncomeText;
    private TypeWriter bannerText;

    private final ActivityResultLauncher<Intent> addDebtLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == getActivity().RESULT_OK) {
                    viewModel.calculateTotals(); // refresh after adding debt
                }
            });

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_expenses, container, false);

        // TypeWriter setup
        bannerText = view.findViewById(R.id.bannerText);
        bannerText.setShadowLayer(8f, 0, 0, 0xFFFFFFFF);
        bannerText.setCharacterDelay(50);     // speed of typing
        bannerText.setRestartDelay(2000);     // delay before restarting loop

        // UI Elements
        CircleImageView profileImage = view.findViewById(R.id.profileImage);
        TextView usernameText = view.findViewById(R.id.username);
        ConstraintLayout itemContainer3 = view.findViewById(R.id.itemContainer3);
        ConstraintLayout itemContainer2 = view.findViewById(R.id.itemContainer2);
        ConstraintLayout itemContainer = view.findViewById(R.id.itemContainer);
        netIncomeText = view.findViewById(R.id.net_income);

        netIncomeText.setSelected(true);

        // Init ViewModel
        viewModel = new ViewModelProvider(this).get(IncomeExpenseViewModel.class);
        viewModel.getNetIncome().observe(getViewLifecycleOwner(),
                totalDebt -> netIncomeText.setText(String.format("%.2f ETB", totalDebt))
        );

        // Load profile image
        SessionManager sessionManager = new SessionManager(requireContext());
        String profilePath = sessionManager.getProfileImagePath();
        if (profilePath != null && !profilePath.isEmpty()) {
            Glide.with(this)
                    .load(profilePath)
                    .placeholder(R.drawable.picture)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .error(R.drawable.picture)
                    .into(profileImage);
        }

        profileImage.setOnClickListener(v -> {
            if (requireActivity() instanceof Main) {
                ((Main) requireActivity()).setCurrentPage(4);
            }
        });

        // Set formatted username
        String fullName = sessionManager.getSessionDetails("key_session_name");
        usernameText.setText("Welcome,\n" + formatName(fullName));

        // Click listeners
        itemContainer3.setOnClickListener(v -> startActivity(new Intent(requireContext(), ShowExpense.class)));
        itemContainer2.setOnClickListener(v -> startActivity(new Intent(requireContext(), AddIncome.class)));
        itemContainer.setOnClickListener(v -> startActivity(new Intent(requireContext(), AddExpense.class)));

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) viewModel.calculateTotals();

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

        // Restart typing animation
        if (bannerText != null) {
            bannerText.animateText("Keep your money moving — record your earnings and spendings effortlessly.");
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

    // Name formatting helpers
    private String formatName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) return "";
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) return capitalize(parts[0]);
        return capitalize(parts[0]) + " " + capitalize(parts[parts.length - 1]);
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return "";
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
}
