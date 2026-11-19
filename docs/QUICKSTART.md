# FlagShip — 15-Minute Adoption & Quickstart Guide (QA5)

Welcome to **FlagShip** — a production-grade, self-hosted feature flag and gradual rollout platform for Spring Boot and modern web applications.

This guide walks you through integrating the `flagship-spring-boot-starter` into a Spring Boot application and testing feature evaluation in under 15 minutes.

---

## 1. System Requirements
- **Java:** JDK 21+ (Java Virtual Threads supported)
- **Spring Boot:** 3.4.x
- **Build Tool:** Maven 3.9+ or Gradle 8+

---

## 2. Add the Maven Dependency

Add the FlagShip Spring Boot Starter to your application's `pom.xml`:

```xml
<dependency>
    <groupId>io.flagship</groupId>
    <artifactId>flagship-spring-boot-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

---

## 3. Configure `application.yml`

Configure your connection to the FlagShip server and specify your environment's API Key:

```yaml
flagship:
  enabled: true
  server-url: http://localhost:8080
  api-key: dev-sdk-key-secret-12345
  startup-timeout-ms: 1500           # Strictly capped <= 2000 ms (Zero startup hang)
  cache-file-path: ./.flagship/snapshot.json # Local atomic disk cache for offline resilience
  sse-enabled: true                  # Real-time Server-Sent Events push (changes < 5s)
  polling-interval-ms: 30000         # Polling fallback interval if SSE disconnects
  event-flush-interval-ms: 5000      # Async batching flush interval for experiments

management:
  endpoints:
    web:
      exposure:
        include: health,info,flagship # Custom /actuator/flagship endpoint
```

---

## 4. Guarding Methods with `@FeatureFlag`

Use the declarative `@FeatureFlag` annotation to gate methods with automatic fallback handling:

```java
package com.example.demo.service;

import io.flagship.starter.annotation.FeatureFlag;
import org.springframework.stereotype.Service;

@Service
public class CheckoutService {

    // Modern payment flow activated via feature flag
    @FeatureFlag(key = "new-checkout-flow", fallbackMethod = "legacyCheckout")
    public String processOrder(String orderId, double amount) {
        return "Processed order " + orderId + " ($" + amount + ") via NextGen Stripe Checkout";
    }

    // Fallback method executed when the flag is OFF, missing, or during server outage
    public String legacyCheckout(String orderId, double amount) {
        return "Processed order " + orderId + " ($" + amount + ") via Legacy Monolith Gateway";
    }
}
```

---

## 5. Direct Client Programmatic Evaluation

You can also inject `FlagShipClient` directly for granular, programmatic evaluation:

```java
package com.example.demo.controller;

import io.flagship.core.model.EvaluationContext;
import io.flagship.core.model.EvaluationResult;
import io.flagship.starter.client.FlagShipClient;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final FlagShipClient flagShipClient;

    public PaymentController(FlagShipClient flagShipClient) {
        this.flagShipClient = flagShipClient;
    }

    @GetMapping("/route")
    public String getPaymentRoute(@RequestParam String userKey, @RequestParam String country) {
        // Build targeting context
        EvaluationContext ctx = EvaluationContext.builder()
                .userKey(userKey)
                .attribute("country", country)
                .build();

        // Safe evaluation with 0 exceptions guaranteed (Zero-Exception Contract)
        return flagShipClient.getString("payment-gateway", ctx, "default-paypal");
    }

    @PostMapping("/checkout-completed")
    public void recordGoal(@RequestParam String userKey, @RequestParam double orderAmount) {
        // Track conversion goal for A/B experiment analytics
        flagShipClient.track("payment-gateway", userKey, "order-completed", orderAmount);
    }
}
```

---

## 6. Inspecting the Actuator Endpoint

FlagShip provides a custom Spring Boot Actuator endpoint at `/actuator/flagship`:

```bash
curl -s http://localhost:8080/actuator/flagship | jq .
```

Sample Response:
```json
{
  "loadedFlags": 14,
  "snapshotVersion": 3,
  "lastSyncTime": "2026-11-19T14:45:00.123Z",
  "syncMode": "SSE",
  "serverUrl": "http://localhost:8080",
  "status": "HEALTHY"
}
```

---

## 7. Metrics & Observability (Micrometer)

FlagShip automatically registers the following Micrometer metrics:
- `flagship.evaluations.total` (Tags: `flag_key`, `reason`)
- `flagship.evaluation.duration` (Timer recording microsecond evaluation latencies)
- `flagship.snapshot.age.seconds` (Gauge showing age of current local snapshot)
- `flagship.sync.errors` (Counter recording sync reconnects and failures)

View them at `/actuator/metrics/flagship.evaluations.total`.

---

## 8. Outage Resilience & Offline Operation

If the FlagShip server goes offline:
1. The application **will still start** without delay (loads `./.flagship/snapshot.json` or defaults within `<= 1500 ms`).
2. Evaluations continue serving cached values with **zero exceptions**.
3. Reconnection happens automatically in the background using exponential backoff.
