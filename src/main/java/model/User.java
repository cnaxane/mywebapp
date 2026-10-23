package model;

public class User {
    private int userId;
    private String fullName;
    private String email;
    private double monthlyIncome;
    private String passwordHash;

    public User() {
    }

    public User(int userId, String fullName, String email, double monthlyIncome) {
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.monthlyIncome = monthlyIncome;
    }

    public User(int userId, String fullName, String email, double monthlyIncome, String passwordHash) {
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.monthlyIncome = monthlyIncome;
        this.passwordHash = passwordHash;
    }

    public User(String fullName, String email, double monthlyIncome) {
        this.fullName = fullName;
        this.email = email;
        this.monthlyIncome = monthlyIncome;
    }

    public User(String fullName, String email, double monthlyIncome, String passwordHash) {
        this.fullName = fullName;
        this.email = email;
        this.monthlyIncome = monthlyIncome;
        this.passwordHash = passwordHash;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public double getMonthlyIncome() {
        return monthlyIncome;
    }

    public void setMonthlyIncome(double monthlyIncome) {
        this.monthlyIncome = monthlyIncome;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    @Override
    public String toString() {
        return fullName + " (" + email + ")";
    }
}
