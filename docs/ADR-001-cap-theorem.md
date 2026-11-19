# ADR-001: CAP Theorem Trade-off — Control Plane CP vs. Data Plane AP

- **Status:** Accepted
- **Date:** 2026-11-19
- **Deciders:** Principal Architecture Team

---

## 1. Context and Problem Statement

Feature flag platforms operate at two distinct operational tiers:
1. **Control Plane (`flagship-server`):** Where administrators, developers, and release managers define flags, alter segment targeting rules, adjust rollout percentages, submit production change requests requiring approval (Four-Eyes principle), and schedule automated cutovers.
2. **Data Plane (`flagship-spring-boot-starter`):** Embedded within microservices and backend services, evaluating flags synchronously on the application hot path during high-throughput user requests.

The CAP Theorem states that any distributed data store can guarantee at most two of the following three properties:
- **Consistency (C):** Every read receives the most recent write or an error.
- **Availability (A):** Every non-failing node returns a non-error response without guarantee that it contains the most recent write.
- **Partition Tolerance (P):** The system continues to operate despite an arbitrary number of messages being dropped or delayed by the network between nodes.

In production environments, network partitions ($P$) are unavoidable. Therefore, the system must choose between Consistency ($CP$) and Availability ($AP$) for its respective sub-systems.

---

## 2. Decision

We explicitly architect FlagShip as a **bifurcated CAP system**:
1. **Control Plane (`flagship-server`) is CP (Consistency + Partition Tolerance):**
   - Administrative mutations are transactional.
   - Enforced by PostgreSQL relational storage (`ACID` transactions with serializable / read-committed isolation levels).
   - Change requests, version history commits, and approval state transitions prioritize strict consistency over write availability.
   - If database connectivity is impaired or partitioned, the control plane rejects write requests rather than allowing split-brain or stale mutations.

2. **Data Plane (`flagship-spring-boot-starter`) is AP (Availability + Partition Tolerance with Eventual Consistency):**
   - The evaluation engine operates **purely in-memory** against an immutable `AtomicReference<CompiledSnapshot>`.
   - The starter guarantees the **Zero-Exception Contract (QA2)**: under total network disconnection, server crash, or corrupt payloads, evaluation never fails and never blocks the host application.
   - Synchronization with the control plane uses an asynchronous Server-Sent Events (SSE) push channel with automatic HTTP polling fallback.
   - Updates propagate with **Eventual Consistency** (guaranteed within $\le 5$ seconds at p95 per **QA3**).
   - In the event of a network partition between client microservices and the control plane, evaluations continue serving from the local atomic disk snapshot cache (`./.flagship/snapshot.json`).

---

## 3. Detailed Architectural Comparison

| Dimension | Control Plane (`flagship-server`) | Data Plane (`flagship-spring-boot-starter`) |
| :--- | :--- | :--- |
| **CAP Classification** | **CP** (Consistency / Partition Tolerance) | **AP** (Availability / Partition Tolerance) |
| **Storage Mechanism** | PostgreSQL 16 (RDBMS) via Flyway & Spring Data JPA | In-Memory `AtomicReference<CompiledSnapshot>` + Disk JSON |
| **Consistency Model** | Immediate / Strong Consistency | Eventual Consistency ($\le 5\text{ s}$ via SSE push) |
| **Write Availability** | Degrades during DB partition; mutations rejected | Writes (events/telemetry) buffered in non-blocking queue |
| **Read Availability** | Requires DB read connection | 100% available; lock-free RAM reads ($\le 50\,\mu\text{s}$) |
| **Failure Behavior** | Fails fast with descriptive HTTP error codes (409, 503) | Never throws; returns caller-supplied default value |
| **Partition Response** | Protects integrity of approvals and flag versions | Continues evaluating locally without interruption |

---

## 4. Consequences and Mitigations

### Positive Consequences
- **Zero Runtime Blocker:** An outage of the FlagShip server or database has zero impact on user-facing applications consuming the starter.
- **Ultra-Low Latency:** In-memory evaluation avoids network hops on the critical path, keeping p99 evaluation latencies well below $50\,\mu\text{s}$ (measured at $\sim 30\,\mu\text{s}$).
- **Audit & Compliance Safety:** Production changes cannot be silently lost or applied concurrently without four-eyes approval due to strict CP guarantees on the control plane.

### Trade-offs & Mitigations
- **Propagation Lag Window:** A change made on the control plane takes up to a few milliseconds (and guaranteed $< 5$ seconds) to reach all connected instances.
  - *Mitigation:* The SSE push mechanism runs on Java 21 Virtual Threads, delivering updates concurrently to hundreds of connected instances within tens of milliseconds (QA3 verified at $39.3\text{ ms}$).
- **Temporary Stale Reads During Partitions:** If a client node is isolated from the network, it will serve flag rules from its last known snapshot.
  - *Mitigation:* The custom `/actuator/flagship` endpoint and Micrometer metric `flagship.snapshot.age.seconds` alert operations teams if any node's snapshot exceeds staleness thresholds.
