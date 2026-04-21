package org.example.currency.exchange;

import org.example.currency.account.Currency;
import org.example.currency.nbp.NbpRate;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.example.currency.account.Currency.MONETARY_SCALE;

record ExchangeCalculation(BigDecimal rateApplied, BigDecimal amountCredited) {

    /**
     * Credited amount is always rounded in the client's disfavor (towards zero).
     * This prevents arbitrage on sub-grosz dust: e.g., 0.02 PLN / 3.6271 = 0.0055 USD
     * must not be rounded up to 0.01 USD (which would be worth 0.03+ PLN at bid).
     * Banker's rounding (HALF_EVEN) is unsafe here because the client controls the
     * transaction side and can cherry-pick amounts that round in their favor.
     */
    private static final RoundingMode CREDIT_ROUNDING = RoundingMode.DOWN;

    static ExchangeCalculation compute(Currency fromCurrency, Currency toCurrency, BigDecimal amount, NbpRate usdRate) {
        if (fromCurrency == Currency.PLN && toCurrency == Currency.USD) {
            BigDecimal rateApplied = usdRate.askRate();
            BigDecimal creditedAmount = amount.divide(rateApplied, MONETARY_SCALE, CREDIT_ROUNDING);
            return buildCalculation(fromCurrency, toCurrency, amount, rateApplied, creditedAmount);
        }
        if (fromCurrency == Currency.USD && toCurrency == Currency.PLN) {
            BigDecimal rateApplied = usdRate.bidRate();
            BigDecimal creditedAmount = amount.multiply(rateApplied).setScale(MONETARY_SCALE, CREDIT_ROUNDING);
            return buildCalculation(fromCurrency, toCurrency, amount, rateApplied, creditedAmount);
        }
        throw new UnsupportedExchangeException(fromCurrency, toCurrency);
    }

    private static ExchangeCalculation buildCalculation(Currency fromCurrency, Currency toCurrency,
                                                        BigDecimal amount, BigDecimal rateApplied,
                                                        BigDecimal creditedAmount) {
        if (creditedAmount.signum() == 0) {
            throw new ExchangeAmountTooSmallException(fromCurrency, toCurrency, amount);
        }
        return new ExchangeCalculation(rateApplied, creditedAmount);
    }
}
