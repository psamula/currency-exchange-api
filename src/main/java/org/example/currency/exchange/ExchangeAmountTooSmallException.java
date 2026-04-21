package org.example.currency.exchange;

import lombok.Getter;
import org.example.currency.account.Currency;

import java.math.BigDecimal;

@Getter
public class ExchangeAmountTooSmallException extends RuntimeException {

    private final Currency fromCurrency;
    private final Currency toCurrency;
    private final BigDecimal amount;

    ExchangeAmountTooSmallException(Currency fromCurrency, Currency toCurrency, BigDecimal amount) {
        super("Exchange amount %s %s is too small to produce a non-zero %s amount"
                .formatted(amount, fromCurrency, toCurrency));
        this.fromCurrency = fromCurrency;
        this.toCurrency = toCurrency;
        this.amount = amount;
    }
}
