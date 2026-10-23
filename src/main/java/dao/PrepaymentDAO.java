package dao;

import db.DBConnection;
import exception.InvalidPrepaymentException;
import model.Loan;
import model.Prepayment;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PrepaymentDAO {

    private final LoanDAO loanDAO = new LoanDAO();

    /**
     * Inserts a prepayment after validating that amount > 0 and amount <= current balance.
     *
     * @param prepayment prepayment details
     * @return true if insertion succeeded
     * @throws InvalidPrepaymentException if amount is non-positive or exceeds balance
     */
    public boolean insert(Prepayment prepayment) throws InvalidPrepaymentException {
        if (prepayment == null) {
            throw new InvalidPrepaymentException("Prepayment details cannot be null.");
        }

        if (prepayment.getAmount() <= 0) {
            throw new InvalidPrepaymentException("Prepayment amount must be strictly greater than zero.", prepayment.getAmount(), 0);
        }

        Loan loan = loanDAO.findById(prepayment.getLoanId());
        if (loan == null) {
            throw new InvalidPrepaymentException("Target loan with ID " + prepayment.getLoanId() + " does not exist.");
        }

        if (prepayment.getAmount() > loan.getOutstandingBalance() + 0.01) { // 0.01 tolerance for precision
            throw new InvalidPrepaymentException(
                    "Prepayment exceeds outstanding loan balance.",
                    prepayment.getAmount(),
                    loan.getOutstandingBalance()
            );
        }

        String sql = "INSERT INTO prepayment (loan_id, prepayment_date, amount, strategy, notes) " +
                "VALUES (?, ?, ?, ?, ?)";

        try {
            Connection conn = DBConnection.getInstance().getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setInt(1, prepayment.getLoanId());
                stmt.setDate(2, prepayment.getPrepaymentDate() != null ? Date.valueOf(prepayment.getPrepaymentDate()) : Date.valueOf(LocalDate.now()));
                stmt.setDouble(3, prepayment.getAmount());

                String strat = prepayment.getStrategy() != null ? prepayment.getStrategy().toUpperCase() : "MANUAL";
                if (strat.contains("AVALANCHE")) {
                    strat = "AVALANCHE";
                } else if (strat.contains("SNOWBALL")) {
                    strat = "SNOWBALL";
                } else {
                    strat = "MANUAL";
                }
                stmt.setString(4, strat);
                stmt.setString(5, prepayment.getNotes());

                int affected = stmt.executeUpdate();
                if (affected > 0) {
                    try (ResultSet keys = stmt.getGeneratedKeys()) {
                        if (keys.next()) {
                            prepayment.setPrepaymentId(keys.getInt(1));
                        }
                    }

                    // Fallback ensure balance is updated if DB triggers are not active
                    double newBalance = Math.max(0.0, loan.getOutstandingBalance() - prepayment.getAmount());
                    loanDAO.updateOutstandingBalance(loan.getLoanId(), newBalance);

                    return true;
                }
            }
        } catch (SQLException e) {
            System.err.println("[PrepaymentDAO] Error inserting prepayment: " + e.getMessage());
        }
        return false;
    }

    public List<Prepayment> findByLoanId(int loanId) {
        List<Prepayment> prepayments = new ArrayList<>();
        String sql = "SELECT prepayment_id, loan_id, prepayment_date, amount, strategy, notes " +
                "FROM prepayment WHERE loan_id = ? ORDER BY prepayment_date DESC, prepayment_id DESC";

        try {
            Connection conn = DBConnection.getInstance().getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, loanId);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Date sqlDate = rs.getDate("prepayment_date");
                        LocalDate prepDate = sqlDate != null ? sqlDate.toLocalDate() : LocalDate.now();

                        prepayments.add(new Prepayment(
                                rs.getInt("prepayment_id"),
                                rs.getInt("loan_id"),
                                prepDate,
                                rs.getDouble("amount"),
                                rs.getString("strategy"),
                                rs.getString("notes")
                        ));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[PrepaymentDAO] Error in findByLoanId: " + e.getMessage());
        }
        return prepayments;
    }
}
