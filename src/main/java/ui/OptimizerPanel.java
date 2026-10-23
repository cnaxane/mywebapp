package ui;

import model.Loan;
import model.User;
import service.LoanService;
import service.SimulationThread;
import strategy.AvalancheStrategy;
import strategy.PayoffStrategy;
import strategy.SnowballStrategy;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class OptimizerPanel extends JPanel implements UserChangeListener {

    private final LoanService loanService;
    private final DashboardPanel.UserSelectionManager selectionManager;

    private final PayoffStrategy avalancheStrategy = new AvalancheStrategy();
    private final PayoffStrategy snowballStrategy = new SnowballStrategy();

    private JRadioButton avalancheRadio;
    private JRadioButton snowballRadio;
    private JSlider extraPaymentSlider;
    private JTextField extraPaymentField;
    private boolean isUpdatingFromText = false;
    private JLabel sliderValueLabel;
    private JButton simulateBtn;
    private JProgressBar progressBar;

    // Metric Displays
    private JLabel interestSavedLabel;
    private JLabel monthsSavedLabel;
    private JLabel baselineDateLabel;
    private JLabel acceleratedDateLabel;
    private JLabel strategyExplanationLabel;

    // Priority Table
    private DefaultTableModel priorityTableModel;
    private JTable priorityTable;

    public OptimizerPanel(LoanService loanService, DashboardPanel.UserSelectionManager selectionManager) {
        this.loanService = loanService;
        this.selectionManager = selectionManager;

        setLayout(new BorderLayout(16, 16));
        setBackground(UITheme.BG_LIGHT);
        setBorder(new EmptyBorder(20, 24, 20, 24));

        initComponents();
        selectionManager.registerListener(this);
    }

    private void initComponents() {
        // --- Header ---
        JPanel topBar = new JPanel(new GridLayout(2, 1, 0, 4));
        topBar.setOpaque(false);

        JLabel title = new JLabel("Debt Acceleration & Prepayment Optimizer");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_MAIN);

        JLabel subtitle = new JLabel("Simulate Debt Avalanche (Rate-First) vs Debt Snowball (Balance-First) using background multithreading");
        subtitle.setFont(UITheme.FONT_REGULAR);
        subtitle.setForeground(UITheme.TEXT_MUTED);

        topBar.add(title);
        topBar.add(subtitle);

        // --- Left Control Card & Right Results Card ---
        JPanel centerPanel = new JPanel(new GridLayout(1, 2, 16, 0));
        centerPanel.setOpaque(false);

        // Left Controls Panel
        JPanel controlsCard = new JPanel(new BorderLayout(0, 16));
        controlsCard.setBackground(UITheme.CARD_BG);
        controlsCard.setBorder(UITheme.createCardBorder());

        JLabel configHeader = new JLabel("Simulation Configuration");
        configHeader.setFont(UITheme.FONT_HEADER);
        configHeader.setForeground(UITheme.TEXT_MAIN);

        JPanel configBody = new JPanel();
        configBody.setLayout(new BoxLayout(configBody, BoxLayout.Y_AXIS));
        configBody.setOpaque(false);

        // Strategy Selector
        JLabel stratLabel = new JLabel("Choose Payoff Acceleration Strategy:");
        stratLabel.setFont(UITheme.FONT_BOLD);
        stratLabel.setForeground(UITheme.TEXT_MAIN);
        stratLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        avalancheRadio = new JRadioButton("Debt Avalanche (Highest Interest Rate First)", true);
        avalancheRadio.setFont(UITheme.FONT_REGULAR);
        avalancheRadio.setOpaque(false);

        snowballRadio = new JRadioButton("Debt Snowball (Smallest Outstanding Balance First)", false);
        snowballRadio.setFont(UITheme.FONT_REGULAR);
        snowballRadio.setOpaque(false);

        ButtonGroup strategyGroup = new ButtonGroup();
        strategyGroup.add(avalancheRadio);
        strategyGroup.add(snowballRadio);

        avalancheRadio.addActionListener(e -> updatePriorityTablePreview());
        snowballRadio.addActionListener(e -> updatePriorityTablePreview());

        strategyExplanationLabel = new JLabel("<html><i>" + avalancheStrategy.getDescription() + "</i></html>");
        strategyExplanationLabel.setFont(UITheme.FONT_SMALL);
        strategyExplanationLabel.setForeground(UITheme.TEXT_MUTED);
        strategyExplanationLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Prepayment Slider & Direct Numeric Input Header
        JPanel budgetHeaderPanel = new JPanel(new BorderLayout(8, 0));
        budgetHeaderPanel.setOpaque(false);
        budgetHeaderPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        budgetHeaderPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel sliderTitle = new JLabel("Extra Monthly Prepayment Budget (₹):");
        sliderTitle.setFont(UITheme.FONT_BOLD);
        sliderTitle.setForeground(UITheme.TEXT_MAIN);

        extraPaymentField = new JTextField("5000", 7);
        extraPaymentField.setFont(UITheme.FONT_BOLD);
        extraPaymentField.setHorizontalAlignment(JTextField.RIGHT);
        extraPaymentField.setPreferredSize(new Dimension(110, 32));
        extraPaymentField.putClientProperty("JTextField.placeholderText", "0 - 50000");

        extraPaymentField.addActionListener(e -> syncFromTextField());
        extraPaymentField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                syncFromTextField();
            }
        });

        budgetHeaderPanel.add(sliderTitle, BorderLayout.WEST);
        budgetHeaderPanel.add(extraPaymentField, BorderLayout.EAST);

        extraPaymentSlider = new JSlider(0, 50000, 5000);
        extraPaymentSlider.setMajorTickSpacing(10000);
        extraPaymentSlider.setMinorTickSpacing(2500);
        extraPaymentSlider.setPaintTicks(true);
        extraPaymentSlider.setPaintLabels(true);
        extraPaymentSlider.setBackground(Color.WHITE);
        extraPaymentSlider.setAlignmentX(Component.LEFT_ALIGNMENT);

        sliderValueLabel = new JLabel("Selected Extra Monthly Payment: ₹5,000");
        sliderValueLabel.setFont(UITheme.FONT_BOLD);
        sliderValueLabel.setForeground(UITheme.ACCENT_BLUE);
        sliderValueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        extraPaymentSlider.addChangeListener(e -> {
            if (!isUpdatingFromText) {
                int val = extraPaymentSlider.getValue();
                extraPaymentField.setText(String.valueOf(val));
                sliderValueLabel.setText("Selected Extra Monthly Payment: " + UITheme.formatCurrency(val));
            }
        });

        // Trigger Button and Progress
        simulateBtn = UITheme.createStyledButton("Simulate Acceleration Savings", UITheme.ACCENT_BLUE, Color.WHITE);
        simulateBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        simulateBtn.addActionListener(e -> triggerSimulation());

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setVisible(false);
        progressBar.setAlignmentX(Component.LEFT_ALIGNMENT);

        configBody.add(stratLabel);
        configBody.add(Box.createVerticalStrut(6));
        configBody.add(avalancheRadio);
        configBody.add(snowballRadio);
        configBody.add(Box.createVerticalStrut(4));
        configBody.add(strategyExplanationLabel);
        configBody.add(Box.createVerticalStrut(20));
        configBody.add(budgetHeaderPanel);
        configBody.add(Box.createVerticalStrut(6));
        configBody.add(extraPaymentSlider);
        configBody.add(Box.createVerticalStrut(8));
        configBody.add(sliderValueLabel);
        configBody.add(Box.createVerticalStrut(20));
        configBody.add(simulateBtn);
        configBody.add(Box.createVerticalStrut(10));
        configBody.add(progressBar);

        controlsCard.add(configHeader, BorderLayout.NORTH);
        controlsCard.add(configBody, BorderLayout.CENTER);

        // Right Results Panel
        JPanel resultsCard = new JPanel(new BorderLayout(0, 14));
        resultsCard.setBackground(UITheme.CARD_BG);
        resultsCard.setBorder(UITheme.createCardBorder());

        JLabel resultsHeader = new JLabel("Projected Financial Impact & Payoff Priority");
        resultsHeader.setFont(UITheme.FONT_HEADER);
        resultsHeader.setForeground(UITheme.TEXT_MAIN);

        // KPI Metrics Row
        JPanel kpiRow = new JPanel(new GridLayout(2, 2, 12, 12));
        kpiRow.setOpaque(false);

        interestSavedLabel = new JLabel("₹0.00");
        interestSavedLabel.setFont(UITheme.FONT_KPI_VAL);
        interestSavedLabel.setForeground(UITheme.ACCENT_GREEN);

        monthsSavedLabel = new JLabel("0 Months");
        monthsSavedLabel.setFont(UITheme.FONT_KPI_VAL);
        monthsSavedLabel.setForeground(UITheme.ACCENT_BLUE);

        baselineDateLabel = new JLabel("—");
        baselineDateLabel.setFont(UITheme.FONT_BOLD);
        baselineDateLabel.setForeground(UITheme.TEXT_MUTED);

        acceleratedDateLabel = new JLabel("—");
        acceleratedDateLabel.setFont(UITheme.FONT_BOLD);
        acceleratedDateLabel.setForeground(UITheme.ACCENT_GREEN);

        kpiRow.add(createMiniCard("TOTAL INTEREST ELIMINATED", interestSavedLabel));
        kpiRow.add(createMiniCard("TENURE REDUCED (MONTHS SAVED)", monthsSavedLabel));
        kpiRow.add(createMiniCard("BASELINE DEBT-FREE DATE", baselineDateLabel));
        kpiRow.add(createMiniCard("ACCELERATED DEBT-FREE DATE", acceleratedDateLabel));

        // Priority Table
        String[] prioCols = {"Priority Order", "Loan Name", "Lender", "Balance (₹)", "Rate (%)"};
        priorityTableModel = new DefaultTableModel(prioCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        priorityTable = new JTable(priorityTableModel);
        priorityTable.setRowHeight(28);
        priorityTable.setFont(UITheme.FONT_REGULAR);
        priorityTable.getTableHeader().setFont(UITheme.FONT_BOLD);
        priorityTable.getTableHeader().setBackground(new Color(241, 245, 249));

        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        priorityTable.getColumnModel().getColumn(3).setCellRenderer(rightRenderer);
        priorityTable.getColumnModel().getColumn(4).setCellRenderer(rightRenderer);

        JScrollPane scroll = new JScrollPane(priorityTable);
        scroll.setBorder(new LineBorder(UITheme.BORDER_COLOR, 1));

        JPanel centerResults = new JPanel(new BorderLayout(0, 10));
        centerResults.setOpaque(false);
        centerResults.add(kpiRow, BorderLayout.NORTH);

        JLabel prioTitle = new JLabel("Target Payoff Sequence (Strategy Allocation Queue):");
        prioTitle.setFont(UITheme.FONT_BOLD);
        prioTitle.setForeground(UITheme.TEXT_MAIN);

        JPanel tableWrapper = new JPanel(new BorderLayout(0, 6));
        tableWrapper.setOpaque(false);
        tableWrapper.add(prioTitle, BorderLayout.NORTH);
        tableWrapper.add(scroll, BorderLayout.CENTER);

        centerResults.add(tableWrapper, BorderLayout.CENTER);

        resultsCard.add(resultsHeader, BorderLayout.NORTH);
        resultsCard.add(centerResults, BorderLayout.CENTER);

        centerPanel.add(controlsCard);
        centerPanel.add(resultsCard);

        add(topBar, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
    }

    private JPanel createMiniCard(String title, JLabel valLabel) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setBackground(new Color(248, 250, 252));
        p.setBorder(new CompoundBorder(new LineBorder(UITheme.BORDER_COLOR, 1, true), new EmptyBorder(10, 12, 10, 12)));

        JLabel t = new JLabel(title);
        t.setFont(UITheme.FONT_SMALL);
        t.setForeground(UITheme.TEXT_MUTED);

        p.add(t, BorderLayout.NORTH);
        p.add(valLabel, BorderLayout.CENTER);
        return p;
    }

    private PayoffStrategy getSelectedStrategy() {
        return avalancheRadio.isSelected() ? avalancheStrategy : snowballStrategy;
    }

    public void updatePriorityTablePreview() {
        PayoffStrategy strategy = getSelectedStrategy();
        strategyExplanationLabel.setText("<html><i>" + strategy.getDescription() + "</i></html>");

        User user = selectionManager.getCurrentUser();
        priorityTableModel.setRowCount(0);
        if (user == null) return;

        List<Loan> active = loanService.getActiveLoansByUserId(user.getUserId());
        List<Loan> sorted = loanService.getPrioritizedLoans(active, strategy);

        int rank = 1;
        for (Loan l : sorted) {
            priorityTableModel.addRow(new Object[]{
                    "#" + (rank++) + " " + (rank == 2 ? "(Primary Target)" : ""),
                    l.getLoanName(),
                    l.getLenderName(),
                    UITheme.formatCurrency(l.getOutstandingBalance()),
                    String.format("%.2f%%", l.getAnnualInterestRate())
            });
        }
    }

    private void syncFromTextField() {
        String text = extraPaymentField.getText().replaceAll("[^0-9]", "").trim();
        int val = 0;
        if (!text.isEmpty()) {
            try {
                val = Integer.parseInt(text);
            } catch (NumberFormatException e) {
                val = extraPaymentSlider.getValue();
            }
        }
        // Clamp strictly within slider min/max bounds (0 to 50000)
        val = Math.max(0, Math.min(50000, val));

        isUpdatingFromText = true;
        try {
            extraPaymentSlider.setValue(val);
            extraPaymentField.setText(String.valueOf(val));
            sliderValueLabel.setText("Selected Extra Monthly Payment: " + UITheme.formatCurrency(val));
        } finally {
            isUpdatingFromText = false;
        }
    }

    private void triggerSimulation() {
        syncFromTextField();

        User user = selectionManager.getCurrentUser();
        if (user == null) {
            JOptionPane.showMessageDialog(this, "Please select an active user profile first.", "Notice", JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<Loan> activeLoans = loanService.getActiveLoansByUserId(user.getUserId());
        if (activeLoans.isEmpty()) {
            JOptionPane.showMessageDialog(this, "The selected user has no active loans to simulate.", "No Active Loans", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        PayoffStrategy strategy = getSelectedStrategy();
        double extraPayment = extraPaymentSlider.getValue();

        simulateBtn.setEnabled(false);
        progressBar.setValue(0);
        progressBar.setVisible(true);

        SimulationThread.SimulationCallback callback = new SimulationThread.SimulationCallback() {
            @Override
            public void onSimulationStarted() {
                progressBar.setValue(10);
            }

            @Override
            public void onSimulationProgress(int percentComplete) {
                progressBar.setValue(percentComplete);
            }

            @Override
            public void onSimulationCompleted(SimulationThread.SimulationResult result) {
                progressBar.setValue(100);
                simulateBtn.setEnabled(true);
                progressBar.setVisible(false);

                // Update Display
                interestSavedLabel.setText(UITheme.formatCurrency(result.getInterestSaved()));
                monthsSavedLabel.setText(result.getMonthsSaved() + " Months (" +
                        String.format("%.1f", result.getMonthsSaved() / 12.0) + " Years)");

                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MMM yyyy");
                baselineDateLabel.setText(result.getDebtFreeDateBaseline().format(dtf) +
                        " (" + result.getBaselineMonths() + " mos)");
                acceleratedDateLabel.setText(result.getDebtFreeDateAccelerated().format(dtf) +
                        " (" + result.getAcceleratedMonths() + " mos)");

                updatePriorityTablePreview();
            }

            @Override
            public void onSimulationFailed(String errorMessage) {
                simulateBtn.setEnabled(true);
                progressBar.setVisible(false);
                JOptionPane.showMessageDialog(OptimizerPanel.this, errorMessage, "Simulation Error", JOptionPane.ERROR_MESSAGE);
            }
        };

        // Launch background thread
        SimulationThread simulationThread = new SimulationThread(activeLoans, strategy, extraPayment, callback);
        Thread worker = new Thread(simulationThread, "Simulation-Worker-Thread");
        worker.start();
    }

    @Override
    public void onUserChanged(User newUser) {
        interestSavedLabel.setText("₹0.00");
        monthsSavedLabel.setText("0 Months");
        baselineDateLabel.setText("—");
        acceleratedDateLabel.setText("—");
        updatePriorityTablePreview();
    }
}
