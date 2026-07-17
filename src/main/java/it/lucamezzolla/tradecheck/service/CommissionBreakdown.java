package it.lucamezzolla.tradecheck.service;

public record CommissionBreakdown(
        double tradeValue,
        double baseBeforeLimits,
        double brokerCommission,
        double knownExternalFees,
        double total,
        boolean variableExternalFees
) {
}
