package ui;

import model.AuditLog;
import service.LoanService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class AuditPanel extends JPanel {

    private final LoanService loanService;
    private JTable auditTable;
    private DefaultTableModel tableModel;
    private JLabel countLabel;

    private static final DateTimeFormatter TS_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public AuditPanel(LoanService loanService) {
        this.loanService = loanService;

        setLayout(new BorderLayout(16, 16));
        setBackground(UITheme.BG_LIGHT);
        setBorder(new EmptyBorder(20, 24, 20, 24));

        initComponents();
        refreshAuditLogs();
    }

    private void initComponents() {
        // Top Bar
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);

        JPanel titlePanel = new JPanel(new GridLayout(2, 1, 0, 4));
        titlePanel.setOpaque(false);

        JLabel title = new JLabel("System Audit & Transaction Ledger");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_MAIN);

        JLabel subtitle = new JLabel("Immutable ledger tracking automatic database triggers, rate adjustments, repayments, and prepayments");
        subtitle.setFont(UITheme.FONT_REGULAR);
        subtitle.setForeground(UITheme.TEXT_MUTED);

        titlePanel.add(title);
        titlePanel.add(subtitle);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionPanel.setOpaque(false);

        JButton refreshBtn = UITheme.createStyledButton("Refresh Ledger", UITheme.BG_DARK, Color.WHITE);
        refreshBtn.addActionListener(e -> refreshAuditLogs());

        actionPanel.add(refreshBtn);

        topBar.add(titlePanel, BorderLayout.WEST);
        topBar.add(actionPanel, BorderLayout.EAST);

        // Center Table Card
        JPanel tableCard = new JPanel(new BorderLayout(0, 10));
        tableCard.setBackground(UITheme.CARD_BG);
        tableCard.setBorder(UITheme.createCardBorder());

        String[] cols = {"Log ID", "Loan ID", "Action Type", "Action Details", "Timestamp"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        auditTable = new JTable(tableModel);
        auditTable.setRowHeight(32);
        auditTable.setFont(UITheme.FONT_REGULAR);
        auditTable.getTableHeader().setFont(UITheme.FONT_BOLD);
        auditTable.getTableHeader().setBackground(new Color(241, 245, 249));
        auditTable.setSelectionBackground(new Color(219, 234, 254));
        auditTable.setGridColor(UITheme.BORDER_COLOR);

        // Column widths
        auditTable.getColumnModel().getColumn(0).setMaxWidth(80);
        auditTable.getColumnModel().getColumn(1).setMaxWidth(90);
        auditTable.getColumnModel().getColumn(2).setPreferredWidth(180);
        auditTable.getColumnModel().getColumn(3).setPreferredWidth(450);
        auditTable.getColumnModel().getColumn(4).setPreferredWidth(160);

        // Custom Cell Renderer for Action Type
        DefaultTableCellRenderer actionTypeRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected && value != null) {
                    String action = value.toString();
                    if (action.contains("PREPAYMENT")) {
                        c.setForeground(UITheme.ACCENT_GREEN);
                        setFont(UITheme.FONT_BOLD);
                    } else if (action.contains("REPAYMENT")) {
                        c.setForeground(UITheme.ACCENT_BLUE);
                        setFont(UITheme.FONT_BOLD);
                    } else if (action.contains("DELETE")) {
                        c.setForeground(UITheme.ACCENT_RED);
                        setFont(UITheme.FONT_BOLD);
                    } else {
                        c.setForeground(UITheme.TEXT_MAIN);
                        setFont(UITheme.FONT_REGULAR);
                    }
                }
                return c;
            }
        };
        auditTable.getColumnModel().getColumn(2).setCellRenderer(actionTypeRenderer);

        JScrollPane scrollPane = new JScrollPane(auditTable);
        scrollPane.setBorder(new LineBorder(UITheme.BORDER_COLOR, 1));

        countLabel = new JLabel("Showing recent audit transactions");
        countLabel.setFont(UITheme.FONT_SMALL);
        countLabel.setForeground(UITheme.TEXT_MUTED);
        countLabel.setBorder(new EmptyBorder(6, 4, 4, 4));

        tableCard.add(scrollPane, BorderLayout.CENTER);
        tableCard.add(countLabel, BorderLayout.SOUTH);

        add(topBar, BorderLayout.NORTH);
        add(tableCard, BorderLayout.CENTER);
    }

    public void refreshAuditLogs() {
        tableModel.setRowCount(0);
        List<AuditLog> logs = loanService.getRecentAuditLogs(50);

        for (AuditLog log : logs) {
            tableModel.addRow(new Object[]{
                    "#" + log.getLogId(),
                    log.getLoanId() != null ? "Loan #" + log.getLoanId() : "—",
                    log.getActionType(),
                    log.getActionDetails(),
                    log.getPerformedAt() != null ? log.getPerformedAt().format(TS_FORMATTER) : "—"
            });
        }

        countLabel.setText("Displaying " + logs.size() + " most recent actions from database audit ledger.");
    }
}
