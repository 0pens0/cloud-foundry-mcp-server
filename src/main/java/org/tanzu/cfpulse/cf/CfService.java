package org.tanzu.cfpulse.cf;

import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.applications.*;
import org.cloudfoundry.operations.networkpolicies.AddNetworkPolicyRequest;
import org.cloudfoundry.operations.networkpolicies.ListNetworkPoliciesRequest;
import org.cloudfoundry.operations.networkpolicies.Policy;
import org.cloudfoundry.operations.networkpolicies.RemoveNetworkPolicyRequest;
import org.cloudfoundry.operations.organizations.OrganizationSummary;
import org.cloudfoundry.operations.routes.*;
import org.cloudfoundry.operations.services.*;
import org.cloudfoundry.operations.spaceadmin.GetSpaceQuotaRequest;
import org.cloudfoundry.operations.spaceadmin.SpaceQuota;
import org.cloudfoundry.operations.spaces.CreateSpaceRequest;
import org.cloudfoundry.operations.spaces.DeleteSpaceRequest;
import org.cloudfoundry.operations.spaces.RenameSpaceRequest;
import org.cloudfoundry.operations.spaces.SpaceSummary;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;
import java.util.List;

@Service
public class CfService {

    private final CloudFoundryOperationsFactory operationsFactory;

    public CfService(CloudFoundryOperationsFactory operationsFactory) {
        this.operationsFactory = operationsFactory;
    }

    /*
        Applications
    */
    private static final String APPLICATION_LIST = "Return the applications (apps) in a Cloud Foundry space. Organization and space parameters are optional - if not provided or null, the configured default org/space will be used automatically.";
    private static final String ORG_PARAM = "Name of the Cloud Foundry organization. Optional - can be null or omitted to use the configured default organization.";
    private static final String SPACE_PARAM = "Name of the Cloud Foundry space. Optional - can be null or omitted to use the configured default space.";

    @Tool(description = APPLICATION_LIST)
    public List<ApplicationSummary> applicationsList(
            @ToolParam(description = ORG_PARAM, required = false) String organization,
            @ToolParam(description = SPACE_PARAM, required = false) String space) {
        return getOperations(organization, space).applications().list().collectList().block();
    }

    private static final String APPLICATION_DETAILS = "Gets detailed information about a Cloud Foundry application. Organization and space parameters are optional - if not provided or null, the configured default org/space will be used automatically.";

    @Tool(description = APPLICATION_DETAILS)
    public ApplicationDetail applicationDetails(
            @ToolParam(description = NAME_PARAM) String applicationName,
            @ToolParam(description = ORG_PARAM, required = false) String organization,
            @ToolParam(description = SPACE_PARAM, required = false) String space) {
        GetApplicationRequest request = GetApplicationRequest.builder().name(applicationName).build();
        return getOperations(organization, space).applications().get(request).block();
    }


    private static final String PUSH_APPLICATION = "Push an application JAR file to a Cloud Foundry space. Organization and space parameters are optional - if not provided or null, the configured default org/space will be used automatically.";
    private static final String NAME_PARAM = "Name of the Cloud Foundry application";
    private static final String PATH_PARAM = "Fully qualified directory pathname to the compiled JAR file for the application";
    private static final String NO_START_PARAM = "Set this flag to true if you want to explicitly prevent the app from starting after being pushed.";

    @Tool(description = PUSH_APPLICATION)
    public void pushApplication(@ToolParam(description = NAME_PARAM) String applicationName,
                                @ToolParam(description = PATH_PARAM) String path,
                                @ToolParam(description = NO_START_PARAM, required = false) Boolean noStart,
                                @ToolParam(description = MEMORY_PARAM, required = false) Integer memory,
                                @ToolParam(description = DISK_PARAM, required = false) Integer disk,
                                @ToolParam(description = ORG_PARAM, required = false) String organization,
                                @ToolParam(description = SPACE_PARAM, required = false) String space) {
        PushApplicationRequest request = PushApplicationRequest.builder().
                name(applicationName).
                path(Paths.get(path)).
                noStart(true).
                buildpack("java_buildpack_offline").
                memory(memory).
                diskQuota(disk).
                build();
        CloudFoundryOperations operations = getOperations(organization, space);
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

    private static final String SCALE_APPLICATION = "Scale the number of instances, memory, or disk size of an application. Organization and space parameters are optional - if not provided or null, the configured default org/space will be used automatically.";
    private static final String INSTANCES_PARAM = "The new number of instances of the Cloud Foundry application";
    private static final String MEMORY_PARAM = "The memory limit, in megabytes, of the Cloud Foundry application";
    private static final String DISK_PARAM = "The disk size, in megabytes, of the Cloud Foundry application";

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

    private static final String START_APPLICATION = "Start a Cloud Foundry application. Organization and space parameters are optional - if not provided or null, the configured default org/space will be used automatically.";

    @Tool(description = START_APPLICATION)
    public void startApplication(@ToolParam(description = NAME_PARAM) String applicationName,
                                @ToolParam(description = ORG_PARAM, required = false) String organization,
                                @ToolParam(description = SPACE_PARAM, required = false) String space) {
        StartApplicationRequest startApplicationRequest = StartApplicationRequest.builder().
                name(applicationName).
                build();
        getOperations(organization, space).applications().start(startApplicationRequest).block();
    }

    private static final String STOP_APPLICATION = "Stop a running Cloud Foundry application. Organization and space parameters are optional - if not provided or null, the configured default org/space will be used automatically.";

    @Tool(description = STOP_APPLICATION)
    public void stopApplication(@ToolParam(description = NAME_PARAM) String applicationName,
                               @ToolParam(description = ORG_PARAM, required = false) String organization,
                               @ToolParam(description = SPACE_PARAM, required = false) String space) {
        StopApplicationRequest stopApplicationRequest = StopApplicationRequest.builder().
                name(applicationName).
                build();
        getOperations(organization, space).applications().stop(stopApplicationRequest).block();
    }

    private static final String RESTART_APPLICATION = "Restart a running Cloud Foundry application. Organization and space parameters are optional - if not provided or null, the configured default org/space will be used automatically.";

    @Tool(description = RESTART_APPLICATION)
    public void restartApplication(@ToolParam(description = NAME_PARAM) String applicationName,
                                  @ToolParam(description = ORG_PARAM, required = false) String organization,
                                  @ToolParam(description = SPACE_PARAM, required = false) String space) {
        RestartApplicationRequest request = RestartApplicationRequest.builder().name(applicationName).build();
        getOperations(organization, space).applications().restart(request).block();
    }

    private static final String DELETE_APPLICATION = "Delete a Cloud Foundry application. Organization and space parameters are optional - if not provided or null, the configured default org/space will be used automatically.";

    @Tool(description = DELETE_APPLICATION)
    public void deleteApplication(@ToolParam(description = NAME_PARAM) String applicationName,
                                 @ToolParam(description = ORG_PARAM, required = false) String organization,
                                 @ToolParam(description = SPACE_PARAM, required = false) String space) {
        DeleteApplicationRequest deleteApplicationRequest = DeleteApplicationRequest.builder().
                name(applicationName).
                build();
        getOperations(organization, space).applications().delete(deleteApplicationRequest).block();
    }

    /*
        Organizations
     */
    private static final String ORGANIZATION_LIST = "Return the organizations (orgs) in the Cloud Foundry foundation";

    @Tool(description = ORGANIZATION_LIST)
    public List<OrganizationSummary> organizationsList() {
        return operationsFactory.getDefaultOperations().organizations().list().collectList().block();
    }

    /*
        Services
     */
    private static final String SERVICE_INSTANCE_LIST = "Return the service instances (SIs) in a Cloud Foundry space. Organization and space parameters are optional - if not provided or null, the configured default org/space will be used automatically.";

    @Tool(description = SERVICE_INSTANCE_LIST)
    public List<ServiceInstanceSummary> serviceInstancesList(
            @ToolParam(description = ORG_PARAM, required = false) String organization,
            @ToolParam(description = SPACE_PARAM, required = false) String space) {
        return getOperations(organization, space).services().listInstances().collectList().block();
    }

    private static final String SERVICE_INSTANCE_DETAIL = "Get detailed information about a service instance in a Cloud Foundry space. Organization and space parameters are optional - if not provided or null, the configured default org/space will be used automatically.";

    @Tool(description = SERVICE_INSTANCE_DETAIL)
    public ServiceInstance serviceInstanceDetails(
            @ToolParam(description = NAME_PARAM) String serviceInstanceName,
            @ToolParam(description = ORG_PARAM, required = false) String organization,
            @ToolParam(description = SPACE_PARAM, required = false) String space) {
        GetServiceInstanceRequest request = GetServiceInstanceRequest.builder().name(serviceInstanceName).build();
        return getOperations(organization, space).services().getInstance(request).block();
    }

    private static final String SERVICE_OFFERINGS_LIST = "Return the service offerings available in the Cloud Foundry marketplace. Organization and space parameters are optional - if not provided or null, the configured default org/space will be used automatically.";

    @Tool(description = SERVICE_OFFERINGS_LIST)
    public List<ServiceOffering> serviceOfferingsList(
            @ToolParam(description = ORG_PARAM, required = false) String organization,
            @ToolParam(description = SPACE_PARAM, required = false) String space) {
        ListServiceOfferingsRequest request = ListServiceOfferingsRequest.builder().build();
        return getOperations(organization, space).services().listServiceOfferings(request).collectList().block();
    }

    private static final String BIND_SERVICE_INSTANCE = "Bind a service instance to a Cloud Foundry application. Organization and space parameters are optional - if not provided or null, the configured default org/space will be used automatically.";
    private static final String SI_NAME_PARAM = "Name of the Cloud Foundry service instance";

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

    private static final String UNBIND_SERVICE_INSTANCE = "Unbind a service instance from a Cloud Foundry application. Organization and space parameters are optional - if not provided or null, the configured default org/space will be used automatically.";

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

    private static final String DELETE_SERVICE_INSTANCE = "Delete a Cloud Foundry service instance. Organization and space parameters are optional - if not provided or null, the configured default org/space will be used automatically.";

    @Tool(description = DELETE_SERVICE_INSTANCE)
    public void deleteServiceInstance(@ToolParam(description = SI_NAME_PARAM) String serviceInstanceName,
                                     @ToolParam(description = ORG_PARAM, required = false) String organization,
                                     @ToolParam(description = SPACE_PARAM, required = false) String space) {
        DeleteServiceInstanceRequest request = DeleteServiceInstanceRequest.builder().
                name(serviceInstanceName).
                build();
        getOperations(organization, space).services().deleteInstance(request).block();
    }

    /*
        Spaces
     */
    private static final String SPACE_LIST = "Returns the spaces in a Cloud Foundry organization (org). Organization parameter is optional - if not provided or null, the configured default organization will be used automatically.";

    @Tool(description = SPACE_LIST)
    public List<SpaceSummary> spacesList(
            @ToolParam(description = ORG_PARAM, required = false) String organization) {
        return getOperations(organization, null).spaces().list().collectList().block();
    }

    private static final String GET_SPACE_QUOTA = "Returns a quota (set of resource limits) scoped to a Cloud Foundry space. Organization parameter is optional - if not provided or null, the configured default organization will be used automatically.";
    private static final String SPACE_QUOTA_NAME_PARAM = "Name of the Cloud Foundry space quota";

    @Tool(description = GET_SPACE_QUOTA)
    public SpaceQuota getSpaceQuota(@ToolParam(description = SPACE_QUOTA_NAME_PARAM) String spaceName,
                                   @ToolParam(description = ORG_PARAM, required = false) String organization) {
        GetSpaceQuotaRequest request = GetSpaceQuotaRequest.builder().name(spaceName).build();
        return getOperations(organization, null).spaceAdmin().get(request).block();
    }

    private static final String CREATE_SPACE = "Create a new Cloud Foundry space in an organization. Organization parameter is optional - if not provided or null, the configured default organization will be used automatically.";
    private static final String SPACE_NAME_PARAM = "Name of the Cloud Foundry space";
    private static final String SPACE_QUOTA_PARAM = "Name of the space quota to apply to the new space (optional)";

    @Tool(description = CREATE_SPACE)
    public void createSpace(@ToolParam(description = SPACE_NAME_PARAM) String spaceName,
                           @ToolParam(description = ORG_PARAM, required = false) String organization,
                           @ToolParam(description = SPACE_QUOTA_PARAM, required = false) String spaceQuota) {
        CreateSpaceRequest.Builder builder = CreateSpaceRequest.builder().name(spaceName);
        if (organization != null) {
            builder.organization(organization);
        }
        if (spaceQuota != null) {
            builder.spaceQuota(spaceQuota);
        }
        CreateSpaceRequest request = builder.build();
        getOperations(organization, null).spaces().create(request).block();
    }

    private static final String DELETE_SPACE = "Delete a Cloud Foundry space. Organization parameter is optional - if not provided or null, the configured default organization will be used automatically.";

    @Tool(description = DELETE_SPACE)
    public void deleteSpace(@ToolParam(description = SPACE_NAME_PARAM) String spaceName,
                           @ToolParam(description = ORG_PARAM, required = false) String organization) {
        DeleteSpaceRequest request = DeleteSpaceRequest.builder().name(spaceName).build();
        getOperations(organization, null).spaces().delete(request).block();
    }

    private static final String RENAME_SPACE = "Rename a Cloud Foundry space. Organization parameter is optional - if not provided or null, the configured default organization will be used automatically.";
    private static final String NEW_SPACE_NAME_PARAM = "New name for the Cloud Foundry space";

    @Tool(description = RENAME_SPACE)
    public void renameSpace(@ToolParam(description = SPACE_NAME_PARAM) String currentSpaceName,
                           @ToolParam(description = NEW_SPACE_NAME_PARAM) String newSpaceName,
                           @ToolParam(description = ORG_PARAM, required = false) String organization) {
        RenameSpaceRequest request = RenameSpaceRequest.builder()
                .name(currentSpaceName)
                .newName(newSpaceName)
                .build();
        getOperations(organization, null).spaces().rename(request).block();
    }

    /*
        Routes
     */
    private static final String ROUTE_LIST = "Return the routes in a Cloud Foundry space. Organization and space parameters are optional - if not provided or null, the configured default org/space will be used automatically.";

    @Tool(description = ROUTE_LIST)
    public List<org.cloudfoundry.operations.routes.Route> routesList(
            @ToolParam(description = ORG_PARAM, required = false) String organization,
            @ToolParam(description = SPACE_PARAM, required = false) String space) {
        ListRoutesRequest request = ListRoutesRequest.builder().build();
        return getOperations(organization, space).routes().list(request).collectList().block();
    }

    private static final String CREATE_ROUTE = "Create a new route in a Cloud Foundry space. Organization and space parameters are optional - if not provided or null, the configured default org/space will be used automatically.";
    private static final String DOMAIN_PARAM = "The domain name for the route (e.g., apps.example.com)";
    private static final String HOST_PARAM = "The hostname for the route (optional)";
    private static final String PATH_ROUTE_PARAM = "The path for the route (optional)";
    private static final String PORT_PARAM = "The port for the route (optional)";

    @Tool(description = CREATE_ROUTE)
    public void createRoute(@ToolParam(description = DOMAIN_PARAM) String domain,
                           @ToolParam(description = HOST_PARAM, required = false) String host,
                           @ToolParam(description = PATH_ROUTE_PARAM, required = false) String path,
                           @ToolParam(description = PORT_PARAM, required = false) Integer port,
                           @ToolParam(description = ORG_PARAM, required = false) String organization,
                           @ToolParam(description = SPACE_PARAM, required = false) String space) {
        CloudFoundryOperations operations = getOperations(organization, space);
        
        // The CreateRouteRequest API requires an explicit space parameter, unlike other operations
        // We need to resolve the target space from the operations context
        String targetSpace = space != null ? space : operationsFactory.getDefaultSpace();
        
        CreateRouteRequest.Builder builder = CreateRouteRequest.builder()
                .domain(domain)
                .space(targetSpace);
        if (host != null) builder.host(host);
        if (path != null) builder.path(path);
        if (port != null) builder.port(port);
        
        CreateRouteRequest request = builder.build();
        operations.routes().create(request).block();
    }

    private static final String DELETE_ROUTE = "Delete a route in a Cloud Foundry space. Organization and space parameters are optional - if not provided or null, the configured default org/space will be used automatically.";

    @Tool(description = DELETE_ROUTE)
    public void deleteRoute(@ToolParam(description = DOMAIN_PARAM) String domain,
                           @ToolParam(description = HOST_PARAM, required = false) String host,
                           @ToolParam(description = PATH_ROUTE_PARAM, required = false) String path,
                           @ToolParam(description = PORT_PARAM, required = false) Integer port,
                           @ToolParam(description = ORG_PARAM, required = false) String organization,
                           @ToolParam(description = SPACE_PARAM, required = false) String space) {
        DeleteRouteRequest.Builder builder = DeleteRouteRequest.builder().domain(domain);
        if (host != null) builder.host(host);
        if (path != null) builder.path(path);
        if (port != null) builder.port(port);
        
        DeleteRouteRequest request = builder.build();
        getOperations(organization, space).routes().delete(request).block();
    }

    private static final String DELETE_ORPHANED_ROUTES = "Delete all orphaned routes (routes not bound to any applications) in a Cloud Foundry space. Organization and space parameters are optional - if not provided or null, the configured default org/space will be used automatically.";

    @Tool(description = DELETE_ORPHANED_ROUTES)
    public void deleteOrphanedRoutes(@ToolParam(description = ORG_PARAM, required = false) String organization,
                                    @ToolParam(description = SPACE_PARAM, required = false) String space) {
        DeleteOrphanedRoutesRequest request = DeleteOrphanedRoutesRequest.builder().build();
        getOperations(organization, space).routes().deleteOrphanedRoutes(request).block();
    }

    private static final String MAP_ROUTE = "Map a route to a Cloud Foundry application. Organization and space parameters are optional - if not provided or null, the configured default org/space will be used automatically.";

    @Tool(description = MAP_ROUTE)
    public void mapRoute(@ToolParam(description = NAME_PARAM) String applicationName,
                        @ToolParam(description = DOMAIN_PARAM) String domain,
                        @ToolParam(description = HOST_PARAM, required = false) String host,
                        @ToolParam(description = PATH_ROUTE_PARAM, required = false) String path,
                        @ToolParam(description = PORT_PARAM, required = false) Integer port,
                        @ToolParam(description = ORG_PARAM, required = false) String organization,
                        @ToolParam(description = SPACE_PARAM, required = false) String space) {
        MapRouteRequest.Builder builder = MapRouteRequest.builder()
                .applicationName(applicationName)
                .domain(domain);
        if (host != null) builder.host(host);
        if (path != null) builder.path(path);
        if (port != null) builder.port(port);
        
        MapRouteRequest request = builder.build();
        getOperations(organization, space).routes().map(request).block();
    }

    private static final String UNMAP_ROUTE = "Unmap a route from a Cloud Foundry application. Organization and space parameters are optional - if not provided or null, the configured default org/space will be used automatically.";

    @Tool(description = UNMAP_ROUTE)
    public void unmapRoute(@ToolParam(description = NAME_PARAM) String applicationName,
                          @ToolParam(description = DOMAIN_PARAM) String domain,
                          @ToolParam(description = HOST_PARAM, required = false) String host,
                          @ToolParam(description = PATH_ROUTE_PARAM, required = false) String path,
                          @ToolParam(description = PORT_PARAM, required = false) Integer port,
                          @ToolParam(description = ORG_PARAM, required = false) String organization,
                          @ToolParam(description = SPACE_PARAM, required = false) String space) {
        UnmapRouteRequest.Builder builder = UnmapRouteRequest.builder()
                .applicationName(applicationName)
                .domain(domain);
        if (host != null) builder.host(host);
        if (path != null) builder.path(path);
        if (port != null) builder.port(port);
        
        UnmapRouteRequest request = builder.build();
        getOperations(organization, space).routes().unmap(request).block();
    }

    /*
        Network Policies
     */
    private static final String ADD_NETWORK_POLICY = "Add a network policy to allow communication between applications. Organization and space parameters are optional - if not provided or null, the configured default org/space will be used automatically.";
    private static final String SOURCE_APP_PARAM = "Name of the source application that will initiate the network connection";
    private static final String DEST_APP_PARAM = "Name of the destination application that will receive the network connection";
    private static final String PROTOCOL_PARAM = "Network protocol for the policy (tcp or udp)";
    private static final String PORTS_PARAM = "Port or port range for the policy (e.g., '8080' or '8080-8090')";

    @Tool(description = ADD_NETWORK_POLICY)
    public void addNetworkPolicy(@ToolParam(description = SOURCE_APP_PARAM) String sourceApp,
                                @ToolParam(description = DEST_APP_PARAM) String destinationApp,
                                @ToolParam(description = PROTOCOL_PARAM) String protocol,
                                @ToolParam(description = PORTS_PARAM) String ports,
                                @ToolParam(description = ORG_PARAM, required = false) String organization,
                                @ToolParam(description = SPACE_PARAM, required = false) String space) {
        // Parse port range
        String[] portParts = ports.split("-");
        Integer startPort = Integer.parseInt(portParts[0]);
        Integer endPort = portParts.length > 1 ? Integer.parseInt(portParts[1]) : null;
        
        AddNetworkPolicyRequest request = AddNetworkPolicyRequest.builder()
                .source(sourceApp)
                .destination(destinationApp)
                .protocol(protocol)
                .startPort(startPort)
                .endPort(endPort)
                .build();
        getOperations(organization, space).networkPolicies().add(request).blockLast();
    }

    private static final String LIST_NETWORK_POLICIES = "List network policies in a Cloud Foundry space. Organization and space parameters are optional - if not provided or null, the configured default org/space will be used automatically.";

    @Tool(description = LIST_NETWORK_POLICIES)
    public List<Policy> listNetworkPolicies(@ToolParam(description = ORG_PARAM, required = false) String organization,
                                           @ToolParam(description = SPACE_PARAM, required = false) String space) {
        ListNetworkPoliciesRequest request = ListNetworkPoliciesRequest.builder().build();
        return getOperations(organization, space).networkPolicies().list(request).collectList().block();
    }

    private static final String REMOVE_NETWORK_POLICY = "Remove a network policy between applications. Organization and space parameters are optional - if not provided or null, the configured default org/space will be used automatically.";

    @Tool(description = REMOVE_NETWORK_POLICY)
    public void removeNetworkPolicy(@ToolParam(description = SOURCE_APP_PARAM) String sourceApp,
                                   @ToolParam(description = DEST_APP_PARAM) String destinationApp,
                                   @ToolParam(description = PROTOCOL_PARAM) String protocol,
                                   @ToolParam(description = PORTS_PARAM) String ports,
                                   @ToolParam(description = ORG_PARAM, required = false) String organization,
                                   @ToolParam(description = SPACE_PARAM, required = false) String space) {
        // Parse port range
        String[] portParts = ports.split("-");
        Integer startPort = Integer.parseInt(portParts[0]);
        Integer endPort = portParts.length > 1 ? Integer.parseInt(portParts[1]) : null;
        
        RemoveNetworkPolicyRequest request = RemoveNetworkPolicyRequest.builder()
                .source(sourceApp)
                .destination(destinationApp)
                .protocol(protocol)
                .startPort(startPort)
                .endPort(endPort)
                .build();
        getOperations(organization, space).networkPolicies().remove(request).blockLast();
    }

    /**
     * Helper method for context resolution.
     * Resolves the CloudFoundryOperations instance based on the provided organization and space parameters.
     * 
     * @param organization the organization name (optional)
     * @param space the space name (optional)
     * @return CloudFoundryOperations instance for the specified or default context
     */
    private CloudFoundryOperations getOperations(String organization, String space) {
        if (organization == null && space == null) {
            return operationsFactory.getDefaultOperations();
        }
        return operationsFactory.getOperations(organization, space);
    }
}
