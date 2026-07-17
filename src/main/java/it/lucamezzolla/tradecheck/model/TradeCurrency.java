package it.lucamezzolla.tradecheck.model;

public enum TradeCurrency {
    EUR("EUR", "€"),
    USD("USD", "$");

    private final String code;
    private final String symbol;

    TradeCurrency(String code, String symbol) {
        this.code = code;
        this.symbol = symbol;
    }

    public String code() {
        return code;
    }

    public String symbol() {
        return symbol;
    }

    @Override
    public String toString() {
        return code + " (" + symbol + ")";
    }
}
