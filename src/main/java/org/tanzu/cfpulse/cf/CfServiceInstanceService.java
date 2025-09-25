package org.tanzu.cfpulse.cf;

import org.cloudfoundry.operations.services.*;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CfServiceInstanceService extends CfBaseService {

    private static final String SERVICE_INSTANCE_LIST = "Return the service instances (SIs) in a Cloud Foundry space. Organization and space parameters are optional - if not provided or null, the configured default org/space will be used automatically.";
    private static final String SERVICE_INSTANCE_DETAIL = "Get detailed information about a service instance in a Cloud Foundry space. Organization and space parameters are optional - if not provided or null, the configured default org/space will be used automatically.";
    private static final String SERVICE_OFFERINGS_LIST = "Return the service offerings available in the Cloud Foundry marketplace. Organization and space parameters are optional - if not provided or null, the configured default org/space will be used automatically.";
    private static final String CREATE_SERVICE_INSTANCE = "Create a new service instance in a Cloud Foundry space. Organization and space parameters are optional - if not provided or null, the configured default org/space will be used automatically.";
    private static final String BIND_SERVICE_INSTANCE = "Bind a service instance to a Cloud Foundry application. Organization and space parameters are optional - if not provided or null, the configured default org/space will be used automatically.";
    private static final String UNBIND_SERVICE_INSTANCE = "Unbind a service instance from a Cloud Foundry application. Organization and space parameters are optional - if not provided or null, the configured default org/space will be used automatically.";
    private static final String DELETE_SERVICE_INSTANCE = "Delete a Cloud Foundry service instance. Organization and space parameters are optional - if not provided or null, the configured default org/space will be used automatically.";
    
    private static final String SI_NAME_PARAM = "Name of the Cloud Foundry service instance";
    private static final String SERVICE_PLAN_PARAM = "Name of the service plan for the service instance";
    private static final String SERVICE_OFFERING_PARAM = "Name of the service offering for the service instance";

    public CfServiceInstanceService(CloudFoundryOperationsFactory operationsFactory) {
        super(operationsFactory);
    }

    @Tool(description = SERVICE_INSTANCE_LIST)
    public List<ServiceInstanceSummary> serviceInstancesList(
            @ToolParam(description = ORG_PARAM, required = false) String organization,
            @ToolParam(description = SPACE_PARAM, required = false) String space) {
        return getOperations(organization, space).services().listInstances().collectList().block();
    }

    @Tool(description = SERVICE_INSTANCE_DETAIL)
    public ServiceInstance serviceInstanceDetails(
            @ToolParam(description = SI_NAME_PARAM) String serviceInstanceName,
            @ToolParam(description = ORG_PARAM, required = false) String organization,
            @ToolParam(description = SPACE_PARAM, required = false) String space) {
        GetServiceInstanceRequest request = GetServiceInstanceRequest.builder().name(serviceInstanceName).build();
        return getOperations(organization, space).services().getInstance(request).block();
    }

    @Tool(description = SERVICE_OFFERINGS_LIST)
    public List<ServiceOffering> serviceOfferingsList(
            @ToolParam(description = ORG_PARAM, required = false) String organization,
            @ToolParam(description = SPACE_PARAM, required = false) String space) {
        ListServiceOfferingsRequest request = ListServiceOfferingsRequest.builder().build();
        return getOperations(organization, space).services().listServiceOfferings(request).collectList().block();
    }


    @Tool(description = CREATE_SERVICE_INSTANCE)
    public void createServiceInstance(@ToolParam(description = SI_NAME_PARAM) String serviceInstanceName,
                                    @ToolParam(description = SERVICE_OFFERING_PARAM) String serviceOffering,
                                    @ToolParam(description = SERVICE_PLAN_PARAM) String servicePlan,
                                    @ToolParam(description = ORG_PARAM, required = false) String organization,
                                    @ToolParam(description = SPACE_PARAM, required = false) String space) {
        CreateServiceInstanceRequest request = CreateServiceInstanceRequest.builder()
                .serviceInstanceName(serviceInstanceName)
                .serviceName(serviceOffering)
                .planName(servicePlan)
                .build();
        getOperations(organization, space).services().createInstance(request).block();
    }

    @Tool(description = BIND_SERVICE_INSTANCE)
    public void bindServiceInstance(@ToolParam(description = SI_NAME_PARAM) String serviceInstanceName,
                                    @ToolParam(description = NAME_PARAM) String applicationName,
                                    @ToolParam(description = ORG_PARAM, required = false) String organization,
                                    @ToolParam(description = SPACE_PARAM, required = false) String space) {
        BindServiceInstanceRequest request = BindServiceInstanceRequest.builder().
                serviceInstanceName(serviceInstanceName).
                applicationName(applicationName).
                build();
        getOperations(organization, space).services().bind(request).block();
    }

    @Tool(description = UNBIND_SERVICE_INSTANCE)
    public void unbindServiceInstance(@ToolParam(description = SI_NAME_PARAM) String serviceInstanceName,
                                      @ToolParam(description = NAME_PARAM) String applicationName,
                                      @ToolParam(description = ORG_PARAM, required = false) String organization,
                                      @ToolParam(description = SPACE_PARAM, required = false) String space) {
        UnbindServiceInstanceRequest request = UnbindServiceInstanceRequest.builder().
                serviceInstanceName(serviceInstanceName).
                applicationName(applicationName).
                build();
        getOperations(organization, space).services().unbind(request).block();
    }

    @Tool(description = DELETE_SERVICE_INSTANCE)
    public void deleteServiceInstance(@ToolParam(description = SI_NAME_PARAM) String serviceInstanceName,
                                     @ToolParam(description = ORG_PARAM, required = false) String organization,
                                     @ToolParam(description = SPACE_PARAM, required = false) String space) {
        DeleteServiceInstanceRequest request = DeleteServiceInstanceRequest.builder().
                name(serviceInstanceName).
                build();
        getOperations(organization, space).services().deleteInstance(request).block();
    }
}