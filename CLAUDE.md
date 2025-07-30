# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Spring AI MCP (Model Context Protocol) server that enables LLMs to interact with Cloud Foundry foundations. The server exposes Cloud Foundry operations as tools that can be called by LLM clients.

## Architecture

- **Spring Boot Application**: Standard Spring Boot app with Spring AI MCP dependencies
- **MCP Server**: Uses Spring AI's MCP server capabilities to expose CF operations as tools
- **Cloud Foundry Integration**: Uses the official Cloud Foundry Java client libraries
- **Tool-based API**: Each CF operation is exposed as an annotated `@Tool` method

### Key Components

- `CfPulseMcpApplication.java`: Standard Spring Boot main class
- `McpServerConfig.java`: Registers all CF service tools with the MCP server using Spring AI's ToolCallbacks
- `CfConfiguration.java`: Spring configuration for CF client beans (CloudFoundryClient, UaaClient, etc.)
- `CloudFoundryOperationsFactory.java`: Factory for creating CloudFoundryOperations with caching and context switching

### Service Architecture

The CF operations are organized into focused service classes that extend `CfBaseService`:

- `CfBaseService.java`: Abstract base class providing common functionality and shared constants
- `CfApplicationService.java`: Application lifecycle management (8 tools)
- `CfOrganizationService.java`: Organization operations (2 tools) 
- `CfServiceInstanceService.java`: Service instance management (6 tools)
- `CfSpaceService.java`: Space administration (5 tools)
- `CfRouteService.java`: Route management (6 tools)
- `CfNetworkPolicyService.java`: Network policy configuration (3 tools)
- `CfApplicationCloner.java`: Advanced application cloning with buildpack-specific placeholders (1 tool)

### Architecture Details

- **Modular Design**: Each service class handles one CF resource type for better maintainability
- **Shared Base Class**: `CfBaseService` provides common `getOperations()` method and parameter constants
- **Multi-Context Support**: All tools accept optional organization and space parameters for dynamic targeting across environments
- **Intelligent Caching**: `CloudFoundryOperationsFactory` maintains a thread-safe cache (ConcurrentHashMap) of CloudFoundryOperations instances keyed by "org:space"
- **Context Resolution**: Automatic fallback to configured defaults when org/space parameters are null or omitted
- **Reactive Patterns**: Uses reactive CF client with `.block()` calls to provide synchronous tool interfaces
- **Automatic Registration**: Spring AI's MCP server automatically discovers and exposes all @Tool annotated methods from all service classes
- **Advanced Cloning**: `CfApplicationCloner` provides application cloning with buildpack-specific placeholders and environment variable preservation

### Tool Categories by Service

- **CfApplicationService**: List apps, get details, push, scale, start, stop, restart, delete (8 tools)
- **CfOrganizationService**: List organizations, get organization details (2 tools)
- **CfServiceInstanceService**: List instances/offerings, get details, bind, unbind, delete instances (6 tools)
- **CfSpaceService**: List spaces, get quotas, create, delete, rename spaces (5 tools)
- **CfRouteService**: List, create, delete routes, delete orphaned routes, map/unmap to applications (6 tools)
- **CfNetworkPolicyService**: Add, list, remove network policies between applications (3 tools)
- **CfApplicationCloner**: Clone applications with buildpack-specific placeholders preserving configuration and environment variables (1 tool)

### Multi-Context Operation Pattern

All tools support optional `organization` and `space` parameters for cross-environment operations:

```java
// Use default org/space (from configuration)
applicationsList()

// Target specific space in default org
applicationsList(null, "production")

// Target specific org/space combination  
applicationsList("my-org", "staging")
```

**Context Resolution Rules:**
- Both null: Use configured default org/space
- Organization null, Space provided: Use default org with specified space  
- Organization provided, Space null: Use specified org with default space
- Both provided: Use specified org/space combination

**Caching:** The factory maintains a thread-safe cache of CloudFoundryOperations instances per org/space combination for optimal performance.

## Development Commands

### Build
```bash
./mvnw clean package
```

### Run Locally
```bash
java -Dspring.ai.mcp.server.transport=stdio -jar target/cloud-foundry-mcp-0.0.1-SNAPSHOT.jar --server.port=8040
```

### Test
```bash
./mvnw test
```

### Run Single Test
```bash
./mvnw test -Dtest=ClassName#methodName
```

### Run with Specific Profile
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

## Configuration

The application requires CF connection details via environment variables:
- `CF_APIHOST`: CF API endpoint (e.g., api.sys.mycf.com)
- `CF_USERNAME`: CF username
- `CF_PASSWORD`: CF password  
- `CF_ORG`: CF organization name
- `CF_SPACE`: CF space name

Configuration is handled in `application.yaml` with fallbacks to environment variables.

## MCP Client Integration

This server is designed to be used with MCP clients like Claude Desktop. Example claude_desktop_config.json configuration:

```json
{
  "mcpServers": {
    "cloud-foundry": {
      "command": "java",
      "args": [
        "-Dspring.ai.mcp.server.transport=stdio", 
        "-Dlogging.file.name=cloud-foundry-mcp.log", 
        "-jar",
        "/path/to/cloud-foundry-mcp/target/cloud-foundry-mcp-0.0.1-SNAPSHOT.jar",
        "--server.port=8040"
      ],
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

## Dependencies

- Spring Boot 3.4.2
- Spring AI 1.0.0 (MCP server support)
- Cloud Foundry Java Client 5.12.2.RELEASE
- Java 21 runtime requirement

## Development Guidelines

### Adding New Tools

1. **Choose the appropriate service class** based on the CF resource type (apps, orgs, services, spaces, routes, network policies)
2. **Add the @Tool method** to the relevant service class following existing patterns
3. **Use shared constants** from `CfBaseService` for common parameters (ORG_PARAM, SPACE_PARAM, NAME_PARAM)
4. **Follow naming conventions**: method names should be descriptive (e.g., `applicationsList`, `organizationDetails`)
5. **Include parameter descriptions** using @ToolParam annotations
6. **Use getOperations(org, space)** for context resolution - this handles default org/space fallback

### Creating New Service Classes

If adding a new CF resource type:

1. **Extend CfBaseService** to inherit common functionality
2. **Add @Service annotation** for Spring component scanning
3. **Inject CloudFoundryOperationsFactory** via constructor
4. **Register in McpServerConfig** by adding the service to the `registerTools` method parameter list
5. **Follow the pattern** of existing service classes for consistency

### Tool Registration

All @Tool annotated methods are automatically discovered and registered through `McpServerConfig.registerTools()`. The method uses `ToolCallbacks.from()` to extract all tools from each service class and combines them into a single list for the MCP server.

### Special Considerations

**CfApplicationCloner**: This advanced service provides application cloning using buildpack-specific placeholders:
- Creates buildpack-specific placeholders (Java, Node.js, Python, Go, PHP, Ruby, Static)
- Preserves source application configuration (memory, disk, instances, environment variables)
- Guarantees buildpack consistency through pre-deployment buildpack matching
- Includes comprehensive error handling and logging for troubleshooting
- Supports environment variable cloning using CF's `getEnvironments()` and `setEnvironmentVariable()` APIs

**Environment Variable Handling**: When working with environment variables, the system:
- Retrieves user-provided environment variables from source applications
- Converts CF's `Map<String, Object>` format to `Map<String, String>` for consistency
- Sets environment variables sequentially during deployment to ensure proper configuration
- Gracefully handles errors by returning empty maps when environment variables cannot be retrieved