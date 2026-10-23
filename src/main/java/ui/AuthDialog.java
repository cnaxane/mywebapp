package ui;

import model.User;
import service.LoanService;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class AuthDialog extends JDialog {

    private final LoanService loanService;
    private User authenticatedUser;

    // Sign In Components
    private JTextField loginEmailField;
    private JPasswordField loginPasswordField;
    private JLabel loginErrorLabel;

    // Register Components
    private JTextField regNameField;
    private JTextField regEmailField;
    private JTextField regIncomeField;
    private JPasswordField regPasswordField;
    private JLabel regErrorLabel;

    public AuthDialog(Frame parent) {
        this(parent, new LoanService());
    }

    public AuthDialog(Frame parent, LoanService loanService) {
        super(parent, "LoanWise — Account Access", true);
        this.loanService = loanService;

        setModal(true);
        initComponents();

        setSize(480, 560);
        setResizable(false);
        setLocationRelativeTo(parent);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // If closing without authentication, authenticatedUser remains null
                dispose();
            }
        });
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        getContentPane().setBackground(UITheme.BG_LIGHT);

        // Header Banner
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(UITheme.BG_DARK);
        headerPanel.setBorder(new EmptyBorder(20, 24, 20, 24));

        JLabel titleLabel = new JLabel("LoanWise");
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
        titleLabel.setForeground(Color.WHITE);

        JLabel subLabel = new JLabel("Secure personal loan optimization & debt acceleration");
        subLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        subLabel.setForeground(new Color(148, 163, 184));

        headerPanel.add(titleLabel, BorderLayout.NORTH);
        headerPanel.add(subLabel, BorderLayout.SOUTH);

        // Tabbed Panel
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(UITheme.FONT_TAB);
        tabbedPane.setBackground(Color.WHITE);

        tabbedPane.addTab("  Sign In  ", createSignInPanel());
        tabbedPane.addTab("  Register Account  ", createRegisterPanel());

        add(headerPanel, BorderLayout.NORTH);
        add(tabbedPane, BorderLayout.CENTER);
    }

    private JPanel createSignInPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UITheme.CARD_BG);
        panel.setBorder(new EmptyBorder(24, 30, 24, 30));

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setOpaque(false);

        JLabel emailLabel = new JLabel("Email Address:");
        emailLabel.setFont(UITheme.FONT_BOLD);
        emailLabel.setForeground(UITheme.TEXT_MAIN);
        emailLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        loginEmailField = new JTextField("aarav@example.com");
        loginEmailField.setFont(UITheme.FONT_REGULAR);
        loginEmailField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        loginEmailField.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel passLabel = new JLabel("Password:");
        passLabel.setFont(UITheme.FONT_BOLD);
        passLabel.setForeground(UITheme.TEXT_MAIN);
        passLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        loginPasswordField = new JPasswordField("Password@123");
        loginPasswordField.setFont(UITheme.FONT_REGULAR);
        loginPasswordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        loginPasswordField.setAlignmentX(Component.LEFT_ALIGNMENT);

        loginErrorLabel = new JLabel(" ");
        loginErrorLabel.setFont(UITheme.FONT_SMALL);
        loginErrorLabel.setForeground(UITheme.ACCENT_RED);
        loginErrorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton signInBtn = UITheme.createStyledButton("Sign In", UITheme.ACCENT_BLUE, Color.WHITE);
        signInBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        signInBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        signInBtn.addActionListener(e -> handleSignIn());

        // Press Enter to submit
        loginPasswordField.addActionListener(e -> handleSignIn());
        loginEmailField.addActionListener(e -> handleSignIn());

        // Demo credentials hint card
        JPanel demoCard = new JPanel(new BorderLayout(0, 4));
        demoCard.setBackground(new Color(241, 245, 249));
        demoCard.setBorder(new CompoundBorder(
                new LineBorder(UITheme.BORDER_COLOR, 1, true),
                new EmptyBorder(10, 12, 10, 12)
        ));
        demoCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel demoTitle = new JLabel("Demo Accounts (Password: Password@123):");
        demoTitle.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        demoTitle.setForeground(UITheme.TEXT_MAIN);

        JLabel demoHint = new JLabel("<html>• Aarav: aarav@example.com<br>• Diya: diya@example.com<br>• Kabir: kabir@example.com</html>");
        demoHint.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        demoHint.setForeground(UITheme.TEXT_MUTED);

        demoCard.add(demoTitle, BorderLayout.NORTH);
        demoCard.add(demoHint, BorderLayout.CENTER);

        form.add(emailLabel);
        form.add(Box.createVerticalStrut(6));
        form.add(loginEmailField);
        form.add(Box.createVerticalStrut(14));
        form.add(passLabel);
        form.add(Box.createVerticalStrut(6));
        form.add(loginPasswordField);
        form.add(Box.createVerticalStrut(8));
        form.add(loginErrorLabel);
        form.add(Box.createVerticalStrut(14));
        form.add(signInBtn);
        form.add(Box.createVerticalStrut(20));
        form.add(demoCard);

        panel.add(form, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createRegisterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UITheme.CARD_BG);
        panel.setBorder(new EmptyBorder(20, 30, 20, 30));

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setOpaque(false);

        JLabel nameLabel = new JLabel("Full Name:");
        nameLabel.setFont(UITheme.FONT_BOLD);
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        regNameField = new JTextField();
        regNameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        regNameField.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel emailLabel = new JLabel("Email Address:");
        emailLabel.setFont(UITheme.FONT_BOLD);
        emailLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        regEmailField = new JTextField();
        regEmailField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        regEmailField.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel incomeLabel = new JLabel("Monthly Net Income (₹):");
        incomeLabel.setFont(UITheme.FONT_BOLD);
        incomeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        regIncomeField = new JTextField("75000");
        regIncomeField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        regIncomeField.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel passLabel = new JLabel("Password (min 6 characters):");
        passLabel.setFont(UITheme.FONT_BOLD);
        passLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        regPasswordField = new JPasswordField();
        regPasswordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        regPasswordField.setAlignmentX(Component.LEFT_ALIGNMENT);

        regErrorLabel = new JLabel(" ");
        regErrorLabel.setFont(UITheme.FONT_SMALL);
        regErrorLabel.setForeground(UITheme.ACCENT_RED);
        regErrorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton regBtn = UITheme.createStyledButton("Register Account", UITheme.ACCENT_GREEN, Color.WHITE);
        regBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        regBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        regBtn.addActionListener(e -> handleRegister());

        form.add(nameLabel);
        form.add(Box.createVerticalStrut(4));
        form.add(regNameField);
        form.add(Box.createVerticalStrut(10));
        form.add(emailLabel);
        form.add(Box.createVerticalStrut(4));
        form.add(regEmailField);
        form.add(Box.createVerticalStrut(10));
        form.add(incomeLabel);
        form.add(Box.createVerticalStrut(4));
        form.add(regIncomeField);
        form.add(Box.createVerticalStrut(10));
        form.add(passLabel);
        form.add(Box.createVerticalStrut(4));
        form.add(regPasswordField);
        form.add(Box.createVerticalStrut(6));
        form.add(regErrorLabel);
        form.add(Box.createVerticalStrut(12));
        form.add(regBtn);

        panel.add(form, BorderLayout.CENTER);
        return panel;
    }

    private void handleSignIn() {
        String email = loginEmailField.getText().trim();
        String password = new String(loginPasswordField.getPassword());

        loginErrorLabel.setText(" ");

        if (email.isEmpty() || password.isEmpty()) {
            loginErrorLabel.setText("Please enter both email and password.");
            return;
        }

        User user = loanService.authenticateUser(email, password);
        if (user != null) {
            this.authenticatedUser = user;
            dispose();
        } else {
            loginErrorLabel.setText("Invalid email or password. Please try again.");
        }
    }

    private void handleRegister() {
        String name = regNameField.getText().trim();
        String email = regEmailField.getText().trim();
        String incomeStr = regIncomeField.getText().trim();
        String password = new String(regPasswordField.getPassword());

        regErrorLabel.setText(" ");

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            regErrorLabel.setText("Please fill in all required fields.");
            return;
        }

        double income;
        try {
            income = Double.parseDouble(incomeStr);
            if (income < 0) {
                regErrorLabel.setText("Monthly income cannot be negative.");
                return;
            }
        } catch (NumberFormatException e) {
            regErrorLabel.setText("Please enter a valid numerical monthly income.");
            return;
        }

        try {
            User newUser = loanService.registerUser(name, email, income, password);
            if (newUser != null) {
                JOptionPane.showMessageDialog(this,
                        "Registration successful!\nWelcome to LoanWise, " + newUser.getFullName() + "!",
                        "Account Created",
                        JOptionPane.INFORMATION_MESSAGE);
                this.authenticatedUser = newUser;
                dispose();
            } else {
                regErrorLabel.setText("Registration failed. Please try again.");
            }
        } catch (IllegalArgumentException ex) {
            regErrorLabel.setText(ex.getMessage());
        }
    }

    public User getAuthenticatedUser() {
        return authenticatedUser;
    }
}
