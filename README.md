# TradeCheck

[![Donate with PayPal](https://img.shields.io/badge/Donate-PayPal-00457C?logo=paypal&logoColor=white)](https://www.paypal.com/paypalme/lucamezzolla82)

> If you find TradeCheck useful or want to support its development, you can make a small donation through PayPal.  
> Your support helps improve documentation, testing, safety checks, UI polish and controlled production-readiness.

---

## Screenshots

### Main application

![TradeCheck main window](docs/images/main-window.png)

---

Applicazione desktop **Java Swing**

## Versione

1.2.0

## Novità della versione 1.2.0

- Motore commissionale automatico applicato separatamente ad acquisto e vendita.
- Cinque profili predefiniti:
  - **Fineco Italia — Conto Trading**;
  - **IBKR Europa — Tiered**;
  - **IBKR Europa — Fixed SmartRouting**;
  - **IBKR USA — Tiered**;
  - **IBKR USA — Fixed**.
- Supporto a due metodi di calcolo:
  - percentuale sul controvalore;
  - costo per azione.
- Supporto a minimo per ordine e tre tipi di massimo:
  - nessun massimo;
  - importo fisso;
  - percentuale del controvalore.
- Costi aggiuntivi configurabili, separati tra acquisto e vendita:
  - importo fisso;
  - importo per azione;
  - percentuale sul controvalore.
- In **Trade effettuato** le commissioni vengono calcolate automaticamente. È comunque possibile inserire la commissione reale applicata dal broker, che prevale sulla stima.
- Il prezzo di pareggio ora tiene conto della commissione di vendita variabile e viene calcolato numericamente.
- La valuta del pannello viene allineata automaticamente al profilo selezionato.
- Migrazione automatica dei profili della versione 1.1.0.

## Profili predefiniti

I valori iniziali sono basati sui tariffari pubblici consultati il 17 luglio 2026:

- Fineco Conto Trading Italia: 0,19% del controvalore, minimo 2,95 EUR, massimo 19 EUR.
- IBKR Europa Tiered: 0,05% del controvalore, minimo 1,25 EUR, massimo 29 EUR, primo scaglione mensile.
- IBKR Europa Fixed SmartRouting: 0,05% del controvalore, minimo 3 EUR, nessun massimo indicato.
- IBKR USA Tiered: 0,0035 USD per azione, minimo 0,35 USD, massimo 1% del controvalore, primo scaglione mensile.
- IBKR USA Fixed: 0,005 USD per azione, minimo 1 USD, massimo 1% del controvalore.

Per IBKR USA Tiered sono inclusi anche i costi regolamentari e di clearing noti configurati nel profilo. Le commissioni Tiered possono comunque variare in base alla sede di esecuzione, all'instradamento e alla liquidità: TradeCheck lo segnala nel risultato. Quando l'ordine è già stato eseguito, inserire la commissione effettiva rende il calcolo aderente al dato del broker.

Fonti ufficiali:

- https://it.finecobank.com/trading/conto-trading/
- https://www.interactivebrokers.com/en/pricing/commissions-stocks-europe.php
- https://www.interactivebrokers.com/en/pricing/commissions-stocks.php

> I tariffari possono cambiare. I profili sono modificabili dalla finestra **Opzioni**.

La valuta selezionata determina il simbolo e la formattazione dei risultati. TradeCheck non esegue conversioni automatiche tra euro e dollari. La stima fiscale in USD non sostituisce il calcolo fiscale in EUR con i cambi ufficiali.

## Requisiti

- Java 21
- Maven

## Compilazione

```bash
mvn clean package
```

## Avvio

```bash
java -jar target/tradecheck-1.2.0.jar
```

Su macOS puoi anche eseguire:

```bash
chmod +x run-macos.command
./run-macos.command
```

## Struttura principale

- `CommissionCalculator`: calcolo commissione base, minimi, massimi e costi aggiuntivi.
- `BreakEvenCalculator`: pareggio con commissione di vendita variabile.
- `PurchasePreviewPanel`: valutazione preventiva con commissioni calcolate sul prezzo di acquisto, stop e target.
- `CompletedTradePanel`: pareggio e tre scenari di vendita, con override delle commissioni reali.
- `SettingsService`: profili e migrazione tramite Java Preferences.
- `OptionsDialog`: modifica completa dei profili.
- `I18n`: inglese, italiano, spagnolo, francese e portoghese.
