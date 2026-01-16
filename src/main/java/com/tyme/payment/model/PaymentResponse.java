package com.tyme.payment.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        String transactionId,
        String status,
        BigDecimal amount,
        String currency,
        LocalDateTime createdAt
) {
}