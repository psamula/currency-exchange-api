package org.example.currency.exchange;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import org.example.currency.account.Currency;

import java.math.BigDecimal;

public record ExchangeRequest(
        @NotNull
        Currency fromCurrency,

        @NotNull
        Currency toCurrency,

        @NotNull
        @DecimalMin(value = "0.01", message = "amount must be greater than zero")
        @Digits(integer = 19, fraction = 2, message = "amount must have at most 2 decimal places")
        BigDecimal amount
) {
}
