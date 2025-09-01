package com.abebegetachew.abebe.salarymanagement.abebegetachew;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class PaidDebtsAdapter extends RecyclerView.Adapter<PaidDebtsAdapter.ViewHolder> {

    private final Context context;
    private final List<Debt> debts;
    private boolean multiSelectMode = false;
    private final List<Debt> selectedItems = new ArrayList<>();
    private Listener listener;

    public PaidDebtsAdapter(Context context, List<Debt> debts) {
        this.context = context;
        this.debts = debts;
    }

    // Allow activity/fragment to listen for selection mode
    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public interface Listener {
        void onMultiSelectEnabled(boolean enabled);
        void onListEmpty(boolean empty);
    }

    public void toggleMultiSelectMode(boolean enable) {
        multiSelectMode = enable;
        notifyDataSetChanged();
    }

    public List<Debt> getSelectedItems() {
        return new ArrayList<>(selectedItems);
    }

    private void toggleSelection(Debt debt) {
        if (selectedItems.contains(debt)) {
            selectedItems.remove(debt);
        } else {
            selectedItems.add(debt);
        }
        if (selectedItems.isEmpty()) {
            toggleMultiSelectMode(false);
            if (listener != null) listener.onMultiSelectEnabled(false);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_paid_debt, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Debt debt = debts.get(position);

        // Multi-select handling
        holder.checkBox.setVisibility(multiSelectMode ? View.VISIBLE : View.GONE);
        holder.checkBox.setChecked(selectedItems.contains(debt));

        holder.itemView.setOnClickListener(v -> {
            if (multiSelectMode) {
                toggleSelection(debt);
                holder.checkBox.setChecked(selectedItems.contains(debt));
            } else {
                // normal click → expand details
                if (holder.layoutExpanded.getVisibility() == View.GONE) {
                    holder.layoutExpanded.setVisibility(View.VISIBLE);
                } else {
                    holder.layoutExpanded.setVisibility(View.GONE);
                }
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (!multiSelectMode) {
                toggleMultiSelectMode(true);
                toggleSelection(debt);
                notifyItemChanged(position);
                if (listener != null) listener.onMultiSelectEnabled(true);
            }
            return true;
        });

        // Bind normal data
        holder.tvBorrower.setText(PaidDebts.capitalizeWords(debt.getBorrower()));
        holder.tvAmountOwed.setText("Owed: " + debt.getAmountOwed() + " ETB");
        holder.tvAmountRepaid.setText("Paid: " + debt.getAmountPaid() + " ETB");
        holder.tvAmountPaidCollapsed.setText("Repaid: " + debt.getAmountPaid() + " ETB");
        holder.tvReason.setText("Reason: \n" + (debt.getReason() == null ? "-" : debt.getReason()));
        holder.tvRepaymentDate.setText(debt.getDate());
        holder.tvDateCollapsed.setText(debt.getDate());

        holder.btnClose.setOnClickListener(v -> holder.layoutExpanded.setVisibility(View.GONE));
    }

    // Clear selection properly
    public void clearSelection() {
        selectedItems.clear();
        multiSelectMode = false;
        notifyDataSetChanged();
        if (listener != null) {
            listener.onMultiSelectEnabled(false);
            listener.onListEmpty(debts.isEmpty());
        }
    }


    @Override
    public int getItemCount() {
        return debts.size();
    }

    // 🔥 Add this method to fix "Cannot resolve method removeItems"
    public void removeItems(List<Debt> itemsToRemove) {
        debts.removeAll(itemsToRemove);
        selectedItems.clear();
        toggleMultiSelectMode(false);
        notifyDataSetChanged();
        if (listener != null) {
            listener.onMultiSelectEnabled(false);
            listener.onListEmpty(debts.isEmpty());
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvBorrower, tvAmountOwed, tvAmountRepaid, tvReason, tvRepaymentDate, tvAmountPaidCollapsed, tvDateCollapsed;
        ImageView btnClose;
        View layoutExpanded;
        View layoutCollapsedCard;
        CheckBox checkBox;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBorrower = itemView.findViewById(R.id.tvBorrower);
            tvAmountOwed = itemView.findViewById(R.id.tvAmountOwed);
            tvAmountRepaid = itemView.findViewById(R.id.tvAmountRepaid);
            tvReason = itemView.findViewById(R.id.tvReason);
            tvRepaymentDate = itemView.findViewById(R.id.tvRepaymentDate);
            tvAmountPaidCollapsed = itemView.findViewById(R.id.tvAmountPaidCollapsed);
            tvDateCollapsed = itemView.findViewById(R.id.tvDateCollapsed);
            btnClose = itemView.findViewById(R.id.btnClose);
            layoutExpanded = itemView.findViewById(R.id.layoutExpanded);
            layoutCollapsedCard = itemView.findViewById(R.id.layoutCollapsedCard);

            // Make sure your item_paid_debt.xml has a CheckBox with id "checkBox"
            checkBox = itemView.findViewById(R.id.checkBox);
        }
    }
}
