package org.example.currency.account;

import org.example.currency.common.InsufficientFundsException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountTest {

    @Test
    void should_startWithProvidedPlnBalanceAndZeroUsd_when_accountIsOpened() {
        Account account = Account.open("Jan", "Kowalski", new BigDecimal("1000.00"));

        assertThat(account.getFirstName()).isEqualTo("Jan");
        assertThat(account.getLastName()).isEqualTo("Kowalski");
        assertThat(account.getBalancePln()).isEqualByComparingTo("1000.00");
        assertThat(account.getBalanceUsd()).isEqualByComparingTo("0.00");
        assertThat(account.getId()).isNotNull();
    }

    @Test
    void should_reduceBalanceOfRequestedCurrency_when_withdrawCalled() {
        Account account = Account.open("Jan", "Kowalski", new BigDecimal("1000.00"));

        account.withdraw(Currency.PLN, new BigDecimal("250.00"));

        assertThat(account.getBalancePln()).isEqualByComparingTo("750.00");
    }

    @Test
    void should_increaseBalanceOfRequestedCurrency_when_depositCalled() {
        Account account = Account.open("Jan", "Kowalski", new BigDecimal("100.00"));

        account.deposit(Currency.USD, new BigDecimal("42.50"));

        assertThat(account.getBalanceUsd()).isEqualByComparingTo("42.50");
    }

    @Test
    void should_throwInsufficientFundsAndLeaveBalanceUnchanged_when_withdrawExceedsBalance() {
        Account account = Account.open("Jan", "Kowalski", new BigDecimal("100.00"));

        assertThatThrownBy(() -> account.withdraw(Currency.PLN, new BigDecimal("200.00")))
                .isInstanceOf(InsufficientFundsException.class);

        assertThat(account.getBalancePln()).isEqualByComparingTo("100.00");
    }

    @Test
    void should_rejectWithdraw_when_amountIsNotPositive() {
        Account account = Account.open("Jan", "Kowalski", new BigDecimal("100.00"));

        assertThatThrownBy(() -> account.withdraw(Currency.PLN, BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_rejectDeposit_when_amountIsNotPositive() {
        Account account = Account.open("Jan", "Kowalski", new BigDecimal("100.00"));

        assertThatThrownBy(() -> account.deposit(Currency.USD, new BigDecimal("-1.00")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
