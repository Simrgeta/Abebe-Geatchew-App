package com.abebegetachew.abebe.salarymanagement.abebegetachew;

public class Debt {
    private int repaidId; // -1 if not from RepaidDebts
    private String id;
    private String borrower;
    private double amountOwed;
    private double amountPaid;
    private double remainingDebt;
    private String reason;
    private String date;

    // Constructor
    public Debt(int repaidId, String id, String borrower, double amountOwed, double amountPaid,
                double remainingDebt, String reason, String date) {
        this.repaidId = repaidId;
        this.id = id;
        this.borrower = borrower;
        this.amountOwed = amountOwed;
        this.amountPaid = amountPaid;
        this.remainingDebt = remainingDebt;
        this.reason = reason;
        this.date = date;
    }

    // Getters
    public int getRepaidId() { return repaidId; }
    public String getId() { return id; }
    public String getBorrower() { return borrower; }
    public double getAmountOwed() { return amountOwed; }
    public double getAmountPaid() { return amountPaid; }
    public double getRemainingDebt() { return remainingDebt; }
    public String getReason() { return reason; }
    public String getDate() { return date; }
}
