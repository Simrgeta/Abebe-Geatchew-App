package com.abebegetachew.abebe.salarymanagement.abebegetachew;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder> {

    private Context context;
    private List<Expense> expenseList;

    public ExpenseAdapter(Context context, List<Expense> expenseList) {
        this.context = context;
        this.expenseList = expenseList;
    }

    @NonNull
    @Override
    public ExpenseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_expense, parent, false);
        return new ExpenseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExpenseViewHolder holder, int position) {
        Expense expense = expenseList.get(position);
        holder.tvCategory.setText(expense.getCategory());
        String reason = expense.getReason();
        if (reason == null || reason.trim().isEmpty()) {
            holder.tvReason.setText("None");
        } else {
            holder.tvReason.setText(reason);
        }
        holder.tvAmount.setText(String.format("%.2f", expense.getAmount()) + " ETB");
    }

    @Override
    public int getItemCount() {
        return expenseList.size();
    }

    public static class ExpenseViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategory, tvReason, tvAmount;

        public ExpenseViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategory = itemView.findViewById(R.id.tvExpenseCategory);
            tvReason = itemView.findViewById(R.id.tvExpenseReason);
            tvAmount = itemView.findViewById(R.id.tvExpenseAmount);
        }
    }
}
