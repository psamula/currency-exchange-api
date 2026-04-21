package org.example.currency.account;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.currency.common.InsufficientFundsException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "accounts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account {

    @Id
    private UUID id;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "balance_pln", nullable = false)
    private BigDecimal balancePln;

    @Column(name = "balance_usd", nullable = false)
    private BigDecimal balanceUsd;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public static Account open(String firstName, String lastName, BigDecimal initialBalancePln) {
        Account account = new Account();
        account.id = UUID.randomUUID();
        account.firstName = firstName;
        account.lastName = lastName;
        account.balancePln = normalize(initialBalancePln);
        account.balanceUsd = BigDecimal.ZERO.setScale(Currency.MONETARY_SCALE, Currency.MONETARY_ROUNDING);
        account.createdAt = Instant.now();
        return account;
    }

    public BigDecimal balanceOf(Currency currency) {
        return switch (currency) {
            case PLN -> balancePln;
            case USD -> balanceUsd;
        };
    }

    public void withdraw(Currency currency, BigDecimal amount) {
        BigDecimal normalizedAmount = normalize(amount);
        requirePositive(normalizedAmount);
        BigDecimal currentBalance = balanceOf(currency);
        if (currentBalance.compareTo(normalizedAmount) < 0) {
            throw new InsufficientFundsException(id, currency, currentBalance, normalizedAmount);
        }
        updateBalance(currency, currentBalance.subtract(normalizedAmount));
    }

    public void deposit(Currency currency, BigDecimal amount) {
        BigDecimal normalizedAmount = normalize(amount);
        requirePositive(normalizedAmount);
        updateBalance(currency, balanceOf(currency).add(normalizedAmount));
    }

    private void updateBalance(Currency currency, BigDecimal newBalance) {
        switch (currency) {
            case PLN -> this.balancePln = newBalance;
            case USD -> this.balanceUsd = newBalance;
        }
    }

    private static BigDecimal normalize(BigDecimal amount) {
        return amount.setScale(Currency.MONETARY_SCALE, Currency.MONETARY_ROUNDING);
    }

    private static void requirePositive(BigDecimal amount) {
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be positive, was: " + amount);
        }
    }
}
