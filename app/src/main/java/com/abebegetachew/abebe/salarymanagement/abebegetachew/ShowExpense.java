package com.abebegetachew.abebe.salarymanagement.abebegetachew;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import androidx.appcompat.widget.SearchView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class ShowExpense extends AppCompatActivity {

    private DatabaseHelper db;
    private List<DaySummary> daySummaries;
    private List<DaySummary> filteredList;
    private DaySummaryAdapter adapter;
    private ImageButton btnDelete;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_show_expense);

        ConstraintLayout root = findViewById(R.id.root);
        MaterialCardView card = findViewById(R.id.card);
        LinearLayout placeholder = findViewById(R.id.placeholderCard);
        RecyclerView recyclerView = findViewById(R.id.recyclerExpenses);
        SearchView searchView = findViewById(R.id.searchView);
        MaterialToolbar topAppBar = findViewById(R.id.topAppBar);
        btnDelete = findViewById(R.id.btnDelete);

        topAppBar.setNavigationOnClickListener(v -> finish());

        db = new DatabaseHelper(this);
        loadDaySummaries();
        filteredList = new ArrayList<>(daySummaries);




        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DaySummaryAdapter(this, filteredList, db);
        recyclerView.setAdapter(adapter);

        // Adapter listener for multi-select and empty list handling
        adapter.setListener(new DaySummaryAdapter.Listener() {
            @Override
            public void onMultiSelectEnabled(boolean enabled) {
                btnDelete.setVisibility(enabled ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onListEmpty(boolean empty) {
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

        // Delete selected items
        btnDelete.setOnClickListener(v -> {
            adapter.removeSelectedItems();
        });

        setupSearch(searchView, recyclerView, placeholder);

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("expense_date")) {
            String expense_date = intent.getStringExtra("expense_date");
            if (expense_date != null) {
                int positionToScroll = -1;
                for (int i = 0; i < filteredList.size(); i++) {
                    // Loop through DaySummary and check if any expense has this ID
                    for (Expense e : filteredList.get(i).getExpenses()) {
                        if (expense_date.equals(e.getDate())) {
                            positionToScroll = i;
                            break;
                        }
                    }
                    if (positionToScroll != -1) break;
                }

                if (positionToScroll != -1) {
                    final int finalPosition = positionToScroll;
                    recyclerView.post(() -> recyclerView.scrollToPosition(finalPosition));
                    adapter.setHighlightedPosition(finalPosition);
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        adapter.setHighlightedPosition(-1);
                    }, 3000); // highlight disappears after 3 seconds

                }
            }
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
            View target = filteredList.isEmpty() ? placeholder : recyclerView;
            target.animate().alpha(1f).translationY(0f).setStartDelay(300).setDuration(durationRecycler).start();
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (adapter != null && !adapter.getSelectedItems().isEmpty()) {
                    // Clear all selections properly
                    adapter.clearSelection();
                } else {
                    setEnabled(false);
                    ShowExpense.super.onBackPressed();
                }
            }
        });

        updatePlaceholderVisibility(recyclerView, placeholder);
    }

    private void loadDaySummaries() {
        daySummaries = new ArrayList<>();
        List<String> allDates = db.getAllDates();
        for (String date : allDates) {
            List<Income> incomes = db.getIncomesByDate(date);
            List<Expense> expenses = db.getExpensesByDate(date);
            daySummaries.add(new DaySummary(date, incomes, expenses));
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
                    filteredList.addAll(daySummaries);
                } else {
                    String lowerQuery = query.toLowerCase();
                    for (DaySummary day : daySummaries) {
                        boolean match = day.getDate().toLowerCase().contains(lowerQuery);
                        if (!match) {
                            for (Expense e : day.getExpenses()) {
                                if (e.getReason().toLowerCase().contains(lowerQuery) ||
                                        e.getCategory().toLowerCase().contains(lowerQuery)) {
                                    match = true;
                                    break;
                                }
                            }
                        }
                        if (match) filteredList.add(day);
                    }
                }
                adapter.updateList(filteredList);

                if (filteredList.isEmpty()) {
                    recyclerView.setVisibility(View.GONE);
                    placeholder.setVisibility(View.VISIBLE);
                } else {
                    recyclerView.setVisibility(View.VISIBLE);
                    placeholder.setVisibility(View.GONE);
                }
            }
        });
    }

    private void updatePlaceholderVisibility(RecyclerView recyclerView, LinearLayout placeholder) {
        if (filteredList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            placeholder.setVisibility(View.VISIBLE);
            placeholder.setAlpha(1f);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            placeholder.setVisibility(View.GONE);
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        LinearLayout placeholder = findViewById(R.id.placeholderCard);
        RecyclerView recyclerView = findViewById(R.id.recyclerExpenses);
        loadDaySummaries();
        filteredList.clear();
        filteredList.addAll(daySummaries);
        if (adapter != null) adapter.notifyDataSetChanged();
        updatePlaceholderVisibility(recyclerView, placeholder);

    }
}
