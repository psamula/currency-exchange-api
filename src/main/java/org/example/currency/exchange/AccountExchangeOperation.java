package org.example.currency.exchange;

import lombok.RequiredArgsConstructor;
import org.example.currency.account.Account;
import org.example.currency.account.AccountRepository;
import org.example.currency.account.Currency;
import org.example.currency.common.AccountNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
class AccountExchangeOperation {

    private final AccountRepository accountRepository;

    @Transactional
    public Account apply(UUID accountId,
                         Currency fromCurrency,
                         BigDecimal amountDebited,
                         Currency toCurrency,
                         BigDecimal amountCredited) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
        account.withdraw(fromCurrency, amountDebited);
        account.deposit(toCurrency, amountCredited);
        return account;
    }
}
