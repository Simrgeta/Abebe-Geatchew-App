package com.abebegetachew.abebe.salarymanagement.abebegetachew;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textview.MaterialTextView;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

public class ShowTotalDebt extends AppCompatActivity {

    private DatabaseHelper db;
    private List<Debt> allDebts;       // All debts for this borrower
    private List<Debt> filteredDebts;  // Filtered list for search
    private UnpaidDebtsAdapter adapter;

    private String borrowerName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_show_total_debt);

        ConstraintLayout root = findViewById(R.id.root);
        MaterialCardView card = findViewById(R.id.card);
        RecyclerView recyclerView = findViewById(R.id.recyclerDebts);
        LinearLayout placeholder = findViewById(R.id.placeholderCard);
        SearchView searchView = findViewById(R.id.searchView);
        MaterialToolbar topAppBar = findViewById(R.id.topAppBar);

        // Back button
        topAppBar.setNavigationOnClickListener(v -> finish());

        // Get borrower name from intent
        borrowerName = getIntent().getStringExtra("BORROWER_NAME");
        if (borrowerName == null) borrowerName = "Unknown";

        db = new DatabaseHelper(this);

        // Load all debts for this borrower (case-insensitive, remaining > 0)
        allDebts = db.getDebtsByBorrower(borrowerName);
        filteredDebts = new ArrayList<>(allDebts);

        // Set borrower name and total
        TextView tvBorrower = findViewById(R.id.tvBorrowerName);
        TextView tvTotal = findViewById(R.id.tvTotalDebt);
        tvBorrower.setText(borrowerName);
        tvTotal.setText("Total Debt for " + capitalize(borrowerName) + ": " + calculateTotal(allDebts));

        if (allDebts.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            placeholder.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            placeholder.setVisibility(View.GONE);

            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            adapter = new UnpaidDebtsAdapter(this, filteredDebts);
            recyclerView.setAdapter(adapter);

            setupSearch(searchView, recyclerView, placeholder);
        }

        // Entry animations
        float ty = 24f * getResources().getDisplayMetrics().density; // 24dp
        long durationCard = 1100L;
        long durationRecycler = 950L;

        Interpolator interp = AnimationUtils.loadInterpolator(this, android.R.interpolator.fast_out_slow_in);

        card.setAlpha(0f);
        card.setTranslationY(ty);

        recyclerView.setAlpha(0f);
        recyclerView.setTranslationY(ty);

        placeholder.setAlpha(0f);
        placeholder.setTranslationY(ty);

        root.post(() -> {
            card.animate().alpha(1f).translationY(0f).setStartDelay(0).setDuration(durationCard).setInterpolator(interp).start();
            View target = allDebts.isEmpty() ? placeholder : recyclerView;
            target.animate().alpha(1f).translationY(0f).setStartDelay(300).setDuration(durationRecycler).setInterpolator(interp).start();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        allDebts.clear();
        allDebts.addAll(db.getDebtsByBorrower(borrowerName));

        filteredDebts.clear();
        filteredDebts.addAll(allDebts);

        if (adapter != null) adapter.notifyDataSetChanged();

        RecyclerView recyclerView = findViewById(R.id.recyclerDebts);
        LinearLayout placeholder = findViewById(R.id.placeholderCard);

        if (allDebts.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            placeholder.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            placeholder.setVisibility(View.GONE);
        }

        TextView tvTotal = findViewById(R.id.tvTotalDebt);
        tvTotal.setText("Total Debt for " + capitalize(borrowerName) + ": " + calculateTotal(allDebts) + " ETB");
    }

    private void setupSearch(SearchView searchView, RecyclerView recyclerView, LinearLayout placeholder) {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterList(query, recyclerView, placeholder);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterList(newText, recyclerView, placeholder);
                return true;
            }

            private void filterList(String query, RecyclerView recyclerView, LinearLayout placeholder) {
                filteredDebts.clear();
                if (TextUtils.isEmpty(query)) {
                    filteredDebts.addAll(allDebts);
                } else {
                    String lowerQuery = query.toLowerCase();
                    for (Debt debt : allDebts) {
                        // Check all fields
                        if (debt.getBorrower().toLowerCase().contains(lowerQuery)
                                || debt.getReason().toLowerCase().contains(lowerQuery)
                                || String.valueOf(debt.getAmountOwed()).contains(lowerQuery)
                                || String.valueOf(debt.getAmountPaid()).contains(lowerQuery)
                                || String.valueOf(debt.getRemainingDebt()).contains(lowerQuery)
                                || debt.getDate().toLowerCase().contains(lowerQuery)) {
                            filteredDebts.add(debt);
                        }
                    }
                }
                adapter.notifyDataSetChanged();

                if (filteredDebts.isEmpty()) {
                    recyclerView.setVisibility(View.GONE);
                    placeholder.setVisibility(View.VISIBLE);
                } else {
                    recyclerView.setVisibility(View.VISIBLE);
                    placeholder.setVisibility(View.GONE);
                }
            }
        });
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        String[] words = s.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (word.length() > 0) {
                sb.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1).toLowerCase())
                        .append(" ");
            }
        }
        return sb.toString().trim();
    }

    private double calculateTotal(List<Debt> debts) {
        double total = 0;
        for (Debt d : debts) {
            if (d.getRemainingDebt() > 0) total += d.getRemainingDebt();
        }
        return total;
    }
}
