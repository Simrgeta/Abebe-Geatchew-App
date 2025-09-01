package com.abebegetachew.abebe.salarymanagement.abebegetachew;

public class Income {
    private String incomeId;
    private double amount;
    private String date;

    // Constructor
    public Income(String incomeId, double amount, String date) {
        this.incomeId = incomeId;
        this.amount = amount;
        this.date = date;
    }

    // Getters
    public String getIncomeId() { return incomeId; }
    public double getAmount() { return amount; }
    public String getDate() { return date; }
}