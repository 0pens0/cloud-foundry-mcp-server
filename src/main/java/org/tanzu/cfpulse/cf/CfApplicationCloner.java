package org.tanzu.cfpulse.cf;

import org.cloudfoundry.operations.applications.*;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Application cloning service that creates buildpack-specific placeholders to ensure consistent deployments.
 * 
 * This approach ensures buildpack consistency by:
 * 1. Detecting source app's buildpack and configuration
 * 2. Creating buildpack-specific placeholder content
 * 3. Deploying placeholder with source app's exact buildpack
 * 4. Copying source over placeholder to preserve buildpack
 * 5. Scaling and starting with verified buildpack matching
 */
@Service
public class CfApplicationCloner extends CfBaseService {

    public CfApplicationCloner(CloudFoundryOperationsFactory operationsFactory) {
        super(operationsFactory);
    }

    /**
     * Clone an existing Cloud Foundry application by creating a buildpack-specific placeholder
     */
    @Tool(description = "Clone an existing Cloud Foundry application to create a copy with a new name. Uses buildpack-specific placeholders to ensure consistent deployments.")
    public void cloneApp(
            @ToolParam(description = "Source application name") String sourceApp,
            @ToolParam(description = "Target application name") String targetApp,
            @ToolParam(description = "Organization name (optional)", required = false) String organization,
            @ToolParam(description = "Space name (optional)", required = false) String space) {
        
        System.out.println("Starting clone operation: " + sourceApp + " -> " + targetApp);
        
        try {
            // First get both source app config AND buildpack info
            Mono.zip(
                getSourceAppConfig(sourceApp, organization, space),
                getBuildpackInfo(sourceApp, organization, space)
            )
                    .flatMap(tuple -> {
                        AppConfig config = tuple.getT1();
                        String sourceBuildpack = tuple.getT2();
                        
                        System.out.println("Retrieved source app info: memory=" + config.memoryLimit() + 
                                         ", disk=" + config.diskQuota() + ", instances=" + config.instances() +
                                         ", buildpack=" + sourceBuildpack + 
                                         ", env vars=" + config.environmentVariables().size());
                        
                        return Mono.fromCallable(() -> createBuildpackPlaceholder(targetApp, sourceBuildpack))
                                .flatMap(placeholderPath -> {
                                    System.out.println("Created buildpack placeholder for: " + sourceBuildpack);
                                    
                                    return deployPlaceholderWithSourceBuildpack(targetApp, placeholderPath, sourceBuildpack, organization, space, config)
                                            .doOnSuccess(v -> System.out.println("Placeholder deployed with matching buildpack: " + sourceBuildpack))
                                            .then(
                                                    // Step 2: Copy application source (buildpack already matches)
                                                    copySourceWithBuildpackVerification(sourceApp, targetApp, sourceBuildpack, organization, space, config)
                                                            .doOnSuccess(v -> System.out.println("Source copy completed with buildpack verification"))
                                            )
                                            .doFinally(signal -> {
                                                System.out.println("Cleaning up temporary files...");
                                                cleanup(placeholderPath);
                                            });
                                });
                    })
                    .timeout(Duration.ofMinutes(10)) // 10 minute timeout for the entire operation
                    .block(); // Block to make it synchronous for MCP
            
            System.out.println("Clone operation completed successfully: " + sourceApp + " -> " + targetApp);
            
        } catch (Exception e) {
            System.err.println("Clone operation failed: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to clone application: " + sourceApp + " -> " + targetApp, e);
        }
    }




    private Path createBuildpackPlaceholder(String appName, String buildpack) {
        try {
            Path result = createPlaceholderForBuildpack(appName, buildpack);
            System.out.println("Created " + buildpack + " placeholder at: " + result.toAbsolutePath());
            return result;
        } catch (Exception e) {
            System.err.println("Failed to create " + buildpack + " placeholder for app: " + appName);
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to create " + buildpack + " placeholder for app: " + appName, e);
        }
    }


    private Path createPlaceholderForBuildpack(String appName, String buildpack) throws IOException {
        Path tempDir = Files.createTempDirectory("cf-" + buildpack.replace("_", "-") + "-" + appName);
        
        // Create buildpack-specific minimal content that can stage successfully
        switch (buildpack.toLowerCase()) {
            case "java_buildpack", "java_buildpack_offline" -> createJavaPlaceholder(tempDir, appName);
            case "nodejs_buildpack" -> createNodeJsPlaceholder(tempDir, appName);
            case "python_buildpack" -> createPythonPlaceholder(tempDir, appName);
            case "go_buildpack" -> createGoPlaceholder(tempDir, appName);
            case "php_buildpack" -> createPhpPlaceholder(tempDir, appName);
            case "ruby_buildpack" -> createRubyPlaceholder(tempDir, appName);
            case "staticfile_buildpack" -> createStaticPlaceholder(tempDir, appName);
            default -> {
                System.out.println("Unknown buildpack " + buildpack + ", falling back to static placeholder");
                createStaticPlaceholder(tempDir, appName);
            }
        }
        
        return tempDir;
    }

    private void createJavaPlaceholder(Path tempDir, String appName) throws IOException {
        // Create a minimal Spring Boot JAR structure
        String manifestContent = """
                Manifest-Version: 1.0
                Main-Class: org.springframework.boot.loader.JarLauncher
                Start-Class: PlaceholderApplication
                Spring-Boot-Version: 3.0.0
                """;
        
        String javaCode = """
                package placeholder;
                
                @org.springframework.boot.autoconfigure.SpringBootApplication
                public class PlaceholderApplication {
                    public static void main(String[] args) {
                        org.springframework.boot.SpringApplication.run(PlaceholderApplication.class, args);
                    }
                    
                    @org.springframework.web.bind.annotation.RestController
                    static class PlaceholderController {
                        @org.springframework.web.bind.annotation.GetMapping("/")
                        String home() { return "Placeholder for " + appName; }
                    }
                }
                """;
        
        // Create basic pom.xml for Maven detection
        String pomContent = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>placeholder</groupId>
                    <artifactId>%s</artifactId>
                    <version>1.0.0</version>
                    <packaging>jar</packaging>
                    <parent>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-parent</artifactId>
                        <version>3.0.0</version>
                    </parent>
                    <dependencies>
                        <dependency>
                            <groupId>org.springframework.boot</groupId>
                            <artifactId>spring-boot-starter-web</artifactId>
                        </dependency>
                    </dependencies>
                </project>
                """.formatted(appName);
        
        Files.createDirectories(tempDir.resolve("src/main/java/placeholder"));
        Files.writeString(tempDir.resolve("pom.xml"), pomContent);
        Files.writeString(tempDir.resolve("src/main/java/placeholder/PlaceholderApplication.java"), javaCode);
        Files.createDirectories(tempDir.resolve("META-INF"));
        Files.writeString(tempDir.resolve("META-INF/MANIFEST.MF"), manifestContent);
    }

    private void createNodeJsPlaceholder(Path tempDir, String appName) throws IOException {
        String packageJson = """
                {
                  "name": "%s-placeholder",
                  "version": "1.0.0",
                  "main": "server.js",
                  "scripts": {
                    "start": "node server.js"
                  },
                  "engines": {
                    "node": ">=18.0.0"
                  }
                }
                """.formatted(appName);
        
        String serverJs = """
                const http = require('http');
                const port = process.env.PORT || 8080;
                
                const server = http.createServer((req, res) => {
                  res.writeHead(200, { 'Content-Type': 'text/html' });
                  res.end('<h1>Placeholder for %s</h1><p>This app will be replaced with real source.</p>');
                });
                
                server.listen(port, () => {
                  console.log('Placeholder server running on port ' + port);
                });
                """.formatted(appName);
        
        Files.writeString(tempDir.resolve("package.json"), packageJson);
        Files.writeString(tempDir.resolve("server.js"), serverJs);
    }

    private void createPythonPlaceholder(Path tempDir, String appName) throws IOException {
        String requirementsTxt = """
                Flask==2.3.0
                gunicorn==20.1.0
                """;
        
        String appPy = """
                from flask import Flask
                import os
                
                app = Flask(__name__)
                
                @app.route('/')
                def hello():
                    return f'<h1>Placeholder for %s</h1><p>This app will be replaced with real source.</p>'
                
                if __name__ == '__main__':
                    port = int(os.environ.get('PORT', 8080))
                    app.run(host='0.0.0.0', port=port)
                """.formatted(appName);
        
        String procfile = "web: gunicorn app:app";
        
        Files.writeString(tempDir.resolve("requirements.txt"), requirementsTxt);
        Files.writeString(tempDir.resolve("app.py"), appPy);
        Files.writeString(tempDir.resolve("Procfile"), procfile);
    }

    private void createGoPlaceholder(Path tempDir, String appName) throws IOException {
        String goMod = """
                module %s-placeholder
                
                go 1.19
                """.formatted(appName);
        
        String mainGo = """
                package main
                
                import (
                    "fmt"
                    "net/http"
                    "os"
                )
                
                func main() {
                    http.HandleFunc("/", func(w http.ResponseWriter, r *http.Request) {
                        fmt.Fprintf(w, "<h1>Placeholder for %s</h1><p>This app will be replaced with real source.</p>")
                    })
                    
                    port := os.Getenv("PORT")
                    if port == "" {
                        port = "8080"
                    }
                    
                    fmt.Printf("Placeholder server starting on port %%s\\n", port)
                    http.ListenAndServe(":"+port, nil)
                }
                """.formatted(appName);
        
        Files.writeString(tempDir.resolve("go.mod"), goMod);
        Files.writeString(tempDir.resolve("main.go"), mainGo);
    }

    private void createPhpPlaceholder(Path tempDir, String appName) throws IOException {
        String composerJson = """
                {
                    "name": "%s/placeholder",
                    "require": {
                        "php": ">=8.1"
                    }
                }
                """.formatted(appName);
        
        String indexPhp = """
                <?php
                echo "<h1>Placeholder for %s</h1>";
                echo "<p>This app will be replaced with real source.</p>";
                ?>
                """.formatted(appName);
        
        Files.writeString(tempDir.resolve("composer.json"), composerJson);
        Files.writeString(tempDir.resolve("index.php"), indexPhp);
    }

    private void createRubyPlaceholder(Path tempDir, String appName) throws IOException {
        String gemfile = """
                source 'https://rubygems.org'
                ruby '3.1.0'
                
                gem 'sinatra'
                gem 'puma'
                """;
        
        String appRb = """
                require 'sinatra'
                
                get '/' do
                  "<h1>Placeholder for %s</h1><p>This app will be replaced with real source.</p>"
                end
                """.formatted(appName);
        
        String configRu = """
                require './app'
                run Sinatra::Application
                """;
        
        Files.writeString(tempDir.resolve("Gemfile"), gemfile);
        Files.writeString(tempDir.resolve("app.rb"), appRb);
        Files.writeString(tempDir.resolve("config.ru"), configRu);
    }

    private void createStaticPlaceholder(Path tempDir, String appName) throws IOException {
        String indexHtml = """
                <!DOCTYPE html>
                <html>
                <head><title>%s - Placeholder</title></head>
                <body>
                    <h1>Placeholder for %s</h1>
                    <p>This app will be replaced with real source.</p>
                </body>
                </html>
                """.formatted(appName, appName);
        
        String staticfile = "root: .";
        
        Files.writeString(tempDir.resolve("index.html"), indexHtml);
        Files.writeString(tempDir.resolve("Staticfile"), staticfile);
    }

    private Mono<Void> deployPlaceholderWithSourceBuildpack(String appName, Path placeholderPath, String buildpack, String organization, String space, AppConfig config) {
        return getOperations(organization, space).applications()
                .push(PushApplicationRequest.builder()
                        .name(appName)
                        .path(placeholderPath)
                        .noStart(true)           // Don't start placeholder
                        .memory(config.memoryLimit())    // Match source app memory
                        .diskQuota(config.diskQuota())   // Match source app disk
                        .instances(config.instances())   // Match source app instances
                        .buildpack(buildpack)           // Use source app's buildpack
                        .stagingTimeout(Duration.ofMinutes(3)) // Allow time for buildpack-specific staging
                        .build())
                .then(setEnvironmentVariables(appName, config.environmentVariables(), organization, space));
    }



    private Mono<Void> copySourceWithBuildpackVerification(String sourceApp, String targetApp, String expectedBuildpack, String organization, String space, AppConfig config) {
        return getOperations(organization, space).applications()
                .copySource(CopySourceApplicationRequest.builder()
                        .name(sourceApp)
                        .targetName(targetApp)
                        .restart(false)
                        .stagingTimeout(Duration.ofMinutes(8))
                        .startupTimeout(Duration.ofMinutes(5))
                        .build())
                .doOnSuccess(v -> System.out.println("Copied source from " + sourceApp + " to " + targetApp))
                .then(
                        // Scale to match source configuration
                        getOperations(organization, space).applications()
                                .scale(ScaleApplicationRequest.builder()
                                        .name(targetApp)
                                        .memoryLimit(config.memoryLimit())
                                        .diskLimit(config.diskQuota())
                                        .instances(config.instances())
                                        .build())
                                .doOnSuccess(v -> System.out.println("Scaled app to match source configuration"))
                )
                .then(
                        // Start the app with the existing buildpack
                        getOperations(organization, space).applications()
                                .start(StartApplicationRequest.builder()
                                        .name(targetApp)
                                        .stagingTimeout(Duration.ofMinutes(8))
                                        .startupTimeout(Duration.ofMinutes(5))
                                        .build())
                                .doOnSuccess(v -> System.out.println("Started app with pre-configured buildpack"))
                )
                .then(
                        // Verify buildpack matches expected (should be guaranteed now)
                        getBuildpackInfo(targetApp, organization, space)
                                .flatMap(actualBuildpack -> {
                                    if (expectedBuildpack.equals(actualBuildpack)) {
                                        System.out.println("✓ SUCCESS: Buildpack preserved during source copy - " + actualBuildpack);
                                        return Mono.empty();
                                    } else {
                                        String errorMsg = String.format(
                                                "❌ UNEXPECTED: Buildpack changed during copy! Expected: %s, Actual: %s. " +
                                                "This should not happen with buildpack-matched placeholder.",
                                                expectedBuildpack, actualBuildpack);
                                        System.err.println(errorMsg);
                                        return Mono.error(new RuntimeException(errorMsg));
                                    }
                                })
                );
    }


    private Mono<String> getBuildpackInfo(String appName, String organization, String space) {
        return getOperations(organization, space).applications()
                .get(GetApplicationRequest.builder().name(appName).build())
                .map(appDetail -> {
                    List<String> buildpacks = appDetail.getBuildpacks();
                    return (buildpacks != null && !buildpacks.isEmpty()) ?
                            String.join(", ", buildpacks) : "unknown";
                });
    }

    /**
     * Get source application configuration (memory, disk, instances, environment variables)
     */
    private Mono<AppConfig> getSourceAppConfig(String sourceApp, String organization, String space) {
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

    private Mono<Map<String, String>> getEnvironmentVariables(String appName, String organization, String space) {
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

    private Mono<Void> setEnvironmentVariables(String appName, Map<String, String> envVars, String organization, String space) {
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

    private void cleanup(Path tempDir) {
        if (tempDir == null) return;

        try (var pathStream = Files.walk(tempDir)) {
            pathStream.sorted(Comparator.reverseOrder()) // Critical: files before directories
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            // Silent cleanup - don't break main flow
                            System.err.println("Warning: Could not delete " + path);
                        }
                    });
        } catch (IOException e) {
            System.err.println("Warning: Could not traverse directory " + tempDir);
        }
    }

    record AppConfig(Integer memoryLimit, Integer diskQuota, Integer instances, Map<String, String> environmentVariables) {}
}
