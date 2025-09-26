package org.tanzu.cfpulse.cf;

import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.applications.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CfApplicationServiceTest {

    @Mock
    private CloudFoundryOperationsFactory operationsFactory;

    @Mock
    private CloudFoundryOperations operations;

    @Mock
    private Applications applications;

    private CfApplicationService cfApplicationService;

    @BeforeEach
    void setUp() {
        cfApplicationService = new CfApplicationService(operationsFactory, "java_buildpack_offline", "17.+");
        lenient().when(operationsFactory.getOperations(any(), any())).thenReturn(operations);
        lenient().when(operationsFactory.getDefaultOperations()).thenReturn(operations);
        lenient().when(operations.applications()).thenReturn(applications);
    }

    @Test
    void testApplicationDetails_WithValidName_ShouldReturnDetails() {
        // Given
        String appName = "test-app";
        ApplicationDetail expectedDetail = ApplicationDetail.builder()
                .name(appName)
                .id("test-id")
                .diskQuota(1024)
                .instances(1)
                .memoryLimit(512)
                .requestedState("STOPPED")
                .runningInstances(0)
                .stack("cflinuxfs4")
                .build();
        
        when(applications.get(any(GetApplicationRequest.class)))
                .thenReturn(Mono.just(expectedDetail));

        // When
        ApplicationDetail result = cfApplicationService.applicationDetails(appName, null, null);

        // Then
        assertNotNull(result);
        assertEquals(appName, result.getName());
        verify(applications).get(any(GetApplicationRequest.class));
    }

    @Test
    void testApplicationDetails_WithEmptyName_ShouldThrowException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
                () -> cfApplicationService.applicationDetails("", null, null));
        assertEquals("Application name is required", exception.getMessage());
    }

    @Test
    void testApplicationDetails_WithNullName_ShouldThrowException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
                () -> cfApplicationService.applicationDetails(null, null, null));
        assertEquals("Application name is required", exception.getMessage());
    }

    @Test
    void testScaleApplication_WithValidParameters_ShouldScale() {
        // Given
        String appName = "test-app";
        Integer instances = 3;
        Integer memory = 512;
        Integer disk = 1024;
        
        when(applications.scale(any(ScaleApplicationRequest.class)))
                .thenReturn(Mono.empty());

        // When
        cfApplicationService.scaleApplication(appName, instances, memory, disk, null, null);

        // Then
        verify(applications).scale(any(ScaleApplicationRequest.class));
    }

    @Test
    void testScaleApplication_WithNoParameters_ShouldThrowException() {
        // Given
        String appName = "test-app";

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
                () -> cfApplicationService.scaleApplication(appName, null, null, null, null, null));
        assertEquals("At least one scaling parameter (instances, memory, or disk) must be provided", 
                    exception.getMessage());
    }

    @Test
    void testStartApplication_WithValidName_ShouldStart() {
        // Given
        String appName = "test-app";
        
        when(applications.start(any(StartApplicationRequest.class)))
                .thenReturn(Mono.empty());

        // When
        cfApplicationService.startApplication(appName, null, null);

        // Then
        verify(applications).start(any(StartApplicationRequest.class));
    }

    @Test
    void testStopApplication_WithValidName_ShouldStop() {
        // Given
        String appName = "test-app";
        
        when(applications.stop(any(StopApplicationRequest.class)))
                .thenReturn(Mono.empty());

        // When
        cfApplicationService.stopApplication(appName, null, null);

        // Then
        verify(applications).stop(any(StopApplicationRequest.class));
    }

    @Test
    void testRestartApplication_WithValidName_ShouldRestart() {
        // Given
        String appName = "test-app";
        
        when(applications.restart(any(RestartApplicationRequest.class)))
                .thenReturn(Mono.empty());

        // When
        cfApplicationService.restartApplication(appName, null, null);

        // Then
        verify(applications).restart(any(RestartApplicationRequest.class));
    }

    @Test
    void testDeleteApplication_WithValidName_ShouldDelete() {
        // Given
        String appName = "test-app";
        
        when(applications.delete(any(DeleteApplicationRequest.class)))
                .thenReturn(Mono.empty());

        // When
        cfApplicationService.deleteApplication(appName, null, null);

        // Then
        verify(applications).delete(any(DeleteApplicationRequest.class));
    }

    @Test
    void testApplicationsList_ShouldReturnList() {
        // Given
        List<ApplicationSummary> expectedList = List.of(
                ApplicationSummary.builder()
                        .name("app1")
                        .id("id1")
                        .diskQuota(1024)
                        .instances(1)
                        .memoryLimit(512)
                        .requestedState("STOPPED")
                        .runningInstances(0)
                        .build(),
                ApplicationSummary.builder()
                        .name("app2")
                        .id("id2")
                        .diskQuota(1024)
                        .instances(1)
                        .memoryLimit(512)
                        .requestedState("STOPPED")
                        .runningInstances(0)
                        .build()
        );
        
        when(applications.list()).thenReturn(reactor.core.publisher.Flux.fromIterable(expectedList));

        // When
        List<ApplicationSummary> result = cfApplicationService.applicationsList(null, null);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(applications).list();
    }
}