package it.lucamezzolla.tradecheck;

import it.lucamezzolla.tradecheck.i18n.I18n;
import it.lucamezzolla.tradecheck.ui.MainFrame;

import javax.swing.*;

public final class TradeCheckApplication {
    private TradeCheckApplication() {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            I18n.initialize();
            new MainFrame().setVisible(true);
        });
    }
}
