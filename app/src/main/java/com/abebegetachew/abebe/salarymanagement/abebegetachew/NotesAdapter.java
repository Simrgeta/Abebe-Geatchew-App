package com.abebegetachew.abebe.salarymanagement.abebegetachew;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.checkbox.MaterialCheckBox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.ViewHolder> {

    private Context context;
    private List<String> dateList;
    private Map<String, List<Note>> notesMap;
    private DatabaseHelper db;

    private boolean multiSelectEnabled = false;
    private List<String> selectedDates = new ArrayList<>();
    private Map<String, Boolean> expandedMap = new HashMap<>();
    private Listener listener;

    public NotesAdapter(Context context, List<Note> allNotes, DatabaseHelper db) {
        this.context = context;
        this.db = db;
        groupNotesByDate(allNotes);
    }

    private void groupNotesByDate(List<Note> allNotes) {
        notesMap = new HashMap<>();
        dateList = new ArrayList<>();
        expandedMap.clear();

        for (Note note : allNotes) {
            String date = note.getDate();
            if (!notesMap.containsKey(date)) {
                notesMap.put(date, new ArrayList<>());
                dateList.add(date);
                expandedMap.put(date, false); // default collapsed
            }
            notesMap.get(date).add(note);
        }
    }

    public void updateList(List<Note> allNotes) {
        groupNotesByDate(allNotes);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_note, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String date = dateList.get(position);
        holder.tvDate.setText(date);

        DividerItemDecoration divider = new DividerItemDecoration(context, DividerItemDecoration.VERTICAL);
        divider.setDrawable(ContextCompat.getDrawable(context, R.drawable.divider));

        List<Note> notesForDate = notesMap.get(date);
        holder.rvNotes.setLayoutManager(new LinearLayoutManager(context));
        holder.rvNotes.setAdapter(new InnerNoteAdapter(notesForDate, context));

        if (holder.rvNotes.getItemDecorationCount() == 0) {
            holder.rvNotes.addItemDecoration(divider);
        }

        boolean isExpanded = expandedMap.getOrDefault(date, false);
        holder.contentLayout.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
        holder.expandIcon.setRotation(isExpanded ? 180f : 0f);

        holder.checkBox.setVisibility(multiSelectEnabled ? View.VISIBLE : View.GONE);
        holder.checkBox.setChecked(selectedDates.contains(date));

        // Handle click and long click
        View.OnLongClickListener longClickListener = v -> {
            if (!multiSelectEnabled) {
                enableMultiSelect();
                toggleSelection(date);
            }
            return true;
        };

        holder.headerLayout.setOnClickListener(v -> {
            if (multiSelectEnabled) {
                toggleSelection(date);
            } else {
                expandedMap.put(date, !isExpanded);
                notifyItemChanged(position);
            }
        });

        holder.headerLayout.setOnLongClickListener(longClickListener);
        holder.contentLayout.setOnLongClickListener(longClickListener);
    }

    private void toggleSelection(String date) {
        if (selectedDates.contains(date)) selectedDates.remove(date);
        else selectedDates.add(date);

        notifyDataSetChanged();

        if (listener != null) listener.onMultiSelectEnabled(!selectedDates.isEmpty());

        if (selectedDates.isEmpty()) multiSelectEnabled = false;
    }

    private void enableMultiSelect() {
        multiSelectEnabled = true;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return dateList.size();
    }

    public void removeSelectedItems() {
        // Delete selected dates from DB
        for (String date : selectedDates) {
            db.deleteNoteByDate(date);
        }

        // Remove from adapter
        List<String> itemsToRemove = new ArrayList<>(selectedDates);
        dateList.removeAll(itemsToRemove);
        for (String date : itemsToRemove) {
            notesMap.remove(date);
            expandedMap.remove(date);
        }

        // Clear selection
        selectedDates.clear();
        multiSelectEnabled = false;

        // Notify adapter
        notifyDataSetChanged();

        // Notify listener
        if (listener != null) {
            listener.onMultiSelectEnabled(false);
            listener.onListEmpty(dateList.isEmpty());
        }
    }



    public void clearSelection() {
        selectedDates.clear();
        multiSelectEnabled = false;
        notifyDataSetChanged();
        if (listener != null) listener.onMultiSelectEnabled(false);
    }

    public List<String> getSelectedItems() {
        return new ArrayList<>(selectedDates);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public interface Listener {
        void onMultiSelectEnabled(boolean enabled);
        void onListEmpty(boolean empty);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate;
        RecyclerView rvNotes;
        LinearLayout headerLayout;
        LinearLayout contentLayout;
        ImageView expandIcon;
        MaterialCheckBox checkBox;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvDate);
            rvNotes = itemView.findViewById(R.id.rvNotes);
            headerLayout = itemView.findViewById(R.id.headerLayout);
            contentLayout = itemView.findViewById(R.id.contentLayout);
            expandIcon = itemView.findViewById(R.id.expandIcon);
            checkBox = itemView.findViewById(R.id.checkBox);
        }
    }

    private static class InnerNoteAdapter extends RecyclerView.Adapter<InnerNoteAdapter.NoteViewHolder> {
        private final List<Note> notes;
        private final Context context;

        InnerNoteAdapter(List<Note> notes, Context context) {
            this.notes = notes;
            this.context = context;
        }

        @NonNull
        @Override
        public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_note_inner, parent, false);
            return new NoteViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
            Note note = notes.get(position);
            holder.tvDescription.setText(note.getDescription());
            holder.tvAmount.setText(String.format("%.2f ETB", note.getAmount()));
            holder.tvAmount.setTextColor(note.getAmount() >= 0 ?
                    ContextCompat.getColor(context, R.color.positiveNet) :
                    ContextCompat.getColor(context, R.color.negativeNet));
        }

        @Override
        public int getItemCount() {
            return notes.size();
        }

        static class NoteViewHolder extends RecyclerView.ViewHolder {
            TextView tvDescription, tvAmount;

            NoteViewHolder(@NonNull View itemView) {
                super(itemView);
                tvDescription = itemView.findViewById(R.id.tvNoteDescription);
                tvAmount = itemView.findViewById(R.id.tvNoteAmount);
            }
        }
    }
}
