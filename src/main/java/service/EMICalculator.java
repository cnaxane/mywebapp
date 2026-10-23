package service;

import model.AmortizationScheduleItem;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class EMICalculator {

    /**
     * Calculates monthly Equated Monthly Installment (EMI) using reducing balance formula:
     * EMI = [P * r * (1+r)^n] / [(1+r)^n - 1]
     *
     * @param principal  principal amount (P)
     * @param annualRate annual interest rate in percent (e.g., 8.5 for 8.5%)
     * @param months     total loan tenure in months (n)
     * @return calculated monthly EMI amount
     */
    public static double calculateEMI(double principal, double annualRate, int months) {
        if (principal <= 0 || months <= 0) {
            return 0.0;
        }

        if (annualRate <= 0.00001) {
            return principal / months;
        }

        double monthlyRate = (annualRate / 100.0) / 12.0; // r
        double factor = Math.pow(1.0 + monthlyRate, months); // (1+r)^n
        double emi = (principal * monthlyRate * factor) / (factor - 1.0);

        if (Double.isNaN(emi) || Double.isInfinite(emi)) {
            return 0.0;
        }
        return emi;
    }

    /**
     * Generates a complete month-by-month amortization schedule for a loan.
     *
     * @param principal  outstanding principal
     * @param annualRate annual interest rate %
     * @param months     tenure in months
     * @param startDate  start date of the schedule
     * @return list of installment rows
     */
    public static List<AmortizationScheduleItem> generateAmortizationSchedule(
            double principal, double annualRate, int months, LocalDate startDate) {

        List<AmortizationScheduleItem> schedule = new ArrayList<>();
        if (principal <= 0 || months <= 0) {
            return schedule;
        }

        double emi = calculateEMI(principal, annualRate, months);
        double monthlyRate = (annualRate / 100.0) / 12.0;
        double currentBalance = principal;
        LocalDate installmentDate = startDate != null ? startDate : LocalDate.now();

        for (int i = 1; i <= months && currentBalance > 0.001; i++) {
            double opening = currentBalance;
            double interest = opening * monthlyRate;
            double principalComponent = emi - interest;

            if (principalComponent > opening || i == months) {
                // Final adjustment
                principalComponent = opening;
                emi = principalComponent + interest;
            }

            double closing = Math.max(0.0, opening - principalComponent);

            schedule.add(new AmortizationScheduleItem(
                    i,
                    installmentDate,
                    opening,
                    emi,
                    principalComponent,
                    interest,
                    closing
            ));

            currentBalance = closing;
            installmentDate = installmentDate.plusMonths(1);
        }

        return schedule;
    }

    /**
     * Generates an amortization schedule with custom scenario parameters:
     * rate adjustments and ongoing monthly prepayments from an effective month.
     *
     * @param principal       outstanding principal
     * @param initialRate     original annual interest rate %
     * @param revisedRate     revised annual interest rate % applied from effectiveMonth
     * @param effectiveMonth  1-based month index from which scenario rules apply
     * @param extraPrepayment extra monthly prepayment added to scheduled principal payment
     * @param originalTenure  original sanctioned tenure in months
     * @param startDate       start date of the schedule
     * @return list of installment rows reflecting the scenario
     */
    public static List<AmortizationScheduleItem> generateScenarioSchedule(
            double principal,
            double initialRate,
            double revisedRate,
            int effectiveMonth,
            double extraPrepayment,
            int originalTenure,
            LocalDate startDate) {

        List<AmortizationScheduleItem> schedule = new ArrayList<>();
        if (principal <= 0 || originalTenure <= 0) {
            return schedule;
        }

        double baseEMI = calculateEMI(principal, initialRate, originalTenure);
        int effMonth = Math.max(1, effectiveMonth);
        double extraPay = Math.max(0.0, extraPrepayment);

        double currentBalance = principal;
        LocalDate installmentDate = startDate != null ? startDate : LocalDate.now();
        int maxCapMonths = Math.max(originalTenure * 3, 600);

        for (int i = 1; i <= maxCapMonths && currentBalance > 0.001; i++) {
            double opening = currentBalance;
            double rate = (i >= effMonth) ? revisedRate : initialRate;
            double monthlyRate = (rate / 100.0) / 12.0;
            double interest = opening * monthlyRate;

            double extraThisMonth = (i >= effMonth) ? extraPay : 0.0;
            double scheduledPayment = baseEMI + extraThisMonth;

            // Ensure payment covers at least interest to avoid negative amortization
            if (scheduledPayment <= interest + 0.01) {
                scheduledPayment = interest + Math.max(500.0, baseEMI * 0.1);
            }

            double totalPayment;
            double principalComponent;

            if (opening + interest <= scheduledPayment + 0.01) {
                // Final payoff month
                principalComponent = opening;
                totalPayment = principalComponent + interest;
                currentBalance = 0.0;
            } else {
                totalPayment = scheduledPayment;
                principalComponent = Math.max(0.0, totalPayment - interest);
                currentBalance = Math.max(0.0, opening - principalComponent);
            }

            schedule.add(new AmortizationScheduleItem(
                    i,
                    installmentDate,
                    opening,
                    totalPayment,
                    principalComponent,
                    interest,
                    currentBalance
            ));

            installmentDate = installmentDate.plusMonths(1);
        }

        return schedule;
    }

    /**
     * Aggregated metrics summary of an amortization schedule.
     */
    public static class ScheduleSummary {
        private final int totalMonths;
        private final double totalInterest;
        private final double totalOutflow;
        private final double totalPrincipal;

        public ScheduleSummary(List<AmortizationScheduleItem> schedule) {
            this.totalMonths = schedule != null ? schedule.size() : 0;
            double interest = 0.0;
            double outflow = 0.0;
            double principal = 0.0;
            if (schedule != null) {
                for (AmortizationScheduleItem item : schedule) {
                    interest += item.getInterestPaid();
                    outflow += item.getEmi();
                    principal += item.getPrincipalPaid();
                }
            }
            this.totalInterest = interest;
            this.totalOutflow = outflow;
            this.totalPrincipal = principal;
        }

        public int getTotalMonths() { return totalMonths; }
        public double getTotalInterest() { return totalInterest; }
        public double getTotalOutflow() { return totalOutflow; }
        public double getTotalPrincipal() { return totalPrincipal; }
    }
}
