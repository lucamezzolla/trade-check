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
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;

@SuppressWarnings("serial")
public final class CompletedTradePanel extends JPanel {
    private static final double ROUNDING_TOLERANCE = 0.0000001;

    private final JComboBox<TradeCurrency> currency = new JComboBox<>(TradeCurrency.values());
    private final JTextField executionPrice = new JTextField();
    private final JTextField averagePurchasePrice = new JTextField();
    private final JTextField quantity = new JTextField();
    private final JTextField calculatedBuyCommission = new JTextField();
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

        calculatedBuyCommission.setEditable(false);
        calculatedBuyCommission.setBackground(new Color(238, 238, 238));
        calculatedBuyCommission.setHorizontalAlignment(SwingConstants.RIGHT);

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.add(UiSupport.row(I18n.text("field.currency"), currency));
        form.add(UiSupport.row(I18n.text("field.executionPrice"), I18n.text("field.executionPrice.tooltip"), executionPrice));
        form.add(UiSupport.row(I18n.text("field.averagePurchasePrice"),
                I18n.text("field.averagePurchasePrice.tooltip"), averagePurchasePrice));
        form.add(UiSupport.row(I18n.text("field.quantity"), quantity));
        form.add(UiSupport.row(I18n.text("field.buyCommissionCalculated"),
                I18n.text("field.buyCommissionCalculated.tooltip"), calculatedBuyCommission));
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

        installBuyCommissionAutoCalculation();
        applyProfile(activeProfile);
    }

    public void applyProfile(BrokerProfile profile) {
        this.activeProfile = profile;
        currency.setSelectedItem(profile.currency());
        averagePurchasePrice.setText("");
        calculatedBuyCommission.setText("");
        sellCommissionOverride.setText("");
        result.setText("");
        updateCalculatedBuyCommission();
    }

    private void calculate() {
        try {
            double execution = UiSupport.number(executionPrice);
            Double brokerAveragePrice = UiSupport.optionalNumber(averagePurchasePrice);
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
            if (execution <= 0 || q <= 0 || dividendValue < 0 || scenarioCount == 0
                    || (brokerAveragePrice != null && brokerAveragePrice <= 0)
                    || (sellOverride != null && sellOverride < 0)) {
                throw new NumberFormatException();
            }
            if (brokerAveragePrice != null && brokerAveragePrice + ROUNDING_TOLERANCE < execution) {
                throw new AveragePriceBelowExecutionException();
            }

            CommissionBreakdown calculatedBuy = commissionCalculator
                    .calculate(activeProfile, execution, q, TradeSide.BUY);
            boolean buyCommissionFromAveragePrice = brokerAveragePrice != null;
            double effectiveBuyFee = buyCommissionFromAveragePrice
                    ? Math.max(0.0, (brokerAveragePrice - execution) * q)
                    : calculatedBuy.total();
            double purchaseValue = execution * q;
            double totalPurchaseCost = buyCommissionFromAveragePrice
                    ? brokerAveragePrice * q
                    : purchaseValue + effectiveBuyFee;
            double effectiveAveragePrice = buyCommissionFromAveragePrice
                    ? brokerAveragePrice
                    : totalPurchaseCost / q;
            double breakEven = breakEvenCalculator.calculate(
                    activeProfile,
                    execution,
                    q,
                    effectiveBuyFee,
                    sellOverride,
                    dividendValue
            );

            StringBuilder out = new StringBuilder();
            out.append(I18n.text("result.profile")).append(": ").append(activeProfile.name()).append("\n");
            out.append(I18n.text("result.currency")).append(": ").append(selectedCurrency.code()).append("\n");
            appendAccuracy(out, activeProfile);
            out.append("\n");
            out.append(I18n.text("result.purchaseValue")).append(": ").append(UiSupport.money(purchaseValue, selectedCurrency)).append("\n");
            appendBuyCommission(out, calculatedBuy, effectiveBuyFee,
                    buyCommissionFromAveragePrice, selectedCurrency);
            out.append(I18n.text("result.totalPurchaseCost")).append(": ").append(UiSupport.money(totalPurchaseCost, selectedCurrency)).append("\n");
            out.append(I18n.text("result.averagePrice")).append(": ").append(UiSupport.price(effectiveAveragePrice, selectedCurrency)).append("\n");
            out.append(I18n.text("result.breakEven")).append(": ").append(UiSupport.price(breakEven, selectedCurrency)).append("\n");

            int visibleScenario = 0;
            for (double sellPrice : sellPrices) {
                if (sellPrice <= 0) {
                    continue;
                }
                visibleScenario++;
                CommissionBreakdown calculatedSell = commissionCalculator
                        .calculate(activeProfile, sellPrice, q, TradeSide.SELL);
                appendScenario(out, visibleScenario, sellPrice, execution, q,
                        effectiveBuyFee, calculatedSell, sellOverride, dividendValue,
                        totalPurchaseCost, selectedCurrency, activeProfile.taxRate());
            }

            result.setText(out.toString());
            result.setCaretPosition(0);
        } catch (CurrencyMismatchException ex) {
            JOptionPane.showMessageDialog(this, I18n.text("error.currencyMismatch"),
                    I18n.text("error.title"), JOptionPane.ERROR_MESSAGE);
        } catch (AveragePriceBelowExecutionException ex) {
            JOptionPane.showMessageDialog(this, I18n.text("error.averagePriceBelowExecution"),
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
            double execution,
            int quantity,
            double buyFee,
            CommissionBreakdown calculatedSell,
            Double sellOverride,
            double dividendValue,
            double totalPurchaseCost,
            TradeCurrency currency,
            double taxRate
    ) {
        double effectiveSellFee = sellOverride != null ? sellOverride : calculatedSell.total();
        double saleValue = sellPrice * quantity;
        double grossProfit = (sellPrice - execution) * quantity;
        double totalCommissions = buyFee + effectiveSellFee;
        double afterCommissions = saleValue - effectiveSellFee - totalPurchaseCost + dividendValue;
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
        out.append(I18n.text("result.totalCommissions")).append(": ").append(UiSupport.money(totalCommissions, currency)).append("\n");
        out.append(I18n.text("result.afterCommissions")).append(": ").append(UiSupport.money(afterCommissions, currency)).append("\n");
        out.append(I18n.text("result.tax")).append(": ").append(UiSupport.money(tax, currency)).append("\n");
        out.append(I18n.text("result.netProfit")).append(": ").append(UiSupport.money(netProfit, currency)).append("\n");
    }

    private void appendBuyCommission(
            StringBuilder out,
            CommissionBreakdown calculated,
            double effective,
            boolean fromAveragePrice,
            TradeCurrency currency
    ) {
        out.append(I18n.text("result.buyCommission")).append(": ")
                .append(UiSupport.money(effective, currency))
                .append(" [").append(I18n.text(fromAveragePrice
                        ? "result.commissionSource.averagePrice"
                        : "result.commissionSource.calculated")).append("]");
        if (!fromAveragePrice && calculated.knownExternalFees() > 0) {
            out.append(" (").append(I18n.text("result.brokerCommission")).append(" ")
                    .append(UiSupport.money(calculated.brokerCommission(), currency))
                    .append(" + ").append(I18n.text("result.knownExternalFees")).append(" ")
                    .append(UiSupport.money(calculated.knownExternalFees(), currency)).append(")");
        }
        out.append("\n");
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

    private void installBuyCommissionAutoCalculation() {
        DocumentListener listener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateCalculatedBuyCommission();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateCalculatedBuyCommission();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateCalculatedBuyCommission();
            }
        };
        executionPrice.getDocument().addDocumentListener(listener);
        averagePurchasePrice.getDocument().addDocumentListener(listener);
        quantity.getDocument().addDocumentListener(listener);
        currency.addActionListener(e -> updateCalculatedBuyCommission());
    }

    private void updateCalculatedBuyCommission() {
        try {
            double execution = UiSupport.number(executionPrice);
            int q = UiSupport.integer(quantity);
            if (execution <= 0 || q <= 0 || activeProfile == null) {
                clearCalculatedBuyCommission();
                return;
            }

            Double brokerAveragePrice = UiSupport.optionalNumber(averagePurchasePrice);
            double buyFee;
            String sourceTooltip;
            if (brokerAveragePrice != null) {
                if (brokerAveragePrice <= 0 || brokerAveragePrice + ROUNDING_TOLERANCE < execution) {
                    clearCalculatedBuyCommission();
                    return;
                }
                buyFee = Math.max(0.0, (brokerAveragePrice - execution) * q);
                sourceTooltip = I18n.text("result.commissionSource.averagePrice");
            } else {
                buyFee = commissionCalculator.calculate(activeProfile, execution, q, TradeSide.BUY).total();
                sourceTooltip = I18n.text("result.commissionSource.calculated");
            }

            calculatedBuyCommission.setText(UiSupport.decimal(buyFee, 4));
            calculatedBuyCommission.setToolTipText(
                    I18n.text("field.buyCommissionCalculated.tooltip") + " — " + sourceTooltip);
        } catch (NumberFormatException ex) {
            clearCalculatedBuyCommission();
        }
    }

    private void clearCalculatedBuyCommission() {
        calculatedBuyCommission.setText("");
        calculatedBuyCommission.setToolTipText(I18n.text("field.buyCommissionCalculated.tooltip"));
    }

    private void clear() {
        executionPrice.setText("");
        averagePurchasePrice.setText("");
        quantity.setText("");
        calculatedBuyCommission.setText("");
        sellCommissionOverride.setText("");
        for (JTextField expectedSellPrice : expectedSellPrices) {
            expectedSellPrice.setText("");
        }
        dividends.setText("0");
        result.setText("");
    }

    private static final class CurrencyMismatchException extends RuntimeException {
    }

    private static final class AveragePriceBelowExecutionException extends RuntimeException {
    }
}
