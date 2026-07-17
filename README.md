# TradeCheck

[![Donate with PayPal](https://img.shields.io/badge/Donate-PayPal-00457C?logo=paypal&logoColor=white)](https://www.paypal.com/paypalme/lucamezzolla82)

> If you find TradeCheck useful or want to support its development, you can make a small donation through PayPal.  
> Your support helps improve documentation, testing, safety checks, UI polish and controlled production-readiness.

---

## Screenshots

### Main application

![TradeCheck main window](docs/images/main-window.png)

---

Desktop application built with **Java Swing**

## Version

1.2.1

## What's new in version 1.2.1

- Automatic commission engine applied separately to purchases and sales.
- Five predefined profiles:
  - **Fineco Italy — Trading Account**;
  - **IBKR Europe — Tiered**;
  - **IBKR Europe — Fixed SmartRouting**;
  - **IBKR USA — Tiered**;
  - **IBKR USA — Fixed**.
- Support for two calculation methods:
  - percentage of the trade value;
  - cost per share.
- Support for a minimum commission per order and three maximum commission types:
  - no maximum;
  - fixed amount;
  - percentage of the trade value.
- Configurable additional costs, separated between purchases and sales:
  - fixed amount;
  - amount per share;
  - percentage of the trade value.
- In **Trade** mode, the entire holding is treated as one aggregate lot.
- The broker average purchase price must include all purchase commissions and charges for the whole position.
- TradeCheck does not add purchase costs again, preventing commissions from being counted twice.
- The price of the latest individual purchase must not be used when the position contains multiple purchases; the broker average price for the complete position must be entered instead.
- The actual selling commission can still be entered manually and takes precedence over the profile estimate.
- If the actual selling commission is not provided, TradeCheck calculates it from the selected platform profile for each selling scenario.
- The break-even price now accounts for the variable selling commission and is calculated numerically.
- The panel currency is automatically aligned with the selected profile.
- The **Trade** label is used consistently in every supported language.
- The **Options** window has been enlarged and the form width has been improved to avoid horizontal scrolling.
- The aggregate-position instructions are displayed directly in the application in every supported language.
- Automatic migration of profiles from version 1.1.0.

## Aggregate position model

In **Trade** mode, TradeCheck considers the current holding to be a single aggregate lot, even when it was built through several purchases at different prices.

Enter:

- the broker average purchase price for the entire current position;
- the total quantity currently held;
- one or more expected selling prices;
- the actual selling commission only when it is already known.

The average purchase price must already include all purchase commissions and charges. TradeCheck therefore calculates the total position cost as:

```text
position cost = broker average purchase price × total quantity
```

Purchase commissions are not added separately because they are already included in that average price. This avoids double-counting and allows a position containing several purchases to be handled without reconstructing every individual order.

For each selling scenario, TradeCheck calculates or accepts the selling commission and then estimates:

- sale value;
- gross profit relative to the aggregate position cost;
- profit after the selling commission and dividends;
- estimated tax;
- estimated net profit;
- break-even selling price.

> Do not enter the execution price of the latest purchase when it differs from the average price of the complete position. Use the broker average price for all shares currently held.

## Predefined profiles

The initial values are based on the publicly available pricing schedules consulted on July 17, 2026:

- Fineco Italy Trading Account: 0.19% of the trade value, minimum 2.95 EUR, maximum 19 EUR.
- IBKR Europe Tiered: 0.05% of the trade value, minimum 1.25 EUR, maximum 29 EUR, first monthly volume tier.
- IBKR Europe Fixed SmartRouting: 0.05% of the trade value, minimum 3 EUR, no stated maximum.
- IBKR USA Tiered: 0.0035 USD per share, minimum 0.35 USD, maximum 1% of the trade value, first monthly volume tier.
- IBKR USA Fixed: 0.005 USD per share, minimum 1 USD, maximum 1% of the trade value.

The IBKR USA Tiered profile also includes the known regulatory and clearing costs configured in the profile. Tiered commissions may still vary depending on the execution venue, routing and liquidity; TradeCheck reports this in the result. When the order has already been executed, entering the actual selling commission makes the calculation consistent with the broker's data.

For the purchase side in **Trade** mode, TradeCheck does not attempt to derive an individual purchase commission from the latest execution price. The broker average purchase price is used directly because it represents the aggregate cost of the full position and already includes purchase commissions and charges.

Official sources:

- https://it.finecobank.com/trading/conto-trading/
- https://www.interactivebrokers.com/en/pricing/commissions-stocks-europe.php
- https://www.interactivebrokers.com/en/pricing/commissions-stocks.php

> Pricing schedules may change. Profiles can be edited from the **Options** window.

The selected currency determines the symbol and formatting used in the results. TradeCheck does not perform automatic conversions between euros and US dollars. A tax estimate in USD does not replace the tax calculation in EUR using official exchange rates.

## Requirements

- Java 21
- Maven

## Build

```bash
mvn clean package
```

## Run

```bash
java -jar target/tradecheck-1.2.1.jar
```

On macOS, you can also run:

```bash
chmod +x run-macos.command
./run-macos.command
```

## Main structure

- `CommissionCalculator`: calculates base commissions, minimums, maximums and additional costs.
- `BreakEvenCalculator`: calculates the break-even price from the aggregate position cost and a variable selling commission.
- `PurchasePreviewPanel`: pre-trade assessment with commissions calculated for the purchase price, stop and target.
- `CompletedTradePanel`: aggregate-position cost, break-even calculation and three selling scenarios, with an optional actual selling commission override.
- `SettingsService`: profile storage and migration through Java Preferences.
- `OptionsDialog`: complete profile editing.
- `I18n`: English, Italian, Spanish, French and Portuguese.
