package com.abebegetachew.abebe.salarymanagement.abebegetachew;

import java.util.List;

public class DaySummary {
    private String date;
    private List<Income> incomes;
    private List<Expense> expenses;
    private boolean isExpanded = false; // new property to track expansion

    public DaySummary(String date, List<Income> incomes, List<Expense> expenses) {
        this.date = date;
        this.incomes = incomes;
        this.expenses = expenses;
    }

    public String getDate() {
        return date;
    }

    public List<Income> getIncomes() {
        return incomes;
    }

    public List<Expense> getExpenses() {
        return expenses;
    }

    public boolean isExpanded() {
        return isExpanded;
    }

    public void setExpanded(boolean expanded) {
        this.isExpanded = expanded;
    }

    public double getIncomeTotal() {
        double sum = 0;
        for (Income i : incomes) sum += i.getAmount();
        return sum;
    }

    public double getExpenseTotal() {
        double sum = 0;
        for (Expense e : expenses) sum += e.getAmount();
        return sum;
    }

    public double getNetTotal() {
        return getIncomeTotal() - getExpenseTotal();
    }
}
