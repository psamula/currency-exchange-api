package org.example.currency.nbp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.currency.account.Currency;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
@RequiredArgsConstructor
public class NbpExchangeRateClient {

    static final String RATES_CACHE = "nbp-rates";
    private static final String RATE_PATH = "/exchangerates/rates/c/{currencyCode}";

    private final RestClient nbpRestClient;

    @Cacheable(RATES_CACHE)
    public NbpRate fetchLatestRate(Currency currency) {
        String currencyCode = currency.name();
        log.debug("Fetching latest NBP rate for {}", currencyCode);
        try {
            NbpRateResponse nbpRateResponse = nbpRestClient.get()
                    .uri(RATE_PATH, currencyCode.toLowerCase())
                    .retrieve()
                    .body(NbpRateResponse.class);

            if (nbpRateResponse == null) {
                throw new ExchangeRateUnavailableException(currencyCode, null);
            }
            return nbpRateResponse.toDomain();
        } catch (RestClientException exception) {
            throw new ExchangeRateUnavailableException(currencyCode, exception);
        }
    }
}
