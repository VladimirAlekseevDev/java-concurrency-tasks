package dev.valekseev.jct.task01;

import java.util.ArrayList;
import java.util.List;

public class LoadBalancer {

    private static final int DEFAULT_MAX_CAPACITY = 10;

    private final List<String> registry = new ArrayList<>();
    private final int maxCapacity;

    public LoadBalancer() {
        this(DEFAULT_MAX_CAPACITY);
    }

    public LoadBalancer(int maxCapacity) {
        if (maxCapacity <= 0) {
            throw new IllegalArgumentException();
        }
        this.maxCapacity = maxCapacity;
    }

    public boolean register(String address) {
        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException();
        }

        if (registry.contains(address)) {
            return false;
        }

        if (registry.size() == maxCapacity) {
            throw new IllegalStateException();
        }

        registry.add(address);
        return true;
    }

    public List<String> getInstances() {
        return List.copyOf(registry);
    }

}
