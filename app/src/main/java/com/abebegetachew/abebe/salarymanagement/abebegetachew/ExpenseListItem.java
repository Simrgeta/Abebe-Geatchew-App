package com.abebegetachew.abebe.salarymanagement.abebegetachew;

public class ExpenseListItem {
    public static final int TYPE_HEADER = 0;
    public static final int TYPE_ITEM = 1;

    private int type;
    private String category;   // for header
    private Expense expense;   // for item

    public ExpenseListItem(int type, String category, Expense expense) {
        this.type = type;
        this.category = category;
        this.expense = expense;
    }

    public String getHeaderTitle() {
        return category;
    }
    public int getType() { return type; }
    public String getCategory() { return category; }
    public Expense getExpense() { return expense; }
}

