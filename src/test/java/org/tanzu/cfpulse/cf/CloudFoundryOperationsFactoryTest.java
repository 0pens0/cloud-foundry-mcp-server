package org.tanzu.cfpulse.cf;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.doppler.DopplerClient;
import org.cloudfoundry.networking.NetworkingClient;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.uaa.UaaClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CloudFoundryOperationsFactoryTest {

    @Mock
    private CloudFoundryClient cloudFoundryClient;

    @Mock
    private DopplerClient dopplerClient;

    @Mock
    private UaaClient uaaClient;

    @Mock
    private NetworkingClient networkingClient;

    private CloudFoundryOperationsFactory factory;

    @BeforeEach
    void setUp() {
        factory = new CloudFoundryOperationsFactory(
                cloudFoundryClient,
                dopplerClient,
                uaaClient,
                networkingClient,
                "test-org",
                "test-space"
        );
    }

    @Test
    void testGetDefaultOperations_ShouldReturnSameInstance() {
        // When
        CloudFoundryOperations operations1 = factory.getDefaultOperations();
        CloudFoundryOperations operations2 = factory.getDefaultOperations();

        // Then
        assertNotNull(operations1);
        assertNotNull(operations2);
        assertSame(operations1, operations2);
    }

    @Test
    void testGetOperations_WithDefaultOrgAndSpace_ShouldReturnDefaultOperations() {
        // When
        CloudFoundryOperations operations = factory.getOperations("test-org", "test-space");

        // Then
        assertNotNull(operations);
        assertSame(factory.getDefaultOperations(), operations);
    }

    @Test
    void testGetOperations_WithDifferentOrgAndSpace_ShouldReturnNewInstance() {
        // When
        CloudFoundryOperations operations1 = factory.getOperations("org1", "space1");
        CloudFoundryOperations operations2 = factory.getOperations("org1", "space1");

        // Then
        assertNotNull(operations1);
        assertNotNull(operations2);
        assertSame(operations1, operations2);
    }

    @Test
    void testGetOperations_WithNullParameters_ShouldReturnDefaultOperations() {
        // When
        CloudFoundryOperations operations = factory.getOperations(null, null);

        // Then
        assertNotNull(operations);
        assertSame(factory.getDefaultOperations(), operations);
    }

    @Test
    void testClearCache_ShouldResetDefaultOperations() {
        // Given
        CloudFoundryOperations originalOperations = factory.getDefaultOperations();

        // When
        factory.clearCache();
        CloudFoundryOperations newOperations = factory.getDefaultOperations();

        // Then
        assertNotNull(newOperations);
        assertNotSame(originalOperations, newOperations);
    }

    @Test
    void testGetCacheSize_ShouldReturnCorrectSize() {
        // Given
        factory.getOperations("org1", "space1");
        factory.getOperations("org2", "space2");

        // When
        int cacheSize = factory.getCacheSize();

        // Then
        assertEquals(2, cacheSize);
    }

    @Test
    void testGetCachedContexts_ShouldReturnCorrectContexts() {
        // Given
        factory.getOperations("org1", "space1");
        factory.getOperations("org2", "space2");

        // When
        var contexts = factory.getCachedContexts();

        // Then
        assertEquals(2, contexts.size());
        assertTrue(contexts.contains("org1:space1"));
        assertTrue(contexts.contains("org2:space2"));
    }

    @Test
    void testGetDefaultSpace_ShouldReturnCorrectSpace() {
        // When
        String defaultSpace = factory.getDefaultSpace();

        // Then
        assertEquals("test-space", defaultSpace);
    }
}
