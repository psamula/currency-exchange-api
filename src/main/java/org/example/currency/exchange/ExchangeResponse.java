package org.example.currency.exchange;

import org.example.currency.account.Account;
import org.example.currency.account.Currency;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ExchangeResponse(
        UUID accountId,
        Currency fromCurrency,
        BigDecimal amountDebited,
        Currency toCurrency,
        BigDecimal amountCredited,
        BigDecimal rateApplied,
        LocalDate rateEffectiveDate,
        BigDecimal balancePln,
        BigDecimal balanceUsd
) {
    public static ExchangeResponse of(Account account,
                                       Currency fromCurrency,
                                       BigDecimal amountDebited,
                                       Currency toCurrency,
                                       BigDecimal amountCredited,
                                       BigDecimal rateApplied,
                                       LocalDate rateEffectiveDate) {
        return new ExchangeResponse(
                account.getId(),
                fromCurrency,
                amountDebited,
                toCurrency,
                amountCredited,
                rateApplied,
                rateEffectiveDate,
                account.getBalancePln(),
                account.getBalanceUsd()
        );
    }
}
