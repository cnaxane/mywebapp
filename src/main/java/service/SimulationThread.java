package service;

import model.Loan;
import strategy.PayoffStrategy;

import javax.swing.SwingUtilities;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class SimulationThread implements Runnable {

    public interface SimulationCallback {
        void onSimulationStarted();
        void onSimulationProgress(int percentComplete);
        void onSimulationCompleted(SimulationResult result);
        void onSimulationFailed(String errorMessage);
    }

    public static class SimulationResult {
        private final String strategyName;
        private final double extraMonthlyPrepayment;
        private final double baselineTotalInterest;
        private final int baselineMonths;
        private final double acceleratedTotalInterest;
        private final int acceleratedMonths;
        private final double interestSaved;
        private final int monthsSaved;
        private final LocalDate debtFreeDateBaseline;
        private final LocalDate debtFreeDateAccelerated;
        private final int totalLoansCount;
        private final double totalPrincipal;

        public SimulationResult(String strategyName, double extraMonthlyPrepayment,
                                double baselineTotalInterest, int baselineMonths,
                                double acceleratedTotalInterest, int acceleratedMonths,
                                double interestSaved, int monthsSaved,
                                LocalDate debtFreeDateBaseline, LocalDate debtFreeDateAccelerated,
                                int totalLoansCount, double totalPrincipal) {
            this.strategyName = strategyName;
            this.extraMonthlyPrepayment = extraMonthlyPrepayment;
            this.baselineTotalInterest = baselineTotalInterest;
            this.baselineMonths = baselineMonths;
            this.acceleratedTotalInterest = acceleratedTotalInterest;
            this.acceleratedMonths = acceleratedMonths;
            this.interestSaved = interestSaved;
            this.monthsSaved = monthsSaved;
            this.debtFreeDateBaseline = debtFreeDateBaseline;
            this.debtFreeDateAccelerated = debtFreeDateAccelerated;
            this.totalLoansCount = totalLoansCount;
            this.totalPrincipal = totalPrincipal;
        }

        public String getStrategyName() { return strategyName; }
        public double getExtraMonthlyPrepayment() { return extraMonthlyPrepayment; }
        public double getBaselineTotalInterest() { return baselineTotalInterest; }
        public int getBaselineMonths() { return baselineMonths; }
        public double getAcceleratedTotalInterest() { return acceleratedTotalInterest; }
        public int getAcceleratedMonths() { return acceleratedMonths; }
        public double getInterestSaved() { return interestSaved; }
        public int getMonthsSaved() { return monthsSaved; }
        public LocalDate getDebtFreeDateBaseline() { return debtFreeDateBaseline; }
        public LocalDate getDebtFreeDateAccelerated() { return debtFreeDateAccelerated; }
        public int getTotalLoansCount() { return totalLoansCount; }
        public double getTotalPrincipal() { return totalPrincipal; }
    }

    private static class LoanSimState {
        int id;
        String name;
        double balance;
        double annualRate;
        double monthlyRate;
        double regularEMI;
        boolean isPaidOff;

        LoanSimState(Loan loan) {
            this.id = loan.getLoanId();
            this.name = loan.getLoanName();
            this.balance = loan.getOutstandingBalance();
            this.annualRate = loan.getAnnualInterestRate();
            this.monthlyRate = (loan.getAnnualInterestRate() / 100.0) / 12.0;
            this.regularEMI = EMICalculator.calculateEMI(
                    loan.getOutstandingBalance() > 0 ? loan.getOutstandingBalance() : loan.getPrincipalAmount(),
                    loan.getAnnualInterestRate(),
                    loan.getTenureMonths() > 0 ? loan.getTenureMonths() : 12
            );
            this.isPaidOff = (this.balance <= 0.001);
        }

        LoanSimState(LoanSimState other) {
            this.id = other.id;
            this.name = other.name;
            this.balance = other.balance;
            this.annualRate = other.annualRate;
            this.monthlyRate = other.monthlyRate;
            this.regularEMI = other.regularEMI;
            this.isPaidOff = other.isPaidOff;
        }
    }

    private final List<Loan> loans;
    private final PayoffStrategy strategy;
    private final double extraMonthlyPrepayment;
    private final SimulationCallback callback;

    public SimulationThread(List<Loan> loans, PayoffStrategy strategy,
                            double extraMonthlyPrepayment, SimulationCallback callback) {
        this.loans = loans != null ? new ArrayList<>(loans) : new ArrayList<>();
        this.strategy = strategy;
        this.extraMonthlyPrepayment = Math.max(0.0, extraMonthlyPrepayment);
        this.callback = callback;
    }

    @Override
    public void run() {
        if (callback != null) {
            SwingUtilities.invokeLater(callback::onSimulationStarted);
        }

        try {
            if (loans.isEmpty()) {
                if (callback != null) {
                    SwingUtilities.invokeLater(() -> callback.onSimulationFailed("No active loans available to simulate."));
                }
                return;
            }

            // Simulate slight async computing delay to give realistic background worker feel
            Thread.sleep(250);

            // 1. Sort loans by chosen strategy
            List<Loan> prioritizedLoans = strategy.sortLoans(loans);

            double totalPrincipal = 0.0;
            for (Loan l : loans) {
                totalPrincipal += l.getOutstandingBalance();
            }

            // --- Baseline Projection ---
            List<LoanSimState> baselineLoans = new ArrayList<>();
            for (Loan l : prioritizedLoans) {
                baselineLoans.add(new LoanSimState(l));
            }

            double baselineTotalInterest = 0.0;
            int baselineMonths = 0;
            int maxCapMonths = 600; // 50 years max safe cap

            while (hasActiveLoans(baselineLoans) && baselineMonths < maxCapMonths) {
                baselineMonths++;
                for (LoanSimState s : baselineLoans) {
                    if (s.balance > 0.001) {
                        double interest = s.balance * s.monthlyRate;
                        baselineTotalInterest += interest;
                        double principalPaid = Math.min(s.balance, s.regularEMI - interest);
                        if (principalPaid < 0) {
                            principalPaid = 0; // Negative amortization guard
                        }
                        s.balance -= principalPaid;
                        if (s.balance <= 0.001) {
                            s.balance = 0.0;
                            s.isPaidOff = true;
                        }
                    }
                }
            }

            if (callback != null) {
                SwingUtilities.invokeLater(() -> callback.onSimulationProgress(50));
            }

            // --- Accelerated Projection ---
            List<LoanSimState> acceleratedLoans = new ArrayList<>();
            for (Loan l : prioritizedLoans) {
                acceleratedLoans.add(new LoanSimState(l));
            }

            double acceleratedTotalInterest = 0.0;
            int acceleratedMonths = 0;

            while (hasActiveLoans(acceleratedLoans) && acceleratedMonths < maxCapMonths) {
                acceleratedMonths++;

                // 1. Normal EMI payments and accrued interest
                double freedUpEMIFromFinishedLoans = 0.0;

                for (LoanSimState s : acceleratedLoans) {
                    if (s.balance > 0.001) {
                        double interest = s.balance * s.monthlyRate;
                        acceleratedTotalInterest += interest;
                        double regularPayment = Math.min(s.regularEMI, s.balance + interest);
                        double principalPaid = Math.max(0.0, regularPayment - interest);
                        s.balance -= principalPaid;
                        if (s.balance <= 0.001) {
                            s.balance = 0.0;
                            s.isPaidOff = true;
                        }
                    } else {
                        // Rollover effect: EMIs from previously paid off loans are rolled into extra prepayment pool
                        freedUpEMIFromFinishedLoans += s.regularEMI;
                    }
                }

                // 2. Extra monthly prepayment allocation according to strategy priority
                double pool = extraMonthlyPrepayment + freedUpEMIFromFinishedLoans;

                if (pool > 0.0) {
                    for (LoanSimState s : acceleratedLoans) {
                        if (s.balance > 0.001) {
                            double prepay = Math.min(pool, s.balance);
                            s.balance -= prepay;
                            pool -= prepay;
                            if (s.balance <= 0.001) {
                                s.balance = 0.0;
                                s.isPaidOff = true;
                            }
                            if (pool <= 0.001) {
                                break;
                            }
                        }
                    }
                }
            }

            if (callback != null) {
                SwingUtilities.invokeLater(() -> callback.onSimulationProgress(100));
            }

            // Results synthesis
            double interestSaved = Math.max(0.0, baselineTotalInterest - acceleratedTotalInterest);
            int monthsSaved = Math.max(0, baselineMonths - acceleratedMonths);
            LocalDate now = LocalDate.now();
            LocalDate baselineDate = now.plusMonths(baselineMonths);
            LocalDate acceleratedDate = now.plusMonths(acceleratedMonths);

            SimulationResult result = new SimulationResult(
                    strategy.getStrategyName(),
                    extraMonthlyPrepayment,
                    baselineTotalInterest,
                    baselineMonths,
                    acceleratedTotalInterest,
                    acceleratedMonths,
                    interestSaved,
                    monthsSaved,
                    baselineDate,
                    acceleratedDate,
                    loans.size(),
                    totalPrincipal
            );

            if (callback != null) {
                SwingUtilities.invokeLater(() -> callback.onSimulationCompleted(result));
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            if (callback != null) {
                SwingUtilities.invokeLater(() -> callback.onSimulationFailed("Simulation was cancelled."));
            }
        } catch (Exception e) {
            if (callback != null) {
                SwingUtilities.invokeLater(() -> callback.onSimulationFailed("Simulation error: " + e.getMessage()));
            }
        }
    }

    private boolean hasActiveLoans(List<LoanSimState> list) {
        for (LoanSimState s : list) {
            if (s.balance > 0.001) {
                return true;
            }
        }
        return false;
    }
}
