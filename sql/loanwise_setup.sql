-- ========================================================
-- LoanWise — Complete Database Schema & Seed Data
-- ========================================================

DROP DATABASE IF EXISTS loanwise;
CREATE DATABASE loanwise;
USE loanwise;

-- 1. USER ACCOUNT TABLE
CREATE TABLE user_account (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(120) NOT NULL UNIQUE,
    monthly_income DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. LOAN TABLE
CREATE TABLE loan (
    loan_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    loan_name VARCHAR(100) NOT NULL,
    lender_name VARCHAR(100) NOT NULL,
    loan_type ENUM('HOME', 'AUTO', 'PERSONAL', 'EDUCATION') NOT NULL,
    principal_amount DECIMAL(12,2) NOT NULL,
    outstanding_balance DECIMAL(12,2) NOT NULL,
    annual_interest_rate DECIMAL(5,2) NOT NULL,
    annual_rate_type ENUM('FIXED', 'FLOATING') DEFAULT 'FIXED',
    tenure_months INT NOT NULL,
    start_date DATE NOT NULL,
    due_day INT DEFAULT 5,
    loan_status ENUM('ACTIVE', 'CLOSED') DEFAULT 'ACTIVE',
    FOREIGN KEY (user_id) REFERENCES user_account(user_id) ON DELETE CASCADE
);

-- 3. REPAYMENT (EMI) TABLE
CREATE TABLE repayment (
    repayment_id INT AUTO_INCREMENT PRIMARY KEY,
    loan_id INT NOT NULL,
    payment_date DATE NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    principal_paid DECIMAL(12,2) NOT NULL,
    interest_paid DECIMAL(12,2) NOT NULL,
    payment_method VARCHAR(50) DEFAULT 'AUTO_DEBIT',
    reference_code VARCHAR(50),
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (loan_id) REFERENCES loan(loan_id) ON DELETE CASCADE
);

-- 4. PREPAYMENT TABLE
CREATE TABLE prepayment (
    prepayment_id INT AUTO_INCREMENT PRIMARY KEY,
    loan_id INT NOT NULL,
    prepayment_date DATE NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    strategy VARCHAR(50) DEFAULT 'AVALANCHE',
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (loan_id) REFERENCES loan(loan_id) ON DELETE CASCADE
);

-- 5. AUDIT LOG TABLE
CREATE TABLE audit_log (
    log_id INT AUTO_INCREMENT PRIMARY KEY,
    loan_id INT,
    action_type VARCHAR(50) NOT NULL,
    action_details TEXT NOT NULL,
    action_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ========================================================
-- TRIGGERS (Automated Audit Logging & Balance Updates)
-- ========================================================

DELIMITER $$

-- Trigger: Log interest rate changes
CREATE TRIGGER trg_loan_rate_update
AFTER UPDATE ON loan
FOR EACH ROW
BEGIN
    IF OLD.annual_interest_rate != NEW.annual_interest_rate THEN
        INSERT INTO audit_log (loan_id, action_type, action_details)
        VALUES (
            NEW.loan_id, 
            'UPDATE', 
            CONCAT('Loan ', NEW.loan_id, ': rate changed from ', OLD.annual_interest_rate, '% to ', NEW.annual_interest_rate, '%')
        );
    END IF;
END$$

-- Trigger: Automatically reduce outstanding balance and log on prepayment
CREATE TRIGGER trg_prepayment_audit
AFTER INSERT ON prepayment
FOR EACH ROW
BEGIN
    UPDATE loan 
    SET outstanding_balance = GREATEST(0, outstanding_balance - NEW.amount)
    WHERE loan_id = NEW.loan_id;

    INSERT INTO audit_log (loan_id, action_type, action_details)
    VALUES (
        NEW.loan_id, 
        'INSERT', 
        CONCAT('Prepayment amount=', NEW.amount, ', strategy=', NEW.strategy)
    );
END$$

-- Trigger: Automatically reduce outstanding balance and log on regular repayment
CREATE TRIGGER trg_repayment_audit
AFTER INSERT ON repayment
FOR EACH ROW
BEGIN
    UPDATE loan 
    SET outstanding_balance = GREATEST(0, outstanding_balance - NEW.principal_paid)
    WHERE loan_id = NEW.loan_id;

    INSERT INTO audit_log (loan_id, action_type, action_details)
    VALUES (
        NEW.loan_id, 
        'INSERT', 
        CONCAT('Repayment amount=', NEW.amount, ', principal=', NEW.principal_paid)
    );
END$$

DELIMITER ;

-- ========================================================
-- SEED DATA
-- Default password for all seed accounts is: Password@123
-- Verified jBCrypt Hash: $2a$10$UDqS7E/gQ6IhvG12E/pZ6uaRguGtlEirVpR7VusQaoH50gd7KLQIS
-- ========================================================

INSERT INTO user_account (user_id, full_name, email, monthly_income, password_hash) VALUES
(1, 'Aarav Sharma', 'aarav@example.com', 85000.00, '$2a$10$UDqS7E/gQ6IhvG12E/pZ6uaRguGtlEirVpR7VusQaoH50gd7KLQIS'),
(2, 'Diya Patel', 'diya@example.com', 65000.00, '$2a$10$UDqS7E/gQ6IhvG12E/pZ6uaRguGtlEirVpR7VusQaoH50gd7KLQIS'),
(3, 'Kabir Mehta', 'kabir@example.com', 120000.00, '$2a$10$UDqS7E/gQ6IhvG12E/pZ6uaRguGtlEirVpR7VusQaoH50gd7KLQIS');

-- Aarav's Loans (User ID 1)
INSERT INTO loan (loan_id, user_id, loan_name, lender_name, loan_type, principal_amount, outstanding_balance, annual_interest_rate, annual_rate_type, tenure_months, start_date, due_day, loan_status) VALUES
(1, 1, 'Home Loan', 'HDFC Bank', 'HOME', 3500000.00, 3107400.00, 8.45, 'FLOATING', 240, '2024-01-05', 5, 'ACTIVE'),
(2, 1, 'Car Loan', 'ICICI Bank', 'AUTO', 850000.00, 486300.00, 9.25, 'FIXED', 60, '2024-08-05', 5, 'ACTIVE'),
(3, 1, 'Education Loan', 'SBI', 'EDUCATION', 600000.00, 405500.00, 7.90, 'FLOATING', 120, '2023-07-05', 5, 'ACTIVE');

-- Diya's Loans (User ID 2)
INSERT INTO loan (loan_id, user_id, loan_name, lender_name, loan_type, principal_amount, outstanding_balance, annual_interest_rate, annual_rate_type, tenure_months, start_date, due_day, loan_status) VALUES
(4, 2, 'Two-Wheeler Loan', 'Bajaj Finance', 'AUTO', 120000.00, 68000.00, 11.50, 'FIXED', 24, '2024-03-10', 10, 'ACTIVE'),
(5, 2, 'Personal Loan', 'Axis Bank', 'PERSONAL', 250000.00, 185000.00, 13.00, 'FIXED', 36, '2024-05-15', 15, 'ACTIVE');

-- Kabir's Loans (User ID 3)
INSERT INTO loan (loan_id, user_id, loan_name, lender_name, loan_type, principal_amount, outstanding_balance, annual_interest_rate, annual_rate_type, tenure_months, start_date, due_day, loan_status) VALUES
(6, 3, 'Apartment Mortgage', 'Kotak Mahindra', 'HOME', 5500000.00, 4920000.00, 8.65, 'FLOATING', 240, '2023-11-01', 1, 'ACTIVE');