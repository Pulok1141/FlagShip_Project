# ADR-002: Architectural Position on the Singleton Pattern — Rejecting GoF Static Singletons in Favor of Spring IoC Singletons

- **Status:** Accepted
- **Date:** 2026-11-19
- **Deciders:** Principal Architecture Team

---

## 1. Context and Problem Statement

Feature flag SDKs often face the question of how to expose the client entrypoint to application code. Historically, many legacy feature flag libraries (such as early versions of LaunchDarkly or custom internal tools) implemented the classic **Gang of Four (GoF) Singleton Pattern**:
```java
// Anti-Pattern: Classic GoF Static Singleton
public class FlagShipClient {
    private static FlagShipClient INSTANCE;

    public static synchronized FlagShipClient getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new FlagShipClient();
        }
        return INSTANCE;
    }
}
```

While simple on the surface, static singletons introduce severe architectural problems in enterprise Java and Spring Boot ecosystems. We must establish a clear and binding architectural policy regarding the singleton pattern in FlagShip.

---

## 2. Decision

We **explicitly reject the GoF static singleton pattern** (`static getInstance()`, static global state, and private constructors preventing instantiation) in FlagShip.

Instead, we strictly enforce **Inversion of Control (IoC) Singletons** managed by the **Spring ApplicationContext** lifecycle:
```java
// Architectural Standard: Spring IoC Managed Singleton Bean
@AutoConfiguration
@ConditionalOnProperty(prefix = "flagship", name = "enabled", matchIfMissing = true)
public class FlagShipAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public FlagShipClient flagShipClient(...) {
        // Returned bean is singleton-scoped within the Spring ApplicationContext
        return new MeteredFlagShipClient(new ResilientFlagShipClient(new SnapshotFlagShipClient(...)));
    }
}
```

---

## 3. Rationale & Analysis of Anti-Pattern Pitfalls

### Pitfall 1: Untestable Code and Hidden Dependencies
When classes call `FlagShipClient.getInstance()` directly inside methods, the dependency is hidden and tightly coupled. Unit testing becomes extraordinarily difficult:
- Cannot inject a mock or stub instance without reflective bytecode hacks (`setAccessible(true)` or PowerMock).
- Static state leaks across independent test classes, leading to flaky test suites where test execution order matters.
- Parallel test execution (`junit.jupiter.execution.parallel.enabled=true`) becomes unstable due to race conditions on shared static state.

With Spring IoC singletons:
- Dependencies are declared explicitly in constructors (`public CheckoutService(FlagShipClient client)`).
- Unit tests can trivially pass a mock or lightweight stub (`new CheckoutService(new StubFlagShipClient())`) without needing Spring or dynamic agent attachment.

### Pitfall 2: ClassLoader and Multi-Context Leaks
In enterprise environments (e.g., modular web applications, OSGi, multi-tenant containers, or `@DirtiesContext` test suites), classes loaded by shared parent classloaders retain static references indefinitely:
- Memory leaks occur when application contexts reload but static references keep old instances pinned in memory.
- In Spring Boot integration tests, multiple `ApplicationContext` instances are created; a static singleton cannot adapt to different configurations (`test-application.yml` vs `dev-application.yml`).

With Spring IoC singletons:
- Lifecycle is bound strictly to the `ApplicationContext`.
- When the context shuts down, `@PreDestroy` or `AutoCloseable.close()` executes cleanly, terminating SSE background threads, executors, and event flushers without leaving orphan threads.

### Pitfall 3: Inability to Support Multi-Tenant or Multi-Project Setups
If a single service needs to connect to two different FlagShip projects or environments (e.g., a batch reconciliation service that verifies flags across both `staging` and `production`), a static singleton prevents having two client instances.

With Spring IoC:
- Users can define multiple qualified beans (`@Qualifier("stagingClient")`, `@Qualifier("prodClient")`) if needed, without altering the framework code.

### Pitfall 4: Violates Open/Closed and Decorator Principles
A static singleton makes it impossible to apply clean decorator stacks dynamically. In FlagShip, `FlagShipClient` is an interface wrapped in a 3-layer decorator chain:
`MeteredFlagShipClient` $\rightarrow$ `ResilientFlagShipClient` $\rightarrow$ `SnapshotFlagShipClient`.
A static class prevents this clean separation of concerns.

---

## 4. Comparison Summary

| Criteria | GoF Static Singleton (`getInstance()`) | Spring IoC Container Singleton |
| :--- | :--- | :--- |
| **Instantiation** | Static field, private constructor | Managed by Spring IoC container |
| **Dependency Visibility** | Hidden inside method bodies | Explicit in constructor signatures |
| **Testability** | Nightmare; requires reflection / leaks state | Pristine; easily mocked or stubbed |
| **Lifecycle Control** | Uncontrolled; stays until ClassLoader unloads | Tied to `ApplicationContext` start/stop |
| **Decorators / Proxies** | Inflexible; cannot easily wrap or intercept | Seamless (Micrometer, Resilience, AOP) |
| **Thread Cleanliness** | Threads leak during application restart | Clean disposal via `close()` / `destroy()` |
| **Parallel Testing** | Flaky due to global shared state | 100% thread-safe and isolated |

---

## 5. Architectural Rule Enforced in Codebase

1. No class in `flagship-core`, `flagship-server`, or `flagship-spring-boot-starter` may declare a `public static getInstance()` method for client access.
2. Applications obtain `FlagShipClient` exclusively via standard Spring constructor injection or field autowiring.
3. The Starter registers the client bean as a standard Spring singleton via `FlagShipAutoConfiguration`.
