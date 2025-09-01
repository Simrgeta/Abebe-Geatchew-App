package com.abebegetachew.abebe.salarymanagement.abebegetachew;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import androidx.appcompat.widget.SearchView;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class ShowNote extends AppCompatActivity {

    private DatabaseHelper db;
    private List<Note> notesList;
    private List<Note> filteredList;
    private NotesAdapter adapter;
    private ImageButton btnDelete;

    private RecyclerView recyclerView;
    private LinearLayout placeholder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_show_note);

        ConstraintLayout root = findViewById(R.id.root);
        MaterialCardView card = findViewById(R.id.card);
        placeholder = findViewById(R.id.placeholderCard);
        recyclerView = findViewById(R.id.recyclerNotes);
        SearchView searchView = findViewById(R.id.searchView);
        MaterialToolbar topAppBar = findViewById(R.id.topAppBar);
        btnDelete = findViewById(R.id.btnDelete);

        topAppBar.setNavigationOnClickListener(v -> finish());

        db = new DatabaseHelper(this);
        loadNotes();
        filteredList = new ArrayList<>(notesList);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotesAdapter(this, filteredList, db);
        recyclerView.setAdapter(adapter);

        adapter.setListener(new NotesAdapter.Listener() {
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


        btnDelete.setOnClickListener(v -> {
            adapter.removeSelectedItems();
        });

        setupSearch(searchView);

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
                    adapter.clearSelection();
                } else {
                    setEnabled(false);
                    ShowNote.super.onBackPressed();
                }
            }
        });

        updatePlaceholderVisibility();
    }

    private void loadNotes() {
        notesList = db.getAllNotes();
    }

    private void setupSearch(SearchView searchView) {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterList(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterList(newText);
                return true;
            }

            private void filterList(String query) {
                filteredList.clear();
                if (query == null || query.isEmpty()) {
                    filteredList.addAll(notesList);
                } else {
                    String lowerQuery = query.toLowerCase();
                    for (Note note : notesList) {
                        if (note.getDescription().toLowerCase().contains(lowerQuery) ||
                                note.getDate().toLowerCase().contains(lowerQuery)) {
                            filteredList.add(note);
                        }
                    }
                }
                adapter.updateList(filteredList);
                updatePlaceholderVisibility();
            }
        });
    }

    private void updatePlaceholderVisibility() {
        if (recyclerView != null && placeholder != null) {
            if (filteredList.isEmpty()) {
                recyclerView.setVisibility(View.GONE);
                placeholder.setVisibility(View.VISIBLE);
                placeholder.setAlpha(1f);
            } else {
                recyclerView.setVisibility(View.VISIBLE);
                placeholder.setVisibility(View.GONE);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNotes();
        filteredList.clear();
        filteredList.addAll(notesList);
        if (adapter != null) adapter.notifyDataSetChanged();
        updatePlaceholderVisibility();
    }
}
