package ui;

import model.Loan;
import model.User;
import service.EMICalculator;
import service.LoanService;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class DashboardPanel extends JPanel {

    private final LoanService loanService;
    private final UserSelectionManager selectionManager;

    private JLabel accountBadgeLabel;
    private JLabel totalDebtLabel;
    private JLabel activeLoansLabel;
    private JLabel totalEmiLabel;
    private JLabel monthlyIncomeLabel;
    private JLabel dtiRatioLabel;
    private JLabel dtiBadgeLabel;
    private JTable portfolioTable;
    private DefaultTableModel tableModel;

    public interface UserSelectionManager {
        User getCurrentUser();
        void selectUser(User user);
        void registerListener(UserChangeListener listener);
    }

    public DashboardPanel(LoanService loanService, UserSelectionManager selectionManager) {
        this.loanService = loanService;
        this.selectionManager = selectionManager;

        setLayout(new BorderLayout(16, 16));
        setBackground(UITheme.BG_LIGHT);
        setBorder(new EmptyBorder(20, 24, 20, 24));

        initComponents();

        // Register to receive updates when user changes externally
        selectionManager.registerListener(user -> {
            refreshData(user);
        });
    }

    private void initComponents() {
        // --- Top Bar: User Switcher & Header ---
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);

        JPanel titlePanel = new JPanel(new GridLayout(2, 1, 0, 4));
        titlePanel.setOpaque(false);

        JLabel titleLabel = new JLabel("Personal Debt Portfolio Overview");
        titleLabel.setFont(UITheme.FONT_TITLE);
        titleLabel.setForeground(UITheme.TEXT_MAIN);

        JLabel subtitleLabel = new JLabel("Monitor active debt exposures, monthly commitments, and portfolio velocity");
        subtitleLabel.setFont(UITheme.FONT_REGULAR);
        subtitleLabel.setForeground(UITheme.TEXT_MUTED);

        titlePanel.add(titleLabel);
        titlePanel.add(subtitleLabel);

        // User Badge & Refresh Box
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPanel.setOpaque(false);

        accountBadgeLabel = new JLabel("User: None");
        accountBadgeLabel.setFont(UITheme.FONT_BOLD);
        accountBadgeLabel.setForeground(UITheme.TEXT_MAIN);
        accountBadgeLabel.setBackground(Color.WHITE);
        accountBadgeLabel.setOpaque(true);
        accountBadgeLabel.setBorder(new CompoundBorder(
                new LineBorder(UITheme.BORDER_COLOR, 1, true),
                new EmptyBorder(8, 14, 8, 14)
        ));

        JButton refreshBtn = UITheme.createStyledButton("Refresh", UITheme.BG_DARK, Color.WHITE);
        refreshBtn.addActionListener(e -> refreshData(selectionManager.getCurrentUser()));

        rightPanel.add(accountBadgeLabel);
        rightPanel.add(refreshBtn);

        topPanel.add(titlePanel, BorderLayout.WEST);
        topPanel.add(rightPanel, BorderLayout.EAST);

        // --- Center Content: KPI Cards + Portfolio Table ---
        JPanel contentPanel = new JPanel(new BorderLayout(0, 20));
        contentPanel.setOpaque(false);

        // 5 KPI Cards Grid
        JPanel kpiGrid = new JPanel(new GridLayout(1, 5, 14, 0));
        kpiGrid.setOpaque(false);

        totalDebtLabel = new JLabel("₹0.00");
        activeLoansLabel = new JLabel("0");
        totalEmiLabel = new JLabel("₹0.00");
        monthlyIncomeLabel = new JLabel("₹0.00");
        dtiRatioLabel = new JLabel("0.0%");
        dtiBadgeLabel = new JLabel("Healthy (< 36%)");

        kpiGrid.add(createKpiCard("TOTAL ACTIVE DEBT", totalDebtLabel, UITheme.ACCENT_RED, "Across all lenders"));
        kpiGrid.add(createKpiCard("ACTIVE LOANS COUNT", activeLoansLabel, UITheme.ACCENT_BLUE, "Outstanding accounts"));
        kpiGrid.add(createKpiCard("MONTHLY EMI COMMITMENT", totalEmiLabel, UITheme.ACCENT_ORANGE, "Base scheduled outflow"));
        kpiGrid.add(createKpiCard("MONTHLY INCOME", monthlyIncomeLabel, UITheme.ACCENT_GREEN, "Verified declared income"));
        kpiGrid.add(createKpiCard("DEBT-TO-INCOME (DTI)", dtiRatioLabel, new Color(0x2e, 0x7d, 0x32), dtiBadgeLabel));

        contentPanel.add(kpiGrid, BorderLayout.NORTH);

        // Active Loans Table Card
        JPanel tableCard = new JPanel(new BorderLayout(0, 10));
        tableCard.setBackground(UITheme.CARD_BG);
        tableCard.setBorder(UITheme.createCardBorder());

        JLabel tableTitle = new JLabel("Active Loans Breakdown");
        tableTitle.setFont(UITheme.FONT_HEADER);
        tableTitle.setForeground(UITheme.TEXT_MAIN);

        String[] columns = {"ID", "Loan Name", "Lender", "Category", "Principal (₹)", "Outstanding Balance (₹)", "Interest Rate", "Tenure (Mo)", "Est. Monthly EMI (₹)"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        portfolioTable = new JTable(tableModel);
        portfolioTable.setRowHeight(32);
        portfolioTable.setFont(UITheme.FONT_REGULAR);
        portfolioTable.getTableHeader().setFont(UITheme.FONT_BOLD);
        portfolioTable.getTableHeader().setBackground(new Color(241, 245, 249));
        portfolioTable.getTableHeader().setForeground(UITheme.TEXT_MAIN);
        portfolioTable.setGridColor(UITheme.BORDER_COLOR);
        portfolioTable.setSelectionBackground(new Color(219, 234, 254));

        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        portfolioTable.getColumnModel().getColumn(4).setCellRenderer(rightRenderer);
        portfolioTable.getColumnModel().getColumn(5).setCellRenderer(rightRenderer);
        portfolioTable.getColumnModel().getColumn(6).setCellRenderer(rightRenderer);
        portfolioTable.getColumnModel().getColumn(8).setCellRenderer(rightRenderer);

        JScrollPane scrollPane = new JScrollPane(portfolioTable);
        scrollPane.setBorder(new LineBorder(UITheme.BORDER_COLOR, 1));

        tableCard.add(tableTitle, BorderLayout.NORTH);
        tableCard.add(scrollPane, BorderLayout.CENTER);

        contentPanel.add(tableCard, BorderLayout.CENTER);

        add(topPanel, BorderLayout.NORTH);
        add(contentPanel, BorderLayout.CENTER);
    }

    private JPanel createKpiCard(String labelText, JLabel valueLabel, Color accentColor, String subtext) {
        JLabel footer = new JLabel(subtext);
        footer.setFont(UITheme.FONT_SMALL);
        footer.setForeground(UITheme.TEXT_MUTED);
        return createKpiCard(labelText, valueLabel, accentColor, footer);
    }

    private JPanel createKpiCard(String labelText, JLabel valueLabel, Color accentColor, JLabel footerLabel) {
        JPanel card = new JPanel(new BorderLayout(0, 6));
        card.setBackground(UITheme.CARD_BG);
        card.setBorder(UITheme.createCardBorder());

        JLabel header = new JLabel(labelText);
        header.setFont(UITheme.FONT_SMALL);
        header.setForeground(UITheme.TEXT_MUTED);

        valueLabel.setFont(UITheme.FONT_KPI_VAL);
        valueLabel.setForeground(accentColor);

        footerLabel.setFont(UITheme.FONT_SMALL);

        card.add(header, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        card.add(footerLabel, BorderLayout.SOUTH);

        return card;
    }

    public void refreshData(User user) {
        if (user == null) {
            accountBadgeLabel.setText("User: None");
            totalDebtLabel.setText("₹0.00");
            activeLoansLabel.setText("0");
            totalEmiLabel.setText("₹0.00");
            monthlyIncomeLabel.setText("₹0.00");
            dtiRatioLabel.setText("0.0%");
            dtiBadgeLabel.setText("—");
            dtiBadgeLabel.setForeground(UITheme.TEXT_MUTED);
            tableModel.setRowCount(0);
            return;
        }

        accountBadgeLabel.setText("User: " + user.getFullName());

        double totalDebt = loanService.getTotalDebt(user.getUserId());
        int activeCount = loanService.getActiveLoansCount(user.getUserId());
        double totalEMI = loanService.getTotalMonthlyEMI(user.getUserId());
        double income = user.getMonthlyIncome();

        totalDebtLabel.setText(UITheme.formatCurrency(totalDebt));
        activeLoansLabel.setText(String.valueOf(activeCount));
        totalEmiLabel.setText(UITheme.formatCurrency(totalEMI));
        monthlyIncomeLabel.setText(UITheme.formatCurrency(income));

        updateDtiMetric(totalEMI, income);

        tableModel.setRowCount(0);
        List<Loan> loans = loanService.getActiveLoansByUserId(user.getUserId());
        for (Loan l : loans) {
            double emi = EMICalculator.calculateEMI(
                    l.getOutstandingBalance(),
                    l.getAnnualInterestRate(),
                    l.getTenureMonths() > 0 ? l.getTenureMonths() : 12
            );
            tableModel.addRow(new Object[]{
                    l.getLoanId(),
                    l.getLoanName(),
                    l.getLenderName(),
                    l.getLoanType(),
                    UITheme.formatCurrency(l.getPrincipalAmount()),
                    UITheme.formatCurrency(l.getOutstandingBalance()),
                    String.format("%.2f%%", l.getAnnualInterestRate()),
                    l.getTenureMonths(),
                    UITheme.formatCurrency(emi)
            });
        }
    }

    private void updateDtiMetric(double totalEMI, double income) {
        if (income <= 0.0) {
            dtiRatioLabel.setText(totalEMI > 0 ? "100.0%" : "0.0%");
            Color criticalColor = new Color(0xd3, 0x2f, 0x2f); // #d32f2f
            Color healthyColor = new Color(0x2e, 0x7d, 0x32);  // #2e7d32
            dtiRatioLabel.setForeground(totalEMI > 0 ? criticalColor : healthyColor);
            dtiBadgeLabel.setText(totalEMI > 0 ? "Critical Exposure (> 50%)" : "Healthy (< 36%)");
            dtiBadgeLabel.setForeground(totalEMI > 0 ? criticalColor : healthyColor);
            return;
        }

        double dti = (totalEMI / income) * 100.0;
        dtiRatioLabel.setText(String.format("%.1f%%", dti));

        Color healthyColor = new Color(0x2e, 0x7d, 0x32);   // #2e7d32
        Color moderateColor = new Color(0xed, 0x6c, 0x02);  // #ed6c02
        Color criticalColor = new Color(0xd3, 0x2f, 0x2f);  // #d32f2f

        if (dti < 36.0) {
            dtiRatioLabel.setForeground(healthyColor);
            dtiBadgeLabel.setText("Healthy (< 36%)");
            dtiBadgeLabel.setForeground(healthyColor);
        } else if (dti <= 50.0) {
            dtiRatioLabel.setForeground(moderateColor);
            dtiBadgeLabel.setText("Moderate Risk (36% - 50%)");
            dtiBadgeLabel.setForeground(moderateColor);
        } else {
            dtiRatioLabel.setForeground(criticalColor);
            dtiBadgeLabel.setText("Critical Exposure (> 50%)");
            dtiBadgeLabel.setForeground(criticalColor);
        }
    }
}
