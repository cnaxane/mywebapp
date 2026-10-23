package dao;

import db.DBConnection;
import model.Repayment;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class RepaymentDAO {

    public boolean insert(Repayment repayment) {
        String sql = "INSERT INTO repayment (loan_id, payment_date, amount, principal_paid, " +
                "interest_paid, payment_method, reference_code, notes) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try {
            Connection conn = DBConnection.getInstance().getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setInt(1, repayment.getLoanId());
                stmt.setDate(2, repayment.getPaymentDate() != null ? Date.valueOf(repayment.getPaymentDate()) : Date.valueOf(LocalDate.now()));
                stmt.setDouble(3, repayment.getAmount());
                stmt.setDouble(4, repayment.getPrincipalPaid());
                stmt.setDouble(5, repayment.getInterestPaid());

                String method = repayment.getPaymentMethod() != null ? repayment.getPaymentMethod().toUpperCase() : "BANK_TRANSFER";
                if (!method.equals("UPI") && !method.equals("BANK_TRANSFER") && !method.equals("AUTO_DEBIT") && !method.equals("CASH")) {
                    method = "BANK_TRANSFER";
                }
                stmt.setString(6, method);
                stmt.setString(7, repayment.getReferenceCode());
                stmt.setString(8, repayment.getNotes());

                int affected = stmt.executeUpdate();
                if (affected > 0) {
                    try (ResultSet keys = stmt.getGeneratedKeys()) {
                        if (keys.next()) {
                            repayment.setRepaymentId(keys.getInt(1));
                        }
                    }
                    return true;
                }
            }
        } catch (SQLException e) {
            System.err.println("[RepaymentDAO] Error inserting repayment: " + e.getMessage());
        }
        return false;
    }

    public List<Repayment> findByLoanId(int loanId) {
        List<Repayment> repayments = new ArrayList<>();
        String sql = "SELECT repayment_id, loan_id, payment_date, amount, principal_paid, " +
                "interest_paid, payment_method, reference_code, notes " +
                "FROM repayment WHERE loan_id = ? ORDER BY payment_date DESC, repayment_id DESC";

        try {
            Connection conn = DBConnection.getInstance().getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, loanId);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Date sqlDate = rs.getDate("payment_date");
                        LocalDate paymentDate = sqlDate != null ? sqlDate.toLocalDate() : LocalDate.now();

                        repayments.add(new Repayment(
                                rs.getInt("repayment_id"),
                                rs.getInt("loan_id"),
                                paymentDate,
                                rs.getDouble("amount"),
                                rs.getDouble("principal_paid"),
                                rs.getDouble("interest_paid"),
                                rs.getString("payment_method"),
                                rs.getString("reference_code"),
                                rs.getString("notes")
                        ));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[RepaymentDAO] Error in findByLoanId: " + e.getMessage());
        }
        return repayments;
    }
}
