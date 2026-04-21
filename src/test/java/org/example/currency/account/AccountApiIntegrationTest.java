package org.example.currency.account;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccountApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void should_return201WithInitialBalancesAndLocationHeader_when_creatingAccount() throws Exception {
        CreateAccountRequest createAccountRequest = new CreateAccountRequest("Jan", "Kowalski", new BigDecimal("500.00"));

        MvcResult result = mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createAccountRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        AccountResponse accountResponse = readAccountResponse(result);
        assertThat(accountResponse.id()).isNotNull();
        assertThat(accountResponse.firstName()).isEqualTo("Jan");
        assertThat(accountResponse.lastName()).isEqualTo("Kowalski");
        assertThat(accountResponse.balancePln()).isEqualByComparingTo("500.00");
        assertThat(accountResponse.balanceUsd()).isEqualByComparingTo("0.00");
        assertThat(result.getResponse().getHeader("Location"))
                .endsWith("/api/accounts/" + accountResponse.id());
    }

    @Test
    void should_returnStoredState_when_gettingExistingAccount() throws Exception {
        UUID accountId = createAccount(new CreateAccountRequest("Anna", "Nowak", new BigDecimal("100.00")));

        MvcResult result = mockMvc.perform(get("/api/accounts/{id}", accountId))
                .andExpect(status().isOk())
                .andReturn();

        AccountResponse accountResponse = readAccountResponse(result);
        assertThat(accountResponse.id()).isEqualTo(accountId);
        assertThat(accountResponse.firstName()).isEqualTo("Anna");
        assertThat(accountResponse.balancePln()).isEqualByComparingTo("100.00");
    }

    @Test
    void should_return404_when_accountDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/accounts/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Account not found"));
    }

    @Test
    void should_return400_when_accountIdInPathIsNotAValidUuid() throws Exception {
        mockMvc.perform(get("/api/accounts/{id}", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request parameter"));
    }

    @Test
    void should_return400_when_firstNameIsBlank() throws Exception {
        String malformedBody = """
                {
                  "firstName": "",
                  "lastName": "Nowak",
                  "initialBalancePln": 100.00
                }
                """;

        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"));
    }

    @Test
    void should_return400_when_initialBalanceIsNegative() throws Exception {
        CreateAccountRequest createAccountRequest = new CreateAccountRequest("Jan", "Kowalski", new BigDecimal("-1.00"));

        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createAccountRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return400_when_initialBalanceHasMoreThanTwoDecimals() throws Exception {
        CreateAccountRequest createAccountRequest = new CreateAccountRequest("Jan", "Kowalski", new BigDecimal("10.001"));

        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createAccountRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"));
    }

    private UUID createAccount(CreateAccountRequest createAccountRequest) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createAccountRequest)))
                .andExpect(status().isCreated())
                .andReturn();
        AccountResponse accountResponse = readAccountResponse(result);
        assertThat(accountResponse.id()).isNotNull();
        return accountResponse.id();
    }

    private AccountResponse readAccountResponse(MvcResult result) throws Exception {
        return objectMapper.readValue(result.getResponse().getContentAsString(), AccountResponse.class);
    }
}
