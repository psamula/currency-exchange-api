package org.example.currency.account;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        String firstName,
        String lastName,
        BigDecimal balancePln,
        BigDecimal balanceUsd
) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getFirstName(),
                account.getLastName(),
                account.getBalancePln(),
                account.getBalanceUsd()
        );
    }
}
