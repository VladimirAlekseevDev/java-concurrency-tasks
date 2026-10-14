package dev.valekseev.jct.task01;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class LoadBalancerTest {

    private static final int MAX_ADDRESSES = 10;

    private LoadBalancer loadBalancer;

    @BeforeEach
    void setUp() {
        loadBalancer = new LoadBalancer();
    }

    @ParameterizedTest
    @MethodSource("provideValidAddresses")
    void whenRegisterUpToMaxValidAndUniqueAddresses_thenRegisterAllSuccessfully(List<String> testAddresses) {
        for (String testAddress : testAddresses) {
            boolean registered = loadBalancer.register(testAddress);
            assertTrue(registered);
        }

        List<String> actualAddresses = loadBalancer.getInstances();
        assertEquals(testAddresses.size(), actualAddresses.size());
        assertTrue(actualAddresses.containsAll(testAddresses));
    }

    static Stream<List<String>> provideValidAddresses() {
        return Stream.of(
                List.of("a"),
                List.of("a", "b", "c"),
                List.of(
                    "a", "A",
                    "ab", "AB",
                    "aB", "Ba",
                    ".a", "a.",
                    "1a", "1A"
                )
        );
    }

    @ParameterizedTest
    @MethodSource("provideValidAddressesAndOneDuplicate")
    void whenTryingToRegisterDuplicateAddress_thenReturnFalse(List<String> testAddresses, String duplicateAddress) {
        for (String testAddress : testAddresses) {
            boolean registered = loadBalancer.register(testAddress);
            assertTrue(registered);
        }

        boolean duplicateRegistered = loadBalancer.register(duplicateAddress);
        assertFalse(duplicateRegistered);

        List<String> actualAddresses = loadBalancer.getInstances();
        assertNotNull(actualAddresses);
        assertEquals(testAddresses.size(), actualAddresses.size());
        assertTrue(actualAddresses.containsAll(testAddresses));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void whenTryingToRegisterNullOrEmptyAddress_thenThrowsIllegalStateExc(String emptyAdress) {
        assertThrows(IllegalStateException.class, () -> loadBalancer.register(emptyAdress));
    }

    static Stream<Arguments> provideValidAddressesAndOneDuplicate() {
        return Stream.of(
          Arguments.of(List.of("a"), "a"),
          Arguments.of(List.of("a", "b", "c"), "b"),
          Arguments.of(List.of("0", "1", "2", "3", "4", "5", "6", "7", "8", "9"), "0")
        );
    }

}
