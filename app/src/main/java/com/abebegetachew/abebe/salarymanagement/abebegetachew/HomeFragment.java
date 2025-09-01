package com.abebegetachew.abebe.salarymanagement.abebegetachew;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.Observer;

public class HomeFragment extends Fragment {


    private RecyclerView debtsRecyclerView, expensesRecyclerView;
    private RecentDebtsAdapter debtAdapter;

    private RecentExpensesAdapter expenseAdapter;
    private DatabaseHelper db;
    private String lastSearchQuery = ""; // keep last queryprivate
    LinearLayout debtPlaceholderCard;
    LinearLayout expensePlaceholderCard;

    private BalanceViewModel balanceViewModel;
    private DebtsViewModel debtsViewModel;
    private IncomeExpenseViewModel incomeExpenseViewModel;
    private TextView totalBalanceText;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Get UI elements
        CircleImageView profileImage = view.findViewById(R.id.profileImage);
        TextView usernameText = view.findViewById(R.id.username);
        totalBalanceText = view.findViewById(R.id.total_balance);

        totalBalanceText.setSelected(true);

        // Get saved local path from SessionManager
        SessionManager sessionManager = new SessionManager(requireContext());
        String profilePath = sessionManager.getProfileImagePath(); // Implement this getter in SessionManager

        // Load profile image if exists
        if (profilePath != null && !profilePath.isEmpty()) {
            File imgFile = new File(profilePath);
            if (imgFile.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                profileImage.setImageBitmap(bitmap); // Directly set bitmap
            } else {
                profileImage.setImageResource(R.drawable.picture); // fallback
            }
        } else {
            profileImage.setImageResource(R.drawable.picture); // fallback
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

        // Initialize the other ViewModels
        debtsViewModel = new ViewModelProvider(this).get(DebtsViewModel.class);
        incomeExpenseViewModel = new ViewModelProvider(this).get(IncomeExpenseViewModel.class);

        balanceViewModel = new BalanceViewModel(
                requireActivity().getApplication(),
                debtsViewModel,
                incomeExpenseViewModel
        );

        balanceViewModel.getBalance().observe(getViewLifecycleOwner(), new Observer<Double>() {
            @Override
            public void onChanged(Double balance) {
                totalBalanceText.setText(String.format(Locale.US, "%.2f", balance) + " ETB");
            }
        });

        debtsRecyclerView = view.findViewById(R.id.debtsRecyclerView);
        expensesRecyclerView = view.findViewById(R.id.expensesRecyclerView);
        debtPlaceholderCard = view.findViewById(R.id.debtPlaceholderCard);
        expensePlaceholderCard = view.findViewById(R.id.expensePlaceholderCard);
        checkPlaceholder(); // ✅ update placeholder

        db = new DatabaseHelper(requireContext());


        // Layout managers
        debtsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        expensesRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        loadData();

        EditText searchEditText = view.findViewById(R.id.searchEditText);

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                lastSearchQuery = s.toString().trim(); // save query
                if (debtAdapter != null) debtAdapter.filter(lastSearchQuery);
                if (expenseAdapter != null) expenseAdapter.filter(lastSearchQuery);
                checkPlaceholder();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });



        return view;
    }

    private void checkPlaceholder() {
        boolean debtsEmpty = debtAdapter == null || debtAdapter.getItemCount() == 0;
        boolean expensesEmpty = expenseAdapter == null || expenseAdapter.getItemCount() == 0;

        if (debtsEmpty) {
            debtsRecyclerView.setVisibility(View.GONE);
            debtPlaceholderCard.setVisibility(View.VISIBLE);
        } else {
            debtsRecyclerView.setVisibility(View.VISIBLE);
            debtPlaceholderCard.setVisibility(View.GONE);
        }


        if (expensesEmpty) {
            expensesRecyclerView.setVisibility(View.GONE);
            expensePlaceholderCard.setVisibility(View.VISIBLE);
        } else {
            expensesRecyclerView.setVisibility(View.VISIBLE);
            expensePlaceholderCard.setVisibility(View.GONE);
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        loadData(); // Refresh debts/expenses

        // ✅ Reload profile from SessionManager
        SessionManager sessionManager = new SessionManager(requireContext());
        String profilePath = sessionManager.getProfileImagePath();
        String fullName = sessionManager.getSessionDetails("key_session_name");

        CircleImageView profileImage = requireView().findViewById(R.id.profileImage);
        TextView usernameText = requireView().findViewById(R.id.username);

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

        // Reload username
        String formattedName = formatName(fullName);
        usernameText.setText("Welcome,\n" + formattedName);

        // Reapply last search
        if (!lastSearchQuery.isEmpty()) {
            if (debtAdapter != null) debtAdapter.filter(lastSearchQuery);
            if (expenseAdapter != null) expenseAdapter.filter(lastSearchQuery);
        }

        checkPlaceholder(); // ✅ always update placeholder

        // Trigger LiveData recalculation
        debtsViewModel.calculateTotalDebt();
        incomeExpenseViewModel.calculateTotals();
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

    private void loadData() {
        // Load last 3 debts
        List<Debt> recentDebts = db.getAllDebts();
        if (debtAdapter == null) {
            debtAdapter = new RecentDebtsAdapter(requireContext(), recentDebts);
            debtsRecyclerView.setAdapter(debtAdapter);
        } else {
            debtAdapter.updateList(recentDebts); // You need to implement this method in your adapter
        }


        List<Expense> recentExpenses = db.getAllExpenses();
        if (expenseAdapter == null) {
            expenseAdapter = new RecentExpensesAdapter(requireContext(), recentExpenses);
            expensesRecyclerView.setAdapter(expenseAdapter);
        } else {
            expenseAdapter.updateList(recentExpenses); // You need to implement this method in your adapter
        }

        checkPlaceholder(); // ✅ show/hide placeholder after loading

    }
}
