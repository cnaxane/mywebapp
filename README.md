# LoanWise — Loan Prepayment and Payoff Optimizer

> **"Instead of just tracking loans, it answers: Which loan should my extra ₹5,000 go to, and how many months and how much interest does that save?"**

**LoanWise** is a personal‑finance desktop optimization tool built using **Java (Swing GUI)** and **JDBC** connected to a **MySQL** database. Unlike standard trackers that merely record existing debt, LoanWise evaluates your loan portfolio across lenders, models reducing‑balance amortization schedules, and calculates optimal acceleration strategies (**Debt Avalanche** vs. **Debt Snowball**) using asynchronous background multithreading.

---

## Key Features

- **Profile Dashboard:** Switch across user accounts to view total active debt, active loan counts, and total monthly EMI commitments.
- **Loan Management (CRUD):** Add, view, and delete loans across categories (Home, Auto, Personal, Education) with full validation.
- **Prepayment Optimizer:** Interactive slider (₹0 – ₹50,000+) to test extra monthly payments and instantly see months saved and net interest eliminated.
- **Debt Avalanche vs. Snowball Strategies**
  - **Avalanche:** Prioritises loans with the highest interest rates to minimise total lifetime interest.
  - **Snowball:** Prioritises loans with the smallest outstanding balances to eliminate individual loan payments faster.
- **Amortization Schedule & CSV Export:** View detailed installment breakdowns (principal, interest, opening/closing balances) and export schedules directly to CSV files.
- **Multithreaded Simulation Engine:** Runs heavy iterative payment projections in a dedicated background `Runnable` thread so the Swing UI never freezes.
- **Database Triggers & Audit Ledger:** Automatically updates loan balances and logs every rate change, repayment, and prepayment directly into an audit table.

---

## Lab‑Manual Java Concepts Mapping

| Java Concept | Project Implementation File | Assignment |
|--------------|-----------------------------|------------|
| **Static Methods** | `service/EMICalculator.java` | A‑3 |
| **Inner / Anonymous Classes** | Swing `ActionListener` implementations across panels | A‑6 |
| **Inheritance & Interfaces** | `strategy/AvalancheStrategy.java`, `strategy/SnowballStrategy.java` | A‑7 |
| **Abstract Classes** | Scaffolding for base models / reports | A‑10 |
| **Interfaces** | `strategy/PayoffStrategy.java` | A‑11 |
| **File Handling** | CSV Schedule Export in `ui/SchedulePanel.java` | A‑13 |
| **Exception Handling (`try‑catch‑finally`)** | Resource management across all DAO classes and DB connection | A‑14, A‑15 |
| **Custom / User‑Defined Exception** | `exception/InvalidPrepaymentException.java` | A‑16 |
| **Swing GUI Architecture** | `ui/MainFrame.java` (using `JTabbedPane`, `JPanel`, `JTable`) | A‑17 |
| **Event Handling** | `ActionListener` bindings across buttons and sliders | A‑18 |
| **JDBC `SELECT` Queries** | `dao/LoanDAO.java`, `dao/UserDAO.java` | A‑19, A‑23 |
| **JDBC `INSERT` / `UPDATE` / `DELETE`** | `dao/RepaymentDAO.java`, `dao/PrepaymentDAO.java` | A‑20 |
| **Prepared Statements** | Parameterised queries in all DAO files to prevent SQL injection | A‑22 |
| **Swing + JDBC Integration** | `ui/LoanPanel.java`, `ui/OptimizerPanel.java` | A‑24 |
| **Multithreading (`Runnable`)** | `service/SimulationThread.java` for async calculation | A‑25, A‑26 |

---

## Project Structure

```text
loanwise/
├── pom.xml
├── README.md
├── .gitignore
├── sql/
│   └── loanwise_setup.sql
└── src/
    └── main/
        └── java/
            ├── db/
            │   └── DBConnection.java
            ├── model/
            │   ├── User.java
            │   ├── Loan.java
            │   ├── Repayment.java
            │   └── Prepayment.java
            ├── exception/
            │   └── InvalidPrepaymentException.java
            ├── strategy/
            │   ├── PayoffStrategy.java
            │   ├── AvalancheStrategy.java
            │   └── SnowballStrategy.java
            ├── dao/
            │   ├── UserDAO.java
            │   ├── LoanDAO.java
            │   ├── RepaymentDAO.java
            │   └── PrepaymentDAO.java
            ├── service/
            │   ├── EMICalculator.java
            │   ├── LoanService.java
            │   └── SimulationThread.java
            └── ui/
                ├── MainFrame.java
                ├── DashboardPanel.java
                ├── LoanPanel.java
                ├── OptimizerPanel.java
                ├── SchedulePanel.java
                └── AuditPanel.java
```

---

## Getting Started

1. **Setup Database:**
   Import and run `sql/loanwise_setup.sql` in MySQL Workbench or via CLI:
   ```bash
   mysql -u root -p < sql/loanwise_setup.sql
   ```

2. **Configure Database Credentials:**
   Copy the example properties file and update with your local MySQL password:
   ```bash
   cp db.properties.example db.properties
   ```
   *(Note: `db.properties` is git-ignored to keep your credentials secure).*

3. **Run the Application:**
   ```bash
   mvn clean compile
   mvn exec:java
   ```