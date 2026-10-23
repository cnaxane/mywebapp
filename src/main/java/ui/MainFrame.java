package ui;

import com.formdev.flatlaf.FlatLightLaf;
import db.DBConnection;
import model.User;
import service.LoanService;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class MainFrame extends JFrame implements DashboardPanel.UserSelectionManager {

    private final LoanService loanService;
    private final List<UserChangeListener> listeners = new ArrayList<>();
    private User currentUser;

    private JLabel connectionStatusLabel;
    private JLabel currentUserBadge;

    private DashboardPanel dashboardPanel;
    private LoanPanel loanPanel;
    private OptimizerPanel optimizerPanel;
    private SchedulePanel schedulePanel;
    private AuditPanel auditPanel;

    public MainFrame() {
        this(null);
    }

    public MainFrame(User authenticatedUser) {
        super("LoanWise — Loan Prepayment and Payoff Optimizer");

        this.loanService = new LoanService();
        this.currentUser = authenticatedUser;

        // Ensure database schema and seed data are initialized
        loanService.initializeDatabaseSchema();

        initComponents();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1320, 840);
        setMinimumSize(new Dimension(1080, 720));
        setLocationRelativeTo(null);

        if (authenticatedUser != null) {
            selectUser(authenticatedUser);
        }
    }

    private void initComponents() {
        setLayout(new BorderLayout());

        // --- Top Header Bar ---
        JPanel topHeader = new JPanel(new BorderLayout());
        topHeader.setBackground(UITheme.BG_DARK);
        topHeader.setBorder(new EmptyBorder(16, 24, 16, 24));

        JPanel brandPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        brandPanel.setOpaque(false);

        JLabel logoLabel = new JLabel("LoanWise");
        logoLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        logoLabel.setForeground(Color.WHITE);

        JLabel tagLabel = new JLabel("|  Personal Debt Prepayment & Payoff Acceleration Engine");
        tagLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        tagLabel.setForeground(new Color(148, 163, 184));

        brandPanel.add(logoLabel);
        brandPanel.add(tagLabel);

        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 0));
        statusPanel.setOpaque(false);

        boolean isDbConnected = DBConnection.getInstance().isConnected();
        connectionStatusLabel = new JLabel(isDbConnected ? "[DB Connected]" : "[DB Offline]");
        connectionStatusLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        connectionStatusLabel.setForeground(isDbConnected ? new Color(52, 211, 153) : new Color(248, 113, 113));

        currentUserBadge = new JLabel("User: None");
        currentUserBadge.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        currentUserBadge.setForeground(Color.WHITE);
        currentUserBadge.setBorder(new CompoundBorder(
                new LineBorder(new Color(51, 65, 85), 1, true),
                new EmptyBorder(6, 12, 6, 12)
        ));

        JButton signOutBtn = UITheme.createStyledButton("Sign Out", UITheme.ACCENT_RED, Color.WHITE);
        signOutBtn.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        signOutBtn.addActionListener(e -> handleSignOut());

        statusPanel.add(connectionStatusLabel);
        statusPanel.add(currentUserBadge);
        statusPanel.add(signOutBtn);

        topHeader.add(brandPanel, BorderLayout.WEST);
        topHeader.add(statusPanel, BorderLayout.EAST);

        // --- Navigation Tabs ---
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(UITheme.FONT_TAB);
        tabbedPane.setBackground(Color.WHITE);

        // Comfortable padding for tabs
        UIManager.put("TabbedPane.tabInsets", new Insets(10, 20, 10, 20));
        UIManager.put("TabbedPane.selectedBackground", Color.WHITE);
        UIManager.put("TabbedPane.hoverColor", new Color(241, 245, 249));

        dashboardPanel = new DashboardPanel(loanService, this);
        loanPanel = new LoanPanel(loanService, this);
        optimizerPanel = new OptimizerPanel(loanService, this);
        schedulePanel = new SchedulePanel(loanService, this);
        auditPanel = new AuditPanel(loanService);

        tabbedPane.addTab("  Dashboard  ", dashboardPanel);
        tabbedPane.addTab("  My Loans  ", loanPanel);
        tabbedPane.addTab("  Payoff Optimizer  ", optimizerPanel);
        tabbedPane.addTab("  Amortization  ", schedulePanel);
        tabbedPane.addTab("  Audit Ledger  ", auditPanel);

        // Tab selection listener to keep active views synchronized
        tabbedPane.addChangeListener(e -> {
            int selectedIndex = tabbedPane.getSelectedIndex();
            switch (selectedIndex) {
                case 0 -> dashboardPanel.refreshData(currentUser);
                case 1 -> loanPanel.refreshLoans();
                case 2 -> optimizerPanel.updatePriorityTablePreview();
                case 3 -> schedulePanel.populateLoanSelector(currentUser);
                case 4 -> auditPanel.refreshAuditLogs();
            }
        });

        add(topHeader, BorderLayout.NORTH);
        add(tabbedPane, BorderLayout.CENTER);
    }

    private void handleSignOut() {
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to sign out of your account?",
                "Confirm Sign Out",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            this.dispose();
            showAuthAndLaunch();
        }
    }

    /**
     * Prompts the user with the modal AuthDialog before constructing or displaying MainFrame.
     */
    public static void showAuthAndLaunch() {
        AuthDialog authDialog = new AuthDialog(null);
        authDialog.setModal(true);
        authDialog.setVisible(true);

        User authUser = authDialog.getAuthenticatedUser();
        if (authUser == null) {
            System.exit(0);
        } else {
            MainFrame mainFrame = new MainFrame(authUser);
            mainFrame.setVisible(true);
        }
    }

    @Override
    public User getCurrentUser() {
        return currentUser;
    }

    @Override
    public void selectUser(User user) {
        this.currentUser = user;
        if (user != null) {
            currentUserBadge.setText("User: " + user.getFullName());
        } else {
            currentUserBadge.setText("User: None");
        }

        for (UserChangeListener listener : listeners) {
            listener.onUserChanged(user);
        }
    }

    @Override
    public void registerListener(UserChangeListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public static void main(String[] args) {
        // Set up FlatLaf Modern Look & Feel
        try {
            FlatLightLaf.setup();
            UIManager.put("TabbedPane.tabInsets", new Insets(10, 20, 10, 20));
            UIManager.put("Table.rowHeight", 30);
            UIManager.put("TableHeader.height", 32);
        } catch (Exception e) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
        }

        SwingUtilities.invokeLater(MainFrame::showAuthAndLaunch);
    }
}
