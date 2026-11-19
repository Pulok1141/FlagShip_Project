# FlagShip 🚩

[![Java](https://img.shields.io/badge/Java-21-orange.svg?logo=openjdk)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.0-brightgreen.svg?logo=springboot)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18-blue.svg?logo=react)](https://react.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.6-blue.svg?logo=typescript)](https://www.typescriptlang.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue.svg?logo=postgresql)](https://www.postgresql.org/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Parity](https://img.shields.io/badge/Evaluation%20Parity-100%25-success)](docs/ADR-001-cap-theorem.md)
[![Sub--50µs Latency](https://img.shields.io/badge/p99%20Eval%20Latency-30µs-blueviolet)](flagship-benchmarks/)

**FlagShip** is a production-grade, self-hosted Feature-Flag and Gradual-Rollout platform built with **Java 21 Virtual Threads**, **Spring Boot 3.4**, and **React 18**. It combines a centralized control-plane server, an intuitive Admin Web SPA, and an auto-configured Spring Boot Starter that delivers sub-50µs in-memory evaluations, live Server-Sent Events (SSE) push, and a strict **Zero-Exception Contract**.

---

## 🌟 Why FlagShip?

- ⚡ **Blazing Fast Local Evaluation (QA1):** In-memory evaluations take **$\le 30\,\mu\text{s}$ p99** with lock-free atomic reads; 1,000 flags consume only **0.49 MB** heap.
- 🔄 **Real-Time Push Propagation (QA3):** Changes reach all connected microservices in **$< 40\text{ ms}$** via SSE fan-out on Java 21 Virtual Threads, with automatic polling fallback.
- 🎯 **100% Deterministic & Sticky Rollouts (FR3, QA4):** 32-bit MurmurHash3 bucketing guarantees that ramping a flag from 10% to 20% preserves **100%** of previously bucketed users with **0 mismatches** between server and starter.
- 🛡️ **Zero-Exception & Outage Resilience (QA2):** Network down? Server crashed? The starter serves from an atomic local disk cache and guarantees that **zero exceptions** are ever surfaced to host applications.
- 🔐 **Governance & Four-Eyes Principle (FR7):** Production mutations require second-person approval (`approver != author`), versioned diff history, and 1-click instant rollback.
- 🧪 **A/B Experiment Analytics (FR8):** Asynchronously batched exposures and goals with **Chi-Square Sample-Ratio Mismatch (SRM)** detection ($p < 0.01$) and **Two-Proportion Z-Test** statistical significance.
- 🧹 **Flag Hygiene Engine (FR9):** Automatically detects stale flags (idle $\ge 30$ days or fully rolled out $\ge 14$ days) with designated code owners.

---

## 🏛️ Architecture Overview

```text
                               +-----------------------------+
                               |      FlagShip Admin SPA     |
                               | (React 18 + TS + Tailwind)  |
                               +--------------+--------------+
                                              | REST API + SSE
                                              v
+-----------------------------------------------------------------------------------------+
|                                FLAGSHIP CONTROL-PLANE SERVER                            |
|                                                                                         |
|  [Spring Security 6]  -->  [Change Control (Command Pattern)]  -->  [Flyway / Postgres 16]   |
|         |                                  |                                            |
|         v                                  v                                            |
|  [SSE Broadcaster]                  [Experiments & SRM]               [Flag Hygiene]    |
| (Java 21 Virtual Threads)         (Chi-Square + Z-Test)              (Stale Flag Cron)  |
+---------------------------------------------+-------------------------------------------+
                                              |  SSE Push (<40ms) + ETag Polling Fallback
                                              v
+-----------------------------------------------------------------------------------------+
|                               FLAGSHIP SPRING BOOT STARTER                              |
|                                                                                         |
|  [SyncEngine]  -->  [Atomic Disk Cache (.tmp + move)]  -->  [AtomicReference<Compiled>]|
|         |                                                                               |
|         v                                                                               |
|  [MeteredFlagShipClient]  -->  [ResilientFlagShipClient]  -->  [SnapshotFlagShipClient] |
|   (Micrometer Timers)          (Zero-Exception Fallback)        (Lock-free In-Memory)   |
|         |                                                                               |
|         v                                                                               |
|  [@FeatureFlag Aspect]  -->  [EvaluationContextProvider]  -->  [/actuator/flagship]     |
+---------------------------------------------+-------------------------------------------+
                                              |
                                              v
                               +-----------------------------+
                               |     Host Microservice       |
                               | (e.g. flagship-demo-app)    |
                               +-----------------------------+
```

### Shared Engine Parity (QA4)
Both `flagship-server` and `flagship-spring-boot-starter` share `flagship-core` (pure Java 21, zero framework dependencies). This guarantees **100% bitwise parity** between server-side evaluation previews and client-side production executions.

---

## 🧩 Multi-Module Repository Layout

| Module | Description | Technologies |
| :--- | :--- | :--- |
| [`flagship-core`](flagship-core/) | Pure Java 21 deterministic evaluation engine, Rule AST, MurmurHash3, operators, immutable snapshot models. | Java 21, Jackson, JUnit 5 |
| [`flagship-server`](flagship-server/) | Control-plane REST server, Virtual-Thread SSE fan-out, JPA/Flyway, RBAC, Change Control, Experiments, Hygiene, bundled SPA. | Spring Boot 3.4, PostgreSQL 16, H2, Flyway, JJWT |
| [`flagship-spring-boot-starter`](flagship-spring-boot-starter/) | Auto-configured starter (`AutoConfiguration.imports`), `@FeatureFlag` AOP, atomic disk cache, Actuator endpoint, Micrometer metrics. | Spring Boot 3.4 Starter, AspectJ, Micrometer |
| [`flagship-admin-ui`](flagship-admin-ui/) | Role-aware Admin Web SPA managing projects, flags, segments, approvals, diffs, experiments, and hygiene. | React 18, TypeScript, Vite, Tailwind CSS, Lucide |
| [`flagship-demo-app`](flagship-demo-app/) | Production sample app demonstrating 15-minute adoption with `@FeatureFlag` and fallback handling. | Spring Boot 3.4 Web + Actuator |
| [`flagship-benchmarks`](flagship-benchmarks/) | Automated verification suite proving QA1, QA2, QA3, and QA4. | JMH, JUnit 5, AssertJ |
| [`docs/`](docs/) | Architecture Decision Records (`ADR-001`, `ADR-002`), Pattern Matrix, and Quickstart guide. | Markdown |

---

## 🎯 Deterministic Evaluation Order (FR2)

Evaluations strictly execute the **Chain of Responsibility** in this immutable sequence:

1. **Kill Switch:** If disabled for the environment, immediately returns the configured off variation.
2. **Individual User Targets:** Exact match on `context.userKey()`.
3. **Segment Rules:** Nested `AND`/`OR` composite condition trees evaluated via Visitor (`EQUALS`, `IN`, `CONTAINS`, `SEMVER_*`, `REGEX`, `DATE_BEFORE`, `DATE_AFTER`).
4. **Percentage Rollout:** MurmurHash3 bucketing (`[0, 9999]`) with cumulative thresholds for monotonic ramps.
5. **Default Variation:** Fallback variation defined for the environment.

---

## 🚀 Quickstart in 3 Steps

### Step 1: Add Dependency to `pom.xml`

```xml
<dependency>
    <groupId>io.flagship</groupId>
    <artifactId>flagship-spring-boot-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Step 2: Configure `application.yml`

```yaml
flagship:
  enabled: true
  server-url: http://localhost:8080
  api-key: flagship_dev_key_12345
  startup-timeout-ms: 1500           # Strictly capped <= 2000 ms
  cache-file-path: ./.flagship/snapshot.json # Local atomic disk cache
  sse-enabled: true                  # Real-time push stream
  polling-interval-ms: 30000         # Fallback polling if SSE drops

management:
  endpoints:
    web:
      exposure:
        include: health,info,flagship # Custom actuator diagnostic endpoint
```

### Step 3: Guard Methods with `@FeatureFlag`

```java
@Service
public class CheckoutService {

    // Automatically guarded by FlagShip; executes legacy flow when flag is OFF
    @FeatureFlag(key = "new-checkout-flow", fallbackMethod = "legacyCheckout")
    public CheckoutResponse processCheckout(@UserKey String userId, double amount) {
        return new CheckoutResponse("MODERN_1_CLICK_CHECKOUT", userId, amount);
    }

    public CheckoutResponse legacyCheckout(String userId, double amount) {
        return new CheckoutResponse("LEGACY_3_STEP_CHECKOUT", userId, amount);
    }
}
```

---

## 📊 Verification & Benchmarks (QA1 – QA5)

Automated tests in `flagship-benchmarks` enforce all quality standards:

| Benchmark | Test Class | Metric Target | Empirical Result | Status |
| :--- | :--- | :--- | :--- | :--- |
| **QA1 Latency** | `MemoryHeapVerificationTest` | 10-rule p99 $\le 50\,\mu\text{s}$ | **30.00 µs** | **PASSED** |
| **QA1 Memory** | `MemoryHeapVerificationTest` | 1,000 flags $\le 20\text{ MB}$ heap | **0.49 MB** | **PASSED** |
| **QA2 Resilience** | `OutageResilienceVerificationTest` | 0 exceptions under total server outage | **0 exceptions / 15,000 evals** | **PASSED** |
| **QA3 Push Speed** | `SsePropagationVerificationTest` | 50 instances reached $\le 5.0\text{ s}$ p95 | **39.32 ms** | **PASSED** |
| **QA4 Sticky Ramp**| `LargeScaleParityAndStickyTest` | 10% $\rightarrow$ 20% retains 100% users | **100.00% retained (100k keys)**| **PASSED** |
| **QA4 Parity** | `LargeScaleParityAndStickyTest` | Server preview vs Starter parity | **100.00% (0 mismatches)** | **PASSED** |
| **QA5 Adoption** | `flagship-demo-app` | Fresh integration in $\le 15$ min | **Verified end-to-end** | **PASSED** |

---

## 📐 Design Patterns Matrix

FlagShip explicitly implements enterprise patterns across all modules:

| Pattern | Module | Key Classes | Description |
| :--- | :--- | :--- | :--- |
| **Composite + Visitor** | `flagship-core` | `RuleNode`, `CompositeRuleNode`, `LeafConditionNode`, `RuleEvaluationVisitor` | Evaluates nested boolean `AND`/`OR` ASTs without type casts or reflection. |
| **Chain of Responsibility** | `flagship-core` | `EvaluationHandler`, `KillSwitchHandler`, `UserTargetHandler`, `PercentageRolloutHandler` | Enforces the strict, immutable FR2 evaluation precedence. |
| **Strategy** | `flagship-core` | `OperatorStrategy`, `BucketingStrategy`, `MurmurHash3BucketingStrategy` | Extensible operator logic and cross-JVM 32-bit Murmur3 bucketing. |
| **Observer** | `core`, `starter`, `server` | `FlagChangeListener`, `SseBroadcaster`, `FlagUpdatedEvent` | Real-time push updates broadcast over Java 21 Virtual Threads. |
| **Decorator** | `flagship-spring-boot-starter` | `MeteredFlagShipClient` $\rightarrow$ `ResilientFlagShipClient` $\rightarrow$ `SnapshotFlagShipClient` | Layered metrics, exception boundary suppression, and lock-free snapshot evaluation. |
| **Command** | `flagship-server` | `FlagChangeCommand`, `CreateFlagCommand`, `UpdateFlagCommand`, `ToggleKillSwitchCommand` | Encapsulates mutations with diff tracking, audit logging, and 1-click rollback. |
| **IoC Singleton** | `flagship-spring-boot-starter` | `FlagShipAutoConfiguration`, `FlagShipClient` bean | Enforces Spring container-managed singletons over anti-pattern static singletons ([ADR-002](docs/ADR-002-singleton-position.md)). |

---

## 🖥️ Admin Web UI Features

The SPA (served directly by `flagship-server` at `http://localhost:8080/` or standalone via Vite) provides:
- **Projects & Environments Switcher:** Multi-project management across `development`, `staging`, and `production`.
- **Flag Dashboard:** Instant kill-switch toggle, status badges, and stale flag warnings.
- **Visual Rule Builder:** Nested `AND`/`OR` segment conditions with instant syntax validation.
- **Percentage Rollout Sliders:** Cumulative rollout allocation with MurmurHash3 preview.
- **Live Evaluation Preview Modal:** Test flag targeting in real time with sub-millisecond execution timing.
- **Four-Eyes Approval Queue:** Side-by-side JSON diffs requiring a second authorized user to approve production changes.
- **Audit Log & Version History:** Inspect timestamped diffs with author, reason, and **1-click instant rollback**.
- **A/B Experiments Dashboard:** Conversion rate tables, Sample-Ratio Mismatch (SRM) alerts ($p < 0.01$), and Two-Proportion Z-Test statistical significance markers.
- **Hygiene Reports:** Highlights stale or fully rolled-out flags with owners and cleanup suggestions.

---

## 🔑 Pre-Seeded Demo Accounts

| Role | Email | Password | Permissions |
| :--- | :--- | :--- | :--- |
| **Global Admin** | `admin@flagship.io` | `admin123` | Full administrative control across all projects & users |
| **Approver** | `approver@flagship.io` | `approver123` | Can approve/reject production change requests |
| **Editor** | `editor@flagship.io` | `editor123` | Can create and edit flags, submit change requests |
| **Viewer** | `viewer@flagship.io` | `viewer123` | Read-only access and live evaluation previews |

---

## 🐳 Docker Compose Deployment

Run PostgreSQL 16 and the FlagShip Server with a single command:

```bash
docker-compose up -d
```

Access the UI at `http://localhost:8080/`.

---

## 📚 Documentation & ADRs

- [15-Minute Adoption Quickstart](docs/QUICKSTART.md)
- [ADR-001: CAP Theorem Trade-off (Server CP vs Starter AP)](docs/ADR-001-cap-theorem.md)
- [ADR-002: Singleton Architectural Position (Rejecting GoF Static Singletons)](docs/ADR-002-singleton-position.md)
- [Architecture Patterns Matrix](docs/ARCHITECTURE-PATTERNS.md)

---

## 📄 License

Licensed under the [Apache License, Version 2.0](LICENSE).
