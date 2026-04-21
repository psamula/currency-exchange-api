package org.example.currency.common;

import lombok.Getter;
import org.example.currency.account.Currency;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
public class InsufficientFundsException extends RuntimeException {

    private final UUID accountId;
    private final Currency currency;
    private final BigDecimal availableBalance;
    private final BigDecimal requestedAmount;

    public InsufficientFundsException(UUID accountId,
                                       Currency currency,
                                       BigDecimal availableBalance,
                                       BigDecimal requestedAmount) {
        super("Insufficient %s balance on account %s: available=%s, requested=%s"
                .formatted(currency, accountId, availableBalance, requestedAmount));
        this.accountId = accountId;
        this.currency = currency;
        this.availableBalance = availableBalance;
        this.requestedAmount = requestedAmount;
    }
}
