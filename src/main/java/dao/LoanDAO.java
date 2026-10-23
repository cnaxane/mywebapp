package dao;

import db.DBConnection;
import model.Loan;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class LoanDAO {

    public List<Loan> findByUserId(int userId) {
        List<Loan> loans = new ArrayList<>();
        String sql = "SELECT loan_id, user_id, loan_name, lender_name, loan_type, " +
                "principal_amount, outstanding_balance, annual_interest_rate, annual_rate_type, " +
                "tenure_months, start_date, due_day, loan_status " +
                "FROM loan WHERE user_id = ? ORDER BY loan_id ASC";

        try {
            Connection conn = DBConnection.getInstance().getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        loans.add(mapResultSetToLoan(rs));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[LoanDAO] Error in findByUserId: " + e.getMessage());
        }
        return loans;
    }

    public List<Loan> findActiveByUserId(int userId) {
        List<Loan> loans = new ArrayList<>();
        String sql = "SELECT loan_id, user_id, loan_name, lender_name, loan_type, " +
                "principal_amount, outstanding_balance, annual_interest_rate, annual_rate_type, " +
                "tenure_months, start_date, due_day, loan_status " +
                "FROM loan WHERE user_id = ? AND loan_status = 'ACTIVE' AND outstanding_balance > 0 ORDER BY loan_id ASC";

        try {
            Connection conn = DBConnection.getInstance().getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        loans.add(mapResultSetToLoan(rs));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[LoanDAO] Error in findActiveByUserId: " + e.getMessage());
        }
        return loans;
    }

    public Loan findById(int loanId) {
        String sql = "SELECT loan_id, user_id, loan_name, lender_name, loan_type, " +
                "principal_amount, outstanding_balance, annual_interest_rate, annual_rate_type, " +
                "tenure_months, start_date, due_day, loan_status " +
                "FROM loan WHERE loan_id = ?";

        try {
            Connection conn = DBConnection.getInstance().getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, loanId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return mapResultSetToLoan(rs);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[LoanDAO] Error in findById: " + e.getMessage());
        }
        return null;
    }

    public boolean insert(Loan loan) {
        String sql = "INSERT INTO loan (user_id, loan_name, lender_name, loan_type, " +
                "principal_amount, outstanding_balance, annual_interest_rate, annual_rate_type, " +
                "annual_rate_start_date, tenure_months, start_date, due_day, loan_status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try {
            Connection conn = DBConnection.getInstance().getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setInt(1, loan.getUserId());
                stmt.setString(2, loan.getLoanName());
                stmt.setString(3, loan.getLenderName());

                String loanType = loan.getLoanType() != null ? loan.getLoanType().toUpperCase() : "OTHER";
                if (!loanType.equals("HOME") && !loanType.equals("AUTO") && !loanType.equals("PERSONAL") && !loanType.equals("EDUCATION")) {
                    loanType = "OTHER";
                }
                stmt.setString(4, loanType);

                stmt.setDouble(5, loan.getPrincipalAmount());
                stmt.setDouble(6, loan.getOutstandingBalance());
                stmt.setDouble(7, loan.getAnnualInterestRate());

                String rateType = loan.getAnnualRateType() != null ? loan.getAnnualRateType().toUpperCase() : "FIXED";
                if (!rateType.equals("FIXED") && !rateType.equals("FLOATING")) {
                    rateType = "FIXED";
                }
                stmt.setString(8, rateType);

                LocalDate startDate = loan.getStartDate() != null ? loan.getStartDate() : LocalDate.now();
                stmt.setDate(9, Date.valueOf(startDate)); // annual_rate_start_date
                stmt.setInt(10, loan.getTenureMonths() > 0 ? loan.getTenureMonths() : 12);
                stmt.setDate(11, Date.valueOf(startDate)); // start_date

                int dueDay = loan.getDueDay();
                if (dueDay < 1 || dueDay > 28) {
                    dueDay = 5;
                }
                stmt.setInt(12, dueDay);

                String status = loan.getLoanStatus() != null ? loan.getLoanStatus().toUpperCase() : "ACTIVE";
                if (!status.equals("ACTIVE") && !status.equals("PAID_OFF") && !status.equals("CLOSED")) {
                    status = "ACTIVE";
                }
                stmt.setString(13, status);

                int affected = stmt.executeUpdate();
                if (affected > 0) {
                    try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            loan.setLoanId(generatedKeys.getInt(1));
                        }
                    }
                    return true;
                }
            }
        } catch (SQLException e) {
            System.err.println("[LoanDAO] Error in insert: " + e.getMessage());
        }
        return false;
    }

    public boolean updateLoan(Loan loan) {
        if (loan == null) {
            return false;
        }

        String sql = "UPDATE loan SET loan_name = ?, lender_name = ?, loan_type = ?, " +
                "principal_amount = ?, outstanding_balance = ?, annual_interest_rate = ?, " +
                "annual_rate_type = ?, tenure_months = ?, start_date = ?, due_day = ?, loan_status = ? " +
                "WHERE loan_id = ?";

        try {
            Connection conn = DBConnection.getInstance().getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, loan.getLoanName());
                stmt.setString(2, loan.getLenderName());

                String loanType = loan.getLoanType() != null ? loan.getLoanType().toUpperCase() : "OTHER";
                if (!loanType.equals("HOME") && !loanType.equals("AUTO") && !loanType.equals("PERSONAL") && !loanType.equals("EDUCATION")) {
                    loanType = "OTHER";
                }
                stmt.setString(3, loanType);

                stmt.setDouble(4, loan.getPrincipalAmount());
                stmt.setDouble(5, Math.max(0.0, loan.getOutstandingBalance()));
                stmt.setDouble(6, loan.getAnnualInterestRate());

                String rateType = loan.getAnnualRateType() != null ? loan.getAnnualRateType().toUpperCase() : "FIXED";
                if (!rateType.equals("FIXED") && !rateType.equals("FLOATING")) {
                    rateType = "FIXED";
                }
                stmt.setString(7, rateType);

                stmt.setInt(8, Math.max(1, loan.getTenureMonths()));
                stmt.setDate(9, loan.getStartDate() != null ? Date.valueOf(loan.getStartDate()) : Date.valueOf(LocalDate.now()));

                int dueDay = loan.getDueDay();
                if (dueDay < 1 || dueDay > 31) {
                    dueDay = 5;
                }
                stmt.setInt(10, dueDay);

                String status = loan.getLoanStatus() != null ? loan.getLoanStatus().toUpperCase() : "ACTIVE";
                if (!status.equals("ACTIVE") && !status.equals("PAID_OFF") && !status.equals("CLOSED")) {
                    status = (loan.getOutstandingBalance() <= 0) ? "PAID_OFF" : "ACTIVE";
                }
                stmt.setString(11, status);

                stmt.setInt(12, loan.getLoanId());

                return stmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("[LoanDAO] Error in updateLoan: " + e.getMessage());
        }
        return false;
    }

    public boolean update(Loan loan) {
        return updateLoan(loan);
    }

    public boolean updateOutstandingBalance(int loanId, double newBalance) {
        String sql = "UPDATE loan SET outstanding_balance = ?, loan_status = CASE WHEN ? <= 0 THEN 'PAID_OFF' ELSE loan_status END WHERE loan_id = ?";

        try {
            Connection conn = DBConnection.getInstance().getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setDouble(1, Math.max(0.0, newBalance));
                stmt.setDouble(2, newBalance);
                stmt.setInt(3, loanId);
                return stmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("[LoanDAO] Error in updateOutstandingBalance: " + e.getMessage());
        }
        return false;
    }

    public boolean delete(int loanId) {
        String sql = "DELETE FROM loan WHERE loan_id = ?";

        try {
            Connection conn = DBConnection.getInstance().getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, loanId);
                return stmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("[LoanDAO] Error in delete: " + e.getMessage());
        }
        return false;
    }

    private Loan mapResultSetToLoan(ResultSet rs) throws SQLException {
        Date sqlDate = rs.getDate("start_date");
        LocalDate startDate = sqlDate != null ? sqlDate.toLocalDate() : LocalDate.now();

        return new Loan(
                rs.getInt("loan_id"),
                rs.getInt("user_id"),
                rs.getString("loan_name"),
                rs.getString("lender_name"),
                rs.getString("loan_type"),
                rs.getDouble("principal_amount"),
                rs.getDouble("outstanding_balance"),
                rs.getDouble("annual_interest_rate"),
                rs.getString("annual_rate_type"),
                rs.getInt("tenure_months"),
                startDate,
                rs.getInt("due_day"),
                rs.getString("loan_status")
        );
    }
}
