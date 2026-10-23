package ui;

import model.AmortizationScheduleItem;
import model.Loan;
import model.User;
import service.EMICalculator;
import service.LoanService;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SchedulePanel extends JPanel implements UserChangeListener {

    private final LoanService loanService;
    private final DashboardPanel.UserSelectionManager selectionManager;

    private JComboBox<Loan> loanSelector;
    private JTable scheduleTable;
    private DefaultTableModel tableModel;
    private List<AmortizationScheduleItem> currentSchedule = new ArrayList<>();

    // Scenario Simulator UI Controls
    private JTextField extraPrepayField;
    private JTextField revisedRateField;
    private JSpinner effectiveMonthSpinner;
    private JButton applyScenarioBtn;
    private JButton resetBaselineBtn;
    private JButton exportCsvBtn;

    // Comparative Summary Banner Components
    private JLabel baselineStatsLabel;
    private JLabel scenarioStatsLabel;
    private JLabel deltaBadgeLabel;

    // Scenario State
    private boolean isScenarioActive = false;
    private EMICalculator.ScheduleSummary baselineSummary;
    private EMICalculator.ScheduleSummary currentSummary;
    private double appliedExtraPrepayment = 0.0;
    private double appliedRevisedRate = 0.0;
    private int appliedEffectiveMonth = 1;

    public SchedulePanel(LoanService loanService, DashboardPanel.UserSelectionManager selectionManager) {
        this.loanService = loanService;
        this.selectionManager = selectionManager;

        setLayout(new BorderLayout(16, 16));
        setBackground(UITheme.BG_LIGHT);
        setBorder(new EmptyBorder(20, 24, 20, 24));

        initComponents();
        selectionManager.registerListener(this);
    }

    private void initComponents() {
        // --- Top Container (Title row + Scenario Simulator controls) ---
        JPanel topContainer = new JPanel();
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));
        topContainer.setOpaque(false);

        // Top Bar: Title & Loan Selector / CSV Export
        JPanel topBar = new JPanel(new BorderLayout(16, 0));
        topBar.setOpaque(false);

        JPanel titlePanel = new JPanel(new GridLayout(2, 1, 0, 4));
        titlePanel.setOpaque(false);

        JLabel title = new JLabel("Amortization Schedule & Projection");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_MAIN);

        JLabel subtitle = new JLabel("Installment-by-installment reducing-balance principal vs interest breakdown");
        subtitle.setFont(UITheme.FONT_REGULAR);
        subtitle.setForeground(UITheme.TEXT_MUTED);

        titlePanel.add(title);
        titlePanel.add(subtitle);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);

        JLabel selectLabel = new JLabel("Select Loan Account:");
        selectLabel.setFont(UITheme.FONT_BOLD);
        selectLabel.setForeground(UITheme.TEXT_MAIN);

        loanSelector = new JComboBox<>();
        loanSelector.setFont(UITheme.FONT_REGULAR);
        loanSelector.setBackground(Color.WHITE);
        loanSelector.setPreferredSize(new Dimension(300, 36));

        loanSelector.addActionListener(e -> onLoanSelected());

        exportCsvBtn = UITheme.createStyledButton("Export to CSV", UITheme.ACCENT_GREEN, Color.WHITE);
        exportCsvBtn.addActionListener(e -> exportScheduleToCsv());

        actions.add(selectLabel);
        actions.add(loanSelector);
        actions.add(exportCsvBtn);

        topBar.add(titlePanel, BorderLayout.WEST);
        topBar.add(actions, BorderLayout.EAST);

        // Scenario Simulator Header Control Panel
        JPanel scenarioCard = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        scenarioCard.setBackground(UITheme.CARD_BG);
        scenarioCard.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(UITheme.BORDER_COLOR, 1, true),
                new EmptyBorder(8, 14, 8, 14)
        ));

        JLabel simulatorLabel = new JLabel("Scenario Simulator:");
        simulatorLabel.setFont(UITheme.FONT_BOLD);
        simulatorLabel.setForeground(UITheme.ACCENT_BLUE);

        JLabel prepayLabel = new JLabel("Extra Prepayment (₹/mo):");
        prepayLabel.setFont(UITheme.FONT_REGULAR);
        prepayLabel.setForeground(UITheme.TEXT_MAIN);

        extraPrepayField = new JTextField("0", 7);
        extraPrepayField.setFont(UITheme.FONT_REGULAR);
        extraPrepayField.setPreferredSize(new Dimension(85, 30));
        extraPrepayField.setToolTipText("Enter extra monthly prepayment (₹)");
        extraPrepayField.addActionListener(e -> applyScenario());

        JLabel rateLabel = new JLabel("Revised Interest Rate (%):");
        rateLabel.setFont(UITheme.FONT_REGULAR);
        rateLabel.setForeground(UITheme.TEXT_MAIN);

        revisedRateField = new JTextField("", 5);
        revisedRateField.setFont(UITheme.FONT_REGULAR);
        revisedRateField.setPreferredSize(new Dimension(65, 30));
        revisedRateField.setToolTipText("Enter revised annual rate (%)");
        revisedRateField.addActionListener(e -> applyScenario());

        JLabel monthLabel = new JLabel("Effective From Month #:");
        monthLabel.setFont(UITheme.FONT_REGULAR);
        monthLabel.setForeground(UITheme.TEXT_MAIN);

        effectiveMonthSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 600, 1));
        effectiveMonthSpinner.setFont(UITheme.FONT_REGULAR);
        effectiveMonthSpinner.setPreferredSize(new Dimension(65, 30));
        effectiveMonthSpinner.setToolTipText("Installment number from which revised rate and extra prepayment take effect");

        applyScenarioBtn = UITheme.createStyledButton("Apply Scenario", UITheme.ACCENT_BLUE, Color.WHITE);
        applyScenarioBtn.addActionListener(e -> applyScenario());

        resetBaselineBtn = UITheme.createStyledButton("Reset to Baseline", new Color(241, 245, 249), UITheme.TEXT_MAIN);
        resetBaselineBtn.setBorder(new CompoundBorder(
                new LineBorder(UITheme.BORDER_COLOR, 1, true),
                new EmptyBorder(8, 14, 8, 14)
        ));
        resetBaselineBtn.addActionListener(e -> resetToBaseline());

        scenarioCard.add(simulatorLabel);
        scenarioCard.add(prepayLabel);
        scenarioCard.add(extraPrepayField);
        scenarioCard.add(rateLabel);
        scenarioCard.add(revisedRateField);
        scenarioCard.add(monthLabel);
        scenarioCard.add(effectiveMonthSpinner);
        scenarioCard.add(applyScenarioBtn);
        scenarioCard.add(resetBaselineBtn);

        topBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        scenarioCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        topContainer.add(topBar);
        topContainer.add(Box.createVerticalStrut(12));
        topContainer.add(scenarioCard);

        // --- Center Table Card ---
        JPanel tableCard = new JPanel(new BorderLayout(0, 10));
        tableCard.setBackground(UITheme.CARD_BG);
        tableCard.setBorder(UITheme.createCardBorder());

        String[] cols = {
                "Inst. #", "Due Date", "Opening Balance (₹)",
                "EMI (₹)", "Principal Paid (₹)", "Interest Paid (₹)", "Closing Balance (₹)"
        };

        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        scheduleTable = new JTable(tableModel);
        scheduleTable.setRowHeight(30);
        scheduleTable.setFont(UITheme.FONT_REGULAR);
        scheduleTable.getTableHeader().setFont(UITheme.FONT_BOLD);
        scheduleTable.getTableHeader().setBackground(new Color(241, 245, 249));
        scheduleTable.setSelectionBackground(new Color(219, 234, 254));
        scheduleTable.setGridColor(UITheme.BORDER_COLOR);

        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        for (int i = 2; i <= 6; i++) {
            scheduleTable.getColumnModel().getColumn(i).setCellRenderer(rightRenderer);
        }

        JScrollPane scrollPane = new JScrollPane(scheduleTable);
        scrollPane.setBorder(new LineBorder(UITheme.BORDER_COLOR, 1));

        // --- Comparative Summary Footer Banner ---
        JPanel comparisonBanner = new JPanel(new BorderLayout(16, 8));
        comparisonBanner.setBackground(new Color(248, 250, 252));
        comparisonBanner.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(UITheme.BORDER_COLOR, 1, true),
                new EmptyBorder(12, 16, 12, 16)
        ));

        JPanel statsPanel = new JPanel(new GridLayout(2, 1, 0, 6));
        statsPanel.setOpaque(false);

        baselineStatsLabel = new JLabel("Baseline: Select a loan account to calculate amortization projection.");
        baselineStatsLabel.setFont(UITheme.FONT_REGULAR);
        baselineStatsLabel.setForeground(UITheme.TEXT_MAIN);

        scenarioStatsLabel = new JLabel("Scenario: No simulation active.");
        scenarioStatsLabel.setFont(UITheme.FONT_REGULAR);
        scenarioStatsLabel.setForeground(UITheme.TEXT_MUTED);

        statsPanel.add(baselineStatsLabel);
        statsPanel.add(scenarioStatsLabel);

        JPanel badgeContainer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 4));
        badgeContainer.setOpaque(false);

        deltaBadgeLabel = new JLabel("Baseline Active");
        deltaBadgeLabel.setFont(UITheme.FONT_BOLD);
        deltaBadgeLabel.setOpaque(true);
        deltaBadgeLabel.setBackground(new Color(241, 245, 249));
        deltaBadgeLabel.setForeground(UITheme.TEXT_MUTED);
        deltaBadgeLabel.setBorder(new CompoundBorder(
                new LineBorder(UITheme.BORDER_COLOR, 1, true),
                new EmptyBorder(6, 14, 6, 14)
        ));

        badgeContainer.add(deltaBadgeLabel);

        comparisonBanner.add(statsPanel, BorderLayout.CENTER);
        comparisonBanner.add(badgeContainer, BorderLayout.EAST);

        tableCard.add(scrollPane, BorderLayout.CENTER);
        tableCard.add(comparisonBanner, BorderLayout.SOUTH);

        add(topContainer, BorderLayout.NORTH);
        add(tableCard, BorderLayout.CENTER);
    }

    @Override
    public void onUserChanged(User newUser) {
        populateLoanSelector(newUser);
    }

    public void populateLoanSelector(User user) {
        loanSelector.removeAllItems();
        if (user == null) {
            clearScheduleDisplay();
            baselineStatsLabel.setText("No active user profile selected.");
            return;
        }

        List<Loan> loans = loanService.getActiveLoansByUserId(user.getUserId());
        for (Loan l : loans) {
            loanSelector.addItem(l);
        }

        if (loans.isEmpty()) {
            clearScheduleDisplay();
            baselineStatsLabel.setText("No active loans found for " + user.getFullName());
        } else {
            loanSelector.setSelectedIndex(0);
        }
    }

    private void onLoanSelected() {
        Loan selectedLoan = (Loan) loanSelector.getSelectedItem();
        if (selectedLoan == null) {
            clearScheduleDisplay();
            return;
        }

        revisedRateField.setText(String.format(Locale.US, "%.2f", selectedLoan.getAnnualInterestRate()));
        extraPrepayField.setText("0");
        effectiveMonthSpinner.setValue(1);
        isScenarioActive = false;
        appliedExtraPrepayment = 0.0;
        appliedRevisedRate = selectedLoan.getAnnualInterestRate();
        appliedEffectiveMonth = 1;

        generateBaselineSchedule();
    }

    private void clearScheduleDisplay() {
        tableModel.setRowCount(0);
        currentSchedule.clear();
        baselineSummary = null;
        currentSummary = null;
        isScenarioActive = false;
        baselineStatsLabel.setText("Select a loan account to calculate amortization projection.");
        scenarioStatsLabel.setText("Scenario: No simulation active.");
        deltaBadgeLabel.setText("No Loan Selected");
        deltaBadgeLabel.setForeground(UITheme.TEXT_MUTED);
        deltaBadgeLabel.setBackground(new Color(241, 245, 249));
        deltaBadgeLabel.setBorder(new CompoundBorder(
                new LineBorder(UITheme.BORDER_COLOR, 1, true),
                new EmptyBorder(6, 14, 6, 14)
        ));
    }

    private void generateBaselineSchedule() {
        Loan selectedLoan = (Loan) loanSelector.getSelectedItem();
        if (selectedLoan == null) {
            clearScheduleDisplay();
            return;
        }

        LocalDate start = selectedLoan.getStartDate() != null ? selectedLoan.getStartDate() : LocalDate.now();
        int tenure = selectedLoan.getTenureMonths() > 0 ? selectedLoan.getTenureMonths() : 12;
        double balance = selectedLoan.getOutstandingBalance() > 0 ? selectedLoan.getOutstandingBalance() : selectedLoan.getPrincipalAmount();

        List<AmortizationScheduleItem> baseline = EMICalculator.generateAmortizationSchedule(
                balance,
                selectedLoan.getAnnualInterestRate(),
                tenure,
                start
        );

        baselineSummary = new EMICalculator.ScheduleSummary(baseline);
        currentSchedule = baseline;
        currentSummary = baselineSummary;
        isScenarioActive = false;
        appliedExtraPrepayment = 0.0;
        appliedRevisedRate = selectedLoan.getAnnualInterestRate();
        appliedEffectiveMonth = 1;

        displaySchedule(currentSchedule);
        updateComparisonBanner();
    }

    private void applyScenario() {
        Loan selectedLoan = (Loan) loanSelector.getSelectedItem();
        if (selectedLoan == null) {
            JOptionPane.showMessageDialog(this, "Please select an active loan first.", "No Loan Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        double extraPrepayment;
        try {
            String extraText = extraPrepayField.getText().trim();
            extraPrepayment = extraText.isEmpty() ? 0.0 : Double.parseDouble(extraText);
            if (extraPrepayment < 0) {
                JOptionPane.showMessageDialog(this, "Extra prepayment cannot be negative.", "Validation Error", JOptionPane.ERROR_MESSAGE);
                extraPrepayField.requestFocus();
                return;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter a valid numeric amount for extra prepayment.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
            extraPrepayField.requestFocus();
            return;
        }

        double revisedRate;
        try {
            String rateText = revisedRateField.getText().trim();
            revisedRate = Double.parseDouble(rateText);
            if (revisedRate <= 0 || revisedRate > 100) {
                JOptionPane.showMessageDialog(this, "Revised interest rate must be between 0.1% and 100%.", "Validation Error", JOptionPane.ERROR_MESSAGE);
                revisedRateField.requestFocus();
                return;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter a valid numeric interest rate.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
            revisedRateField.requestFocus();
            return;
        }

        int effectiveMonth = (Integer) effectiveMonthSpinner.getValue();
        if (effectiveMonth < 1) {
            effectiveMonth = 1;
            effectiveMonthSpinner.setValue(1);
        }

        double principal = selectedLoan.getOutstandingBalance() > 0 ? selectedLoan.getOutstandingBalance() : selectedLoan.getPrincipalAmount();
        int originalTenure = selectedLoan.getTenureMonths() > 0 ? selectedLoan.getTenureMonths() : 12;
        LocalDate start = selectedLoan.getStartDate() != null ? selectedLoan.getStartDate() : LocalDate.now();

        currentSchedule = EMICalculator.generateScenarioSchedule(
                principal,
                selectedLoan.getAnnualInterestRate(),
                revisedRate,
                effectiveMonth,
                extraPrepayment,
                originalTenure,
                start
        );

        currentSummary = new EMICalculator.ScheduleSummary(currentSchedule);
        isScenarioActive = true;
        appliedExtraPrepayment = extraPrepayment;
        appliedRevisedRate = revisedRate;
        appliedEffectiveMonth = effectiveMonth;

        displaySchedule(currentSchedule);
        updateComparisonBanner();
    }

    private void resetToBaseline() {
        Loan selectedLoan = (Loan) loanSelector.getSelectedItem();
        if (selectedLoan == null) return;

        extraPrepayField.setText("0");
        revisedRateField.setText(String.format(Locale.US, "%.2f", selectedLoan.getAnnualInterestRate()));
        effectiveMonthSpinner.setValue(1);
        isScenarioActive = false;
        appliedExtraPrepayment = 0.0;
        appliedRevisedRate = selectedLoan.getAnnualInterestRate();
        appliedEffectiveMonth = 1;

        generateBaselineSchedule();
    }

    private void displaySchedule(List<AmortizationScheduleItem> schedule) {
        tableModel.setRowCount(0);
        for (AmortizationScheduleItem item : schedule) {
            tableModel.addRow(new Object[]{
                    "#" + item.getInstallmentNumber(),
                    item.getPaymentDate().toString(),
                    UITheme.formatCurrency(item.getOpeningBalance()),
                    UITheme.formatCurrency(item.getEmi()),
                    UITheme.formatCurrency(item.getPrincipalPaid()),
                    UITheme.formatCurrency(item.getInterestPaid()),
                    UITheme.formatCurrency(item.getClosingBalance())
            });
        }
    }

    private void updateComparisonBanner() {
        Loan selectedLoan = (Loan) loanSelector.getSelectedItem();
        if (selectedLoan == null || baselineSummary == null || currentSummary == null) {
            return;
        }

        // Format baseline stats
        baselineStatsLabel.setText(String.format(
                "Baseline:   Payoff: %d months  |  Total Interest: %s  |  Lifetime Outflow: %s",
                baselineSummary.getTotalMonths(),
                UITheme.formatCurrency(baselineSummary.getTotalInterest()),
                UITheme.formatCurrency(baselineSummary.getTotalOutflow())
        ));

        if (!isScenarioActive) {
            scenarioStatsLabel.setText("Scenario:   Baseline active (no prepayments or rate adjustments applied).");
            deltaBadgeLabel.setText("Baseline Schedule Active");
            deltaBadgeLabel.setForeground(UITheme.TEXT_MUTED);
            deltaBadgeLabel.setBackground(new Color(241, 245, 249));
            deltaBadgeLabel.setBorder(new CompoundBorder(
                    new LineBorder(UITheme.BORDER_COLOR, 1, true),
                    new EmptyBorder(6, 14, 6, 14)
            ));
            return;
        }

        // Format scenario stats
        scenarioStatsLabel.setText(String.format(
                "Scenario:   Payoff: %d months  |  Total Interest: %s  |  Lifetime Outflow: %s",
                currentSummary.getTotalMonths(),
                UITheme.formatCurrency(currentSummary.getTotalInterest()),
                UITheme.formatCurrency(currentSummary.getTotalOutflow())
        ));

        int monthsSaved = baselineSummary.getTotalMonths() - currentSummary.getTotalMonths();
        double interestSaved = baselineSummary.getTotalInterest() - currentSummary.getTotalInterest();

        if (monthsSaved > 0 && interestSaved > 0) {
            deltaBadgeLabel.setText(String.format("✓ Saves %d month%s and %s in interest",
                    monthsSaved, monthsSaved > 1 ? "s" : "", UITheme.formatCurrency(interestSaved)));
            deltaBadgeLabel.setForeground(UITheme.ACCENT_GREEN);
            deltaBadgeLabel.setBackground(new Color(236, 253, 245));
            deltaBadgeLabel.setBorder(new CompoundBorder(
                    new LineBorder(UITheme.ACCENT_GREEN, 1, true),
                    new EmptyBorder(6, 14, 6, 14)
            ));
        } else if (monthsSaved > 0 && interestSaved <= 0) {
            deltaBadgeLabel.setText(String.format("Saves %d month%s, but adds %s in interest",
                    monthsSaved, monthsSaved > 1 ? "s" : "", UITheme.formatCurrency(-interestSaved)));
            deltaBadgeLabel.setForeground(UITheme.ACCENT_ORANGE);
            deltaBadgeLabel.setBackground(new Color(255, 247, 237));
            deltaBadgeLabel.setBorder(new CompoundBorder(
                    new LineBorder(UITheme.ACCENT_ORANGE, 1, true),
                    new EmptyBorder(6, 14, 6, 14)
            ));
        } else if (interestSaved < 0) {
            String monthNote = monthsSaved < 0 ? String.format(" (+%d months)", -monthsSaved) : "";
            deltaBadgeLabel.setText(String.format("⚠ Adds %s in interest%s",
                    UITheme.formatCurrency(-interestSaved), monthNote));
            deltaBadgeLabel.setForeground(UITheme.ACCENT_RED);
            deltaBadgeLabel.setBackground(new Color(254, 242, 242));
            deltaBadgeLabel.setBorder(new CompoundBorder(
                    new LineBorder(UITheme.ACCENT_RED, 1, true),
                    new EmptyBorder(6, 14, 6, 14)
            ));
        } else if (interestSaved > 0 && monthsSaved == 0) {
            deltaBadgeLabel.setText(String.format("✓ Saves %s in interest (same tenure)",
                    UITheme.formatCurrency(interestSaved)));
            deltaBadgeLabel.setForeground(UITheme.ACCENT_GREEN);
            deltaBadgeLabel.setBackground(new Color(236, 253, 245));
            deltaBadgeLabel.setBorder(new CompoundBorder(
                    new LineBorder(UITheme.ACCENT_GREEN, 1, true),
                    new EmptyBorder(6, 14, 6, 14)
            ));
        } else {
            deltaBadgeLabel.setText("Scenario matches baseline schedule");
            deltaBadgeLabel.setForeground(UITheme.TEXT_MUTED);
            deltaBadgeLabel.setBackground(new Color(241, 245, 249));
            deltaBadgeLabel.setBorder(new CompoundBorder(
                    new LineBorder(UITheme.BORDER_COLOR, 1, true),
                    new EmptyBorder(6, 14, 6, 14)
            ));
        }
    }

    private void exportScheduleToCsv() {
        if (currentSchedule.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No amortization schedule available to export.", "Empty Schedule", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Loan selectedLoan = (Loan) loanSelector.getSelectedItem();
        String suffix = isScenarioActive ? "_Scenario" : "_Baseline";
        String defaultFileName = (selectedLoan != null ?
                selectedLoan.getLoanName().replaceAll("[^a-zA-Z0-9_-]", "_") : "Amortization") + suffix + "_Schedule.csv";

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Amortization Schedule to CSV");
        fileChooser.setSelectedFile(new File(defaultFileName));
        fileChooser.setFileFilter(new FileNameExtensionFilter("CSV Files (*.csv)", "csv"));

        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            if (!fileToSave.getName().toLowerCase().endsWith(".csv")) {
                fileToSave = new File(fileToSave.getAbsolutePath() + ".csv");
            }

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileToSave))) {
                writer.write("# LoanWise Amortization Schedule Export");
                writer.newLine();
                if (selectedLoan != null) {
                    writer.write(String.format(Locale.US, "# Loan: %s | Lender: %s | Principal: %.2f",
                            selectedLoan.getLoanName(), selectedLoan.getLenderName(), selectedLoan.getPrincipalAmount()));
                    writer.newLine();
                }
                if (isScenarioActive && baselineSummary != null && currentSummary != null) {
                    writer.write(String.format(Locale.US, "# Mode: Scenario Simulation (Extra Prepayment: %.2f/mo, Revised Rate: %.2f%% from Month %d)",
                            appliedExtraPrepayment, appliedRevisedRate, appliedEffectiveMonth));
                    writer.newLine();
                    writer.write(String.format(Locale.US, "# Baseline: %d months | Interest: %.2f | Outflow: %.2f",
                            baselineSummary.getTotalMonths(), baselineSummary.getTotalInterest(), baselineSummary.getTotalOutflow()));
                    writer.newLine();
                    writer.write(String.format(Locale.US, "# Scenario: %d months | Interest: %.2f | Outflow: %.2f",
                            currentSummary.getTotalMonths(), currentSummary.getTotalInterest(), currentSummary.getTotalOutflow()));
                    writer.newLine();
                    int mDelta = baselineSummary.getTotalMonths() - currentSummary.getTotalMonths();
                    double iDelta = baselineSummary.getTotalInterest() - currentSummary.getTotalInterest();
                    writer.write(String.format(Locale.US, "# Net Impact: %s (%d months saved, %.2f interest saved)",
                            deltaBadgeLabel.getText().replace("✓ ", "").replace("⚠ ", ""), mDelta, iDelta));
                    writer.newLine();
                } else if (baselineSummary != null) {
                    writer.write("# Mode: Baseline Schedule");
                    writer.newLine();
                    writer.write(String.format(Locale.US, "# Baseline: %d months | Interest: %.2f | Outflow: %.2f",
                            baselineSummary.getTotalMonths(), baselineSummary.getTotalInterest(), baselineSummary.getTotalOutflow()));
                    writer.newLine();
                }
                writer.write("#");
                writer.newLine();
                writer.write("Installment Number,Due Date,Opening Balance,EMI,Principal Paid,Interest Paid,Closing Balance");
                writer.newLine();

                for (AmortizationScheduleItem item : currentSchedule) {
                    writer.write(String.format(Locale.US, "%d,%s,%.2f,%.2f,%.2f,%.2f,%.2f",
                            item.getInstallmentNumber(),
                            item.getPaymentDate(),
                            item.getOpeningBalance(),
                            item.getEmi(),
                            item.getPrincipalPaid(),
                            item.getInterestPaid(),
                            item.getClosingBalance()
                    ));
                    writer.newLine();
                }

                JOptionPane.showMessageDialog(this, "Schedule exported successfully to:\n" + fileToSave.getAbsolutePath(), "Export Complete", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error exporting CSV: " + ex.getMessage(), "File Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
