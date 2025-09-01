package com.abebegetachew.abebe.salarymanagement.abebegetachew;

import android.content.Context;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
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

public class DaySummaryAdapter extends RecyclerView.Adapter<DaySummaryAdapter.ViewHolder> {

    private Context context;
    private List<DaySummary> list;
    private DatabaseHelper db;

    private boolean multiSelectEnabled = false;
    private List<DaySummary> selectedItems = new ArrayList<>();
    private Listener listener;
    private Map<String, Boolean> expandedMap = new HashMap<>();
    private int highlightedPosition = -1;

    public void setHighlightedPosition(int position) {
        highlightedPosition = position;
        notifyDataSetChanged();
    }



    public DaySummaryAdapter(Context context, List<DaySummary> list, DatabaseHelper db) {
        this.context = context;
        this.list = list;
        this.db = db;
    }

    public void updateList(List<DaySummary> newList) {
        list.clear();
        list.addAll(newList);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_day_summary, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DaySummary day = list.get(position);

        holder.tvDate.setText(day.getDate());

        double net = day.getNetTotal();
        holder.tvNet.setText(String.format("Net: %.2f ETB", net));

        if (net >= 0) {
            holder.tvNet.setTextColor(ContextCompat.getColor(context, R.color.positiveNet));
        } else {
            holder.tvNet.setTextColor(ContextCompat.getColor(context, R.color.negativeNet));
        }

        DividerItemDecoration divider = new DividerItemDecoration(context, DividerItemDecoration.VERTICAL);
        divider.setDrawable(ContextCompat.getDrawable(context, R.drawable.divider));

        // Setup Income RecyclerView
        List<Income> incomes = day.getIncomes();
        if (incomes.isEmpty()) {
            holder.tvIncomeTitle.setVisibility(View.GONE);
            holder.rvIncomes.setVisibility(View.GONE);
        } else {
            holder.tvIncomeTitle.setVisibility(View.VISIBLE);
            holder.rvIncomes.setVisibility(View.VISIBLE);
            IncomeAdapter incomeAdapter = new IncomeAdapter(context, incomes);
            holder.rvIncomes.setAdapter(incomeAdapter);
            holder.rvIncomes.setLayoutManager(new LinearLayoutManager(context));
            if (holder.rvIncomes.getItemDecorationCount() == 0) {
                holder.rvIncomes.addItemDecoration(divider);
            }
        }

        // Setup Expense RecyclerView
        List<Expense> expenses = day.getExpenses();
        if (expenses.isEmpty()) {
            holder.tvExpenseTitle.setVisibility(View.GONE);
            holder.rvExpenses.setVisibility(View.GONE);
        } else {
            holder.tvExpenseTitle.setVisibility(View.VISIBLE);
            holder.rvExpenses.setVisibility(View.VISIBLE);
            ExpenseAdapter expenseAdapter = new ExpenseAdapter(context, expenses);
            holder.rvExpenses.setAdapter(expenseAdapter);
            holder.rvExpenses.setLayoutManager(new LinearLayoutManager(context));
            if (holder.rvExpenses.getItemDecorationCount() == 0) {
                holder.rvExpenses.addItemDecoration(divider);
            }
        }

        // If both are empty, hide content layout completely
        if (incomes.isEmpty() && expenses.isEmpty()) {
            holder.tvIncomeTitle.setVisibility(View.GONE);
            holder.tvExpenseTitle.setVisibility(View.GONE);
            holder.contentLayout.setVisibility(View.GONE);
        } else {
            holder.contentLayout.setVisibility(day.isExpanded() ? View.VISIBLE : View.GONE);
        }

        holder.expandIcon.setRotation(day.isExpanded() ? 180f : 0f);

        holder.headerLayout.setSelected(position == highlightedPosition);

        holder.headerLayout.setOnClickListener(v -> {
            if (!multiSelectEnabled) {
                day.setExpanded(!day.isExpanded());
                notifyItemChanged(position);
            } else {
                toggleSelection(day);
            }
        });

        View.OnLongClickListener longClickListener = v -> {
            if (!multiSelectEnabled) {
                enableMultiSelect();
                toggleSelection(day);
            }
            return true;
        };

        holder.headerLayout.setOnLongClickListener(longClickListener);
        holder.contentLayout.setOnLongClickListener(longClickListener);

        holder.checkBox.setVisibility(multiSelectEnabled ? View.VISIBLE : View.GONE);
        holder.checkBox.setChecked(selectedItems.contains(day));
    }



    private void toggleSelection(DaySummary day) {
        if (selectedItems.contains(day)) selectedItems.remove(day);
        else selectedItems.add(day);

        notifyDataSetChanged();

        if (listener != null) listener.onMultiSelectEnabled(!selectedItems.isEmpty());

        if (selectedItems.isEmpty()) multiSelectEnabled = false;
    }

    private void enableMultiSelect() {
        multiSelectEnabled = true;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public void removeSelectedItems() {
        for (DaySummary day : selectedItems) {
            db.deleteByDate(day.getDate());
        }
        list.removeAll(selectedItems);
        selectedItems.clear();
        multiSelectEnabled = false;
        notifyDataSetChanged();

        if (listener != null) {
            listener.onMultiSelectEnabled(false);
            listener.onListEmpty(list.isEmpty());
        }
    }


    public void clearSelection() {
        selectedItems.clear();
        multiSelectEnabled = false;
        notifyDataSetChanged();
        if (listener != null) listener.onMultiSelectEnabled(false);
    }

    public List<DaySummary> getSelectedItems() {
        return new ArrayList<>(selectedItems);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public interface Listener {
        void onMultiSelectEnabled(boolean enabled);
        void onListEmpty(boolean empty);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvNet, tvExpenseTitle, tvIncomeTitle;
        LinearLayout contentLayout, headerLayout;
        ImageView expandIcon;
        MaterialCheckBox checkBox;
        RecyclerView rvIncomes, rvExpenses;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvNet = itemView.findViewById(R.id.tvNetAmount);
            tvExpenseTitle = itemView.findViewById(R.id.tvExpenseTitle);
            tvIncomeTitle = itemView.findViewById(R.id.tvIncomeTitle);
            contentLayout = itemView.findViewById(R.id.contentLayout);
            headerLayout = itemView.findViewById(R.id.headerLayout);
            expandIcon = itemView.findViewById(R.id.expandIcon);
            checkBox = itemView.findViewById(R.id.checkBox);
            rvIncomes = itemView.findViewById(R.id.rvIncomes);
            rvExpenses = itemView.findViewById(R.id.rvExpenses);
        }
    }
}
