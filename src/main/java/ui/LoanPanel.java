package ui;

import exception.InvalidPrepaymentException;
import model.Loan;
import model.Prepayment;
import model.Repayment;
import model.User;
import service.EMICalculator;
import service.LoanService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class LoanPanel extends JPanel implements UserChangeListener {

    private final LoanService loanService;
    private final DashboardPanel.UserSelectionManager selectionManager;

    private JTable loansTable;
    private DefaultTableModel tableModel;
    private List<Loan> displayedLoans = new ArrayList<>();
    private JLabel headerLabel;

    private JButton editLoanBtn;
    private JButton deleteLoanBtn;
    private JButton prepayBtn;

    public LoanPanel(LoanService loanService, DashboardPanel.UserSelectionManager selectionManager) {
        this.loanService = loanService;
        this.selectionManager = selectionManager;

        setLayout(new BorderLayout(16, 16));
        setBackground(UITheme.BG_LIGHT);
        setBorder(new EmptyBorder(20, 24, 20, 24));

        initComponents();
        selectionManager.registerListener(this);
    }

    private void initComponents() {
        // Top Toolbar
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);

        JPanel titlePanel = new JPanel(new GridLayout(2, 1, 0, 4));
        titlePanel.setOpaque(false);

        headerLabel = new JLabel("Manage Loan Accounts");
        headerLabel.setFont(UITheme.FONT_TITLE);
        headerLabel.setForeground(UITheme.TEXT_MAIN);

        JLabel subtitle = new JLabel("Register new liabilities, edit existing terms, manage balances, and record payments");
        subtitle.setFont(UITheme.FONT_REGULAR);
        subtitle.setForeground(UITheme.TEXT_MUTED);

        titlePanel.add(headerLabel);
        titlePanel.add(subtitle);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);

        JButton addLoanBtn = UITheme.createStyledButton("Add New Loan", UITheme.ACCENT_BLUE, Color.WHITE);
        editLoanBtn = UITheme.createStyledButton("Edit Loan", new Color(15, 23, 42), Color.WHITE);
        deleteLoanBtn = UITheme.createStyledButton("Delete Loan", UITheme.ACCENT_RED, Color.WHITE);
        prepayBtn = UITheme.createStyledButton("Apply Prepayment", UITheme.ACCENT_GREEN, Color.WHITE);
        JButton refreshBtn = UITheme.createStyledButton("Refresh", new Color(100, 116, 139), Color.WHITE);

        // Buttons initially disabled until a table row is selected
        editLoanBtn.setEnabled(false);
        deleteLoanBtn.setEnabled(false);
        prepayBtn.setEnabled(false);

        addLoanBtn.addActionListener(e -> showAddLoanDialog());
        editLoanBtn.addActionListener(e -> showEditLoanDialog());
        deleteLoanBtn.addActionListener(e -> handleDeleteLoan());
        prepayBtn.addActionListener(e -> showPrepaymentDialog());
        refreshBtn.addActionListener(e -> refreshLoans());

        buttonPanel.add(addLoanBtn);
        buttonPanel.add(editLoanBtn);
        buttonPanel.add(deleteLoanBtn);
        buttonPanel.add(prepayBtn);
        buttonPanel.add(refreshBtn);

        topBar.add(titlePanel, BorderLayout.WEST);
        topBar.add(buttonPanel, BorderLayout.EAST);

        // Center Table Card
        JPanel tableCard = new JPanel(new BorderLayout());
        tableCard.setBackground(UITheme.CARD_BG);
        tableCard.setBorder(UITheme.createCardBorder());

        String[] cols = {
                "Loan ID", "Loan Name", "Lender", "Type",
                "Principal (₹)", "Outstanding Balance (₹)",
                "Interest Rate (%)", "Rate Type", "Tenure (Mo)", "Start Date", "Status"
        };

        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        loansTable = new JTable(tableModel);
        loansTable.setRowHeight(32);
        loansTable.setFont(UITheme.FONT_REGULAR);
        loansTable.getTableHeader().setFont(UITheme.FONT_BOLD);
        loansTable.getTableHeader().setBackground(new Color(241, 245, 249));
        loansTable.getTableHeader().setForeground(UITheme.TEXT_MAIN);
        loansTable.setSelectionBackground(new Color(219, 234, 254));
        loansTable.setGridColor(UITheme.BORDER_COLOR);

        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        loansTable.getColumnModel().getColumn(4).setCellRenderer(rightRenderer);
        loansTable.getColumnModel().getColumn(5).setCellRenderer(rightRenderer);
        loansTable.getColumnModel().getColumn(6).setCellRenderer(rightRenderer);
        loansTable.getColumnModel().getColumn(8).setCellRenderer(rightRenderer);

        // Selection Listener: Enable/Disable action buttons based on row selection
        loansTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                boolean hasSelection = loansTable.getSelectedRow() >= 0;
                editLoanBtn.setEnabled(hasSelection);
                deleteLoanBtn.setEnabled(hasSelection);
                prepayBtn.setEnabled(hasSelection);
            }
        });

        // Double-click row listener to edit loan
        loansTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && loansTable.getSelectedRow() >= 0) {
                    showEditLoanDialog();
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                // Ensure right click selects row before popup opens
                if (SwingUtilities.isRightMouseButton(e)) {
                    int row = loansTable.rowAtPoint(e.getPoint());
                    if (row >= 0 && !loansTable.isRowSelected(row)) {
                        loansTable.setRowSelectionInterval(row, row);
                    }
                }
            }
        });

        // Context Menu
        JPopupMenu contextMenu = new JPopupMenu();
        JMenuItem editItem = new JMenuItem("Edit Loan");
        editItem.addActionListener(e -> showEditLoanDialog());

        JMenuItem prepayItem = new JMenuItem("Apply Prepayment");
        prepayItem.addActionListener(e -> showPrepaymentDialog());

        JMenuItem deleteItem = new JMenuItem("Delete Loan");
        deleteItem.addActionListener(e -> handleDeleteLoan());

        contextMenu.add(editItem);
        contextMenu.add(prepayItem);
        contextMenu.addSeparator();
        contextMenu.add(deleteItem);

        loansTable.setComponentPopupMenu(contextMenu);

        JScrollPane scrollPane = new JScrollPane(loansTable);
        scrollPane.setBorder(new LineBorder(UITheme.BORDER_COLOR, 1));

        tableCard.add(scrollPane, BorderLayout.CENTER);

        add(topBar, BorderLayout.NORTH);
        add(tableCard, BorderLayout.CENTER);
    }

    @Override
    public void onUserChanged(User newUser) {
        if (newUser != null) {
            headerLabel.setText("Manage Loans — " + newUser.getFullName());
        }
        refreshLoans();
    }

    public void refreshLoans() {
        User user = selectionManager.getCurrentUser();
        tableModel.setRowCount(0);
        displayedLoans.clear();

        if (editLoanBtn != null) editLoanBtn.setEnabled(false);
        if (deleteLoanBtn != null) deleteLoanBtn.setEnabled(false);
        if (prepayBtn != null) prepayBtn.setEnabled(false);

        if (user == null) {
            return;
        }

        displayedLoans = loanService.getAllLoansByUserId(user.getUserId());
        for (Loan l : displayedLoans) {
            tableModel.addRow(new Object[]{
                    l.getLoanId(),
                    l.getLoanName(),
                    l.getLenderName(),
                    l.getLoanType(),
                    UITheme.formatCurrency(l.getPrincipalAmount()),
                    UITheme.formatCurrency(l.getOutstandingBalance()),
                    String.format("%.2f%%", l.getAnnualInterestRate()),
                    l.getAnnualRateType() != null ? l.getAnnualRateType() : "FIXED",
                    l.getTenureMonths(),
                    l.getStartDate() != null ? l.getStartDate().toString() : "-",
                    l.getLoanStatus()
            });
        }
    }

    private void showEditLoanDialog() {
        int selectedRow = loansTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a loan from the table to edit.", "Select Loan", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int loanId = (int) tableModel.getValueAt(selectedRow, 0);
        Loan loan = loanService.getLoanById(loanId);
        if (loan == null) {
            JOptionPane.showMessageDialog(this, "Loan details not found in database.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        EditLoanDialog dialog = new EditLoanDialog(
                SwingUtilities.getWindowAncestor(this),
                loan,
                loanService,
                () -> {
                    refreshLoans();
                    User currentUser = selectionManager.getCurrentUser();
                    if (currentUser != null) {
                        selectionManager.selectUser(currentUser);
                    }
                }
        );
        dialog.setVisible(true);
    }

    private void showAddLoanDialog() {
        User currentUser = selectionManager.getCurrentUser();
        if (currentUser == null) {
            JOptionPane.showMessageDialog(this, "Please select an active user profile first.", "Notice", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Add New Loan Account", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(500, 560);
        dialog.setLocationRelativeTo(this);

        JPanel formPanel = new JPanel(new GridLayout(9, 2, 10, 12));
        formPanel.setBorder(new EmptyBorder(20, 24, 20, 24));

        JTextField nameField = new JTextField();
        JTextField lenderField = new JTextField();
        JComboBox<String> typeBox = new JComboBox<>(new String[]{"HOME", "AUTO", "PERSONAL", "EDUCATION", "OTHER"});
        JTextField principalField = new JTextField();
        JTextField balanceField = new JTextField();
        JTextField rateField = new JTextField();
        JComboBox<String> rateTypeBox = new JComboBox<>(new String[]{"FIXED", "FLOATING"});
        JTextField tenureField = new JTextField();
        JTextField dueDayField = new JTextField("5");

        formPanel.add(new JLabel("Loan Name:"));
        formPanel.add(nameField);
        formPanel.add(new JLabel("Lender Name:"));
        formPanel.add(lenderField);
        formPanel.add(new JLabel("Loan Category:"));
        formPanel.add(typeBox);
        formPanel.add(new JLabel("Sanctioned Principal (₹):"));
        formPanel.add(principalField);
        formPanel.add(new JLabel("Current Outstanding Balance (₹):"));
        formPanel.add(balanceField);
        formPanel.add(new JLabel("Annual Interest Rate (%):"));
        formPanel.add(rateField);
        formPanel.add(new JLabel("Rate Type:"));
        formPanel.add(rateTypeBox);
        formPanel.add(new JLabel("Tenure (Months):"));
        formPanel.add(tenureField);
        formPanel.add(new JLabel("Monthly Due Day (1-31):"));
        formPanel.add(dueDayField);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton saveBtn = UITheme.createStyledButton("Save Loan", UITheme.ACCENT_BLUE, Color.WHITE);
        JButton cancelBtn = new JButton("Cancel");

        saveBtn.addActionListener(e -> {
            try {
                String name = nameField.getText().trim();
                String lender = lenderField.getText().trim();
                if (name.isEmpty() || lender.isEmpty()) {
                    JOptionPane.showMessageDialog(dialog, "Loan Name and Lender Name are required.", "Validation Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                double principal = Double.parseDouble(principalField.getText().trim());
                double balance = Double.parseDouble(balanceField.getText().trim());
                double rate = Double.parseDouble(rateField.getText().trim());
                int tenure = Integer.parseInt(tenureField.getText().trim());
                int dueDay = Integer.parseInt(dueDayField.getText().trim());

                if (principal <= 0 || balance < 0 || rate < 0 || tenure <= 0) {
                    JOptionPane.showMessageDialog(dialog, "Principal, rate, and tenure must be positive values.", "Validation Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                Loan newLoan = new Loan(
                        currentUser.getUserId(),
                        name,
                        lender,
                        (String) typeBox.getSelectedItem(),
                        principal,
                        balance,
                        rate,
                        (String) rateTypeBox.getSelectedItem(),
                        tenure,
                        LocalDate.now(),
                        dueDay,
                        balance <= 0 ? "PAID_OFF" : "ACTIVE"
                );

                boolean success = loanService.addLoan(newLoan);
                if (success) {
                    JOptionPane.showMessageDialog(dialog, "Loan added successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    dialog.dispose();
                    refreshLoans();
                    selectionManager.selectUser(currentUser);
                } else {
                    JOptionPane.showMessageDialog(dialog, "Failed to save loan to database.", "Database Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (NumberFormatException nfe) {
                JOptionPane.showMessageDialog(dialog, "Please enter valid numerical values for principal, balance, rate, and tenure.", "Validation Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        cancelBtn.addActionListener(e -> dialog.dispose());

        actions.add(cancelBtn);
        actions.add(saveBtn);

        dialog.add(formPanel, BorderLayout.CENTER);
        dialog.add(actions, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void handleDeleteLoan() {
        int selectedRow = loansTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a loan from the table to delete.", "Select Loan", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int loanId = (int) tableModel.getValueAt(selectedRow, 0);
        String loanName = (String) tableModel.getValueAt(selectedRow, 1);

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete loan '" + loanName + "' (ID: " + loanId + ")?\nThis action cannot be undone.",
                "Confirm Loan Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            boolean deleted = loanService.deleteLoan(loanId);
            if (deleted) {
                JOptionPane.showMessageDialog(this, "Loan deleted successfully.", "Deleted", JOptionPane.INFORMATION_MESSAGE);
                refreshLoans();
                if (selectionManager.getCurrentUser() != null) {
                    selectionManager.selectUser(selectionManager.getCurrentUser());
                }
            } else {
                JOptionPane.showMessageDialog(this, "Failed to delete loan from database.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showPrepaymentDialog() {
        int selectedRow = loansTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select an active loan to apply a prepayment.", "Select Loan", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int loanId = (int) tableModel.getValueAt(selectedRow, 0);
        Loan loan = loanService.getLoanById(loanId);
        if (loan == null || loan.getOutstandingBalance() <= 0) {
            JOptionPane.showMessageDialog(this, "Selected loan has no outstanding balance.", "Notice", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Record Loan Prepayment", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(440, 320);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridLayout(4, 2, 10, 14));
        panel.setBorder(new EmptyBorder(20, 24, 20, 24));

        JLabel loanInfo = new JLabel(loan.getLoanName() + " (Bal: " + UITheme.formatCurrency(loan.getOutstandingBalance()) + ")");
        loanInfo.setFont(UITheme.FONT_BOLD);

        JTextField amountField = new JTextField();
        JComboBox<String> strategyBox = new JComboBox<>(new String[]{"MANUAL", "AVALANCHE", "SNOWBALL"});
        JTextField notesField = new JTextField("Lump sum prepayment");

        panel.add(new JLabel("Target Loan:"));
        panel.add(loanInfo);
        panel.add(new JLabel("Prepayment Amount (₹):"));
        panel.add(amountField);
        panel.add(new JLabel("Strategy Tag:"));
        panel.add(strategyBox);
        panel.add(new JLabel("Notes:"));
        panel.add(notesField);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton applyBtn = UITheme.createStyledButton("Apply Prepayment", UITheme.ACCENT_GREEN, Color.WHITE);
        JButton cancelBtn = new JButton("Cancel");

        applyBtn.addActionListener(e -> {
            try {
                double amount = Double.parseDouble(amountField.getText().trim());
                Prepayment prep = new Prepayment(
                        loan.getLoanId(),
                        LocalDate.now(),
                        amount,
                        (String) strategyBox.getSelectedItem(),
                        notesField.getText().trim()
                );

                boolean done = loanService.recordPrepayment(prep);
                if (done) {
                    JOptionPane.showMessageDialog(dialog, "Prepayment recorded successfully! Outstanding balance reduced.", "Success", JOptionPane.INFORMATION_MESSAGE);
                    dialog.dispose();
                    refreshLoans();
                    if (selectionManager.getCurrentUser() != null) {
                        selectionManager.selectUser(selectionManager.getCurrentUser());
                    }
                }
            } catch (InvalidPrepaymentException ipe) {
                JOptionPane.showMessageDialog(dialog, ipe.getMessage(), "Invalid Prepayment", JOptionPane.ERROR_MESSAGE);
            } catch (NumberFormatException nfe) {
                JOptionPane.showMessageDialog(dialog, "Please enter a valid numerical amount.", "Validation Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        cancelBtn.addActionListener(e -> dialog.dispose());

        actions.add(cancelBtn);
        actions.add(applyBtn);

        dialog.add(panel, BorderLayout.CENTER);
        dialog.add(actions, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }
}
