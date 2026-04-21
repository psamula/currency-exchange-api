package org.example.currency.account;

import lombok.RequiredArgsConstructor;
import org.example.currency.common.AccountNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;

    @Transactional
    public AccountResponse createAccount(CreateAccountRequest createAccountRequest) {
        Account account = Account.open(
                createAccountRequest.firstName(),
                createAccountRequest.lastName(),
                createAccountRequest.initialBalancePln()
        );
        Account savedAccount = accountRepository.save(account);
        return AccountResponse.from(savedAccount);
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccount(UUID accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
        return AccountResponse.from(account);
    }
}
