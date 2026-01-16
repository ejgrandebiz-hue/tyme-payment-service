package com.tyme.payment.model;

import java.time.LocalDateTime;

public record ApiError(
    String code,
    String status,
    String message,
    LocalDateTime timestamp
) {}