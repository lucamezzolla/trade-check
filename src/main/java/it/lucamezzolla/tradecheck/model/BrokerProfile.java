package it.lucamezzolla.tradecheck.model;

import java.util.Objects;

/**
 * Parametri di calcolo delle commissioni di una piattaforma/mercato.
 *
 * <p>La commissione base può essere percentuale sul controvalore oppure per
 * azione. Minimo e massimo vengono applicati alla commissione base; i costi
 * aggiuntivi vengono sommati successivamente e possono differire tra acquisto
 * e vendita.</p>
 */
public record BrokerProfile(
        String id,
        String name,
        TradeCurrency currency,
        CommissionBasis commissionBasis,
        double rate,
        double minimum,
        MaximumType maximumType,
        double maximumValue,
        double buyExtraFixed,
        double sellExtraFixed,
        double buyExtraPerShare,
        double sellExtraPerShare,
        double buyExtraPercent,
        double sellExtraPercent,
        boolean variableExternalFees,
        double taxRate
) {
    public BrokerProfile {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(currency, "currency");
        Objects.requireNonNull(commissionBasis, "commissionBasis");
        Objects.requireNonNull(maximumType, "maximumType");
    }

    @Override
    public String toString() {
        return name;
    }
}
