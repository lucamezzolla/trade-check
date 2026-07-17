package it.lucamezzolla.tradecheck.service;

import it.lucamezzolla.tradecheck.model.BrokerProfile;
import it.lucamezzolla.tradecheck.model.CommissionBasis;
import it.lucamezzolla.tradecheck.model.MaximumType;
import it.lucamezzolla.tradecheck.model.TradeSide;

public final class CommissionCalculator {

    public CommissionBreakdown calculate(BrokerProfile profile, double price, int quantity, TradeSide side) {
        if (profile == null || price < 0 || quantity < 0 || side == null) {
            throw new IllegalArgumentException("Invalid commission input");
        }

        double tradeValue = price * quantity;
        double baseBeforeLimits = profile.commissionBasis() == CommissionBasis.TRADE_VALUE_PERCENT
                ? tradeValue * profile.rate() / 100.0
                : quantity * profile.rate();

        double brokerCommission = Math.max(baseBeforeLimits, profile.minimum());
        double cap = switch (profile.maximumType()) {
            case NONE -> Double.POSITIVE_INFINITY;
            case FIXED_AMOUNT -> profile.maximumValue();
            case TRADE_VALUE_PERCENT -> tradeValue * profile.maximumValue() / 100.0;
        };

        // IBKR specifica che, quando il massimo calcolato è inferiore al minimo,
        // prevale il massimo. Applicare il cap dopo il minimo riproduce la regola.
        brokerCommission = Math.min(brokerCommission, cap);

        double extraFixed = side == TradeSide.BUY
                ? profile.buyExtraFixed()
                : profile.sellExtraFixed();
        double extraPerShare = side == TradeSide.BUY
                ? profile.buyExtraPerShare()
                : profile.sellExtraPerShare();
        double extraPercent = side == TradeSide.BUY
                ? profile.buyExtraPercent()
                : profile.sellExtraPercent();

        double knownExternalFees = extraFixed
                + quantity * extraPerShare
                + tradeValue * extraPercent / 100.0;

        return new CommissionBreakdown(
                tradeValue,
                baseBeforeLimits,
                brokerCommission,
                knownExternalFees,
                brokerCommission + knownExternalFees,
                profile.variableExternalFees()
        );
    }
}
