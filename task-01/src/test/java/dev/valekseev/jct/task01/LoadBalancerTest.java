package dev.valekseev.jct.task01;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class LoadBalancerTest {

    private static final int MAX_ADDRESSES = 10;

    @InjectMocks
    private LoadBalancer loadBalancer;

    @Test
    void whenRegisterUpToMaxValidAndUniqueAddresses_shouldRegisterSuccessfully() {
        List<String> testAddresses = List.of(
                "a", "A",
                "ab", "AB",
                "aB", "Ba",
                ".a", "a.",
                "1a", "1A"
        );

        for (String testAddress : testAddresses) {
            boolean registered = loadBalancer.register(testAddress);
            assertTrue(registered);
        }

        List<String> actualAddresses = loadBalancer.getInstances();
        assertEquals(testAddresses.size(), actualAddresses.size());
        assertTrue(actualAddresses.containsAll(testAddresses));
    }

    @Test
    void whenTryingToRegisterDuplicateAddress_shouldReturnFalse() {

    }

}
