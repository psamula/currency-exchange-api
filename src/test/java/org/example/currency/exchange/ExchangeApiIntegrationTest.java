package org.example.currency.exchange;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.example.currency.account.AccountResponse;
import org.example.currency.account.CreateAccountRequest;
import org.example.currency.account.Currency;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExchangeApiIntegrationTest {

    private static final String USD_RATE_STUB = "nbp/usd-rate.json";

    @RegisterExtension
    static WireMockExtension nbpServer = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort().usingFilesUnderClasspath("."))
            .build();

    @DynamicPropertySource
    static void nbpProperties(DynamicPropertyRegistry registry) {
        registry.add("nbp.base-url", () -> nbpServer.baseUrl() + "/api");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void should_debitPlnAndCreditUsdAtAskRate_when_exchangingPlnToUsd() throws Exception {
        stubUsdRate();
        UUID accountId = createAccount(new CreateAccountRequest("Jan", "Kowalski", new BigDecimal("1000.00")));

        ExchangeRequest exchangeRequest = new ExchangeRequest(Currency.PLN, Currency.USD, new BigDecimal("100.00"));

        MvcResult result = mockMvc.perform(post("/api/accounts/{id}/exchanges", accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(exchangeRequest)))
                .andExpect(status().isOk())
                .andReturn();

        ExchangeResponse exchangeResponse = readExchangeResponse(result);
        assertThat(exchangeResponse.accountId()).isEqualTo(accountId);
        assertThat(exchangeResponse.fromCurrency()).isEqualTo(Currency.PLN);
        assertThat(exchangeResponse.toCurrency()).isEqualTo(Currency.USD);
        assertThat(exchangeResponse.rateApplied()).isEqualByComparingTo("3.8982");
        assertThat(exchangeResponse.amountDebited()).isEqualByComparingTo("100.00");
        assertThat(exchangeResponse.amountCredited()).isEqualByComparingTo("25.65");
        assertThat(exchangeResponse.balancePln()).isEqualByComparingTo("900.00");
        assertThat(exchangeResponse.balanceUsd()).isEqualByComparingTo("25.65");
    }

    @Test
    void should_creditPlnAtBidRate_when_exchangingUsdToPln() throws Exception {
        stubUsdRate();
        UUID accountId = createAccount(new CreateAccountRequest("Jan", "Kowalski", new BigDecimal("1000.00")));
        ExchangeRequest plnToUsdRequest = new ExchangeRequest(Currency.PLN, Currency.USD, new BigDecimal("100.00"));
        mockMvc.perform(post("/api/accounts/{id}/exchanges", accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(plnToUsdRequest)))
                .andExpect(status().isOk());

        ExchangeRequest exchangeRequest = new ExchangeRequest(Currency.USD, Currency.PLN, new BigDecimal("25.00"));

        MvcResult result = mockMvc.perform(post("/api/accounts/{id}/exchanges", accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(exchangeRequest)))
                .andExpect(status().isOk())
                .andReturn();

        ExchangeResponse exchangeResponse = readExchangeResponse(result);
        assertThat(exchangeResponse.rateApplied()).isEqualByComparingTo("3.8214");
        assertThat(exchangeResponse.amountDebited()).isEqualByComparingTo("25.00");
        assertThat(exchangeResponse.amountCredited()).isEqualByComparingTo("95.53");
        assertThat(exchangeResponse.balanceUsd()).isEqualByComparingTo("0.65");
        assertThat(exchangeResponse.balancePln()).isEqualByComparingTo("995.53");
    }

    @Test
    void should_return422_when_creditedAmountRoundsDownToZero() throws Exception {
        stubUsdRate();
        UUID accountId = createAccount(new CreateAccountRequest("Jan", "Kowalski", new BigDecimal("1000.00")));

        ExchangeRequest exchangeRequest = new ExchangeRequest(Currency.PLN, Currency.USD, new BigDecimal("0.02"));

        mockMvc.perform(post("/api/accounts/{id}/exchanges", accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(exchangeRequest)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Exchange amount too small"))
                .andExpect(jsonPath("$.fromCurrency").value("PLN"))
                .andExpect(jsonPath("$.toCurrency").value("USD"))
                .andExpect(jsonPath("$.amount").value(0.02));
    }

    @Test
    void should_roundCreditedAmountDown_preventingDustArbitrage() throws Exception {
        stubUsdRate();
        UUID accountId = createAccount(new CreateAccountRequest("Jan", "Kowalski", new BigDecimal("1000.00")));

        ExchangeRequest exchangeRequest = new ExchangeRequest(Currency.PLN, Currency.USD, new BigDecimal("0.07"));

        MvcResult result = mockMvc.perform(post("/api/accounts/{id}/exchanges", accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(exchangeRequest)))
                .andExpect(status().isOk())
                .andReturn();

        ExchangeResponse exchangeResponse = readExchangeResponse(result);
        assertThat(exchangeResponse.amountCredited()).isEqualByComparingTo("0.01");
    }

    @Test
    void should_return422_when_balanceIsInsufficient() throws Exception {
        stubUsdRate();
        UUID accountId = createAccount(new CreateAccountRequest("Jan", "Kowalski", new BigDecimal("10.00")));

        ExchangeRequest exchangeRequest = new ExchangeRequest(Currency.PLN, Currency.USD, new BigDecimal("1000.00"));

        mockMvc.perform(post("/api/accounts/{id}/exchanges", accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(exchangeRequest)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Insufficient funds"));
    }

    @Test
    void should_return400_when_currenciesAreTheSame() throws Exception {
        UUID accountId = createAccount(new CreateAccountRequest("Jan", "Kowalski", new BigDecimal("100.00")));

        ExchangeRequest exchangeRequest = new ExchangeRequest(Currency.PLN, Currency.PLN, new BigDecimal("10.00"));

        mockMvc.perform(post("/api/accounts/{id}/exchanges", accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(exchangeRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Unsupported exchange pair"));
    }

    @Test
    void should_return503_when_nbpIsUnavailable() throws Exception {
        nbpServer.stubFor(get(urlPathEqualTo("/api/exchangerates/rates/c/usd"))
                .willReturn(aResponse().withStatus(500)));
        UUID accountId = createAccount(new CreateAccountRequest("Jan", "Kowalski", new BigDecimal("100.00")));

        ExchangeRequest exchangeRequest = new ExchangeRequest(Currency.PLN, Currency.USD, new BigDecimal("10.00"));

        mockMvc.perform(post("/api/accounts/{id}/exchanges", accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(exchangeRequest)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.title").value("Exchange rate unavailable"));
    }

    @Test
    void should_return404_when_accountDoesNotExist() throws Exception {
        stubUsdRate();

        ExchangeRequest exchangeRequest = new ExchangeRequest(Currency.PLN, Currency.USD, new BigDecimal("10.00"));

        mockMvc.perform(post("/api/accounts/{id}/exchanges", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(exchangeRequest)))
                .andExpect(status().isNotFound());
    }

    private void stubUsdRate() {
        nbpServer.stubFor(get(urlPathEqualTo("/api/exchangerates/rates/c/usd"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile(USD_RATE_STUB)));
    }

    private UUID createAccount(CreateAccountRequest createAccountRequest) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createAccountRequest)))
                .andExpect(status().isCreated())
                .andReturn();
        AccountResponse accountResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(), AccountResponse.class);
        assertThat(accountResponse.id()).isNotNull();
        return accountResponse.id();
    }

    private ExchangeResponse readExchangeResponse(MvcResult result) throws Exception {
        return objectMapper.readValue(result.getResponse().getContentAsString(), ExchangeResponse.class);
    }
}
