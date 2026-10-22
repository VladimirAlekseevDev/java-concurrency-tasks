package dev.valekseev.jct.task02;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class TransferService {

    private Map<String, Long> balances;
    private ReentrantReadWriteLock readWriteLock;

    public TransferService(Map<String, Long> balances) {
        if (balances == null) {
            throw new IllegalArgumentException();
        }
        balances.forEach((accId, balance) -> {
            validateAccountId(accId);
            validateInitialBalance(balance);
        });

        this.balances = new HashMap<>(balances);
    }

    public void transfer(String from, String to, long amount) {
        validateAccountId(from);
        validateAccountId(to);
        validateTransferAmount(amount);

        long fromAccountBalance = getAccountBalanceOrThrow(from);
        long toAccountBalance = getAccountBalanceOrThrow(to);

        if (fromAccountBalance - amount < 0) {
            throw new IllegalStateException();
        }
        if (Long.MAX_VALUE - toAccountBalance < fromAccountBalance) {
            throw new ArithmeticException();
        }
    }

    public Map<String, Long> getBalances() {
        return Map.copyOf(balances);
    }

    private void validateAccountId(String accountId) {
        if (accountId == null || accountId.isBlank()) {
            throw new IllegalArgumentException();
        }
    }

    private void validateInitialBalance(Long balance) {
        if (balance == null || balance < 0) {
            throw new IllegalArgumentException();
        }
    }

    private void validateTransferAmount(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException();
        }
    }

    private long getAccountBalanceOrThrow(String accountId) {
        Long balance = balances.get(accountId);
        if (balance == null) {
            throw new NoSuchElementException();
        }
        return balance;
    }

}
