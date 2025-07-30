package org.tanzu.cfpulse.clone;

import org.cloudfoundry.operations.applications.*;
import org.springframework.stereotype.Service;
import org.tanzu.cfpulse.cf.CfBaseService;
import org.tanzu.cfpulse.cf.CloudFoundryOperationsFactory;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;

/**
 * Service for handling Cloud Foundry application deployment operations.
 */
@Service
public class ApplicationDeploymentService extends CfBaseService {

    private final ApplicationConfigService configService;

    public ApplicationDeploymentService(CloudFoundryOperationsFactory operationsFactory, ApplicationConfigService configService) {
        super(operationsFactory);
        this.configService = configService;
    }

    /**
     * Deploy a placeholder application with the specified buildpack and configuration
     */
    public Mono<Void> deployPlaceholderWithSourceBuildpack(String appName, Path placeholderPath, String buildpack, 
                                                          String organization, String space, ApplicationConfigService.AppConfig config) {
        return getOperations(organization, space).applications()
                .push(PushApplicationRequest.builder()
                        .name(appName)
                        .path(placeholderPath)
                        .noStart(true)           // Don't start placeholder
                        .memory(config.memoryLimit())    // Match source app memory
                        .diskQuota(config.diskQuota())   // Match source app disk
                        .instances(config.instances())   // Match source app instances
                        .buildpack(buildpack)           // Use source app's buildpack
                        .stagingTimeout(Duration.ofMinutes(3)) // Allow time for buildpack-specific staging
                        .build())
                .then(configService.setEnvironmentVariables(appName, config.environmentVariables(), organization, space));
    }

    /**
     * Copy source application to target with buildpack verification
     */
    public Mono<Void> copySourceWithBuildpackVerification(String sourceApp, String targetApp, String expectedBuildpack, 
                                                         String organization, String space, ApplicationConfigService.AppConfig config) {
        return getOperations(organization, space).applications()
                .copySource(CopySourceApplicationRequest.builder()
                        .name(sourceApp)
                        .targetName(targetApp)
                        .restart(false)
                        .stagingTimeout(Duration.ofMinutes(8))
                        .startupTimeout(Duration.ofMinutes(5))
                        .build())
                .doOnSuccess(v -> System.out.println("Copied source from " + sourceApp + " to " + targetApp))
                .then(
                        // Scale to match source configuration
                        getOperations(organization, space).applications()
                                .scale(ScaleApplicationRequest.builder()
                                        .name(targetApp)
                                        .memoryLimit(config.memoryLimit())
                                        .diskLimit(config.diskQuota())
                                        .instances(config.instances())
                                        .build())
                                .doOnSuccess(v -> System.out.println("Scaled app to match source configuration"))
                )
                .then(
                        // Start the app with the existing buildpack
                        getOperations(organization, space).applications()
                                .start(StartApplicationRequest.builder()
                                        .name(targetApp)
                                        .stagingTimeout(Duration.ofMinutes(8))
                                        .startupTimeout(Duration.ofMinutes(5))
                                        .build())
                                .doOnSuccess(v -> System.out.println("Started app with pre-configured buildpack"))
                )
                .then(
                        // Verify buildpack matches expected (should be guaranteed now)
                        configService.getBuildpackInfo(targetApp, organization, space)
                                .flatMap(actualBuildpack -> {
                                    if (expectedBuildpack.equals(actualBuildpack)) {
                                        System.out.println("✓ SUCCESS: Buildpack preserved during source copy - " + actualBuildpack);
                                        return Mono.empty();
                                    } else {
                                        String errorMsg = String.format(
                                                "❌ UNEXPECTED: Buildpack changed during copy! Expected: %s, Actual: %s. " +
                                                "This should not happen with buildpack-matched placeholder.",
                                                expectedBuildpack, actualBuildpack);
                                        System.err.println(errorMsg);
                                        return Mono.error(new RuntimeException(errorMsg));
                                    }
                                })
                );
    }

    /**
     * Clean up temporary directory and files
     */
    public void cleanup(Path tempDir) {
        if (tempDir == null) return;

        try (var pathStream = Files.walk(tempDir)) {
            pathStream.sorted(Comparator.reverseOrder()) // Critical: files before directories
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            // Silent cleanup - don't break main flow
                            System.err.println("Warning: Could not delete " + path);
                        }
                    });
        } catch (IOException e) {
            System.err.println("Warning: Could not traverse directory " + tempDir);
        }
    }
}