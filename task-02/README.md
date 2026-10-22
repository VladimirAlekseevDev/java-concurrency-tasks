# Task 02 — Money Transfers

Implement a thread-safe in-memory `TransferService` initialised with account balances. A null input map throws `IllegalArgumentException`; an empty map is allowed.

- Use non-negative `long` balances in integer minor units. Account IDs are non-blank, exact, case-sensitive strings. Copy the input map; accounts are fixed and instances independent.
- `transfer(from, to, amount)` moves a positive amount between two distinct existing accounts.
- Reject invalid input with `IllegalArgumentException`, unknown accounts with `NoSuchElementException`, insufficient funds with `IllegalStateException`, and balance overflow with `ArithmeticException`.
- Transfers must be atomic and preserve total funds. Failed transfers change nothing; concurrent transfers must not overdraw accounts, lose updates, or deadlock.
- `getBalances()` returns an unmodifiable, consistent snapshot unaffected by later transfers; a snapshot must never show a partially applied transfer.
