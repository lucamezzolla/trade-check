package it.lucamezzolla.tradecheck.ui;

import it.lucamezzolla.tradecheck.i18n.I18n;
import it.lucamezzolla.tradecheck.model.BrokerProfile;
import it.lucamezzolla.tradecheck.model.TradeCurrency;
import it.lucamezzolla.tradecheck.model.TradeSide;
import it.lucamezzolla.tradecheck.service.CommissionBreakdown;
import it.lucamezzolla.tradecheck.service.CommissionCalculator;
import it.lucamezzolla.tradecheck.service.SettingsService;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("serial")
public final class PurchasePreviewPanel extends JPanel {
    private final JComboBox<TradeCurrency> currency = new JComboBox<>(TradeCurrency.values());
    private final JTextField ticker = new JTextField();
    private final JTextField price = new JTextField();
    private final JTextField quantity = new JTextField();
    private final JTextField stopLoss = new JTextField();
    private final JTextField target = new JTextField();

    private final JCheckBox weekly = new JCheckBox();
    private final JCheckBox daily = new JCheckBox();
    private final JCheckBox fourHour = new JCheckBox();
    private final JCheckBox volume = new JCheckBox();
    private final JCheckBox resistance = new JCheckBox();
    private final JCheckBox earnings = new JCheckBox();

    private final JTextArea result = new JTextArea(12, 72);
    private final SettingsService settingsService;
    private final CommissionCalculator commissionCalculator = new CommissionCalculator();
    private BrokerProfile activeProfile;

    public PurchasePreviewPanel(SettingsService settingsService) {
        this.settingsService = settingsService;
        this.activeProfile = settingsService.loadActiveProfile();

        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.add(UiSupport.row(I18n.text("field.currency"), currency));
        form.add(UiSupport.row(I18n.text("field.ticker"), ticker));
        form.add(UiSupport.row(I18n.text("field.price"), price));
        form.add(UiSupport.row(I18n.text("field.quantity"), quantity));
        form.add(UiSupport.row(I18n.text("field.stopLoss"), stopLoss));
        form.add(UiSupport.row(I18n.text("field.target"), target));

        weekly.setText(I18n.text("check.weekly"));
        daily.setText(I18n.text("check.daily"));
        fourHour.setText(I18n.text("check.fourHour"));
        volume.setText(I18n.text("check.volume"));
        resistance.setText(I18n.text("check.resistance"));
        earnings.setText(I18n.text("check.earnings"));

        JPanel checks = new JPanel();
        checks.setLayout(new BoxLayout(checks, BoxLayout.Y_AXIS));
        checks.setBorder(BorderFactory.createTitledBorder(I18n.text("section.assessment")));
        checks.add(weekly);
        checks.add(daily);
        checks.add(fourHour);
        checks.add(volume);
        checks.add(resistance);
        checks.add(earnings);

        JButton calculate = new JButton(I18n.text("button.calculate"));
        calculate.addActionListener(e -> calculate());
        JButton clear = new JButton(I18n.text("button.clear"));
        clear.addActionListener(e -> clear());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(calculate);
        buttons.add(clear);

        JPanel left = new JPanel(new BorderLayout(8, 8));
        left.add(form, BorderLayout.NORTH);
        left.add(checks, BorderLayout.CENTER);
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

    public void applyProfile(BrokerProfile profile) {
        this.activeProfile = profile;
        currency.setSelectedItem(profile.currency());
        result.setText("");
    }

    private void calculate() {
        try {
            double p = UiSupport.number(price);
            int q = UiSupport.integer(quantity);
            double stop = UiSupport.number(stopLoss);
            double t = UiSupport.number(target);
            TradeCurrency selectedCurrency = (TradeCurrency) currency.getSelectedItem();

            activeProfile = settingsService.loadActiveProfile();
            validateCurrency(selectedCurrency);
            if (p <= 0 || q <= 0) {
                throw new NumberFormatException();
            }

            double capital = p * q;
            CommissionBreakdown buy = commissionCalculator.calculate(activeProfile, p, q, TradeSide.BUY);
            CommissionBreakdown sellAtTarget = t > 0
                    ? commissionCalculator.calculate(activeProfile, t, q, TradeSide.SELL)
                    : commissionCalculator.calculate(activeProfile, p, q, TradeSide.SELL);
            CommissionBreakdown sellAtStop = stop > 0
                    ? commissionCalculator.calculate(activeProfile, stop, q, TradeSide.SELL)
                    : sellAtTarget;

            double totalTargetCommissions = buy.total() + sellAtTarget.total();
            double maxLoss = stop > 0 && stop < p
                    ? (p - stop) * q + buy.total() + sellAtStop.total()
                    : 0.0;
            double potentialProfit = t > p
                    ? (t - p) * q - totalTargetCommissions
                    : 0.0;
            double rr = maxLoss > 0 ? potentialProfit / maxLoss : 0.0;

            int score = 0;
            List<String> reasons = new ArrayList<>();
            if (weekly.isSelected()) score += 20; else reasons.add(I18n.text("reason.weekly"));
            if (daily.isSelected()) score += 20; else reasons.add(I18n.text("reason.daily"));
            if (fourHour.isSelected()) score += 25; else reasons.add(I18n.text("reason.fourHour"));
            if (volume.isSelected()) score += 15; else reasons.add(I18n.text("reason.volume"));
            if (!resistance.isSelected()) score += 10; else reasons.add(I18n.text("reason.resistance"));
            if (!earnings.isSelected()) score += 5; else reasons.add(I18n.text("reason.earnings"));
            if (rr >= 2.0) score += 5; else reasons.add(I18n.text("reason.rr"));

            String verdict;
            if (stop <= 0 || t <= p) verdict = I18n.text("verdict.incomplete");
            else if (resistance.isSelected() || rr < 1.0) verdict = I18n.text("verdict.avoid");
            else if (score >= 80) verdict = I18n.text("verdict.valid");
            else if (score >= 60) verdict = I18n.text("verdict.wait");
            else verdict = I18n.text("verdict.weak");

            StringBuilder out = new StringBuilder();
            out.append(I18n.text("result.profile")).append(": ").append(activeProfile.name()).append("\n");
            out.append(I18n.text("result.currency")).append(": ").append(selectedCurrency.code()).append("\n");
            appendAccuracy(out, activeProfile);
            out.append(I18n.text("result.verdict")).append(": ").append(verdict).append("\n");
            out.append(I18n.text("result.score")).append(": ").append(score).append("/100\n\n");
            out.append(I18n.text("result.capital")).append(": ").append(UiSupport.money(capital, selectedCurrency)).append("\n");
            appendCommission(out, I18n.text("result.buyCommission"), buy, selectedCurrency);
            appendCommission(out, I18n.text("result.sellCommissionAtTarget"), sellAtTarget, selectedCurrency);
            out.append(I18n.text("result.totalCommissions")).append(": ")
                    .append(UiSupport.money(totalTargetCommissions, selectedCurrency)).append("\n");
            out.append(I18n.text("result.maxLoss")).append(": ").append(UiSupport.money(maxLoss, selectedCurrency)).append("\n");
            out.append(I18n.text("result.potentialProfit")).append(": ").append(UiSupport.money(potentialProfit, selectedCurrency)).append("\n");
            out.append(I18n.text("result.rr")).append(": ").append(UiSupport.decimal(rr, 2)).append("\n");
            if (!reasons.isEmpty()) {
                out.append("\n").append(I18n.text("result.reasons")).append(":\n");
                reasons.forEach(r -> out.append("- ").append(r).append("\n"));
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

    private void appendCommission(StringBuilder out, String label, CommissionBreakdown commission,
                                  TradeCurrency selectedCurrency) {
        out.append(label).append(": ").append(UiSupport.money(commission.total(), selectedCurrency));
        if (commission.knownExternalFees() > 0) {
            out.append(" (").append(I18n.text("result.brokerCommission")).append(" ")
                    .append(UiSupport.money(commission.brokerCommission(), selectedCurrency))
                    .append(" + ").append(I18n.text("result.knownExternalFees")).append(" ")
                    .append(UiSupport.money(commission.knownExternalFees(), selectedCurrency)).append(")");
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
        ticker.setText("");
        price.setText("");
        quantity.setText("");
        stopLoss.setText("");
        target.setText("");
        weekly.setSelected(false);
        daily.setSelected(false);
        fourHour.setSelected(false);
        volume.setSelected(false);
        resistance.setSelected(false);
        earnings.setSelected(false);
        result.setText("");
    }

    private static final class CurrencyMismatchException extends RuntimeException {
    }
}
