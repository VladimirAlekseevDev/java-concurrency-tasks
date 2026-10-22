package dev.valekseev.jct.task02;

import java.util.HashMap;
import java.util.Map;

public class TransferService {

    private Map<String, Long> balances;

    public TransferService(Map<String, Long> balances) {
        this.balances = new HashMap<>(balances);
    }

    public void transfer(String from, String to, long amount) {

    }

    public Map<String, Long> getBalances() {
        return Map.of();
    }

}
