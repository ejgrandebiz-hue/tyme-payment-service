package com.tyme.payment.controller;

import com.tyme.payment.exception.IdempotencyKeyRequiredException;
import com.tyme.payment.model.ApiError;
import com.tyme.payment.model.IdempotencyOutcome;
import com.tyme.payment.model.PaymentRequest;
import com.tyme.payment.model.PaymentResponse;
import com.tyme.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<?> createPayment(
            @RequestHeader(value = "Idempotency-Key", required = false) String key,
            @Valid @RequestBody PaymentRequest request) {

        if (key == null || key.isBlank()) {
            throw new IdempotencyKeyRequiredException("The 'Idempotency-Key' header is mandatory for payment transactions.");
        }

        IdempotencyOutcome<PaymentResponse> outcome = paymentService.processPayment(key, request);

        return switch (outcome.status()) {
            case CREATED -> ResponseEntity.status(HttpStatus.CREATED).body(outcome.data());
            case ALREADY_EXISTS -> ResponseEntity.ok(outcome.data());
            case IN_PROGRESS -> ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ApiError("PROCESSING", String.valueOf(HttpStatus.CONFLICT), "Transaction is currently being handled.", LocalDateTime.now()));
            case DATA_MISMATCH -> ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(new ApiError("KEY_REUSE",  String.valueOf(HttpStatus.UNPROCESSABLE_ENTITY),"Idempotency Key used with different request data.", LocalDateTime.now()));
        };
    }
}