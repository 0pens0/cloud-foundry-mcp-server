# Cloud Foundry MCP Server

This MCP Server provides an LLM interface for interacting with your Cloud Foundry foundation. It was built with the [Spring AI MCP Server Boot Starter](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-server-boot-starter-docs.html).

![Sample](images/sample.png)

## Building the Server

```bash
./mvnw clean package
```

### Using with MCP Client

By default, this MCP server runs with the SSE transport. Configure your MCP client like this:

```json
{
  "mcpServers": {
    "cloud-foundry": {
      "url": "https://[URL for deployed CF MCP server]/sse",
      "env": {
        "CF_APIHOST": "[Your CF API Endpoint e.g. api.sys.mycf.com]",
        "CF_USERNAME": "[Your CF User]",
        "CF_PASSWORD": "[Your CF Password]",
        "CF_ORG": "[Your Org]",
        "CF_SPACE": "[Your Space]"
      }
    }
  }
}
```

## Capabilities

This MCP server exposes the following Cloud Foundry operations as tools:

### Application Management (8 tools)
- **applicationsList** - List all applications in a space
- **applicationDetails** - Get detailed information about a specific application
- **cloneApplication** - Clone an existing application
- **scaleApplication** - Scale application instances, memory, or disk quota
- **startApplication** - Start a stopped application
- **stopApplication** - Stop a running application
- **restartApplication** - Restart an application
- **deleteApplication** - Delete an application

### Organization & Space Management (7 tools)
- **organizationsList** - List all organizations
- **organizationDetails** - Get details about a specific organization
- **spacesList** - List all spaces in an organization
- **getSpaceQuota** - Get quota information for a space
- **createSpace** - Create a new space
- **deleteSpace** - Delete a space
- **renameSpace** - Rename an existing space

### Service Management (6 tools)
- **serviceInstancesList** - List all service instances in a space
- **serviceInstanceDetails** - Get details about a specific service instance
- **serviceOfferingsList** - List available service offerings
- **bindServiceInstance** - Bind a service instance to an application
- **unbindServiceInstance** - Unbind a service instance from an application
- **deleteServiceInstance** - Delete a service instance

### Route Management (6 tools)
- **routesList** - List all routes in a space
- **createRoute** - Create a new route
- **deleteRoute** - Delete a specific route
- **deleteOrphanedRoutes** - Delete all unmapped routes
- **mapRoute** - Map a route to an application
- **unmapRoute** - Unmap a route from an application

### Network Policy Management (3 tools)
- **addNetworkPolicy** - Create network policy between applications
- **listNetworkPolicies** - List all network policies
- **removeNetworkPolicy** - Remove network policy between applications

### Application Cloning (1 tool)

All tools support multi-context operations with optional `organization` and `space` parameters to target different environments.
