package dev.valekseev.jct.task02;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

public class TransferService {

    private final Map<String, Long> balances;
    private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final WriteLock writeLock = readWriteLock.writeLock();
    private final ReadLock readLock = readWriteLock.readLock();

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
        validateNotTheSameAccountId(from, to);
        validateTransferAmount(amount);

        writeLock.lock();
        try {
            long fromAccountBalance = getAccountBalanceOrThrow(from);
            long toAccountBalance = getAccountBalanceOrThrow(to);

            validateEnoughFunds(fromAccountBalance, amount);
            validateNoOverflow(toAccountBalance, amount);

            balances.replace(from, fromAccountBalance - amount);
            balances.replace(to, toAccountBalance + amount);
        } finally {
            writeLock.unlock();
        }
    }

    public Map<String, Long> getBalances() {
        readLock.lock();
        try {
            return Map.copyOf(balances);
        } finally {
            readLock.unlock();
        }
    }

    private void validateAccountId(String accountId) {
        if (accountId == null || accountId.isBlank()) {
            throw new IllegalArgumentException();
        }
    }

    private void validateNotTheSameAccountId(String from, String to) {
        if (Objects.equals(from, to)) {
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

    private void validateEnoughFunds(long accountBalance, long transferAmount) {
        if (accountBalance < transferAmount) {
            throw new IllegalStateException();
        }
    }

    private void validateNoOverflow(long toAccountBalance, long amount) {
        // Also can be implemented as Math.addExact(toAccountBalance, amount);
        if (Long.MAX_VALUE - toAccountBalance < amount) {
            throw new ArithmeticException();
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
