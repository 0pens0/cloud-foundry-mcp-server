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
- `McpServerConfig.java`: Registers the CF service tools with the MCP server using Spring AI's ToolCallbacks
- `CfService.java`: Core service containing all CF operations exposed as @Tool annotated methods
- `CfConfiguration.java`: Spring configuration for CF client beans (CloudFoundryClient, UaaClient, etc.)
- `CloudFoundryOperationsFactory.java`: Factory for creating CloudFoundryOperations with caching and context switching

### Architecture Details

- **Context Switching**: The factory supports switching between different org/space contexts while maintaining a cache of CloudFoundryOperations instances
- **Parameter Resolution**: All tools accept optional organization and space parameters that fall back to configured defaults
- **Reactive Patterns**: Uses reactive CF client with `.block()` calls to provide synchronous tool interfaces
- **Tool Registration**: Spring AI's MCP server automatically discovers and exposes all @Tool annotated methods

### Tool Categories

The `CfService` exposes tools organized by CF resource type:

- **Applications**: List, get details, push, scale, start/stop/restart, delete
- **Organizations**: List orgs
- **Services**: List instances/offerings, bind/unbind, delete instances  
- **Spaces**: List spaces, get quotas, create, delete, rename
- **Routes**: List, create, delete, map/unmap to applications, delete orphaned routes
- **Network Policies**: Add, list, remove policies between applications

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