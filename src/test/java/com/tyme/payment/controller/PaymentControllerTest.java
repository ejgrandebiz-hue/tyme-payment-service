package com.tyme.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tyme.payment.model.IdempotencyOutcome;
import com.tyme.payment.model.PaymentRequest;
import com.tyme.payment.model.PaymentResponse;
import com.tyme.payment.service.PaymentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Requirement 1: Should return 400 Bad Request when Idempotency-Key is missing")
    void shouldReturn400_WhenHeaderIsMissing() throws Exception {
        PaymentRequest request = new PaymentRequest("ACC1", new BigDecimal("100"), "PHP", "DEST2");

        mockMvc.perform(post("/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_MISSING"));
    }

    @Test
    @DisplayName("Success Flow: Should return 201 Created for a new transaction")
    void shouldReturn201_WhenTransactionIsNew() throws Exception {
        String key = UUID.randomUUID().toString();
        PaymentRequest request = new PaymentRequest("ACC1", new BigDecimal("100"), "PHP", "DEST2");
        PaymentResponse response = new PaymentResponse("TXN-123", "SUCCESS", new BigDecimal("100"), "PHP", LocalDateTime.now());

        when(paymentService.processPayment(eq(key), any()))
                .thenReturn(new IdempotencyOutcome<>(response, IdempotencyOutcome.OutcomeStatus.CREATED));

        mockMvc.perform(post("/v1/payments")
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionId").value("TXN-123"));
    }

    @Test
    @DisplayName("Requirement 4: Should return 409 Conflict when transaction is IN_PROGRESS")
    void shouldReturn409_WhenInProgress() throws Exception {
        String key = "processing-key";
        PaymentRequest request = new PaymentRequest("ACC1", new BigDecimal("100"), "PHP", "DEST2");

        when(paymentService.processPayment(eq(key), any()))
                .thenReturn(new IdempotencyOutcome<>(null, IdempotencyOutcome.OutcomeStatus.IN_PROGRESS));

        mockMvc.perform(post("/v1/payments")
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PROCESSING"));
    }

    @Test
    @DisplayName("Requirement 2: Should return 422 Unprocessable Entity on Data Mismatch")
    void shouldReturn422_OnDataMismatch() throws Exception {
        String key = "reused-key";
        PaymentRequest request = new PaymentRequest("ACC1", new BigDecimal("100"), "PHP", "DEST2");

        when(paymentService.processPayment(eq(key), any()))
                .thenReturn(new IdempotencyOutcome<>(null, IdempotencyOutcome.OutcomeStatus.DATA_MISMATCH));

        mockMvc.perform(post("/v1/payments")
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("KEY_REUSE"));
    }
}