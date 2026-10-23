package service;

import dao.AuditLogDAO;
import dao.LoanDAO;
import dao.PrepaymentDAO;
import dao.RepaymentDAO;
import dao.UserDAO;
import db.DBConnection;
import exception.InvalidPrepaymentException;
import model.AuditLog;
import model.Loan;
import model.Prepayment;
import model.Repayment;
import model.User;
import strategy.PayoffStrategy;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class LoanService {

    private final UserDAO userDAO;
    private final LoanDAO loanDAO;
    private final RepaymentDAO repaymentDAO;
    private final PrepaymentDAO prepaymentDAO;
    private final AuditLogDAO auditLogDAO;

    public LoanService() {
        this.userDAO = new UserDAO();
        this.loanDAO = new LoanDAO();
        this.repaymentDAO = new RepaymentDAO();
        this.prepaymentDAO = new PrepaymentDAO();
        this.auditLogDAO = new AuditLogDAO();
    }

    // --- User Operations ---

    public List<User> getAllUsers() {
        return userDAO.findAll();
    }

    public User getUserById(int userId) {
        return userDAO.findById(userId);
    }

    public User getUserByEmail(String email) {
        return userDAO.findByEmail(email);
    }

    public User registerUser(String fullName, String email, double monthlyIncome, String plainPassword) throws IllegalArgumentException {
        User user = userDAO.registerUser(fullName, email, monthlyIncome, plainPassword);
        if (user != null) {
            auditLogDAO.log(null, "USER_REGISTERED", "New user registered: " + user.getFullName() + " (" + user.getEmail() + ")");
        }
        return user;
    }

    public User authenticateUser(String email, String plainPassword) {
        User user = userDAO.authenticateUser(email, plainPassword);
        if (user != null) {
            auditLogDAO.log(null, "USER_LOGIN", "User authenticated: " + user.getFullName() + " (" + user.getEmail() + ")");
        }
        return user;
    }

    // --- Loan Operations ---

    public List<Loan> getActiveLoansByUserId(int userId) {
        return loanDAO.findActiveByUserId(userId);
    }

    public List<Loan> getAllLoansByUserId(int userId) {
        return loanDAO.findByUserId(userId);
    }

    public Loan getLoanById(int loanId) {
        return loanDAO.findById(loanId);
    }

    public boolean addLoan(Loan loan) {
        boolean success = loanDAO.insert(loan);
        if (success) {
            auditLogDAO.log(loan.getLoanId(), "LOAN_CREATED",
                    "Added loan '" + loan.getLoanName() + "' (Principal: ₹" +
                            String.format("%.2f", loan.getPrincipalAmount()) + ", Rate: " + loan.getAnnualInterestRate() + "%)");
        }
        return success;
    }

    public boolean updateLoan(Loan loan) {
        boolean success = loanDAO.updateLoan(loan);
        if (success) {
            auditLogDAO.log(loan.getLoanId(), "LOAN_UPDATED",
                    "Updated loan '" + loan.getLoanName() + "' (New Balance: ₹" +
                            String.format("%.2f", loan.getOutstandingBalance()) + ")");
        }
        return success;
    }

    public boolean deleteLoan(int loanId) {
        Loan loan = loanDAO.findById(loanId);
        boolean success = loanDAO.delete(loanId);
        if (success) {
            String name = (loan != null) ? loan.getLoanName() : "ID #" + loanId;
            auditLogDAO.log(loanId, "LOAN_DELETED", "Deleted loan '" + name + "'");
        }
        return success;
    }

    // --- Repayment & Prepayment Operations ---

    public boolean recordRepayment(Repayment repayment) {
        boolean success = repaymentDAO.insert(repayment);
        if (success) {
            // Adjust balance in loan table if trigger didn't handle it
            Loan loan = loanDAO.findById(repayment.getLoanId());
            if (loan != null) {
                double newBal = Math.max(0.0, loan.getOutstandingBalance() - repayment.getPrincipalPaid());
                loanDAO.updateOutstandingBalance(loan.getLoanId(), newBal);
            }
            auditLogDAO.log(repayment.getLoanId(), "REPAYMENT_RECORDED",
                    "Recorded repayment of ₹" + String.format("%.2f", repayment.getAmount()) +
                            " (Principal: ₹" + String.format("%.2f", repayment.getPrincipalPaid()) +
                            ", Interest: ₹" + String.format("%.2f", repayment.getInterestPaid()) + ")");
        }
        return success;
    }

    public boolean recordPrepayment(Prepayment prepayment) throws InvalidPrepaymentException {
        boolean success = prepaymentDAO.insert(prepayment);
        if (success) {
            auditLogDAO.log(prepayment.getLoanId(), "PREPAYMENT_APPLIED",
                    "Applied prepayment of ₹" + String.format("%.2f", prepayment.getAmount()) +
                            " [Strategy: " + prepayment.getStrategy() + "]");
        }
        return success;
    }

    public List<Repayment> getRepaymentsForLoan(int loanId) {
        return repaymentDAO.findByLoanId(loanId);
    }

    public List<Prepayment> getPrepaymentsForLoan(int loanId) {
        return prepaymentDAO.findByLoanId(loanId);
    }

    // --- Payoff Strategy Integration ---

    public List<Loan> getPrioritizedLoans(List<Loan> loans, PayoffStrategy strategy) {
        if (strategy == null) {
            return loans != null ? new ArrayList<>(loans) : new ArrayList<>();
        }
        return strategy.sortLoans(loans);
    }

    // --- Dashboard KPI Calculations ---

    public double getTotalDebt(int userId) {
        List<Loan> active = getActiveLoansByUserId(userId);
        double total = 0.0;
        for (Loan l : active) {
            total += l.getOutstandingBalance();
        }
        return total;
    }

    public int getActiveLoansCount(int userId) {
        return getActiveLoansByUserId(userId).size();
    }

    public double getTotalMonthlyEMI(int userId) {
        List<Loan> active = getActiveLoansByUserId(userId);
        double totalEMI = 0.0;
        for (Loan l : active) {
            totalEMI += EMICalculator.calculateEMI(
                    l.getOutstandingBalance(),
                    l.getAnnualInterestRate(),
                    l.getTenureMonths() > 0 ? l.getTenureMonths() : 12
            );
        }
        return totalEMI;
    }

    // --- Audit Log Access ---

    public List<AuditLog> getRecentAuditLogs(int limit) {
        return auditLogDAO.findRecent(limit);
    }

    // --- Database Schema Initializer / Seeder ---

    /**
     * Initializes database tables and seed data if they do not exist.
     */
    public synchronized void initializeDatabaseSchema() {
        try {
            DBConnection.getInstance().ensureDatabaseExists();
            Connection conn = DBConnection.getInstance().getConnection();

            // Check if user_account table exists and has data
            List<User> existingUsers = userDAO.findAll();
            if (existingUsers.isEmpty()) {
                System.out.println("[LoanService] Setting up schema and seeding initial data...");
                executeSetupScript(conn);
            }
        } catch (Exception e) {
            System.err.println("[LoanService] Schema initialization warning: " + e.getMessage());
        }
    }

    private void executeSetupScript(Connection conn) {
        Path scriptPath = Paths.get("sql/loanwise_setup.sql");
        if (Files.exists(scriptPath)) {
            try {
                String sqlContent = Files.readString(scriptPath);
                executeRawSql(conn, sqlContent);
            } catch (Exception e) {
                System.err.println("[LoanService] Could not read sql/loanwise_setup.sql: " + e.getMessage());
                fallbackBootstrap(conn);
            }
        } else {
            fallbackBootstrap(conn);
        }
    }

    private void executeRawSql(Connection conn, String script) {
        try (Statement stmt = conn.createStatement()) {
            // Split by semicolon outside triggers
            String[] commands = script.split(";");
            for (String cmd : commands) {
                String trimmed = cmd.trim();
                if (!trimmed.isEmpty() && !trimmed.startsWith("--") && !trimmed.startsWith("DELIMITER")) {
                    try {
                        stmt.execute(trimmed);
                    } catch (Exception ex) {
                        // Ignore individual command duplicates or trigger syntax subtleties
                    }
                }
            }
            System.out.println("[LoanService] Database script applied.");
        } catch (Exception e) {
            System.err.println("[LoanService] Error executing SQL script: " + e.getMessage());
        }
    }

    private void fallbackBootstrap(Connection conn) {
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS user_account (" +
                    "user_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "full_name VARCHAR(100) NOT NULL, " +
                    "email VARCHAR(100) NOT NULL UNIQUE, " +
                    "monthly_income DECIMAL(12,2) NOT NULL DEFAULT 0.00)");

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS loan (" +
                    "loan_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "user_id INT NOT NULL, " +
                    "loan_name VARCHAR(100) NOT NULL, " +
                    "lender_name VARCHAR(100) NOT NULL, " +
                    "loan_type VARCHAR(50) NOT NULL, " +
                    "principal_amount DECIMAL(12,2) NOT NULL, " +
                    "outstanding_balance DECIMAL(12,2) NOT NULL, " +
                    "annual_interest_rate DECIMAL(5,2) NOT NULL, " +
                    "annual_rate_type VARCHAR(20) NOT NULL DEFAULT 'REDUCING', " +
                    "tenure_months INT NOT NULL, " +
                    "start_date DATE NOT NULL, " +
                    "due_day INT NOT NULL DEFAULT 5, " +
                    "loan_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE')");

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS repayment (" +
                    "repayment_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "loan_id INT NOT NULL, " +
                    "payment_date DATE NOT NULL, " +
                    "amount DECIMAL(12,2) NOT NULL, " +
                    "principal_paid DECIMAL(12,2) NOT NULL, " +
                    "interest_paid DECIMAL(12,2) NOT NULL, " +
                    "payment_method VARCHAR(50) NOT NULL DEFAULT 'NET_BANKING', " +
                    "reference_code VARCHAR(100), notes TEXT)");

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS prepayment (" +
                    "prepayment_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "loan_id INT NOT NULL, " +
                    "prepayment_date DATE NOT NULL, " +
                    "amount DECIMAL(12,2) NOT NULL, " +
                    "strategy VARCHAR(50) NOT NULL DEFAULT 'MANUAL', notes TEXT)");

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS audit_log (" +
                    "log_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "loan_id INT NULL, " +
                    "action_type VARCHAR(50) NOT NULL, " +
                    "action_details TEXT NOT NULL, " +
                    "performed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

            // Insert initial default users if none
            stmt.executeUpdate("INSERT IGNORE INTO user_account (user_id, full_name, email, monthly_income) VALUES " +
                    "(1, 'Aarav Sharma', 'aarav.sharma@example.com', 125000.00), " +
                    "(2, 'Diya Patel', 'diya.patel@example.com', 95000.00), " +
                    "(3, 'Kabir Mehta', 'kabir.mehta@example.com', 160000.00)");

            // Insert initial default loans if none
            stmt.executeUpdate("INSERT IGNORE INTO loan (loan_id, user_id, loan_name, lender_name, loan_type, principal_amount, outstanding_balance, annual_interest_rate, annual_rate_type, tenure_months, start_date, due_day, loan_status) VALUES " +
                    "(101, 1, 'Dream Home Loan', 'HDFC Bank', 'HOME', 4500000.00, 4120000.00, 8.50, 'REDUCING', 240, '2023-01-15', 5, 'ACTIVE'), " +
                    "(102, 1, 'Creta SUV Auto Loan', 'ICICI Bank', 'AUTO', 900000.00, 620000.00, 9.25, 'REDUCING', 60, '2023-06-10', 10, 'ACTIVE'), " +
                    "(103, 1, 'Executive Personal Loan', 'State Bank of India', 'PERSONAL', 350000.00, 210000.00, 12.75, 'REDUCING', 36, '2024-02-01', 7, 'ACTIVE'), " +
                    "(104, 1, 'Credit Line Overdraft', 'Axis Bank', 'PERSONAL', 150000.00, 85000.00, 15.00, 'REDUCING', 24, '2024-05-12', 2, 'ACTIVE'), " +
                    "(201, 2, 'Higher Education Loan', 'Canara Bank', 'EDUCATION', 1800000.00, 1420000.00, 8.85, 'REDUCING', 84, '2022-09-01', 5, 'ACTIVE'), " +
                    "(202, 2, 'Relocation Personal Loan', 'Kotak Mahindra', 'PERSONAL', 250000.00, 135000.00, 13.50, 'REDUCING', 36, '2023-11-15', 10, 'ACTIVE'), " +
                    "(301, 3, 'Commercial Office Loan', 'State Bank of India', 'HOME', 6500000.00, 5800000.00, 8.75, 'REDUCING', 180, '2022-04-10', 5, 'ACTIVE'), " +
                    "(302, 3, 'EV Sedan Auto Loan', 'Tata Capital', 'AUTO', 1200000.00, 780000.00, 9.60, 'REDUCING', 48, '2023-08-01', 12, 'ACTIVE'), " +
                    "(303, 3, 'Gadget Flexi Loan', 'Bajaj Finserv', 'PERSONAL', 120000.00, 48000.00, 16.50, 'REDUCING', 18, '2024-03-20', 15, 'ACTIVE')");
        } catch (Exception e) {
            System.err.println("[LoanService] Fallback bootstrap warning: " + e.getMessage());
        }
    }
}
