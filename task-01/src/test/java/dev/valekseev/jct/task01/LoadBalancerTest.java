package dev.valekseev.jct.task01;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@Timeout(15)
class LoadBalancerTest {

    private LoadBalancer loadBalancer;

    @BeforeEach
    void setUp() {
        loadBalancer = new LoadBalancer();
    }

    @Test
    void startsWithAnEmptyUnmodifiableSnapshot() {
        List<String> snapshot = loadBalancer.getInstances();

        assertTrue(snapshot.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.add("backend"));
        assertTrue(loadBalancer.getInstances().isEmpty());
    }

    @ParameterizedTest
    @MethodSource("validAddresses")
    void registersAddressesWithoutNormalizingOrValidatingThem(List<String> addresses) {
        registerAll(loadBalancer, addresses);

        assertSameAddresses(addresses, loadBalancer.getInstances());
    }

    static Stream<List<String>> validAddresses() {
        return Stream.of(
                List.of("backend"),
                List.of("a", "A", " a", "a ", "\ta", "a\n", "not a URL"),
                addresses(10)
        );
    }

    @Test
    void returnsFalseForADuplicateWithoutChangingTheRegistry() {
        registerAll(loadBalancer, List.of("a", "b", "c"));

        assertFalse(loadBalancer.register("b"));

        assertSameAddresses(List.of("a", "b", "c"), loadBalancer.getInstances());
    }

    @Test
    void returnsFalseForADuplicateEvenWhenFull() {
        List<String> addresses = addresses(10);
        registerAll(loadBalancer, addresses);

        assertFalse(loadBalancer.register(addresses.getFirst()));

        assertSameAddresses(addresses, loadBalancer.getInstances());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n", "\r\n", " \t\n ", "\u2003"})
    void rejectsNullOrBlankAddressesWithoutChangingExistingEntries(String address) {
        registerAll(loadBalancer, List.of("existing"));

        assertThrows(IllegalArgumentException.class, () -> loadBalancer.register(address));

        assertSameAddresses(List.of("existing"), loadBalancer.getInstances());
    }

    @Test
    void rejectsAnInvalidAddressEvenWhenFull() {
        List<String> addresses = addresses(10);
        registerAll(loadBalancer, addresses);

        assertThrows(IllegalArgumentException.class, () -> loadBalancer.register(" "));

        assertSameAddresses(addresses, loadBalancer.getInstances());
    }

    @Test
    void rejectsAnEleventhUniqueAddressWithoutChangingTheRegistry() {
        List<String> addresses = addresses(10);
        registerAll(loadBalancer, addresses);

        assertThrows(IllegalStateException.class, () -> loadBalancer.register("overflow"));

        assertSameAddresses(addresses, loadBalancer.getInstances());
    }

    @Test
    void returnsAnUnmodifiableSnapshot() {
        registerAll(loadBalancer, List.of("a", "b"));
        List<String> snapshot = loadBalancer.getInstances();

        assertThrows(UnsupportedOperationException.class, () -> snapshot.add("c"));
        assertThrows(UnsupportedOperationException.class, () -> snapshot.remove("a"));
        assertThrows(UnsupportedOperationException.class, () -> snapshot.set(0, "c"));
        assertThrows(UnsupportedOperationException.class, snapshot::clear);

        assertSameAddresses(List.of("a", "b"), snapshot);
        assertSameAddresses(List.of("a", "b"), loadBalancer.getInstances());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 9})
    void keepsEarlierSnapshotsUnchangedWhileNewSnapshotsIncludeNewEntries(int initialSize) {
        List<String> initialAddresses = addresses(initialSize);
        registerAll(loadBalancer, initialAddresses);
        List<String> earlierSnapshot = loadBalancer.getInstances();

        assertTrue(loadBalancer.register("new-backend"));

        assertSameAddresses(initialAddresses, earlierSnapshot);
        List<String> expected = new ArrayList<>(initialAddresses);
        expected.add("new-backend");
        assertSameAddresses(expected, loadBalancer.getInstances());
    }

    @Test
    void keepsRegistriesIndependentBetweenInstances() {
        LoadBalancer other = new LoadBalancer();
        registerAll(loadBalancer, List.of("shared", "first"));

        assertTrue(other.getInstances().isEmpty());
        registerAll(other, List.of("shared", "second"));

        assertSameAddresses(List.of("shared", "first"), loadBalancer.getInstances());
        assertSameAddresses(List.of("shared", "second"), other.getInstances());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, Integer.MIN_VALUE})
    void rejectsNonPositiveCapacity(int capacity) {
        assertThrows(IllegalArgumentException.class, () -> new LoadBalancer(capacity));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 3, 12})
    void honoursConfiguredCapacity(int capacity) {
        LoadBalancer configured = new LoadBalancer(capacity);
        List<String> addresses = addresses(capacity);
        registerAll(configured, addresses);

        assertFalse(configured.register(addresses.getFirst()));
        assertThrows(IllegalStateException.class, () -> configured.register("overflow"));

        assertSameAddresses(addresses, configured.getInstances());
    }

    @Test
    void registersTheSameAddressExactlyOnceUnderContention() throws Exception {
        List<Callable<Boolean>> attempts = IntStream.range(0, 32)
                .mapToObj(ignored -> (Callable<Boolean>) () -> loadBalancer.register("shared"))
                .toList();

        List<Boolean> results = runConcurrently(attempts);

        assertEquals(1L, results.stream().filter(Boolean::booleanValue).count());
        assertSameAddresses(List.of("shared"), loadBalancer.getInstances());
    }

    @Test
    void registersAllDistinctAddressesUnderContention() throws Exception {
        List<String> addresses = addresses(10);
        List<Callable<Boolean>> attempts = addresses.stream()
                .map(address -> (Callable<Boolean>) () -> loadBalancer.register(address))
                .toList();

        List<Boolean> results = runConcurrently(attempts);

        assertTrue(results.stream().allMatch(Boolean::booleanValue));
        assertSameAddresses(addresses, loadBalancer.getInstances());
    }

    @Test
    void allowsExactlyOneRegistrationForTheLastFreeSlot() throws Exception {
        List<String> existingAddresses = addresses(9);
        registerAll(loadBalancer, existingAddresses);
        List<Callable<Optional<String>>> attempts = IntStream.range(0, 32)
                .mapToObj(index -> (Callable<Optional<String>>) () -> {
                    String address = "candidate-" + index;
                    try {
                        assertTrue(loadBalancer.register(address));
                        return Optional.of(address);
                    } catch (IllegalStateException expected) {
                        return Optional.<String>empty();
                    }
                })
                .toList();

        List<String> registered = runConcurrently(attempts).stream()
                .flatMap(Optional::stream)
                .toList();

        assertEquals(1, registered.size());
        List<String> expected = new ArrayList<>(existingAddresses);
        expected.addAll(registered);
        assertSameAddresses(expected, loadBalancer.getInstances());
    }

    @Test
    void concurrentRegistrationsPreserveUniqueEntriesAndSuccessCounts() throws Exception {
        List<String> addresses = addresses(256);
        LoadBalancer balancer = new LoadBalancer(addresses.size());
        List<Callable<Integer>> attempts = IntStream.range(0, 8)
                .mapToObj(ignored -> (Callable<Integer>) () -> {
                    int successfulRegistrations = 0;
                    for (String address : addresses) {
                        if (balancer.register(address)) {
                            successfulRegistrations++;
                        }
                    }
                    return successfulRegistrations;
                })
                .toList();

        List<Integer> results = runConcurrently(attempts);

        assertEquals(addresses.size(), results.stream().mapToInt(Integer::intValue).sum());
        assertSameAddresses(addresses, balancer.getInstances());
    }

    @Test
    void readsConsistentSnapshotsDuringRegistration() throws Exception {
        List<String> addresses = addresses(10);
        int readerCount = 8;
        CyclicBarrier roundStart = new CyclicBarrier(readerCount + 1);
        List<Callable<Void>> tasks = new ArrayList<>();

        tasks.add(() -> {
            for (String address : addresses) {
                roundStart.await(5, SECONDS);
                assertTrue(loadBalancer.register(address));
            }
            return null;
        });
        for (int reader = 0; reader < readerCount; reader++) {
            tasks.add(() -> {
                for (int round = 0; round < addresses.size(); round++) {
                    roundStart.await(5, SECONDS);
                    List<String> snapshot = loadBalancer.getInstances();
                    int size = snapshot.size();
                    assertTrue(size == round || size == round + 1,
                            "Snapshot must reflect the state before or after this round's registration");
                    assertSameAddresses(addresses.subList(0, size), snapshot);
                }
                return null;
            });
        }

        runConcurrently(tasks);

        assertSameAddresses(addresses, loadBalancer.getInstances());
    }

    private static List<String> addresses(int count) {
        return IntStream.range(0, count)
                .mapToObj(index -> "backend-" + index)
                .toList();
    }

    private static void registerAll(LoadBalancer balancer, List<String> addresses) {
        for (String address : addresses) {
            assertTrue(balancer.register(address));
        }
    }

    private static void assertSameAddresses(List<String> expected, List<String> actual) {
        assertEquals(expected.size(), actual.size(), "Snapshot size must match exactly");
        assertEquals(Set.copyOf(expected), Set.copyOf(actual), "Snapshot contents must match exactly");
    }

    private static <T> List<T> runConcurrently(List<Callable<T>> tasks)
            throws InterruptedException, ExecutionException, TimeoutException {
        ExecutorService executor = Executors.newFixedThreadPool(tasks.size());
        CountDownLatch ready = new CountDownLatch(tasks.size());
        CountDownLatch start = new CountDownLatch(1);
        List<Future<T>> futures = new ArrayList<>();

        try {
            for (Callable<T> task : tasks) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    assertTrue(start.await(5, SECONDS), "Workers must receive the start signal");
                    return task.call();
                }));
            }
            assertTrue(ready.await(5, SECONDS), "All workers must be ready");
            start.countDown();

            List<T> results = new ArrayList<>();
            for (Future<T> future : futures) {
                results.add(future.get(5, SECONDS));
            }
            return results;
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, SECONDS), "Executor must terminate");
        }
    }
}
