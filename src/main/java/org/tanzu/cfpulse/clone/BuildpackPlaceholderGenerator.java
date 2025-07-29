package org.tanzu.cfpulse.clone;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Generates buildpack-specific placeholder applications for consistent deployment.
 * Each buildpack type gets a minimal but valid application structure.
 */
@Component
public class BuildpackPlaceholderGenerator {

    public Path createPlaceholder(String appName, String buildpack) {
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
                        String home() { return "Placeholder for """ + appName + """
                        "; }
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
}