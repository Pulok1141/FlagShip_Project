# Memory.md — FlagShip Project Context & State

## 1. Project Identity
- **Name:** FlagShip (Project 3)
- **Goal:** Self-hosted Feature-Flag & Gradual-Rollout Platform with a Spring Boot Starter.
- **Core Value Proposition:** Sub-50µs local flag evaluation, 5-second live SSE propagation, full offline resilience (zero outages), governance/approvals, A/B experiments, and flag hygiene.

---

## 2. Architecture & Module Map
| Module | Responsibility | Status |
| :--- | :--- | :--- |
| `flagship-core` | Pure Java evaluation engine, AST, operators, MurmurHash3 bucketing, immutable snapshot models. | `[x] Completed (20/20 tests passing, 1M-key monotonicity verified)` |
| `flagship-server` | Spring Boot 3.4 control plane, PostgreSQL/Flyway, RBAC, Change Control, SSE Fan-out, Experiments, Hygiene. | `[x] Completed (7/7 tests passing: E2E Auth/SDK ETag/Events, 4-Eyes RBAC, Chi-Square SRM + Z-Test, Hygiene, SSE fan-out)` |
| `flagship-spring-boot-starter` | Auto-configuration, `@FeatureFlag` AOP, SSE+Polling sync, atomic disk cache, Actuator, Micrometer, Event batcher. | `[x] Completed (5/5 tests passing: AutoConfig, @FeatureFlag AOP, Zero-Exception Resilience, Startup Timeout <2s, Disk Cache Outage Recovery)` |
| `flagship-admin-ui` | React + TypeScript + Tailwind SPA for full management & analytics. | `[ ] Pending` |
| `flagship-demo-app` | Clean sample app demonstrating 15-minute starter adoption (QA5). | `[ ] Pending` |
| `flagship-benchmarks` | JMH latency/memory benchmarks (QA1), 50-client SSE test (QA3), 1M-key parity test (QA4). | `[ ] Pending` |

---

## 3. Key Architectural Decisions (ADRs)
1. **ADR-001 (CAP Trade-off):** Server is CP (PostgreSQL ACID for mutations/approvals); Starter clients are AP with Eventual Consistency (immutable in-memory snapshots + local disk fallback).
2. **ADR-002 (Singleton Position):** Rejected GoF static `Singleton.getInstance()` because it introduces hidden global state, breaks unit testing, and conflicts with Spring's IoC container. Used Spring-managed singleton beans instead.
3. **ADR-003 (Deterministic Hashing):** Selected 32-bit MurmurHash3 (`seed = 0`) over `flagKey + ":" + userKey` modulo `10000` (0.01% granularity). Guarantees cross-JVM consistency and monotonic rollouts (10% $\rightarrow$ 20% preserves original 10%).
4. **ADR-004 (SSE Concurrency):** Selected Spring MVC with Java 21 Virtual Threads (`spring.threads.virtual.enabled=true`) for SSE fan-out—provides high-concurrency scalability for 50+ long-lived connections with simpler debugging and JPA compatibility than WebFlux.
5. **ADR-005 (Compiled Snapshot for QA1):** Upon receiving a JSON snapshot, the client compiles regex `Pattern`s and parses `Instant`/SemVer values once into an immutable `CompiledSnapshot` swapped via `AtomicReference`. Hot-path reads are lock-free and allocation-minimal.

---

## 4. Requirements Traceability Matrix
- **FR1 (Flags):** `projects`, `environments` (`development`, `staging`, `production`), `BOOLEAN`/`STRING`/`NUMBER`/`JSON`, named variations, per-env default.
- **FR2 (Targeting):** Kill Switch $\rightarrow$ User Targets $\rightarrow$ Segment Rules (`equals`, `in`, `contains`, `semver`, `regex`, `date before/after`, `AND`/`OR`) $\rightarrow$ Percentage Rollout $\rightarrow$ Default.
- **FR3 (Sticky Rollouts):** Deterministic MurmurHash3(flagKey, userKey); monotonic ramp.
- **FR4 (Starter):** Auto-config, `@ConfigurationProperties`, `@FeatureFlag` AOP, `/actuator/flagship`, Micrometer metrics.
- **FR5 (Live Updates):** SSE push + polling fallback; $< 5\text{ s}$ propagation.
- **FR6 (Offline Resilience):** In-memory snapshot + local disk persistence; $\le 2\text{ s}$ startup wait.
- **FR7 (Change Control):** Prod second-person approval, versioned diff/author/reason, 1-action restore, scheduled changes.
- **FR8 (Experiments):** Batched exposure & goal events; conversion rate, Chi-square SRM check, Z-test significance.
- **FR9 (Hygiene):** Stale if unused $\ge 30$ days OR 100% rolled out $\ge 14$ days; listed with owner.
- **FR10 (Admin UI):** Manage projects/flags/segments; per-project roles (`VIEWER`, `EDITOR`, `APPROVER`, `ADMIN`).

---

## 5. Quality Targets Checklist
- [ ] **QA1:** 10-rule eval $\le 50\,\mu\text{s}$ p99; 1,000 flags $\le 20\text{ MB}$ heap.
- [ ] **QA2:** 30-min server outage $\rightarrow$ last known values, clean restart from disk snapshot, 0 exceptions.
- [ ] **QA3:** Kill-switch reaches 50 running instances $\le 5\text{ s}$ at p95.
- [ ] **QA4:** 1,000,000 user keys match 100% between starter & server; 10% $\rightarrow$ 20% keeps 100% of original 10%.
- [ ] **QA5:** Fresh Spring Boot app integration in $\le 15$ minutes via `docs/QUICKSTART.md`.

---

## 6. Out of Scope (Do Not Implement)
- Non-Java/mobile SDKs (JS, Python, iOS, Android).
- CDN / Edge worker delivery.
- Bayesian statistical models.