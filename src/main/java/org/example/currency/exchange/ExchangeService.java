package org.example.currency.exchange;

import lombok.RequiredArgsConstructor;
import org.example.currency.account.Account;
import org.example.currency.account.Currency;
import org.example.currency.nbp.NbpExchangeRateClient;
import org.example.currency.nbp.NbpRate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExchangeService {

    private final NbpExchangeRateClient nbpExchangeRateClient;
    private final AccountExchangeOperation accountExchangeOperation;

    public ExchangeResponse exchange(UUID accountId, ExchangeRequest exchangeRequest) {
        rejectSameCurrency(exchangeRequest.fromCurrency(), exchangeRequest.toCurrency());
        NbpRate usdRate = nbpExchangeRateClient.fetchLatestRate(Currency.USD);
        ExchangeCalculation calculation = ExchangeCalculation.compute(
                exchangeRequest.fromCurrency(),
                exchangeRequest.toCurrency(),
                exchangeRequest.amount(),
                usdRate
        );
        Account mutatedAccount = accountExchangeOperation.apply(
                accountId,
                exchangeRequest.fromCurrency(),
                exchangeRequest.amount(),
                exchangeRequest.toCurrency(),
                calculation.amountCredited()
        );
        return ExchangeResponse.of(
                mutatedAccount,
                exchangeRequest.fromCurrency(),
                exchangeRequest.amount(),
                exchangeRequest.toCurrency(),
                calculation.amountCredited(),
                calculation.rateApplied(),
                usdRate.effectiveDate()
        );
    }

    private static void rejectSameCurrency(Currency fromCurrency, Currency toCurrency) {
        if (fromCurrency == toCurrency) {
            throw new UnsupportedExchangeException(fromCurrency, toCurrency);
        }
    }
}
