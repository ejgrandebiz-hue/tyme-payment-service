package com.tyme.payment.service;

import com.tyme.payment.BaseRedisIntegrationTest;
import com.tyme.payment.model.IdempotencyOutcome;
import com.tyme.payment.model.PaymentRequest;
import com.tyme.payment.model.PaymentResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class PaymentServiceIntegrationTest extends BaseRedisIntegrationTest {

    @Autowired
    private PaymentService paymentService;

    @Test
    @DisplayName("Requirement 3: Return cached response when same key is reused")
    void shouldReturnCachedResponse_WhenKeyIsReused() {
        // Arrange
        String key = "txn_" + UUID.randomUUID();
        PaymentRequest request = new PaymentRequest("ACC-001", new BigDecimal("1500.00"), "PHP", "DEST-999");

        // Act - First attempt (Requirement 2: Store request/response)
        var firstOutcome = paymentService.processPayment(key, request);

        // Act - Second attempt (Requirement 3: Return response when key is reused)
        var secondOutcome = paymentService.processPayment(key, request);

        // Assert
        assertEquals(IdempotencyOutcome.OutcomeStatus.CREATED, firstOutcome.status());
        assertEquals(IdempotencyOutcome.OutcomeStatus.ALREADY_EXISTS, secondOutcome.status());
        assertEquals(firstOutcome.data().transactionId(), secondOutcome.data().transactionId(),
                "The replayed transaction ID must be identical to the original.");
    }

    @Test
    @DisplayName("Requirement 4: Handle concurrent requests with the same key using Virtual Threads")
    void shouldHandleConcurrency_Correctly() throws InterruptedException {
        // Arrange
        String key = "race-condition-" + UUID.randomUUID();
        PaymentRequest request = new PaymentRequest("ACC-1", new BigDecimal("50.00"), "PHP", "DEST-2");

        int threadCount = 10;
        // Best Practice: Use Virtual Thread Executor for Java 21
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            CountDownLatch startGate = new CountDownLatch(1);
            CountDownLatch endGate = new CountDownLatch(threadCount);
            List<IdempotencyOutcome<PaymentResponse>> outcomes = Collections.synchronizedList(new ArrayList<>());

            // Act
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        startGate.await(); // Wait for the "Go" signal to ensure a race condition
                        outcomes.add(paymentService.processPayment(key, request));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        endGate.countDown();
                    }
                });
            }

            startGate.countDown(); // Bang! All threads hit the service at once
            boolean completed = endGate.await(10, TimeUnit.SECONDS);

            // Assert
            assertTrue(completed, "Threads failed to complete in time.");

            long createdCount = outcomes.stream()
                    .filter(o -> o.status() == IdempotencyOutcome.OutcomeStatus.CREATED).count();
            long inProgressCount = outcomes.stream()
                    .filter(o -> o.status() == IdempotencyOutcome.OutcomeStatus.IN_PROGRESS).count();

            // Requirement 4: Exactly one must succeed, others must be blocked/conflict
            assertEquals(1, createdCount, "Exactly one request should have been processed.");
            assertTrue(inProgressCount >= 1, "Concurrent requests should have encountered IN_PROGRESS status.");
        }
    }

    @Test
    @DisplayName("Integrity Check: Detect if same key is used for different data (Payload Mismatch)")
    void shouldDetectMismatch_WhenPayloadChanges() {
        // Arrange
        String key = "tamper-key-" + UUID.randomUUID();
        PaymentRequest original = new PaymentRequest("ACC-1", new BigDecimal("100"), "PHP", "DEST-2");
        PaymentRequest modified = new PaymentRequest("ACC-1", new BigDecimal("9999"), "PHP", "DEST-2"); // Tampered amount

        // Act
        paymentService.processPayment(key, original);
        var mismatchOutcome = paymentService.processPayment(key, modified);

        // Assert
        assertEquals(IdempotencyOutcome.OutcomeStatus.DATA_MISMATCH, mismatchOutcome.status(),
                "Service must reject key reuse if the payload fingerprint has changed.");
    }

    @Test
    @DisplayName("Requirement 5: Ensure keys are stored with expiration (Manual deletion simulation)")
    void shouldAllowReprocessing_AfterKeyIsRemoved() {
        // Arrange
        String key = "expiry-test-" + UUID.randomUUID();
        PaymentRequest request = new PaymentRequest("A1", new BigDecimal("10"), "PHP", "B2");

        // Act 1: Process o
        paymentService.processPayment(key, request);

        // Act 2: Manually remove the key (Requirement 5: Simulation of TTL expiration)
        // In a real environment, Redis handles this automatically via the configured 24h TTL.
        paymentService.clearKeyManual(key);

        // Act 3: Process again
        var outcome = paymentService.processPayment(key, request);

        // Assert
        assertEquals(IdempotencyOutcome.OutcomeStatus.CREATED, outcome.status(),
                "After expiration, the same key should be treated as a new request.");
    }
}