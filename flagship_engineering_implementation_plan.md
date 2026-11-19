# FlagShip — Engineering & Implementation Plan

## 1. Executive Summary & Architectural Vision
**FlagShip** is a self-hosted feature-flag and gradual-rollout platform consisting of a central control-plane server, an Admin Web UI, a shared deterministic evaluation core, and an auto-configured **Spring Boot Starter**.

To guarantee **QA4 (100% evaluation parity between starter and server)** and **QA1 ($\le 50\,\mu\text{s}$ p99 evaluation, $\le 20\text{ MB}$ heap for 1,000 flags)**, FlagShip uses a **multi-module Maven architecture** where the core evaluation engine (`flagship-core`) is pure, zero-framework Java shared by both `flagship-server` and `flagship-spring-boot-starter`.

### CAP Theorem Trade-off
- **Control Plane (`flagship-server`):** Favors **Consistency (CP)** for administrative mutations, approvals, and version history using PostgreSQL transactions.
- **Data Plane (`flagship-spring-boot-starter`):** Favors **Availability & Partition Tolerance (AP)** with **Eventual Consistency**. Clients evaluate locally against immutable in-memory snapshots backed by a local disk cache, ensuring zero downtime or exceptions during a control-plane outage (**QA2**).

---

## 2. Technology Stack
- **Language & Runtime:** Java 21 (Virtual Threads enabled for high-concurrency SSE fan-out)
- **Framework:** Spring Boot 3.4.x
- **Build Tool:** Maven (Multi-module project)
- **Database:** PostgreSQL 16 (Production/Docker) + H2 / Testcontainers (Integration Tests) + Flyway Migrations
- **Security:** Spring Security 6 (JWT for Admin UI, Environment-scoped API Keys for SDK/Starter clients)
- **Observability:** Spring Boot Actuator + Micrometer
- **Admin Web UI (MVVM SPA):** React 18 + TypeScript + Vite + Tailwind CSS (served standalone or bundled)
- **Benchmarking & Testing:** JUnit 5, Mockito, Testcontainers, JMH (Java Microbenchmark Harness for QA1)

---

## 3. Multi-Module Project Structure
```text
flagship/
├── pom.xml                             # Parent POM (dependency management, plugins)
├── flagship-core/                      # Pure Java: Domain models, Rule AST, Evaluation Engine, Hashing
├── flagship-spring-boot-starter/       # Auto-config, @FeatureFlag AOP, SSE/Polling Client, Disk Cache, Actuator, Micrometer
├── flagship-server/                    # Control plane API, SSE Fan-out, Change Control, Experiments, Hygiene, Security
├── flagship-admin-ui/                  # React + TypeScript Admin SPA (Projects, Flags, Segments, Approvals, Experiments)
├── flagship-demo-app/                  # Sample Spring Boot app proving QA5 (15-min quick-start)
├── flagship-benchmarks/                # JMH benchmarks (QA1), Consistency verifier (QA4), 50-instance SSE load test (QA3)
├── docs/                               # Quick-start guide, CAP theorem ADR, Singleton position ADR, Pattern mapping
└── docker-compose.yml                  # PostgreSQL + FlagShip Server + Admin UI
```

---

## 4. Design Patterns Mapping (Mandatory Course Requirements)
| Pattern | Module | Implementation Detail |
| :--- | :--- | :--- |
| **Composite + Visitor** | `flagship-core` | `RuleNode` interface with `CompositeRuleNode` (`AND`/`OR`) and `LeafConditionNode`; `RuleEvaluationVisitor` evaluates trees, `RuleValidationVisitor` validates syntax. |
| **Chain of Responsibility** | `flagship-core` | `EvaluationHandler` chain in strict FR2 order: `KillSwitchHandler` $\rightarrow$ `UserTargetHandler` $\rightarrow$ `SegmentRuleHandler` $\rightarrow$ `PercentageRolloutHandler` $\rightarrow$ `DefaultVariationHandler`. |
| **Strategy** | `flagship-core` | `OperatorStrategy` (`EQUALS`, `IN`, `CONTAINS`, `SEMVER_*`, `REGEX`, `DATE_BEFORE`, `DATE_AFTER`) and `BucketingStrategy` (`MurmurHash3BucketingStrategy`). |
| **Observer** | `core` & `starter` & `server` | `FlagChangeListener` notified on snapshot updates in starter; `SseBroadcaster` pushes `FlagUpdatedEvent` to subscribers in server. |
| **Decorator / Proxy** | `flagship-spring-boot-starter` | `MeteredFlagShipClient` (Micrometer metrics) wraps `ResilientFlagShipClient` (zero-exception fallback) wraps `SnapshotFlagShipClient` (in-memory evaluation). |
| **Command** | `flagship-server` | `FlagChangeCommand` encapsulating create/update/toggle/kill-switch with `execute()`, `computeDiff()`, `rollback()`, and scheduled execution support. |
| **Builder** | `flagship-core` | Fluent `EvaluationContext.builder().userKey(...).attribute(...).build()`. |
| **Singleton Position** | `docs/ADR-002-singleton.md` | Documented architectural decision: Reject GoF static `getInstance()` in favor of Spring IoC singleton-scoped beans for testability, lifecycle management, and multi-context isolation. |

---

## 5. Phased Implementation Roadmap

### Phase 1: Core Evaluation Engine (`flagship-core`)
1. Define immutable domain records: `FlagDefinition`, `Variation`, `FlagType` (`BOOLEAN`, `STRING`, `NUMBER`, `JSON`), `EnvironmentSnapshot`, `SegmentDefinition`, `EvaluationContext`, `EvaluationResult`.
2. Implement **Builder** for `EvaluationContext`.
3. Implement **Strategy** for all attribute operators (including SemVer parser and ISO-8601 date comparison) and deterministic **MurmurHash3 (32-bit)** bucketing (`bucket = unsigned(murmur3(flagKey + ":" + userKey)) % 10000`, representing `0.00%` to `99.99%`).
4. Implement **Composite + Visitor** for nested `AND`/`OR` segment rules.
5. Implement **Chain of Responsibility** strictly enforcing FR2 order.
6. Write unit tests verifying FR2, FR3 sticky rollouts, and 1,000,000-key monotonicity (10% $\rightarrow$ 20%).

### Phase 2: Control-Plane Server (`flagship-server`)
1. **Database Schema (Flyway):** `users`, `projects`, `project_memberships`, `environments`, `flags`, `flag_env_configs`, `segments`, `change_requests`, `flag_versions`, `scheduled_changes`, `experiment_events`, `flag_evaluation_stats`.
2. **Security & RBAC (FR10):** JWT auth for Admin API; per-project role enforcement (`VIEWER`, `EDITOR`, `APPROVER`, `ADMIN`) via custom Spring Security `@PreAuthorize` evaluators; API Key authentication for SDK endpoints (`/api/sdk/**`).
3. **Flags & Segments CRUD (FR1, FR2):** Full REST API + Server-side Evaluation Preview endpoint (using `flagship-core`).
4. **Change Control (FR7):**
   - Command pattern for all mutations with JSON Patch/diff generation, author, and reason.
   - Version history table + one-click restore command.
   - Production approval workflow: `EDITOR` creates `PENDING_APPROVAL` change request; a different user with `APPROVER`/`ADMIN` role approves/rejects it.
   - Scheduled changes executed by a `@Scheduled` job (with ShedLock or DB row locking).
5. **Live Updates SSE Fan-out (FR5, QA3):**
   - Enable Java 21 Virtual Threads (`spring.threads.virtual.enabled=true`).
   - `SseEmitter` registry keyed by `projectId:envKey` using `CopyOnWriteArraySet`.
   - Heartbeat ping every 15s; instant broadcast on flag commit/kill-switch/restore/scheduled apply.
   - Polling fallback endpoint (`GET /api/sdk/v1/snapshot` with `ETag` / `If-None-Match` version header).
6. **Experiments Engine (FR8):**
   - Batch ingestion endpoint (`POST /api/sdk/v1/events`) for `EXPOSURE` and `GOAL` events.
   - Analytics service computing: conversion rate per variation, **Chi-square goodness-of-fit test for Sample-Ratio Mismatch (SRM)** ($p < 0.01$ alert), and **Two-proportion Z-test** for statistical significance ($p$-value and 95% confidence interval).
7. **Flag Hygiene Engine (FR9):**
   - Track `last_evaluated_at` (updated via SDK telemetry/events) and `fully_rolled_out_since` (updated when flag hits 100% single variation).
   - Scheduled job & API listing stale flags (unused $> 30$ days OR 100% rolled out $> 14$ days) with owner info.

### Phase 3: Spring Boot Starter (`flagship-spring-boot-starter`)
1. **Auto-Configuration (FR4):** Register `FlagShipAutoConfiguration` in `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` using `@ConditionalOnClass`, `@ConditionalOnProperty`, and `@ConditionalOnMissingBean`.
2. **Configuration Properties:** `@ConfigurationProperties(prefix = "flagship")` (`serverUrl`, `apiKey`, `startupTimeoutMs` capped at $\le 2000\text{ ms}$, `cacheFilePath`, `pollingIntervalMs`, `sseEnabled`, `eventFlushIntervalMs`).
3. **Resilient Sync & Offline Snapshot (FR5, FR6, QA2):**
   - Startup bootstrap: fetch initial snapshot with strict 2-second timeout; on failure, seamlessly load from local JSON disk cache; if absent, start with empty snapshot (never crash app).
   - Background SSE listener with exponential backoff reconnect; automatic fallback to polling when SSE is disconnected.
   - Atomic `AtomicReference<EnvironmentSnapshot>` swap on update + async atomic file write (`temp` file + `Files.move`) to disk.
4. **Client Decorator Stack:** `MeteredFlagShipClient` $\rightarrow$ `ResilientFlagShipClient` (catches all throwables, returns default) $\rightarrow$ `SnapshotFlagShipClient` (evaluates via `flagship-core`).
5. **AOP Method Guarding:** `@FeatureFlag(key = "...", defaultValue = false, fallbackMethod = "...")` + `FeatureFlagAspect` extracting `EvaluationContext` via pluggable `EvaluationContextProvider`.
6. **Observability:**
   - Custom `@Endpoint(id = "flagship")` Actuator endpoint exposing loaded flags, snapshot version, last sync timestamp, sync mode (`SSE`/`POLLING`/`OFFLINE`), and server reachability.
   - Micrometer counters/timers: `flagship.evaluations.total`, `flagship.evaluation.duration`, `flagship.snapshot.age.seconds`, `flagship.sync.errors`.
7. **Event Batcher (FR8):** Non-blocking bounded queue batching exposure/goal events and flushing periodically to server (dropping gracefully if server is down).

### Phase 4: Admin Web Application (`flagship-admin-ui`)
1. Clean React + TypeScript + Tailwind SPA with dark/light modern dashboard UI.
2. Views:
   - Login & Project Switcher (role-aware UI hiding unauthorized actions).
   - Flags Dashboard (environment tabs: Dev / Staging / Prod, instant Kill-Switch toggle, stale badges).
   - Flag Editor (Variations, Kill Switch, Individual Targets, Visual Segment Rule Builder, Sticky Rollout Slider, Live Evaluation Preview tester).
   - Change Control & Audit Log (Pending Approvals queue with visual JSON Diff, Version History with 1-click Restore, Scheduled Changes).
   - Segments Manager.
   - Experiments Dashboard (Conversion table, SRM warning badge, p-value significance indicator).
   - Flag Hygiene Report (Stale flags list with owner and reason).
   - Team & RBAC Management (`viewer`, `editor`, `approver`, `admin`).

### Phase 5: Verification, Benchmarks & Quick-Start (QA1 – QA5)
1. **QA1 Benchmark (`flagship-benchmarks`):** JMH benchmark evaluating a 10-rule flag (assert p99 $\le 50\,\mu\text{s}$) + heap measurement test loading 1,000 flags (assert retained heap $\le 20\text{ MB}$).
2. **QA2 Outage Test:** Integration test stopping server/container, evaluating flags, restarting client app from disk snapshot, asserting zero exceptions.
3. **QA3 Propagation Test:** Spin up 50 concurrent SSE client instances, trigger kill switch, measure p95 arrival latency (assert $\le 5\text{ s}$).
4. **QA4 Parity & Monotonicity Test:** Evaluate 1,000,000 synthetic user keys on both starter and server preview engine; assert 100% exact match and 100% stickiness when ramping 10% $\rightarrow$ 20%.
5. **QA5 Adoption & Demo (`flagship-demo-app` + `docs/QUICKSTART.md`):** Step-by-step quick-start guide verified end-to-end in under 15 minutes.