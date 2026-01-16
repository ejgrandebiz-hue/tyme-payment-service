package com.tyme.payment.service;

import com.tyme.payment.config.IdempotencyProperties;
import com.tyme.payment.model.IdempotencyOutcome;
import com.tyme.payment.model.IdempotentResponse;
import com.tyme.payment.model.PaymentRequest;
import com.tyme.payment.model.PaymentResponse;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final String PROCESSING_SENTINEL = "PROCESSING";

    private final RedisTemplate<String, Object> redisTemplate;
    private final IdempotencyProperties properties;


    public IdempotencyOutcome<PaymentResponse> processPayment(String key, PaymentRequest request) {
        String currentRequestHash = generateHash(request);

        Boolean isNewClaim = redisTemplate.opsForValue().setIfAbsent(key, PROCESSING_SENTINEL, properties.lockDuration());

        if (Boolean.FALSE.equals(isNewClaim)) {
            return handleExistingKey(key, currentRequestHash);
        }

        try {
            PaymentResponse response = executePaymentTransaction(request);

            IdempotentResponse cacheWrapper = new IdempotentResponse(201, response, currentRequestHash);
            redisTemplate.opsForValue().set(key, cacheWrapper, properties.lockDuration());

            return new IdempotencyOutcome<>(response, IdempotencyOutcome.OutcomeStatus.CREATED);

        } catch (Exception e) {
            redisTemplate.delete(key);
            throw e;
        }
    }

    private IdempotencyOutcome<PaymentResponse> handleExistingKey(String key, String currentHash) {
        Object cachedValue = redisTemplate.opsForValue().get(key);

        if (PROCESSING_SENTINEL.equals(cachedValue)) {
            return new IdempotencyOutcome<>(null, IdempotencyOutcome.OutcomeStatus.IN_PROGRESS);
        }

        if (cachedValue instanceof IdempotentResponse stored) {
            if (!stored.requestHash().equals(currentHash)) {
                return new IdempotencyOutcome<>(null, IdempotencyOutcome.OutcomeStatus.DATA_MISMATCH);
            }
            return new IdempotencyOutcome<>((PaymentResponse) stored.responseBody(), IdempotencyOutcome.OutcomeStatus.ALREADY_EXISTS);
        }

        throw new IllegalStateException("Unexpected cache state for key: " + key);
    }

    private String generateHash(PaymentRequest request) {
        String rawData = request.accountId() + request.amount() + request.destinationAccount();
        return DigestUtils.sha256Hex(rawData);
    }

    private PaymentResponse executePaymentTransaction(PaymentRequest request) {
        return new PaymentResponse(UUID.randomUUID().toString(), "SUCCESS", request.amount(), request.currency(), LocalDateTime.now());
    }

    public void clearKeyManual(String key) {
        redisTemplate.delete(key);
    }
}