package org.tanzu.cfpulse.cf;

import org.cloudfoundry.operations.applications.*;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;
import java.util.List;

@Service
public class CfApplicationService extends CfBaseService {

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

    public CfApplicationService(CloudFoundryOperationsFactory operationsFactory) {
        super(operationsFactory);
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
        GetApplicationRequest request = GetApplicationRequest.builder().name(applicationName).build();
        return getOperations(organization, space).applications().get(request).block();
    }

    @Tool(description = PUSH_APPLICATION)
    public void pushApplication(@ToolParam(description = NAME_PARAM) String applicationName,
                                @ToolParam(description = PATH_PARAM) String path,
                                @ToolParam(description = NO_START_PARAM, required = false) Boolean noStart,
                                @ToolParam(description = MEMORY_PARAM, required = false) Integer memory,
                                @ToolParam(description = DISK_PARAM, required = false) Integer disk,
                                @ToolParam(description = ORG_PARAM, required = false) String organization,
                                @ToolParam(description = SPACE_PARAM, required = false) String space) {
        PushApplicationRequest.Builder builder = PushApplicationRequest.builder()
                .name(applicationName)
                .path(Paths.get(path))
                .buildpack("java_buildpack_offline");
        
        if (noStart != null) {
            builder.noStart(noStart);
        }
        if (memory != null) {
            builder.memory(memory);
        }
        if (disk != null) {
            builder.diskQuota(disk);
        }
        
        PushApplicationRequest request = builder.build();
        var operations = getOperations(organization, space);
        operations.applications().push(request).block();

        SetEnvironmentVariableApplicationRequest envRequest = SetEnvironmentVariableApplicationRequest.builder().
                name(applicationName).variableName("JBP_CONFIG_OPEN_JDK_JRE").variableValue("{ jre: { version: 17.+ } }").
                build();
        operations.applications().setEnvironmentVariable(envRequest).block();

        if (noStart == null || !noStart) {
            StartApplicationRequest startApplicationRequest = StartApplicationRequest.builder().
                    name(applicationName).
                    build();
            operations.applications().start(startApplicationRequest).block();
        }
    }

    @Tool(description = SCALE_APPLICATION)
    public void scaleApplication(@ToolParam(description = NAME_PARAM) String applicationName,
                                 @ToolParam(description = INSTANCES_PARAM, required = false) Integer instances,
                                 @ToolParam(description = MEMORY_PARAM, required = false) Integer memory,
                                 @ToolParam(description = DISK_PARAM, required = false) Integer disk,
                                 @ToolParam(description = ORG_PARAM, required = false) String organization,
                                 @ToolParam(description = SPACE_PARAM, required = false) String space) {
        ScaleApplicationRequest scaleApplicationRequest = ScaleApplicationRequest.builder().
                name(applicationName).
                instances(instances).
                diskLimit(disk).
                memoryLimit(memory).
                build();
        getOperations(organization, space).applications().scale(scaleApplicationRequest).block();
    }

    @Tool(description = START_APPLICATION)
    public void startApplication(@ToolParam(description = NAME_PARAM) String applicationName,
                                @ToolParam(description = ORG_PARAM, required = false) String organization,
                                @ToolParam(description = SPACE_PARAM, required = false) String space) {
        StartApplicationRequest startApplicationRequest = StartApplicationRequest.builder().
                name(applicationName).
                build();
        getOperations(organization, space).applications().start(startApplicationRequest).block();
    }

    @Tool(description = STOP_APPLICATION)
    public void stopApplication(@ToolParam(description = NAME_PARAM) String applicationName,
                               @ToolParam(description = ORG_PARAM, required = false) String organization,
                               @ToolParam(description = SPACE_PARAM, required = false) String space) {
        StopApplicationRequest stopApplicationRequest = StopApplicationRequest.builder().
                name(applicationName).
                build();
        getOperations(organization, space).applications().stop(stopApplicationRequest).block();
    }

    @Tool(description = RESTART_APPLICATION)
    public void restartApplication(@ToolParam(description = NAME_PARAM) String applicationName,
                                  @ToolParam(description = ORG_PARAM, required = false) String organization,
                                  @ToolParam(description = SPACE_PARAM, required = false) String space) {
        RestartApplicationRequest request = RestartApplicationRequest.builder().name(applicationName).build();
        getOperations(organization, space).applications().restart(request).block();
    }

    @Tool(description = DELETE_APPLICATION)
    public void deleteApplication(@ToolParam(description = NAME_PARAM) String applicationName,
                                 @ToolParam(description = ORG_PARAM, required = false) String organization,
                                 @ToolParam(description = SPACE_PARAM, required = false) String space) {
        DeleteApplicationRequest deleteApplicationRequest = DeleteApplicationRequest.builder().
                name(applicationName).
                build();
        getOperations(organization, space).applications().delete(deleteApplicationRequest).block();
    }
}