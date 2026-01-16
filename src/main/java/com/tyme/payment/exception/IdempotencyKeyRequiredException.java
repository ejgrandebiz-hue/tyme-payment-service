package com.tyme.payment.exception;

public class IdempotencyKeyRequiredException extends RuntimeException {
    public IdempotencyKeyRequiredException(String message) {
        super(message);
    }
}