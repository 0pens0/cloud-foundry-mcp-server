package org.tanzu.cfpulse.cf;

import org.cloudfoundry.operations.applications.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
public class CfApplicationService extends CfBaseService {

    private static final Logger logger = LoggerFactory.getLogger(CfApplicationService.class);

    private static final String APPLICATION_LIST = "Return the applications (apps) in a Cloud Foundry space. Organization and space parameters are optional - if not provided or null, the configured default org/space will be used automatically.";
    private static final String APPLICATION_DETAILS = "Gets detailed information about a Cloud Foundry application. Organization and space parameters are optional - if not provided or null, the configured default org/space will be used automatically.";
    private static final String PUSH_APPLICATION = "Push an application JAR file to a Cloud Foundry space. Organization and space parameters are optional - if not provided or null, the configured default org/space will be used automatically.";
    private static final String SCALE_APPLICATION = "Scale the number of instances, memory, or disk size of an application. Organization and space parameters are optional - if not provided or null, the configured default org/space will be used automatically.";
    private static final String START_APPLICATION = "Start a Cloud Foundry application. Organization and space parameters are optional - if not provided or null, the configured default org/space will be used automatically.";
    private static final String STOP_APPLICATION = "Stop a running Cloud Foundry application. Organization and space parameters are optional - if not provided or null, the configured default org/space will be used automatically.";
    private static final String RESTART_APPLICATION = "Restart a running Cloud Foundry application. Organization and space parameters are optional - if not provided or null, the configured default org/space will be used automatically.";
    private static final String DELETE_APPLICATION = "Delete a Cloud Foundry application. Organization and space parameters are optional - if not provided or null, the configured default org/space will be used automatically.";
    
    private static final String PATH_PARAM = "Fully qualified directory pathname to the compiled JAR file for the application";
    private static final String NO_START_PARAM = "Set this flag to true if you want to explicitly prevent the app from starting after being pushed.";
    private static final String INSTANCES_PARAM = "The new number of instances of the Cloud Foundry application";
    private static final String MEMORY_PARAM = "The memory limit, in megabytes, of the Cloud Foundry application";
    private static final String DISK_PARAM = "The disk size, in megabytes, of the Cloud Foundry application";

    private final String defaultBuildpack;
    private final String defaultJavaVersion;

    public CfApplicationService(CloudFoundryOperationsFactory operationsFactory,
                               @Value("${cf.buildpack:java_buildpack_offline}") String defaultBuildpack,
                               @Value("${cf.javaVersion:17.+}") String defaultJavaVersion) {
        super(operationsFactory);
        this.defaultBuildpack = defaultBuildpack;
        this.defaultJavaVersion = defaultJavaVersion;
    }

    @Tool(description = APPLICATION_LIST)
    public List<ApplicationSummary> applicationsList(
            @ToolParam(description = ORG_PARAM, required = false) String organization,
            @ToolParam(description = SPACE_PARAM, required = false) String space) {
        return getOperations(organization, space).applications().list().collectList().block();
    }

    @Tool(description = APPLICATION_DETAILS)
    public ApplicationDetail applicationDetails(
            @ToolParam(description = NAME_PARAM) String applicationName,
            @ToolParam(description = ORG_PARAM, required = false) String organization,
            @ToolParam(description = SPACE_PARAM, required = false) String space) {
        
        if (!StringUtils.hasText(applicationName)) {
            throw new IllegalArgumentException("Application name is required");
        }
        
        try {
            logger.debug("Getting application details for: {}", applicationName);
            GetApplicationRequest request = GetApplicationRequest.builder().name(applicationName).build();
            return getOperations(organization, space).applications().get(request).block();
        } catch (Exception e) {
            logger.error("Failed to get application details for: {}", applicationName, e);
            throw new RuntimeException("Failed to get application details: " + e.getMessage(), e);
        }
    }

    @Tool(description = PUSH_APPLICATION)
    public void pushApplication(@ToolParam(description = NAME_PARAM) String applicationName,
                                @ToolParam(description = PATH_PARAM) String path,
                                @ToolParam(description = NO_START_PARAM, required = false) Boolean noStart,
                                @ToolParam(description = MEMORY_PARAM, required = false) Integer memory,
                                @ToolParam(description = DISK_PARAM, required = false) Integer disk,
                                @ToolParam(description = ORG_PARAM, required = false) String organization,
                                @ToolParam(description = SPACE_PARAM, required = false) String space) {
        
        if (!StringUtils.hasText(applicationName)) {
            throw new IllegalArgumentException("Application name is required");
        }
        if (!StringUtils.hasText(path)) {
            throw new IllegalArgumentException("Application path is required");
        }
        
        Path appPath = Paths.get(path);
        if (!Files.exists(appPath)) {
            throw new IllegalArgumentException("Application path does not exist: " + path);
        }
        
        try {
            logger.info("Pushing application: {} from path: {}", applicationName, path);
            
            PushApplicationRequest.Builder builder = PushApplicationRequest.builder()
                    .name(applicationName)
                    .path(appPath)
                    .buildpack(defaultBuildpack);
            
            if (noStart != null) {
                builder.noStart(noStart);
            }
            if (memory != null && memory > 0) {
                builder.memory(memory);
            }
            if (disk != null && disk > 0) {
                builder.diskQuota(disk);
            }
            
            PushApplicationRequest request = builder.build();
            var operations = getOperations(organization, space);
            operations.applications().push(request).block();

            // Set Java version environment variable
            SetEnvironmentVariableApplicationRequest envRequest = SetEnvironmentVariableApplicationRequest.builder()
                    .name(applicationName)
                    .variableName("JBP_CONFIG_OPEN_JDK_JRE")
                    .variableValue("{ jre: { version: " + defaultJavaVersion + " } }")
                    .build();
            operations.applications().setEnvironmentVariable(envRequest).block();

            if (noStart == null || !noStart) {
                StartApplicationRequest startApplicationRequest = StartApplicationRequest.builder()
                        .name(applicationName)
                        .build();
                operations.applications().start(startApplicationRequest).block();
                logger.info("Application {} started successfully", applicationName);
            }
            
        } catch (Exception e) {
            logger.error("Failed to push application: {}", applicationName, e);
            throw new RuntimeException("Failed to push application: " + e.getMessage(), e);
        }
    }

    @Tool(description = SCALE_APPLICATION)
    public void scaleApplication(@ToolParam(description = NAME_PARAM) String applicationName,
                                 @ToolParam(description = INSTANCES_PARAM, required = false) Integer instances,
                                 @ToolParam(description = MEMORY_PARAM, required = false) Integer memory,
                                 @ToolParam(description = DISK_PARAM, required = false) Integer disk,
                                 @ToolParam(description = ORG_PARAM, required = false) String organization,
                                 @ToolParam(description = SPACE_PARAM, required = false) String space) {
        
        if (!StringUtils.hasText(applicationName)) {
            throw new IllegalArgumentException("Application name is required");
        }
        
        if (instances == null && memory == null && disk == null) {
            throw new IllegalArgumentException("At least one scaling parameter (instances, memory, or disk) must be provided");
        }
        
        try {
            logger.info("Scaling application: {} - instances: {}, memory: {}, disk: {}", 
                       applicationName, instances, memory, disk);
            
            ScaleApplicationRequest.Builder builder = ScaleApplicationRequest.builder()
                    .name(applicationName);
            
            if (instances != null && instances > 0) {
                builder.instances(instances);
            }
            if (disk != null && disk > 0) {
                builder.diskLimit(disk);
            }
            if (memory != null && memory > 0) {
                builder.memoryLimit(memory);
            }
            
            ScaleApplicationRequest scaleApplicationRequest = builder.build();
            getOperations(organization, space).applications().scale(scaleApplicationRequest).block();
            logger.info("Application {} scaled successfully", applicationName);
            
        } catch (Exception e) {
            logger.error("Failed to scale application: {}", applicationName, e);
            throw new RuntimeException("Failed to scale application: " + e.getMessage(), e);
        }
    }

    @Tool(description = START_APPLICATION)
    public void startApplication(@ToolParam(description = NAME_PARAM) String applicationName,
                                @ToolParam(description = ORG_PARAM, required = false) String organization,
                                @ToolParam(description = SPACE_PARAM, required = false) String space) {
        
        if (!StringUtils.hasText(applicationName)) {
            throw new IllegalArgumentException("Application name is required");
        }
        
        try {
            logger.info("Starting application: {}", applicationName);
            StartApplicationRequest startApplicationRequest = StartApplicationRequest.builder()
                    .name(applicationName)
                    .build();
            getOperations(organization, space).applications().start(startApplicationRequest).block();
            logger.info("Application {} started successfully", applicationName);
        } catch (Exception e) {
            logger.error("Failed to start application: {}", applicationName, e);
            throw new RuntimeException("Failed to start application: " + e.getMessage(), e);
        }
    }

    @Tool(description = STOP_APPLICATION)
    public void stopApplication(@ToolParam(description = NAME_PARAM) String applicationName,
                               @ToolParam(description = ORG_PARAM, required = false) String organization,
                               @ToolParam(description = SPACE_PARAM, required = false) String space) {
        
        if (!StringUtils.hasText(applicationName)) {
            throw new IllegalArgumentException("Application name is required");
        }
        
        try {
            logger.info("Stopping application: {}", applicationName);
            StopApplicationRequest stopApplicationRequest = StopApplicationRequest.builder()
                    .name(applicationName)
                    .build();
            getOperations(organization, space).applications().stop(stopApplicationRequest).block();
            logger.info("Application {} stopped successfully", applicationName);
        } catch (Exception e) {
            logger.error("Failed to stop application: {}", applicationName, e);
            throw new RuntimeException("Failed to stop application: " + e.getMessage(), e);
        }
    }

    @Tool(description = RESTART_APPLICATION)
    public void restartApplication(@ToolParam(description = NAME_PARAM) String applicationName,
                                  @ToolParam(description = ORG_PARAM, required = false) String organization,
                                  @ToolParam(description = SPACE_PARAM, required = false) String space) {
        
        if (!StringUtils.hasText(applicationName)) {
            throw new IllegalArgumentException("Application name is required");
        }
        
        try {
            logger.info("Restarting application: {}", applicationName);
            RestartApplicationRequest request = RestartApplicationRequest.builder()
                    .name(applicationName)
                    .build();
            getOperations(organization, space).applications().restart(request).block();
            logger.info("Application {} restarted successfully", applicationName);
        } catch (Exception e) {
            logger.error("Failed to restart application: {}", applicationName, e);
            throw new RuntimeException("Failed to restart application: " + e.getMessage(), e);
        }
    }

    @Tool(description = DELETE_APPLICATION)
    public void deleteApplication(@ToolParam(description = NAME_PARAM) String applicationName,
                                 @ToolParam(description = ORG_PARAM, required = false) String organization,
                                 @ToolParam(description = SPACE_PARAM, required = false) String space) {
        
        if (!StringUtils.hasText(applicationName)) {
            throw new IllegalArgumentException("Application name is required");
        }
        
        try {
            logger.warn("Deleting application: {}", applicationName);
            DeleteApplicationRequest deleteApplicationRequest = DeleteApplicationRequest.builder()
                    .name(applicationName)
                    .build();
            getOperations(organization, space).applications().delete(deleteApplicationRequest).block();
            logger.info("Application {} deleted successfully", applicationName);
        } catch (Exception e) {
            logger.error("Failed to delete application: {}", applicationName, e);
            throw new RuntimeException("Failed to delete application: " + e.getMessage(), e);
        }
    }
}