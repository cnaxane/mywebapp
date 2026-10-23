package ui;

import model.Loan;
import service.LoanService;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.Locale;

/**
 * Modal dialog for editing an existing loan account's parameters.
 */
public class EditLoanDialog extends JDialog {

    private final Loan loan;
    private final LoanService loanService;
    private final Runnable onSaveSuccess;

    private JTextField nameField;
    private JTextField lenderField;
    private JTextField rateField;
    private JTextField tenureField;
    private JTextField balanceField;
    private JComboBox<String> rateTypeBox;

    public EditLoanDialog(Window owner, Loan loan, LoanService loanService, Runnable onSaveSuccess) {
        super(owner, "Edit Loan Account — " + (loan != null ? loan.getLoanName() : ""), ModalityType.APPLICATION_MODAL);
        this.loan = loan;
        this.loanService = loanService;
        this.onSaveSuccess = onSaveSuccess;

        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(0, 0));
        setSize(520, 560);
        setResizable(false);
        setLocationRelativeTo(getOwner());

        // Header Panel
        JPanel headerPanel = new JPanel(new BorderLayout(10, 6));
        headerPanel.setBackground(UITheme.BG_DARK);
        headerPanel.setBorder(new EmptyBorder(18, 24, 18, 24));

        JLabel titleLabel = new JLabel("Edit Loan Account");
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        titleLabel.setForeground(Color.WHITE);

        String contextSub = (loan != null)
                ? String.format("Account ID #%d  •  Category: %s  •  Principal: %s",
                loan.getLoanId(), loan.getLoanType(), UITheme.formatCurrency(loan.getPrincipalAmount()))
                : "Modify terms, rate, and active liability balance";
        JLabel subtitleLabel = new JLabel(contextSub);
        subtitleLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        subtitleLabel.setForeground(new Color(148, 163, 184));

        headerPanel.add(titleLabel, BorderLayout.NORTH);
        headerPanel.add(subtitleLabel, BorderLayout.SOUTH);

        // Form Card Panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(UITheme.CARD_BG);
        formPanel.setBorder(new EmptyBorder(22, 24, 20, 24));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 6, 6, 6);

        // Initialize Fields
        nameField = new JTextField(loan != null ? loan.getLoanName() : "", 20);
        lenderField = new JTextField(loan != null ? loan.getLenderName() : "", 20);
        rateField = new JTextField(loan != null ? String.format(Locale.US, "%.2f", loan.getAnnualInterestRate()) : "", 20);
        tenureField = new JTextField(loan != null ? String.valueOf(loan.getTenureMonths()) : "", 20);
        balanceField = new JTextField(loan != null ? String.format(Locale.US, "%.2f", loan.getOutstandingBalance()) : "", 20);

        String[] rateTypes = {"FIXED", "FLOATING"};
        rateTypeBox = new JComboBox<>(rateTypes);
        rateTypeBox.setBackground(Color.WHITE);
        if (loan != null && "FLOATING".equalsIgnoreCase(loan.getAnnualRateType())) {
            rateTypeBox.setSelectedItem("FLOATING");
        } else {
            rateTypeBox.setSelectedItem("FIXED");
        }

        // Set standard heights
        Dimension fieldSize = new Dimension(220, 32);
        nameField.setPreferredSize(fieldSize);
        lenderField.setPreferredSize(fieldSize);
        rateField.setPreferredSize(fieldSize);
        tenureField.setPreferredSize(fieldSize);
        balanceField.setPreferredSize(fieldSize);
        rateTypeBox.setPreferredSize(fieldSize);

        int row = 0;
        addFormRow(formPanel, gbc, row++, "Loan Name *:", nameField);
        addFormRow(formPanel, gbc, row++, "Lender Name *:", lenderField);
        addFormRow(formPanel, gbc, row++, "Annual Interest Rate (%) *:", rateField);
        addFormRow(formPanel, gbc, row++, "Remaining Tenure (Months) *:", tenureField);
        addFormRow(formPanel, gbc, row++, "Outstanding Balance (₹) *:", balanceField);
        addFormRow(formPanel, gbc, row++, "Rate Type (FIXED / FLOATING):", rateTypeBox);

        // Button Toolbar
        JPanel bottomBar = new JPanel(new BorderLayout());
        bottomBar.setBackground(new Color(248, 250, 252));
        bottomBar.setBorder(new CompoundBorder(
                new LineBorder(UITheme.BORDER_COLOR, 1, false),
                new EmptyBorder(12, 24, 14, 24)
        ));

        JLabel hintLabel = new JLabel("Updating interest rate automatically logs into the database audit ledger.");
        hintLabel.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 11));
        hintLabel.setForeground(UITheme.TEXT_MUTED);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setOpaque(false);

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.setFont(UITheme.FONT_BOLD);
        cancelBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        cancelBtn.addActionListener(e -> dispose());

        JButton saveBtn = UITheme.createStyledButton("Save Changes", UITheme.ACCENT_BLUE, Color.WHITE);
        saveBtn.addActionListener(e -> handleSaveLoan());

        btnPanel.add(cancelBtn);
        btnPanel.add(saveBtn);

        bottomBar.add(hintLabel, BorderLayout.WEST);
        bottomBar.add(btnPanel, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);
        add(formPanel, BorderLayout.CENTER);
        add(bottomBar, BorderLayout.SOUTH);
    }

    private void addFormRow(JPanel panel, GridBagConstraints gbc, int row, String labelText, JComponent input) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.35;
        JLabel label = new JLabel(labelText);
        label.setFont(UITheme.FONT_BOLD);
        label.setForeground(UITheme.TEXT_MAIN);
        panel.add(label, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.65;
        input.setFont(UITheme.FONT_REGULAR);
        panel.add(input, gbc);
    }

    private void handleSaveLoan() {
        if (loan == null) {
            dispose();
            return;
        }

        String name = nameField.getText().trim();
        String lender = lenderField.getText().trim();

        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Loan Name cannot be empty.", "Validation Error", JOptionPane.ERROR_MESSAGE);
            nameField.requestFocus();
            return;
        }

        if (lender.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Lender Name cannot be empty.", "Validation Error", JOptionPane.ERROR_MESSAGE);
            lenderField.requestFocus();
            return;
        }

        double rate;
        try {
            rate = Double.parseDouble(rateField.getText().trim());
            if (rate <= 0 || rate > 100) {
                JOptionPane.showMessageDialog(this, "Annual Interest Rate must be between 0.1% and 100%.", "Validation Error", JOptionPane.ERROR_MESSAGE);
                rateField.requestFocus();
                return;
            }
        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(this, "Please enter a valid numeric interest rate.", "Validation Error", JOptionPane.ERROR_MESSAGE);
            rateField.requestFocus();
            return;
        }

        int tenure;
        try {
            tenure = Integer.parseInt(tenureField.getText().trim());
            if (tenure <= 0 || tenure > 600) {
                JOptionPane.showMessageDialog(this, "Remaining Tenure must be between 1 and 600 months.", "Validation Error", JOptionPane.ERROR_MESSAGE);
                tenureField.requestFocus();
                return;
            }
        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(this, "Please enter a valid integer for remaining tenure.", "Validation Error", JOptionPane.ERROR_MESSAGE);
            tenureField.requestFocus();
            return;
        }

        double balance;
        try {
            balance = Double.parseDouble(balanceField.getText().trim());
            if (balance < 0) {
                JOptionPane.showMessageDialog(this, "Outstanding Balance cannot be negative.", "Validation Error", JOptionPane.ERROR_MESSAGE);
                balanceField.requestFocus();
                return;
            }
        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(this, "Please enter a valid numeric outstanding balance.", "Validation Error", JOptionPane.ERROR_MESSAGE);
            balanceField.requestFocus();
            return;
        }

        String rateType = (String) rateTypeBox.getSelectedItem();
        if (rateType == null || (!rateType.equals("FIXED") && !rateType.equals("FLOATING"))) {
            rateType = "FIXED";
        }

        double originalRate = loan.getAnnualInterestRate();
        boolean rateChanged = Double.compare(originalRate, rate) != 0;

        // Apply edits to Loan entity
        loan.setLoanName(name);
        loan.setLenderName(lender);
        loan.setAnnualInterestRate(rate);
        loan.setTenureMonths(tenure);
        loan.setOutstandingBalance(balance);
        loan.setAnnualRateType(rateType);

        if (balance <= 0) {
            loan.setLoanStatus("PAID_OFF");
        } else if ("PAID_OFF".equalsIgnoreCase(loan.getLoanStatus()) && balance > 0) {
            loan.setLoanStatus("ACTIVE");
        }

        boolean success = loanService.updateLoan(loan);
        if (success) {
            String rateNotice = rateChanged
                    ? String.format("\nRate adjustment (%.2f%% → %.2f%%) recorded in Audit Ledger.", originalRate, rate)
                    : "";

            JOptionPane.showMessageDialog(
                    this,
                    "Loan account '" + loan.getLoanName() + "' was successfully updated!" + rateNotice,
                    "Loan Updated",
                    JOptionPane.INFORMATION_MESSAGE
            );

            dispose();

            if (onSaveSuccess != null) {
                onSaveSuccess.run();
            }
        } else {
            JOptionPane.showMessageDialog(
                    this,
                    "Failed to save changes to the database. Please verify connection status.",
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }
}
