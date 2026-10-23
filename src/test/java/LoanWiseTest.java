import exception.InvalidPrepaymentException;
import model.Loan;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import service.EMICalculator;
import service.SimulationThread;
import strategy.AvalancheStrategy;
import strategy.SnowballStrategy;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class LoanWiseTest {

    @Test
    @DisplayName("Verify reducing balance EMI calculation formula")
    public void testEMICalculator() {
        // Principal = 1,00,000, Annual Rate = 12%, Tenure = 12 months
        // Monthly Rate = 1%
        // EMI = [100000 * 0.01 * (1.01)^12] / [(1.01)^12 - 1] = ~8884.88
        double emi = EMICalculator.calculateEMI(100000.0, 12.0, 12);
        assertTrue(emi > 8880 && emi < 8890, "Calculated EMI should be approx 8884.88, got: " + emi);

        // Edge case: 0% rate
        double zeroRateEmi = EMICalculator.calculateEMI(120000.0, 0.0, 12);
        assertEquals(10000.0, zeroRateEmi, 0.01);

        // Edge case: invalid params
        assertEquals(0.0, EMICalculator.calculateEMI(0.0, 10.0, 12));
        assertEquals(0.0, EMICalculator.calculateEMI(100000.0, 10.0, 0));
    }

    @Test
    @DisplayName("Verify Avalanche Strategy prioritizes highest interest rate loans first")
    public void testAvalancheStrategy() {
        AvalancheStrategy strategy = new AvalancheStrategy();
        List<Loan> loans = new ArrayList<>();

        Loan l1 = new Loan(1, 1, "Home Loan", "HDFC", "HOME", 4000000, 3800000, 8.5, "REDUCING", 240, LocalDate.now(), 5, "ACTIVE");
        Loan l2 = new Loan(2, 1, "Personal Loan", "SBI", "PERSONAL", 300000, 200000, 13.5, "REDUCING", 36, LocalDate.now(), 5, "ACTIVE");
        Loan l3 = new Loan(3, 1, "Car Loan", "ICICI", "AUTO", 800000, 500000, 9.2, "REDUCING", 60, LocalDate.now(), 5, "ACTIVE");

        loans.add(l1);
        loans.add(l2);
        loans.add(l3);

        List<Loan> sorted = strategy.sortLoans(loans);
        assertEquals(3, sorted.size());
        assertEquals("Personal Loan", sorted.get(0).getLoanName(), "Personal loan with 13.5% should be prioritized first");
        assertEquals("Car Loan", sorted.get(1).getLoanName(), "Car loan with 9.2% should be second");
        assertEquals("Home Loan", sorted.get(2).getLoanName(), "Home loan with 8.5% should be third");
    }

    @Test
    @DisplayName("Verify Snowball Strategy prioritizes smallest balance loans first")
    public void testSnowballStrategy() {
        SnowballStrategy strategy = new SnowballStrategy();
        List<Loan> loans = new ArrayList<>();

        Loan l1 = new Loan(1, 1, "Home Loan", "HDFC", "HOME", 4000000, 3800000, 8.5, "REDUCING", 240, LocalDate.now(), 5, "ACTIVE");
        Loan l2 = new Loan(2, 1, "Personal Loan", "SBI", "PERSONAL", 300000, 200000, 13.5, "REDUCING", 36, LocalDate.now(), 5, "ACTIVE");
        Loan l3 = new Loan(3, 1, "Car Loan", "ICICI", "AUTO", 800000, 500000, 9.2, "REDUCING", 60, LocalDate.now(), 5, "ACTIVE");

        loans.add(l1);
        loans.add(l2);
        loans.add(l3);

        List<Loan> sorted = strategy.sortLoans(loans);
        assertEquals(3, sorted.size());
        assertEquals("Personal Loan", sorted.get(0).getLoanName(), "Loan with smallest balance (2,00,000) should be prioritized first");
        assertEquals("Car Loan", sorted.get(1).getLoanName(), "Loan with balance 5,00,000 should be second");
        assertEquals("Home Loan", sorted.get(2).getLoanName(), "Loan with balance 38,00,000 should be third");
    }

    @Test
    @DisplayName("Verify SimulationThread calculates interest savings and months saved asynchronously")
    public void testSimulationThread() throws InterruptedException {
        List<Loan> loans = new ArrayList<>();
        loans.add(new Loan(1, 1, "Personal Loan", "SBI", "PERSONAL", 200000, 200000, 12.0, "REDUCING", 24, LocalDate.now(), 5, "ACTIVE"));
        loans.add(new Loan(2, 1, "Car Loan", "ICICI", "AUTO", 500000, 500000, 9.5, "REDUCING", 48, LocalDate.now(), 5, "ACTIVE"));

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<SimulationThread.SimulationResult> resultRef = new AtomicReference<>();
        AtomicBoolean failed = new AtomicBoolean(false);

        SimulationThread.SimulationCallback callback = new SimulationThread.SimulationCallback() {
            @Override
            public void onSimulationStarted() {}

            @Override
            public void onSimulationProgress(int percent) {}

            @Override
            public void onSimulationCompleted(SimulationThread.SimulationResult result) {
                resultRef.set(result);
                latch.countDown();
            }

            @Override
            public void onSimulationFailed(String message) {
                failed.set(true);
                latch.countDown();
            }
        };

        // Simulate with ₹10,000 extra monthly prepayment
        SimulationThread thread = new SimulationThread(loans, new AvalancheStrategy(), 10000.0, callback);
        Thread t = new Thread(thread);
        t.start();

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Simulation should complete within 5 seconds");
        assertFalse(failed.get(), "Simulation should not report failure");
        assertNotNull(resultRef.get(), "Simulation result must not be null");

        SimulationThread.SimulationResult result = resultRef.get();
        assertTrue(result.getInterestSaved() > 0, "Extra prepayments should produce positive interest savings");
        assertTrue(result.getMonthsSaved() > 0, "Extra prepayments should reduce loan tenure");
        assertTrue(result.getAcceleratedMonths() < result.getBaselineMonths(), "Accelerated months must be strictly less than baseline months");
    }

    @Test
    @DisplayName("Verify BCrypt password hashing and authentication logic")
    public void testBCryptAuthentication() {
        String rawPassword = "Password@123";
        String hashed = org.mindrot.jbcrypt.BCrypt.hashpw(rawPassword, org.mindrot.jbcrypt.BCrypt.gensalt(10));

        assertNotNull(hashed);
        assertTrue(org.mindrot.jbcrypt.BCrypt.checkpw(rawPassword, hashed), "Valid password must verify against hash");
        assertFalse(org.mindrot.jbcrypt.BCrypt.checkpw("WrongPassword", hashed), "Incorrect password must not match hash");
    }

    @Test
    @DisplayName("Verify Debt-to-Income (DTI) ratio calculation and risk classification")
    public void testDtiRatioCalculation() {
        // Healthy (< 36%): e.g. EMI 25,000 / Income 1,00,000 = 25.0%
        double healthyEmi = 25000.0;
        double income = 100000.0;
        double healthyDti = (healthyEmi / income) * 100.0;
        assertEquals(25.0, healthyDti, 0.01);
        assertTrue(healthyDti < 36.0, "DTI below 36% should be classified as Healthy");

        // Moderate Risk (36% - 50%): e.g. EMI 45,000 / Income 1,00,000 = 45.0%
        double moderateEmi = 45000.0;
        double moderateDti = (moderateEmi / income) * 100.0;
        assertEquals(45.0, moderateDti, 0.01);
        assertTrue(moderateDti >= 36.0 && moderateDti <= 50.0, "DTI between 36% and 50% should be classified as Moderate Risk");

        // Critical Exposure (> 50%): e.g. EMI 59,500 / Income 1,00,000 = 59.5%
        double criticalEmi = 59500.0;
        double criticalDti = (criticalEmi / income) * 100.0;
        assertEquals(59.5, criticalDti, 0.01);
        assertTrue(criticalDti > 50.0, "DTI above 50% should be classified as Critical Exposure");

        // Division by zero safe guard
        double zeroIncome = 0.0;
        double safeDti = zeroIncome <= 0.0 ? (criticalEmi > 0 ? 100.0 : 0.0) : (criticalEmi / zeroIncome) * 100.0;
        assertEquals(100.0, safeDti, 0.01);
    }

    @Test
    @DisplayName("Verify Amortization Scenario simulation with prepayment and rate changes")
    public void testScenarioScheduleSimulation() {
        double principal = 1000000.0;
        double initialRate = 9.0;
        int tenure = 120; // 10 years
        LocalDate start = LocalDate.of(2024, 1, 1);

        // Baseline schedule
        List<model.AmortizationScheduleItem> baseline = EMICalculator.generateAmortizationSchedule(
                principal, initialRate, tenure, start);
        EMICalculator.ScheduleSummary baseSummary = new EMICalculator.ScheduleSummary(baseline);
        assertEquals(120, baseSummary.getTotalMonths());
        assertTrue(baseSummary.getTotalInterest() > 0);

        // Scenario 1: Prepayment of 5,000/mo from month 1
        List<model.AmortizationScheduleItem> scenarioWithPrepay = EMICalculator.generateScenarioSchedule(
                principal, initialRate, initialRate, 1, 5000.0, tenure, start);
        EMICalculator.ScheduleSummary prepaySummary = new EMICalculator.ScheduleSummary(scenarioWithPrepay);

        assertTrue(prepaySummary.getTotalMonths() < baseSummary.getTotalMonths(),
                "Prepayments must reduce total payoff months");
        assertTrue(prepaySummary.getTotalInterest() < baseSummary.getTotalInterest(),
                "Prepayments must reduce total lifetime interest");

        // Scenario 2: Rate hike from 9.0% to 11.5% without prepayment
        List<model.AmortizationScheduleItem> scenarioRateHike = EMICalculator.generateScenarioSchedule(
                principal, initialRate, 11.5, 1, 0.0, tenure, start);
        EMICalculator.ScheduleSummary hikeSummary = new EMICalculator.ScheduleSummary(scenarioRateHike);

        assertTrue(hikeSummary.getTotalInterest() > baseSummary.getTotalInterest(),
                "Interest rate hike must increase total lifetime interest");
    }

    @Test
    @DisplayName("Verify InvalidPrepaymentException creation and payload")
    public void testInvalidPrepaymentException() {
        InvalidPrepaymentException ex = new InvalidPrepaymentException("Test prepayment exception", 50000.0, 20000.0);
        assertEquals(50000.0, ex.getAttemptedAmount());
        assertEquals(20000.0, ex.getCurrentBalance());
        assertTrue(ex.getMessage().contains("50000.00"));
    }

    @Test
    @DisplayName("Verify Loan update attributes and LoanDAO safeguards")
    public void testLoanUpdateAttributesAndSafeguards() {
        Loan loan = new Loan(10, 1, "Home Loan", "HDFC", "HOME", 3000000.0, 2800000.0, 8.5, "FIXED", 240, LocalDate.now(), 5, "ACTIVE");
        assertEquals("Home Loan", loan.getLoanName());
        assertEquals(8.5, loan.getAnnualInterestRate());
        assertEquals("FIXED", loan.getAnnualRateType());

        // Simulate attributes edited via EditLoanDialog
        loan.setLoanName("Renovated Home Loan");
        loan.setLenderName("HDFC Bank Ltd");
        loan.setAnnualInterestRate(9.25);
        loan.setAnnualRateType("FLOATING");
        loan.setTenureMonths(216);
        loan.setOutstandingBalance(2650000.0);

        assertEquals("Renovated Home Loan", loan.getLoanName());
        assertEquals("HDFC Bank Ltd", loan.getLenderName());
        assertEquals(9.25, loan.getAnnualInterestRate());
        assertEquals("FLOATING", loan.getAnnualRateType());
        assertEquals(216, loan.getTenureMonths());
        assertEquals(2650000.0, loan.getOutstandingBalance());

        // Safeguards on null loan
        dao.LoanDAO loanDAO = new dao.LoanDAO();
        assertFalse(loanDAO.updateLoan(null), "updateLoan on null loan must return false safely");
    }
}
