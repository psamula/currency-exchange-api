package org.example.currency.exchange;

import org.example.currency.account.Currency;
import org.example.currency.nbp.NbpRate;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExchangeCalculationTest {

    private final NbpRate usdRate = new NbpRate(
            "USD",
            LocalDate.of(2026, 4, 16),
            new BigDecimal("3.8214"),
            new BigDecimal("3.8982")
    );

    @Test
    void should_useAskRateAndRoundToTwoDecimals_when_exchangingPlnToUsd() {
        ExchangeCalculation result = ExchangeCalculation.compute(
                Currency.PLN, Currency.USD, new BigDecimal("100.00"), usdRate);

        assertThat(result.rateApplied()).isEqualByComparingTo("3.8982");
        assertThat(result.amountCredited()).isEqualByComparingTo("25.65");
    }

    @Test
    void should_useBidRateAndRoundToTwoDecimals_when_exchangingUsdToPln() {
        ExchangeCalculation result = ExchangeCalculation.compute(
                Currency.USD, Currency.PLN, new BigDecimal("50.00"), usdRate);

        assertThat(result.rateApplied()).isEqualByComparingTo("3.8214");
        assertThat(result.amountCredited()).isEqualByComparingTo("191.07");
    }

    @Test
    void should_throwUnsupportedExchange_when_currenciesAreTheSame() {
        assertThatThrownBy(() -> ExchangeCalculation.compute(
                Currency.PLN, Currency.PLN, new BigDecimal("10.00"), usdRate))
                .isInstanceOf(UnsupportedExchangeException.class);
    }
}
