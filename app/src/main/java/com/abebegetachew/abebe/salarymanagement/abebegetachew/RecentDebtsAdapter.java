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

public class RecentDebtsAdapter extends RecyclerView.Adapter<RecentDebtsAdapter.DebtViewHolder> {

    private final Context context;
    private List<Debt> debtList;
    private List<Debt> originalList;
    private List<Debt> filteredList;

    public RecentDebtsAdapter(Context context, List<Debt> debts) {
        this.context = context;
        this.debtList = debts;
        this.originalList = new ArrayList<>(debts);
        this.filteredList = debts;
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
            for (Debt debt : originalList) {
                if (debt.getBorrower().toLowerCase().contains(lowerCaseQuery) ||
                        debt.getReason().toLowerCase().contains(lowerCaseQuery) ||
                        String.valueOf(debt.getRemainingDebt()).contains(lowerCaseQuery) ||
                        debt.getDate().toLowerCase().contains(lowerCaseQuery)) {
                    filteredList.add(debt);
                }
            }
        }
        Collections.reverse(filteredList); // optional: last items first
        notifyDataSetChanged();
    }


    @NonNull
    @Override
    public DebtViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_debt, parent, false);
        return new DebtViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DebtViewHolder holder, int position) {
        Debt debt = filteredList.get(position);

        // Show only capitalized first name
        String fullName = debt.getBorrower();
        String firstName = "";
        if (fullName != null && !fullName.trim().isEmpty()) {
            firstName = fullName.trim().split("\\s+")[0];
            firstName = firstName.substring(0, 1).toUpperCase() + firstName.substring(1).toLowerCase();
        }

        holder.debtName.setText(firstName);
        holder.debtAmount.setText("$" + String.format(Locale.US, "%.2f", debt.getAmountOwed()));
        holder.debtDate.setText(debt.getDate());


        // Make marquee work
        holder.debtAmount.setSelected(true);

        // Set click listener for the whole item
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, UnpaidDebts.class);
            // Optional: pass debt ID or info if needed
            intent.putExtra("debt_id", debt.getId()); // assuming Debt has getId()
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    public void updateList(List<Debt> newDebts) {
        this.originalList = new ArrayList<>(newDebts); // keep full list
        // Keep last 3 for default display
        List<Debt> displayList = new ArrayList<>(newDebts);
        if (displayList.size() > 2) {
            displayList = displayList.subList(displayList.size() - 2, displayList.size());
        }
        Collections.reverse(displayList);
        this.filteredList = displayList;
        notifyDataSetChanged();
    }


    static class DebtViewHolder extends RecyclerView.ViewHolder {
        TextView debtName, debtAmount, debtDate;

        public DebtViewHolder(@NonNull View itemView) {
            super(itemView);
            debtName = itemView.findViewById(R.id.debtName);
            debtAmount = itemView.findViewById(R.id.debtAmount);
            debtDate = itemView.findViewById(R.id.debtDate);
        }
    }
}
