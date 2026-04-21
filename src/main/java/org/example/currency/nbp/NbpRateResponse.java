package org.example.currency.nbp;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

record NbpRateResponse(String table, String code, List<Entry> rates) {

    NbpRate toDomain() {
        if (rates == null || rates.isEmpty()) {
            throw new IllegalStateException("NBP response contains no rates for " + code);
        }
        Entry latestEntry = rates.getFirst();
        return new NbpRate(code, latestEntry.effectiveDate(), latestEntry.bidRate(), latestEntry.askRate());
    }

    record Entry(
            String no,
            LocalDate effectiveDate,
            @JsonProperty("bid") BigDecimal bidRate,
            @JsonProperty("ask") BigDecimal askRate
    ) {
    }
}
