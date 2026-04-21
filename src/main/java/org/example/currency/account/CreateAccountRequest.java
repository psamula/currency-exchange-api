package org.example.currency.account;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateAccountRequest(
        @NotBlank
        @Size(max = 100)
        String firstName,

        @NotBlank
        @Size(max = 100)
        String lastName,

        @NotNull
        @DecimalMin(value = "0.00", message = "initial balance must be non-negative")
        @Digits(integer = 19, fraction = 2, message = "initial balance must have at most 2 decimal places")
        BigDecimal initialBalancePln
) {
}
