package it.lucamezzolla.tradecheck.ui;

import it.lucamezzolla.tradecheck.i18n.I18n;
import it.lucamezzolla.tradecheck.model.TradeCurrency;

import javax.swing.*;
import java.awt.*;
import java.text.NumberFormat;

final class UiSupport {
    private UiSupport() {}

    static JPanel row(String label, JComponent component) {
        return row(label, null, component);
    }

    static JPanel row(String label, String tooltip, JComponent component) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        JLabel jLabel = new JLabel(label);
        if (tooltip != null && !tooltip.isBlank()) {
            jLabel.setToolTipText(tooltip);
            component.setToolTipText(tooltip);
        }
        jLabel.setPreferredSize(new Dimension(315, 32));
        row.add(jLabel, BorderLayout.WEST);
        row.add(component, BorderLayout.CENTER);
        row.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        return row;
    }

    static double number(JTextField field) {
        String text = field.getText().trim().replace(',', '.');
        return text.isEmpty() ? 0.0 : Double.parseDouble(text);
    }

    static Double optionalNumber(JTextField field) {
        String text = field.getText().trim().replace(',', '.');
        return text.isEmpty() ? null : Double.parseDouble(text);
    }

    static int integer(JTextField field) {
        String text = field.getText().trim();
        return text.isEmpty() ? 0 : Integer.parseInt(text);
    }

    static String money(double value, TradeCurrency currency) {
        NumberFormat format = NumberFormat.getNumberInstance(I18n.getLocale());
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);
        return currency.symbol() + " " + format.format(value);
    }

    static String price(double value, TradeCurrency currency) {
        return currency.symbol() + " " + decimal(value, 4);
    }

    static String decimal(double value, int fractionDigits) {
        NumberFormat format = NumberFormat.getNumberInstance(I18n.getLocale());
        format.setMinimumFractionDigits(fractionDigits);
        format.setMaximumFractionDigits(fractionDigits);
        return format.format(value);
    }

    static String editableNumber(double value) {
        if (value == Math.rint(value)) {
            return String.format("%.0f", value);
        }
        return Double.toString(value);
    }

    static String separator() {
        return "────────────────────────────────────────────";
    }
}
