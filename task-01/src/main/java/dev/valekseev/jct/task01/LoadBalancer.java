package dev.valekseev.jct.task01;

import java.util.ArrayList;
import java.util.Collections;
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

        return false;
    }

    public List<String> getInstances() {
        return Collections.unmodifiableList(registry);
    }

}
