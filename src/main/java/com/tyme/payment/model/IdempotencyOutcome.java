package com.tyme.payment.model;

public record IdempotencyOutcome<T>(
    T data,
    OutcomeStatus status
) {
    public enum OutcomeStatus {
        CREATED,
        ALREADY_EXISTS,
        IN_PROGRESS,
        DATA_MISMATCH
    }
}