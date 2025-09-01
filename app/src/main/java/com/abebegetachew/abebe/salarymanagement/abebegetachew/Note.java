package com.abebegetachew.abebe.salarymanagement.abebegetachew;

public class Note {
    private String description;
    private double amount;
    private String date; // will serve as the unique ID

    // Constructor
    public Note(String description, double amount, String date) {
        this.description = description;
        this.amount = amount;
        this.date = date;
    }

    // Getters
    public String getDescription() { return description; }
    public double getAmount() { return amount; }
    public String getDate() { return date; }
}
