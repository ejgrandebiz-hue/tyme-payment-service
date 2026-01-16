package com.tyme.payment.model;

public record IdempotentResponse(
        int statusCode,
        Object responseBody,
        String requestHash
) {
}