# Release Notes

## Version 0.1.0 - Cloud Foundry MCP Server

### 🚀 Major Features

**Cloud Foundry MCP Server** - A comprehensive Model Context Protocol (MCP) server that provides AI-powered access to Cloud Foundry operations through 31 specialized tools.

### ✨ Key Capabilities

#### Application Management (8 tools)
- **applicationsList** - List applications in a CF space
- **applicationDetails** - Get detailed app information  
- **pushApplication** - Deploy JAR files to CF
- **scaleApplication** - Scale app instances, memory, disk
- **startApplication** - Start CF applications
- **stopApplication** - Stop running applications
- **restartApplication** - Restart applications
- **deleteApplication** - Delete applications

#### Organization & Space Management (8 tools)
- **organizationsList** - List all organizations
- **organizationDetails** - Get org details
- **spacesList** - List spaces in an org
- **getSpaceQuota** - Get space quota details
- **createSpace** - Create new spaces
- **deleteSpace** - Delete spaces
- **renameSpace** - Rename spaces
- **deleteOrphanedRoutes** - Clean up orphaned routes

#### Service Management (6 tools)
- **serviceInstancesList** - List service instances
- **serviceInstanceDetails** - Get service instance details
- **serviceOfferingsList** - List marketplace services
- **bindServiceInstance** - Bind services to apps
- **unbindServiceInstance** - Unbind services from apps
- **deleteServiceInstance** - Delete service instances

#### Route Management (6 tools)
- **routesList** - List routes in a space
- **createRoute** - Create new routes
- **deleteRoute** - Delete routes
- **mapRoute** - Map routes to applications
- **unmapRoute** - Unmap routes from applications
- **deleteOrphanedRoutes** - Clean up orphaned routes

#### Network Policy Management (3 tools)
- **addNetworkPolicy** - Add network policies between apps
- **listNetworkPolicies** - List network policies
- **removeNetworkPolicy** - Remove network policies

#### Application Cloning (1 tool)
- **cloneApp** - Clone existing applications with buildpack consistency

### 🛠 Technical Implementation

- **Spring AI 1.1.0-M2** - Latest MCP server framework
- **Spring Boot 3.4.2** - Modern Java framework
- **Java 21** - Latest LTS Java version
- **HTTP Streamable Transport** - Modern HTTP-based communication protocol
- **Cloud Foundry Java Client** - Official CF operations library

### 🔧 Configuration

#### Environment Variables
```bash
CF_APIHOST=api.sys.tp.penso.io
CF_USERNAME=admin
CF_PASSWORD=your-password
CF_ORG=tanzu-platform-demo
CF_SPACE=mcp-server
```

#### MCP Client Configuration
```json
{
  "mcpServers": {
    "cloud-foundry": {
      "disabled": false,
      "timeout": 60,
      "type": "streamable",
      "url": "https://cloud-foundry-mcp-server.apps.tp.penso.io/mcp",
      "autoApprove": []
    }
  }
}
```

### 📦 Deployment

#### Cloud Foundry Deployment
```bash
# Build the application
./mvnw clean package -DskipTests

# Deploy to Cloud Foundry
cf push cloud-foundry-mcp-server
```

#### Local Development
```bash
# Run with local profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

### 🏗 Architecture

- **MCP Server**: Spring AI-based MCP server with HTTP Streamable transport
- **Service Layer**: Modular services for different CF operations
- **Configuration**: Environment-based CF credentials
- **Health Checks**: Spring Boot Actuator health endpoints
- **Logging**: Comprehensive debug logging for troubleshooting

### 🔒 Security

- **Credential Management**: Environment variable-based configuration
- **SSL/TLS**: HTTPS endpoints for secure communication
- **Authentication**: Cloud Foundry UAA integration
- **Authorization**: CF role-based access control

### 📊 Monitoring

- **Health Endpoint**: `/actuator/health`
- **Application Metrics**: Memory, disk, CPU monitoring
- **Log Aggregation**: Structured logging for observability
- **Error Handling**: Global exception handling

### 🚀 Performance

- **Connection Pooling**: Efficient CF client connections
- **Async Operations**: Non-blocking CF operations
- **Memory Optimization**: Efficient resource usage
- **Response Caching**: Optimized API responses

### 🧪 Testing

- **Unit Tests**: Service layer testing
- **Integration Tests**: CF operations testing
- **Health Checks**: Automated health verification
- **Load Testing**: Performance validation

### 📚 Documentation

- **API Documentation**: Comprehensive tool descriptions
- **Configuration Guide**: Setup and deployment instructions
- **Troubleshooting**: Common issues and solutions
- **Examples**: Usage patterns and best practices

### 🔄 Version History

#### v0.1.0 (2025-09-25)
- Initial release
- 31 Cloud Foundry tools
- Spring AI 1.1.0-M2 integration
- HTTP Streamable transport support
- Cloud Foundry deployment ready
- Comprehensive documentation

### 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

### 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

### 🆘 Support

- **Issues**: GitHub Issues
- **Documentation**: README.md
- **Examples**: /examples directory
- **Community**: GitHub Discussions

---

**Built with ❤️ using Spring AI and Cloud Foundry**
