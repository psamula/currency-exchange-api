package org.example.currency.exchange;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/accounts/{accountId}/exchanges")
@RequiredArgsConstructor
public class ExchangeController {

    private final ExchangeService exchangeService;

    @PostMapping
    public ExchangeResponse exchange(@PathVariable UUID accountId,
                                     @Valid @RequestBody ExchangeRequest exchangeRequest) {
        return exchangeService.exchange(accountId, exchangeRequest);
    }
}
