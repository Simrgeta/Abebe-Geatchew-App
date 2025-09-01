package com.abebegetachew.abebe.salarymanagement.abebegetachew;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SearchView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class PaidDebts extends AppCompatActivity {

    private DatabaseHelper db;
    private List<Debt> paidList;
    private List<Debt> filteredList;
    private PaidDebtsAdapter adapter;
    private ImageButton btnDelete;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_paid_debts);

        ConstraintLayout root = findViewById(R.id.root);
        MaterialCardView card = findViewById(R.id.card);
        LinearLayout placeholder = findViewById(R.id.placeholderCard);
        RecyclerView recyclerView = findViewById(R.id.recyclerPaidDebts);
        SearchView searchView = findViewById(R.id.searchView);
        MaterialToolbar topAppBar = findViewById(R.id.topAppBar);
        btnDelete = findViewById(R.id.btnDelete);

        topAppBar.setNavigationOnClickListener(v -> finish());

        db = new DatabaseHelper(this);
        paidList = db.getPaidDebts(); // now returns Debt objects with repaidId
        filteredList = new ArrayList<>(paidList);

        if (paidList.isEmpty()) {
            recyclerView.setVisibility(RecyclerView.GONE);
            placeholder.setVisibility(LinearLayout.VISIBLE);
        } else {
            recyclerView.setVisibility(RecyclerView.VISIBLE);
            placeholder.setVisibility(LinearLayout.GONE);

            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            adapter = new PaidDebtsAdapter(this, filteredList);
            recyclerView.setAdapter(adapter);

            adapter.setListener(new PaidDebtsAdapter.Listener() {
                @Override
                public void onMultiSelectEnabled(boolean enabled) {
                    btnDelete.setVisibility(enabled ? View.VISIBLE : View.GONE);
                }

                @Override
                public void onListEmpty(boolean empty) {
                    RecyclerView recyclerView = findViewById(R.id.recyclerPaidDebts);
                    LinearLayout placeholder = findViewById(R.id.placeholderCard);

                    if (empty) {
                        recyclerView.setVisibility(View.GONE);
                        placeholder.setVisibility(View.VISIBLE);
                        placeholder.setAlpha(1f);
                    } else {
                        recyclerView.setVisibility(View.VISIBLE);
                        placeholder.setVisibility(View.GONE);
                    }
                }
            });

            btnDelete.setOnClickListener(v -> {
                List<Debt> toDelete = adapter.getSelectedItems();
                deleteFromDatabase(toDelete);
                adapter.removeItems(toDelete);
            });

            setupSearch(searchView, recyclerView, placeholder);
        }

        // Entry animations
        float ty = 24f * getResources().getDisplayMetrics().density;
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
            View target = paidList.isEmpty() ? placeholder : recyclerView;
            target.animate().alpha(1f).translationY(0f).setStartDelay(300).setDuration(durationRecycler).setInterpolator(interp).start();
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (adapter != null && !adapter.getSelectedItems().isEmpty()) {
                    // Clear all selections properly
                    adapter.clearSelection();
                } else {
                    // No selection, finish normally
                    setEnabled(false); // Disable this callback
                    PaidDebts.super.onBackPressed(); // call default back behavior
                }
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        paidList.clear();
        paidList.addAll(db.getPaidDebts());
        filteredList.clear();
        filteredList.addAll(paidList);

        if (adapter != null) adapter.notifyDataSetChanged();

        RecyclerView recyclerView = findViewById(R.id.recyclerPaidDebts);
        LinearLayout placeholder = findViewById(R.id.placeholderCard);

        if (paidList.isEmpty()) {
            recyclerView.setVisibility(RecyclerView.GONE);
            placeholder.setVisibility(LinearLayout.VISIBLE);
        } else {
            recyclerView.setVisibility(RecyclerView.VISIBLE);
            placeholder.setVisibility(LinearLayout.GONE);
        }
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
                filteredList.clear();
                if (TextUtils.isEmpty(query)) {
                    filteredList.addAll(paidList);
                } else {
                    String lowerQuery = query.toLowerCase();
                    for (Debt debt : paidList) {
                        if (debt.getBorrower().toLowerCase().contains(lowerQuery) ||
                                debt.getReason().toLowerCase().contains(lowerQuery) ||
                                String.valueOf(debt.getAmountPaid()).contains(lowerQuery) ||
                                String.valueOf(debt.getAmountOwed()).contains(lowerQuery) ||
                                debt.getDate().toLowerCase().contains(lowerQuery)) {
                            filteredList.add(debt);
                        }
                    }
                }
                adapter.notifyDataSetChanged();

                if (filteredList.isEmpty()) {
                    recyclerView.setVisibility(RecyclerView.GONE);
                    placeholder.setVisibility(LinearLayout.VISIBLE);
                    placeholder.setAlpha(1f);
                } else {
                    recyclerView.setVisibility(RecyclerView.VISIBLE);
                    placeholder.setVisibility(LinearLayout.GONE);
                }
            }
        });
    }

    private void deleteFromDatabase(List<Debt> toDelete) {
        for (Debt d : toDelete) {
            db.deleteRepaidDebtById(d.getRepaidId()); // 🔹 use repaidId now
        }
    }

    public static String capitalizeWords(String str) {
        if (str == null || str.isEmpty()) return str;
        String[] words = str.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1).toLowerCase()).append(" ");
        }
        return sb.toString().trim();
    }
}
