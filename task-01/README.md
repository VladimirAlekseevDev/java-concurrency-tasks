# Task 01 — Load Balancer

Build a small, thread-safe in-memory load balancer as a reusable Java component.

**Session:** 35–40 minutes, including tests. Aim to complete this first stage in 20–25 minutes; further requirements will follow after review.

## Stage 1: Instance registration

Implement `LoadBalancer` in the `concurrency.task01` package with this public API:

```java
public LoadBalancer();
public boolean register(String address);
public List<String> getInstances();
```

The registry starts empty and holds at most **10 unique backend addresses**.

### Behaviour

- `register` returns `true` when it adds a new address and `false` when that address is already registered.
- Registering a duplicate returns `false` even when the registry is full.
- Registering a new address when the registry is full throws `IllegalStateException`.
- A `null` or blank address throws `IllegalArgumentException` and leaves the registry unchanged.
- Addresses are opaque strings compared by exact, case-sensitive equality. Do not trim, normalise, resolve, or validate them as URLs.
- `getInstances` returns an unmodifiable snapshot of the registered addresses. Its order is unspecified; later registrations must not change an earlier snapshot.
- Separate `LoadBalancer` objects have independent registries.

### Concurrency contract

Both methods may be called concurrently on the same object. Every call must behave as if it took effect at a single point between invocation and completion.

The capacity and uniqueness rules must always hold. Concurrent registrations of the same new address must add it only once; when space is available, exactly one of those calls returns `true`. Snapshots must represent a consistent registry state.

## Constraints and tests

Use only the JDK for the implementation. Use the existing JUnit dependencies for tests; Mockito is optional. No additional dependencies, networking, database, or background tasks are needed.

Write tests in `src/test/java/concurrency/task01`. Use clear names and cover normal behaviour, errors, boundaries, and concurrent access. Prefer small test-first steps and the simplest correct implementation.

Run from the repository root:

```powershell
.\gradlew.bat :task-01:test --rerun-tasks
```

On Linux/macOS, use `./gradlew` instead of `.\gradlew.bat`.

When ready for review, share your elapsed coding time and any assumptions or questions. Only this stage is required now.

## Background

This is a training adaptation, not an official Revolut task. Similar registration exercises appear in [candidate reports](https://leetcode.com/discuss/post/1566703/revolut-backend-engineer-remote-nov-2021-reject/). The exact API and contract above are defined for this practice session.
