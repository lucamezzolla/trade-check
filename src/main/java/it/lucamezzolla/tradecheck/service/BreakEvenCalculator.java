package it.lucamezzolla.tradecheck.service;

import it.lucamezzolla.tradecheck.model.BrokerProfile;
import it.lucamezzolla.tradecheck.model.TradeSide;

public final class BreakEvenCalculator {
    private final CommissionCalculator commissionCalculator;

    public BreakEvenCalculator(CommissionCalculator commissionCalculator) {
        this.commissionCalculator = commissionCalculator;
    }

    public double calculate(
            BrokerProfile profile,
            double averagePurchasePrice,
            int quantity,
            Double sellCommissionOverride,
            double dividends
    ) {
        if (quantity <= 0) {
            return 0.0;
        }

        double purchaseCost = averagePurchasePrice * quantity;
        if (sellCommissionOverride != null) {
            return Math.max(0.0, (purchaseCost + sellCommissionOverride - dividends) / quantity);
        }

        double low = 0.0;
        double high = Math.max(1.0, averagePurchasePrice * 2.0);
        while (netBeforeTax(profile, high, quantity, purchaseCost, dividends) < 0 && high < 1_000_000_000.0) {
            high *= 2.0;
        }

        for (int i = 0; i < 100; i++) {
            double mid = (low + high) / 2.0;
            if (netBeforeTax(profile, mid, quantity, purchaseCost, dividends) >= 0) {
                high = mid;
            } else {
                low = mid;
            }
        }
        return high;
    }

    private double netBeforeTax(
            BrokerProfile profile,
            double sellPrice,
            int quantity,
            double purchaseCost,
            double dividends
    ) {
        double sellCommission = commissionCalculator
                .calculate(profile, sellPrice, quantity, TradeSide.SELL)
                .total();
        return sellPrice * quantity - sellCommission - purchaseCost + dividends;
    }
}
