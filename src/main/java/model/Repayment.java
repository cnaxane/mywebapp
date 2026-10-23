package model;

import java.time.LocalDate;

public class Repayment {
    private int repaymentId;
    private int loanId;
    private LocalDate paymentDate;
    private double amount;
    private double principalPaid;
    private double interestPaid;
    private String paymentMethod;
    private String referenceCode;
    private String notes;

    public Repayment() {
        this.paymentDate = LocalDate.now();
        this.paymentMethod = "NET_BANKING";
    }

    public Repayment(int repaymentId, int loanId, LocalDate paymentDate, double amount,
                     double principalPaid, double interestPaid, String paymentMethod,
                     String referenceCode, String notes) {
        this.repaymentId = repaymentId;
        this.loanId = loanId;
        this.paymentDate = paymentDate;
        this.amount = amount;
        this.principalPaid = principalPaid;
        this.interestPaid = interestPaid;
        this.paymentMethod = paymentMethod;
        this.referenceCode = referenceCode;
        this.notes = notes;
    }

    public Repayment(int loanId, LocalDate paymentDate, double amount,
                     double principalPaid, double interestPaid, String paymentMethod,
                     String referenceCode, String notes) {
        this.loanId = loanId;
        this.paymentDate = paymentDate;
        this.amount = amount;
        this.principalPaid = principalPaid;
        this.interestPaid = interestPaid;
        this.paymentMethod = paymentMethod;
        this.referenceCode = referenceCode;
        this.notes = notes;
    }

    public int getRepaymentId() {
        return repaymentId;
    }

    public void setRepaymentId(int repaymentId) {
        this.repaymentId = repaymentId;
    }

    public int getLoanId() {
        return loanId;
    }

    public void setLoanId(int loanId) {
        this.loanId = loanId;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public double getPrincipalPaid() {
        return principalPaid;
    }

    public void setPrincipalPaid(double principalPaid) {
        this.principalPaid = principalPaid;
    }

    public double getInterestPaid() {
        return interestPaid;
    }

    public void setInterestPaid(double interestPaid) {
        this.interestPaid = interestPaid;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getReferenceCode() {
        return referenceCode;
    }

    public void setReferenceCode(String referenceCode) {
        this.referenceCode = referenceCode;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Override
    public String toString() {
        return "Repayment #" + repaymentId + " [Loan ID: " + loanId + ", Amount: ₹" + amount + "]";
    }
}
