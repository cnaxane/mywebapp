package db;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class DBConnection {

    private static String HOST = "localhost";
    private static int PORT = 3306;
    private static String DATABASE = "loanwise";
    private static String USERNAME = "root";
    private static String PASSWORD = "";

    private static String SERVER_URL;
    private static String DB_URL;

    static {
        loadConfig();
    }

    private static void loadConfig() {
        Properties props = new Properties();

        // 1. Try reading from db.properties in the working directory
        File propFile = new File("db.properties");
        if (propFile.exists()) {
            try (FileInputStream fis = new FileInputStream(propFile)) {
                props.load(fis);
            } catch (Exception e) {
                System.err.println("[DBConnection] Could not read db.properties: " + e.getMessage());
            }
        } else {
            // 2. Try classpath
            try (InputStream is = DBConnection.class.getClassLoader().getResourceAsStream("db.properties")) {
                if (is != null) {
                    props.load(is);
                }
            } catch (Exception ignored) {}
        }

        HOST = props.getProperty("db.host", System.getenv().getOrDefault("DB_HOST", HOST));
        String portStr = props.getProperty("db.port", System.getenv().getOrDefault("DB_PORT", String.valueOf(PORT)));
        try {
            PORT = Integer.parseInt(portStr);
        } catch (NumberFormatException ignored) {}

        DATABASE = props.getProperty("db.name", System.getenv().getOrDefault("DB_NAME", DATABASE));
        USERNAME = props.getProperty("db.username", System.getenv().getOrDefault("DB_USER", USERNAME));
        PASSWORD = props.getProperty("db.password", System.getenv().getOrDefault("DB_PASSWORD", PASSWORD));

        SERVER_URL = "jdbc:mysql://" + HOST + ":" + PORT +
                "/?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        DB_URL = "jdbc:mysql://" + HOST + ":" + PORT + "/" + DATABASE +
                "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    }

    private static volatile DBConnection instance;
    private Connection connection;

    private DBConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("[DBConnection] MySQL JDBC Driver not found in classpath: " + e.getMessage());
        }
    }

    public static DBConnection getInstance() {
        if (instance == null) {
            synchronized (DBConnection.class) {
                if (instance == null) {
                    instance = new DBConnection();
                }
            }
        }
        return instance;
    }

    /**
     * Obtains the shared database connection, reconnecting if closed or invalid.
     *
     * @return active java.sql.Connection
     * @throws SQLException if a database access error occurs
     */
    public synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed() || !connection.isValid(2)) {
            try {
                connection = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD);
            } catch (SQLException e) {
                // If database might not exist yet, attempt creating it first
                if (e.getErrorCode() == 1049 || (e.getMessage() != null && e.getMessage().contains("Unknown database"))) {
                    ensureDatabaseExists();
                    connection = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD);
                } else {
                    if (e.getErrorCode() == 1045 || (e.getMessage() != null && e.getMessage().contains("Access denied"))) {
                        System.err.println("[DBConnection] Database authentication failed. Check credentials in 'db.properties' (see db.properties.example).");
                    }
                    throw e;
                }
            }
        }
        return connection;
    }

    /**
     * Checks if connection is currently active.
     */
    public synchronized boolean isConnected() {
        try {
            return connection != null && !connection.isClosed() && connection.isValid(2);
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Ensures that the 'loanwise' database exists on the MySQL server.
     */
    public synchronized void ensureDatabaseExists() {
        try (Connection rootConn = DriverManager.getConnection(SERVER_URL, USERNAME, PASSWORD);
             Statement stmt = rootConn.createStatement()) {
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + DATABASE);
            System.out.println("[DBConnection] Database '" + DATABASE + "' verified or created successfully.");
        } catch (SQLException e) {
            System.err.println("[DBConnection] Could not verify/create database: " + e.getMessage());
        }
    }

    /**
     * Safely closes the shared connection.
     */
    public synchronized void closeConnection() {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException e) {
                System.err.println("[DBConnection] Error closing connection: " + e.getMessage());
            } finally {
                connection = null;
            }
        }
    }
}
