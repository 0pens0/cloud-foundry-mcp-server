package org.tanzu.cfpulse.cf;

import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.routes.*;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CfRouteService extends CfBaseService {

    private static final String ROUTE_LIST = "Return the routes in a Cloud Foundry space. Organization and space parameters are optional - if not provided or null, the configured default org/space will be used automatically.";
    private static final String CREATE_ROUTE = "Create a new route in a Cloud Foundry space. Organization and space parameters are optional - if not provided or null, the configured default org/space will be used automatically.";
    private static final String DELETE_ROUTE = "Delete a route in a Cloud Foundry space. Organization and space parameters are optional - if not provided or null, the configured default org/space will be used automatically.";
    private static final String DELETE_ORPHANED_ROUTES = "Delete all orphaned routes (routes not bound to any applications) in a Cloud Foundry space. Organization and space parameters are optional - if not provided or null, the configured default org/space will be used automatically.";
    private static final String MAP_ROUTE = "Map a route to a Cloud Foundry application. Organization and space parameters are optional - if not provided or null, the configured default org/space will be used automatically.";
    private static final String UNMAP_ROUTE = "Unmap a route from a Cloud Foundry application. Organization and space parameters are optional - if not provided or null, the configured default org/space will be used automatically.";
    
    private static final String DOMAIN_PARAM = "The domain name for the route (e.g., apps.example.com)";
    private static final String HOST_PARAM = "The hostname for the route (optional)";
    private static final String PATH_ROUTE_PARAM = "The path for the route (optional)";
    private static final String PORT_PARAM = "The port for the route (optional)";

    public CfRouteService(CloudFoundryOperationsFactory operationsFactory) {
        super(operationsFactory, 3, 2);
    }

    @Tool(description = ROUTE_LIST)
    public List<Route> routesList(
            @ToolParam(description = ORG_PARAM, required = false) String organization,
            @ToolParam(description = SPACE_PARAM, required = false) String space) {
        ListRoutesRequest request = ListRoutesRequest.builder().build();
        return getOperations(organization, space).routes().list(request).collectList().block();
    }

    @Tool(description = CREATE_ROUTE)
    public void createRoute(@ToolParam(description = DOMAIN_PARAM) String domain,
                           @ToolParam(description = HOST_PARAM, required = false) String host,
                           @ToolParam(description = PATH_ROUTE_PARAM, required = false) String path,
                           @ToolParam(description = PORT_PARAM, required = false) Integer port,
                           @ToolParam(description = ORG_PARAM, required = false) String organization,
                           @ToolParam(description = SPACE_PARAM, required = false) String space) {
        CloudFoundryOperations operations = getOperations(organization, space);
        
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

    @Tool(description = DELETE_ORPHANED_ROUTES)
    public void deleteOrphanedRoutes(@ToolParam(description = ORG_PARAM, required = false) String organization,
                                    @ToolParam(description = SPACE_PARAM, required = false) String space) {
        DeleteOrphanedRoutesRequest request = DeleteOrphanedRoutesRequest.builder().build();
        getOperations(organization, space).routes().deleteOrphanedRoutes(request).block();
    }

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
}