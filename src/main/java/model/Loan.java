package model;

import java.time.LocalDate;

public class Loan {
    private int loanId;
    private int userId;
    private String loanName;
    private String lenderName;
    private String loanType;
    private double principalAmount;
    private double outstandingBalance;
    private double annualInterestRate;
    private String annualRateType;
    private int tenureMonths;
    private LocalDate startDate;
    private int dueDay;
    private String loanStatus;

    public Loan() {
        this.annualRateType = "FIXED";
        this.loanStatus = "ACTIVE";
        this.dueDay = 5;
    }

    public Loan(int loanId, int userId, String loanName, String lenderName, String loanType,
                double principalAmount, double outstandingBalance, double annualInterestRate,
                String annualRateType, int tenureMonths, LocalDate startDate, int dueDay,
                String loanStatus) {
        this.loanId = loanId;
        this.userId = userId;
        this.loanName = loanName;
        this.lenderName = lenderName;
        this.loanType = loanType;
        this.principalAmount = principalAmount;
        this.outstandingBalance = outstandingBalance;
        this.annualInterestRate = annualInterestRate;
        this.annualRateType = annualRateType;
        this.tenureMonths = tenureMonths;
        this.startDate = startDate;
        this.dueDay = dueDay;
        this.loanStatus = loanStatus;
    }

    public Loan(int userId, String loanName, String lenderName, String loanType,
                double principalAmount, double outstandingBalance, double annualInterestRate,
                String annualRateType, int tenureMonths, LocalDate startDate, int dueDay,
                String loanStatus) {
        this.userId = userId;
        this.loanName = loanName;
        this.lenderName = lenderName;
        this.loanType = loanType;
        this.principalAmount = principalAmount;
        this.outstandingBalance = outstandingBalance;
        this.annualInterestRate = annualInterestRate;
        this.annualRateType = annualRateType;
        this.tenureMonths = tenureMonths;
        this.startDate = startDate;
        this.dueDay = dueDay;
        this.loanStatus = loanStatus;
    }

    public int getLoanId() {
        return loanId;
    }

    public void setLoanId(int loanId) {
        this.loanId = loanId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getLoanName() {
        return loanName;
    }

    public void setLoanName(String loanName) {
        this.loanName = loanName;
    }

    public String getLenderName() {
        return lenderName;
    }

    public void setLenderName(String lenderName) {
        this.lenderName = lenderName;
    }

    public String getLoanType() {
        return loanType;
    }

    public void setLoanType(String loanType) {
        this.loanType = loanType;
    }

    public double getPrincipalAmount() {
        return principalAmount;
    }

    public void setPrincipalAmount(double principalAmount) {
        this.principalAmount = principalAmount;
    }

    public double getOutstandingBalance() {
        return outstandingBalance;
    }

    public void setOutstandingBalance(double outstandingBalance) {
        this.outstandingBalance = outstandingBalance;
    }

    public double getAnnualInterestRate() {
        return annualInterestRate;
    }

    public void setAnnualInterestRate(double annualInterestRate) {
        this.annualInterestRate = annualInterestRate;
    }

    public String getAnnualRateType() {
        return annualRateType;
    }

    public void setAnnualRateType(String annualRateType) {
        this.annualRateType = annualRateType;
    }

    public int getTenureMonths() {
        return tenureMonths;
    }

    public void setTenureMonths(int tenureMonths) {
        this.tenureMonths = tenureMonths;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public int getDueDay() {
        return dueDay;
    }

    public void setDueDay(int dueDay) {
        this.dueDay = dueDay;
    }

    public String getLoanStatus() {
        return loanStatus;
    }

    public void setLoanStatus(String loanStatus) {
        this.loanStatus = loanStatus;
    }

    @Override
    public String toString() {
        return loanName + " (" + lenderName + ") - Bal: ₹" + String.format("%.2f", outstandingBalance) + " @" + annualInterestRate + "%";
    }
}
