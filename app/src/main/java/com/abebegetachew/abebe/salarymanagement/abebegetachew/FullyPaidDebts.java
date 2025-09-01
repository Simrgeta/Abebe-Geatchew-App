package com.abebegetachew.abebe.salarymanagement.abebegetachew;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import androidx.appcompat.widget.SearchView;

import java.util.ArrayList;
import java.util.List;

public class FullyPaidDebts extends AppCompatActivity {

    private DatabaseHelper db;
    private List<Debt> paidList;
    private List<Debt> filteredList;
    private FullyPaidDebtsAdapter adapter;
    private ImageButton btnDelete;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_fully_paid_debts);

        ConstraintLayout root = findViewById(R.id.root);
        MaterialCardView card = findViewById(R.id.card);
        LinearLayout placeholder = findViewById(R.id.placeholderCard);
        RecyclerView recyclerView = findViewById(R.id.recyclerPaidDebts);
        SearchView searchView = findViewById(R.id.searchView);
        MaterialToolbar topAppBar = findViewById(R.id.topAppBar);
        btnDelete = findViewById(R.id.btnDelete); // ✅ Delete button in your layout

        topAppBar.setNavigationOnClickListener(v -> finish());

        db = new DatabaseHelper(this);
        paidList = db.getFullyPaidDebts();
        filteredList = new ArrayList<>(paidList);

        if (paidList.isEmpty()) {
            recyclerView.setVisibility(RecyclerView.GONE);
            placeholder.setVisibility(LinearLayout.VISIBLE);
        } else {
            recyclerView.setVisibility(RecyclerView.VISIBLE);
            placeholder.setVisibility(LinearLayout.GONE);

            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            adapter = new FullyPaidDebtsAdapter(this, filteredList);
            recyclerView.setAdapter(adapter);

            // 🔹 Multi-select listener
            adapter.setListener(new FullyPaidDebtsAdapter.Listener() {
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


            // 🔹 Delete selected items
            btnDelete.setOnClickListener(v -> {
                List<Debt> toDelete = adapter.getSelectedItems();
                deleteFromDatabase(toDelete);
                adapter.removeItems(toDelete);
            });

            setupSearch(searchView, recyclerView, placeholder);
        }

        // Entry animations
        float ty = 24f * getResources().getDisplayMetrics().density; // 24dp
        long durationCard = 1100L;
        long durationRecycler = 950L;

        card.setAlpha(0f);
        card.setTranslationY(ty);
        recyclerView.setAlpha(0f);
        recyclerView.setTranslationY(ty);
        placeholder.setAlpha(0f);
        placeholder.setTranslationY(ty);

        root.post(() -> {
            card.animate().alpha(1f).translationY(0f).setStartDelay(0).setDuration(durationCard).start();
            View target = paidList.isEmpty() ? placeholder : recyclerView;
            target.animate().alpha(1f).translationY(0f).setStartDelay(300).setDuration(durationRecycler).start();
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
                    FullyPaidDebts.super.onBackPressed(); // call default back behavior
                }
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        paidList.clear();
        paidList.addAll(db.getFullyPaidDebts());

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
                if (query == null || query.isEmpty()) {
                    filteredList.addAll(paidList);
                } else {
                    String lowerQuery = query.toLowerCase();
                    for (Debt debt : paidList) {
                        if ((debt.getBorrower() != null && debt.getBorrower().toLowerCase().contains(lowerQuery)) ||
                                (debt.getReason() != null && debt.getReason().toLowerCase().contains(lowerQuery)) ||
                                (debt.getDate() != null && debt.getDate().toLowerCase().contains(lowerQuery)) ||
                                String.valueOf(debt.getAmountOwed()).toLowerCase().contains(lowerQuery)) {
                            filteredList.add(debt);
                        }
                    }
                }

                adapter.notifyDataSetChanged();

                if (filteredList.isEmpty()) {
                    recyclerView.setVisibility(View.GONE);
                    placeholder.setVisibility(LinearLayout.VISIBLE);
                    placeholder.setAlpha(1f);
                } else {
                    recyclerView.setVisibility(View.VISIBLE);
                    placeholder.setVisibility(LinearLayout.GONE);
                }
            }
        });
    }

    private void deleteFromDatabase(List<Debt> toDelete) {
        for (Debt debt : toDelete) {
            db.deleteFullyPaidDebtById(debt.getId()); // Make sure this method exists
        }
    }
}
