package dev.valekseev.jct.task02;

import java.util.Map;

public interface TransferService {

    void transfer(String from, String to, long amount);

    Map<String, Long> getBalances();

}
