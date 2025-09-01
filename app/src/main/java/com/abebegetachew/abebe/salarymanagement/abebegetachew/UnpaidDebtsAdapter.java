package com.abebegetachew.abebe.salarymanagement.abebegetachew;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class UnpaidDebtsAdapter extends RecyclerView.Adapter<UnpaidDebtsAdapter.ViewHolder> {

    private final Context context;
    private final List<Debt> debts;
    private int expandedPosition = RecyclerView.NO_POSITION;

    private int highlightedPosition = RecyclerView.NO_POSITION;

    public void setHighlightedPosition(int position) {
        highlightedPosition = position;
        notifyDataSetChanged();
    }


    // Interface for long-click actions
    private OnItemLongClickListener longClickListener;

    public interface OnItemLongClickListener {
        void onItemLongClick(int position);
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.longClickListener = listener;
    }

    public UnpaidDebtsAdapter(Context context, List<Debt> debts) {
        this.context = context;
        this.debts = debts;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_unpaid_debt, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Debt debt = debts.get(position);

        holder.tvBorrower.setText(debt.getBorrower());
        holder.tvRemaining.setText(debt.getRemainingDebt() + " ETB");
        holder.tvDate.setText(debt.getDate());
        holder.tvAmountOwed.setText("Owed: " + debt.getAmountOwed() + " ETB");
        holder.tvAmountPaid.setText("Paid: " + debt.getAmountPaid() + " ETB");
        holder.tvReason.setText("Reason: \n" + debt.getReason());

        // Toggle expanded/collapsed based on current expandedPosition
        boolean isExpanded = position == expandedPosition;
        holder.layoutExpanded.setVisibility(isExpanded ? View.VISIBLE : View.GONE);

        holder.layoutCollapsedCard.setSelected(position == highlightedPosition);
        holder.tvBorrower.setSelected(position == highlightedPosition);
        holder.tvRemaining.setSelected(position == highlightedPosition);

        holder.itemView.setOnClickListener(v -> {
            int adapterPos = holder.getAdapterPosition();
            if (adapterPos == RecyclerView.NO_POSITION) return;

            int oldExpanded = expandedPosition;
            if (isExpanded) {
                expandedPosition = RecyclerView.NO_POSITION;
            } else {
                expandedPosition = adapterPos;
            }
            notifyItemChanged(oldExpanded);
            notifyItemChanged(expandedPosition);
        });

        holder.btnClose.setOnClickListener(v -> {
            int adapterPos = holder.getAdapterPosition();
            if (adapterPos == RecyclerView.NO_POSITION) return;

            expandedPosition = RecyclerView.NO_POSITION;
            notifyItemChanged(adapterPos);
        });

        holder.itemView.setOnLongClickListener(v -> {
            int adapterPos = holder.getAdapterPosition();
            if (adapterPos == RecyclerView.NO_POSITION) return true;

            if (longClickListener != null) {
                longClickListener.onItemLongClick(adapterPos);
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return debts.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvBorrower, tvRemaining, tvDate, tvAmountOwed, tvAmountPaid, tvReason;
        View layoutCollapsed, layoutExpanded;
        View btnClose;
        View layoutCollapsedCard;


        ViewHolder(View itemView) {
            super(itemView);
            tvBorrower = itemView.findViewById(R.id.tvBorrower);
            tvRemaining = itemView.findViewById(R.id.tvRemaining);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvAmountOwed = itemView.findViewById(R.id.tvAmountOwed);
            tvAmountPaid = itemView.findViewById(R.id.tvAmountPaid);
            tvReason = itemView.findViewById(R.id.tvReason);
            layoutCollapsed = itemView.findViewById(R.id.layoutCollapsed);
            layoutExpanded = itemView.findViewById(R.id.layoutExpanded);
            btnClose = itemView.findViewById(R.id.btnClose);
            layoutCollapsedCard = itemView.findViewById(R.id.layoutCollapsedCard);
        }
    }

    public void updateList(List<Debt> newDebts) {
        this.debts.clear();
        this.debts.addAll(newDebts);
        notifyDataSetChanged();
    }

}
