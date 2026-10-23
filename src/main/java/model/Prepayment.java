package model;

import java.time.LocalDate;

public class Prepayment {
    private int prepaymentId;
    private int loanId;
    private LocalDate prepaymentDate;
    private double amount;
    private String strategy;
    private String notes;

    public Prepayment() {
        this.prepaymentDate = LocalDate.now();
        this.strategy = "MANUAL";
    }

    public Prepayment(int prepaymentId, int loanId, LocalDate prepaymentDate, double amount,
                      String strategy, String notes) {
        this.prepaymentId = prepaymentId;
        this.loanId = loanId;
        this.prepaymentDate = prepaymentDate;
        this.amount = amount;
        this.strategy = strategy;
        this.notes = notes;
    }

    public Prepayment(int loanId, LocalDate prepaymentDate, double amount,
                      String strategy, String notes) {
        this.loanId = loanId;
        this.prepaymentDate = prepaymentDate;
        this.amount = amount;
        this.strategy = strategy;
        this.notes = notes;
    }

    public int getPrepaymentId() {
        return prepaymentId;
    }

    public void setPrepaymentId(int prepaymentId) {
        this.prepaymentId = prepaymentId;
    }

    public int getLoanId() {
        return loanId;
    }

    public void setLoanId(int loanId) {
        this.loanId = loanId;
    }

    public LocalDate getPrepaymentDate() {
        return prepaymentDate;
    }

    public void setPrepaymentDate(LocalDate prepaymentDate) {
        this.prepaymentDate = prepaymentDate;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Override
    public String toString() {
        return "Prepayment #" + prepaymentId + " [Loan ID: " + loanId + ", Amount: ₹" + amount + ", Strategy: " + strategy + "]";
    }
}
