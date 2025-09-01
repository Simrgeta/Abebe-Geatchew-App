package com.abebegetachew.abebe.salarymanagement.abebegetachew;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.checkbox.MaterialCheckBox;

import java.util.ArrayList;
import java.util.List;

public class FullyPaidDebtsAdapter extends RecyclerView.Adapter<FullyPaidDebtsAdapter.ViewHolder> {

    private Context context;
    private List<Debt> debts;
    private List<Debt> selectedDebts = new ArrayList<>();
    private boolean multiSelectEnabled = false;
    private Listener listener;

    public FullyPaidDebtsAdapter(Context context, List<Debt> debts) {
        this.context = context;
        this.debts = debts;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_fully_paid_debt, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Debt debt = debts.get(position);

        holder.tvBorrower.setText(capitalizeWords(debt.getBorrower()));
        holder.tvAmountOwed.setText("Owed: " + debt.getAmountOwed() + " ETB");
        holder.tvDate.setText(debt.getDate());
        holder.tvReason.setText("Reason: " + (debt.getReason() == null ? "-" : debt.getReason()));

        // Handle checkbox visibility and state
        holder.checkBox.setVisibility(multiSelectEnabled ? View.VISIBLE : View.GONE);
        holder.checkBox.setChecked(selectedDebts.contains(debt));

        holder.itemView.setOnLongClickListener(v -> {
            if (!multiSelectEnabled) {
                enableMultiSelect();
                toggleSelection(debt);
            }
            return true;
        });

        holder.itemView.setOnClickListener(v -> {
            if (multiSelectEnabled) {
                toggleSelection(debt);
            }
            // else: normal click behavior (if any)
        });
    }

    @Override
    public int getItemCount() {
        return debts.size();
    }

    private void toggleSelection(Debt debt) {
        if (selectedDebts.contains(debt)) {
            selectedDebts.remove(debt);
        } else {
            selectedDebts.add(debt);
        }
        notifyDataSetChanged();

        if (listener != null) {
            listener.onMultiSelectEnabled(!selectedDebts.isEmpty());
        }

        // Disable multi-select if nothing is selected
        if (selectedDebts.isEmpty()) {
            multiSelectEnabled = false;
        }
    }

    private void enableMultiSelect() {
        multiSelectEnabled = true;
        notifyDataSetChanged();
    }

    public void removeItems(List<Debt> debtsToRemove) {
        debts.removeAll(debtsToRemove);
        selectedDebts.removeAll(debtsToRemove);
        notifyDataSetChanged();

        if (listener != null) {
            listener.onMultiSelectEnabled(!selectedDebts.isEmpty());
            listener.onListEmpty(debts.isEmpty());
        }
    }

    public void clearSelection() {
        selectedDebts.clear();
        multiSelectEnabled = false;
        notifyDataSetChanged();
        if (listener != null) {
            listener.onMultiSelectEnabled(false); // hides delete button
            listener.onListEmpty(debts.isEmpty());
        }
    }


    public List<Debt> getSelectedItems() {
        return new ArrayList<>(selectedDebts);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public interface Listener {
        void onMultiSelectEnabled(boolean enabled);
        void onListEmpty(boolean empty);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvBorrower, tvAmountOwed, tvDate, tvReason;
        MaterialCheckBox checkBox;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBorrower = itemView.findViewById(R.id.tvBorrower);
            tvAmountOwed = itemView.findViewById(R.id.tvAmountOwed);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvReason = itemView.findViewById(R.id.tvReason);
            checkBox = itemView.findViewById(R.id.checkBox);
        }
    }

    // Capitalize each word in the name
    public static String capitalizeWords(String str) {
        if (str == null || str.isEmpty()) return "";
        String[] words = str.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (word.length() > 0) {
                sb.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1).toLowerCase())
                        .append(" ");
            }
        }
        return sb.toString().trim();
    }
}
