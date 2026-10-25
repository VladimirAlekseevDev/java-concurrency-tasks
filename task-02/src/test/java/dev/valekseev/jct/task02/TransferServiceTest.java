package dev.valekseev.jct.task02;

import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
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
    void whenProvidedNotEmptyMap_shouldReturnItsCopyByGetBalances() {
        Map<String, Long> initialBalances = Map.of(
                "1", 1L,
                "2", 2L,
                "3", 3L
        );
        transferService = new TransferService(initialBalances);

        Map<String, Long> balances = transferService.getBalances();
        assertEquals(initialBalances, balances);
        assertNotSame(initialBalances, balances);

        transferService.transfer("1", "2", 1L);
        assertEquals(initialBalances, balances);
    }

    @Test
    void whenCalledGetBalances_shouldReturnUnmodifiableSnapshot() {
        Map<String, Long> initialBalances = Map.of(
                "1", 1L,
                "2", 2L
        );
        transferService = new TransferService(initialBalances);

        Map<String, Long> balances = transferService.getBalances();
        assertEquals(initialBalances, balances);
        assertNotSame(initialBalances, balances);

        transferService.transfer("1", "2", 1L);
        assertEquals(initialBalances, balances);
    }

    @ParameterizedTest
    @MethodSource("getInvalidArgsForTransfer")
    void whenTransferWithInvalidParams_shouldThrowIllegalArgExc(String from, String to, long amount) {
        transferService = new TransferService(emptyMap());
        assertThrows(IllegalArgumentException.class, () -> transferService.transfer(from, to, amount));
    }

    private static Stream<Arguments> getInvalidArgsForTransfer() {
        return Stream.of(
            Arguments.of(null, "1", 1L),
            Arguments.of(" ", "1", 1L),
            Arguments.of(" \t", "1", 1L),
            Arguments.of("1", null, 1L),
            Arguments.of("1", " ", 1L),
            Arguments.of("1", "\n", 1L),
            Arguments.of("1", "1", -50),
            Arguments.of("1", "1", -1),
            Arguments.of("1", "1", 0)
        );
    }

    @Test
    void whenTransferWithNonExistentAccountIdParam_shouldThrowNoSuchElExc() {
        transferService = new TransferService(emptyMap());
        assertThrows(NoSuchElementException.class, () -> transferService.transfer("1", "2", 3));

        transferService = new TransferService(Map.of("1", 2L, "2", 3L));
        assertThrows(NoSuchElementException.class, () -> transferService.transfer("1", "3", 3));
        assertThrows(NoSuchElementException.class, () -> transferService.transfer("3", "2", 3));
        assertThrows(NoSuchElementException.class, () -> transferService.transfer("5", "7", 3));
    }

    @Test
    void whenTransferWithInsufficientFunds_shouldThrowIllegalStateExc() {
        transferService = new TransferService(Map.of("1", 2L, "2", 3L));
        assertThrows(IllegalStateException.class, () -> transferService.transfer("1", "2", 3L));
        assertThrows(IllegalStateException.class, () -> transferService.transfer("2", "1", 4L));
    }

    @ParameterizedTest
    @MethodSource("getArgsForTransferWithOverflow")
    void whenTransferWithBalanceOverflow_shouldThrowArithmeticExc(Long fromBalance, Long toBalance, Long amount) {
        transferService = new TransferService(Map.of("1", fromBalance, "2", toBalance));
        assertThrows(ArithmeticException.class, () -> transferService.transfer("1", "2", amount));
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
        transferService = new TransferService(Map.of("1", 10L));
        assertThrows(IllegalArgumentException.class, () -> transferService.transfer("1", "1", 1L));
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
