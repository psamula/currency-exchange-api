package org.example.currency.nbp;

public class ExchangeRateUnavailableException extends RuntimeException {

    public ExchangeRateUnavailableException(String currencyCode, Throwable cause) {
        super("Failed to fetch NBP exchange rate for " + currencyCode, cause);
    }
}
