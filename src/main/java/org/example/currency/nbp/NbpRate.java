package org.example.currency.nbp;

import java.math.BigDecimal;
import java.time.LocalDate;

public record NbpRate(
        String code,
        LocalDate effectiveDate,
        BigDecimal bidRate,
        BigDecimal askRate
) {
}
