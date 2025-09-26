# Cloud Foundry MCP Server

A comprehensive Model Context Protocol (MCP) server that provides AI-powered access to Cloud Foundry operations through 32 specialized tools. Built with [Spring AI 1.1.0-M2](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-server-boot-starter-docs.html) and deployed on Cloud Foundry.

![Cloud Foundry MCP Server](https://img.shields.io/badge/Cloud%20Foundry-MCP%20Server-blue?style=for-the-badge&logo=cloudfoundry)
![Spring AI](https://img.shields.io/badge/Spring%20AI-1.1.0--M2-green?style=for-the-badge&logo=spring)
![Java](https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.2-brightgreen?style=for-the-badge&logo=springboot)
![MCP](https://img.shields.io/badge/MCP-Protocol-purple?style=for-the-badge)
![Streamable](https://img.shields.io/badge/Transport-HTTP%20Streamable-blue?style=for-the-badge)

![Version](https://img.shields.io/badge/Version-0.1.0-red?style=for-the-badge)
![License](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)
![Maven](https://img.shields.io/badge/Maven-3.9+-red?style=for-the-badge&logo=apache-maven)
![Cloud Foundry](https://img.shields.io/badge/Deployed%20on-Cloud%20Foundry-0066CC?style=for-the-badge&logo=cloudfoundry)

<div align="center">
  <img src="images/cf-mcp-architecture.svg" alt="Cloud Foundry MCP Server Architecture" width="600"/>
  <p><em>Cloud Foundry MCP Server - AI-powered Cloud Foundry operations via Model Context Protocol</em></p>
</div>

## 🚀 Quick Start

### Deploy the Server
1. **Build and deploy** the MCP server to your Cloud Foundry foundation
2. **Configure** your Cloud Foundry credentials in the manifest
3. **Get the deployed URL** from `cf apps` command

### MCP Client Configuration
Once deployed, configure your MCP client with the server URL:

```json
{
  "mcpServers": {
    "cloud-foundry": {
      "disabled": false,
      "timeout": 60,
      "type": "streamable",
      "url": "https://your-mcp-server.apps.your-cf-domain.com/mcp",
      "autoApprove": []
    }
  }
}
```

**Replace** `your-mcp-server.apps.your-cf-domain.com` with your actual deployed server URL.

## 🛠 Building & Deployment

### Build the Server
```bash
./mvnw clean package -DskipTests
```

### Deploy to Cloud Foundry
```bash
# Copy the template and configure with your credentials
cp manifest-template.yml manifest.yml
# Edit manifest.yml with your CF credentials
cf push cloud-foundry-mcp-server

# Get the deployed URL
cf apps
# Look for your app and copy the URL (e.g., https://cloud-foundry-mcp-server.apps.your-domain.com)
```

### Local Development
```bash
# Run with local profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

## ⚙️ Configuration

### Environment Variables
```bash
CF_APIHOST=api.your-cf-domain.com
CF_USERNAME=your-username
CF_PASSWORD=your-password
CF_ORG=your-organization
CF_SPACE=your-space
```

### Configuration Validation
The server includes automatic configuration validation on startup:
- **Required**: API host, username, and password must be configured
- **Optional**: Organization and space (warnings if not set)
- **Startup Check**: Validates CF connectivity and credentials
- **Error Handling**: Fails fast with clear error messages if configuration is invalid

### Application Properties
```properties
spring.ai.mcp.server.name=cloud-foundry-mcp
spring.ai.mcp.server.version=0.1.0
spring.ai.mcp.server.prompt-change-notification=false
spring.ai.mcp.server.resource-change-notification=false
spring.ai.mcp.server.protocol=streamable

management.endpoints.web.exposure.include=health,info,mappings
management.endpoint.health.show-details=always

logging.level.io.modelcontextprotocol=DEBUG
logging.level.org.springframework.ai.mcp=DEBUG
```

## 🛠 Capabilities

This MCP server exposes **38 Cloud Foundry operations** as AI-powered tools:

### Application Management (8 tools)
- **applicationsList** - List applications in a CF space
- **applicationDetails** - Get detailed app information
- **pushApplication** - Deploy JAR files to CF
- **scaleApplication** - Scale app instances, memory, disk
- **startApplication** - Start CF applications
- **stopApplication** - Stop running applications
- **restartApplication** - Restart applications
- **deleteApplication** - Delete applications

### Organization & Space Management (8 tools)
- **organizationsList** - List all organizations
- **organizationDetails** - Get org details
- **spacesList** - List spaces in an org
- **getSpaceQuota** - Get space quota details
- **createSpace** - Create new spaces
- **deleteSpace** - Delete spaces
- **renameSpace** - Rename spaces
- **deleteOrphanedRoutes** - Clean up orphaned routes

### Service Management (7 tools)
- **serviceInstancesList** - List service instances
- **serviceInstanceDetails** - Get service instance details
- **serviceOfferingsList** - List marketplace services
- **createServiceInstance** - Create new service instances
- **bindServiceInstance** - Bind services to apps
- **unbindServiceInstance** - Unbind services from apps
- **deleteServiceInstance** - Delete service instances

### Route Management (6 tools)
- **routesList** - List routes in a space
- **createRoute** - Create new routes
- **deleteRoute** - Delete routes
- **mapRoute** - Map routes to applications
- **unmapRoute** - Unmap routes from applications
- **deleteOrphanedRoutes** - Clean up orphaned routes

### Network Policy Management (3 tools)
- **addNetworkPolicy** - Add network policies between apps
- **listNetworkPolicies** - List network policies
- **removeNetworkPolicy** - Remove network policies

### Application Cloning (1 tool)
- **cloneApp** - Clone existing applications with buildpack consistency

### Target Management (3 tools)
- **targetCf** - Set the target organization and space for CF operations
- **getCurrentTarget** - Get the current target organization and space
- **clearTarget** - Clear the current target, reverting to configuration defaults

### Configuration & Validation (1 tool)
- **CfConfigurationValidator** - Validates Cloud Foundry configuration on startup

## 🔧 Technical Details

- **Spring AI Version**: 1.1.0-M2
- **Spring Boot Version**: 3.4.2
- **Java Version**: 21
- **Transport**: HTTP Streamable
- **Health Endpoint**: `/actuator/health`
- **Configuration**: Environment variable-based CF credentials

## 📊 Health Status

The server provides comprehensive health monitoring:
- **Application Health**: Memory, disk, CPU usage
- **SSL/TLS Status**: Certificate validation
- **Cloud Foundry Connectivity**: API endpoint health
- **MCP Server Status**: Tool registration and transport health

## 🚀 Current Deployment

**Status**: ✅ Successfully Deployed and Running

**URL**: `https://cloud-foundry-mcp-courteous-lemur-hd.apps.tp.penso.io`

**Organization**: `tanzu-platform-demo`  
**Space**: `mcp-server`  
**Application**: `cloud-foundry-mcp`

### Recent Updates (v0.1.0)
- ✅ **Added Target Management Tools** - New `targetCf`, `getCurrentTarget`, and `clearTarget` operations
- ✅ **Configuration Validation** - Added startup validation for CF credentials and settings
- ✅ **Enhanced Error Handling** - Better validation and parameter processing
- ✅ **Comprehensive Testing** - Added unit tests for all new services
- ✅ **Fixed critical service instance creation** - Added missing `createServiceInstance` method
- ✅ **Improved application push handling** - Fixed hardcoded parameter issues
- ✅ **Verified all 38 tools** - Comprehensive testing of all MCP operations
- ✅ **Successful deployment** - Application running and healthy on Cloud Foundry

## 🔒 Security

- **Credential Management**: Environment variable-based configuration
- **SSL/TLS**: HTTPS endpoints for secure communication
- **Authentication**: Cloud Foundry UAA integration
- **Authorization**: CF role-based access control

### 🔐 Credential Security

**Important**: The `manifest.yml` file contains sensitive credentials and is excluded from git via `.gitignore`.

- **Template**: Use `manifest-template.yml` as a starting point
- **Local Configuration**: Copy template and add your credentials
- **Environment Variables**: Credentials are passed via CF environment variables
- **Never Commit**: Actual manifest files with credentials should never be committed to git

## 📚 Documentation

- **Release Notes**: [RELEASE_NOTES.md](RELEASE_NOTES.md)
- **API Documentation**: Comprehensive tool descriptions
- **Configuration Guide**: Setup and deployment instructions
- **Troubleshooting**: Common issues and solutions

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## 📄 License

This project is licensed under the MIT License.

---

**Built with ❤️ using Spring AI and Cloud Foundry**
