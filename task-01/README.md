# Task 01 — Load Balancer

Build a small, thread-safe in-memory load balancer as a reusable Java component.

## Stage 1: Instance registration

Implement `LoadBalancer` in the `dev.valekseev.jct.task01` package with this public API:

```java
public LoadBalancer();
public LoadBalancer(int maxCapacity);
public boolean register(String address);
public List<String> getInstances();
```

The default registry starts empty and holds at most **10 unique backend addresses**. The optional constructor accepts a custom positive capacity; zero or negative capacity throws `IllegalArgumentException`.

### Behaviour

- `register` returns `true` when it adds a new address and `false` when that address is already registered.
- Registering a duplicate returns `false` even when the registry is full.
- Registering a new address when the registry is full throws `IllegalStateException`.
- A `null` or blank address throws `IllegalArgumentException` and leaves the registry unchanged.
- Addresses are opaque strings compared by exact, case-sensitive equality. Do not trim, normalise, resolve, or validate them as URLs.
- `getInstances` returns an unmodifiable snapshot of the registered addresses. Its order is unspecified; later registrations must not change an earlier snapshot.
- Separate `LoadBalancer` objects have independent registries.

### Concurrency contract

Both methods may be called concurrently on the same object. Every call must behave as if it took 
effect at a single point between invocation and completion.

The capacity and uniqueness rules must always hold. Concurrent registrations of the same new address
must add it only once; when space is available, exactly one of those calls returns `true`. Snapshots must represent a consistent registry state.