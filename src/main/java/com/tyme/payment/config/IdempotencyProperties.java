package com.tyme.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.idempotency")
public record IdempotencyProperties(
    Duration ttlDuration,
    Duration lockDuration
) {}