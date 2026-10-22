package dev.valekseev.jct.task02;

import java.util.HashMap;
import java.util.Map;

public class TransferServiceImpl implements TransferService {

    private Map<String, Long> balances;

    public TransferServiceImpl(Map<String, Long> balances) {
        this.balances = new HashMap<>(balances);
    }

    @Override
    public void transfer(String from, String to, long amount) {

    }

    @Override
    public Map<String, Long> getBalances() {
        return Map.of();
    }

}
