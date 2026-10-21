# Task 02 — Money Transfers

Build a thread-safe in-memory service for transferring money between accounts.

**Timebox:** 35–40 minutes including tests. Aim to finish Stage 1 in 25–30 minutes, leaving time for an extension after review.

## Stage 1: Atomic transfers

Implement `TransferService` in the `dev.valekseev.jct.task02` package with this public API:

```java
public TransferService(Map<String, Long> initialBalances);
public void transfer(String fromAccountId, String toAccountId, long amount);
public Map<String, Long> getBalances();
```

Amounts and balances are integer minor units, such as cents. Each balance must stay between zero and `Long.MAX_VALUE`. Do not use floating-point numbers.

### Accounts

- The constructor creates the accounts and their starting balances. An empty map is allowed.
- A null map, null or blank account ID, null balance, or negative balance throws `IllegalArgumentException`.
- Account IDs are opaque strings compared by exact, case-sensitive equality. Do not normalise them.
- Later changes to the caller's input map must not affect the service.
- Accounts cannot be added or removed in this stage. Separate service instances have independent state.

### Transfers

- A successful transfer subtracts `amount` from the source and adds it to the destination.
- Null or blank account IDs, a non-positive amount, or a transfer to the same account throw `IllegalArgumentException`.
- An unknown source or destination throws `NoSuchElementException`.
- Insufficient source funds throw `IllegalStateException`.
- A destination balance exceeding `Long.MAX_VALUE` throws `ArithmeticException`; balances must never wrap around.
- A failed transfer leaves all balances unchanged. If several validation rules are violated, exception precedence is unspecified.

For example, starting with `{alice=1000, bob=500}`, transferring `250` from `alice` to `bob` results in `{alice=750, bob=750}`.

### Reads and concurrency

- `getBalances()` returns an unmodifiable, independent snapshot of all account balances. Later transfers must not change earlier snapshots. Map iteration order is unspecified.
- Both methods may be called concurrently on the same service.
- Every transfer and snapshot must behave as if it took effect at a single point between invocation and completion.
- A snapshot must never show only the debit or only the credit of a transfer.
- Concurrent transfers must preserve the total amount of money and must not overdraw an account, lose updates, or deadlock, including transfers in opposite directions.
- No throughput target or requirement for independent transfers to execute in parallel is imposed at this stage.

## Scope and tests

Use only the JDK for production code and the existing JUnit dependencies for tests; Mockito is optional. No new dependencies, database, HTTP layer, persistence, or background tasks are needed.

Write the solution in `src/main/java` and tests in `src/test/java`. Use clear test names and small test-first steps. Cover normal behaviour, failure paths, boundaries, and concurrent access without relying on sleeps for coordination.

Run from the repository root:

```powershell
.\gradlew.bat :task-02:test --rerun-tasks
```

On Linux/macOS, use `./gradlew` instead of `.\gradlew.bat`.

When ready for review, share your elapsed coding time and any assumptions. Implement only the current stage; further requirements will follow separately.

This is a practice exercise inspired by interview-style problems, not an official Revolut task statement.
