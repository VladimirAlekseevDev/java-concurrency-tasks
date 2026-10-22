# Task 01 — Load Balancer

Implement a thread-safe in-memory `LoadBalancer`.

- Register up to 10 unique addresses by default; allow a custom positive capacity.
- `register(address)` returns `true` for a new address and `false` for a duplicate, even when full.
- Reject null/blank addresses or invalid capacity with `IllegalArgumentException`; reject a new address when full with `IllegalStateException`. Failed calls leave the registry unchanged.
- Compare addresses exactly and case-sensitively, without normalisation.
- `getInstances()` returns an unmodifiable, consistent snapshot unaffected by later registrations. Instances have independent state; concurrent calls must preserve uniqueness and capacity.
