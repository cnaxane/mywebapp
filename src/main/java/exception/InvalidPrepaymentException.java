package exception;

public class InvalidPrepaymentException extends Exception {
    private double attemptedAmount;
    private double currentBalance;

    public InvalidPrepaymentException(String message) {
        super(message);
    }

    public InvalidPrepaymentException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidPrepaymentException(String message, double attemptedAmount, double currentBalance) {
        super(message + " [Attempted: ₹" + String.format("%.2f", attemptedAmount) +
              ", Current Balance: ₹" + String.format("%.2f", currentBalance) + "]");
        this.attemptedAmount = attemptedAmount;
        this.currentBalance = currentBalance;
    }

    public double getAttemptedAmount() {
        return attemptedAmount;
    }

    public double getCurrentBalance() {
        return currentBalance;
    }
}
