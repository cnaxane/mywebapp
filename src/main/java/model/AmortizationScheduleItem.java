package model;

import java.time.LocalDate;

public class AmortizationScheduleItem {
    private int installmentNumber;
    private LocalDate paymentDate;
    private double openingBalance;
    private double emi;
    private double principalPaid;
    private double interestPaid;
    private double closingBalance;

    public AmortizationScheduleItem() {
    }

    public AmortizationScheduleItem(int installmentNumber, LocalDate paymentDate, double openingBalance,
                                    double emi, double principalPaid, double interestPaid, double closingBalance) {
        this.installmentNumber = installmentNumber;
        this.paymentDate = paymentDate;
        this.openingBalance = openingBalance;
        this.emi = emi;
        this.principalPaid = principalPaid;
        this.interestPaid = interestPaid;
        this.closingBalance = closingBalance;
    }

    public int getInstallmentNumber() {
        return installmentNumber;
    }

    public void setInstallmentNumber(int installmentNumber) {
        this.installmentNumber = installmentNumber;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
    }

    public double getOpeningBalance() {
        return openingBalance;
    }

    public void setOpeningBalance(double openingBalance) {
        this.openingBalance = openingBalance;
    }

    public double getEmi() {
        return emi;
    }

    public void setEmi(double emi) {
        this.emi = emi;
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

    public double getClosingBalance() {
        return closingBalance;
    }

    public void setClosingBalance(double closingBalance) {
        this.closingBalance = closingBalance;
    }
}
