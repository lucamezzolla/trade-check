package it.lucamezzolla.tradecheck.ui;

import it.lucamezzolla.tradecheck.i18n.I18n;
import it.lucamezzolla.tradecheck.model.BrokerProfile;
import it.lucamezzolla.tradecheck.model.TradeCurrency;
import it.lucamezzolla.tradecheck.model.TradeSide;
import it.lucamezzolla.tradecheck.service.BreakEvenCalculator;
import it.lucamezzolla.tradecheck.service.CommissionBreakdown;
import it.lucamezzolla.tradecheck.service.CommissionCalculator;
import it.lucamezzolla.tradecheck.service.SettingsService;

import javax.swing.*;
import java.awt.*;

@SuppressWarnings("serial")
public final class CompletedTradePanel extends JPanel {
    private final JComboBox<TradeCurrency> currency = new JComboBox<>(TradeCurrency.values());
    private final JTextField averagePurchasePrice = new JTextField();
    private final JTextField quantity = new JTextField();
    private final JTextField sellCommissionOverride = new JTextField();
    private final JTextField[] expectedSellPrices = {
            new JTextField(),
            new JTextField(),
            new JTextField()
    };
    private final JTextField dividends = new JTextField("0");
    private final JTextArea result = new JTextArea(12, 72);
    private final SettingsService settingsService;
    private final CommissionCalculator commissionCalculator = new CommissionCalculator();
    private final BreakEvenCalculator breakEvenCalculator = new BreakEvenCalculator(commissionCalculator);
    private BrokerProfile activeProfile;

    public CompletedTradePanel(SettingsService settingsService) {
        this.settingsService = settingsService;
        this.activeProfile = settingsService.loadActiveProfile();

        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.add(createAggregatePositionNotice());
        form.add(Box.createVerticalStrut(8));
        form.add(UiSupport.row(I18n.text("field.currency"), currency));
        form.add(UiSupport.row(I18n.text("field.averagePurchasePrice"),
                I18n.text("field.averagePurchasePrice.tooltip"), averagePurchasePrice));
        form.add(UiSupport.row(I18n.text("field.quantity"), quantity));
        form.add(UiSupport.row(I18n.text("field.sellCommissionOverride"),
                I18n.text("field.sellCommissionOverride.tooltip"), sellCommissionOverride));
        form.add(UiSupport.row(I18n.text("field.expectedSellPrice1"), expectedSellPrices[0]));
        form.add(UiSupport.row(I18n.text("field.expectedSellPrice2"), expectedSellPrices[1]));
        form.add(UiSupport.row(I18n.text("field.expectedSellPrice3"), expectedSellPrices[2]));
        form.add(UiSupport.row(I18n.text("field.dividends"), dividends));

        JButton calculate = new JButton(I18n.text("button.calculate"));
        calculate.addActionListener(e -> calculate());
        JButton clear = new JButton(I18n.text("button.clear"));
        clear.addActionListener(e -> clear());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(calculate);
        buttons.add(clear);

        JPanel left = new JPanel(new BorderLayout(8, 8));
        left.add(form, BorderLayout.NORTH);
        left.add(buttons, BorderLayout.SOUTH);

        result.setEditable(false);
        result.setLineWrap(true);
        result.setWrapStyleWord(true);
        result.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        result.setBorder(BorderFactory.createTitledBorder(I18n.text("section.result")));

        add(left, BorderLayout.CENTER);
        add(new JScrollPane(result), BorderLayout.EAST);

        applyProfile(activeProfile);
    }

    private JPanel createAggregatePositionNotice() {
        JTextArea note = new JTextArea(I18n.text("trade.positionNote"));
        note.setEditable(false);
        note.setFocusable(false);
        note.setLineWrap(true);
        note.setWrapStyleWord(true);
        note.setOpaque(false);
        note.setFont(UIManager.getFont("Label.font"));
        note.setRows(4);

        JButton showExample = new JButton(
                "<html><u>" + I18n.text("trade.showExample") + "</u></html>"
        );
        showExample.setBorder(BorderFactory.createEmptyBorder());
        showExample.setBorderPainted(false);
        showExample.setContentAreaFilled(false);
        showExample.setFocusPainted(false);
        showExample.setOpaque(false);
        showExample.setForeground(new Color(0, 102, 204));
        showExample.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        showExample.setHorizontalAlignment(SwingConstants.LEFT);
        showExample.setToolTipText(I18n.text("trade.showExample.tooltip"));
        showExample.addActionListener(e -> populateExample());

        JPanel exampleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        exampleRow.setOpaque(false);
        exampleRow.add(showExample);

        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(I18n.text("trade.positionModel.title")),
                BorderFactory.createEmptyBorder(4, 8, 6, 8)
        ));
        panel.add(note, BorderLayout.CENTER);
        panel.add(exampleRow, BorderLayout.SOUTH);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));
        return panel;
    }

    private void populateExample() {
        activeProfile = settingsService.loadActiveProfile();
        currency.setSelectedItem(activeProfile.currency());
        averagePurchasePrice.setText("100");
        quantity.setText("10");
        sellCommissionOverride.setText("");
        expectedSellPrices[0].setText("98");
        expectedSellPrices[1].setText("105");
        expectedSellPrices[2].setText("115");
        dividends.setText("0");
        result.setText("");
        averagePurchasePrice.requestFocusInWindow();
    }

    public void applyProfile(BrokerProfile profile) {
        this.activeProfile = profile;
        currency.setSelectedItem(profile.currency());
        averagePurchasePrice.setText("");
        sellCommissionOverride.setText("");
        result.setText("");
    }

    private void calculate() {
        try {
            double averagePrice = UiSupport.number(averagePurchasePrice);
            int q = UiSupport.integer(quantity);
            Double sellOverride = UiSupport.optionalNumber(sellCommissionOverride);
            double dividendValue = UiSupport.number(dividends);
            TradeCurrency selectedCurrency = (TradeCurrency) currency.getSelectedItem();

            double[] sellPrices = new double[expectedSellPrices.length];
            int scenarioCount = 0;
            for (int i = 0; i < expectedSellPrices.length; i++) {
                sellPrices[i] = UiSupport.number(expectedSellPrices[i]);
                if (sellPrices[i] > 0) {
                    scenarioCount++;
                }
            }

            activeProfile = settingsService.loadActiveProfile();
            validateCurrency(selectedCurrency);
            if (averagePrice <= 0 || q <= 0 || dividendValue < 0 || scenarioCount == 0
                    || (sellOverride != null && sellOverride < 0)) {
                throw new NumberFormatException();
            }

            double totalPurchaseCost = averagePrice * q;
            double breakEven = breakEvenCalculator.calculate(
                    activeProfile,
                    averagePrice,
                    q,
                    sellOverride,
                    dividendValue
            );

            StringBuilder out = new StringBuilder();
            out.append(I18n.text("result.profile")).append(": ").append(activeProfile.name()).append("\n");
            out.append(I18n.text("result.currency")).append(": ").append(selectedCurrency.code()).append("\n");
            appendAccuracy(out, activeProfile);
            out.append("\n");
            out.append(I18n.text("result.averagePrice")).append(": ")
                    .append(UiSupport.price(averagePrice, selectedCurrency)).append("\n");
            out.append(I18n.text("result.positionCost")).append(": ")
                    .append(UiSupport.money(totalPurchaseCost, selectedCurrency)).append("\n");
            out.append(I18n.text("result.purchaseCostsIncluded")).append("\n");
            out.append(I18n.text("result.breakEven")).append(": ")
                    .append(UiSupport.price(breakEven, selectedCurrency)).append("\n");

            int visibleScenario = 0;
            for (double sellPrice : sellPrices) {
                if (sellPrice <= 0) {
                    continue;
                }
                visibleScenario++;
                CommissionBreakdown calculatedSell = commissionCalculator
                        .calculate(activeProfile, sellPrice, q, TradeSide.SELL);
                appendScenario(out, visibleScenario, sellPrice, q,
                        calculatedSell, sellOverride, dividendValue,
                        totalPurchaseCost, selectedCurrency, activeProfile.taxRate());
            }

            result.setText(out.toString());
            result.setCaretPosition(0);
        } catch (CurrencyMismatchException ex) {
            JOptionPane.showMessageDialog(this, I18n.text("error.currencyMismatch"),
                    I18n.text("error.title"), JOptionPane.ERROR_MESSAGE);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, I18n.text("error.invalidNumber"),
                    I18n.text("error.title"), JOptionPane.ERROR_MESSAGE);
        }
    }

    private void appendScenario(
            StringBuilder out,
            int scenarioNumber,
            double sellPrice,
            int quantity,
            CommissionBreakdown calculatedSell,
            Double sellOverride,
            double dividendValue,
            double totalPurchaseCost,
            TradeCurrency currency,
            double taxRate
    ) {
        double effectiveSellFee = sellOverride != null ? sellOverride : calculatedSell.total();
        double saleValue = sellPrice * quantity;
        double grossProfit = saleValue - totalPurchaseCost;
        double afterCommissions = grossProfit - effectiveSellFee + dividendValue;
        double tax = afterCommissions > 0 ? afterCommissions * taxRate / 100.0 : 0.0;
        double netProfit = afterCommissions - tax;

        out.append("\n").append(UiSupport.separator()).append("\n");
        out.append(I18n.text("result.scenario")).append(" ").append(scenarioNumber)
                .append(" — ").append(I18n.text("field.expectedSellPrice"))
                .append(": ").append(UiSupport.price(sellPrice, currency)).append("\n");
        out.append(UiSupport.separator()).append("\n");
        out.append(I18n.text("result.saleValue")).append(": ").append(UiSupport.money(saleValue, currency)).append("\n");
        out.append(I18n.text("result.grossProfit")).append(": ").append(UiSupport.money(grossProfit, currency)).append("\n");
        appendEffectiveCommission(out, I18n.text("result.sellCommission"), calculatedSell,
                sellOverride, currency);
        if (dividendValue > 0) {
            out.append(I18n.text("result.dividendsIncluded")).append(": ")
                    .append(UiSupport.money(dividendValue, currency)).append("\n");
        }
        out.append(I18n.text("result.afterCommissions")).append(": ").append(UiSupport.money(afterCommissions, currency)).append("\n");
        out.append(I18n.text("result.tax")).append(": ").append(UiSupport.money(tax, currency)).append("\n");
        out.append(I18n.text("result.netProfit")).append(": ").append(UiSupport.money(netProfit, currency)).append("\n");
    }

    private void appendEffectiveCommission(
            StringBuilder out,
            String label,
            CommissionBreakdown calculated,
            Double override,
            TradeCurrency currency
    ) {
        double effective = override != null ? override : calculated.total();
        out.append(label).append(": ").append(UiSupport.money(effective, currency))
                .append(" [").append(I18n.text(override != null
                        ? "result.commissionSource.actual"
                        : "result.commissionSource.calculated")).append("]");
        if (override == null && calculated.knownExternalFees() > 0) {
            out.append(" (").append(I18n.text("result.brokerCommission")).append(" ")
                    .append(UiSupport.money(calculated.brokerCommission(), currency))
                    .append(" + ").append(I18n.text("result.knownExternalFees")).append(" ")
                    .append(UiSupport.money(calculated.knownExternalFees(), currency)).append(")");
        }
        out.append("\n");
    }

    private void appendAccuracy(StringBuilder out, BrokerProfile profile) {
        out.append(I18n.text("result.commissionAccuracy")).append(": ")
                .append(I18n.text(profile.variableExternalFees()
                        ? "result.commissionAccuracy.estimated"
                        : "result.commissionAccuracy.published"))
                .append("\n");
    }

    private void validateCurrency(TradeCurrency selectedCurrency) {
        if (selectedCurrency == null || selectedCurrency != activeProfile.currency()) {
            throw new CurrencyMismatchException();
        }
    }

    private void clear() {
        averagePurchasePrice.setText("");
        quantity.setText("");
        sellCommissionOverride.setText("");
        for (JTextField expectedSellPrice : expectedSellPrices) {
            expectedSellPrice.setText("");
        }
        dividends.setText("0");
        result.setText("");
    }

    private static final class CurrencyMismatchException extends RuntimeException {
    }
}
