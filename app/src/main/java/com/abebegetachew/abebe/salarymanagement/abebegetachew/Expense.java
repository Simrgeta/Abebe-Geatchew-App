package com.abebegetachew.abebe.salarymanagement.abebegetachew;

public class Expense {
    private String expenseId;
    private double amount;
    private String reason;
    private String date;
    private String category; // New field

    // Constructor
    public Expense(String expenseId, double amount, String reason, String date, String category) {
        this.expenseId = expenseId;
        this.amount = amount;
        this.reason = reason;
        this.date = date;
        this.category = category;
    }

    // Getters
    public String getExpenseId() { return expenseId; }
    public double getAmount() { return amount; }
    public String getReason() { return reason; }
    public String getDate() { return date; }
    public String getCategory() { return category; } // New getter
}
