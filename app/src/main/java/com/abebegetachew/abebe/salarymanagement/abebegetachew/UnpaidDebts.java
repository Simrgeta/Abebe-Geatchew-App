package com.abebegetachew.abebe.salarymanagement.abebegetachew;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.SearchView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class UnpaidDebts extends AppCompatActivity {

    private DatabaseHelper db;
    private List<Debt> unpaidList;
    private List<Debt> filteredList;
    private UnpaidDebtsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_unpaid_debts);

        ConstraintLayout root = findViewById(R.id.root);
        MaterialCardView card = findViewById(R.id.card);
        LinearLayout placeholder = findViewById(R.id.placeholderCard);
        RecyclerView recyclerView = findViewById(R.id.recyclerUnpaidDebts);
        SearchView searchView = findViewById(R.id.searchView);
        MaterialToolbar topAppBar = findViewById(R.id.topAppBar);

        topAppBar.setNavigationOnClickListener(v -> finish());

        db = new DatabaseHelper(this);
        unpaidList = db.getUnpaidDebts();
        filteredList = new ArrayList<>(unpaidList);

        if (unpaidList.isEmpty()) {
            recyclerView.setVisibility(RecyclerView.GONE);
            placeholder.setVisibility(LinearLayout.VISIBLE);
        } else {
            recyclerView.setVisibility(RecyclerView.VISIBLE);
            placeholder.setVisibility(LinearLayout.GONE);

            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            adapter = new UnpaidDebtsAdapter(this, filteredList);
            recyclerView.setAdapter(adapter);

            Intent intent = getIntent();
            if (intent != null && intent.hasExtra("debt_id")) {
                String debtId = intent.getStringExtra("debt_id");
                if (debtId != null && !debtId.equals("-1")) {
                    int positionToScroll = -1;
                    for (int i = 0; i < filteredList.size(); i++) {
                        if (debtId.equals(filteredList.get(i).getId())) {
                            positionToScroll = i;
                            break;
                        }
                    }

                    if (positionToScroll != -1) {
                        final int finalPosition = positionToScroll; // make a final copy
                        recyclerView.post(() -> recyclerView.scrollToPosition(finalPosition));
                        adapter.setHighlightedPosition(finalPosition);
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            adapter.setHighlightedPosition(-1);
                        }, 3000); // highlight disappears after 3 seconds
                    }
                }
            }

            setupSearch(searchView, recyclerView, placeholder);
            setupContextMenu(recyclerView);
        }

        // ✅ Add entry animations like AddDebt
        float ty = 24f * getResources().getDisplayMetrics().density; // 24dp
        long durationCard = 1100L;
        long durationRecycler = 950L;

        Interpolator interp = AnimationUtils.loadInterpolator(this, android.R.interpolator.fast_out_slow_in);

        // Initial states
        card.setAlpha(0f);
        card.setTranslationY(ty);

        recyclerView.setAlpha(0f);
        recyclerView.setTranslationY(ty);

        placeholder.setAlpha(0f);
        placeholder.setTranslationY(ty);

        // Animate after layout is ready
        root.post(() -> {
            // Animate card
            card.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(0)
                    .setDuration(durationCard)
                    .setInterpolator(interp)
                    .start();

            // Animate recycler/placeholder
            View target = unpaidList.isEmpty() ? placeholder : recyclerView;
            target.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(300)
                    .setDuration(durationRecycler)
                    .setInterpolator(interp)
                    .start();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data from the database
        unpaidList.clear();
        unpaidList.addAll(db.getUnpaidDebts());

        // Filtered list should also reflect the latest data
        filteredList.clear();
        filteredList.addAll(unpaidList);

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }

        // Show/hide placeholder based on updated list
        RecyclerView recyclerView = findViewById(R.id.recyclerUnpaidDebts);
        LinearLayout placeholder = findViewById(R.id.placeholderCard);

        if (unpaidList.isEmpty()) {
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
                    filteredList.addAll(unpaidList);
                } else {
                    String lowerQuery = query.toLowerCase();
                    for (Debt debt : unpaidList) {
                        if (debt.getBorrower().toLowerCase().contains(lowerQuery) ||
                                debt.getReason().toLowerCase().contains(lowerQuery)) {
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

    private void setupContextMenu(RecyclerView recyclerView) {
        adapter.setOnItemLongClickListener(position -> {
            Debt debt = filteredList.get(position);

            View itemView = recyclerView.getLayoutManager().findViewByPosition(position);
            if (itemView == null) return;

            PopupMenu popup = new PopupMenu(this, itemView);

            // ✅ Tint icons with custom colors
            Drawable totalDebtIcon = AppCompatResources.getDrawable(this, R.drawable.ic_total_debt);
            if (totalDebtIcon != null) {
                totalDebtIcon = DrawableCompat.wrap(totalDebtIcon);
                DrawableCompat.setTint(totalDebtIcon, Color.GRAY); // Red
            }

            Drawable repayDebtIcon = AppCompatResources.getDrawable(this, R.drawable.ic_repay_debt);
            if (repayDebtIcon != null) {
                repayDebtIcon = DrawableCompat.wrap(repayDebtIcon);
                DrawableCompat.setTint(repayDebtIcon, Color.GRAY); // Green
            }

            popup.getMenu().add(0, 1, 0, "Show Total Debt").setIcon(totalDebtIcon);
            popup.getMenu().add(0, 2, 1, "Repay Debt").setIcon(repayDebtIcon);

            try {
                java.lang.reflect.Field[] fields = popup.getClass().getDeclaredFields();
                for (java.lang.reflect.Field field : fields) {
                    if ("mPopup".equals(field.getName())) {
                        field.setAccessible(true);
                        Object menuPopupHelper = field.get(popup);
                        Class<?> classPopupHelper = Class.forName(menuPopupHelper.getClass().getName());
                        java.lang.reflect.Method setForceIcons = classPopupHelper.getMethod("setForceShowIcon", boolean.class);
                        setForceIcons.invoke(menuPopupHelper, true);
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            popup.setOnMenuItemClickListener((MenuItem item) -> {
                if (item.getItemId() == 1) showTotalDebt(debt);
                else if (item.getItemId() == 2) repayDebt(debt);
                return true;
            });

            popup.show();
        });
    }

    private void repayDebt(Debt debt) {
        Intent intent = new Intent(this, RepayDebt.class);
        intent.putExtra("DEBT_ID", debt.getId());
        intent.putExtra("BORROWER_NAME", debt.getBorrower());
        startActivity(intent);
    }

    private void showTotalDebt(Debt debt) {
        Intent intent = new Intent(this, ShowTotalDebt.class);
        intent.putExtra("BORROWER_NAME", debt.getBorrower());
        startActivity(intent);
    }
}
