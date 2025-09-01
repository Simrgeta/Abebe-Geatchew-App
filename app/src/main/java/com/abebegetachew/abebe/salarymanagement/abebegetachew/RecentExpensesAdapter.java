package com.abebegetachew.abebe.salarymanagement.abebegetachew;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class RecentExpensesAdapter extends RecyclerView.Adapter<RecentExpensesAdapter.ExpenseViewHolder> {

    private final Context context;
    private List<Expense> expenseList;
    private List<Expense> originalList;
    private List<Expense> filteredList;

    public RecentExpensesAdapter(Context context, List<Expense> expenses) {
        this.context = context;
        this.expenseList = expenses;
        this.originalList = new ArrayList<>(expenses);
        this.filteredList = expenses;
    }

    public void filter(String query) {
        filteredList = new ArrayList<>();
        if (query == null || query.trim().isEmpty()) {
            // Show only last 2 items when search is cleared
            int size = originalList.size();
            for (int i = Math.max(0, size - 2); i < size; i++) {
                filteredList.add(originalList.get(i));
            }
        } else {
            String lowerCaseQuery = query.toLowerCase();
            for (Expense expense : originalList) {
                if (expense.getCategory().toLowerCase().contains(lowerCaseQuery) ||
                        expense.getReason().toLowerCase().contains(lowerCaseQuery) ||
                        String.valueOf(expense.getAmount()).contains(lowerCaseQuery) ||
                        expense.getDate().toLowerCase().contains(lowerCaseQuery)) {
                    filteredList.add(expense);
                }
            }
        }
        Collections.reverse(filteredList); // optional: last items first
        notifyDataSetChanged();
    }




    @NonNull
    @Override
    public ExpenseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_expenses, parent, false);
        return new ExpenseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExpenseViewHolder holder, int position) {
        Expense expense = filteredList.get(position);

        holder.expenseReason.setText(expense.getReason());
        holder.expenseAmount.setText("$" + String.format(Locale.US, "%.2f", expense.getAmount()));
        holder.expenseDate.setText(expense.getDate());

        // Set click listener for the whole item
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ShowExpense.class);
            // Optional: pass expense ID or info if needed
            intent.putExtra("expense_date", expense.getDate()); // assuming Expense has getId()
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    public void updateList(List<Expense> newExpenses) {
        this.originalList = new ArrayList<>(newExpenses); // keep full list
        List<Expense> displayList = new ArrayList<>(newExpenses);
        if (displayList.size() > 2) {
            displayList = displayList.subList(displayList.size() - 2, displayList.size());
        }
        Collections.reverse(displayList);
        this.filteredList = displayList;
        notifyDataSetChanged();
    }


    static class ExpenseViewHolder extends RecyclerView.ViewHolder {
        TextView expenseReason, expenseAmount, expenseDate;

        public ExpenseViewHolder(@NonNull View itemView) {
            super(itemView);
            expenseReason = itemView.findViewById(R.id.expenseReason);
            expenseAmount = itemView.findViewById(R.id.expenseAmount);
            expenseDate = itemView.findViewById(R.id.expenseDate);
        }
    }
}
