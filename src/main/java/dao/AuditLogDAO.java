package dao;

import db.DBConnection;
import model.AuditLog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AuditLogDAO {

    public List<AuditLog> findRecent(int limit) {
        List<AuditLog> logs = new ArrayList<>();
        int max = limit > 0 ? limit : 50;

        // Try schema with action_timestamp (from sql/loanwise_setup.sql)
        String sql1 = "SELECT log_id, loan_id, action_type, action_details, action_timestamp " +
                "FROM audit_log ORDER BY action_timestamp DESC, log_id DESC LIMIT ?";

        try {
            Connection conn = DBConnection.getInstance().getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql1)) {
                stmt.setInt(1, max);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        int logId = rs.getInt("log_id");
                        int loanIdVal = rs.getInt("loan_id");
                        Integer loanId = rs.wasNull() ? null : loanIdVal;
                        String actionType = rs.getString("action_type");
                        String actionDetails = rs.getString("action_details");
                        Timestamp ts = rs.getTimestamp("action_timestamp");
                        LocalDateTime performedAt = ts != null ? ts.toLocalDateTime() : LocalDateTime.now();

                        logs.add(new AuditLog(logId, loanId, actionType, actionDetails, performedAt));
                    }
                    return logs;
                }
            }
        } catch (SQLException e1) {
            // Fallback 1: schema with performed_at
            String sql2 = "SELECT log_id, loan_id, action_type, action_details, performed_at " +
                    "FROM audit_log ORDER BY performed_at DESC, log_id DESC LIMIT ?";
            try {
                Connection conn = DBConnection.getInstance().getConnection();
                try (PreparedStatement stmt = conn.prepareStatement(sql2)) {
                    stmt.setInt(1, max);
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            int logId = rs.getInt("log_id");
                            int loanIdVal = rs.getInt("loan_id");
                            Integer loanId = rs.wasNull() ? null : loanIdVal;
                            String actionType = rs.getString("action_type");
                            String actionDetails = rs.getString("action_details");
                            Timestamp ts = rs.getTimestamp("performed_at");
                            LocalDateTime performedAt = ts != null ? ts.toLocalDateTime() : LocalDateTime.now();

                            logs.add(new AuditLog(logId, loanId, actionType, actionDetails, performedAt));
                        }
                        return logs;
                    }
                }
            } catch (SQLException e2) {
                // Fallback 2: schema with audit_id / action_time
                String sql3 = "SELECT audit_id, entity_id, action_name, action_details, action_time " +
                        "FROM audit_log ORDER BY action_time DESC, audit_id DESC LIMIT ?";
                try {
                    Connection conn = DBConnection.getInstance().getConnection();
                    try (PreparedStatement stmt = conn.prepareStatement(sql3)) {
                        stmt.setInt(1, max);
                        try (ResultSet rs = stmt.executeQuery()) {
                            while (rs.next()) {
                                int logId = rs.getInt("audit_id");
                                int entityIdVal = rs.getInt("entity_id");
                                Integer loanId = rs.wasNull() ? null : entityIdVal;
                                String actionType = rs.getString("action_name");
                                String actionDetails = rs.getString("action_details");
                                Timestamp ts = rs.getTimestamp("action_time");
                                LocalDateTime performedAt = ts != null ? ts.toLocalDateTime() : LocalDateTime.now();

                                logs.add(new AuditLog(logId, loanId, actionType, actionDetails, performedAt));
                            }
                        }
                    }
                } catch (SQLException ex) {
                    System.err.println("[AuditLogDAO] Error in findRecent: " + ex.getMessage());
                }
            }
        }
        return logs;
    }

    public boolean log(Integer loanId, String actionType, String details) {
        // Preferred schema: loan_id, action_type, action_details (timestamp auto-populates via default)
        String sql = "INSERT INTO audit_log (loan_id, action_type, action_details) VALUES (?, ?, ?)";

        try {
            Connection conn = DBConnection.getInstance().getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                if (loanId != null) {
                    stmt.setInt(1, loanId);
                } else {
                    stmt.setNull(1, java.sql.Types.INTEGER);
                }
                stmt.setString(2, actionType);
                stmt.setString(3, details);

                return stmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            // Fallback for alternative table definition
            String fallbackSql = "INSERT INTO audit_log (entity_name, entity_id, action_name, action_details, action_time) VALUES ('LOAN', ?, ?, ?, ?)";
            try {
                Connection conn = DBConnection.getInstance().getConnection();
                try (PreparedStatement stmt = conn.prepareStatement(fallbackSql)) {
                    stmt.setInt(1, loanId != null ? loanId : 0);
                    stmt.setString(2, actionType != null ? (actionType.length() > 30 ? actionType.substring(0, 30) : actionType) : "LOG");
                    stmt.setString(3, details != null ? (details.length() > 500 ? details.substring(0, 500) : details) : "");
                    stmt.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
                    return stmt.executeUpdate() > 0;
                }
            } catch (SQLException ex) {
                System.err.println("[AuditLogDAO] Error logging event: " + ex.getMessage());
            }
        }
        return false;
    }
}
