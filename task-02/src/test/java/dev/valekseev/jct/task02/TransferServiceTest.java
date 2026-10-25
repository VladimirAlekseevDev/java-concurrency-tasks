package dev.valekseev.jct.task02;

import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

public class TransferServiceTest {

    private TransferService transferService;

    @BeforeEach
    void setUp() {
        transferService = null;
    }

    @Test
    void whenProvidedNullMap_shouldThrowIllegalArgExc() {
        assertThrows(IllegalArgumentException.class, () -> new TransferService(null));
    }

    @Test
    void whenProvidedMapWithNullAccountId_shouldThrowIllegalArgExc() {
        Map<String, Long> initialBalances = new HashMap<>();
        initialBalances.put(null, 1L);

        assertThrows(IllegalArgumentException.class, () -> new TransferService(initialBalances));
    }

    @Test
    void whenProvidedMapWithNullBalance_shouldThrowIllegalArgExc() {
        Map<String, Long> initialBalances = new HashMap<>();
        initialBalances.put("1", null);

        assertThrows(IllegalArgumentException.class, () -> new TransferService(initialBalances));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t", "\r", "\n", "\t \r \n"})
    void whenProvidedMapWithIncorrectAccountId_shouldThrowIllegalArgExc(String accountId) {
        assertThrows(IllegalArgumentException.class, () -> new TransferService(Map.of(accountId, 1L)));
    }

    @ParameterizedTest
    @ValueSource(longs = { Long.MIN_VALUE, -100L, -1 })
    void whenProvidedMapWithIncorrectBalance_shouldThrowIllegalArgExc(Long balance) {
        assertThrows(IllegalArgumentException.class, () -> new TransferService(Map.of("1", balance)));
    }

    @Test
    void whenProvidedEmptyMap_shouldNotThrowAndCopyMapAndReturnEmptyBalances() {
        Map<String, Long> initialBalances = emptyMap();
        transferService = new TransferService(initialBalances);

        Map<String, Long> balances = transferService.getBalances();
        assertEquals(initialBalances, balances);
        assertNotSame(initialBalances, balances);
    }

    @Test
    void whenInputMapChanges_shouldKeepInitialBalances() {
        Map<String, Long> expectedBalances = Map.of("1", 100L, "2", 20L);
        Map<String, Long> initialBalances = new HashMap<>(expectedBalances);
        transferService = new TransferService(initialBalances);

        initialBalances.put("1", 999L);
        initialBalances.remove("2");
        initialBalances.put("3", 30L);

        assertEquals(expectedBalances, transferService.getBalances());
    }

    @Test
    void whenTransferSucceeds_shouldNotModifyInputMap() {
        Map<String, Long> expectedInitialBalances = Map.of("1", 100L, "2", 20L);
        Map<String, Long> initialBalances = new HashMap<>(expectedInitialBalances);
        transferService = new TransferService(initialBalances);

        transferService.transfer("1", "2", 30L);

        assertEquals(expectedInitialBalances, initialBalances);
        assertEquals(Map.of("1", 70L, "2", 50L), transferService.getBalances());
    }

    @Test
    void whenCalledGetBalances_shouldReturnUnmodifiableSnapshot() {
        Map<String, Long> initialBalances = Map.of(
                "1", 1L,
                "2", 2L
        );
        transferService = new TransferService(initialBalances);

        Map<String, Long> balancesSnapshot = transferService.getBalances();
        assertEquals(initialBalances, balancesSnapshot);
        assertNotSame(initialBalances, balancesSnapshot);

        transferService.transfer("1", "2", 1L);
        assertEquals(initialBalances, balancesSnapshot);

        assertThrows(UnsupportedOperationException.class, () -> balancesSnapshot.put("3", 3L));
        assertThrows(UnsupportedOperationException.class, () -> balancesSnapshot.remove("2"));
        assertThrows(UnsupportedOperationException.class, () -> balancesSnapshot.replace("2", 5L));
    }

    @ParameterizedTest(name = "[{index}] from={0}, to={1}, amount={2}")
    @MethodSource("getArgsForSuccessfulTransfer")
    void whenTransferWithValidParams_shouldUpdateBalances(
            long fromBalance, long toBalance, long amount,
            long expectedFromBalance, long expectedToBalance) {
        transferService = new TransferService(Map.of("1", fromBalance, "2", toBalance, "3", 7L));

        transferService.transfer("1", "2", amount);

        assertEquals(Map.of("1", expectedFromBalance, "2", expectedToBalance, "3", 7L),
                transferService.getBalances());
    }

    private static Stream<Arguments> getArgsForSuccessfulTransfer() {
        return Stream.of(
            Arguments.of(100L, 20L, 30L, 70L, 50L),
            Arguments.of(100L, 20L, 100L, 0L, 120L),
            Arguments.of(10L, Long.MAX_VALUE - 10, 10L, 0L, Long.MAX_VALUE),
            Arguments.of(100L, Long.MAX_VALUE - 10, 5L, 95L, Long.MAX_VALUE - 5)
        );
    }

    @ParameterizedTest
    @MethodSource("getInvalidArgsForTransfer")
    void whenTransferWithInvalidParams_shouldThrowIllegalArgExc(String from, String to, long amount) {
        Map<String, Long> initialBalances = Map.of("1", 10L, "2", 20L);
        transferService = new TransferService(initialBalances);

        assertThrows(IllegalArgumentException.class, () -> transferService.transfer(from, to, amount));
        assertEquals(initialBalances, transferService.getBalances());
    }

    private static Stream<Arguments> getInvalidArgsForTransfer() {
        return Stream.of(
            Arguments.of(null, "1", 1L),
            Arguments.of(" ", "1", 1L),
            Arguments.of(" \t", "1", 1L),
            Arguments.of("1", null, 1L),
            Arguments.of("1", " ", 1L),
            Arguments.of("1", "\n", 1L),
            Arguments.of("1", "2", -50),
            Arguments.of("1", "2", -1),
            Arguments.of("1", "2", 0)
        );
    }

    @Test
    void whenTransferWithNonExistentAccountIdParam_shouldThrowNoSuchElExc() {
        transferService = new TransferService(emptyMap());
        assertThrows(NoSuchElementException.class, () -> transferService.transfer("1", "2", 3));
        assertEquals(emptyMap(), transferService.getBalances());

        Map<String, Long> initialBalances = Map.of("1", 2L, "2", 3L);
        transferService = new TransferService(initialBalances);
        assertThrows(NoSuchElementException.class, () -> transferService.transfer("1", "3", 3));
        assertThrows(NoSuchElementException.class, () -> transferService.transfer("3", "2", 3));
        assertThrows(NoSuchElementException.class, () -> transferService.transfer("5", "7", 3));
        assertEquals(initialBalances, transferService.getBalances());
    }

    @Test
    void whenTransferWithInsufficientFunds_shouldThrowIllegalStateExc() {
        Map<String, Long> initialBalances = Map.of("1", 2L, "2", 3L);
        transferService = new TransferService(initialBalances);
        assertThrows(IllegalStateException.class, () -> transferService.transfer("1", "2", 3L));
        assertThrows(IllegalStateException.class, () -> transferService.transfer("2", "1", 4L));
        assertEquals(initialBalances, transferService.getBalances());
    }

    @ParameterizedTest
    @MethodSource("getArgsForTransferWithOverflow")
    void whenTransferWithBalanceOverflow_shouldThrowArithmeticExc(Long fromBalance, Long toBalance, Long amount) {
        Map<String, Long> initialBalances = Map.of("1", fromBalance, "2", toBalance);
        transferService = new TransferService(initialBalances);
        assertThrows(ArithmeticException.class, () -> transferService.transfer("1", "2", amount));
        assertEquals(initialBalances, transferService.getBalances());
    }

    private static Stream<Arguments> getArgsForTransferWithOverflow() {
        return Stream.of(
            Arguments.of(Long.MAX_VALUE, 1L, Long.MAX_VALUE),
            Arguments.of(Long.MAX_VALUE, 100L, Long.MAX_VALUE),
            Arguments.of(1L, Long.MAX_VALUE, 1L),
            Arguments.of(100L, Long.MAX_VALUE, 100L),
            Arguments.of(11L, Long.MAX_VALUE-10, 11L),
            Arguments.of(Long.MAX_VALUE / 2 + 2, Long.MAX_VALUE / 2, Long.MAX_VALUE / 2 + 2)
        );
    }

    @Test
    void whenTrasferFromAndToAccountsAreSame_shouldThrowIllegalArgExc() {
        Map<String, Long> initialBalances = Map.of("1", 10L, "2", 20L);
        transferService = new TransferService(initialBalances);
        assertThrows(IllegalArgumentException.class, () -> transferService.transfer("1", "1", 1L));
        assertThrows(IllegalArgumentException.class, () -> transferService.transfer("2", "2", 5L));
        assertEquals(initialBalances, transferService.getBalances());
    }

    @Test
    void whenConcurrentTransfers_shouldNotLoseUpdate() {
        transferService = new TransferService(Map.of("1", 100L, "2", 0L));
        List<Runnable> transferTasks = IntStream.range(0, 20)
                .mapToObj(i -> (Runnable) () -> transferService.transfer("1", "2", 5L))
                .toList();

        assertDoesNotThrow(() -> runConcurrently(transferTasks));

        Map<String, Long> balances = transferService.getBalances();
        assertEquals(Map.of("1", 0L, "2", 100L), balances);
    }

    @Test
    void whenConcurrentTransfers_shouldNotOverdraw() {
        transferService = new TransferService(Map.of("1", 100L, "2", 0L));

        AtomicInteger successfulTransfers = new AtomicInteger();
        AtomicInteger rejectedTransfers = new AtomicInteger();
        List<Runnable> transferTasks = IntStream.range(0, 30)
                .mapToObj(i -> (Runnable) () -> {
                    try {
                        transferService.transfer("1", "2", 5L);
                        successfulTransfers.incrementAndGet();
                    } catch (IllegalStateException e) {
                        rejectedTransfers.incrementAndGet();
                    }
                })
                .toList();

        runConcurrently(transferTasks);

        Map<String, Long> balances = transferService.getBalances();
        assertEquals(Map.of("1", 0L, "2", 100L), balances);
        assertEquals(20, successfulTransfers.get());
        assertEquals(10, rejectedTransfers.get());
    }

    private void runConcurrently(List<Runnable> transferTasks) {
        try (ExecutorService executor = Executors.newFixedThreadPool(transferTasks.size())) {
            CyclicBarrier start = new CyclicBarrier(transferTasks.size());
            List<Future<?>> futures = new ArrayList<>();

            for (Runnable task : transferTasks) {
                Future<?> future = executor.submit(() -> {
                    try {
                        start.await(5, TimeUnit.SECONDS);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    task.run();
                });
                futures.add(future);
            }
            for (Future<?> future : futures) {
                try {
                    future.get(5, TimeUnit.SECONDS);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

}
