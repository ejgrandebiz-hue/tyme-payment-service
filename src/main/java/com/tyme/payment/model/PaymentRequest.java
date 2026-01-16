package com.tyme.payment.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PaymentRequest(
        @NotBlank(message = "Account ID is required")
        String accountId,

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be greater than zero")
        BigDecimal amount,

        @NotBlank(message = "Currency code is required")
        @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code")
        String currency,

        @NotBlank(message = "Destination account is required")
        String destinationAccount
) {
}
