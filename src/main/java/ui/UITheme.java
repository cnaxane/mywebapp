package ui;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.text.DecimalFormat;

public class UITheme {

    public static final Color BG_DARK = new Color(15, 23, 42); // Navy 900
    public static final Color BG_LIGHT = new Color(248, 250, 252); // Slate 50
    public static final Color CARD_BG = Color.WHITE;
    public static final Color TEXT_MAIN = new Color(30, 41, 59); // Slate 800
    public static final Color TEXT_MUTED = new Color(100, 116, 139); // Slate 500
    public static final Color ACCENT_BLUE = new Color(37, 99, 235); // Blue 600
    public static final Color ACCENT_GREEN = new Color(16, 149, 106); // Emerald
    public static final Color ACCENT_ORANGE = new Color(234, 88, 12); // Orange 600
    public static final Color ACCENT_RED = new Color(225, 29, 72); // Rose 600
    public static final Color BORDER_COLOR = new Color(226, 232, 240); // Slate 200

    public static final Font FONT_TITLE = new Font(Font.SANS_SERIF, Font.BOLD, 22);
    public static final Font FONT_HEADER = new Font(Font.SANS_SERIF, Font.BOLD, 16);
    public static final Font FONT_TAB = new Font(Font.SANS_SERIF, Font.BOLD, 14);
    public static final Font FONT_BOLD = new Font(Font.SANS_SERIF, Font.BOLD, 13);
    public static final Font FONT_REGULAR = new Font(Font.SANS_SERIF, Font.PLAIN, 13);
    public static final Font FONT_SMALL = new Font(Font.SANS_SERIF, Font.PLAIN, 11);
    public static final Font FONT_KPI_VAL = new Font(Font.SANS_SERIF, Font.BOLD, 20);

    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("₹#,##,##0.00");
    private static final DecimalFormat INTEGER_FORMAT = new DecimalFormat("#,##0");

    public static String formatCurrency(double amount) {
        return CURRENCY_FORMAT.format(amount);
    }

    public static String formatInteger(long value) {
        return INTEGER_FORMAT.format(value);
    }

    public static Border createCardBorder() {
        return new CompoundBorder(
                new LineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(16, 18, 16, 18)
        );
    }

    public static JButton createStyledButton(String text, Color background, Color foreground) {
        JButton button = new JButton(text);
        button.setBackground(background);
        button.setForeground(foreground);
        button.setFocusPainted(false);
        button.setFont(FONT_BOLD);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.putClientProperty("JButton.buttonType", "roundRect");
        button.setBorder(new CompoundBorder(
                new LineBorder(background.darker(), 1, true),
                new EmptyBorder(8, 16, 8, 16)
        ));
        button.setOpaque(true);
        return button;
    }
}
