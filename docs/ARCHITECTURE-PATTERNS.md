# FlagShip — Design Patterns Architectural Matrix

FlagShip explicitly applies proven Gang of Four (GoF) and Enterprise Integration patterns across its modules. This document details each pattern, where it is implemented, its architectural motivation, and the interfaces involved.

---

## Pattern Summary Matrix

| Pattern | Module | Key Classes / Interfaces | Purpose & Architectural Justification |
| :--- | :--- | :--- | :--- |
| **Composite + Visitor** | `flagship-core` | `RuleNode`, `CompositeRuleNode`, `LeafConditionNode`, `RuleNodeVisitor<T>`, `RuleEvaluationVisitor`, `RuleValidationVisitor` | Enables arbitrary nested boolean ASTs (`AND`/`OR` trees of targeting conditions) evaluated without type checks or coupling logic to node structures. |
| **Chain of Responsibility** | `flagship-core` | `EvaluationHandler`, `KillSwitchHandler`, `UserTargetHandler`, `SegmentRuleHandler`, `PercentageRolloutHandler`, `DefaultVariationHandler` | Guarantees strict, immutable evaluation precedence (FR2) where handlers can resolve immediately or pass control to the successor. |
| **Strategy** | `flagship-core` | `OperatorStrategy`, `OperatorRegistry`, `BucketingStrategy`, `MurmurHash3BucketingStrategy` | Decouples attribute condition evaluation (SemVer, regex, dates) and hashing algorithms from the core evaluation engine. |
| **Observer** | `core`, `starter`, `server` | `FlagChangeListener`, `FlagChangeEvent`, `SseBroadcaster`, `FlagUpdatedEvent` | Decouples update publishers from subscribers. The starter listens for snapshot refreshes; the server broadcasts mutations via Virtual-Thread SSE. |
| **Decorator** | `flagship-spring-boot-starter` | `FlagShipClient`, `MeteredFlagShipClient`, `ResilientFlagShipClient`, `SnapshotFlagShipClient` | Layered separation of concerns: Snapshot evaluation $\rightarrow$ Zero-Exception contract & fallback $\rightarrow$ Micrometer metrics recording. |
| **Command** | `flagship-server` | `FlagChangeCommand`, `CreateFlagCommand`, `UpdateFlagCommand`, `ToggleKillSwitchCommand`, `RestoreVersionCommand` | Encapsulates all mutations into discrete command objects supporting JSON diff generation, audit logs, rollback, and scheduled execution. |
| **Builder** | `flagship-core` | `EvaluationContext.Builder` | Provides a fluent, type-safe API for constructing multi-attribute targeting contexts while maintaining object immutability. |
| **IoC Singleton** | `flagship-spring-boot-starter` | `FlagShipAutoConfiguration`, `FlagShipClient` bean | Enforces lifecycle-managed singleton beans over static GoF singletons, ensuring testability and thread safety (ADR-002). |

---

## Detailed Pattern Implementations

### 1. Composite + Visitor Pattern (`flagship-core`)
- **Problem:** Target segments require complex conditions combining multiple attributes with nested `AND` and `OR` logic (e.g., `(country IN [US, CA] AND appVersion >= 2.0.0) OR betaTester EQUALS true`).
- **Solution:**
  - `RuleNode` is the Component interface defining `<T> T accept(RuleNodeVisitor<T> visitor)`.
  - `CompositeRuleNode` is the Composite containing a `LogicalOperator` (`AND`/`OR`) and a `List<RuleNode>`.
  - `LeafConditionNode` is the Leaf containing `attribute`, `operator`, and `List<String> values`.
  - `RuleEvaluationVisitor` traverses the AST against an `EvaluationContext`, evaluating operators dynamically.
  - `RuleValidationVisitor` validates AST syntax and operator value constraints before saving.

```text
               +-------------------+
               |     RuleNode      |
               +-------------------+
               | + accept(visitor) |
               +---------+---------+
                         |
           +-------------+-------------+
           |                           |
+----------v----------+     +----------v----------+
|  CompositeRuleNode  |     |  LeafConditionNode  |
+---------------------+     +---------------------+
| - operator: AND/OR  |     | - attribute: String |
| - children: List    |     | - operator: Type    |
+---------------------+     | - values: List      |
                            +---------------------+
```

---

### 2. Chain of Responsibility Pattern (`flagship-core`)
- **Problem:** FR2 mandates a fixed, immutable evaluation order:
  1. Kill Switch
  2. Individual User Targets
  3. Segment Rules (Composite AST)
  4. Percentage Rollout
  5. Default Variation
- **Solution:**
  - `EvaluationHandler` abstract class defines `EvaluationResult handle(EvaluationContext ctx, CompiledFlag flag, EvaluationChain chain)`.
  - Each handler inspects the flag configuration. If its criteria match, it returns an `EvaluationResult` with the appropriate `EvaluationReason` (`OFF`, `TARGET_MATCH`, `RULE_MATCH`, `PERCENTAGE_ROLLOUT`, `DEFAULT`). If not, it invokes `chain.proceed(ctx, flag)`.

```text
[KillSwitch] -> [UserTargets] -> [SegmentRules] -> [PercentageRollout] -> [DefaultVariation]
```

---

### 3. Strategy Pattern (`flagship-core`)
- **Problem:** Operators (`EQUALS`, `IN`, `CONTAINS`, `SEMVER_*`, `REGEX`, `DATE_BEFORE`, `DATE_AFTER`) and hashing algorithms must be extensible and testable in isolation.
- **Solution:**
  - `OperatorStrategy` defines `boolean evaluate(String actualValue, List<String> targetValues)`.
  - `OperatorRegistry` maintains an immutable map of all strategies.
  - `BucketingStrategy` defines `int calculateBucket(String flagKey, String userKey)`.
  - `MurmurHash3BucketingStrategy` computes a deterministic 32-bit Murmur3 hash mapped unsigned to `[0, 9999]`.

---

### 4. Decorator Pattern (`flagship-spring-boot-starter`)
- **Problem:** The client must evaluate flags with maximum speed, guarantee zero runtime exceptions to host applications, and report observability metrics without mixing concerns into a single monolithic class.
- **Solution:**
  - Core Component: `SnapshotFlagShipClient` evaluates in-memory against `AtomicReference<CompiledSnapshot>`.
  - Decorator 1: `ResilientFlagShipClient` wraps any client with `try-catch (Throwable t)`, logging errors and returning the caller-supplied default value.
  - Decorator 2: `MeteredFlagShipClient` wraps the resilient client, recording timer durations and evaluation counters into Micrometer.

```text
Caller Application
       │
       ▼
[MeteredFlagShipClient]      (Records Micrometer Timers & Counters)
       │
       ▼
[ResilientFlagShipClient]    (Guarantees Zero Exceptions; Catches Throwables)
       │
       ▼
[SnapshotFlagShipClient]     (Lock-Free In-Memory Atomic Snapshot Evaluation)
```

---

### 5. Command Pattern (`flagship-server`)
- **Problem:** All flag mutations (create, update, toggle kill switch, rollback) require audit tracking, JSON diff generation, approval gating (Four-Eyes principle), and scheduled execution.
- **Solution:**
  - `FlagChangeCommand` interface defines `FlagDefinition execute(FlagService flagService)` and `String computeDiff(FlagDefinition before, FlagDefinition after)`.
  - Concrete commands: `CreateFlagCommand`, `UpdateFlagCommand`, `ToggleKillSwitchCommand`, `RestoreVersionCommand`.
  - Commands can be executed immediately or serialized into `change_requests` or `scheduled_changes` for future automated execution.
