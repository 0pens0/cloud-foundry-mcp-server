package org.tanzu.cfpulse.clone;

import org.cloudfoundry.operations.applications.GetApplicationEnvironmentsRequest;
import org.cloudfoundry.operations.applications.GetApplicationRequest;
import org.cloudfoundry.operations.applications.SetEnvironmentVariableApplicationRequest;
import org.springframework.stereotype.Service;
import org.tanzu.cfpulse.cf.CfBaseService;
import org.tanzu.cfpulse.cf.CloudFoundryOperationsFactory;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for handling application configuration including environment variables and buildpack information.
 */
@Service
public class ApplicationConfigService extends CfBaseService {

    public ApplicationConfigService(CloudFoundryOperationsFactory operationsFactory) {
        super(operationsFactory, 3, 2);
    }

    /**
     * Get source application configuration (memory, disk, instances, environment variables)
     */
    public Mono<AppConfig> getSourceAppConfig(String sourceApp, String organization, String space) {
        return getOperations(organization, space).applications()
                .get(GetApplicationRequest.builder().name(sourceApp).build())
                .flatMap(appDetail -> 
                    getEnvironmentVariables(sourceApp, organization, space)
                        .map(envVars -> new AppConfig(
                                appDetail.getMemoryLimit(),
                                appDetail.getDiskQuota(), 
                                appDetail.getInstances(),
                                envVars
                        ))
                );
    }

    /**
     * Get buildpack information for an application
     */
    public Mono<String> getBuildpackInfo(String appName, String organization, String space) {
        return getOperations(organization, space).applications()
                .get(GetApplicationRequest.builder().name(appName).build())
                .map(appDetail -> {
                    List<String> buildpacks = appDetail.getBuildpacks();
                    return (buildpacks != null && !buildpacks.isEmpty()) ?
                            String.join(", ", buildpacks) : "unknown";
                });
    }

    /**
     * Get user-provided environment variables for an application
     */
    public Mono<Map<String, String>> getEnvironmentVariables(String appName, String organization, String space) {
        return getOperations(organization, space).applications()
                .getEnvironments(GetApplicationEnvironmentsRequest.builder().name(appName).build())
                .map(response -> {
                    Map<String, String> envVars = new HashMap<>();
                    // Get user-provided environment variables
                    if (response.getUserProvided() != null) {
                        response.getUserProvided().forEach((key, value) -> 
                            envVars.put(key, value != null ? value.toString() : ""));
                    }
                    return envVars;
                })
                .onErrorReturn(new HashMap<>()); // Return empty map if env vars cannot be retrieved
    }

    /**
     * Set environment variables for an application
     */
    public Mono<Void> setEnvironmentVariables(String appName, Map<String, String> envVars, String organization, String space) {
        if (envVars == null || envVars.isEmpty()) {
            return Mono.empty(); // No environment variables to set
        }
        
        // Set each environment variable sequentially using reduce to chain them
        return envVars.entrySet().stream()
                .reduce(Mono.empty(),
                        (mono, entry) -> mono.then(
                            getOperations(organization, space).applications()
                                .setEnvironmentVariable(SetEnvironmentVariableApplicationRequest.builder()
                                        .name(appName)
                                        .variableName(entry.getKey())
                                        .variableValue(entry.getValue())
                                        .build())
                                .doOnSuccess(v -> System.out.println("Set environment variable: " + entry.getKey() + "=" + entry.getValue()))
                        ),
                        Mono::then);
    }

    /**
     * Application configuration record
     */
    public record AppConfig(Integer memoryLimit, Integer diskQuota, Integer instances, Map<String, String> environmentVariables) {}
}