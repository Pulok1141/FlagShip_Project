# Error.md — Known Pitfalls, Troubleshooting & Bug Log

This file tracks common architectural pitfalls to proactively avoid during FlagShip development, plus a structured log for any errors encountered and resolved.

---

## 1. Proactive Pitfalls & Prevention Checklist

### A. Spring Boot Starter Auto-Configuration
- **Pitfall:** Using `META-INF/spring.factories` in Spring Boot 3.x (silently ignored).
  - **Fix:** Always register auto-configuration classes in `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.
- **Pitfall:** `flagship-spring-boot-starter` pulling in `spring-boot-starter-web` or conflicting Jackson/AOP versions.
  - **Fix:** Keep optional dependencies marked `<optional>true</optional>` and guard Actuator/AOP/Micrometer beans with `@ConditionalOnClass`.
- **Pitfall:** `@FeatureFlag` AOP not firing on self-invocation (`this.guardedMethod()`).
  - **Fix:** Document standard Spring AOP proxy behavior in `QUICKSTART.md` and ensure aspect uses `@Around("@annotation(featureFlag)")`.

### B. SSE Fan-Out & Concurrency (FR5, QA3)
- **Pitfall:** `AsyncRequestTimeoutException` or `IOException: Broken pipe` flooding server logs when clients disconnect.
  - **Fix:** Register `emitter.onCompletion`, `emitter.onTimeout`, and `emitter.onError` callbacks to cleanly remove dead emitters from `CopyOnWriteArraySet`. Catch `IOException` on send and evict dead emitters immediately.
- **Pitfall:** Database connection pool exhaustion during long-lived SSE requests (`spring.jpa.open-in-view=true`).
  - **Fix:** Set `spring.jpa.open-in-view=false` in `flagship-server` `application.yml` so DB connections are never held open while streaming SSE.
- **Pitfall:** Reverse proxy / Tomcat buffering SSE chunks.
  - **Fix:** Set headers `Cache-Control: no-cache`, `X-Accel-Buffering: no`, and send periodic `: heartbeat\n\n` comments every 15 seconds.

### C. Local Evaluation & Memory (QA1, QA4)
- **Pitfall:** Calling `Pattern.compile(regex)` on every evaluation destroys the $50\,\mu\text{s}$ p99 budget.
  - **Fix:** Pre-compile regex patterns, SemVer objects, and `Instant` timestamps once when building the immutable `CompiledSnapshot`.
- **Pitfall:** Signed integer overflow when computing `hash % 10000` in Java (produces negative buckets!).
  - **Fix:** Always use `Integer.remainderUnsigned(murmurHash3, 10000)` so buckets are strictly in `[0, 9999]`.

### D. Offline Snapshot Persistence (FR6, QA2)
- **Pitfall:** Corrupted local snapshot JSON if the JVM crashes mid-write.
  - **Fix:** Write to `<cacheFile>.tmp` first, flush, then atomically replace using `Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)` (with fallback to `REPLACE_EXISTING`).
- **Pitfall:** `RestClient` / `HttpClient` hanging indefinitely on startup if server drops packets.
  - **Fix:** Configure both `connectTimeout` (e.g., 800 ms) and `readTimeout` (e.g., 1,200 ms) so total startup wait is strictly $\le 2,000\text{ ms}$.

### E. Experiments & Statistics (FR8)
- **Pitfall:** Division by zero in Z-test or Chi-square when a variation has 0 exposures or 0 conversions.
  - **Fix:** Guard calculations with minimum sample checks (`exposures > 0`, `pooledVariance > 0`); return `status = "INSUFFICIENT_DATA"` gracefully instead of `NaN` or `Infinity`.

---

## 2. Runtime Error & Resolution Log
*(Append any errors encountered during build, testing, or execution below.)*

| ID | Date | Module | Error Summary | Root Cause | Resolution | Status |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `ERR-001` | 2026-11-19 | `build` | Maven cannot create ~/.m2/repository in Windows sandbox | Sandbox read/write restricted to workspace | Configured -Dmaven.repo.local in workspace mvnw.cmd | Resolved |
| `ERR-002` | 2026-11-19 | `flagship-core` | Cannot find symbol List.emptyList() in CompiledFlag | List.emptyList() does not exist in standard Java | Replaced with List.of() / Collections.emptyList() | Resolved |
| `ERR-003` | 2026-11-19 | `flagship-server` | LazyInitializationException on Environment.project | FetchType.LAZY accessed outside Hibernate session in ApiKey filter | Set FetchType.EAGER on Environment.project | Resolved |
| `ERR-004` | 2026-11-19 | `flagship-server` | ByteBuddy self-attachment hangs on Windows JVM attach listener | Dynamic agent self-attachment blocked in restricted sandbox | Excluded Mockito inline agent, used Java reflection Proxy stubs | Resolved |
| `ERR-005` | 2026-11-19 | `flagship-server` | HttpMessageNotWritableException JSON recursion | Bidirectional JPA relations (Project/Environment, Flag/FlagEnvConfig) | Added @JsonIgnore to child reference getters | Resolved |
| `ERR-006` | 2026-11-19 | `flagship-spring-boot-starter` | Unable to establish loopback connection in HttpClient | Windows NIO Selector creates TCP 127.0.0.1 pipe for wakeup | Refactored HTTP & SSE sync to use HttpURLConnection | Resolved |