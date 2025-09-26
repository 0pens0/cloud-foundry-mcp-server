package org.tanzu.cfpulse.cf;

import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.spaces.Spaces;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CfTargetServiceTest {

    @Mock
    private CloudFoundryOperationsFactory operationsFactory;

    @Mock
    private CloudFoundryOperations cloudFoundryOperations;

    @Mock
    private Spaces spaces;

    private CfTargetService cfTargetService;

    @BeforeEach
    void setUp() {
        cfTargetService = new CfTargetService(operationsFactory);
    }

    @Test
    void testGetCurrentTarget_WhenNoTargetSet_ReturnsConfigurationDefaults() {
        // Given
        when(operationsFactory.getDefaultOrganization()).thenReturn("default-org");
        when(operationsFactory.getDefaultSpace()).thenReturn("default-space");

        // When
        CfTargetService.TargetInfo targetInfo = cfTargetService.getCurrentTarget();

        // Then
        assertNotNull(targetInfo);
        assertEquals("default-org", targetInfo.getOrganization());
        assertEquals("default-space", targetInfo.getSpace());
        assertTrue(targetInfo.isConfigured());
    }

    @Test
    void testGetCurrentTarget_WhenTargetNotConfigured_ReturnsNotConfigured() {
        // Given
        when(operationsFactory.getDefaultOrganization()).thenReturn("");
        when(operationsFactory.getDefaultSpace()).thenReturn("");

        // When
        CfTargetService.TargetInfo targetInfo = cfTargetService.getCurrentTarget();

        // Then
        assertNotNull(targetInfo);
        assertEquals("", targetInfo.getOrganization());
        assertEquals("", targetInfo.getSpace());
        assertFalse(targetInfo.isConfigured());
    }

    @Test
    void testTargetCf_ValidatesAndSetsTarget() {
        // Given
        String organization = "test-org";
        String space = "test-space";
        
        when(operationsFactory.getOperations(organization, space)).thenReturn(cloudFoundryOperations);
        when(cloudFoundryOperations.spaces()).thenReturn(spaces);
        when(spaces.list()).thenReturn(Flux.empty());

        // When
        cfTargetService.targetCf(organization, space);

        // Then
        verify(operationsFactory).setDefaultTarget(organization, space);
    }

    @Test
    void testClearTarget_ClearsCurrentTarget() {
        // When
        cfTargetService.clearTarget();

        // Then
        verify(operationsFactory).clearDefaultTarget();
    }

    @Test
    void testTargetInfoToString_WhenConfigured_ReturnsFormattedString() {
        // Given
        CfTargetService.TargetInfo targetInfo = new CfTargetService.TargetInfo("test-org", "test-space");

        // When
        String result = targetInfo.toString();

        // Then
        assertEquals("Organization: test-org, Space: test-space", result);
    }

    @Test
    void testTargetInfoToString_WhenNotConfigured_ReturnsNotSetMessage() {
        // Given
        CfTargetService.TargetInfo targetInfo = new CfTargetService.TargetInfo("", "");

        // When
        String result = targetInfo.toString();

        // Then
        assertEquals("No target set (using configuration defaults)", result);
    }
}
