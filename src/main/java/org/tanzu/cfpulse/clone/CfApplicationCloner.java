package org.tanzu.cfpulse.clone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.tanzu.cfpulse.cf.CfBaseService;
import org.tanzu.cfpulse.cf.CloudFoundryOperationsFactory;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Application cloning service that creates buildpack-specific placeholders to ensure consistent deployments.
 * 
 * This approach ensures buildpack consistency by:
 * 1. Detecting source app's buildpack and configuration
 * 2. Creating buildpack-specific placeholder content
 * 3. Deploying placeholder with source app's exact buildpack
 * 4. Copying source over placeholder to preserve buildpack
 * 5. Scaling and starting with verified buildpack matching
 */
@Service
public class CfApplicationCloner extends CfBaseService {

    private static final Logger logger = LoggerFactory.getLogger(CfApplicationCloner.class);

    private final ApplicationConfigService configService;
    private final BuildpackPlaceholderGenerator placeholderGenerator;
    private final ApplicationDeploymentService deploymentService;

    public CfApplicationCloner(CloudFoundryOperationsFactory operationsFactory,
                              ApplicationConfigService configService,
                              BuildpackPlaceholderGenerator placeholderGenerator,
                              ApplicationDeploymentService deploymentService) {
        super(operationsFactory, 3, 2);
        this.configService = configService;
        this.placeholderGenerator = placeholderGenerator;
        this.deploymentService = deploymentService;
    }

    /**
     * Clone an existing Cloud Foundry application by creating a buildpack-specific placeholder
     */
    @Tool(description = "Clone an existing Cloud Foundry application to create a copy with a new name. Uses buildpack-specific placeholders to ensure consistent deployments.")
    public void cloneApp(
            @ToolParam(description = "Source application name") String sourceApp,
            @ToolParam(description = "Target application name") String targetApp,
            @ToolParam(description = "Organization name (optional)", required = false) String organization,
            @ToolParam(description = "Space name (optional)", required = false) String space) {
        
        logger.info("Starting clone operation: {} -> {}", sourceApp, targetApp);
        
        try {
            // First get both source app config AND buildpack info
            Mono.zip(
                configService.getSourceAppConfig(sourceApp, organization, space),
                configService.getBuildpackInfo(sourceApp, organization, space)
            )
                    .flatMap(tuple -> {
                        ApplicationConfigService.AppConfig config = tuple.getT1();
                        String sourceBuildpack = tuple.getT2();
                        
                        logger.debug("Retrieved source app info: memory={}, disk={}, instances={}, buildpack={}, env vars={}", 
                                   config.memoryLimit(), config.diskQuota(), config.instances(), 
                                   sourceBuildpack, config.environmentVariables().size());
                        
                        return Mono.fromCallable(() -> placeholderGenerator.createPlaceholder(targetApp, sourceBuildpack))
                                .flatMap(placeholderPath -> {
                                    logger.debug("Created buildpack placeholder for: {}", sourceBuildpack);
                                    
                                    return deploymentService.deployPlaceholderWithSourceBuildpack(targetApp, placeholderPath, sourceBuildpack, organization, space, config)
                                            .doOnSuccess(v -> logger.debug("Placeholder deployed with matching buildpack: {}", sourceBuildpack))
                                            .then(
                                                    // Step 2: Copy application source (buildpack already matches)
                                                    deploymentService.copySourceWithBuildpackVerification(sourceApp, targetApp, sourceBuildpack, organization, space, config)
                                                            .doOnSuccess(v -> logger.debug("Source copy completed with buildpack verification"))
                                            )
                                            .doFinally(signal -> {
                                                logger.debug("Cleaning up temporary files...");
                                                deploymentService.cleanup(placeholderPath);
                                            });
                                });
                    })
                    .timeout(Duration.ofMinutes(10)) // 10 minute timeout for the entire operation
                    .block(); // Block to make it synchronous for MCP
            
            logger.info("Clone operation completed successfully: {} -> {}", sourceApp, targetApp);
            
        } catch (Exception e) {
            logger.error("Clone operation failed: {} -> {}", sourceApp, targetApp, e);
            throw new RuntimeException("Failed to clone application: " + sourceApp + " -> " + targetApp, e);
        }
    }
}