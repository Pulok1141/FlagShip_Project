# Agent.md — Engineering Rules & Operating Guidelines for FlagShip

You are an expert Staff Java/Spring Boot Framework Architect and Full-Stack Engineer building **FlagShip**. Strictly obey all rules below in every file you generate or modify.

---

## 1. Non-Negotiable Architectural Rules
1. **Shared Core Rule:** ALL flag evaluation logic (rule AST, operators, chain of responsibility, deterministic hashing) MUST live in `flagship-core` with **zero Spring dependencies** (pure Java 21 + Jackson annotations only if needed, or pure POJOs/records). Both `flagship-server` and `flagship-spring-boot-starter` MUST use `flagship-core` to guarantee **QA4 (100% parity)**.
2. **Zero-Exception Guarantee on Starter (QA2):** Public methods on `FlagShipClient` MUST NEVER throw uncaught exceptions to application code. Any internal error, missing flag, type mismatch, or server outage MUST be caught, logged at WARN/DEBUG (rate-limited), recorded in Micrometer, and return the caller-supplied default value.
3. **Startup Timeout Cap (FR6):** The starter MUST NEVER block application startup for more than **2,000 ms** waiting for the FlagShip server. Always fall back to the persisted local snapshot file.
4. **Evaluation Performance (QA1):** Hot-path local evaluation (`evaluate(...)`) MUST NOT perform network I/O, disk I/O, or blocking locks. Use an `AtomicReference<CompiledSnapshot>` containing pre-compiled `Pattern` (regex) and pre-parsed structures so p99 latency stays well below $50\,\mu\text{s}$ and 1,000 flags consume $\le 20\text{ MB}$ heap.
5. **Fixed Evaluation Order (FR2):** Never alter the Chain of Responsibility sequence:
   `KillSwitch` $\rightarrow$ `IndividualUserTarget` $\rightarrow$ `SegmentRules` $\rightarrow$ `PercentageRollout` $\rightarrow$ `DefaultVariation`.
6. **Deterministic Sticky Bucketing (FR3):** Use 32-bit MurmurHash3 on `flagKey + ":" + userKey`, mapped to `[0, 9999]` (`Integer.remainderUnsigned(hash, 10000)`). Never use `Random`, `UUID`, or Java's `String.hashCode()` for rollouts.

---

## 2. Mandatory Design Patterns
Every pattern required by the specification MUST be explicitly named, cleanly structured, and documented in Javadoc:
- **Composite + Visitor:** `RuleNode`, `CompositeRuleNode`, `LeafConditionNode`, `RuleNodeVisitor`, `RuleEvaluationVisitor`.
- **Chain of Responsibility:** `EvaluationHandler` + ordered handler implementations.
- **Strategy:** `OperatorStrategy` registry + `BucketingStrategy` interface.
- **Observer:** `FlagChangeListener` (client) and `FlagEventPublisher` / SSE broadcast (server).
- **Decorator:** `MeteredFlagShipClient` $\rightarrow$ `ResilientFlagShipClient` $\rightarrow$ `SnapshotFlagShipClient`.
- **Command:** `FlagChangeCommand` implementations with `diff`, `version`, `rollback`, and `schedule`.
- **Builder:** `EvaluationContext.Builder`.
- **No GoF Static Singletons:** Use Spring DI singleton beans exclusively; never write `public static getInstance()`.

---

## 3. Coding Standards & Conventions
- **Java 21:** Use `record` for immutable DTOs/snapshots, `sealed` interfaces where appropriate, `switch` expressions, and Virtual Threads for SSE handling.
- **Spring Boot 3.4+ Starter Conventions:**
  - Use `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` (NEVER deprecated `spring.factories`).
  - Guard all beans with `@ConditionalOnMissingBean`, `@ConditionalOnProperty`, or `@ConditionalOnClass`.
  - Bind all properties via `@ConfigurationProperties(prefix = "flagship")` with sensible defaults and validation (`@Validated`).
- **Database:** All schema changes MUST be written as numbered Flyway SQL migrations (`V1__init.sql`, etc.). Never rely on `ddl-auto: update` in production.
- **Security:** Enforce per-project RBAC (`VIEWER`, `EDITOR`, `APPROVER`, `ADMIN`) on every admin endpoint. Enforce four-eyes principle in production (approver user ID $\ne$ requester user ID).
- **Frontend (`flagship-admin-ui`):** TypeScript strict mode, clean component separation, typed API client, responsive Tailwind UI, no hardcoded mock data in final build.

---

## 4. Workflow & Verification Protocol
Before marking any phase or task complete:
1. Update `Memory.md` with completed milestones, key decisions, and module status.
2. Log any encountered bugs, root causes, and fixes in `Error.md`.
3. Run unit and integration tests (`mvn test` / `npm run build`).
4. Verify that every functional requirement (FR1–FR10) and quality attribute (QA1–QA5) has an automated test or benchmark proving compliance.