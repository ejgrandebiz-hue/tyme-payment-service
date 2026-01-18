# Tyme Payment Idempotency Service

A high-performance payment gateway built with **Java 21** and **Redis** to ensure "exactly-once" transaction processing.

---

## Prerequisites
* **Java 21** 
* **Maven 3.9+**
* **Docker Desktop** (Running)

---

## Business Rules

1. **Accept Idempotency Key**: Mandatory `Idempotency-Key` header for all payment requests.
2. **Store Request/Response**: Persists the mapping of keys to transaction results.
3. **Replay Logic**: Returns the original response for reused keys to ensure "Exactly-Once" delivery.
4. **Concurrency Handling**: Detects simultaneous requests and returns `409 Conflict` to prevent duplicate processing.
5. **Expiration**: Stored keys expire automatically after 24 hours (Configurable).
---

## How to Run

### 1. Start Redis
Run the provisioning script to start the Docker container:
```
chmod +x setup-redis.sh
./setup-redis.sh
```

### 2. Run the Application
```
mvn spring-boot:run
```


### 2. Test the API
```
curl -X POST http://localhost:8080/v1/payments \
     -H "Idempotency-Key: test-key-001" \
     -H "Content-Type: application/json" \
     -d '{
           "accountId": "ACC-1",
           "amount": 100.0,
           "currency": "PHP",
           "destinationAccount": "DEST-1"
         }'
```

---
## Running Tests

```
mvn test
```

### API Response Codes

| Status | Description |
| :--- | :--- |
| **201 Created** | First time processing success. |
| **200 OK** | Key reused; returning cached result. |
| **400 Bad Request** | Missing header or invalid JSON. |
| **409 Conflict** | Request is already being processed by another thread. |
| **422 Unprocessable Entity** | Key reuse detected with different payload data. |
