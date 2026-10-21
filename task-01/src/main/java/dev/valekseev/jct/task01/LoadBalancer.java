package dev.valekseev.jct.task01;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class LoadBalancer {

    private static final int DEFAULT_MAX_CAPACITY = 10;

    private final List<String> registry = new ArrayList<>();
    private final int maxCapacity;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public LoadBalancer() {
        this(DEFAULT_MAX_CAPACITY);
    }

    public LoadBalancer(int maxCapacity) {
        if (maxCapacity <= 0) {
            throw new IllegalArgumentException("Capacity must be greater than zero");
        }
        this.maxCapacity = maxCapacity;
    }

    public boolean register(String address) {
        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException("Address must not be null or blank");
        }

        ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
        writeLock.lock();
        try {
            if (registry.contains(address)) {
                return false;
            }

            if (registry.size() >= maxCapacity) {
                throw new IllegalStateException("Registry capacity of " + maxCapacity + " has been reached");
            }

            registry.add(address);
            return true;
        } finally {
            writeLock.unlock();
        }
    }

    public List<String> getInstances() {
        ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
        readLock.lock();
        try {
            return List.copyOf(registry);
        } finally {
            readLock.unlock();
        }
    }

}
