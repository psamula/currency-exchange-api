package org.example.currency.exchange;

import org.example.currency.account.Currency;

public class UnsupportedExchangeException extends RuntimeException {

    public UnsupportedExchangeException(Currency from, Currency to) {
        super("Unsupported exchange pair: %s -> %s".formatted(from, to));
    }
}
