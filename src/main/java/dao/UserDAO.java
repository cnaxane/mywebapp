package dao;

import db.DBConnection;
import model.User;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    public UserDAO() {
        ensurePasswordColumnExists();
    }

    /**
     * Ensures password_hash column exists in user_account and demo users have a valid hash.
     */
    public synchronized void ensurePasswordColumnExists() {
        try {
            Connection conn = DBConnection.getInstance().getConnection();
            DatabaseMetaData meta = conn.getMetaData();
            boolean hasCol = false;
            try (ResultSet rs = meta.getColumns("loanwise", null, "user_account", "password_hash")) {
                if (rs.next()) {
                    hasCol = true;
                }
            }

            if (!hasCol) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("ALTER TABLE user_account ADD COLUMN password_hash VARCHAR(255) NULL");
                    System.out.println("[UserDAO] Added password_hash column to user_account table.");
                }
            }

            // Ensure demo users have default password hash
            String demoHash = BCrypt.hashpw("Password@123", BCrypt.gensalt(10));
            String updateNulls = "UPDATE user_account SET password_hash = ? WHERE password_hash IS NULL OR password_hash = ''";
            try (PreparedStatement stmt = conn.prepareStatement(updateNulls)) {
                stmt.setString(1, demoHash);
                int updated = stmt.executeUpdate();
                if (updated > 0) {
                    System.out.println("[UserDAO] Updated default demo passwords for " + updated + " accounts.");
                }
            }

            String updateDemo = "UPDATE user_account SET password_hash = ? WHERE LOWER(email) IN ('aarav@example.com', 'diya@example.com', 'kabir@example.com') AND (password_hash IS NULL OR password_hash NOT LIKE '$2%')";
            try (PreparedStatement stmt = conn.prepareStatement(updateDemo)) {
                stmt.setString(1, demoHash);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] Warning ensuring password schema: " + e.getMessage());
        }
    }

    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT user_id, full_name, email, monthly_income, password_hash FROM user_account ORDER BY user_id ASC";

        try {
            Connection conn = DBConnection.getInstance().getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    User user = new User(
                            rs.getInt("user_id"),
                            rs.getString("full_name"),
                            rs.getString("email"),
                            rs.getDouble("monthly_income"),
                            rs.getString("password_hash")
                    );
                    users.add(user);
                }
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] Error in findAll: " + e.getMessage());
        }
        return users;
    }

    public User findById(int userId) {
        String sql = "SELECT user_id, full_name, email, monthly_income, password_hash FROM user_account WHERE user_id = ?";

        try {
            Connection conn = DBConnection.getInstance().getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return new User(
                                rs.getInt("user_id"),
                                rs.getString("full_name"),
                                rs.getString("email"),
                                rs.getDouble("monthly_income"),
                                rs.getString("password_hash")
                        );
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] Error in findById: " + e.getMessage());
        }
        return null;
    }

    public User findByEmail(String email) {
        if (email == null) return null;
        String sql = "SELECT user_id, full_name, email, monthly_income, password_hash FROM user_account WHERE LOWER(email) = LOWER(?)";

        try {
            Connection conn = DBConnection.getInstance().getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, email.trim());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return new User(
                                rs.getInt("user_id"),
                                rs.getString("full_name"),
                                rs.getString("email"),
                                rs.getDouble("monthly_income"),
                                rs.getString("password_hash")
                        );
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] Error in findByEmail: " + e.getMessage());
        }
        return null;
    }

    /**
     * Registers a new user with BCrypt password hashing.
     */
    public User registerUser(String fullName, String email, double monthlyIncome, String plainPassword) throws IllegalArgumentException {
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new IllegalArgumentException("Full Name cannot be empty.");
        }
        if (email == null || email.trim().isEmpty() || !email.contains("@")) {
            throw new IllegalArgumentException("Valid Email is required.");
        }
        if (plainPassword == null || plainPassword.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters long.");
        }

        User existing = findByEmail(email);
        if (existing != null) {
            throw new IllegalArgumentException("An account with email '" + email + "' already exists.");
        }

        String hash = BCrypt.hashpw(plainPassword, BCrypt.gensalt(10));
        String sql = "INSERT INTO user_account (full_name, email, monthly_income, password_hash) VALUES (?, ?, ?, ?)";

        try {
            Connection conn = DBConnection.getInstance().getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, fullName.trim());
                stmt.setString(2, email.trim().toLowerCase());
                stmt.setDouble(3, Math.max(0.0, monthlyIncome));
                stmt.setString(4, hash);

                int affected = stmt.executeUpdate();
                if (affected > 0) {
                    try (ResultSet keys = stmt.getGeneratedKeys()) {
                        if (keys.next()) {
                            int newId = keys.getInt(1);
                            return new User(newId, fullName.trim(), email.trim().toLowerCase(), monthlyIncome, hash);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] Error registering user: " + e.getMessage());
            throw new IllegalArgumentException("Database error registering user: " + e.getMessage());
        }
        return null;
    }

    /**
     * Authenticates a user by email and plain password using BCrypt.
     */
    public User authenticateUser(String email, String plainPassword) {
        if (email == null || plainPassword == null) {
            return null;
        }

        User user = findByEmail(email);
        if (user == null || user.getPasswordHash() == null) {
            return null;
        }

        try {
            if (BCrypt.checkpw(plainPassword, user.getPasswordHash())) {
                return user;
            }
        } catch (Exception e) {
            System.err.println("[UserDAO] BCrypt check failed: " + e.getMessage());
        }
        return null;
    }

    public boolean insert(User user) {
        String sql = "INSERT INTO user_account (full_name, email, monthly_income, password_hash) VALUES (?, ?, ?, ?)";

        try {
            Connection conn = DBConnection.getInstance().getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, user.getFullName());
                stmt.setString(2, user.getEmail());
                stmt.setDouble(3, user.getMonthlyIncome());
                stmt.setString(4, user.getPasswordHash());

                int affected = stmt.executeUpdate();
                if (affected > 0) {
                    try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            user.setUserId(generatedKeys.getInt(1));
                        }
                    }
                    return true;
                }
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] Error in insert: " + e.getMessage());
        }
        return false;
    }
}
