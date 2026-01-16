package com.tyme.payment.advice;

import com.tyme.payment.exception.IdempotencyKeyRequiredException;
import com.tyme.payment.model.ApiError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IdempotencyKeyRequiredException.class)
    public ResponseEntity<ApiError> handleIdempotencyException(IdempotencyKeyRequiredException ex) {

        HttpStatus status = HttpStatus.BAD_REQUEST;

        return ResponseEntity.status(status)
                .body(new ApiError("IDEMPOTENCY_KEY_MISSING", String.valueOf(status.value()), ex.getMessage(), LocalDateTime.now()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationExceptions(MethodArgumentNotValidException ex) {

        HttpStatus status = HttpStatus.BAD_REQUEST;

        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        ApiError error = new ApiError(
                "INVALID_REQUEST_PAYLOAD",
                String.valueOf(status.value()),
                errorMessage,
                LocalDateTime.now()
        );
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }
}